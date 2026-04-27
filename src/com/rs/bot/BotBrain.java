package com.rs.bot;

import java.util.*;
import com.rs.utils.Utils;
import com.rs.game.WorldTile;
import com.rs.game.Graphics;
import com.rs.game.player.PublicChatMessage;
import com.rs.game.player.Player;
import com.rs.game.ForceTalk;
import com.rs.game.Animation;
import com.rs.bot.ai.WorldKnowledge;
import com.rs.bot.ai.Goal;
import com.rs.bot.ai.GoalStack;

public class BotBrain {
    private AIPlayer bot;
    private PersonalityProfile personality;
    private EmotionalState emotionalState;
    private MemorySystem memory;
    private GoalStack goalStack;

    private BotState currentState;
    private BotState previousState;
    private long lastDecisionTime;
    private long lastStateChange;

    // Decision making variables
    private int boredomLevel;
    private int restlessness;
    private long lastMajorDecision;

    // Goal-driven behavior
    private long lastGoalCheck;
    private long lastMovementTick;
    private String currentActivity;

    public BotBrain(AIPlayer bot) {
        this.bot = bot;
        this.personality = new PersonalityProfile();
        this.emotionalState = new EmotionalState();
        this.memory = new MemorySystem();
        this.goalStack = new GoalStack(bot); // Fixed: Pass bot to constructor

        this.currentState = BotState.IDLE;
        this.previousState = BotState.IDLE;
        this.lastDecisionTime = System.currentTimeMillis();
        this.lastStateChange = System.currentTimeMillis();
        this.lastGoalCheck = System.currentTimeMillis();
        this.lastMovementTick = 0L;
        this.boredomLevel = 0;
        this.restlessness = 0;
        this.lastMajorDecision = System.currentTimeMillis();
        this.currentActivity = "initializing";

        System.out.println("[BotBrain] " + bot.getDisplayName() + " initialized with goal-driven AI systems");
    }

    public void tick(AIPlayer bot) {
        if (Utils.random(100) < 1) System.out.println("[TICK-DEBUG] " + bot.getDisplayName() + " brain is ticking");
        long currentTime = System.currentTimeMillis();

        // Update all subsystems
        updateEmotionalState();
        updateBoredomAndRestlessness();

        // Check goals periodically (every 10 seconds) - high-level planning only
        if (currentTime - lastGoalCheck > 10000) {
            checkAndUpdateGoals();
            lastGoalCheck = currentTime;
        }

        // Drive movement every tick. Goal evaluation runs every 10s, but the
        // walkSteps queue only holds a few tiles so we have to keep refilling it.
        // The guard inside executeCurrentGoalActions ensures we don't pile up
        // steps when the bot is already walking.
        if (currentTime - lastMovementTick >= 600) {
            executeCurrentGoalActions();
            lastMovementTick = currentTime;
        }

        // Make decisions based on current state and goals
        makeGoalDrivenDecision(currentTime);

        lastDecisionTime = currentTime;
    }

    /**
     * Per-tick movement/action execution. Runs independently of the slower
     * goal-evaluation cycle so bots keep walking smoothly between re-plans.
     */
    private void executeCurrentGoalActions() {
        Goal currentGoal = goalStack.getCurrentGoal();
        if (currentGoal == null) return;
        // If the bot still has queued steps from a prior tick, let processMovement
        // consume them before we enqueue more. This is what was broken before:
        // we kept overwriting the queue with single-tile micro-steps and the
        // bot looked frozen.
        if (!bot.getWalkSteps().isEmpty()) return;
        executeGoalActions(currentGoal);
    }

