package com.rs.bot;

import java.util.*;
import com.rs.utils.Utils;

/**
 * Manages bot's goal hierarchy (short/medium/long-term)
 */
public class GoalStack {
    
    private List<BotGoal> goals;
    private BotGoal currentGoal;
    
    public GoalStack() {
        this.goals = new ArrayList<>();
        this.currentGoal = null;
    }
    
    /**
     * Add a new goal to the stack
     */
    public void addGoal(BotGoal goal) {
        goals.add(goal);
        sortGoalsByPriority();
    }
    
    /**
     * Get the highest priority goal that can be worked on
     */
    public BotGoal getCurrentGoal() {
        if (currentGoal != null && !currentGoal.isCompleted()) {
            return currentGoal;
        }
        
        // Find next available goal
        for (BotGoal goal : goals) {
            if (!goal.isCompleted() && goal.canStart()) {
                currentGoal = goal;
                return goal;
            }
        }
        
        return null; // No available goals
    }
    
    /**
     * Mark current goal as completed and remove from stack
     */
    public void completeCurrentGoal() {
        if (currentGoal != null) {
            currentGoal.setCompleted(true);
            goals.remove(currentGoal);
            currentGoal = null;
        }
    }
    
    /**
     * Add some default goals based on bot level and archetype
     */
    public void generateDefaultGoals(AIPlayer bot) {
        String archetype = bot.getArchetype().toLowerCase();
        int combatLevel = bot.getSkills().getCombatLevel();
        
        // Early game goals (low level)
        if (combatLevel < 20) {
            addGoal(new BotGoal("Get basic gear", GoalType.SHORT_TERM, 100));
            addGoal(new BotGoal("Train to 20 combat", GoalType.SHORT_TERM, 90));
        }
        
        // Archetype-specific goals
        switch (archetype) {
            case "skiller":
                addGoal(new BotGoal("Get 60 Woodcutting", GoalType.MEDIUM_TERM, 80));
                addGoal(new BotGoal("Get 99 Woodcutting", GoalType.LONG_TERM, 70));
                break;
            case "melee":
                addGoal(new BotGoal("Get Dragon weapons", GoalType.MEDIUM_TERM, 80));
                addGoal(new BotGoal("Max melee stats", GoalType.LONG_TERM, 70));
                break;
            case "ranged":
                addGoal(new BotGoal("Get good bow", GoalType.MEDIUM_TERM, 80));
                addGoal(new BotGoal("99 Ranged", GoalType.LONG_TERM, 70));
                break;
            case "magic":
                addGoal(new BotGoal("Get magic gear", GoalType.MEDIUM_TERM, 80));
                addGoal(new BotGoal("99 Magic", GoalType.LONG_TERM, 70));
                break;
        }
        
        // Universal long-term goals
        addGoal(new BotGoal("Build 10M bank", GoalType.LONG_TERM, 60));
        addGoal(new BotGoal("Get max combat", GoalType.LONG_TERM, 50));
    }
    
    private void sortGoalsByPriority() {
        goals.sort((g1, g2) -> Integer.compare(g2.getPriority(), g1.getPriority()));
    }
    
    public List<BotGoal> getAllGoals() { return new ArrayList<>(goals); }
    public boolean hasGoals() { return !goals.isEmpty(); }
}

/**
 * Individual bot goal
 */
class BotGoal {
    private String description;
    private GoalType type;
    private int priority;
    private boolean completed;
    private List<String> prerequisites;
    
    public BotGoal(String description, GoalType type, int priority) {
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.completed = false;
        this.prerequisites = new ArrayList<>();
    }
    
    public boolean canStart() {
        // TODO: Check if prerequisites are met
        return true;
    }
    
    // Getters and setters
    public String getDescription() { return description; }
    public GoalType getType() { return type; }
    public int getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    @Override
    public String toString() {
        return String.format("%s[%s] - %s", type, priority, description);
    }
}

enum GoalType {
    SHORT_TERM,  // Immediate goals (< 1 hour)
    MEDIUM_TERM, // Session goals (1-4 hours) 
    LONG_TERM    // Multi-session goals (days/weeks)
}
