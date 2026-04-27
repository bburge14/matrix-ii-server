package com.rs.bot;

import com.rs.game.player.Skills;
import com.rs.utils.Utils;

/**
 * Computes a 26-element skill level array for a bot based on a target combat level + mode + archetype.
 *
 * Modes:
 *   "default"        - vanilla starter (lvl 1, HP 10, Herblore 3) - returns null
 *   "random_skills"  - every skill 1-99 random (combat ends up wherever it lands)
 *   "set"            - target combat level via the chosen archetype + randomized non-combat skills
 *   "random_combat"  - random target 3-126 then "set"
 *
 * Archetypes (only used when mode=set or random_combat):
 *   "random"  - server picks one of the combat archetypes
 *   "melee", "ranged", "magic", "hybrid", "tank", "pure", "main"  - target combat level
 *   "skiller" - combat stays minimal (cb 3), non-combat skills high (ignores target)
 *   "f2p"     - F2P skills only, balanced combat
 *   "maxed"   - all 99 across the board (ignores target)
 */
public final class BotSkillProfile {

    private BotSkillProfile() {}

    private static final String[] COMBAT_ARCHETYPES = {
        "melee", "ranged", "magic", "hybrid", "tank", "pure", "main"
    };

    // F2P-trainable skills (everything else is members-only and stays at level 1 for f2p archetype)
    // ATTACK, DEFENCE, STRENGTH, HITPOINTS, RANGE, PRAYER, MAGIC, COOKING, WOODCUTTING, FISHING,
    // FIREMAKING, CRAFTING, SMITHING, MINING, RUNECRAFTING
    private static final int[] F2P_SKILLS = {
        Skills.ATTACK, Skills.DEFENCE, Skills.STRENGTH, Skills.HITPOINTS,
        Skills.RANGE, Skills.PRAYER, Skills.MAGIC,
        Skills.COOKING, Skills.WOODCUTTING, Skills.FISHING, Skills.FIREMAKING,
        Skills.CRAFTING, Skills.SMITHING, Skills.MINING, Skills.RUNECRAFTING
    };

    public static int[] build(String mode, int targetCombat, String archetype) {
        if (mode == null) mode = "default";
        if (archetype == null || archetype.isEmpty()) archetype = "random";
        archetype = archetype.toLowerCase();

        if ("default".equalsIgnoreCase(mode)) return null;

        int[] levels = baselineLevels();

        if ("random_skills".equalsIgnoreCase(mode)) {
            for (int i = 0; i < levels.length; i++) {
                if (i == Skills.HITPOINTS) levels[i] = 10 + Utils.random(90);
                else if (i == Skills.HERBLORE) levels[i] = 3 + Utils.random(97);
                else levels[i] = 1 + Utils.random(99);
            }
            return levels;
        }

        // Resolve "random" archetype to an actual one
        if ("random".equals(archetype)) {
            archetype = COMBAT_ARCHETYPES[Utils.random(COMBAT_ARCHETYPES.length)];
        }

        int target;
        if ("random_combat".equalsIgnoreCase(mode)) target = 3 + Utils.random(124);
        else target = Math.max(3, Math.min(138, targetCombat));

        // Special archetypes that don't care about target combat
        if ("skiller".equals(archetype)) return buildSkiller(levels);
        if ("maxed".equals(archetype))   return buildMaxed(levels);
        if ("f2p".equals(archetype))     return buildF2P(levels, target);

        // Combat archetypes: randomize non-combat skills first, then tune combat to hit target
        randomizeNonCombatSkills(levels);
        applyCombatArchetype(levels, archetype, target);
        return levels;
    }

    /** Backwards-compat overload (random archetype). */
    public static int[] build(String mode, int targetCombat) {
        return build(mode, targetCombat, "random");
    }

    // ===== Special archetype builders =====

