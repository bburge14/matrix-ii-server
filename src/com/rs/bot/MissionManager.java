package com.rs.bot;

import java.util.*;
import com.rs.utils.Utils;

public class MissionManager {
    private AIPlayer bot;
    private List<Mission> activeMissions;
    private List<Mission> completedMissions;
    private List<Mission> availableMissions;
    private Mission primaryMission;
    private Queue<Mission> missionQueue;
    
    public MissionManager(AIPlayer bot) {
        this.bot = bot;
        this.activeMissions = new ArrayList<>();
        this.completedMissions = new ArrayList<>();
        this.availableMissions = new ArrayList<>();
        this.missionQueue = new LinkedList<>();
        
        initializeMissions();
        selectInitialMissions();
    }
    
    private void initializeMissions() {
        generateSkillMissions();
        generateItemMissions();
        generateWealthMissions();
        selectInitialMissions();
    }
    
    private void generateSkillMissions() {
        String[] skills = {
            "attack", "defence", "strength", "constitution", "ranged", "prayer", "magic",
            "cooking", "woodcutting", "fletching", "fishing", "firemaking", "crafting",
            "smithing", "mining", "herblore", "agility", "thieving", "slayer", "farming",
            "runecrafting", "hunter", "construction", "summoning", "dungeoneering"
        };
        
        for (String skill : skills) {
            // Progressive skill goals: 10, 20, 30, 40, 50, 60, 70, 80, 90, 99
            for (int level = 10; level <= 99; level += 10) {
                Mission.Priority priority = determineSkillPriority(skill, level);
                Mission skillMission = Mission.createSkillGoal(skill, level, priority);
                availableMissions.add(skillMission);
            }
        }
    }
    
    private void generateItemMissions() {
        // Weapon missions
        availableMissions.add(Mission.createItemGoal("Bronze Scimitar", 1321, Mission.Priority.LOW));
        availableMissions.add(Mission.createItemGoal("Iron Scimitar", 1323, Mission.Priority.LOW));
        availableMissions.add(Mission.createItemGoal("Steel Scimitar", 1325, Mission.Priority.MEDIUM));
        availableMissions.add(Mission.createItemGoal("Mithril Scimitar", 1329, Mission.Priority.MEDIUM));
        availableMissions.add(Mission.createItemGoal("Adamant Scimitar", 1331, Mission.Priority.HIGH));
        availableMissions.add(Mission.createItemGoal("Rune Scimitar", 1333, Mission.Priority.HIGH));
        availableMissions.add(Mission.createItemGoal("Dragon Scimitar", 4587, Mission.Priority.CRITICAL));
        
        // Tool missions
        availableMissions.add(Mission.createItemGoal("Rune Axe", 1359, Mission.Priority.MEDIUM));
        availableMissions.add(Mission.createItemGoal("Dragon Axe", 6739, Mission.Priority.HIGH));
        availableMissions.add(Mission.createItemGoal("Rune Pickaxe", 1275, Mission.Priority.MEDIUM));
        
        // Special items
        availableMissions.add(Mission.createItemGoal("Abyssal Whip", 4151, Mission.Priority.LUXURY));
        availableMissions.add(Mission.createItemGoal("Party Hat", 1038, Mission.Priority.LUXURY));
    }
    
    private void generateWealthMissions() {
        long[] wealthTargets = {10000, 50000, 100000, 500000, 1000000, 5000000, 10000000};
        
        for (long target : wealthTargets) {
            Mission.Priority priority = target <= 100000 ? Mission.Priority.MEDIUM : 
                                       target <= 1000000 ? Mission.Priority.HIGH :
                                       Mission.Priority.LUXURY;
            availableMissions.add(Mission.createWealthGoal(target, priority));
        }
    }
    
    private void selectInitialMissions() {
        String archetype = bot.getArchetype() != null ? bot.getArchetype() : "main";
        
        // Add basic starter missions for all bots
        addBasicStarterMissions();
        
        // Add archetype-specific missions
        if (archetype.equals("combat")) {
            addCombatMissions();
        } else if (archetype.equals("skiller")) {
            addSkillerMissions();
        } else {
            addMainMissions();
        }
    }
    
    private void addBasicStarterMissions() {
        // Every bot gets these basic goals
        Mission basicWealth = Mission.createWealthGoal(10000, Mission.Priority.MEDIUM);
        availableMissions.add(basicWealth);
        
        Mission basicCombat = Mission.createSkillGoal("attack", 30, Mission.Priority.LOW);
        availableMissions.add(basicCombat);
    }
    
