package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GoalStack - Advanced goal management system with intelligent prioritization
 * 
 * Features:
 * - Automatic goal generation based on bot archetype
 * - Smart goal prioritization based on urgency and context
 * - Progress tracking and goal lifecycle management
 * - Goal conflict resolution
 * - Performance analytics
 * 
 * This is the core goal management system for bot AI.
 */
public class GoalStack {
    
    // ===== Core Data Structures =====
    
    private final AIPlayer bot;
    private final Queue<Goal> shortTermGoals;     // 0-30 minutes
    private final Queue<Goal> mediumTermGoals;    // 30min - 4 hours  
    private final Queue<Goal> longTermGoals;      // 4+ hours
    private final List<Goal> completedGoals;
    private final List<Goal> failedGoals;
    
    // ===== Goal Generation =====
    private long lastGoalGenerationTime;
    private static final long GOAL_GENERATION_INTERVAL = 300000; // 5 minutes
    private static final int MAX_GOALS_PER_TIER = 10;
    
    // ===== Progress Tracking =====
    private int goalsCompleted;
    private int goalsFailed;
    private long totalTimeSpent;
    private Map<Goal.GoalCategory, Integer> categoryProgress;
    
    // ===== Current State =====
    private Goal currentGoal;
    private String currentActivity;
    private long currentGoalStartTime;
    
    public GoalStack(AIPlayer bot) {
        this.bot = bot;
        this.shortTermGoals = new ConcurrentLinkedQueue<>();
        this.mediumTermGoals = new ConcurrentLinkedQueue<>(); 
        this.longTermGoals = new ConcurrentLinkedQueue<>();
        this.completedGoals = new ArrayList<>();
        this.failedGoals = new ArrayList<>();
        
        this.lastGoalGenerationTime = 0;
        this.goalsCompleted = 0;
        this.goalsFailed = 0;
        this.totalTimeSpent = 0;
        this.categoryProgress = new EnumMap<>(Goal.GoalCategory.class);
        
        this.currentGoal = null;
        this.currentActivity = "idle";
        this.currentGoalStartTime = 0;
        
        // Initialize category progress tracking
        for (Goal.GoalCategory category : Goal.GoalCategory.values()) {
            categoryProgress.put(category, 0);
        }
        
        // Generate initial goals
        generateGoalsIfNeeded();
    }
    
    // ===== Main Goal Management =====
    
    /**
     * Get the current goal the bot should be working on
     */
    public Goal getCurrentGoal() {
        // Clean up invalid goals first
        cleanupInvalidGoals();
        
        // Generate new goals if needed
        generateGoalsIfNeeded();
        
        // If no current goal or current goal is completed, get next goal
        if (currentGoal == null || currentGoal.getStatus() != Goal.Status.ACTIVE) {
            currentGoal = selectNextGoal();
            if (currentGoal != null) {
                currentGoalStartTime = System.currentTimeMillis();
                currentGoal.setCurrentStep("Starting goal: " + currentGoal.getDescription());
                
                // Log goal selection
                logGoalSelection(currentGoal);
            }
        }
        
        return currentGoal;
    }
    
    /**
     * Update progress on the current goal
     */
    public void updateCurrentGoal(String activity, double progressIncrement) {
        this.currentActivity = activity;
        
        if (currentGoal != null && currentGoal.getStatus() == Goal.Status.ACTIVE) {
            currentGoal.setCurrentStep(activity);
            if (progressIncrement > 0) {
                currentGoal.incrementProgress(progressIncrement);
                
                // Check if goal completed
                if (currentGoal.getStatus() == Goal.Status.COMPLETED) {
                    completeCurrentGoal();
                }
            }
        }
    }
    
    /**
     * Mark current goal as completed
     */
    public void completeCurrentGoal() {
        if (currentGoal != null) {
            currentGoal.complete();
            
            // Track completion stats
            goalsCompleted++;
            totalTimeSpent += System.currentTimeMillis() - currentGoalStartTime;
            categoryProgress.merge(currentGoal.getCategory(), 1, Integer::sum);
            
            // Move to completed list
            completedGoals.add(currentGoal);
            removeGoalFromAllQueues(currentGoal);
            
            // Log completion
            logGoalCompletion(currentGoal);
            
            // Clear current goal so next one is selected
            currentGoal = null;
        }
    }
    
    /**
     * Mark current goal as failed
     */
    public void failCurrentGoal(String reason) {
        if (currentGoal != null) {
            currentGoal.fail(reason);
            
            // Track failure stats
            goalsFailed++;
            
            // Move to failed list
            failedGoals.add(currentGoal);
            removeGoalFromAllQueues(currentGoal);
            
            // Log failure
            logGoalFailure(currentGoal, reason);
            
            // Clear current goal so next one is selected
            currentGoal = null;
        }
    }
    
