package com.rs.bot;

import java.util.*;
import com.rs.utils.Utils;

public class Mission {
    public enum MissionType {
        SKILL_GOAL,      // "99 Woodcutting"
        ITEM_GOAL,       // "Dragon Scimitar"
        WEALTH_GOAL,     // "10M bank value"
        FASHION_GOAL,    // "Full Bandos outfit"
        SOCIAL_GOAL,     // "Join active clan"
        COLLECTION_GOAL, // "All skill capes"
        COMBAT_GOAL,     // "Max combat stats"
        QUEST_GOAL,      // "Quest Cape"
        PVP_GOAL,        // "100 PK kills"
        PRESTIGE_GOAL    // "Party hat ownership"
    }
    
    public enum Priority {
        CRITICAL(100),   // Must complete ASAP
        HIGH(75),        // Very important
        MEDIUM(50),      // Normal priority
        LOW(25),         // Eventually
        LUXURY(10);      // Only when rich/bored
        
        private final int value;
        Priority(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    
    private String missionId;
    private String displayName;
    private MissionType type;
    private Priority priority;
    private Map<String, Object> requirements; // skill levels, items needed, etc.
    private Map<String, Object> rewards;      // what bot gets when complete
    private List<Mission> subMissions;        // breaking down complex goals
    private List<Mission> prerequisites;      // must complete these first
    private boolean isCompleted;
    private boolean isActive;
    private long startTime;
    private long estimatedDuration;           // ticks to complete
    private int progressPercent;
    
    public Mission(String missionId, String displayName, MissionType type, Priority priority) {
        this.missionId = missionId;
        this.displayName = displayName;
        this.type = type;
        this.priority = priority;
        this.requirements = new HashMap<>();
        this.rewards = new HashMap<>();
        this.subMissions = new ArrayList<>();
        this.prerequisites = new ArrayList<>();
        this.isCompleted = false;
        this.isActive = false;
        this.startTime = 0;
        this.estimatedDuration = 0;
        this.progressPercent = 0;
    }
    
    // Factory methods for common mission types
    public static Mission createSkillGoal(String skill, int targetLevel, Priority priority) {
        Mission mission = new Mission(
            "skill_" + skill.toLowerCase() + "_" + targetLevel,
            "Get " + targetLevel + " " + skill,
            MissionType.SKILL_GOAL,
            priority
        );
        mission.addRequirement("skill_" + skill.toLowerCase(), targetLevel);
        mission.addReward("xp_" + skill.toLowerCase(), calculateXpForLevel(targetLevel));
        return mission;
    }
    
    public static Mission createItemGoal(String itemName, int itemId, Priority priority) {
        Mission mission = new Mission(
            "item_" + itemId,
            "Obtain " + itemName,
            MissionType.ITEM_GOAL,
            priority
        );
        mission.addRequirement("item_" + itemId, 1);
        mission.addReward("item_" + itemId, 1);
        return mission;
    }
    
    public static Mission createWealthGoal(long targetGp, Priority priority) {
        Mission mission = new Mission(
            "wealth_" + targetGp,
            "Build " + formatGp(targetGp) + " bank",
            MissionType.WEALTH_GOAL,
            priority
        );
        mission.addRequirement("total_wealth", targetGp);
        mission.addReward("economic_status", targetGp / 1000000); // Prestige points
        return mission;
    }
    
    public static Mission createFashionGoal(String outfitName, List<Integer> itemIds, Priority priority) {
        Mission mission = new Mission(
            "fashion_" + outfitName.toLowerCase().replaceAll(" ", "_"),
            "Complete " + outfitName + " outfit",
            MissionType.FASHION_GOAL,
            priority
        );
        
        for (int itemId : itemIds) {
            mission.addRequirement("item_" + itemId, 1);
        }
        mission.addReward("fashion_points", itemIds.size() * 10);
        return mission;
    }
    
    // Requirement management
    public void addRequirement(String key, Object value) {
        requirements.put(key, value);
    }
    
    public void addReward(String key, Object value) {
        rewards.put(key, value);
    }
    
    public void addSubMission(Mission subMission) {
        subMissions.add(subMission);
        subMission.setParentMission(this);
    }
    
    public void addPrerequisite(Mission prerequisite) {
        prerequisites.add(prerequisite);
    }
    
    // Mission state management
    public void start() {
        if (canStart()) {
            isActive = true;
            startTime = System.currentTimeMillis();
            System.out.println("[Mission] Started: " + displayName);
        }
    }
    
    public void complete() {
        isCompleted = true;
        isActive = false;
        progressPercent = 100;
        System.out.println("[Mission] Completed: " + displayName);
    }
    
    public boolean canStart() {
        // All prerequisites must be completed
        for (Mission prereq : prerequisites) {
            if (!prereq.isCompleted()) {
                return false;
            }
        }
        return true;
    }
    
    public void updateProgress(AIPlayer bot) {
        if (!isActive) return;
        
        int totalRequirements = requirements.size();
        int metRequirements = 0;
        
        for (Map.Entry<String, Object> req : requirements.entrySet()) {
            if (checkRequirement(bot, req.getKey(), req.getValue())) {
                metRequirements++;
            }
        }
        
        progressPercent = totalRequirements > 0 ? (metRequirements * 100) / totalRequirements : 100;
        
        // Auto-complete if all requirements met
        if (progressPercent >= 100) {
            complete();
        }
    }
    
    private boolean checkRequirement(AIPlayer bot, String reqKey, Object reqValue) {
        if (reqKey.startsWith("skill_")) {
            String skill = reqKey.substring(6);
            int requiredLevel = (Integer) reqValue;
            return getPlayerSkillLevel(bot, skill) >= requiredLevel;
        }
        
        if (reqKey.startsWith("item_")) {
            int itemId = Integer.parseInt(reqKey.substring(5));
            int requiredAmount = (Integer) reqValue;
            return bot.getInventory().getAmountOf(itemId) >= requiredAmount ||
                   (bot.getBank() != null && bot.getBank().getAmountOf(itemId) >= requiredAmount);
        }
        
        if (reqKey.equals("total_wealth")) {
            long requiredWealth = (Long) reqValue;
            return calculateTotalWealth(bot) >= requiredWealth;
        }
        
        return false;
    }
    
    // Helper methods
    private static int calculateXpForLevel(int level) {
        if (level <= 1) return 0;
        int xp = 0;
        for (int i = 1; i < level; i++) {
            xp += Math.floor(i + 300 * Math.pow(2, i / 7.0));
        }
        return xp / 4;
    }
    
    private static String formatGp(long amount) {
        if (amount >= 1000000000) return (amount / 1000000000) + "B";
        if (amount >= 1000000) return (amount / 1000000) + "M";
        if (amount >= 1000) return (amount / 1000) + "K";
        return String.valueOf(amount);
    }
    
    private int getPlayerSkillLevel(AIPlayer bot, String skill) {
        // Map skill names to skill IDs - simplified
        Map<String, Integer> skillMap = new HashMap<>();
        skillMap.put("attack", 0);
        skillMap.put("defence", 1);
        skillMap.put("strength", 2);
        skillMap.put("constitution", 3);
        skillMap.put("ranged", 4);
        skillMap.put("prayer", 5);
        skillMap.put("magic", 6);
        skillMap.put("cooking", 7);
        skillMap.put("woodcutting", 8);
        skillMap.put("fletching", 9);
        skillMap.put("fishing", 10);
        skillMap.put("firemaking", 11);
        skillMap.put("crafting", 12);
        skillMap.put("smithing", 13);
        skillMap.put("mining", 14);
        skillMap.put("herblore", 15);
        skillMap.put("agility", 16);
        skillMap.put("thieving", 17);
        skillMap.put("slayer", 18);
        skillMap.put("farming", 19);
        skillMap.put("runecrafting", 20);
        skillMap.put("hunter", 21);
        skillMap.put("construction", 22);
        skillMap.put("summoning", 23);
        skillMap.put("dungeoneering", 24);
        
        Integer skillId = skillMap.get(skill.toLowerCase());
        return skillId != null ? bot.getSkills().getLevel(skillId) : 1;
    }
    
    private long calculateTotalWealth(AIPlayer bot) {
        long wealth = 0;
        
        // Add coins from inventory
        wealth += bot.getInventory().getAmountOf(995);
        
        // Add estimated value of items (simplified)
        for (int i = 0; i < 28; i++) {
            if (bot.getInventory().getItem(i) != null) {
                wealth += 1000; // Simplified item value
            }
        }
        
        return wealth;
    }
    
    // Getters and setters
    public String getMissionId() { return missionId; }
    public String getDisplayName() { return displayName; }
    public MissionType getType() { return type; }
    public Priority getPriority() { return priority; }
    public boolean isCompleted() { return isCompleted; }
    public boolean isActive() { return isActive; }
    public int getProgressPercent() { return progressPercent; }
    public List<Mission> getSubMissions() { return new ArrayList<>(subMissions); }
    public List<Mission> getPrerequisites() { return new ArrayList<>(prerequisites); }
    public Map<String, Object> getRequirements() { return new HashMap<>(requirements); }
    
    private Mission parentMission;
    private void setParentMission(Mission parent) { this.parentMission = parent; }
    public Mission getParentMission() { return parentMission; }
    
    @Override
    public String toString() {
        return displayName + " [" + progressPercent + "% complete]";
    }
}
