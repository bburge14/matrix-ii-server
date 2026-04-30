package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.game.player.Skills;
import java.util.*;

/**
 * ArchetypeGoalGenerator - Intelligently generates goals for bots based on:
 * - Bot archetype (Skiller, Combatant, PKer, Bosser, Quester, Collector)
 * - Current stats and skills
 * - Bank value and equipment
 * - Personality preferences
 * - Progression logic
 * 
 * This is the core intelligence that makes bots choose logical goals.
 */
public class ArchetypeGoalGenerator {
    
    /**
     * Generate appropriate goals for a bot based on its archetype and current state
     */
    public static List<Goal> generateGoals(AIPlayer bot) {
        List<Goal> goals = new ArrayList<>();
        String archetype = bot.getArchetype();
        
        // Get bot's current state
        BotAnalysis analysis = analyzeBot(bot);
        
        // Generate goals based on archetype and analysis.
        // Combat archetypes (melee/ranged/magic/tank/pure/main/maxed/f2p)
        // all funnel through generateCombatantGoals - their MAIN focus is
        // whichever combat style their archetype specifies, but they still
        // train non-combat skills too (added inside generateCombatantGoals).
        // Skillers go through generateSkillerGoals which strips combat goals.
        switch (archetype.toLowerCase()) {
            case "skiller":
                goals.addAll(generateSkillerGoals(bot, analysis));
                break;
            case "combatant":
            case "melee":
            case "ranged":
            case "magic":
            case "tank":
            case "pure":
            case "main":
            case "maxed":
            case "f2p":
                goals.addAll(generateCombatantGoals(bot, analysis));
                break;
            case "pker":
                goals.addAll(generatePKerGoals(bot, analysis));
                break;
            case "bosser":
                goals.addAll(generateBosserGoals(bot, analysis));
                break;
            case "quester":
                goals.addAll(generateQuesterGoals(bot, analysis));
                break;
            case "collector":
                goals.addAll(generateCollectorGoals(bot, analysis));
                break;
            case "hybrid":
            case "balanced":
                goals.addAll(generateBalancedGoals(bot, analysis));
                break;
            default:
                goals.addAll(generateDefaultGoals(bot, analysis));
        }
        
        // Add universal goals that all bots might want
        goals.addAll(generateUniversalGoals(bot, analysis));

        // Tier 0/1 checkpoint goals - level-3 bots need objectives they
        // can hit in hours, not 99-aspirations. Adds combat-30/50/70,
        // skill-30/50/70, and small bank checkpoints. The isAchievable
        // filter below drops anything the bot has already passed.
        goals.addAll(generateTierCheckpointGoals(bot, analysis));

        // Drop goals the bot has already accomplished, AND drop goals we
        // have no plan for. Without isAchievable, bots ended up with goals
        // like "99 Fletching" with no TrainingMethods entry, and just
        // wandered near spawn because the brain had nothing to act on.
        goals.removeIf(g -> g == null || !g.isAchievable(bot));

        // Prioritize the survivors.
        // Lifetime identity bias - boost urgency of goals aligned with the
        // bot's long-term north-star. A COMBAT_MAXER's "Train Attack to 30"
        // goal scores higher than its "99 Mining" goal even if both are
        // applicable. PURE_SKILLER bots get NEGATIVE alignment on combat
        // goals so they avoid them entirely.
        com.rs.bot.ai.LifetimeIdentity identity = bot.getLifetimeIdentity();
        if (identity != null) {
            for (Goal g : goals) {
                GoalType t = g.getData("goalType", GoalType.class);
                int boost = identity.alignmentBoost(t);
                if (boost != 0) {
                    g.addLifetimeBoost(boost / 100.0);
                }
            }
        }

        return prioritizeGoals(goals, analysis);
    }