    private static int[] buildSkiller(int[] levels) {
        // Classic "level 3 skiller": all combat skills 1 (HP 10), but non-combat skills 1-99 random
        // (with bias toward higher levels to feel like a dedicated skiller account)
        levels[Skills.ATTACK] = 1;
        levels[Skills.DEFENCE] = 1;
        levels[Skills.STRENGTH] = 1;
        levels[Skills.HITPOINTS] = 10;
        levels[Skills.RANGE] = 1;
        levels[Skills.PRAYER] = 1;
        levels[Skills.MAGIC] = 1;
        levels[Skills.SUMMONING] = 1;
        // Non-combat: bias toward 50-99 with occasional outliers
        for (int i = 0; i < 26; i++) {
            if (isCombatSkill(i)) continue;
            // 70% chance high (50-99), 30% chance low-mid (1-50)
            if (Utils.random(10) < 7) levels[i] = 50 + Utils.random(50);
            else levels[i] = 1 + Utils.random(50);
        }
        // Herblore special floor
        if (levels[Skills.HERBLORE] < 3) levels[Skills.HERBLORE] = 3;
        return levels;
    }

    private static int[] buildMaxed(int[] levels) {
        for (int i = 0; i < 26; i++) levels[i] = 99;
        return levels;
    }

    private static int[] buildF2P(int[] levels, int target) {
        // All non-F2P skills stay at default (1, or HP 10, or Herb 3)
        // F2P skills get random levels with combat tuned to target
        // First, randomize F2P non-combat skills
        for (int sk : F2P_SKILLS) {
            if (sk == Skills.ATTACK || sk == Skills.DEFENCE || sk == Skills.STRENGTH
                    || sk == Skills.HITPOINTS || sk == Skills.RANGE
                    || sk == Skills.PRAYER || sk == Skills.MAGIC) continue;
            levels[sk] = 1 + Utils.random(99);
        }
        // Combat: tune as melee for simplicity (F2P combat is mostly melee-focused)
        applyCombatArchetype(levels, "melee", target);
        return levels;
    }

    // ===== Combat archetype tuning =====

    private static void randomizeNonCombatSkills(int[] levels) {
        for (int i = 0; i < 26; i++) {
            if (isCombatSkill(i)) continue;
            if (i == Skills.HERBLORE) levels[i] = 3 + Utils.random(97);
            else levels[i] = 1 + Utils.random(99);
        }
    }

    private static boolean isCombatSkill(int i) {
        return i == Skills.ATTACK || i == Skills.DEFENCE || i == Skills.STRENGTH
            || i == Skills.HITPOINTS || i == Skills.RANGE
            || i == Skills.PRAYER || i == Skills.MAGIC || i == Skills.SUMMONING;
    }

    private static void applyCombatArchetype(int[] levels, String arch, int target) {
        int hp = 10 + Utils.random(40);
        int prayer = 1 + Utils.random(Utils.random(4) == 0 ? 60 : 25);

        if ("melee".equals(arch))      tuneMelee(levels, hp, prayer, target);
        else if ("ranged".equals(arch)) tuneRanged(levels, hp, prayer, target);
        else if ("magic".equals(arch))  tuneMagic(levels, hp, prayer, target);
        else if ("hybrid".equals(arch)) tuneHybrid(levels, hp, prayer, target, Utils.random(2) == 0);
        else if ("tank".equals(arch))   tuneTank(levels, hp, prayer, target);
        else if ("pure".equals(arch))   tunePure(levels, hp, prayer, target);
        else if ("main".equals(arch))   tuneMain(levels, hp, prayer, target);
        else                            tuneMelee(levels, hp, prayer, target);  // default fallback
    }

    private static void tuneMelee(int[] levels, int hp, int prayer, int target) {
        for (int L = 1; L <= 99; L++) {
            int c = computeCombat(L, L, L, hp, prayer, 1, 1);
            if (c >= target) { set(levels, L, L, L, hp, prayer, 1, 1); return; }
        }
        scaleHpThenPrayer(levels, 99, 99, 99, hp, prayer, 1, 1, target);
    }

    private static void tuneRanged(int[] levels, int hp, int prayer, int target) {
        int att = 1, str = 1, mag = 1;
        for (int L = 1; L <= 99; L++) {
            int def = Math.max(1, L / 2);
            int c = computeCombat(att, str, def, hp, prayer, L, mag);
            if (c >= target) { set(levels, att, str, def, hp, prayer, L, mag); return; }
        }
        scaleHpThenPrayer(levels, att, str, 99, hp, prayer, 99, mag, target);
    }

    private static void tuneMagic(int[] levels, int hp, int prayer, int target) {
        int att = 1, str = 1, rng = 1;
        for (int L = 1; L <= 99; L++) {
            int def = Math.max(1, L / 2);
            int c = computeCombat(att, str, def, hp, prayer, rng, L);
            if (c >= target) { set(levels, att, str, def, hp, prayer, rng, L); return; }
        }
        scaleHpThenPrayer(levels, att, str, 99, hp, prayer, rng, 99, target);
    }

