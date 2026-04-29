package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.game.WorldTile;
import com.rs.game.player.Skills;
import com.rs.game.player.content.Magic;

/**
 * Real teleport routing for bots. Replaces direct setNextWorldTile jumps
 * with proper Magic.sendNormalTeleportSpell calls - actual rune
 * consumption, animations, and the lock+delay players experience.
 *
 * Each teleport entry: target tile, spell level requirement, XP, rune
 * cost (item IDs - same as the standard spellbook recipe), arrival
 * tile (the actual landing spot, which may be a few tiles from the
 * named target).
 *
 * Falls back to walk if the bot doesn't have the runes/level. Picks the
 * destination closest to the target the BotBrain wants to reach.
 */
public final class BotTeleporter {

    private BotTeleporter() {}

    public static final class Spell {
        public final String name;
        public final WorldTile landingTile;
        public final int magicLevel;
        public final int xp;
        public final int[] runes; // pairs of (id, qty)
        public Spell(String name, WorldTile landing, int level, int xp, int... runes) {
            this.name = name;
            this.landingTile = landing;
            this.magicLevel = level;
            this.xp = xp;
            this.runes = runes;
        }
    }

    /** Standard spellbook teleports the bot can cast. */
    private static final Spell[] STANDARD = new Spell[] {
        new Spell("Lumbridge",  new WorldTile(3222, 3219, 0), 25, 41, 556, 1, 557, 1, 563, 1),
        new Spell("Varrock",    new WorldTile(3210, 3424, 0), 25, 35, 556, 3, 554, 1, 563, 1),
        new Spell("Falador",    new WorldTile(2965, 3380, 0), 37, 48, 556, 3, 555, 1, 563, 1),
        new Spell("Camelot",    new WorldTile(2757, 3477, 0), 45, 55, 556, 5, 563, 1),
        new Spell("Ardougne",   new WorldTile(2664, 3306, 0), 51, 61, 555, 2, 563, 2),
        new Spell("Watchtower", new WorldTile(2548, 3112, 0), 58, 68, 557, 2, 563, 2),
        new Spell("Trollheim",  new WorldTile(2890, 3678, 0), 61, 68, 554, 2, 563, 2),
        new Spell("Ape Atoll",  new WorldTile(2796, 2791, 0), 64, 76, 555, 2, 554, 2, 565, 2, 563, 2),
    };

    /**
     * Find the best teleport spell to get the bot near targetX,targetY.
     * Returns null if no teleport gets meaningfully closer than walking.
     */
    public static Spell pickBest(AIPlayer bot, int targetX, int targetY) {
        Spell best = null;
        long bestDist = Long.MAX_VALUE;
        long currentDist = (long) Math.hypot(bot.getX() - targetX, bot.getY() - targetY);
        for (Spell s : STANDARD) {
            if (bot.getSkills().getLevel(Skills.MAGIC) < s.magicLevel) continue;
            if (!hasRunes(bot, s)) continue;
            long d = (long) Math.hypot(s.landingTile.getX() - targetX, s.landingTile.getY() - targetY);
            if (d < bestDist) { bestDist = d; best = s; }
        }
        // Only return if it's at least 30% closer - otherwise just walk.
        if (best == null) return null;
        if (bestDist >= currentDist * 0.7) return null;
        return best;
    }

    /**
     * Cast the spell (deducts runes, plays animation+gfx, lands the bot at
     * landingTile after the standard cast delay). Returns true on success.
     * False if checkRunes fails midway, level dropped, or the bot is locked.
     */
    public static boolean cast(AIPlayer bot, Spell spell) {
        if (bot == null || spell == null) return false;
        try {
            return Magic.sendNormalTeleportSpell(bot, spell.magicLevel, spell.xp, spell.landingTile, spell.runes);
        } catch (Throwable t) {
            return false;
        }
    }

    /** True if bot has all the runes the spell needs. */
    private static boolean hasRunes(AIPlayer bot, Spell s) {
        try {
            for (int i = 0; i + 1 < s.runes.length; i += 2) {
                int runeId = s.runes[i];
                int qty = s.runes[i + 1];
                if (!bot.getInventory().containsItemToolBelt(runeId, qty)) return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
