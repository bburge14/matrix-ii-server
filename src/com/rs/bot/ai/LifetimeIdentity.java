package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.utils.Utils;

/**
 * Long-term north-star identity per bot. Set once at bot creation, persists
 * for the bot's lifetime. Biases active-goal selection so bots feel like
 * different "real players" - one is grinding cash, one is going for max
 * combat, one just wants to skill, etc.
 *
 * Without this, every bot's goal pool was driven by raw GP-rank or skill-
 * level diff, which converged on the same tiny set. With this, bots
 * naturally diverge across the world even with identical stats.
 */
public enum LifetimeIdentity {
    COMBAT_MAXER ("becoming the strongest fighter"),
    CASH_STACKER ("getting filthy rich"),
    SKILL_MAXER  ("getting all 99s"),
    BOSS_HUNTER  ("hunting every boss in the game"),
    PVP_LORD     ("ruling the wilderness"),
    COMPLETIONIST("100%-ing the game"),
    PURE_SKILLER ("a true non-combat skiller"),
    CASUAL       ("just having fun, no rush");

    public final String label;
    LifetimeIdentity(String l) { this.label = l; }

    /**
     * Pick a lifetime identity for a bot at creation time. Biased by the
     * bot's archetype - combat archetypes lean toward combat identities,
     * skillers toward skill identities, etc., but with enough randomness
     * that some bots break the mold.
     */
    public static LifetimeIdentity pickFor(String archetype) {
        if (archetype == null) archetype = "main";
        archetype = archetype.toLowerCase();
        int roll = Utils.random(100);
        switch (archetype) {
            case "skiller":
                return roll < 70 ? PURE_SKILLER
                     : roll < 90 ? SKILL_MAXER
                     : CASUAL;
            case "ranged":
            case "magic":
            case "melee":
            case "main":
            case "tank":
            case "hybrid":
            case "pure":
                return roll < 30 ? COMBAT_MAXER
                     : roll < 50 ? BOSS_HUNTER
                     : roll < 65 ? PVP_LORD
                     : roll < 80 ? CASH_STACKER
                     : roll < 95 ? COMPLETIONIST
                     : CASUAL;
            case "maxed":
                return roll < 30 ? BOSS_HUNTER
                     : roll < 60 ? PVP_LORD
                     : roll < 85 ? COMPLETIONIST
                     : CASUAL;
            case "f2p":
                return roll < 50 ? CASH_STACKER
                     : roll < 80 ? COMBAT_MAXER
                     : CASUAL;
            default:
                // Truly random fallback
                LifetimeIdentity[] all = values();
                return all[Utils.random(all.length)];
        }
    }

    /**
     * Score boost a goal gets if it aligns with this identity. The
     * goal-ranking pipeline adds this to the base score so aligned
     * goals naturally win the tiebreakers.
     */
    public int alignmentBoost(GoalType type) {
        if (type == null) return 0;
        String key = type.getRequirementKey();
        if (key == null) return 0;
        switch (this) {
            case COMBAT_MAXER:
                if (key.startsWith("combat:") || key.equals("skill:attack")
                    || key.equals("skill:strength") || key.equals("skill:defence")
                    || key.equals("skill:hitpoints")) return 50;
                return 0;
            case CASH_STACKER:
                if (key.startsWith("wealth:")) return 80;
                return 0;
            case SKILL_MAXER:
                if (key.startsWith("skill:")) return 40;
                return 0;
            case BOSS_HUNTER:
                if (key.startsWith("boss:") || key.startsWith("kill:")) return 100;
                if (key.startsWith("equipment:") || key.startsWith("weapon:")) return 30;
                return 0;
            case PVP_LORD:
                if (key.startsWith("combat:")) return 40;
                if (key.startsWith("equipment:") || key.startsWith("weapon:")) return 30;
                return 0;
            case COMPLETIONIST:
                if (key.startsWith("quest:") || key.startsWith("collection:")) return 60;
                if (key.startsWith("skill:")) return 20;
                return 0;
            case PURE_SKILLER:
                if (key.startsWith("skill:") && !isCombatSkill(key)) return 80;
                if (isCombatSkill(key)) return -100;  // negative - actively avoid
                return 0;
            case CASUAL:
                return 0;
        }
        return 0;
    }

    private static boolean isCombatSkill(String key) {
        return key.equals("skill:attack") || key.equals("skill:strength")
            || key.equals("skill:defence") || key.equals("skill:hitpoints")
            || key.equals("skill:ranged") || key.equals("skill:magic")
            || key.equals("skill:prayer") || key.equals("combat:max")
            || key.startsWith("combat:");
    }
}
