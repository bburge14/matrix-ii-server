package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.game.player.CombatDefinitions;
import com.rs.game.player.Skills;

/**
 * Bot combat helper - wraps prayer activation, combat-mode selection, and
 * ability-bar setup so a bot fights with appropriate depth for its level.
 *
 * Design split: Legacy mode (no abilities, just auto-attack) is forced for
 * lowbie bots since they wouldn't have abilities anyway and PlayerCombatNew
 * already handles legacy melee/ranged/magic auto-attacks. Higher-level bots
 * (cb >= 60 ish) flip to Revolution mode so the game auto-cycles whatever
 * abilities are on their action bar.
 *
 * Prayer activation tier-walks based on prayer level + combat style:
 *   melee:  Burst of Strength -> Superhuman -> Ultimate -> Piety (lvl 70)
 *   ranged: Sharp Eye -> Hawk Eye -> Eagle Eye -> Rigour (lvl 70)
 *   magic:  Mystic Will -> Mystic Lore -> Mystic Might -> Augury (lvl 70)
 *
 * Throttled by the brain's reaction-delay gate so it doesn't spam.
 */
public final class BotCombat {

    private BotCombat() {}

    /** Bot's preferred combat style derived from archetype. */
    public enum Style { MELEE, RANGED, MAGIC }

    public static Style styleFor(AIPlayer bot) {
        String a = bot.getArchetype() == null ? "main" : bot.getArchetype().toLowerCase();
        switch (a) {
            case "ranged": return Style.RANGED;
            case "magic":  return Style.MAGIC;
            case "melee":
            case "tank":
            case "pure":
            case "main":
            case "hybrid":
            case "combatant":
            case "f2p":
            case "maxed":
            default:
                return Style.MELEE;
        }
    }

    /**
     * Pre-fight: switch combat mode + activate best prayer for the bot's
     * style and prayer level. Idempotent - safe to call every combat tick.
     */
    public static void preparePreFight(AIPlayer bot) {
        try {
            applyCombatMode(bot);
        } catch (Throwable ignored) {}
        try {
            activateBestPrayer(bot);
        } catch (Throwable ignored) {}
    }

    /**
     * Switch the bot to Revolution mode if their combat level is high
     * enough to benefit from abilities, otherwise leave them in Legacy.
     * Revolution auto-fires abilities from the action bar so we don't
     * have to manually queue them. Legacy is fine for lowbies.
     */
    private static void applyCombatMode(AIPlayer bot) {
        int cb = bot.getSkills().getCombatLevel();
        CombatDefinitions cd = bot.getCombatDefinitions();
        if (cd == null) return;
        // Sub-cb-60 bots can't really use abilities effectively (cooldowns
        // outpace auto-attacks at low gear). Stay in Legacy.
        if (cb < 60) {
            if (cd.getCombatMode() != CombatDefinitions.LEGACY_COMBAT_MODE) {
                if (!bot.isLegacyMode()) bot.switchLegacyMode();
                cd.setCombatMode(CombatDefinitions.LEGACY_COMBAT_MODE);
            }
            return;
        }
        // cb 60+: switch off legacy and into Revolution so abilities auto-cycle.
        if (cd.getCombatMode() != CombatDefinitions.REVOLUTION_COMBAT_MODE) {
            if (bot.isLegacyMode()) bot.switchLegacyMode();
            cd.setCombatMode(CombatDefinitions.REVOLUTION_COMBAT_MODE);
        }
    }