    /**
     * NEW: Goal-driven decision making - the heart of intelligent behavior
     */
    private void checkAndUpdateGoals() {
        try {
            Goal currentGoal = goalStack.getCurrentGoal();
            
            if (currentGoal != null) {
                String goalDescription = currentGoal.getDescription();
                String goalStep = currentGoal.getCurrentStep();
                
                // Update activity based on current goal
                currentActivity = goalStep;
                
                // Determine what state we should be in based on the goal
                BotState recommendedState = determineStateForGoal(currentGoal);
                
                if (recommendedState != currentState) {
                    enterState(recommendedState);
                }
                
                // Simulate progress on the goal based on what we're doing.
                // Real movement/animation is driven separately every tick from
                // executeCurrentGoalActions().
                simulateGoalProgress(currentGoal);

                // Log current goal activity
                if (Utils.random(100) < 5) { // 5% chance to log
                    System.out.println("[BotBrain] " + bot.getDisplayName() + " working on: " + 
                                     goalDescription + " - " + goalStep);
                }
                
            } else {
                // No goal - should be planning
                currentActivity = "looking for something to do";
                if (currentState != BotState.PLANNING) {
                    enterState(BotState.PLANNING);
                }
            }
            
        } catch (Exception e) {
            System.err.println("[BotBrain] Error checking goals for " + bot.getDisplayName() + ": " + e.getMessage());
            currentActivity = "confused";
        }
    }
    
    /**
     * Determine what state the bot should be in based on current goal
     */
    private BotState determineStateForGoal(Goal goal) {
        if (goal == null) return BotState.IDLE;
        
        String description = goal.getDescription().toLowerCase();
        String step = goal.getCurrentStep().toLowerCase();
        
        // Banking activities
        if (step.contains("bank") || step.contains("withdraw") || step.contains("deposit")) {
            return BotState.BANKING;
        }
        
        // Travel activities  
        if (step.contains("travel") || step.contains("walk") || step.contains("go to")) {
            return BotState.TRAVELING;
        }
        
        // Social activities
        if (step.contains("chat") || step.contains("talk") || step.contains("social")) {
            return BotState.SOCIAL;
        }
        
        // Planning activities
        if (step.contains("planning") || step.contains("thinking") || step.contains("deciding")) {
            return BotState.PLANNING;
        }
        
        // Everything else is activity
        return BotState.ACTIVITY;
    }
    
    /**
     * Simulate progress on the current goal
     */
    private void simulateGoalProgress(Goal goal) {
        // Simulate different types of progress based on goal category
        double progressIncrement = 0.0;
        String activity = currentActivity;
        
        switch (goal.getCategory()) {
            case SKILL:
                if (currentState == BotState.ACTIVITY) {
                    progressIncrement = 0.001; // 0.1% progress per tick when actively training
                    activity = "training " + extractSkillFromGoal(goal);
                }
                break;
                
            case COMBAT:
                if (currentState == BotState.ACTIVITY) {
                    progressIncrement = 0.0008; // Slightly slower than skills
                    activity = "combat training";
                }
                break;
                
            case ECONOMIC:
                if (currentState == BotState.ACTIVITY) {
                    progressIncrement = 0.0015; // Money making is faster
                    activity = "making money";
                }
                break;
                
            case QUEST:
                if (currentState == BotState.ACTIVITY) {
                    progressIncrement = 0.002; // Quests complete faster
                    activity = "doing quest";
                }
                break;
                
            default:
                if (currentState == BotState.ACTIVITY) {
                    progressIncrement = 0.001;
                    activity = "working on goal";
                }
        }
        
        // Personality affects efficiency
        progressIncrement *= personality.getEfficiencyMultiplier();
        
        // Emotional state affects efficiency
        progressIncrement *= emotionalState.getEfficiencyModifier();
        
        // Random variation
        if (Utils.random(100) < 60) { // 20% chance for bonus progress
            progressIncrement *= 1.5;
            activity += " (focused)";
        }
        
        // Update goal progress
        if (progressIncrement > 0) {
            goalStack.updateCurrentGoal(activity, progressIncrement);
        }
    }
    
