package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import java.io.Serializable;
import java.util.*;

/**
 * Goal - Represents something a bot wants to achieve.
 * 
 * Goals drive all bot behavior. Bots choose activities based on their current goals.
 * Goals can be:
 * - Short-term: "Bank my logs", "Buy food"
 * - Medium-term: "Get 70 Woodcutting", "Complete Dragon Slayer"  
 * - Long-term: "Get Fire Cape", "Max all skills"
 * 
 * Goals have priorities, deadlines, and prerequisites.
 */
public class Goal implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // ===== Basic Properties =====
    private String id;              // Unique identifier
    private String description;     // Human-readable description
    private Priority priority;      // How important this goal is
    private Status status;          // Current status
    
    // ===== Timing =====
    private long createdTime;       // When goal was created
    private long deadline;          // When goal expires (0 = no deadline)
    private long estimatedTime;     // Estimated time to complete (ms)
    
    // ===== Dependencies =====
    private List<String> prerequisites;    // Goals that must complete first
    private List<String> blockedBy;        // Things preventing this goal
    
    // ===== Progress Tracking =====
    private double progress;        // 0.0 - 1.0 completion
    private String currentStep;     // What we're doing now for this goal
    private Map<String, Object> data; // Goal-specific data storage
    
    // ===== Context =====
    private GoalCategory category;  // What type of goal this is
    private double reward;          // Expected satisfaction from completing
    private boolean abandonable;    // Can this goal be abandoned?
    
    public Goal(String id, String description, Priority priority) {
        this.id = id;
        this.description = description;
        this.priority = priority;
        this.status = Status.ACTIVE;
        this.createdTime = System.currentTimeMillis();
        this.deadline = 0;
        this.estimatedTime = 0;
        this.prerequisites = new ArrayList<>();
        this.blockedBy = new ArrayList<>();
        this.progress = 0.0;
        this.currentStep = "starting";
        this.data = new HashMap<>();
        this.category = GoalCategory.OTHER;
        this.reward = calculateBaseReward();
        this.abandonable = true;
    }
    
    /**
     * Full constructor for complex goals
     */
    public Goal(String id, String description, Priority priority, GoalCategory category,
               long estimatedTime, double reward, boolean abandonable) {
        this(id, description, priority);
        this.category = category;
        this.estimatedTime = estimatedTime;
        this.reward = reward;
        this.abandonable = abandonable;
    }
    
    // ===== Status Management =====
    
    public boolean isValid() {
        // Check if goal is still achievable
        if (status == Status.COMPLETED || status == Status.FAILED || status == Status.ABANDONED) {
            return false;
        }
        
        // Check deadline
        if (deadline > 0 && System.currentTimeMillis() > deadline) {
            status = Status.FAILED;
            return false;
        }
        
        // Check prerequisites (basic check - can be enhanced)
        for (String prereq : prerequisites) {
            if (isBlocked(prereq)) {
                return false;
            }
        }
        
        return true;
    }
    
    public void complete() {
        this.status = Status.COMPLETED;
        this.progress = 1.0;
        this.currentStep = "completed";
    }
    
    public void fail(String reason) {
        this.status = Status.FAILED;
        this.currentStep = "failed: " + reason;
    }
    
    public void abandon(String reason) {
        if (abandonable) {
            this.status = Status.ABANDONED;
            this.currentStep = "abandoned: " + reason;
        }
    }
    
    public void pause() {
        if (status == Status.ACTIVE) {
            this.status = Status.PAUSED;
        }
    }
    
    public void resume() {
        if (status == Status.PAUSED) {
            this.status = Status.ACTIVE;
        }
    }
    
    // ===== Progress Management =====
    
    public void updateProgress(double newProgress) {
        // The progress field is now display-only - HUD and logs may show
        // "73% done" for a sense of activity, but real completion is
        // gated by Goal.isCompleted(bot) which reads actual game state.
        // We deliberately do NOT auto-complete on progress >= 1.0
        // anymore - that was the fake-timer behaviour.
        this.progress = Math.max(0.0, Math.min(1.0, newProgress));
    }
    
    public void incrementProgress(double increment) {
        updateProgress(progress + increment);
    }
    
    public void setCurrentStep(String step) {
        this.currentStep = step;
    }
    
    // ===== Priority and Urgency =====
    
    /**
     * Calculate current urgency based on deadline and priority
     */
    public double getUrgency() {
        double basePriority = priority.getWeight();
        
        if (deadline == 0) {
            return basePriority; // No deadline, just use priority
        }
        
        long timeRemaining = deadline - System.currentTimeMillis();
        long totalTime = deadline - createdTime;
        
        if (timeRemaining <= 0) {
            return 1.0; // Overdue = maximum urgency
        }
        
        // Urgency increases as deadline approaches
        double timeRatio = 1.0 - ((double) timeRemaining / totalTime);
        return basePriority + (timeRatio * 0.5); // Boost priority as deadline approaches
    }
    
    // ===== Dependencies =====
    
    public void addPrerequisite(String goalId) {
        if (!prerequisites.contains(goalId)) {
            prerequisites.add(goalId);
        }
    }
    
    public void addBlocker(String blockerId) {
        if (!blockedBy.contains(blockerId)) {
            blockedBy.add(blockerId);
        }
    }
    
    public void removeBlocker(String blockerId) {
        blockedBy.remove(blockerId);
    }
    
    private boolean isBlocked(String blockerId) {
        // Placeholder - would check actual game state
        return false;
    }

    // ===== Real game-state predicates =====

    /**
     * Is this goal already accomplished by the bot's current state?
     * Checks real skill levels, equipment, inventory, bank, and wealth via
     * GoalStateChecker. Used by GoalStack to auto-complete goals that are
     * already met (e.g. bot already has rune armor) and by the goal
     * generator to skip goals that are already done.
     */
    public boolean isCompleted(AIPlayer bot) {
        if (bot == null) return false;
        GoalType type = getData("goalType", GoalType.class);
        if (type == null) return false;
        return GoalStateChecker.isMet(bot, type.getRequirementKey(), type.getRequirementValue());
    }

    /**
     * Is this goal worth pursuing? Right now this is just "not yet
     * completed". Future hooks (prerequisites, archetype gating, mood)
     * can layer on top.
     */
    public boolean isRelevant(AIPlayer bot) {
        return !isCompleted(bot);
    }

    /**
     * Is this goal something the bot has any plan for? Skill goals must
     * have a TrainingMethods method that applies; equipment/wealth/combat
     * goals always pass because they fall through to money-grind.
     *
     * Used by goal generation so bots don't get e.g. "99 Fletching"
     * goals when there's no fletching method wired - they'd just wander.
     */
    public boolean isAchievable(AIPlayer bot) {
        if (!isRelevant(bot)) return false;
        GoalType type = getData("goalType", GoalType.class);
        if (type == null) return true;
        if (type.getCategory() == Goal.GoalCategory.SKILL) {
            return com.rs.bot.ai.TrainingMethods.bestMethodFor(this, bot) != null;
        }
        return true;
    }
    
    // ===== Data Storage =====
    
    public void setData(String key, Object value) {
        data.put(key, value);
    }
    
    public Object getData(String key) {
        return data.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    // ===== Utility =====
    
    private double calculateBaseReward() {
        // Base reward calculation based on priority and estimated time
        double priorityReward = priority.getWeight() * 10.0;
        double timeReward = Math.log(Math.max(1000, estimatedTime)) / 1000.0; // Longer goals more rewarding
        return priorityReward + timeReward;
    }
    
    public long getAge() {
        return System.currentTimeMillis() - createdTime;
    }
    
    public long getTimeRemaining() {
        if (deadline == 0) return Long.MAX_VALUE;
        return Math.max(0, deadline - System.currentTimeMillis());
    }
    
    // ===== Getters/Setters =====
    
    public String getId() { return id; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public Status getStatus() { return status; }
    public long getCreatedTime() { return createdTime; }
    public long getDeadline() { return deadline; }
    public long getEstimatedTime() { return estimatedTime; }
    public List<String> getPrerequisites() { return new ArrayList<>(prerequisites); }
    public List<String> getBlockedBy() { return new ArrayList<>(blockedBy); }
    public double getProgress() { return progress; }
    public String getCurrentStep() { return currentStep; }
    public GoalCategory getCategory() { return category; }
    public double getReward() { return reward; }
    public boolean isAbandonable() { return abandonable; }
    
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setDeadline(long deadline) { this.deadline = deadline; }
    public void setEstimatedTime(long estimatedTime) { this.estimatedTime = estimatedTime; }
    public void setCategory(GoalCategory category) { this.category = category; }
    public void setReward(double reward) { this.reward = reward; }
    public void setAbandonable(boolean abandonable) { this.abandonable = abandonable; }
    
    // ===== Display =====
    
    @Override
    public String toString() {
        return String.format("Goal[%s] %s (%s) %.1f%% - %s", 
                           id, description, priority, progress * 100, status);
    }
    
    public String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(toString()).append("\n");
        sb.append("  Category: ").append(category).append("\n");
        sb.append("  Current Step: ").append(currentStep).append("\n");
        sb.append("  Age: ").append(getAge() / 1000).append("s\n");
        if (deadline > 0) {
            sb.append("  Time Remaining: ").append(getTimeRemaining() / 1000).append("s\n");
        }
        if (!prerequisites.isEmpty()) {
            sb.append("  Prerequisites: ").append(prerequisites).append("\n");
        }
        if (!blockedBy.isEmpty()) {
            sb.append("  Blocked By: ").append(blockedBy).append("\n");
        }
        return sb.toString();
    }
    
    // ===== Enums =====
    
    public enum Priority {
        URGENT(1.0),    // Drop everything else
        HIGH(0.8),      // Very important
        MEDIUM(0.5),    // Normal importance  
        LOW(0.3),       // Nice to have
        BACKGROUND(0.1); // Idle task
        
        private final double weight;
        
        Priority(double weight) {
            this.weight = weight;
        }
        
        public double getWeight() { return weight; }
    }
    
    public enum Status {
        ACTIVE,         // Currently working on this
        PAUSED,         // Temporarily stopped
        COMPLETED,      // Successfully finished
        FAILED,         // Could not complete
        ABANDONED       // Gave up
    }
    
    public enum GoalCategory {
        SKILL,          // Skill training goals
        ECONOMIC,       // Money-making goals
        SOCIAL,         // Social interaction goals
        COMBAT,         // Combat/PvM/PvP goals
        EXPLORATION,    // Exploration and discovery
        QUEST,          // Quest completion
        ACHIEVEMENT,    // Achievement hunting
        COLLECTION,     // Item collection goals
        OTHER           // Miscellaneous
    }
}