    /**
     * Abandon current goal
     */
    public void abandonCurrentGoal(String reason) {
        if (currentGoal != null && currentGoal.isAbandonable()) {
            currentGoal.abandon(reason);
            removeGoalFromAllQueues(currentGoal);
            
            // Log abandonment
            logGoalAbandon(currentGoal, reason);
            
            // Clear current goal
            currentGoal = null;
        }
    }
    
    // ===== Goal Selection Logic =====
    
    /**
     * Select the next highest priority goal
     */
    private Goal selectNextGoal() {
        // Priority order: Short -> Medium -> Long term
        Goal nextGoal = selectBestGoalFromQueue(shortTermGoals);
        if (nextGoal != null) return nextGoal;
        
        nextGoal = selectBestGoalFromQueue(mediumTermGoals);
        if (nextGoal != null) return nextGoal;
        
        nextGoal = selectBestGoalFromQueue(longTermGoals);
        return nextGoal;
    }
    
    /**
     * Select the best goal from a queue based on urgency and context
     */
    private Goal selectBestGoalFromQueue(Queue<Goal> queue) {
        Goal bestGoal = null;
        double bestScore = -1;
        
        for (Goal goal : queue) {
            if (!goal.isValid()) continue;
            
            double score = calculateGoalScore(goal);
            if (score > bestScore) {
                bestScore = score;
                bestGoal = goal;
            }
        }
        
        return bestGoal;
    }
    
    /**
     * Calculate a score for goal selection
     */
    private double calculateGoalScore(Goal goal) {
        double score = goal.getUrgency();
        
        // Bonus for quick wins (short goals)
        if (goal.getEstimatedTime() < 1800000) { // < 30 minutes
            score += 0.2;
        }
        
        // Bonus for goals matching bot's current situation
        score += calculateContextBonus(goal);
        
        // Penalty for goals that have been failing
        if (hasRecentFailures(goal.getCategory())) {
            score -= 0.3;
        }
        
        return score;
    }
    
    /**
     * Calculate context bonus based on bot's current state
     */
    private double calculateContextBonus(Goal goal) {
        double bonus = 0;
        
        // Economic goals are more valuable when bot is poor
        if (goal.getCategory() == Goal.GoalCategory.ECONOMIC) {
            // Would check actual bank value in full implementation
            bonus += 0.1;
        }
        
        // Combat goals based on current combat level
        if (goal.getCategory() == Goal.GoalCategory.COMBAT) {
            int combatLevel = bot.getSkills().getCombatLevel();
            if (combatLevel < 100) {
                bonus += 0.15;
            }
        }
        
        return bonus;
    }
    
    /**
     * Check if there have been recent failures in a goal category
     */
    private boolean hasRecentFailures(Goal.GoalCategory category) {
        long recentTime = System.currentTimeMillis() - 1800000; // 30 minutes
        
        return failedGoals.stream()
                .anyMatch(goal -> goal.getCategory() == category && 
                                goal.getCreatedTime() > recentTime);
    }
    
    // ===== Goal Generation =====
    
