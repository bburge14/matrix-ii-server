package com.rs.bot;

import java.util.*;
import com.rs.utils.Utils;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.Graphics;
import com.rs.game.player.PublicChatMessage;
import com.rs.game.player.Player;
import com.rs.game.ForceTalk;
import com.rs.game.Animation;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.bot.ai.BotPathing;
import com.rs.bot.ai.EnvironmentScanner;
import com.rs.bot.ai.WorldKnowledge;
import com.rs.bot.ai.Goal;
import com.rs.bot.ai.GoalStack;
import com.rs.game.player.actions.Fishing;
import com.rs.game.player.actions.Woodcutting;
import com.rs.game.player.actions.mining.Mining;

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
        // Skip while a prior teleport is still casting (lock = isLocked()) or the
        // walkSteps queue still has tiles to consume. Both cases mean the bot is
        // mid-action and re-queueing now would either stomp the cast or pile up
        // micro-steps on top of an in-flight path.
        if (bot.isLocked()) return;
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
     * Update the goal's "current step" string for logging/HUD only.
     *
     * Real progress is no longer faked here - completion is determined
     * by Goal.isCompleted(bot) in GoalStack.getCurrentGoal(), which
     * reads actual game state (skill levels, items in equipment/
     * inventory/bank, total wealth). Bots only "finish" a goal when the
     * world says they have, not when a 0.001 timer says so.
     */
    private void simulateGoalProgress(Goal goal) {
        String activity;
        switch (goal.getCategory()) {
            case SKILL:    activity = "training " + extractSkillFromGoal(goal); break;
            case COMBAT:   activity = "combat training"; break;
            case ECONOMIC: activity = "making money"; break;
            case QUEST:    activity = "doing quest"; break;
            default:       activity = "working on goal";
        }
        if (Utils.random(100) < 20) activity += " (focused)";
        goal.setCurrentStep(activity);
        currentActivity = activity;
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
        // Use the engine's RouteFinder so bots route around walls/objects
        // instead of jamming into them with check=true addWalkSteps. If no
        // route exists (e.g., target is inside a wall, bot is trapped), we
        // wiggle to break out of the dead-end.
        boolean ok = BotPathing.walkTo(bot, targetX, targetY);
        if (!ok) BotPathing.wiggle(bot, 4);
        if (Utils.random(50) < 1) {
            System.out.println("[INTELLIGENT-WALK] " + bot.getDisplayName()
                + " " + currentX + "," + currentY + " -> " + targetX + "," + targetY
                + " (route=" + ok + ")");
        }
    }

    private void attemptTeleportTo(int targetX, int targetY, int currentX, int currentY) {
        // Don't queue a new teleport while one is already casting. The bot's
        // executeCurrentGoalActions guard already skips while walkSteps is
        // non-empty; we add isLocked() to cover the cast window before the
        // destination tile is applied.
        if (bot.isLocked()) return;

        int[] bestTeleport = WorldKnowledge.findNearestLocation(WorldKnowledge.TELEPORT_SPOTS, targetX, targetY);
        if (bestTeleport == null) {
            intelligentWalkTo(targetX, targetY, currentX, currentY);
            return;
        }

        final int destX = bestTeleport[0];
        final int destY = bestTeleport[1];
        int[] anim = pickTeleportAnimation(destX, destY);
        final int upEmote = anim[0];
        final int upGfx = anim[1];
        final int downEmote = anim[2];
        final int downGfx = anim[3];

        // Cast: animation + graphics, no movement yet. Lock for 4 ticks
        // (3 cast + 1 settle) so the brain doesn't fire another teleport mid-cast.
        bot.setNextAnimation(new Animation(upEmote));
        if (upGfx != -1) bot.setNextGraphics(new Graphics(upGfx));
        bot.lock(4);
        System.out.println("[TELEPORT] " + bot.getDisplayName() + " casting -> " + destX + "," + destY);

        // Apply destination after 3 game ticks (matches real spell cast time)
        WorldTasksManager.schedule(new WorldTask() {
            @Override
            public void run() {
                if (bot.hasFinished()) return;
                bot.setNextWorldTile(new WorldTile(destX, destY, 0));
                if (downEmote != -1) bot.setNextAnimation(new Animation(downEmote));
                if (downGfx != -1) bot.setNextGraphics(new Graphics(downGfx));
            }
        }, 3);
    }

    /**
     * Pick the right teleport animation/graphics by destination. Returns
     * [upEmote, upGfx, downEmote, downGfx] - downEmote/downGfx may be -1 to skip.
     *
     * Animation IDs cribbed from Magic.java's spellbook teleports:
     *   1979 / 1681 - standard spellbook (Lumby/Varrock/Falador/Ardougne/Camelot)
     *   8939 / 1576 - jewelry & object teleport (glory/ring of dueling)
     *   9606 / 1685 - Camelot-style (older animation, used as fallback variety)
     */
    private int[] pickTeleportAnimation(int destX, int destY) {
        // Glory teleports - Edgeville, Karamja, Draynor, Al-Kharid coords
        if ((destX == 3087 && destY == 3496) || (destX == 2918 && destY == 3176)
                || (destX == 3105 && destY == 3251) || (destX == 3293 && destY == 3163)) {
            return new int[] { 8939, 1576, 8941, 1577 };
        }
        // Camelot - the Seers/Catherby area teleport
        if (destX == 2757 && destY == 3477) {
            return new int[] { 9606, 1685, -1, -1 };
        }
        // Default: standard spellbook teleport
        return new int[] { 1979, 1681, -1, -1 };
    }

    private void performGoalActivity(Goal goal, int x, int y) {
        // If an action is already running (woodcutting, mining, fishing, ...)
        // let it tick and gain XP. We don't stomp a running action - the
        // ActionManager already handles continuation and we want the bot
        // to commit to a tree/rock until depletion.
        if (bot.getActionManager() != null && bot.getActionManager().getAction() != null) {
            return;
        }

        // Pick a concrete training method for this goal based on bot stats.
        // The method tells us where to go, what kind of target to look for
        // and which Action class to start. As the bot levels up, the picker
        // automatically promotes them to the next-tier method.
        com.rs.bot.ai.TrainingMethods.Method method =
            com.rs.bot.ai.TrainingMethods.bestMethodFor(goal, bot);
        if (method != null) {
            executeTrainingMethod(goal, method);
            return;
        }

        // No training method matched the goal type - fall back to legacy
        // description-keyword routing for anything we haven't planned yet
        // (banking, equipment shopping, quests, ...).
        String desc = goal.getDescription().toLowerCase();
        if (desc.contains("bank")) {
            goalStack.updateCurrentGoal("organizing bank", 0);
        } else {
            randomSmartWalk(x, y);
        }
    }

    /**
     * Walk the bot to the method's location and start the matching action.
     * If the bot isn't yet at the location, route there. Once on-site,
     * scan for the right tree/rock/spot type (filtered) and start.
     */
    private void executeTrainingMethod(Goal goal, com.rs.bot.ai.TrainingMethods.Method method) {
        // Walk-out phase - get to the training area first.
        if (method.location != null) {
            int dx = bot.getX() - method.location.getX();
            int dy = bot.getY() - method.location.getY();
            if (dx * dx + dy * dy > 64) { // > ~8 tiles
                if (com.rs.bot.ai.WorldKnowledge.isWalkingDistance(
                        bot.getX(), bot.getY(),
                        method.location.getX(), method.location.getY())) {
                    BotPathing.walkTo(bot, method.location.getX(), method.location.getY());
                } else {
                    attemptTeleportTo(method.location.getX(), method.location.getY(),
                                      bot.getX(), bot.getY());
                }
                return;
            }
        }
        goal.setCurrentStep(method.description);
        switch (method.kind) {
            case WOODCUTTING: tryStartWoodcutting(method); break;
            case MINING:      tryStartMining(method); break;
            case FISHING:     tryStartFishing(method); break;
            case COMBAT:      tryStartCombat(method); break;
        }
    }

    private void tryStartWoodcutting(com.rs.bot.ai.TrainingMethods.Method method) {
        EnvironmentScanner.TreeMatch match =
            EnvironmentScanner.findNearestTree(bot, 12, method == null ? null : method.treeDef);
        if (match == null) {
            BotPathing.wiggle(bot, 5);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), match.object)) {
            BotPathing.walkToObject(bot, match.object);
            return;
        }
        bot.getActionManager().setAction(new Woodcutting(match.object, match.definition));
        if (Utils.random(100) < 30) say(woodcuttingChatter());
    }

    private void tryStartMining(com.rs.bot.ai.TrainingMethods.Method method) {
        EnvironmentScanner.RockMatch match =
            EnvironmentScanner.findNearestRock(bot, 12, method == null ? null : method.rockDef);
        if (match == null) {
            BotPathing.wiggle(bot, 5);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), match.object)) {
            BotPathing.walkToObject(bot, match.object);
            return;
        }
        bot.getActionManager().setAction(new Mining(match.object, match.definition));
        if (Utils.random(100) < 30) say(miningChatter());
    }

    private void tryStartFishing(com.rs.bot.ai.TrainingMethods.Method method) {
        EnvironmentScanner.FishMatch match =
            EnvironmentScanner.findNearestFishingSpot(bot, 14, method == null ? null : method.fishDef);
        if (match == null) {
            BotPathing.wiggle(bot, 5);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), match.npc.getX(), match.npc.getY())) {
            BotPathing.walkToEntity(bot, match.npc);
            return;
        }
        bot.getActionManager().setAction(new Fishing(match.definition, match.npc));
        if (Utils.random(100) < 30) say(fishingChatter());
    }

    private void tryStartCombat(com.rs.bot.ai.TrainingMethods.Method method) {
        // Combat itself isn't wired yet (no PlayerCombat hook for bots).
        // For now the bot stands in the right training area until that
        // pass. Once combat is wired this method picks the right NPC
        // and starts attacking. Just log the intent for now.
        if (Utils.random(100) < 5) {
            System.out.println("[COMBAT-STUB] " + bot.getDisplayName()
                + " arrived at " + method.description + " - awaiting combat wiring.");
        }
    }

    private boolean isAdjacent(int x, int y, com.rs.game.WorldObject o) {
        return isAdjacent(x, y, o.getX(), o.getY());
    }

    private boolean isAdjacent(int x, int y, int tx, int ty) {
        return Math.abs(x - tx) <= 1 && Math.abs(y - ty) <= 1;
    }

    private static final String[] WOODCUTTING_CHATTER = {
        "anyone selling axes?", "yew logs are getting boring", "lvl up!",
        "this tree is taking forever", "teak when?", "I love woodcutting"
    };
    private static final String[] MINING_CHATTER = {
        "buying gold ore", "nooo my pickaxe broke", "rune rock spawn pls",
        "anyone here boosting?", "mining is therapeutic ngl"
    };
    private static final String[] FISHING_CHATTER = {
        "wts raw lobster", "barbarian fishing best xp", "sharks sharks sharks",
        "anyone got an extra harpoon?", "fishing > mining"
    };
    private String woodcuttingChatter() { return WOODCUTTING_CHATTER[Utils.random(WOODCUTTING_CHATTER.length)]; }
    private String miningChatter() { return MINING_CHATTER[Utils.random(MINING_CHATTER.length)]; }
    private String fishingChatter() { return FISHING_CHATTER[Utils.random(FISHING_CHATTER.length)]; }

    private void randomSmartWalk(int currentX, int currentY) {
        int newX = currentX + Utils.random(-5, 6);
        int newY = currentY + Utils.random(-5, 6);
        if (newX == currentX && newY == currentY) return;
        bot.addWalkSteps(newX, newY, 8, true);
    }

    /**
     * Make the bot say something visible to nearby real players, both as a
     * chat balloon over the bot's head AND in their chat box.
     *
     * Two independent pieces:
     *   1. setNextForceTalk - real players' LocalPlayerUpdate already includes
     *      the ForceTalk mask when iterating visible players, so the balloon
     *      shows automatically with no packet plumbing on our side.
     *   2. broadcastPublicMessage - bots have no client, so they can't send
     *      packets themselves. Instead we iterate nearby real players and call
     *      sendPublicMessage on THEIR packet stream with the bot as the source.
     */
    public void say(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            bot.setNextForceTalk(new ForceTalk(text));
            broadcastPublicMessage(text);
        } catch (Throwable t) {
            System.err.println("[BotChat] failed for " + bot.getDisplayName() + ": " + t);
        }
    }

    private void broadcastPublicMessage(String text) {
        PublicChatMessage msg = new PublicChatMessage(text, 0);
        int botX = bot.getX(), botY = bot.getY(), botPlane = bot.getPlane();
        for (Player real : World.getPlayers()) {
            if (real == null) continue;
            if (real instanceof AIPlayer) continue; // bots can't render chat
            if (!real.hasStarted() || real.hasFinished()) continue;
            if (real.getPlane() != botPlane) continue;
            int dx = real.getX() - botX;
            int dy = real.getY() - botY;
            if (dx * dx + dy * dy > 196) continue; // ~14 tile radius
            try {
                real.getPackets().sendPublicMessage(bot, msg);
            } catch (Throwable ignore) {
                // real player's session may have died mid-broadcast; skip
            }
        }
    }
}