    /**
     * Extract skill name from skill goal
     */
    private String extractSkillFromGoal(Goal goal) {
        String description = goal.getDescription().toLowerCase();
        if (description.contains("woodcutting")) return "Woodcutting";
        if (description.contains("mining")) return "Mining";
        if (description.contains("fishing")) return "Fishing";
        if (description.contains("attack")) return "Attack";
        if (description.contains("magic")) return "Magic";
        if (description.contains("cooking")) return "Cooking";
        // Add more skills as needed
        return "skills";
    }

    private void updateEmotionalState() {
        emotionalState.update(bot);
    }

    private void updateBoredomAndRestlessness() {
        long timeSinceStateChange = System.currentTimeMillis() - lastStateChange;
        long timeSinceDecision = System.currentTimeMillis() - lastMajorDecision;

        // Increase boredom if doing same thing too long
        if (timeSinceStateChange > 300000) { // 5 minutes
            boredomLevel = Math.min(100, boredomLevel + 1);
        }

        // Increase restlessness if no major decisions
        if (timeSinceDecision > 600000) { // 10 minutes
            restlessness = Math.min(100, restlessness + 2);
        }

        // Personality affects boredom tolerance
        if (personality.getEfficiencyMultiplier() > 1.0) {
            boredomLevel = Math.max(0, boredomLevel - 1); // Efficient bots don't get bored easily
        }

        if (personality.getChatFrequency() > 60) {
            restlessness += 1; // Social bots get restless without interaction
        }
        
        // Goals reduce boredom
        Goal currentGoal = goalStack.getCurrentGoal();
        if (currentGoal != null) {
            boredomLevel = Math.max(0, boredomLevel - 2); // Having goals reduces boredom
        }
    }

    /**
     * NEW: Goal-driven decision making instead of hardcoded options
     */
    private void makeGoalDrivenDecision(long currentTime) {
        // High-priority decision triggers
        if (shouldMakeMajorDecision()) {
            makeMajorGoalDecision();
            lastMajorDecision = currentTime;
            return;
        }

        // Regular decision making based on current state
        switch (currentState) {
            case IDLE:
                handleIdleState();
                break;
            case PLANNING:
                handlePlanningState();
                break;
            case ACTIVITY:
                handleActivityState();
                break;
            case BANKING:
                handleBankingState();
                break;
            case SOCIAL:
                handleSocialState();
                break;
            case TRAVELING:
                handleTravelingState();
                break;
        }
    }

    private boolean shouldMakeMajorDecision() {
        // High boredom or restlessness
        if (boredomLevel > 70 || restlessness > 80) {
            return true;
        }
        
        // No current goal 
        Goal currentGoal = goalStack.getCurrentGoal();
        if (currentGoal == null) {
            return true;
        }
        
        // Goal completed or failed
        if (currentGoal.getStatus() != Goal.Status.ACTIVE) {
            return true;
        }

        // Random major decision (personality-driven)
        int randomChance = personality.getRiskTolerance() > 0.6 ? 5 : 2; // Risk-takers make more major decisions
        return Utils.random(1000) < randomChance;
    }