    /**
     * Activate the best prayer the bot's prayer level supports for its
     * style. Picks ONE prayer per style tier - Piety/Rigour/Augury at
     * 70+, mid-tier at 25-69, basic at 1-24. No-ops if the bot is
     * already using a prayer of that tier.
     */
    private static void activateBestPrayer(AIPlayer bot) {
        int prayerLvl = bot.getSkills().getLevelForXp(Skills.PRAYER);
        int prayerPts = bot.getPrayer().getPrayerpoints();
        // Don't bother turning on prayers if we have no points - waste of
        // a tick. Top up at altars/restore pots happens elsewhere.
        if (prayerPts < 5) return;
        Style style = styleFor(bot);
        int prayerId = pickPrayerForStyle(style, prayerLvl);
        if (prayerId < 0) return;
        // Skip if already on - usingPrayer(book, id) is true if active.
        try {
            if (bot.getPrayer().usingPrayer(0, prayerId)) return;
        } catch (Throwable ignored) {}
        try {
            bot.getPrayer().switchPrayer(prayerId, false);
        } catch (Throwable ignored) {}
    }

    /**
     * Map style + prayer level to the best ID in the normal prayer book.
     * Prayer indices match prayerLvls[0]:
     *   0=Thick Skin(1) 1=Burst of Str(4) 2=Clarity(7) 3=Sharp Eye(8)
     *   4=Mystic Will(8) 5=Steel Skin(9) 6=Hawk Eye(9) 7=Mystic Lore(19)
     *   8=Rock Skin(22) 9=Superhuman Str(25) 10=ProtectItem(35)
     *   11=Eagle Eye(37) 12=Mystic Might(40) 13=Protect Magic/Range/Melee(43-49)
     *   14=Protect Magic(43) 15=Protect Range(46) 16=Protect Melee(49)
     *   17=Chivalry(60) 18=Sharing(65) 19=Piety(70) 20=Rigour(70) 21=Augury(70)
     *
     * The exact mapping varies by server build; we tier-walk and let the
     * usePrayer() level check inside Prayer.java reject any that don't
     * actually unlock at the bot's level.
     */
    private static int pickPrayerForStyle(Style style, int level) {
        switch (style) {
            case MELEE:
                if (level >= 70) return 19; // Piety
                if (level >= 60) return 17; // Chivalry
                if (level >= 25) return 9;  // Superhuman Strength
                if (level >= 4)  return 1;  // Burst of Strength
                return -1;
            case RANGED:
                if (level >= 70) return 20; // Rigour
                if (level >= 43) return 11; // Eagle Eye
                if (level >= 9)  return 6;  // Hawk Eye
                if (level >= 8)  return 3;  // Sharp Eye
                return -1;
            case MAGIC:
                if (level >= 70) return 21; // Augury
                if (level >= 50) return 12; // Mystic Might
                if (level >= 19) return 7;  // Mystic Lore
                if (level >= 8)  return 4;  // Mystic Will
                return -1;
        }
        return -1;
    }

    /**
     * Pick the protection-prayer index for an enemy's primary attack type.
     * 11=Protect from Magic, 12=Protect from Missiles, 13=Protect from Melee
     * at levels 37/40/43. Returns -1 if the bot's prayer level can't
     * support the appropriate protection.
     */
    public static int protectionPrayerFor(int enemyAttackType, int prayerLvl) {
        // enemyAttackType: 0=melee, 1=range, 2=magic
        if (enemyAttackType == 0 && prayerLvl >= 43) return 13; // PfMelee
        if (enemyAttackType == 1 && prayerLvl >= 40) return 12; // PfMissiles
        if (enemyAttackType == 2 && prayerLvl >= 37) return 11; // PfMagic
        return -1;
    }

    /**
     * Activate a protection prayer matching the enemy's primary attack
     * type. Use sparingly - it drains prayer fast and bots have limited
     * points. Caller should only invoke when fighting a real threat.
     */
    public static void activateProtection(AIPlayer bot, int enemyAttackType) {
        try {
            int prayerLvl = bot.getSkills().getLevelForXp(Skills.PRAYER);
            int prayerPts = bot.getPrayer().getPrayerpoints();
            if (prayerPts < 10) return;
            int id = protectionPrayerFor(enemyAttackType, prayerLvl);
            if (id < 0) return;
            if (bot.getPrayer().usingPrayer(0, id)) return;
            bot.getPrayer().switchPrayer(id, false);
        } catch (Throwable ignored) {}
    }
}