    private void addCombatMissions() {
        // Combat bots prioritize fighting skills
        availableMissions.add(Mission.createSkillGoal("attack", 60, Mission.Priority.HIGH));
        availableMissions.add(Mission.createSkillGoal("strength", 60, Mission.Priority.HIGH));
        availableMissions.add(Mission.createSkillGoal("defence", 60, Mission.Priority.MEDIUM));
        availableMissions.add(Mission.createItemGoal("Dragon Scimitar", 4587, Mission.Priority.CRITICAL));
    }
    
    private void addSkillerMissions() {
        // Skiller bots focus on non-combat skills
        availableMissions.add(Mission.createSkillGoal("woodcutting", 60, Mission.Priority.HIGH));
        availableMissions.add(Mission.createSkillGoal("mining", 60, Mission.Priority.HIGH));
        availableMissions.add(Mission.createSkillGoal("fishing", 60, Mission.Priority.MEDIUM));
        availableMissions.add(Mission.createItemGoal("Rune Axe", 1359, Mission.Priority.HIGH));
    }
    
    private void addMainMissions() {
        // Balanced approach
        availableMissions.add(Mission.createSkillGoal("attack", 50, Mission.Priority.MEDIUM));
        availableMissions.add(Mission.createSkillGoal("woodcutting", 50, Mission.Priority.MEDIUM));
        availableMissions.add(Mission.createWealthGoal(100000, Mission.Priority.MEDIUM));
    }
    
    public void tick() {
        // Update progress on all active missions
        for (Mission mission : new ArrayList<>(activeMissions)) {
            mission.updateProgress(bot);
            
            if (mission.isCompleted()) {
                completeMission(mission);
            }
        }
        
        // Check if we can start new missions
        checkForNewMissions();
        
        // Set primary mission if none exists
        if (primaryMission == null && !activeMissions.isEmpty()) {
            primaryMission = activeMissions.get(0);
        }
    }
    
    public Mission getCurrentPrimaryMission() {
        return primaryMission;
    }
    
    public List<Mission> getActiveMissions() {
        return new ArrayList<>(activeMissions);
    }
    
    public String getCurrentObjective() {
        if (primaryMission != null) {
            return primaryMission.getDisplayName();
        }
        return "Wandering aimlessly";
    }
    
    private Mission.Priority determineSkillPriority(String skill, int level) {
        String archetype = bot.getArchetype() != null ? bot.getArchetype() : "main";
        
        // Archetype-specific skill priorities
        if (archetype.equals("combat") && isCombatSkill(skill)) {
            return level >= 90 ? Mission.Priority.CRITICAL : Mission.Priority.HIGH;
        }
        
        if (archetype.equals("skiller") && !isCombatSkill(skill)) {
            return level >= 80 ? Mission.Priority.HIGH : Mission.Priority.MEDIUM;
        }
        
        // General priority based on level
        if (level >= 90) return Mission.Priority.LUXURY;
        if (level >= 70) return Mission.Priority.HIGH;
        if (level >= 50) return Mission.Priority.MEDIUM;
        return Mission.Priority.LOW;
    }
    
    private boolean isCombatSkill(String skill) {
        return Arrays.asList("attack", "defence", "strength", "ranged", "magic", "prayer").contains(skill.toLowerCase());
    }
    
    private void completeMission(Mission mission) {
        activeMissions.remove(mission);
        completedMissions.add(mission);
        System.out.println("[MissionManager] " + bot.getDisplayName() + " completed: " + mission.getDisplayName());
        
        // If this was primary mission, select new one
        if (mission == primaryMission) {
            primaryMission = null;
        }
        
        checkForNewMissions();
    }
    
    private void checkForNewMissions() {
        // Start available missions that can be started
        for (Mission mission : new ArrayList<>(availableMissions)) {
            if (mission.canStart() && !activeMissions.contains(mission) && activeMissions.size() < 3) {
                mission.start();
                activeMissions.add(mission);
                availableMissions.remove(mission);
                
                // Set as primary if none exists or higher priority
                if (primaryMission == null || mission.getPriority().getValue() > primaryMission.getPriority().getValue()) {
                    primaryMission = mission;
                }
                break; // Only start one mission per tick
            }
        }
    }
}