    /**
     * NEW: Make major decisions based on available goals
     */
    private void makeMajorGoalDecision() {
        System.out.println("[BotBrain] " + bot.getDisplayName() + " making major goal decision...");

        Goal currentGoal = goalStack.getCurrentGoal();
        
        if (currentGoal == null) {
            // No goal - the GoalStack should automatically generate one
            System.out.println("[BotBrain] " + bot.getDisplayName() + " has no goals, waiting for goal generation...");
            enterState(BotState.PLANNING);
        } else if (currentGoal.getStatus() == Goal.Status.COMPLETED) {
            // Goal completed!
            goalStack.completeCurrentGoal();
            System.out.println("[BotBrain] " + bot.getDisplayName() + " completed goal: " + currentGoal.getDescription());
            
            // Look for next goal
            Goal nextGoal = goalStack.getCurrentGoal();
            if (nextGoal != null) {
                System.out.println("[BotBrain] " + bot.getDisplayName() + " starting new goal: " + nextGoal.getDescription());
                enterState(BotState.PLANNING);
            }
        } else if (currentGoal.getStatus() == Goal.Status.FAILED) {
            // Goal failed
            goalStack.failCurrentGoal("Unable to complete");
            System.out.println("[BotBrain] " + bot.getDisplayName() + " failed goal: " + currentGoal.getDescription());
            enterState(BotState.PLANNING);
        } else {
            // Continue with current goal but maybe change approach
            if (boredomLevel > 70) {
                System.out.println("[BotBrain] " + bot.getDisplayName() + " taking a break from: " + currentGoal.getDescription());
                enterState(BotState.SOCIAL);
            }
        }

        // Reset decision factors
        boredomLevel = Math.max(0, boredomLevel - 30);
        restlessness = Math.max(0, restlessness - 40);
    }

    // State handlers - now goal-aware
    private void handleIdleState() {
        Goal currentGoal = goalStack.getCurrentGoal();
        if (currentGoal != null) {
            enterState(BotState.PLANNING);
        } else if (Utils.random(100) < 30) {
            enterState(BotState.PLANNING);
        }
    }

    private void handlePlanningState() {
        Goal currentGoal = goalStack.getCurrentGoal();
        if (currentGoal != null) {
            // Start working on the goal
            enterState(BotState.ACTIVITY);
        } else if (Utils.random(100) < 50) {
            enterState(BotState.IDLE);
        }
    }

    private void handleActivityState() {
        Goal currentGoal = goalStack.getCurrentGoal();
        if (currentGoal == null) {
            enterState(BotState.IDLE);
        } else if (boredomLevel > 60 && Utils.random(100) < 10) {
            enterState(BotState.SOCIAL); // Take a social break
        } else if (Utils.random(100) < 5) {
            enterState(BotState.BANKING); // Occasional banking
        }
    }

    private void handleBankingState() {
        if (Utils.random(100) < 70) {
            enterState(BotState.ACTIVITY); // Return to activity
        }
    }

    private void handleSocialState() {
        if (Utils.random(100) < 40) {
            enterState(BotState.ACTIVITY); // Back to work
        }
    }

    private void handleTravelingState() {
        if (Utils.random(100) < 60) {
            enterState(BotState.ACTIVITY); // Arrived at destination
        }
    }

    private void enterState(BotState newState) {
        if (newState != currentState) {
            previousState = currentState;
            currentState = newState;
            lastStateChange = System.currentTimeMillis();

            System.out.println("[BotBrain] " + bot.getDisplayName() + " entered state: " + newState);
        }
    }

    // Getters for integrated systems
    public PersonalityProfile getPersonality() { return personality; }
    public EmotionalState getEmotionalState() { return emotionalState; }
    public MemorySystem getMemory() { return memory; }
    public GoalStack getGoalStack() { return goalStack; }
    public BotState getCurrentState() { return currentState; }
    public String getCurrentActivity() { return currentActivity; }
    
    /**
     * Get current goal for external monitoring
     */
    public Goal getCurrentGoal() {
        return goalStack.getCurrentGoal();
    }
    /**
     * Execute actual actions based on current goal and state
     */
    private void executeGoalActions(Goal goal) {
        if (goal == null) return;
        int currentX = bot.getX();
        int currentY = bot.getY();
        if (Utils.random(200) < 1) {
            System.out.println("[MOVEMENT-TEST] " + bot.getDisplayName() + " at " + currentX + "," + currentY + " state=" + currentState + " goal=" + goal.getDescription());
        }
        switch (currentState) {
            case ACTIVITY:
                if (Utils.random(100) < 5) announceActivity("Working on: " + goal.getDescription(), true);
                executeSmartGoalMovement(goal, currentX, currentY);
                break;
            case BANKING:
            case TRAVELING:
                executeSmartGoalMovement(goal, currentX, currentY);
                break;
            case PLANNING:
                if (Utils.random(100) < 30) {
                    randomSmartWalk(currentX, currentY);
                }
                break;
            case SOCIAL:
            case IDLE:
                if (Utils.random(100) < 10) {
                    randomSmartWalk(currentX, currentY);
                }
                break;
        }
    }