    /**
     * Generate new goals if needed
     */
    private void generateGoalsIfNeeded() {
        long currentTime = System.currentTimeMillis();
        
        // Check if enough time has passed since last generation
        if (currentTime - lastGoalGenerationTime < GOAL_GENERATION_INTERVAL) {
            return;
        }
        
        // Check if we need more goals
        int totalGoals = shortTermGoals.size() + mediumTermGoals.size() + longTermGoals.size();
        if (totalGoals >= 15) { // Don't overwhelm the bot
            return;
        }
        
        try {
            // Generate new goals using the ArchetypeGoalGenerator
            List<Goal> newGoals = ArchetypeGoalGenerator.generateGoals(bot);
            
            // Categorize and add goals
            categorizeAndAddGoals(newGoals);
            
            lastGoalGenerationTime = currentTime;
            
            // Log goal generation
            System.out.println("[GoalStack] Generated " + newGoals.size() + 
                             " new goals for " + bot.getDisplayName());
            
        } catch (Exception e) {
            System.err.println("[GoalStack] Failed to generate goals for " + 
                             bot.getDisplayName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Categorize goals by estimated time and add to appropriate queues
     */
    private void categorizeAndAddGoals(List<Goal> goals) {
        for (Goal goal : goals) {
            if (goal == null) continue;
            
            // Avoid duplicates
            if (isGoalAlreadyPresent(goal)) {
                continue;
            }
            
            // Categorize by estimated time
            long estimatedTime = goal.getEstimatedTime();
            
            if (estimatedTime <= 1800000) { // <= 30 minutes
                addGoalToQueue(shortTermGoals, goal);
            } else if (estimatedTime <= 14400000) { // <= 4 hours  
                addGoalToQueue(mediumTermGoals, goal);
            } else {
                addGoalToQueue(longTermGoals, goal);
            }
        }
    }
    
    /**
     * Add goal to queue if not full
     */
    private void addGoalToQueue(Queue<Goal> queue, Goal goal) {
        if (queue.size() < MAX_GOALS_PER_TIER) {
            queue.offer(goal);
        }
    }
    
    /**
     * Check if a similar goal is already present
     */
    private boolean isGoalAlreadyPresent(Goal newGoal) {
        String description = newGoal.getDescription();
        
        return getAllActiveGoals().stream()
                .anyMatch(goal -> goal.getDescription().equals(description));
    }
    
    // ===== Goal Cleanup =====
    
    /**
     * Remove invalid goals from all queues
     */
    private void cleanupInvalidGoals() {
        cleanupQueue(shortTermGoals);
        cleanupQueue(mediumTermGoals);  
        cleanupQueue(longTermGoals);
    }
    
    /**
     * Remove invalid goals from a specific queue
     */
    private void cleanupQueue(Queue<Goal> queue) {
        queue.removeIf(goal -> !goal.isValid());
    }
    
    /**
     * Remove a specific goal from all queues
     */
    private void removeGoalFromAllQueues(Goal goal) {
        shortTermGoals.remove(goal);
        mediumTermGoals.remove(goal);
        longTermGoals.remove(goal);
    }
    
    // ===== Status and Information =====
    
    /**
     * Get all active goals
     */
    public List<Goal> getAllActiveGoals() {
        List<Goal> allGoals = new ArrayList<>();
        allGoals.addAll(shortTermGoals);
        allGoals.addAll(mediumTermGoals);
        allGoals.addAll(longTermGoals);
        return allGoals;
    }
    
    /**
     * Get goal summary for monitoring
     */
    public String getGoalSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Goals: S=").append(shortTermGoals.size())
          .append(" M=").append(mediumTermGoals.size())
          .append(" L=").append(longTermGoals.size())
          .append(" | Completed=").append(goalsCompleted)
          .append(" Failed=").append(goalsFailed);
        
        if (currentGoal != null) {
            sb.append(" | Current: ").append(currentGoal.getDescription());
        }
        
        return sb.toString();
    }
    
    /**
     * Get current activity description
     */
    public String getCurrentActivity() {
        if (currentGoal != null) {
            return currentGoal.getCurrentStep();
        }
        return currentActivity;
    }
    
    /**
     * Get statistics
     */
    public GoalStatistics getStatistics() {
        return new GoalStatistics(
            goalsCompleted,
            goalsFailed,
            getAllActiveGoals().size(),
            totalTimeSpent,
            new HashMap<>(categoryProgress)
        );
    }
    
    // ===== Logging =====
    
    private void logGoalSelection(Goal goal) {
        System.out.println("[GoalStack] " + bot.getDisplayName() + " selected goal: " + 
                         goal.getDescription());
    }
    
    private void logGoalCompletion(Goal goal) {
        long timeSpent = System.currentTimeMillis() - currentGoalStartTime;
        System.out.println("[GoalStack] " + bot.getDisplayName() + " completed goal: " + 
                         goal.getDescription() + " in " + (timeSpent/1000) + "s");
    }
    
    private void logGoalFailure(Goal goal, String reason) {
        System.out.println("[GoalStack] " + bot.getDisplayName() + " failed goal: " + 
                         goal.getDescription() + " - " + reason);
    }
    
    private void logGoalAbandon(Goal goal, String reason) {
        System.out.println("[GoalStack] " + bot.getDisplayName() + " abandoned goal: " + 
                         goal.getDescription() + " - " + reason);
    }
    
    // ===== Statistics Class =====
    
    public static class GoalStatistics {
        private final int completed;
        private final int failed;
        private final int active;
        private final long totalTime;
        private final Map<Goal.GoalCategory, Integer> categoryProgress;
        
        public GoalStatistics(int completed, int failed, int active, long totalTime,
                            Map<Goal.GoalCategory, Integer> categoryProgress) {
            this.completed = completed;
            this.failed = failed;
            this.active = active;
            this.totalTime = totalTime;
            this.categoryProgress = categoryProgress;
        }
        
        public int getCompleted() { return completed; }
        public int getFailed() { return failed; }
        public int getActive() { return active; }
        public long getTotalTime() { return totalTime; }
        public Map<Goal.GoalCategory, Integer> getCategoryProgress() { return categoryProgress; }
        
        public double getSuccessRate() {
            int total = completed + failed;
            return total > 0 ? (double) completed / total : 0.0;
        }
        
        public long getAverageCompletionTime() {
            return completed > 0 ? totalTime / completed : 0;
        }
    }
}