    /**
     * Generate goals for Skiller archetype bots
     */
    private static List<Goal> generateSkillerGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        
        // Primary focus: Get 99 in multiple skills
        for (int skill = 0; skill < Skills.SKILL_NAME.length; skill++) {
            int currentLevel = analysis.getSkillLevel(skill);
            String skillName = Skills.SKILL_NAME[skill];
            
            // Skip combat stats for pure skillers
            if (isSkiller(bot) && isCombatStat(skill)) continue;
            
            if (currentLevel < 99) {
                GoalType goalType = getSkill99GoalType(skill);
                if (goalType != null) {
                    Goal goal = createGoal(goalType, bot, analysis);
                    if (goal != null) goals.add(goal);
                }
            }
        }
        
        // Economic goals for buying supplies
        if (analysis.bankValue < 10000000) { // Less than 10M
            goals.add(createGoal(GoalType.BUILD_10M_BANK, bot, analysis));
        } else if (analysis.bankValue < 100000000) { // Less than 100M
            goals.add(createGoal(GoalType.BUILD_100M_BANK, bot, analysis));
        }
        
        // Resource gathering goals
        goals.add(createGoal(GoalType.CUT_100K_LOGS, bot, analysis));
        goals.add(createGoal(GoalType.MINE_100K_ORES, bot, analysis));
        goals.add(createGoal(GoalType.CATCH_100K_FISH, bot, analysis));
        