    private void executeActivityActions(Goal goal, int currentX, int currentY) {
        String description = goal.getDescription().toLowerCase();
        if (description.contains("woodcutting")) {
            moveToWoodcuttingArea(currentX, currentY);
        } else if (description.contains("mining")) {
            moveToMiningArea(currentX, currentY);
        } else if (description.contains("combat") || description.contains("attack")) {
            moveToCombatArea(currentX, currentY);
        } else {
            purposefulWalk(currentX, currentY);
        }
    }

    private void executeBankingActions(int currentX, int currentY) {
        int bankX = 3094; int bankY = 3493;
        if (Math.abs(currentX - bankX) > 1 || Math.abs(currentY - bankY) > 1) {
            moveTowards(bankX, bankY, currentX, currentY);
        } else {
            goalStack.updateCurrentGoal("banking items", 0.005);
        }
    }

    private void executeTravelActions(Goal goal, int currentX, int currentY) {
        String description = goal.getDescription().toLowerCase();
        int destX = currentX, destY = currentY;
        if (description.contains("woodcutting")) { destX = 3096; destY = 3468; }
        else if (description.contains("mining")) { destX = 3110; destY = 3450; }
        else if (description.contains("combat")) { destX = 3120; destY = 3520; }
        moveTowards(destX, destY, currentX, currentY);
    }

    private void moveToWoodcuttingArea(int currentX, int currentY) {
        int targetX = 3096 + Utils.random(-3, 4);
        int targetY = 3468 + Utils.random(-3, 4);
        moveTowards(targetX, targetY, currentX, currentY);
    }

    private void moveToMiningArea(int currentX, int currentY) {
        int targetX = 3110 + Utils.random(-2, 3);
        int targetY = 3450 + Utils.random(-2, 3);
        moveTowards(targetX, targetY, currentX, currentY);
    }

    private void moveToCombatArea(int currentX, int currentY) {
        int targetX = 3120 + Utils.random(-5, 6);
        int targetY = 3520 + Utils.random(-5, 6);
        moveTowards(targetX, targetY, currentX, currentY);
    }

    private void randomWalk(int currentX, int currentY) {
        int newX = currentX + Utils.random(-3, 4);
        int newY = currentY + Utils.random(-3, 4);
        if (newX == currentX && newY == currentY) return;
        bot.addWalkSteps(newX, newY, 5, true);
    }

    private void purposefulWalk(int currentX, int currentY) {
        int newX = currentX + Utils.random(-6, 7);
        int newY = currentY + Utils.random(-6, 7);
        if (newX == currentX && newY == currentY) return;
        bot.addWalkSteps(newX, newY, 10, true);
    }

