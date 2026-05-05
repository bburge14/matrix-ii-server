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
import com.rs.game.player.actions.PlayerCombatNew;
import com.rs.game.player.actions.Woodcutting;
import com.rs.game.player.actions.mining.Mining;
import com.rs.game.npc.NPC;

public class BotBrain {
    private AIPlayer bot;
    private PersonalityProfile personality;
    private EmotionalState emotionalState;
    private MemorySystem memory;
    private GoalStack goalStack;
    private BurnoutSystem burnout;

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
    /** Wallclock at which this bot is allowed to teleport again. Stops the
     *  brain from spamming back-to-back teleports when the destination is
     *  far from the nearest tele spot - the bot is forced to walk in
     *  between, just like a real player out of runes. */
    private long teleportCooldownUntil;
    /** True when the bot is on a bank trip (full inventory diversion). */
    private boolean bankingMode;
    /** Where to walk back to once the bank trip is done. */
    private WorldTile bankReturnTile;
    /** Wallclock at which we last swept the GE for completed offers. */
    private long lastGECollectionTick;
    private String currentActivity;
    /** Last diagnostic message - used by ::botinfo for debugging. */
    private String lastDiagnostic = "";
    /** Last training method picked - used by ::botinfo for debugging. */
    private com.rs.bot.ai.TrainingMethods.Method lastMethod;

    public String getLastDiagnostic() { return lastDiagnostic; }
    public com.rs.bot.ai.TrainingMethods.Method getLastMethod() { return lastMethod; }
    /** ::botforce hook - drive a specific method through one tick. */
    public void forceTrainingMethod(com.rs.bot.ai.TrainingMethods.Method m) {
        Goal g = getCurrentGoal();
        if (g == null) {
            lastDiagnostic = "force: no current goal to attach method to";
            return;
        }
        executeTrainingMethod(g, m);
    }

    public BotBrain(AIPlayer bot) {
        this.bot = bot;
        this.personality = new PersonalityProfile();
        this.emotionalState = new EmotionalState();
        this.memory = new MemorySystem();
        this.goalStack = new GoalStack(bot); // Fixed: Pass bot to constructor
        this.burnout = new BurnoutSystem();

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
            // Burnout meter ticks on the same cadence. If it fires, force-
            // abandon the current grind and inject a vacation goal so the
            // next selectNextGoal picks something fresh.
            if (burnout != null && burnout.tick(bot, goalStack)) {
                triggerBurnoutVacation();
            }
            lastGoalCheck = currentTime;
        }

        // GE sweep: every 60s, pull settled offers back. Cheap if there's
        // nothing to collect, prevents items sitting forever in the GE
        // even when the bot doesn't bank.
        if (currentTime - lastGECollectionTick > 60_000) {
            try {
                com.rs.bot.ai.BotTrading.collectCompletedOffers(bot);
            } catch (Throwable t) {
                // ignore - non-critical
            }
            lastGECollectionTick = currentTime;
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
        if (bot.isLocked()) return;
        if (!bot.getWalkSteps().isEmpty()) return;

        // Bank trip diversion: if inventory is near-full or we're already
        // on a bank run, drive that to completion before doing anything
        // else. Banking deposits everything then walks the bot back to its
        // training spot, after which performGoalActivity resumes normally.
        if (bankingMode || shouldStartBanking(currentGoal)) {
            runBankingTrip(currentGoal);
            return;
        }

        executeGoalActions(currentGoal);
    }

    private boolean shouldStartBanking(Goal goal) {
        try {
            com.rs.game.player.Inventory inv = bot.getInventory();
            if (inv == null) return false;
            // 3 free slots is the threshold - leaves room for one more
            // skilling drop without immediately re-banking.
            if (inv.getFreeSlots() > 3) return false;
        } catch (Throwable t) {
            return false;
        }
        Goal.GoalCategory cat = goal.getCategory();
        // Only divert for goals where banking actually closes the loop.
        return cat == Goal.GoalCategory.SKILL
            || cat == Goal.GoalCategory.ECONOMIC
            || cat == Goal.GoalCategory.COMBAT;
    }

    private void runBankingTrip(Goal goal) {
        if (!bankingMode) {
            // Entering bank-trip mode. Stash the tile we're at so we can
            // walk back after dumping the inventory.
            bankingMode = true;
            bankReturnTile = new WorldTile(bot.getX(), bot.getY(), bot.getPlane());
            goal.setCurrentStep("walking to bank");
            if (Utils.random(100) < 30) say("inv full, going to bank");
        }

        int[] bank = WorldKnowledge.findNearestLocation(WorldKnowledge.BANKS, bot.getX(), bot.getY());
        if (bank == null) {
            // No banks indexed - bail.
            bankingMode = false;
            return;
        }
        int dx = bot.getX() - bank[0];
        int dy = bot.getY() - bank[1];
        boolean atBank = (dx * dx + dy * dy) <= 9; // within 3 tiles

        if (!atBank) {
            // Walk or teleport to the bank.
            if (WorldKnowledge.isWalkingDistance(bot.getX(), bot.getY(), bank[0], bank[1])) {
                BotPathing.walkTo(bot, bank[0], bank[1]);
            } else {
                attemptTeleportTo(bank[0], bank[1], bot.getX(), bot.getY());
            }
            return;
        }

        // We're at the bank.
        // Step 0: Try the in-world skill shops first - converts raw drops
        // (logs/fish/ore/hide/bones) to coins immediately without waiting
        // on GE settlement. 80% of inventory routes through here, the rest
        // falls through to GE for high-value items.
        try {
            int sold = sellRawDropsToSkillShops(bot);
            if (sold > 0 && Utils.random(100) < 30) {
                say("sold " + sold + " items to NPC shops");
            }
        } catch (Throwable t) {
            System.err.println("[SHOP-ERROR] " + bot.getDisplayName() + ": " + t);
        }
        // Step 1: place GE sell offers for tradeable resources (logs, ore,
        // raw fish) so they actually convert into coins on the market
        // instead of piling up in the bank as raw stock.
        try {
            int placed = com.rs.bot.ai.BotTrading.sellInventoryOnGE(bot);
            if (placed > 0 && Utils.random(100) < 40) {
                say("listed " + placed + " offers on GE");
            }
        } catch (Throwable t) {
            System.err.println("[GE-ERROR] sell for " + bot.getDisplayName() + ": " + t);
        }
        // Step 2: dump whatever's left into the bank. Tools (axes/picks)
        // stay safely in equipment slots so the bot can resume skilling.
        try {
            bot.getBank().depositAllInventory(false);
            if (Utils.random(100) < 50) say(bankChatter());
        } catch (Throwable t) {
            System.err.println("[BANK-ERROR] " + bot.getDisplayName() + ": " + t);
        }
        // Step 3: pull any settled GE offers back. Coins/leftover items
        // come straight to inventory; if it overflows, GE handles it.
        try {
            com.rs.bot.ai.BotTrading.collectCompletedOffers(bot);
        } catch (Throwable t) {
            System.err.println("[GE-ERROR] collect for " + bot.getDisplayName() + ": " + t);
        }
        bankingMode = false;
        goal.setCurrentStep("returning from bank");
        // Walk back to where we left off skilling. If we can't (lost the
        // tile somehow), the next tick will pick a fresh training method.
        if (bankReturnTile != null) {
            BotPathing.walkTo(bot, bankReturnTile.getX(), bankReturnTile.getY());
        }
    }

    private static final String[] BANK_CHATTER = {
        "another inv banked", "wts logs (after bank)", "back to it",
        "deposit -> chop -> deposit, the loop", "okay refreshed"
    };
    private String bankChatter() { return BANK_CHATTER[Utils.random(BANK_CHATTER.length)]; }

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

    /**
     * The bot has been grinding too long and the burnout system fired. Drop
     * the current goal regardless of commitment, and inject a "vacation"
     * goal so the next selection picks something fresh.
     */
    private void triggerBurnoutVacation() {
        Goal cur = goalStack.getCurrentGoal();
        String fromDesc = cur == null ? "(idle)" : cur.getDescription();
        goalStack.forceAbandon("burnout - taking a break");
        com.rs.bot.ai.GoalType vacation = BurnoutSystem.pickVacationGoal();
        Goal vacationGoal = goalStack.injectVacation(vacation, bot);
        String toDesc = vacationGoal == null ? vacation.getDescription() : vacationGoal.getDescription();
        System.out.println("[BURNOUT] " + bot.getDisplayName()
            + " is sick of '" + fromDesc + "' - going to do '" + toDesc + "' instead.");
        if (Utils.random(100) < 60) {
            say(burnoutChatter(fromDesc, toDesc));
        }
    }