    private static void tuneHybrid(int[] levels, int hp, int prayer, int target, boolean magicSecondary) {
        for (int L = 1; L <= 99; L++) {
            int secondary = Math.max(1, (L * (60 + Utils.random(20))) / 100);
            int rng = magicSecondary ? 1 : secondary;
            int mag = magicSecondary ? secondary : 1;
            int c = computeCombat(L, L, L, hp, prayer, rng, mag);
            if (c >= target) { set(levels, L, L, L, hp, prayer, rng, mag); return; }
        }
        int rng = magicSecondary ? 1 : 99;
        int mag = magicSecondary ? 99 : 1;
        scaleHpThenPrayer(levels, 99, 99, 99, hp, prayer, rng, mag, target);
    }

    private static void tuneTank(int[] levels, int hp, int prayer, int target) {
        for (int L = 1; L <= 99; L++) {
            int off = Math.max(1, (L * 70) / 100);
            int c = computeCombat(off, off, L, hp, prayer, 1, 1);
            if (c >= target) { set(levels, off, off, L, hp, prayer, 1, 1); return; }
        }
        scaleHpThenPrayer(levels, 70, 70, 99, hp, prayer, 1, 1, target);
    }

    private static void tunePure(int[] levels, int hp, int prayer, int target) {
        // 1 defence pure: max attack/strength, defence stays at 1
        int def = 1;
        for (int L = 1; L <= 99; L++) {
            int c = computeCombat(L, L, def, hp, prayer, 1, 1);
            if (c >= target) { set(levels, L, L, def, hp, prayer, 1, 1); return; }
        }
        scaleHpThenPrayer(levels, 99, 99, def, hp, prayer, 1, 1, target);
    }

    private static void tuneMain(int[] levels, int hp, int prayer, int target) {
        // Balanced: all combat styles trained, melee leads
        for (int L = 1; L <= 99; L++) {
            int rng = Math.max(1, (L * (50 + Utils.random(30))) / 100);
            int mag = Math.max(1, (L * (50 + Utils.random(30))) / 100);
            int c = computeCombat(L, L, L, hp, prayer, rng, mag);
            if (c >= target) { set(levels, L, L, L, hp, prayer, rng, mag); return; }
        }
        scaleHpThenPrayer(levels, 99, 99, 99, hp, prayer, 80, 80, target);
    }

    private static void scaleHpThenPrayer(int[] levels, int att, int str, int def,
                                          int hp, int prayer, int rng, int mag, int target) {
        for (int H = hp; H <= 99; H++) {
            int c = computeCombat(att, str, def, H, prayer, rng, mag);
            if (c >= target) { set(levels, att, str, def, H, prayer, rng, mag); return; }
            hp = H;
        }
        for (int P = prayer; P <= 99; P++) {
            int c = computeCombat(att, str, def, hp, P, rng, mag);
            if (c >= target) { set(levels, att, str, def, hp, P, rng, mag); return; }
            prayer = P;
        }
        set(levels, att, str, def, hp, prayer, rng, mag);
    }

    private static void set(int[] levels, int att, int str, int def, int hp, int pray, int rng, int mag) {
        levels[Skills.ATTACK]    = att;
        levels[Skills.STRENGTH]  = str;
        levels[Skills.DEFENCE]   = def;
        levels[Skills.HITPOINTS] = hp;
        levels[Skills.PRAYER]    = pray;
        levels[Skills.RANGE]     = rng;
        levels[Skills.MAGIC]     = mag;
    }

    private static int computeCombat(int attack, int strength, int defence,
                                     int hitpoints, int prayer, int ranged, int magic) {
        double combatLevel = (defence + hitpoints + Math.floor(prayer / 2.0)) * 0.25;
        double warrior = (attack + strength) * 0.325;
        double ranger = ranged * 0.4875;
        double mage = magic * 0.4875;
        combatLevel += Math.max(warrior, Math.max(ranger, mage));
        return (int) combatLevel;
    }

    private static int[] baselineLevels() {
        int[] levels = new int[26];
        for (int i = 0; i < 26; i++) levels[i] = 1;
        levels[Skills.HITPOINTS] = 10;
        levels[Skills.HERBLORE] = 3;
        return levels;
    }
}