    private boolean scanForTrees(int x, int y) {
        int[][] trees = {{3095,3468},{3096,3468},{3097,3468},{3098,3468}};
        for (int[] tree : trees) {
            if (Math.abs(x - tree[0]) + Math.abs(y - tree[1]) <= 2) {
                announceActivity("Found a tree! Chopping...", false);
                goalStack.updateCurrentGoal("chopping trees", 0.002);
                if (Utils.random(100) < 15) announceActivity("*chop chop*", true);
                return true;
            }
        }
        announceActivity("Looking for trees...", false);
        return false;
    }
    private void announceActivity(String message, boolean publicChat) {
        try {
            if (publicChat && Utils.random(100) < 60) {
                say(message); // ForceTalk balloon visible to nearby real players
            } else if (Utils.random(100) < 40) {
                System.out.println("[BotThought] " + bot.getDisplayName() + " thinks: " + message);
            }
        } catch (Exception e) {
            System.out.println("[BotChat] " + bot.getDisplayName() + ": " + message);
        }
    }
    private void executeRealActivity(Goal goal, int currentX, int currentY) {
        String desc = goal.getDescription().toLowerCase();
        if (desc.contains("armor") || desc.contains("rune")) {
            walkToGeneralStore(currentX, currentY);
        } else if (desc.contains("woodcutting")) {
            walkToTreesAndChop(currentX, currentY);
        } else if (desc.contains("bank") || desc.contains("money")) {
            walkToBankAndBank(currentX, currentY);
        } else {
            randomWalk(currentX, currentY);
        }
    }

    private void walkToGeneralStore(int currentX, int currentY) {
        int shopX = 3081, shopY = 3510;
        if (Math.abs(currentX - shopX) > 2 || Math.abs(currentY - shopY) > 2) {
            moveTowards(shopX, shopY, currentX, currentY);
        } else {
            goalStack.updateCurrentGoal("shopping for armor", 0.005);
        }
    }

    private void walkToTreesAndChop(int currentX, int currentY) {
        int treeX = 3096, treeY = 3468;
        if (Math.abs(currentX - treeX) > 3 || Math.abs(currentY - treeY) > 3) {
            moveTowards(treeX, treeY, currentX, currentY);
        } else {
            goalStack.updateCurrentGoal("chopping trees", 0.002);
            if (Utils.random(100) < 20) bot.setNextAnimation(new Animation(879));
        }
    }

    private void walkToBankAndBank(int currentX, int currentY) {
        int bankX = 3094, bankY = 3493;
        if (Math.abs(currentX - bankX) > 2 || Math.abs(currentY - bankY) > 2) {
            moveTowards(bankX, bankY, currentX, currentY);
        } else {
            goalStack.updateCurrentGoal("organizing bank", 0.01);
        }
    }
    private void moveTowards(int targetX, int targetY, int currentX, int currentY) {
        // Queue a real path toward the destination, capped to keep the bot
        // reactive. check=true so we don't noclip through walls.
        try {
            bot.addWalkSteps(targetX, targetY, 25, true);
        } catch (Exception e) {
            bot.setNextWorldTile(new WorldTile(currentX + Utils.random(-1, 2), currentY + Utils.random(-1, 2), 0));
        }
    }

    private void executeSmartGoalMovement(Goal goal, int currentX, int currentY) {
        int[] targetCoords = WorldKnowledge.getBestLocationForGoal(goal, currentX, currentY);
        if (targetCoords == null) {
            randomSmartWalk(currentX, currentY);
            return;
        }

        int targetX = targetCoords[0];
        int targetY = targetCoords[1];

        if (Math.abs(currentX - targetX) < 5 && Math.abs(currentY - targetY) < 5) {
            performGoalActivity(goal, currentX, currentY);
            return;
        }

        if (WorldKnowledge.isWalkingDistance(currentX, currentY, targetX, targetY)) {
            intelligentWalkTo(targetX, targetY, currentX, currentY);
        } else {
            attemptTeleportTo(targetX, targetY, currentX, currentY);
        }
    }

    private void intelligentWalkTo(int targetX, int targetY, int currentX, int currentY) {
        // Queue a batch of up to MAX_BATCH steps toward the destination. With
        // check=true, addWalkSteps stops at the first blocked tile so bots no
        // longer noclip through walls. Steps queued before the blockage still
        // run, so the bot walks until it hits an obstacle. Real A* pathfinding
        // is the next step (RouteFinder) so bots route around blockers.
        final int MAX_BATCH = 25;
        boolean moved = bot.addWalkSteps(targetX, targetY, MAX_BATCH, true);
        if (Utils.random(50) < 1) {
            System.out.println("[INTELLIGENT-WALK] " + bot.getDisplayName()
                + " queued path " + currentX + "," + currentY
                + " -> " + targetX + "," + targetY + " (ok=" + moved + ")");
        }
    }

