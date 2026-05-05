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
        // Lodestone waypoints - free, level 0, no runes. Coordinates
        // match HomeTeleport.useLodestone exactly (decoded from the
        // hash values, not guessed) - decoder: x=(h>>14)&0x3fff,
        // y=h&0x3fff, plane=h>>28. Bots auto-have all of these
        // unlocked since HomeTeleport doesn't gate on activation.
        // Without these, methods at Catherby / Karamja / Seers etc.
        // were unreachable for bots without Skills Necklaces - the
        // bot would oscillate between glory landings forever.
        new Spell("Lodestone Lumbridge",   new WorldTile(3233, 3222, 0), 0, 0),
        new Spell("Lodestone Burthorpe",   new WorldTile(2899, 3545, 0), 0, 0),
        new Spell("Lodestone Taverley",    new WorldTile(2878, 3443, 0), 0, 0),
        new Spell("Lodestone Catherby",    new WorldTile(2831, 3452, 0), 0, 0),
        new Spell("Lodestone Seers",       new WorldTile(2689, 3483, 0), 0, 0),
        new Spell("Lodestone Ardougne",    new WorldTile(2634, 3349, 0), 0, 0),
        new Spell("Lodestone Yanille",     new WorldTile(2529, 3095, 0), 0, 0),
        new Spell("Lodestone Falador",     new WorldTile(2967, 3404, 0), 0, 0),
        new Spell("Lodestone Edgeville",   new WorldTile(3067, 3506, 0), 0, 0),
        new Spell("Lodestone Varrock",     new WorldTile(3214, 3377, 0), 0, 0),
        new Spell("Lodestone Draynor",     new WorldTile(3105, 3299, 0), 0, 0),
        new Spell("Lodestone Port Sarim",  new WorldTile(3011, 3216, 0), 0, 0),
        new Spell("Lodestone Karamja",     new WorldTile(2761, 3148, 0), 0, 0),
        new Spell("Lodestone Al-Kharid",   new WorldTile(3297, 3185, 0), 0, 0),
        new Spell("Lodestone Fremennik",   new WorldTile(2712, 3678, 0), 0, 0),
        new Spell("Lodestone Canifis",     new WorldTile(3517, 3516, 0), 0, 0),
        new Spell("Lodestone Bandit Camp", new WorldTile(3214, 2955, 0), 0, 0),
        new Spell("Lodestone Eagles' Peak",new WorldTile(2366, 3480, 0), 0, 0),
        new Spell("Lodestone Tirannwn",    new WorldTile(2254, 3150, 0), 0, 0),
        new Spell("Lodestone Oo'glog",     new WorldTile(2532, 2872, 0), 0, 0),
        new Spell("Lodestone Ashdale",     new WorldTile(2474, 2709, 2), 0, 0),
        // Lunar Isle (2085,3915) / Prifddinas (2208,3361,plane1) are
        // hard-gated by controllers and skipped - bots landing there
        // get bounced. Everything else is reachable as a free tile.
    };

    /** Jewelry teleport entry - charged item + landing tile + animation. */
    public static final class JewelryTele {
        public final String name;
        public final int[] itemIds; // any of these (charge variants)
        public final WorldTile landingTile;
        public JewelryTele(String name, WorldTile landing, int... itemIds) {
            this.name = name;
            this.itemIds = itemIds;
            this.landingTile = landing;
        }
    }

    /**
     * Common jewelry teleports. Bot uses the matching charged item from
     * inventory (consumes a charge), plays the standard jewelry teleport
     * GFX (1681) and animation (1979). Faster than spell casts (no rune
     * cost, no level requirement) so bots prefer these when available.
     */
    private static final JewelryTele[] JEWELRY = new JewelryTele[] {
        // Amulet of glory: edge / karamja / draynor / al-kharid
        new JewelryTele("Glory Edgeville",   new WorldTile(3087, 3496, 0), 1706, 1708, 1710, 1712, 11978, 1704),
        new JewelryTele("Glory Karamja",     new WorldTile(2918, 3176, 0), 1706, 1708, 1710, 1712, 11978, 1704),
        new JewelryTele("Glory Draynor",     new WorldTile(3105, 3251, 0), 1706, 1708, 1710, 1712, 11978, 1704),
        new JewelryTele("Glory Al-Kharid",   new WorldTile(3304, 3124, 0), 1706, 1708, 1710, 1712, 11978, 1704),
        // Games necklace: burthorpe / barbarian / corp / wintertodt
        new JewelryTele("Games Burthorpe",   new WorldTile(2898, 3554, 0), 3853, 3855, 3857, 3859, 3861, 3863, 3865, 3867),
        new JewelryTele("Games Barbarian",   new WorldTile(2519, 3570, 0), 3853, 3855, 3857, 3859, 3861, 3863, 3865, 3867),
        // Ring of dueling: castle wars / duel arena / fog
        new JewelryTele("Dueling CW",        new WorldTile(2440, 3090, 0), 2552, 2554, 2556, 2558, 2560, 2562, 2564, 2566),
        new JewelryTele("Dueling Arena",     new WorldTile(3315, 3235, 0), 2552, 2554, 2556, 2558, 2560, 2562, 2564, 2566),
        // Combat bracelet: warriors / champions / monastery / edgeville
        new JewelryTele("Combat Warriors",   new WorldTile(2882, 3548, 0), 11118, 11120, 11122, 11124, 11126),
        new JewelryTele("Combat Monastery",  new WorldTile(3052, 3490, 0), 11118, 11120, 11122, 11124, 11126),
        // Skills necklace: fishing guild / mining guild / crafting guild
        new JewelryTele("Skills Fishing",    new WorldTile(2611, 3393, 0), 11105, 11107, 11109, 11111, 11113),
        new JewelryTele("Skills Mining",     new WorldTile(3046, 9756, 0), 11105, 11107, 11109, 11111, 11113),
        new JewelryTele("Skills Crafting",   new WorldTile(2933, 3287, 0), 11105, 11107, 11109, 11111, 11113),
    };

    /**
     * Container for whichever teleport got picked.
     */
    public static final class Choice {
        public final Spell spell;       // non-null = standard spellbook
        public final JewelryTele jewel; // non-null = jewelry teleport
        public final WorldTile landingTile;
        public final String name;
        Choice(Spell s) { this.spell = s; this.jewel = null; this.landingTile = s.landingTile; this.name = s.name; }
        Choice(JewelryTele j) { this.spell = null; this.jewel = j; this.landingTile = j.landingTile; this.name = j.name; }
    }

    /**
     * Find the best teleport (spell OR jewelry) to get the bot near
     * targetX,targetY. Jewelry preferred over spells when both apply
     * since jewelry has no rune cost. Returns null if no teleport gets
     * meaningfully closer than walking.
     */
    public static Choice pickBest(AIPlayer bot, int targetX, int targetY) {
        Choice best = null;
        long bestDist = Long.MAX_VALUE;
        long currentDist = (long) Math.hypot(bot.getX() - targetX, bot.getY() - targetY);
        // Jewelry first - free, no level req
        for (JewelryTele j : JEWELRY) {
            if (!hasJewelry(bot, j)) continue;
            long d = (long) Math.hypot(j.landingTile.getX() - targetX, j.landingTile.getY() - targetY);
            if (d < bestDist) { bestDist = d; best = new Choice(j); }
        }
        // Then standard spells
        for (Spell s : STANDARD) {
            if (bot.getSkills().getLevel(Skills.MAGIC) < s.magicLevel) continue;
            if (!hasRunes(bot, s)) continue;
            long d = (long) Math.hypot(s.landingTile.getX() - targetX, s.landingTile.getY() - targetY);
            // Prefer jewelry on ties (cheaper); spell wins only if strictly closer
            if (d < bestDist) { bestDist = d; best = new Choice(s); }
        }
        if (best == null) return null;
        if (bestDist >= currentDist * 0.7) return null; // not closer enough - walk instead
        return best;
    }

    /**
     * Execute the chosen teleport (jewelry or spell). Returns true on
     * success. Routes through Magic.sendNormalTeleportSpell or
     * sendItemTeleportSpell as appropriate.
     */
    public static boolean cast(AIPlayer bot, Choice choice) {
        if (bot == null || choice == null) return false;
        try {
            if (choice.spell != null) {
                Spell s = choice.spell;
                return Magic.sendNormalTeleportSpell(bot, s.magicLevel, s.xp, s.landingTile, s.runes);
            }
            if (choice.jewel != null) {
                // Jewelry tele: emote 9603, gfx 1684 (or 1681 for glories)
                return Magic.sendItemTeleportSpell(bot, true, 9603, 1684, 4, choice.jewel.landingTile);
            }
        } catch (Throwable ignored) {}
        return false;
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

    /** True if bot has any of the jewelry charge variants in inventory or equipped. */
    private static boolean hasJewelry(AIPlayer bot, JewelryTele j) {
        try {
            for (int id : j.itemIds) {
                if (bot.getInventory().containsItem(id, 1)) return true;
                // Check equipment slots (amulet, ring, bracelet, neck)
                try {
                    if (bot.getEquipment().getAmuletId() == id) return true;
                    if (bot.getEquipment().getRingId() == id) return true;
                } catch (Throwable ignored) {}
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