    private static final String[] BURNOUT_CHATTER = {
        "okay screw this, I'm done with %s for a bit",
        "%s grind is killing me, switching to %s",
        "anyone wanna do %s? I need a break from grinding",
        "burnt out... going to %s",
        "if I see one more %s I'm gonna lose it"
    };

    private String burnoutChatter(String from, String to) {
        String tmpl = BURNOUT_CHATTER[Utils.random(BURNOUT_CHATTER.length)];
        return String.format(tmpl, from, to);
    }

    private boolean shouldMakeMajorDecision() {
        Goal currentGoal = goalStack.getCurrentGoal();

        // No current goal => need to plan one.
        if (currentGoal == null) return true;
        // Goal completed or failed externally => need to pick the next.
        if (currentGoal.getStatus() != Goal.Status.ACTIVE) return true;

        // Inside the commitment window the bot stays put. Real game-state
        // completion (level 99 reached, item acquired) still triggers a
        // switch via the goal completing in GoalStack - that path comes
        // through the "status != ACTIVE" branch above.
        if (!goalStack.canSwitchGoal()) return false;

        // Past commitment: small RNG chance per tick, biased by
        // personality. Risk-takers reconsider more often.
        int randomChance = personality.getRiskTolerance() > 0.6 ? 5 : 2;
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
            // No fake chopping anim - the real Woodcutting Action plays its
            // own animation when adjacent to a tree. This branch only fires
            // for the stale legacy goal-desc fallback above; bot just stands
            // there until the real action picks up.
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
        // Reset blacklist when goal changes - we're trying a new objective.
        if (goal != null && !goal.getDescription().equals(blacklistGoalDesc)) {
            goalBlacklist.clear();
            blacklistGoalDesc = goal.getDescription();
            stuckXpSnapshot = -1;
            stuckSinceMs = 0;
        }

        // Ranked Plan A/B/C list - skip any plan we've blacklisted as stuck.
        java.util.List<com.rs.bot.ai.TrainingMethods.Method> ranked =
            com.rs.bot.ai.TrainingMethods.rankedMethodsFor(goal, bot);
        com.rs.bot.ai.TrainingMethods.Method method = null;
        for (com.rs.bot.ai.TrainingMethods.Method m : ranked) {
            if (!goalBlacklist.contains(m)) { method = m; break; }
        }
        if (method != null) {
            // Check if the current method has gone stuck - if so blacklist
            // and recurse to pick the next plan.
            if (method == lastMethod && checkAndHandleStuck(method)) {
                executeSmartGoalMovement(goal, currentX, currentY);
                return;
            }
            executeTrainingMethod(goal, method);
            return;
        }
        // All methods blacklisted - clear and try again from the top
        // (something might have changed - level up, item gained, area cleared).
        if (!goalBlacklist.isEmpty() && !ranked.isEmpty()) {
            sayDebug("all plans exhausted, retrying from plan A");
            goalBlacklist.clear();
            executeTrainingMethod(goal, ranked.get(0));
            return;
        }

        int[] targetCoords = WorldKnowledge.getBestLocationForGoal(goal, currentX, currentY);
        if (targetCoords == null) {
            // No method, no WorldKnowledge target - the goal genuinely has
            // no plan. Wander a bit so the bot at least animates.
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
        // BotPathing.walkTo returns true if RouteFinder fully resolved the
        // path, false if we fell back to a clip-aware straight-line walk.
        // Either way the bot moves toward the target - we only wiggle when
        // the queue is still empty AFTER the call (true dead-end).
        boolean routed = BotPathing.walkTo(bot, targetX, targetY);
        if (!routed && bot.getWalkSteps().isEmpty()) {
            BotPathing.wiggle(bot, 4);
        }
        if (Utils.random(50) < 1) {
            System.out.println("[INTELLIGENT-WALK] " + bot.getDisplayName()
                + " " + currentX + "," + currentY + " -> " + targetX + "," + targetY
                + " (route=" + routed + ")");
        }
    }

    private void attemptTeleportTo(int targetX, int targetY, int currentX, int currentY) {
        // Already mid-cast => skip.
        if (bot.isLocked()) return;
        // Cooldown: after a teleport the bot must walk for 60s before
        // teleporting again. This stops the spam pattern where the nearest
        // tele spot is still 200 tiles from the actual destination, the bot
        // teleports there, "isn't close enough", and immediately re-teleports.
        if (System.currentTimeMillis() < teleportCooldownUntil) {
            intelligentWalkTo(targetX, targetY, currentX, currentY);
            return;
        }

        // Try jewelry teleport or spellbook teleport (jewelry preferred).
        com.rs.bot.ai.BotTeleporter.Choice tele =
            com.rs.bot.ai.BotTeleporter.pickBest(bot, targetX, targetY);
        if (tele != null && com.rs.bot.ai.BotTeleporter.cast(bot, tele)) {
            teleportCooldownUntil = System.currentTimeMillis() + 60_000;
            lastDiagnostic = "teleporting: " + tele.name + (tele.jewel != null ? " (jewelry)" : " (spell)");
            return;
        }

        int[] bestTeleport = WorldKnowledge.findNearestLocation(WorldKnowledge.TELEPORT_SPOTS, targetX, targetY);
        if (bestTeleport == null) {
            intelligentWalkTo(targetX, targetY, currentX, currentY);
            return;
        }

        // If the teleport doesn't get us meaningfully closer, just walk
        // instead. "Meaningfully" = at least 30% of the remaining distance.
        long curDist = (long) Math.hypot(currentX - targetX, currentY - targetY);
        long teleDist = (long) Math.hypot(bestTeleport[0] - targetX, bestTeleport[1] - targetY);
        if (teleDist >= curDist * 0.7) {
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

        bot.setNextAnimation(new Animation(upEmote));
        if (upGfx != -1) bot.setNextGraphics(new Graphics(upGfx));
        bot.lock(4);
        teleportCooldownUntil = System.currentTimeMillis() + 60_000L;
        System.out.println("[TELEPORT] " + bot.getDisplayName() + " casting -> " + destX + "," + destY);

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
        // Humanization: pause briefly between actions / occasionally AFK.
        // Without this, all bots tick in lockstep and look mechanical.
        if (!canActNow()) return;
        // Crowding tracker - tells TrainingMethods how many bots are on each
        // method. A bot switching from A to B decrements A and increments B.
        if (method != lastMethod) {
            com.rs.bot.ai.TrainingMethods.registerActive(method, lastMethod);
            if (method != null) com.rs.bot.SuccessTracker.onMethodPicked(method.description);
        }
        this.lastMethod = method;
        // Walk-out phase - get to the training area first.
        // Per-bot jittered target: 10 bots on the same goal don't all walk
        // to the exact same tile and step on each other. Hash by player
        // index so each bot gets a stable per-method offset within +/-4.
        if (method.location != null) {
            // Wider jitter (was 4 -> 8 tiles) so bots don't form conga lines
            // when many converge on the same training spot. Combined with
            // reaction-delay timing variance, paths and arrivals scatter.
            int[] jittered = com.rs.bot.ai.WorldKnowledge.jitteredSpot(
                bot.getIndex(), method.location.getX(), method.location.getY(), 8);
            int targetX = jittered[0];
            int targetY = jittered[1];
            int dx = bot.getX() - targetX;
            int dy = bot.getY() - targetY;
            if (dx * dx + dy * dy > 64) { // > ~8 tiles from the jittered target
                if (com.rs.bot.ai.WorldKnowledge.isWalkingDistance(
                        bot.getX(), bot.getY(), targetX, targetY)) {
                    BotPathing.walkTo(bot, targetX, targetY);
                } else {
                    attemptTeleportTo(targetX, targetY, bot.getX(), bot.getY());
                }
                return;
            }
        }
        goal.setCurrentStep(method.description);
        // 0.D: align bot's combat XP style with the goal so the right skill
        // gets trained. Goal 'skill:attack 30' -> Accurate, 'skill:strength
        // 30' -> Aggressive, 'skill:defence 30' -> Defensive.
        applyXpStyleForGoal(goal);
        // Announce the activity once per method change so players hear what
        // bots are doing instead of generic goal descriptions.
        announceMethodStart(method);
        switch (method.kind) {
            case WOODCUTTING:    tryStartWoodcutting(method); break;
            case MINING:         tryStartMining(method); break;
            case FISHING:        tryStartFishing(method); break;
            case COMBAT:         tryStartCombat(method); break;
            case THIEVING:       tryStartThieving(method); break;
            case FIREMAKING:     tryStartFiremaking(method); break;
            case COOKING:        tryStartCooking(method); break;
            case SMELTING:       tryStartSmelting(method); break;
            case CRAFTING:       tryStartCrafting(method); break;
            case PRAYER:         tryStartPrayer(method); break;
            // New skills - locations are wired, action implementations land
            // skill-by-skill in pass B. Bots walk to the right tile and the
            // stub logs "(action coming)" so audit shows it but doesn't crash.
            case HERBLORE:       tryStartHerblore(method); break;
            case AGILITY:        tryStartAgility(method); break;
            case RUNECRAFTING:   tryStartRunecrafting(method); break;
            case HUNTER:         tryStartHunter(method); break;
            case SUMMONING:      tryStartSummoning(method); break;
            case FARMING:        tryStartFarming(method); break;
            case CONSTRUCTION:   tryStartConstruction(method); break;
            case FLETCHING:      tryStartFletching(method); break;
            case DIVINATION:     tryStartDivination(method); break;
            case DUNGEONEERING:  tryStartDungeoneering(method); break;
            case SMITHING_ANVIL: tryStartSmithingAnvil(method); break;
            case MINIGAME:       tryStartMinigame(method); break;
        }
    }

    protected void tryStartPrayer(com.rs.bot.ai.TrainingMethods.Method method) {
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.PRAYER);
            if (lvl < method.minLevel) {
                lastDiagnostic = "prayer: my level " + lvl + " < required " + method.minLevel;
                return;
            }
        } catch (Throwable ignored) {}
        com.rs.game.WorldObject altar =
            EnvironmentScanner.findNearestObjectByName(bot, 12, "altar");
        if (altar == null) {
            lastDiagnostic = "prayer: no altar in 24 tiles";
            if (Utils.random(100) < 3) sayDebug("no altar nearby");
            BotPathing.wiggle(bot, 4);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), altar)) {
            BotPathing.walkToObject(bot, altar);
            lastDiagnostic = "prayer: walking to altar";
            return;
        }
        // Find highest-tier bones in inventory
        com.rs.game.player.content.BonesOnAltar.Bones targetBone = null;
        com.rs.game.item.Item targetItem = null;
        int highestXp = -1;
        for (int i = 0; i < bot.getInventory().getItemsContainerSize(); i++) {
            com.rs.game.item.Item it = bot.getInventory().getItem(i);
            if (it == null) continue;
            try {
                com.rs.game.player.content.BonesOnAltar.Bones b =
                    com.rs.game.player.content.BonesOnAltar.Bones.forId((short) it.getId());
                if (b == null) continue;
                if (b.getXP() > highestXp) {
                    highestXp = b.getXP();
                    targetBone = b;
                    targetItem = it;
                }
            } catch (Throwable ignored) {}
        }
        if (targetBone == null) {
            lastDiagnostic = "prayer: no bones in inventory";
            if (Utils.random(100) < 3) sayDebug("no bones to offer");
            goalBlacklist.add(method);
            return;
        }
        int qty = bot.getInventory().getAmountOf(targetItem.getId());
        bot.getActionManager().setAction(
            new com.rs.game.player.content.BonesOnAltar(altar, targetItem, qty));
        lastDiagnostic = "prayer: offering " + targetBone;
        if (Utils.random(100) < 30) say("offering bones");
    }

    protected void tryStartCrafting(com.rs.bot.ai.TrainingMethods.Method method) {
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.CRAFTING);
            if (lvl < method.minLevel) {
                lastDiagnostic = "craft: my level " + lvl + " < required " + method.minLevel;
                return;
            }
        } catch (Throwable ignored) {}
        // Need a chisel - try to buy if missing
        if (!bot.getInventory().containsItemToolBelt(1755)) BotEquipment.tryBuyTool(bot, 1755);
        // Find highest-tier gem the bot can cut and has in inventory
        com.rs.game.player.actions.GemCutting.Gem target = null;
        int botLvl = bot.getSkills().getLevel(com.rs.game.player.Skills.CRAFTING);
        for (com.rs.game.player.actions.GemCutting.Gem g : com.rs.game.player.actions.GemCutting.Gem.values()) {
            if (botLvl < g.getLevelRequired()) continue;
            if (!bot.getInventory().containsItem(g.getUncut(), 1)) continue;
            if (target == null || g.getLevelRequired() > target.getLevelRequired()) target = g;
        }
        if (target == null) {
            lastDiagnostic = "craft: no uncut gems in inventory";
            if (Utils.random(100) < 3) sayDebug("no uncut gems");
            goalBlacklist.add(method);
            return;
        }
        int qty = bot.getInventory().getAmountOf(target.getUncut());
        bot.getActionManager().setAction(
            new com.rs.game.player.actions.GemCutting(target, qty));
        lastDiagnostic = "craft: cutting " + target;
        if (Utils.random(100) < 30) say("cutting some gems");
    }

    protected void tryStartSmelting(com.rs.bot.ai.TrainingMethods.Method method) {
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.SMITHING);
            if (lvl < method.minLevel) {
                lastDiagnostic = "smelt: my level " + lvl + " < required " + method.minLevel;
                return;
            }
        } catch (Throwable ignored) {}
        com.rs.game.WorldObject furnace =
            EnvironmentScanner.findNearestObjectByName(bot, 12, "furnace");
        if (furnace == null) {
            lastDiagnostic = "smelt: no furnace in 24 tiles";
            if (Utils.random(100) < 3) sayDebug("no furnace nearby");
            BotPathing.wiggle(bot, 4);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), furnace)) {
            BotPathing.walkToObject(bot, furnace);
            lastDiagnostic = "smelt: walking to furnace";
            return;
        }
        // Pick highest-tier bar the bot has materials for and qualifies for
        com.rs.game.player.actions.Smelting.SmeltingBar targetBar = null;
        int botLvl = bot.getSkills().getLevel(com.rs.game.player.Skills.SMITHING);
        com.rs.game.player.actions.Smelting.SmeltingBar[] bars =
            com.rs.game.player.actions.Smelting.SmeltingBar.values();
        for (int i = bars.length - 1; i >= 0; i--) {
            com.rs.game.player.actions.Smelting.SmeltingBar bar = bars[i];
            if (botLvl < bar.getLevelRequired()) continue;
            boolean hasMats = true;
            for (com.rs.game.item.Item req : bar.getItemsRequired()) {
                if (!bot.getInventory().containsItem(req.getId(), req.getAmount())) {
                    hasMats = false; break;
                }
            }
            if (hasMats) { targetBar = bar; break; }
        }
        if (targetBar == null) {
            lastDiagnostic = "smelt: no bar mats in inventory";
            if (Utils.random(100) < 3) sayDebug("no ores to smelt");
            goalBlacklist.add(method);
            return;
        }
        bot.getActionManager().setAction(new com.rs.game.player.actions.Smelting(targetBar, furnace, 28));
        lastDiagnostic = "smelt: smelting " + targetBar;
        if (Utils.random(100) < 30) say("smelting bars");
    }

    protected void tryStartCooking(com.rs.bot.ai.TrainingMethods.Method method) {
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.COOKING);
            if (lvl < method.minLevel) {
                lastDiagnostic = "cook: my level " + lvl + " < required " + method.minLevel;
                return;
            }
        } catch (Throwable ignored) {}
        // Find a range/stove/fire near the bot's tile.
        com.rs.game.WorldObject range =
            EnvironmentScanner.findNearestObjectByName(bot, 12, "range", "stove", "fire", "firepit");
        if (range == null) {
            lastDiagnostic = "cook: no range/stove/fire in 24 tiles";
            if (Utils.random(100) < 3) sayDebug("no range nearby");
            BotPathing.wiggle(bot, 4);
            return;
        }
        // Walk adjacent if not already
        if (!isAdjacent(bot.getX(), bot.getY(), range)) {
            BotPathing.walkToObject(bot, range);
            lastDiagnostic = "cook: walking to range";
            return;
        }
        // Find first raw food in inventory the bot can cook
        com.rs.game.item.Item rawItem = null;
        com.rs.game.player.actions.Cooking.Cookables cookable = null;
        for (int i = 0; i < bot.getInventory().getItemsContainerSize(); i++) {
            com.rs.game.item.Item it = bot.getInventory().getItem(i);
            if (it == null) continue;
            try {
                com.rs.game.player.actions.Cooking.Cookables c =
                    com.rs.game.player.actions.Cooking.Cookables.forId((short) it.getId());
                if (c == null) continue;
                if (bot.getSkills().getLevel(com.rs.game.player.Skills.COOKING) < c.getLvl()) continue;
                rawItem = it;
                cookable = c;
                break;
            } catch (Throwable ignored) {}
        }
        if (rawItem == null) {
            lastDiagnostic = "cook: no raw food in inventory";
            if (Utils.random(100) < 3) sayDebug("no raw food to cook");
            goalBlacklist.add(method);
            return;
        }
        bot.getActionManager().setAction(
            new com.rs.game.player.actions.Cooking(range, rawItem, 28, cookable));
        lastDiagnostic = "cook: cooking " + rawItem.getId();
        if (Utils.random(100) < 30) say("cooking up dinner");
    }

    protected void tryStartFiremaking(com.rs.bot.ai.TrainingMethods.Method method) {
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.FIREMAKING);
            if (lvl < method.minLevel) {
                lastDiagnostic = "fm: my level " + lvl + " < required " + method.minLevel;
                return;
            }
        } catch (Throwable ignored) {}
        // Need a tinderbox (toolkit-level) and logs in inventory
        if (!bot.getInventory().containsItemToolBelt(590)) {
            BotEquipment.tryBuyTool(bot, 590);
        }
        // Find the highest-tier log the bot can use
        com.rs.game.player.actions.Firemaking.Fire targetFire = null;
        int botFmLvl = bot.getSkills().getLevel(com.rs.game.player.Skills.FIREMAKING);
        for (com.rs.game.player.actions.Firemaking.Fire f : com.rs.game.player.actions.Firemaking.Fire.values()) {
            try {
                java.lang.reflect.Method getLogId = f.getClass().getMethod("getLogId");
                int logId = (Integer) getLogId.invoke(f);
                java.lang.reflect.Method getLevel = f.getClass().getMethod("getLevel");
                int lvlReq = (Integer) getLevel.invoke(f);
                if (botFmLvl >= lvlReq && bot.getInventory().containsItem(logId, 1)) {
                    if (targetFire == null) targetFire = f;
                    else {
                        java.lang.reflect.Method otherLevel = targetFire.getClass().getMethod("getLevel");
                        int curLvl = (Integer) otherLevel.invoke(targetFire);
                        if (lvlReq > curLvl) targetFire = f;
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (targetFire == null) {
            // No logs - blacklist FM, switch to WC to chop some
            lastDiagnostic = "fm: no logs in inventory, need to chop first";
            if (Utils.random(100) < 3) sayDebug("no logs to burn");
            goalBlacklist.add(method);
            return;
        }
        bot.getActionManager().setAction(new com.rs.game.player.actions.Firemaking(targetFire));
        lastDiagnostic = "fm: lighting " + targetFire;
        if (Utils.random(100) < 30) say("nice cozy fire");
    }

    /** Last training method the bot announced - used to avoid spamming chat. */
    private com.rs.bot.ai.TrainingMethods.Method lastAnnouncedMethod;

    /**
     * Humanization: per-bot reaction delay. After picking a new method or
     * action, bot pauses briefly before doing the next thing. Real players
     * take a fraction of a second to a few seconds to react / move mouse /
     * read tooltips. Without this, 50 bots all click 'fight' on the same
     * tick and look obviously synchronized.
     *
     * Stored as an absolute timestamp the bot must wait until before acting.
     */
    private long actNotBefore = 0L;
    /** Occasional longer AFK pause - bots pretending to alt-tab, snack, scroll. */
    private long afkUntil = 0L;

    /**
     * Returns true if the bot is allowed to act this tick. Sets the next
     * reaction delay so subsequent ticks pause appropriately.
     */
    private boolean canActNow() {
        long now = System.currentTimeMillis();
        if (now < afkUntil) return false;
        if (now < actNotBefore) return false;
        // Roll a tiny chance of a longer AFK pause - 0.3% per check, ~once
        // every 5-10 minutes of activity. Mimics tab-out / snack / scroll.
        if (Utils.random(1000) < 3) {
            afkUntil = now + (5_000 + Utils.random(25_000)); // 5-30s
            return false;
        }
        // Normal reaction delay: 350-1500ms with occasional longer thinks.
        int delay = 350 + Utils.random(1150);
        if (Utils.random(100) < 8) delay += Utils.random(2500); // 8% chance of 'thinking'
        actNotBefore = now + delay;
        return true;
    }

    /**
     * Set bot's melee/ranged/magic XP style based on the current goal's
     * target skill. Without this, every combat-trained bot dumped XP into
     * Attack regardless of whether their step was "Train Strength to 30".
     */
    private void applyXpStyleForGoal(Goal goal) {
        if (goal == null) return;
        com.rs.bot.ai.GoalType type = goal.getData("goalType", com.rs.bot.ai.GoalType.class);
        if (type == null) return;
        String key = type.getRequirementKey();
        if (key == null) return;
        try {
            com.rs.game.player.CombatDefinitions cd = bot.getCombatDefinitions();
            if (cd == null) return;
            // Melee styles: 0=Accurate(Attack), 1=Aggressive(Strength), 2=Controlled, 3=Defensive
            if (key.equals("skill:attack"))   cd.setMeleeCombatExperience(0);
            else if (key.equals("skill:strength")) cd.setMeleeCombatExperience(1);
            else if (key.equals("skill:defence"))  cd.setMeleeCombatExperience(3);
            else if (key.equals("skill:hitpoints")) cd.setMeleeCombatExperience(2); // controlled (split)
            else if (key.equals("combat:max")) cd.setMeleeCombatExperience(2); // balanced
            // Ranged styles: 0=Accurate(Range), 1=Rapid(Range), 2=Longrange(Range+Defence)
            else if (key.equals("skill:ranged")) cd.setRangedCombatExperience(0);
            // Magic styles: 0=Magic dmg, 1=Defensive cast (split with Defence)
            else if (key.equals("skill:magic")) cd.setMagicCombatExperience(0);
        } catch (Throwable ignored) {}
    }

    /** Stuck detection: snapshot of current method's relevant XP, when set. */
    private long stuckXpSnapshot = -1;
    private long stuckSinceMs = 0;
    /** Plans we already tried and failed for the current goal - rotated through. */
    private final java.util.Set<com.rs.bot.ai.TrainingMethods.Method> goalBlacklist = new java.util.HashSet<>();
    /** Goal description tied to the current blacklist - reset when goal changes. */
    private String blacklistGoalDesc = null;
    /** Threshold: if no XP gained in this many ms, declare method stuck. */
    private static final long STUCK_THRESHOLD_MS = 25_000;
    /**
     * If the current method has produced no XP gain for STUCK_THRESHOLD_MS,
     * blacklist it and report. Returns true if blacklisted (caller should
     * rotate to the next plan).
     */
    private boolean checkAndHandleStuck(com.rs.bot.ai.TrainingMethods.Method method) {
        if (method == null || method.skill < 0) return false;
        long currentXp;
        try { currentXp = (long) bot.getSkills().getXp(method.skill); }
        catch (Throwable t) { return false; }
        long now = System.currentTimeMillis();
        if (stuckXpSnapshot < 0) {
            stuckXpSnapshot = currentXp;
            stuckSinceMs = now;
            return false;
        }
        if (currentXp > stuckXpSnapshot) {
            // Progress! Reset the timer + count as success once.
            if (stuckXpSnapshot > 0 && method != null) {
                com.rs.bot.SuccessTracker.onMethodSuccess(method.description);
            }
            stuckXpSnapshot = currentXp;
            stuckSinceMs = now;
            return false;
        }
        if (now - stuckSinceMs < STUCK_THRESHOLD_MS) return false;
        // Stuck for the full threshold - blacklist this method for the goal.
        goalBlacklist.add(method);
        com.rs.bot.SuccessTracker.onMethodStuck(method.description);
        sayDebug("plan stuck (no xp 30s): " + method.description + " - trying alt");
        lastDiagnostic = "stuck on " + method.description + " for " + ((now - stuckSinceMs) / 1000) + "s, blacklisted";
        // Reset snapshot for the NEXT method we'll pick.
        stuckXpSnapshot = -1;
        stuckSinceMs = 0;
        return true;
    }

    private void announceMethodStart(com.rs.bot.ai.TrainingMethods.Method method) {
        if (method == null || method == lastAnnouncedMethod) return;
        lastAnnouncedMethod = method;
        Goal g = goalStack.getCurrentGoal();
        if (g != null) sayGoal(g.getDescription());
        switch (method.kind) {
            case WOODCUTTING:    sayStep("chopping " + treeKindLabel(method)); break;
            case MINING:         sayStep("mining " + rockKindLabel(method)); break;
            case FISHING:        sayStep("fishing " + fishKindLabel(method)); break;
            case THIEVING:       sayStep("pickpocketing " + method.description.replace("Pickpocket ", "")); break;
            case COMBAT:         sayStep("combat training"); break;
            case FIREMAKING:     sayStep("firemaking"); break;
            case COOKING:        sayStep("cooking"); break;
            case SMELTING:       sayStep("smelting bars"); break;
            case CRAFTING:       sayStep("crafting"); break;
            case PRAYER:         sayStep("training prayer"); break;
            case HERBLORE:       sayStep("herblore"); break;
            case AGILITY:        sayStep("running agility course"); break;
            case RUNECRAFTING:   sayStep("crafting runes"); break;
            case HUNTER:         sayStep("hunting"); break;
            case SUMMONING:      sayStep("summoning"); break;
            case FARMING:        sayStep("farming"); break;
            case CONSTRUCTION:   sayStep("constructing"); break;
            case FLETCHING:      sayStep("fletching"); break;
            case DIVINATION:     sayStep("harvesting wisps"); break;
            case DUNGEONEERING:  sayStep("dungeoneering"); break;
            case SMITHING_ANVIL: sayStep("smithing at anvil"); break;
            case MINIGAME:       sayStep("queueing for " + method.description.replace("Minigame - ", "").replace(" lobby", "").replace(" outpost", "")); break;
        }
    }

    private static String treeKindLabel(com.rs.bot.ai.TrainingMethods.Method m) {
        if (m.treeDef == null) return "trees";
        return m.treeDef.toString().toLowerCase().replace('_', ' ');
    }
    private static String rockKindLabel(com.rs.bot.ai.TrainingMethods.Method m) {
        if (m.rockDef == null) return "ore";
        return m.rockDef.toString().toLowerCase().replace('_', ' ');
    }
    private static String fishKindLabel(com.rs.bot.ai.TrainingMethods.Method m) {
        if (m.fishDef == null) return "spots";
        return m.fishDef.toString().toLowerCase().replace('_', ' ');
    }

    protected void tryStartThieving(com.rs.bot.ai.TrainingMethods.Method method) {
        if (method == null || method.npcIds == null || method.npcIds.length == 0) {
            lastDiagnostic = "thieving: no npc ids in method";
            return;
        }
        // Inventory check - PickPocketAction.checkAll fails if no free slots.
        try {
            if (bot.getInventory().getFreeSlots() < 1) {
                lastDiagnostic = "thieving: inventory full, going to bank next";
                if (Utils.random(100) < 50) sayDebug("inventory full");
                return;
            }
        } catch (Throwable ignored) {}
        // Skill level pre-check - so we don't try a method we can't actually fire.
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.THIEVING);
            if (lvl < method.minLevel) {
                lastDiagnostic = "thieving: my level " + lvl + " < required " + method.minLevel + " for " + method.description;
                if (Utils.random(100) < 50) sayDebug("thieving lvl " + lvl + " < required " + method.minLevel);
                return;
            }
        } catch (Throwable ignored) {}
        com.rs.game.npc.NPC target = EnvironmentScanner.findNearestNPC(bot, 8, method.npcIds);
        if (target == null) {
            lastDiagnostic = "thieving: no target in 8 tiles for " + method.description;
            if (Utils.random(100) < 50) sayDebug("no pickpocket target in 8 tiles");
            BotPathing.wiggle(bot, 4);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), target.getX(), target.getY())) {
            BotPathing.walkToEntity(bot, target);
            lastDiagnostic = "thieving: walking to NPC " + target.getId();
            return;
        }
        com.rs.game.player.actions.thieving.PickPocketableNPC data =
            com.rs.game.player.actions.thieving.PickPocketableNPC.get(target.getId());
        if (data == null) {
            lastDiagnostic = "thieving: NPC " + target.getId() + " not pickpocketable";
            return;
        }
        bot.getActionManager().setAction(
            new com.rs.game.player.actions.thieving.PickPocketAction(target, data));
        lastDiagnostic = "thieving: pickpocketing NPC " + target.getId();
        if (Utils.random(100) < 25) say("nicked another one");
    }

    protected void tryStartWoodcutting(com.rs.bot.ai.TrainingMethods.Method method) {
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.WOODCUTTING);
            if (lvl < method.minLevel) {
                lastDiagnostic = "wc: my level " + lvl + " < required " + method.minLevel;
                if (Utils.random(100) < 50) sayDebug("woodcutting lvl " + lvl + " < required " + method.minLevel);
                return;
            }
        } catch (Throwable ignored) {}
        if (com.rs.game.player.actions.Woodcutting.getHatchet(bot, false) == null) {
            int cb = bot.getSkills().getCombatLevel();
            int targetAxe;
            if      (cb >= 60) targetAxe = 1359;
            else if (cb >= 30) targetAxe = 1357;
            else if (cb >= 20) targetAxe = 1355;
            else if (cb >= 10) targetAxe = 1353;
            else               targetAxe = 1351;
            if (BotEquipment.tryBuyTool(bot, targetAxe)) {
                lastDiagnostic = "wc: bought a hatchet";
                if (Utils.random(100) < 50) sayDebug("bought a hatchet");
                return;
            }
            if (targetAxe != 1351 && BotEquipment.tryBuyTool(bot, 1351)) {
                lastDiagnostic = "wc: bought a bronze axe (couldn't afford " + targetAxe + ")";
                if (Utils.random(100) < 50) sayDebug("can only afford a bronze axe");
                return;
            }
            goalBlacklist.add(method);
            lastDiagnostic = "wc: broke and no hatchet - need to earn money first";
            if (Utils.random(100) < 50) sayDebug("no axe + no gp, need to earn first");
            return;
        }
        EnvironmentScanner.TreeMatch match =
            EnvironmentScanner.findNearestTree(bot, 24, method == null ? null : method.treeDef);
        if (match == null) {
            lastDiagnostic = "wc: no " + (method == null ? "tree" : method.treeDef) + " in 24 tiles";
            if (Utils.random(100) < 50) sayDebug("no " + treeKindLabel(method) + " in 24 tiles");
            BotPathing.wiggle(bot, 5);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), match.object)) {
            lastDiagnostic = "wc: walking to " + match.definition + " at " + match.object.getX() + "," + match.object.getY();
            BotPathing.walkToObject(bot, match.object);
            return;
        }
        bot.getActionManager().setAction(new Woodcutting(match.object, match.definition));
        lastDiagnostic = "wc: chopping " + match.definition;
        if (Utils.random(100) < 30) say(woodcuttingChatter());
    }

    protected void tryStartMining(com.rs.bot.ai.TrainingMethods.Method method) {
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.MINING);
            if (lvl < method.minLevel) {
                lastDiagnostic = "mining: my level " + lvl + " < required " + method.minLevel;
                if (Utils.random(100) < 50) sayDebug("mining lvl " + lvl + " < required " + method.minLevel);
                return;
            }
        } catch (Throwable ignored) {}
        // Tool check - if no pickaxe: try to BUY one with coins. If broke,
        // blacklist mining and fall back to no-tool activity.
        if (com.rs.game.player.actions.mining.MiningBase.getPickAxeDefinitions(bot, false) == null) {
            int cb = bot.getSkills().getCombatLevel();
            int targetPickaxe;
            if      (cb >= 60) targetPickaxe = 1275;
            else if (cb >= 30) targetPickaxe = 1271;
            else if (cb >= 20) targetPickaxe = 1269;
            else if (cb >= 10) targetPickaxe = 1267;
            else               targetPickaxe = 1265;
            if (BotEquipment.tryBuyTool(bot, targetPickaxe)) {
                lastDiagnostic = "mining: bought a pickaxe";
                if (Utils.random(100) < 50) sayDebug("bought a pickaxe");
                return;
            }
            // Can't afford even bronze - fall back to bronze if higher tier failed
            if (targetPickaxe != 1265 && BotEquipment.tryBuyTool(bot, 1265)) {
                lastDiagnostic = "mining: bought a bronze pickaxe (couldn't afford " + targetPickaxe + ")";
                if (Utils.random(100) < 50) sayDebug("can only afford a bronze pickaxe");
                return;
            }
            // Truly broke - blacklist mining for this goal and fall back
            // to a no-tool earner (the goal-driver will pick pickpocket Man next tick).
            goalBlacklist.add(method);
            lastDiagnostic = "mining: broke and no pickaxe - need to earn money first";
            if (Utils.random(100) < 50) sayDebug("no pickaxe + no gp, need to earn first");
            return;
        }
        EnvironmentScanner.RockMatch match =
            EnvironmentScanner.findNearestRock(bot, 24, method == null ? null : method.rockDef);
        if (match == null) {
            lastDiagnostic = "mining: no " + (method == null ? "rock" : method.rockDef) + " in 24 tiles";
            if (Utils.random(100) < 50) sayDebug("no " + rockKindLabel(method) + " in 24 tiles");
            BotPathing.wiggle(bot, 5);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), match.object)) {
            lastDiagnostic = "mining: walking to " + match.definition + " at " + match.object.getX() + "," + match.object.getY();
            BotPathing.walkToObject(bot, match.object);
            return;
        }
        bot.getActionManager().setAction(new Mining(match.object, match.definition));
        lastDiagnostic = "mining: extracting " + match.definition;
        if (Utils.random(100) < 30) say(miningChatter());
    }

    protected void tryStartFishing(com.rs.bot.ai.TrainingMethods.Method method) {
        try {
            int lvl = bot.getSkills().getLevel(com.rs.game.player.Skills.FISHING);
            if (lvl < method.minLevel) {
                lastDiagnostic = "fishing: my level " + lvl + " < required " + method.minLevel;
                if (Utils.random(100) < 50) sayDebug("fishing lvl " + lvl + " < required " + method.minLevel);
                return;
            }
        } catch (Throwable ignored) {}
        // Tool check by fishing-spot type
        int neededTool = -1;
        if (method.fishDef != null) {
            String spot = method.fishDef.toString();
            if (spot.contains("NET"))     neededTool = 303;       // Small net
            else if (spot.contains("CAGE")) neededTool = 301;     // Lobster pot
            else if (spot.contains("HARPOON")) neededTool = 311;  // Harpoon
            else if (spot.contains("LURE") || spot.contains("BAIT")) neededTool = 307; // Fishing rod
        }
        if (neededTool > 0 && !bot.getInventory().containsItem(neededTool, 1)) {
            if (BotEquipment.tryBuyTool(bot, neededTool)) {
                lastDiagnostic = "fishing: bought tool " + neededTool;
                if (Utils.random(100) < 50) sayDebug("bought a fishing tool");
                return;
            }
            goalBlacklist.add(method);
            lastDiagnostic = "fishing: broke + missing tool " + neededTool;
            if (Utils.random(100) < 50) sayDebug("can't afford a fishing tool");
            return;
        }
        EnvironmentScanner.FishMatch match =
            EnvironmentScanner.findNearestFishingSpot(bot, 24, method == null ? null : method.fishDef);
        if (match == null) {
            lastDiagnostic = "fishing: no " + (method == null ? "spot" : method.fishDef) + " in 24 tiles";
            if (Utils.random(100) < 50) sayDebug("no " + fishKindLabel(method) + " in 24 tiles");
            BotPathing.wiggle(bot, 5);
            return;
        }
        if (!isAdjacent(bot.getX(), bot.getY(), match.npc.getX(), match.npc.getY())) {
            lastDiagnostic = "fishing: walking to " + match.definition + " at " + match.npc.getX() + "," + match.npc.getY();
            BotPathing.walkToEntity(bot, match.npc);
            return;
        }
        bot.getActionManager().setAction(new Fishing(match.definition, match.npc));
        lastDiagnostic = "fishing: " + match.definition;
        if (Utils.random(100) < 30) say(fishingChatter());
    }

    /**
     * Stubs for skills whose locations are wired in TrainingMethods but whose
     * Action implementations land skill-by-skill. Each walks the bot to the
     * method's tile then logs the "(action coming)" diagnostic. Audit log
     * picks this up via SuccessTracker so we can verify the location is
     * reachable before doing the harder action wiring.
     */
    private void walkOrStallAtMethod(com.rs.bot.ai.TrainingMethods.Method method,
                                     int skillId, String label) {
        try {
            int lvl = bot.getSkills().getLevel(skillId);
            if (lvl < method.minLevel) {
                lastDiagnostic = label + ": my level " + lvl + " < required " + method.minLevel;
                return;
            }
        } catch (Throwable ignored) {}
        int dx = bot.getX() - method.location.getX();
        int dy = bot.getY() - method.location.getY();
        if (dx*dx + dy*dy > 16) {
            BotPathing.walkTo(bot, method.location.getX(), method.location.getY());
            lastDiagnostic = label + ": walking to " + method.description;
            return;
        }
        lastDiagnostic = label + ": at location, action not yet wired";
        if (Utils.random(100) < 5) sayDebug(label + " action coming");
    }

    protected void tryStartHerblore(com.rs.bot.ai.TrainingMethods.Method method) {
        walkOrStallAtMethod(method, com.rs.game.player.Skills.HERBLORE, "herblore");
        // At destination: clean grimy herbs (real HerbCleaning XP/anim).
        int dx = bot.getX() - method.location.getX();
        int dy = bot.getY() - method.location.getY();
        if (dx*dx + dy*dy <= 16) {
            if (com.rs.bot.ai.BotSkillActions.cleanHerbs(bot)) {
                lastDiagnostic = "herblore: cleaning a herb";
            }
        }
    }
    /** Stub skills (Agility / RC / Hunter / Summoning / Farming / Divination /
     *  Smithing-anvil) - walk to destination, then gain XP each tick at the
     *  spot. Same pattern as Construction / Dungeoneering. Real action wiring
     *  per-skill is involved (course obstacles, altar enter+craft, trap
     *  timers, patch growth) and lands incrementally. */
    private void autoXpStubSkill(com.rs.bot.ai.TrainingMethods.Method method,
                                 int skillId, String label, double xpPerTick) {
        walkOrStallAtMethod(method, skillId, label);
        int dx = bot.getX() - method.location.getX();
        int dy = bot.getY() - method.location.getY();
        if (dx*dx + dy*dy <= 16) {
            com.rs.bot.ai.BotSkillActions.autoXp(bot, skillId, xpPerTick);
        }
    }

    protected void tryStartAgility(com.rs.bot.ai.TrainingMethods.Method method) {
        autoXpStubSkill(method, com.rs.game.player.Skills.AGILITY, "agility", 1200);
    }
    protected void tryStartRunecrafting(com.rs.bot.ai.TrainingMethods.Method method) {
        autoXpStubSkill(method, com.rs.game.player.Skills.RUNECRAFTING, "rc", 800);
    }
    protected void tryStartHunter(com.rs.bot.ai.TrainingMethods.Method method) {
        autoXpStubSkill(method, com.rs.game.player.Skills.HUNTER, "hunter", 1100);
    }
    protected void tryStartSummoning(com.rs.bot.ai.TrainingMethods.Method method) {
        autoXpStubSkill(method, com.rs.game.player.Skills.SUMMONING, "summoning", 700);
    }
    protected void tryStartFarming(com.rs.bot.ai.TrainingMethods.Method method) {
        autoXpStubSkill(method, com.rs.game.player.Skills.FARMING, "farming", 900);
    }
    protected void tryStartConstruction(com.rs.bot.ai.TrainingMethods.Method method) {
        walkOrStallAtMethod(method, com.rs.game.player.Skills.CONSTRUCTION, "construction");
        // POH building needs UI we don't simulate. Auto-XP at the portal so
        // the bot still progresses (per user spec: "Construction doesn't need
        // [a step process]. Legend bots can just gradually gain exp"). Caller
        // is throttled inside autoXp.
        int dx = bot.getX() - method.location.getX();
        int dy = bot.getY() - method.location.getY();
        if (dx*dx + dy*dy <= 16) {
            com.rs.bot.ai.BotSkillActions.autoXp(bot, com.rs.game.player.Skills.CONSTRUCTION, 1500);
        }
    }
    protected void tryStartFletching(com.rs.bot.ai.TrainingMethods.Method method) {
        walkOrStallAtMethod(method, com.rs.game.player.Skills.FLETCHING, "fletching");
        int dx = bot.getX() - method.location.getX();
        int dy = bot.getY() - method.location.getY();
        if (dx*dx + dy*dy <= 16) {
            // Already cutting? Don't double-set the action.
            if (bot.getActionManager().getAction() instanceof com.rs.game.player.actions.Fletching) return;
            if (com.rs.bot.ai.BotSkillActions.fletchBow(bot)) {
                lastDiagnostic = "fletching: cutting bow";
            }
        }
    }
    protected void tryStartDivination(com.rs.bot.ai.TrainingMethods.Method method) {
        autoXpStubSkill(method, com.rs.game.player.Skills.DIVINATION, "divination", 1000);
    }
    protected void tryStartDungeoneering(com.rs.bot.ai.TrainingMethods.Method method) {
        walkOrStallAtMethod(method, com.rs.game.player.Skills.DUNGEONEERING, "dungeoneering");
        // Dungeon runs need real-player coupling we don't simulate yet.
        // Auto-XP at Daemonheim entrance per user spec.
        int dx = bot.getX() - method.location.getX();
        int dy = bot.getY() - method.location.getY();
        if (dx*dx + dy*dy <= 16) {
            com.rs.bot.ai.BotSkillActions.autoXp(bot, com.rs.game.player.Skills.DUNGEONEERING, 2500);
        }
    }
    protected void tryStartSmithingAnvil(com.rs.bot.ai.TrainingMethods.Method method) {
        autoXpStubSkill(method, com.rs.game.player.Skills.SMITHING, "smith-anvil", 1300);
    }
    protected void tryStartMinigame(com.rs.bot.ai.TrainingMethods.Method method) {
        // No skill gate - minigames are open to all levels. Walk-stall only;
        // actual queue-up + play wiring is per-minigame and lands in a
        // follow-up commit.
        int dx = bot.getX() - method.location.getX();
        int dy = bot.getY() - method.location.getY();
        if (dx*dx + dy*dy > 16) {
            BotPathing.walkTo(bot, method.location.getX(), method.location.getY());
            lastDiagnostic = "minigame: walking to " + method.description;
            return;
        }
        lastDiagnostic = "minigame: at " + method.description + " lobby";
    }

    protected void tryStartCombat(com.rs.bot.ai.TrainingMethods.Method method) {
        // Retreat first if we're low on HP. Eat from inventory if we have
        // food, otherwise stop attacking and walk back to safety.
        if (handleLowHpRetreat()) return;

        // Already attacking something? Let PlayerCombatNew finish its loop.
        if (bot.getActionManager().getAction() instanceof PlayerCombatNew) return;

        if (method.npcIds == null || method.npcIds.length == 0) {
            lastDiagnostic = "combat: method has no npcIds";
            return;
        }

        // Pre-fight buffs - drink combat potions if owned + activate combat
        // prayer if level allows. Mimics what real players do before pulling.
        applyPreFightBuffs();

        // Combat depth: switch to Revolution mode + activate the highest
        // prayer the bot's level supports for its archetype style. Lowbies
        // stay in Legacy so they can auto-attack without ability cooldowns.
        com.rs.bot.ai.BotCombat.preparePreFight(bot);

        // Scan parity: 24 tiles like other skills (was 12, made low-cb bots
        // miss visible NPCs on a jittered teleport landing).
        NPC target = EnvironmentScanner.findNearestNPC(bot, 24, method.npcIds);
        if (target == null) {
            lastDiagnostic = "combat: no NPC " + java.util.Arrays.toString(method.npcIds) + " in 24 tiles";
            if (Utils.random(100) < 50) sayDebug("no enemies here");
            BotPathing.wiggle(bot, 4);
            return;
        }

        // Don't pull a target a real player wouldn't - if combat level
        // gap is too large, blacklist (the method should have gated this
        // but let's catch it just in case).
        int botCb = bot.getSkills().getCombatLevel();
        int targetCb = target.getDefinitions().combatLevel;
        if (targetCb > botCb * 2 + 5) {
            lastDiagnostic = "combat: target cb " + targetCb + " way over bot cb " + botCb + " - skipping";
            if (Utils.random(100) < 3) sayDebug("that's way out of my league");
            goalBlacklist.add(method);
            return;
        }

        // PlayerCombatNew handles its own approach + attack-distance logic
        // (melee = adjacent, ranged/magic = within sight). Just hand it the
        // target and let it run.
        bot.getActionManager().setAction(new PlayerCombatNew(target));
        lastDiagnostic = "combat: attacking NPC " + target.getId() + " (cb " + targetCb + ")";
        if (Utils.random(100) < 20) say(combatChatter());
    }

    /**
     * Pre-fight buff routine: drink owned potions, activate combat prayer
     * the bot's level supports. Called once per combat-method tick before
     * setting the PlayerCombatNew action so buffs are live during the fight.
     *
     * Doesn't try to be optimal - just brings the bot up to the level a
     * real player would have when pulling. Throttled by the canActNow
     * reaction delay so it doesn't spam.
     */
    private void applyPreFightBuffs() {
        try {
            // Combat potions: drink one of each owned type if not already buffed.
            // Item IDs map to potion doses; checking dose-4 first then 3/2/1.
            int[][] combatPots = new int[][] {
                {12695, 12697, 12699, 12701},   // super combat potion
                {2440, 2442, 2444, 2436},        // strength
                {113, 115, 117, 119},             // attack
                {2432, 2434, 2436, 2438},        // defence
                {2444, 2446, 2448, 2450}         // ranged
            };
            for (int[] pot : combatPots) {
                for (int id : pot) {
                    try {
                        if (bot.getInventory().containsItem(id, 1)) {
                            bot.getInventory().deleteItem(id, 1);
                            bot.setNextAnimation(new com.rs.game.Animation(829));
                            // Replace with one-dose-lower if it exists
                            int idx = -1;
                            for (int i = 0; i < pot.length; i++) if (pot[i] == id) { idx = i; break; }
                            if (idx > 0) bot.getInventory().addItem(pot[idx - 1], 1);
                            // Apply rough stat boost (10% + 4 - matches super combat-ish)
                            try {
                                bot.getSkills().set(com.rs.game.player.Skills.STRENGTH,
                                    Math.min(120, bot.getSkills().getLevel(com.rs.game.player.Skills.STRENGTH) + 5));
                                bot.getSkills().set(com.rs.game.player.Skills.ATTACK,
                                    Math.min(120, bot.getSkills().getLevel(com.rs.game.player.Skills.ATTACK) + 5));
                            } catch (Throwable ignored) {}
                            if (Utils.random(100) < 30) say("chugging a potion");
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * In-fight maintenance: eat when HP gets low, drink prayer potions
     * when prayer drops, and bail to safety if we have no food at all.
     * Eat threshold 50% (was 30%) - matches what real players do during
     * sustained training. Returns true if we acted (caller should not
     * start a new combat action this tick).
     */
    private boolean handleLowHpRetreat() {
        int hp = bot.getHitpoints();
        int maxHp = bot.getMaxHitpoints();

        // Top up prayer first if it's getting low. Cheap to do mid-fight
        // and lets the prayer-buffed bot keep its damage/accuracy boost.
        try {
            int pp = bot.getPrayer().getPrayerpoints();
            int maxPp = bot.getPrayer().getMaxPrayerpoints();
            if (maxPp > 0 && pp < maxPp * 0.30) drinkPrayerPotion();
        } catch (Throwable ignored) {}

        // Eat at 50% HP - real players don't tank to 30%, and PlayerCombatNew
        // can take a few hits before we get another chance to eat. If we're
        // already above 50% bail out and let combat continue.
        if (maxHp <= 0 || hp > maxHp * 0.50) return false;

        // Try to eat the strongest food we own. Keep this minimal - we're
        // not simulating proper food cooldowns, just keeping the bot alive.
        for (int foodId : FOOD_ITEM_IDS) {
            if (bot.getInventory().containsItem(foodId, 1)) {
                bot.getInventory().deleteItem(foodId, 1);
                int healAmount = healValueFor(foodId);
                bot.heal(healAmount);
                bot.setNextAnimation(new Animation(829)); // eat anim
                if (Utils.random(100) < 25) say("eating up");
                // Don't bail combat just because we ate - keep attacking
                // unless we're actually below the panic threshold (30%).
                return hp < maxHp * 0.30;
            }
        }

        // No food - bail on combat only if we're truly low (< 30% HP).
        if (hp < maxHp * 0.30) {
            if (bot.getActionManager().getAction() instanceof PlayerCombatNew) {
                bot.getActionManager().forceStop();
            }
            int dx = Utils.random(-6, 7);
            int dy = Utils.random(-6, 7);
            bot.addWalkSteps(bot.getX() + dx, bot.getY() + dy, 8, true);
            if (Utils.random(100) < 50) say("running, no food!");
            return true;
        }
        return false;
    }

    /**
     * Drink a prayer potion if the bot owns one. Restores prayer points
     * for the rest of the fight. Item ID list covers prayer pot (3) ->
     * (1) and super restore (4) -> (1) which also restores stats.
     */
    private void drinkPrayerPotion() {
        // Pairs of (current dose ID, replacement one-dose-lower ID, restore amount)
        int[][] prayerPots = new int[][] {
            {2434, 2436, 70},   // Prayer pot (4) -> (3)
            {2436, 139, 70},    // Prayer pot (3) -> (2)
            {139, 141, 70},     // Prayer pot (2) -> (1)
            {141, -1, 70},      // Prayer pot (1) -> empty vial
            {3024, 3026, 80},   // Super restore (4) -> (3)
            {3026, 3028, 80},   // Super restore (3) -> (2)
            {3028, 3030, 80},   // Super restore (2) -> (1)
            {3030, -1, 80}      // Super restore (1) -> empty vial
        };
        for (int[] pot : prayerPots) {
            try {
                if (bot.getInventory().containsItem(pot[0], 1)) {
                    bot.getInventory().deleteItem(pot[0], 1);
                    bot.setNextAnimation(new Animation(829));
                    int restore = pot[2];
                    int newPts = Math.min(bot.getPrayer().getMaxPrayerpoints(),
                        bot.getPrayer().getPrayerpoints() + restore);
                    bot.getPrayer().setPrayerpoints(newPts);
                    bot.getPrayer().refreshPrayerPoints();
                    if (pot[1] != -1) bot.getInventory().addItem(pot[1], 1);
                    if (Utils.random(100) < 30) say("drinking prayer pot");
                    return;
                }
            } catch (Throwable ignored) {}
        }
    }

    // Common food item IDs. First-match wins, so list strongest first.
    private static final int[] FOOD_ITEM_IDS = {
        15272, // rocktail
        385,   // shark
        379,   // lobster
        373,   // swordfish
        7946,  // monkfish
        333,   // trout
        315,   // shrimp
        2142,  // cooked karambwan
        391    // manta ray
    };

    private static int healValueFor(int foodId) {
        switch (foodId) {
            case 15272: return 230; // rocktail
            case 391:   return 220; // manta
            case 385:   return 200; // shark
            case 7946:  return 160; // monkfish
            case 373:   return 140; // swordfish
            case 379:   return 120; // lobster
            case 333:   return 70;  // trout
            case 2142:  return 180; // karambwan
            case 315:   return 30;  // shrimp
            default:    return 100;
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
    private static final String[] COMBAT_CHATTER = {
        "ez", "any teams?", "wts bones", "almost 99 attack", "one shot lol",
        "this xp tho", "training to max", "anyone got food?"
    };
    private String woodcuttingChatter() { return WOODCUTTING_CHATTER[Utils.random(WOODCUTTING_CHATTER.length)]; }
    private String miningChatter() { return MINING_CHATTER[Utils.random(MINING_CHATTER.length)]; }
    private String fishingChatter() { return FISHING_CHATTER[Utils.random(FISHING_CHATTER.length)]; }
    private String combatChatter() { return COMBAT_CHATTER[Utils.random(COMBAT_CHATTER.length)]; }

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

    /** Tagged chat - prefixes the message so observers can tell what kind of update it is. */
    private void sayGoal(String text)  {
        say("[Goal] " + text);
        com.rs.bot.BotLog.log(bot.getDisplayName(), "[goal] " + text);
        if (com.rs.bot.AuditLog.isStreaming()) com.rs.bot.AuditLog.log(bot.getDisplayName() + " [goal] " + text);
    }
    private void sayStep(String text)  {
        say("[Step] " + text);
        com.rs.bot.BotLog.log(bot.getDisplayName(), "[step] " + text);
        if (com.rs.bot.AuditLog.isStreaming()) com.rs.bot.AuditLog.log(bot.getDisplayName() + " [step] " + text);
    }
    private void sayDebug(String text) {
        say("[Debug] " + text);
        com.rs.bot.BotLog.log(bot.getDisplayName(), "[debug] " + text);
        if (com.rs.bot.AuditLog.isStreaming()) com.rs.bot.AuditLog.log(bot.getDisplayName() + " [debug] " + text);
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

    // ===== Skill-economy shop routing =====
    // Maps raw-drop item IDs to the buyer shop that takes them. We invoke
    // shop.sell() directly via BotTrading - no need to walk to the NPC
    // because at-bank is close enough and the shop API has no proximity
    // check (real players walking to the NPC is purely UX, not enforced).

    private static final int SHOP_FORESTER = 200;
    private static final int SHOP_FISHMONGER = 201;
    private static final int SHOP_ORE_TRADER = 202;
    private static final int SHOP_TANNER = 203;
    private static final int SHOP_FLETCHER = 207;
    private static final int SHOP_SMITH = 208;
    private static final int SHOP_COOK = 209;
    private static final int SHOP_DIVINER = 222;
    private static final int SHOP_FIREMAKING = 218;

    // Logs (any tier including pyre)
    private static final int[] LOG_IDS = {1511, 1521, 1519, 1517, 1515, 1513, 6332, 3448};
    // Raw fish (common types)
    private static final int[] RAW_FISH_IDS = {317, 327, 321, 331, 359, 377, 371, 383, 7944, 15270};
    // Cooked fish (Cook shop buys these back)
    private static final int[] COOKED_FISH_IDS = {315, 319, 325, 333, 379, 385, 7946, 15272};
    // Ores
    private static final int[] ORE_IDS = {436, 438, 440, 442, 444, 447, 449, 451, 21622};
    // Bars (smelted from ores - Smith shop buys back)
    private static final int[] BAR_IDS = {2349, 2351, 2353, 2355, 2357, 2359, 2361, 2363};
    // Hides + bones (Tanner buys both)
    private static final int[] HIDE_BONE_IDS = {1739, 1745, 1751, 526, 532, 536, 3183, 4812, 1741, 1743};
    // Divination memories + energies (12 of each tier)
    private static final int[] DIVINATION_IDS = {
        29312, 29313, 29314, 29315, 29316, 29317, 29318, 29319, 29320, 29321, 29322, 29323,
        29383, 29384, 29385, 29386, 29387, 29388, 29389, 29390, 29391, 29392, 29393, 29394
    };

    /**
     * Walk through inventory and dump matching raw drops to the right
     * skill shop. Returns the count of items sold (across all shops).
     */
    private int sellRawDropsToSkillShops(AIPlayer b) {
        int total = 0;
        total += sellMatchingItems(b, LOG_IDS, SHOP_FORESTER);
        total += sellMatchingItems(b, RAW_FISH_IDS, SHOP_FISHMONGER);
        total += sellMatchingItems(b, COOKED_FISH_IDS, SHOP_COOK);
        total += sellMatchingItems(b, ORE_IDS, SHOP_ORE_TRADER);
        total += sellMatchingItems(b, BAR_IDS, SHOP_SMITH);
        total += sellMatchingItems(b, HIDE_BONE_IDS, SHOP_TANNER);
        total += sellMatchingItems(b, DIVINATION_IDS, SHOP_DIVINER);
        return total;
    }

    private int sellMatchingItems(AIPlayer b, int[] itemIds, int shopId) {
        int count = 0;
        try {
            // Iterate by slot; convert any matching items in-place.
            for (int slot = b.getInventory().getItems().getSize() - 1; slot >= 0; slot--) {
                com.rs.game.item.Item item = b.getInventory().getItem(slot);
                if (item == null) continue;
                int id = item.getId();
                boolean matches = false;
                for (int target : itemIds) {
                    if (id == target) { matches = true; break; }
                }
                if (!matches) continue;
                int qty = item.getAmount();
                if (com.rs.bot.ai.BotTrading.sellToShop(b, shopId, slot, qty)) {
                    count += qty;
                }
            }
        } catch (Throwable ignore) {}
        return count;
    }
}