    private void attemptTeleportTo(int targetX, int targetY, int currentX, int currentY) {
        int[] bestTeleport = WorldKnowledge.findNearestLocation(WorldKnowledge.TELEPORT_SPOTS, targetX, targetY);

        if (bestTeleport != null) {
            System.out.println("[TELEPORT] " + bot.getDisplayName() + " -> " + bestTeleport[0] + "," + bestTeleport[1]);
            bot.setNextAnimation(new Animation(714));
            bot.setNextWorldTile(new WorldTile(bestTeleport[0], bestTeleport[1], 0));
        } else {
            intelligentWalkTo(targetX, targetY, currentX, currentY);
        }
    }

    private void performGoalActivity(Goal goal, int x, int y) {
        String desc = goal.getDescription().toLowerCase();
        boolean log = Utils.random(100) < 5;

        if (desc.contains("woodcutting") || desc.contains("logs")) {
            bot.setNextAnimation(new Animation(879));
            goalStack.updateCurrentGoal("chopping wood", 0.003);
            if (log) System.out.println("[ACTIVITY] " + bot.getDisplayName() + " chopping wood");
        } else if (desc.contains("mining") || desc.contains("ore")) {
            bot.setNextAnimation(new Animation(625));
            goalStack.updateCurrentGoal("mining ore", 0.002);
            if (log) System.out.println("[ACTIVITY] " + bot.getDisplayName() + " mining ore");
        } else if (desc.contains("fishing") || desc.contains("fish")) {
            bot.setNextAnimation(new Animation(623));
            goalStack.updateCurrentGoal("catching fish", 0.004);
            if (log) System.out.println("[ACTIVITY] " + bot.getDisplayName() + " fishing");
        } else if (desc.contains("combat") || desc.contains("train")) {
            int[] combatAnims = {422, 423, 424, 451};
            bot.setNextAnimation(new Animation(combatAnims[Utils.random(combatAnims.length)]));
            goalStack.updateCurrentGoal("training combat", 0.005);
            if (log) System.out.println("[ACTIVITY] " + bot.getDisplayName() + " training combat");
        } else if (desc.contains("bank")) {
            goalStack.updateCurrentGoal("organizing bank", 0.01);
            if (log) System.out.println("[ACTIVITY] " + bot.getDisplayName() + " banking");
        } else {
            randomSmartWalk(x, y);
        }
    }

    private void randomSmartWalk(int currentX, int currentY) {
        int newX = currentX + Utils.random(-5, 6);
        int newY = currentY + Utils.random(-5, 6);
        if (newX == currentX && newY == currentY) return;
        bot.addWalkSteps(newX, newY, 8, true);
    }

    /**
     * Make the bot say something visible to nearby real players.
     *
     * Uses ForceTalk: a chat balloon over the bot's head that real players'
     * LocalPlayerUpdate streams already include automatically when iterating
     * visible players. This works for headless bots because we're not sending
     * packets ourselves - we're just setting a field on the bot, and each real
     * player's update loop picks it up on its own packet stream.
     *
     * For real chat-box public messages we'd iterate World.getPlayers(), find
     * those within ~14 tiles, and call player.getPackets().sendPublicMessage(
     * bot, new PublicChatMessage(text, 0)) on each. ForceTalk is the cheaper
     * first step - balloon shows above the bot's head in-game.
     */
    public void say(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            bot.setNextForceTalk(new ForceTalk(text));
            System.out.println("[BotChat] " + bot.getDisplayName() + ": " + text);
        } catch (Throwable t) {
            System.err.println("[BotChat] failed for " + bot.getDisplayName() + ": " + t);
        }
    }
}