        return goals;
    }
    
    /**
     * Generate goals for Combatant archetype bots. Combat is the MAIN focus
     * (and stays priority via LifetimeIdentity bias), but combat archetypes
     * still train non-combat skills - real players don't ignore cooking,
     * fletching, smithing, etc. just because they're a melee main. The
     * archetype controls WHICH combat styles get prioritized:
     *   melee  -> attack/strength/defence
     *   ranged -> ranged + defence
     *   magic  -> magic + defence
     *   tank   -> defence + hp
     *   pure   -> attack/strength only (no defence)
     *   hybrid/main -> all
     */
    private static List<Goal> generateCombatantGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        String archetype = bot.getArchetype() == null ? "main" : bot.getArchetype().toLowerCase();

        // Combat stat priorities - filtered by archetype style focus
        int attack = analysis.getSkillLevel(Skills.ATTACK);
        int strength = analysis.getSkillLevel(Skills.STRENGTH);
        int defence = analysis.getSkillLevel(Skills.DEFENCE);
        int magic = analysis.getSkillLevel(Skills.MAGIC);
        int ranged = analysis.getSkillLevel(Skills.RANGE);

        boolean wantsMelee  = archetype.equals("melee") || archetype.equals("main") || archetype.equals("hybrid")
                            || archetype.equals("tank") || archetype.equals("pure") || archetype.equals("combatant")
                            || archetype.equals("maxed") || archetype.equals("f2p");
        boolean wantsRanged = archetype.equals("ranged") || archetype.equals("main") || archetype.equals("hybrid")
                            || archetype.equals("maxed");
        boolean wantsMagic  = archetype.equals("magic") || archetype.equals("main") || archetype.equals("hybrid")
                            || archetype.equals("maxed");
        boolean wantsDefence = !archetype.equals("pure"); // pures explicitly avoid defence

        if (wantsMelee && attack < 99) goals.add(createGoal(GoalType.TRAIN_ATTACK_99, bot, analysis));
        if (wantsMelee && strength < 99) goals.add(createGoal(GoalType.TRAIN_STRENGTH_99, bot, analysis));
        if (wantsDefence && defence < 99) goals.add(createGoal(GoalType.TRAIN_DEFENCE_99, bot, analysis));
        if (wantsMagic && magic < 99) goals.add(createGoal(GoalType.TRAIN_MAGIC_99, bot, analysis));
        if (wantsRanged && ranged < 99) goals.add(createGoal(GoalType.TRAIN_RANGED_99, bot, analysis));

        // Equipment progression based on combat level
        int combatLevel = analysis.combatLevel;
        if (combatLevel >= 40 && !analysis.hasEquipment("rune")) {
            goals.add(createGoal(GoalType.GET_RUNE_ARMOR, bot, analysis));
        }
        if (combatLevel >= 60 && !analysis.hasEquipment("dragon")) {
            goals.add(createGoal(GoalType.GET_DRAGON_ARMOR, bot, analysis));
        }
        if (combatLevel >= 70 && !analysis.hasEquipment("barrows")) {
            goals.add(createGoal(GoalType.GET_BARROWS_ARMOR, bot, analysis));
        }
        if (combatLevel >= 70 && analysis.bankValue > 50000000) {
            goals.add(createGoal(GoalType.GET_BANDOS_ARMOR, bot, analysis));
        }

        // Weapon goals
        if (attack >= 70 && !analysis.hasEquipment("whip")) {
            goals.add(createGoal(GoalType.GET_ABYSSAL_WHIP, bot, analysis));
        }
        if (attack >= 75) {
            goals.add(createGoal(GoalType.GET_GODSWORD, bot, analysis));
        }

        // Money for gear
        if (analysis.bankValue < 50000000) {
            goals.add(createGoal(GoalType.BUILD_100M_BANK, bot, analysis));
        }

        // Non-combat skill goals - combat bots also train support skills.
        // Lifetime identity bias keeps combat goals on top, but these
        // provide variety so a melee main isn't just attacking the same
        // chicken for 24h. Cooking and Fletching are universally useful;
        // others scale based on what makes sense for a fighter.
        addNonCombatSkillGoals(goals, bot, analysis);

        return goals;
    }

    /**
     * Add non-combat skill goals to a goal pool. Used by combat archetypes
     * so they still train cooking, fletching, smithing, crafting, etc.
     * alongside their combat focus. The lifetime alignment boost still
     * keeps combat goals as primary (alignmentBoost gives +50 to combat:*
     * for COMBAT_MAXER), but these support goals fire when the bot is
     * idle, stuck on combat methods, or just wants variety.
     */
    private static void addNonCombatSkillGoals(List<Goal> goals, AIPlayer bot, BotAnalysis analysis) {
        // Universal support skills every combat bot benefits from
        if (analysis.getSkillLevel(Skills.COOKING) < 99)     goals.add(createGoal(GoalType.SKILL_COOKING_99, bot, analysis));
        if (analysis.getSkillLevel(Skills.FLETCHING) < 99)   goals.add(createGoal(GoalType.SKILL_FLETCHING_99, bot, analysis));
        if (analysis.getSkillLevel(Skills.FIREMAKING) < 99)  goals.add(createGoal(GoalType.SKILL_FIREMAKING_99, bot, analysis));
        if (analysis.getSkillLevel(Skills.WOODCUTTING) < 99) goals.add(createGoal(GoalType.SKILL_WOODCUTTING_99, bot, analysis));
        if (analysis.getSkillLevel(Skills.FISHING) < 99)     goals.add(createGoal(GoalType.SKILL_FISHING_99, bot, analysis));
        if (analysis.getSkillLevel(Skills.MINING) < 99)      goals.add(createGoal(GoalType.SKILL_MINING_99, bot, analysis));
        if (analysis.getSkillLevel(Skills.SMITHING) < 99)    goals.add(createGoal(GoalType.SKILL_SMITHING_99, bot, analysis));
        if (analysis.getSkillLevel(Skills.CRAFTING) < 99)    goals.add(createGoal(GoalType.SKILL_CRAFTING_99, bot, analysis));
        if (analysis.getSkillLevel(Skills.THIEVING) < 99)    goals.add(createGoal(GoalType.SKILL_THIEVING_99, bot, analysis));
    }
    
    /**
     * Generate goals for PKer archetype bots
     */
    private static List<Goal> generatePKerGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        
        int combatLevel = analysis.combatLevel;
        int defence = analysis.getSkillLevel(Skills.DEFENCE);
        
        // Determine PK build based on current stats
        if (defence == 1 && combatLevel < 50) {
            // Pure build
            goals.add(createGoal(GoalType.BUILD_PURE_ACCOUNT, bot, analysis));
        } else if (defence <= 45) {
            // Barrows pure
            goals.add(createGoal(GoalType.BUILD_BARROWS_PURE, bot, analysis));
        } else if (combatLevel >= 100) {
            // Main account
            goals.add(createGoal(GoalType.BUILD_MAIN_ACCOUNT, bot, analysis));
        }
        
        // PK achievement goals
        goals.add(createGoal(GoalType.GET_100_KILLS, bot, analysis));
        goals.add(createGoal(GoalType.MASTER_COMBO_EATING, bot, analysis));
        goals.add(createGoal(GoalType.LEARN_PERFECT_SWITCHING, bot, analysis));
        
        // Money for supplies
        goals.add(createGoal(GoalType.BUILD_10M_BANK, bot, analysis));

        // Support skills - PKers still cook, fletch arrows, herblore for pots, etc.
        addNonCombatSkillGoals(goals, bot, analysis);

        return goals;
    }

    /**
     * Generate goals for Bosser archetype bots
     */
    private static List<Goal> generateBosserGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        
        int combatLevel = analysis.combatLevel;
        
        // Need high combat for bossing
        if (combatLevel < 100) {
            goals.add(createGoal(GoalType.MAX_COMBAT_STATS, bot, analysis));
        }
        
        // Need good gear for bossing
        if (!analysis.hasEquipment("barrows")) {
            goals.add(createGoal(GoalType.GET_BARROWS_ARMOR, bot, analysis));
        }
        if (analysis.bankValue > 100000000) {
            goals.add(createGoal(GoalType.GET_BANDOS_ARMOR, bot, analysis));
        }
        
        // Boss-specific goals based on gear/stats
        if (combatLevel >= 80) {
            goals.add(createGoal(GoalType.COMPLETE_100_BARROWS, bot, analysis));
            goals.add(createGoal(GoalType.KILL_KBD_100, bot, analysis));
        }
        
        if (combatLevel >= 100 && analysis.hasEquipment("barrows")) {
            goals.add(createGoal(GoalType.KILL_GRAARDOR_100, bot, analysis));
            goals.add(createGoal(GoalType.KILL_KREEARRA_100, bot, analysis));
            goals.add(createGoal(GoalType.KILL_ZILYANA_100, bot, analysis));
            goals.add(createGoal(GoalType.KILL_KRIL_100, bot, analysis));
        }
        
        if (combatLevel >= 120) {
            goals.add(createGoal(GoalType.KILL_CORP_50, bot, analysis));
        }
        
        // Collection goals
        goals.add(createGoal(GoalType.GET_ALL_BARROWS_ITEMS, bot, analysis));
        goals.add(createGoal(GoalType.GET_ALL_GWD_ITEMS, bot, analysis));

        // Support skills - bossers still need cooking for sharks, fletching
        // for darts, herblore for super combat pots, etc.
        addNonCombatSkillGoals(goals, bot, analysis);

        return goals;
    }

    /**
     * Generate goals for Quester archetype bots
     */
    private static List<Goal> generateQuesterGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        
        // Quest progression
        goals.add(createGoal(GoalType.COMPLETE_ALL_F2P_QUESTS, bot, analysis));
        goals.add(createGoal(GoalType.COMPLETE_ALL_P2P_QUESTS, bot, analysis));
        goals.add(createGoal(GoalType.GET_QUEST_CAPE, bot, analysis));
        
        // Important quest rewards
        goals.add(createGoal(GoalType.COMPLETE_DESERT_TREASURE, bot, analysis));
        goals.add(createGoal(GoalType.COMPLETE_RECIPE_DISASTER, bot, analysis));
        goals.add(createGoal(GoalType.GET_BARROWS_GLOVES, bot, analysis));
        goals.add(createGoal(GoalType.UNLOCK_ANCIENT_MAGICKS, bot, analysis));
        
        // Achievement goals
        goals.add(createGoal(GoalType.COMPLETE_ACHIEVEMENT_DIARIES, bot, analysis));
        
        return goals;
    }
    
    /**
     * Generate goals for Collector archetype bots
     */
    private static List<Goal> generateCollectorGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        
        // Collection goals
        goals.add(createGoal(GoalType.GET_ALL_SKILLCAPES, bot, analysis));
        goals.add(createGoal(GoalType.COLLECT_ALL_PETS, bot, analysis));
        goals.add(createGoal(GoalType.OWN_EVERY_WEAPON, bot, analysis));
        goals.add(createGoal(GoalType.OWN_EVERY_ARMOR, bot, analysis));
        goals.add(createGoal(GoalType.COLLECT_HOLIDAY_ITEMS, bot, analysis));
        goals.add(createGoal(GoalType.COLLECT_DISCONTINUED_ITEMS, bot, analysis));
        
        // Need money for collecting
        if (analysis.bankValue < 1000000000) {
            goals.add(createGoal(GoalType.BUILD_1B_BANK, bot, analysis));
        }
        
        return goals;
    }
    
    /**
     * Generate goals for Balanced/Hybrid archetype bots
     */
    private static List<Goal> generateBalancedGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        
        // Mix of everything based on current progress
        int totalLevel = analysis.totalLevel;
        int combatLevel = analysis.combatLevel;
        
        // Early game: basic progression
        if (totalLevel < 500) {
            goals.add(createGoal(GoalType.TOTAL_LEVEL_1000, bot, analysis));
            goals.add(createGoal(GoalType.BUILD_1M_BANK, bot, analysis));
        }
        
        // Mid game: specialization
        if (totalLevel < 1500) {
            goals.add(createGoal(GoalType.TRAIN_ATTACK_99, bot, analysis));
            goals.add(createGoal(GoalType.SKILL_WOODCUTTING_99, bot, analysis));
            goals.add(createGoal(GoalType.GET_BARROWS_ARMOR, bot, analysis));
            goals.add(createGoal(GoalType.BUILD_10M_BANK, bot, analysis));
        }
        
        // Late game: max out
        if (totalLevel < 2000) {
            goals.add(createGoal(GoalType.TOTAL_LEVEL_2000, bot, analysis));
            goals.add(createGoal(GoalType.MAX_COMBAT_STATS, bot, analysis));
            goals.add(createGoal(GoalType.BUILD_100M_BANK, bot, analysis));
        }
        
        // End game: completionist
        if (totalLevel >= 2000) {
            goals.add(createGoal(GoalType.MAX_TOTAL_LEVEL, bot, analysis));
            goals.add(createGoal(GoalType.GET_MAX_CAPE, bot, analysis));
            goals.add(createGoal(GoalType.BUILD_1B_BANK, bot, analysis));
        }
        
        return goals;
    }
    
    /**
     * Generate default goals for unknown archetypes
     */
    private static List<Goal> generateDefaultGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        
        // Safe defaults - basic progression goals
        goals.add(createGoal(GoalType.BUILD_1M_BANK, bot, analysis));
        goals.add(createGoal(GoalType.TRAIN_ATTACK_99, bot, analysis));
        goals.add(createGoal(GoalType.SKILL_WOODCUTTING_99, bot, analysis));
        goals.add(createGoal(GoalType.GET_RUNE_ARMOR, bot, analysis));
        
        return goals;
    }
    
    /**
     * Generate universal goals that any bot might want
     */
    /**
     * Tier-0/1 checkpoint goals - the missing link between 'level 3 bot'
     * and 'aspirational 99'. Adds combat 30/50/70/90 + per-skill 30/50/70
     * + small bank milestones (50K -> 1M). The isAchievable filter that
     * runs after this strips any checkpoints the bot has already passed,
     * so a cb-50 bot only sees combat 70/90 (not 30/50 which it's done).
     *
     * This is what lets a low-level bot have a meaningful 'next thing
     * to do' instead of grinding hopelessly toward Bandos for 100 hours.
     */
    private static List<Goal> generateTierCheckpointGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        int cb = analysis.combatLevel;
        boolean skiller = isSkiller(bot);
        // Combat-level + per-combat-skill checkpoints - skip entirely for skillers
        if (!skiller) {
            if (cb < 30)  goals.add(createGoal(GoalType.REACH_COMBAT_30, bot, analysis));
            if (cb < 50)  goals.add(createGoal(GoalType.REACH_COMBAT_50, bot, analysis));
            if (cb < 70)  goals.add(createGoal(GoalType.REACH_COMBAT_70, bot, analysis));
            if (cb < 90)  goals.add(createGoal(GoalType.REACH_COMBAT_90, bot, analysis));
            addSkillCheckpoints(goals, bot, analysis, Skills.ATTACK,
                GoalType.TRAIN_ATTACK_30, GoalType.TRAIN_ATTACK_50, GoalType.TRAIN_ATTACK_70);
            addSkillCheckpoints(goals, bot, analysis, Skills.STRENGTH,
                GoalType.TRAIN_STRENGTH_30, GoalType.TRAIN_STRENGTH_50, GoalType.TRAIN_STRENGTH_70);
            addSkillCheckpoints(goals, bot, analysis, Skills.DEFENCE,
                GoalType.TRAIN_DEFENCE_30, GoalType.TRAIN_DEFENCE_50, GoalType.TRAIN_DEFENCE_70);
            addSkillCheckpoints(goals, bot, analysis, Skills.HITPOINTS,
                GoalType.TRAIN_HITPOINTS_30, GoalType.TRAIN_HITPOINTS_50, null);
            addSkillCheckpoints(goals, bot, analysis, Skills.RANGE,
                null, GoalType.TRAIN_RANGED_50, GoalType.TRAIN_RANGED_70);
            addSkillCheckpoints(goals, bot, analysis, Skills.MAGIC,
                null, GoalType.TRAIN_MAGIC_50, null);
        }
        // Gathering skills
        addSkillCheckpoints(goals, bot, analysis, Skills.MINING,
            GoalType.TRAIN_MINING_30, GoalType.TRAIN_MINING_50, GoalType.TRAIN_MINING_70);
        addSkillCheckpoints(goals, bot, analysis, Skills.WOODCUTTING,
            GoalType.TRAIN_WC_30, GoalType.TRAIN_WC_50, GoalType.TRAIN_WC_70);
        addSkillCheckpoints(goals, bot, analysis, Skills.FISHING,
            GoalType.TRAIN_FISHING_30, GoalType.TRAIN_FISHING_50, GoalType.TRAIN_FISHING_70);
        addSkillCheckpoints(goals, bot, analysis, Skills.THIEVING,
            GoalType.TRAIN_THIEVING_30, GoalType.TRAIN_THIEVING_50, null);
        // Small bank checkpoints - so new bots have a real wealth target
        if (analysis.bankValue < 50000)  goals.add(createGoal(GoalType.BUILD_50K_BANK, bot, analysis));
        if (analysis.bankValue < 100000) goals.add(createGoal(GoalType.BUILD_100K_BANK, bot, analysis));
        if (analysis.bankValue < 500000) goals.add(createGoal(GoalType.BUILD_500K_BANK, bot, analysis));
        return goals;
    }

    private static void addSkillCheckpoints(List<Goal> goals, AIPlayer bot, BotAnalysis analysis,
                                            int skill, GoalType lvl30, GoalType lvl50, GoalType lvl70) {
        int level = analysis.getSkillLevel(skill);
        if (lvl30 != null && level < 30) goals.add(createGoal(lvl30, bot, analysis));
        if (lvl50 != null && level < 50) goals.add(createGoal(lvl50, bot, analysis));
        if (lvl70 != null && level < 70) goals.add(createGoal(lvl70, bot, analysis));
    }

    private static List<Goal> generateUniversalGoals(AIPlayer bot, BotAnalysis analysis) {
        List<Goal> goals = new ArrayList<>();
        boolean skiller = isSkiller(bot);

        // Financial security - applies to all
        if (analysis.bankValue < 1000000) {
            goals.add(createGoal(GoalType.BUILD_1M_BANK, bot, analysis));
        }

        // Combat-only universal goals - skip for skillers
        if (!skiller) {
            if (analysis.combatLevel > 40 && !analysis.hasEquipment("rune")) {
                goals.add(createGoal(GoalType.GET_RUNE_ARMOR, bot, analysis));
            }
            if (analysis.combatLevel > 80) {
                goals.add(createGoal(GoalType.GET_FIRE_CAPE, bot, analysis));
            }
        }

        return goals;
    }
    
    /**
     * Prioritize goals based on bot's current situation
     */
    private static List<Goal> prioritizeGoals(List<Goal> goals, BotAnalysis analysis) {
        // Sort goals by priority, urgency, and logical progression
        goals.sort((a, b) -> {
            // Higher priority goals first
            int priorityCompare = Double.compare(b.getUrgency(), a.getUrgency());
            if (priorityCompare != 0) return priorityCompare;
            
            // Shorter goals first (quick wins)
            return Long.compare(a.getEstimatedTime(), b.getEstimatedTime());
        });
        
        // Cap goal pool. Combat archetypes now bring ~5 combat goals + ~9
        // support skill goals + ~4 equipment + checkpoints, so we widen
        // the cap from 10 to 20 to keep both primary and secondary goals
        // visible. Lifetime alignment bias still puts primary on top.
        int maxGoals = 20;
        if (goals.size() > maxGoals) {
            goals = goals.subList(0, maxGoals);
        }
        
        return goals;
    }
    
    /**
     * Create a Goal object from a GoalType
     */
    private static Goal createGoal(GoalType goalType, AIPlayer bot, BotAnalysis analysis) {
        try {
            String goalId = generateGoalId(goalType, bot);
            Goal goal = new Goal(goalId, goalType.getDescription(), goalType.getDefaultPriority(),
                               goalType.getCategory(), goalType.getEstimatedTime(), 
                               calculateReward(goalType, analysis), true);
            
            // Set goal-specific data
            goal.setData("goalType", goalType);
            goal.setData("botArchetype", bot.getArchetype());
            goal.setData("requirementKey", goalType.getRequirementKey());
            goal.setData("requirementValue", goalType.getRequirementValue());
            
            return goal;
        } catch (Exception e) {
            System.err.println("[ArchetypeGoalGenerator] Failed to create goal " + goalType + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate unique goal ID
     */
    private static String generateGoalId(GoalType goalType, AIPlayer bot) {
        return bot.getDisplayName() + "_" + goalType.name() + "_" + System.currentTimeMillis();
    }
    
    /**
     * Calculate reward value for a goal based on bot's situation
     */
    private static double calculateReward(GoalType goalType, BotAnalysis analysis) {
        double baseReward = goalType.getDefaultPriority().getWeight() * 10.0;
        
        // Adjust reward based on how much the goal helps the bot
        switch (goalType.getCategory()) {
            case ECONOMIC:
                // More valuable if bot is poor
                if (analysis.bankValue < 10000000) {
                    baseReward *= 1.5;
                }
                break;
            case COMBAT:
                // More valuable if bot has low combat
                if (analysis.combatLevel < 100) {
                    baseReward *= 1.3;
                }
                break;
            case SKILL:
                // More valuable if bot has low total level
                if (analysis.totalLevel < 1500) {
                    baseReward *= 1.2;
                }
                break;
        }
        
        return baseReward;
    }
    
    /**
     * Get the appropriate 99 skill goal for a skill ID
     */
    private static GoalType getSkill99GoalType(int skillId) {
        switch (skillId) {
            case Skills.ATTACK: return GoalType.TRAIN_ATTACK_99;
            case Skills.STRENGTH: return GoalType.TRAIN_STRENGTH_99;
            case Skills.DEFENCE: return GoalType.TRAIN_DEFENCE_99;
            case Skills.RANGE: return GoalType.TRAIN_RANGED_99;
            case Skills.PRAYER: return GoalType.TRAIN_PRAYER_99;
            case Skills.MAGIC: return GoalType.TRAIN_MAGIC_99;
            case Skills.WOODCUTTING: return GoalType.SKILL_WOODCUTTING_99;
            case Skills.MINING: return GoalType.SKILL_MINING_99;
            case Skills.SMITHING: return GoalType.SKILL_SMITHING_99;
            case Skills.FISHING: return GoalType.SKILL_FISHING_99;
            case Skills.COOKING: return GoalType.SKILL_COOKING_99;
            case Skills.FIREMAKING: return GoalType.SKILL_FIREMAKING_99;
            case Skills.CRAFTING: return GoalType.SKILL_CRAFTING_99;
            case Skills.FLETCHING: return GoalType.SKILL_FLETCHING_99;
            case Skills.RUNECRAFTING: return GoalType.SKILL_RUNECRAFTING_99;
            case Skills.HERBLORE: return GoalType.SKILL_HERBLORE_99;
            case Skills.AGILITY: return GoalType.SKILL_AGILITY_99;
            case Skills.THIEVING: return GoalType.SKILL_THIEVING_99;
            case Skills.SLAYER: return GoalType.SKILL_SLAYER_99;
            case Skills.FARMING: return GoalType.SKILL_FARMING_99;
            case Skills.CONSTRUCTION: return GoalType.SKILL_CONSTRUCTION_99;
            case Skills.HUNTER: return GoalType.SKILL_HUNTER_99;
            case Skills.SUMMONING: return GoalType.TRAIN_SUMMONING_99;
            default: return null;
        }
    }
    
    /**
     * Check if a skill is a combat skill
     */
    private static boolean isCombatStat(int skillId) {
        return skillId == Skills.ATTACK || skillId == Skills.STRENGTH || 
               skillId == Skills.DEFENCE || skillId == Skills.RANGE || 
               skillId == Skills.MAGIC || skillId == Skills.PRAYER ||
               skillId == Skills.SUMMONING;
    }
    
    /**
     * Check if bot is a pure skiller
     */
    private static boolean isSkiller(AIPlayer bot) {
        return "skiller".equalsIgnoreCase(bot.getArchetype());
    }
    
    /**
     * Analyze a bot's current state
     */
    private static BotAnalysis analyzeBot(AIPlayer bot) {
        BotAnalysis analysis = new BotAnalysis();
        
        // Get skill levels
        for (int i = 0; i < Skills.SKILL_NAME.length; i++) {
            analysis.skillLevels[i] = bot.getSkills().getLevel(i);
        }
        
        analysis.combatLevel = bot.getSkills().getCombatLevel();
        analysis.totalLevel = bot.getSkills().getTotalLevel();
        
        // Estimate bank value (simplified)
        analysis.bankValue = estimateBankValue(bot);
        
        // Check equipment (simplified)
        analysis.equipment = new HashMap<>();
        analyzeEquipment(bot, analysis);
        
        return analysis;
    }
    
    /**
     * Estimate bot's bank value
     */
    private static long estimateBankValue(AIPlayer bot) {
        // Simplified bank value estimation
        // In a full implementation, this would check actual inventory/bank items
        int combatLevel = bot.getSkills().getCombatLevel();
        int totalLevel = bot.getSkills().getTotalLevel();
        
        // Rough estimation based on levels
        long estimatedValue = (long)(combatLevel * 100000) + (long)(totalLevel * 50000);
        
        return Math.max(estimatedValue, 100000); // Minimum 100k
    }
    
    /**
     * Analyze bot's current equipment
     */
    private static void analyzeEquipment(AIPlayer bot, BotAnalysis analysis) {
        // Simplified equipment analysis
        // In a full implementation, this would check actual equipment slots
        int combatLevel = bot.getSkills().getCombatLevel();
        
        // Estimate equipment based on combat level
        if (combatLevel >= 40) analysis.equipment.put("rune", true);
        if (combatLevel >= 60) analysis.equipment.put("dragon", true);
        if (combatLevel >= 70 && analysis.bankValue > 20000000) {
            analysis.equipment.put("barrows", true);
        }
        if (combatLevel >= 70 && analysis.bankValue > 100000000) {
            analysis.equipment.put("bandos", true);
        }
        
        // Weapons
        if (combatLevel >= 70 && analysis.bankValue > 5000000) {
            analysis.equipment.put("whip", true);
        }
    }
    
    /**
     * Bot analysis data structure
     */
    private static class BotAnalysis {
        int[] skillLevels = new int[Skills.SKILL_NAME.length];
        int combatLevel;
        int totalLevel;
        long bankValue;
        Map<String, Boolean> equipment = new HashMap<>();
        
        int getSkillLevel(int skillId) {
            if (skillId >= 0 && skillId < skillLevels.length) {
                return skillLevels[skillId];
            }
            return 1;
        }
        
        boolean hasEquipment(String type) {
            return equipment.getOrDefault(type, false);
        }
    }
}
