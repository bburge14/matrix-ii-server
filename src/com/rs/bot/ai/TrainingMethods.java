package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.game.WorldTile;
import com.rs.game.player.Skills;
import com.rs.game.player.actions.Fishing.FishingSpots;
import com.rs.game.player.actions.Woodcutting.TreeDefinitions;
import com.rs.game.player.actions.mining.Mining.RockDefinitions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Catalog of training / money-making methods bots can choose from.
 *
 * Each method declares what it requires (skill levels, items) and what it
 * produces (XP rate, GP rate). Bots picking a method for a goal filter
 * by isApplicable(bot) and pick the highest-tier qualifying method, so
 * a level 1 bot starts on normal trees and naturally promotes to oaks
 * at 15, willows at 30, ... yews at 60, magics at 75 - without anyone
 * hand-coding a "switch to X at level Y" decision.
 *
 * Data is pragmatic, not RuneScape-Wiki-perfect. Locations come from
 * WorldKnowledge or are commonly known training spots. XP/GP values are
 * rough orderings used only for ranking, not for pacing.
 */
public final class TrainingMethods {

    /** What kind of action a method maps to. */
    public enum Kind {
        WOODCUTTING, MINING, FISHING, COMBAT, THIEVING, FIREMAKING, COOKING, SMELTING,
        CRAFTING, PRAYER,
        // Locations now wired; per-Kind action wiring lands skill-by-skill.
        // Until wired, bots walk to the right tile and stall - SuccessTracker
        // shows "method picked, 0 success" in audit.log so you can verify
        // locations before wiring.
        HERBLORE, AGILITY, RUNECRAFTING, HUNTER, SUMMONING, FARMING, CONSTRUCTION,
        FLETCHING, DIVINATION, DUNGEONEERING, SMITHING_ANVIL, MINIGAME
    }

    public static final class Method {
        public final String description;
        public final Kind kind;
        public final int skill;          // primary skill (Skills.X) or -1
        public final int minLevel;
        public final int maxLevel;       // recommend swapping out beyond this
        public final WorldTile location;
        public final int xpRate;         // approximate xp/hr - relative ranking only
        public final int gpRate;         // approximate gp/hr - relative ranking only
        public final int[] requiredItems;// any of these item IDs must be owned
                                         // (anywhere - equipment/inventory/bank)
        public final int requiredCombatLevel;
        // Skilling-method-specific: which definition to look for so
        // EnvironmentScanner doesn't grab the wrong tree/rock/spot.
        public final TreeDefinitions treeDef;
        public final RockDefinitions rockDef;
        public final FishingSpots fishDef;
        // Combat-only: NPC IDs to attack at this location.
        public final int[] npcIds;
        // Risky locations (wilderness, deep dungeons, bosses) - bots
        // need adequate combat level + gear to even consider these.
        public final boolean dangerous;

        private Method(Builder b) {
            this.description = b.description;
            this.kind = b.kind;
            this.skill = b.skill;
            this.minLevel = b.minLevel;
            this.maxLevel = b.maxLevel;
            this.location = b.location;
            this.xpRate = b.xpRate;
            this.gpRate = b.gpRate;
            this.requiredItems = b.requiredItems;
            this.requiredCombatLevel = b.requiredCombatLevel;
            this.treeDef = b.treeDef;
            this.rockDef = b.rockDef;
            this.fishDef = b.fishDef;
            this.npcIds = b.npcIds;
            this.dangerous = b.dangerous;
        }

        public boolean isApplicable(AIPlayer bot) {
            try {
                if (skill >= 0) {
                    int lvl = bot.getSkills().getLevel(skill);
                    if (lvl < minLevel) return false;
                }
                if (requiredCombatLevel > 0
                        && bot.getSkills().getCombatLevel() < requiredCombatLevel) return false;
                // Dangerous spots (wilderness, deep dungeons, bosses) need a
                // bot that can survive. Hard gate at cb 80 even before
                // requiredCombatLevel kicks in - prevents low-level bots
                // teleporting to runite mine and getting one-shot.
                if (dangerous && bot.getSkills().getCombatLevel() < 80) return false;
                if (requiredItems != null && requiredItems.length > 0) {
                    boolean hasOne = false;
                    for (int id : requiredItems) {
                        if (GoalStateChecker.ownsItemById(bot, id, 1)) { hasOne = true; break; }
                    }
                    if (!hasOne) return false;
                }
                return true;
            } catch (Throwable t) {
                return false;
            }
        }

        /** Higher = pick first. Combines tier (minLevel) with rate. */
        public int rankFor(Kind goalKind, AIPlayer bot) {
            int score = minLevel * 100;          // tier weight
            score += xpRate / 1000;              // small XP-rate kicker
            return score;
        }

        public int rankForGp(AIPlayer bot) {
            int score = minLevel * 50;
            score += gpRate / 1000;
            return score;
        }
    }

    /** Builder so adding methods stays readable. */
    private static final class Builder {
        String description;
        Kind kind;
        int skill = -1;
        int minLevel = 1;
        int maxLevel = 99;
        WorldTile location;
        int xpRate;
        int gpRate;
        int[] requiredItems = new int[0];
        int requiredCombatLevel = 0;
        boolean dangerous = false;
        TreeDefinitions treeDef;
        RockDefinitions rockDef;
        FishingSpots fishDef;
        int[] npcIds = new int[0];
        Builder(String d, Kind k) { description = d; kind = k; }
        Builder skill(int s) { skill = s; return this; }
        Builder lvl(int min, int max) { minLevel = min; maxLevel = max; return this; }
        Builder at(int x, int y) { location = new WorldTile(x, y, 0); return this; }
        Builder at(int x, int y, int plane) { location = new WorldTile(x, y, plane); return this; }
        Builder xp(int xph) { xpRate = xph; return this; }
        Builder gp(int gph) { gpRate = gph; return this; }
        Builder needs(int... itemIds) { requiredItems = itemIds; return this; }
        Builder cb(int cbLvl) { requiredCombatLevel = cbLvl; return this; }
        Builder tree(TreeDefinitions t) { treeDef = t; return this; }
        Builder rock(RockDefinitions r) { rockDef = r; return this; }
        Builder fish(FishingSpots f) { fishDef = f; return this; }
        Builder npcs(int... ids) { npcIds = ids; return this; }
        Builder dangerous() { dangerous = true; return this; }
        Method build() { return new Method(this); }
    }

    // ===== Method registry =====

    private static final List<Method> ALL = new ArrayList<>();

    /** Read-only access for diagnostics (::auditmethods). */
    public static java.util.List<Method> getAll() { return java.util.Collections.unmodifiableList(ALL); }

    // Crowding tracker - how many bots are CURRENTLY pursuing each method.
    // bestMethodFor / rankedMethodsFor apply a penalty per active bot so 400
    // bots don't all converge on the single highest-GP method. Each tick a
    // bot stays on a method, the count stays elevated; switching decrements
    // the old and increments the new (tracked by BotBrain).
    private static final java.util.Map<Method, Integer> ACTIVE_BOTS_PER_METHOD =
        new java.util.concurrent.ConcurrentHashMap<>();

    /** Tells the registry that a bot picked this method. Idempotent. */
    public static void registerActive(Method m, Method previous) {
        if (m == previous) return;
        if (previous != null) {
            ACTIVE_BOTS_PER_METHOD.merge(previous, -1, (a, b) -> Math.max(0, a + b));
        }
        if (m != null) {
            ACTIVE_BOTS_PER_METHOD.merge(m, 1, Integer::sum);
        }
    }

    /** Tells the registry the bot stopped pursuing all methods. */
    public static void unregisterActive(Method previous) {
        if (previous != null) {
            ACTIVE_BOTS_PER_METHOD.merge(previous, -1, (a, b) -> Math.max(0, a + b));
        }
    }

    public static int activeBotsOn(Method m) {
        return ACTIVE_BOTS_PER_METHOD.getOrDefault(m, 0);
    }

    static {
        // ---- Woodcutting (multiple known F2P+P2P spots per tier so bots
        // scatter across the world instead of all stacking at one tree)
        ALL.add(b("Chop normal trees - Lumbridge", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(1, 15).at(3225, 3220).xp(15000).gp(2000)
            .tree(TreeDefinitions.NORMAL).build());
        ALL.add(b("Chop normal trees - Draynor", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(1, 15).at(3101, 3263).xp(15000).gp(2000)
            .tree(TreeDefinitions.NORMAL).build());
        ALL.add(b("Chop normal trees - Varrock west park", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(1, 15).at(3175, 3413).xp(15000).gp(2000)
            .tree(TreeDefinitions.NORMAL).build());
        ALL.add(b("Chop oak trees - Varrock west", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(15, 30).at(3164, 3414).xp(22000).gp(8000)
            .tree(TreeDefinitions.OAK).build());
        ALL.add(b("Chop oak trees - Falador east", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(15, 30).at(3046, 3322).xp(22000).gp(8000)
            .tree(TreeDefinitions.OAK).build());
        ALL.add(b("Chop willow trees - Draynor", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(30, 45).at(3087, 3234).xp(40000).gp(15000)
            .tree(TreeDefinitions.WILLOW).build());
        // Removed Lumbridge swamp willow @ (3160, 3251) - audit FAIL'd cleanly,
        // no willows in 830 cache at that coord. Draynor willow still works.
        ALL.add(b("Chop willow trees - Port Sarim", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(30, 45).at(3058, 3251).xp(40000).gp(15000)
            .tree(TreeDefinitions.WILLOW).build());
        ALL.add(b("Chop maple trees - Seers'", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(45, 60).at(2728, 3503).xp(60000).gp(18000)
            .tree(TreeDefinitions.MAPLE).build());
        ALL.add(b("Chop yew trees - Edgeville", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(60, 75).at(3087, 3481).xp(70000).gp(45000)
            .tree(TreeDefinitions.YEW).build());
        ALL.add(b("Chop yew trees - Falador east", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(60, 75).at(2935, 3349).xp(70000).gp(45000)
            .tree(TreeDefinitions.YEW).build());
        ALL.add(b("Chop yew trees - Lumbridge", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(60, 75).at(3252, 3163).xp(70000).gp(45000)
            .tree(TreeDefinitions.YEW).build());
        ALL.add(b("Chop magic trees - Tree Gnome Stronghold", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(75, 99).at(2705, 3424).xp(80000).gp(80000)
            .tree(TreeDefinitions.MAGIC).build());
        ALL.add(b("Chop magic trees - Sorcerer's Tower", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(75, 99).at(2702, 3398).xp(80000).gp(80000)
            .tree(TreeDefinitions.MAGIC).build());

        // ---- Mining (verified F2P locations - the old Heroes Guild coord
        // had no mithril/adamant rocks, that's why bots saw 'no ore in 12 tiles')
        ALL.add(b("Mine clay - Draynor", Kind.MINING)
            .skill(Skills.MINING).lvl(1, 15).at(3142, 3305).xp(15000).gp(5000)
            .rock(RockDefinitions.Clay_Ore).build());
        ALL.add(b("Mine tin - Varrock east", Kind.MINING)
            .skill(Skills.MINING).lvl(1, 15).at(3289, 3372).xp(15000).gp(2000)
            .rock(RockDefinitions.Tin_Ore).build());
        ALL.add(b("Mine tin - Lumbridge swamp", Kind.MINING)
            .skill(Skills.MINING).lvl(1, 15).at(3228, 3148).xp(15000).gp(2000)
            .rock(RockDefinitions.Tin_Ore).build());
        ALL.add(b("Mine iron - Varrock east", Kind.MINING)
            .skill(Skills.MINING).lvl(15, 45).at(3289, 3372).xp(35000).gp(20000)
            .rock(RockDefinitions.Iron_Ore).build());
        ALL.add(b("Mine iron - Al-Kharid", Kind.MINING)
            .skill(Skills.MINING).lvl(15, 45).at(3299, 3287).xp(35000).gp(20000)
            .rock(RockDefinitions.Iron_Ore).build());
        ALL.add(b("Mine iron - Dwarf Mine", Kind.MINING)
            .skill(Skills.MINING).lvl(15, 45).at(3030, 9759).xp(35000).gp(20000)
            .rock(RockDefinitions.Iron_Ore).build());
        ALL.add(b("Mine coal - Barbarian Village", Kind.MINING)
            .skill(Skills.MINING).lvl(30, 60).at(3082, 3420).xp(50000).gp(25000)
            .rock(RockDefinitions.Coal_Ore).build());
        ALL.add(b("Mine coal - Dwarf Mine", Kind.MINING)
            .skill(Skills.MINING).lvl(30, 60).at(3018, 9740).xp(50000).gp(25000)
            .rock(RockDefinitions.Coal_Ore).build());
        ALL.add(b("Mine mithril - Mining Guild basement", Kind.MINING)
            .skill(Skills.MINING).lvl(55, 75).at(3033, 9743).xp(60000).gp(40000)
            .rock(RockDefinitions.Mithril_Ore).build());
        ALL.add(b("Mine mithril - Wilderness mine", Kind.MINING)
            .skill(Skills.MINING).lvl(55, 75).at(3092, 3568).xp(60000).gp(40000)
            .rock(RockDefinitions.Mithril_Ore).dangerous().build());
        ALL.add(b("Mine adamantite - Mining Guild basement", Kind.MINING)
            .skill(Skills.MINING).lvl(70, 85).at(3033, 9737).xp(65000).gp(60000)
            .rock(RockDefinitions.Adamant_Ore).build());
        ALL.add(b("Mine runite - Wilderness", Kind.MINING)
            .skill(Skills.MINING).lvl(85, 99).at(3058, 3884).xp(45000).gp(120000)
            .rock(RockDefinitions.Runite_Ore).dangerous().build());

        // ---- Fishing (verified spots per tier) ----
        ALL.add(b("Net shrimp - Lumbridge swamp", Kind.FISHING)
            .skill(Skills.FISHING).lvl(1, 20).at(3242, 3151).xp(10000).gp(2000)
            .fish(FishingSpots.NET).build());
        ALL.add(b("Net shrimp - Draynor village", Kind.FISHING)
            .skill(Skills.FISHING).lvl(1, 20).at(3086, 3232).xp(10000).gp(2000)
            .fish(FishingSpots.NET).build());
        ALL.add(b("Net shrimp - Al-Kharid", Kind.FISHING)
            .skill(Skills.FISHING).lvl(1, 20).at(3266, 3154).xp(10000).gp(2000)
            .fish(FishingSpots.NET).build());
        ALL.add(b("Bait fly fishing - Barbarian Village", Kind.FISHING)
            .skill(Skills.FISHING).lvl(20, 40).at(3105, 3434).xp(30000).gp(8000)
            .fish(FishingSpots.NET).build());
        ALL.add(b("Bait fly fishing - Shilo Village", Kind.FISHING)
            .skill(Skills.FISHING).lvl(20, 40).at(2853, 2967).xp(30000).gp(8000)
            .fish(FishingSpots.NET).build());
        ALL.add(b("Cage lobster - Karamja", Kind.FISHING)
            .skill(Skills.FISHING).lvl(40, 76).at(2924, 3179).xp(45000).gp(40000)
            .fish(FishingSpots.CAGE).build());
        ALL.add(b("Cage lobster - Catherby", Kind.FISHING)
            .skill(Skills.FISHING).lvl(40, 76).at(2837, 3429).xp(45000).gp(40000)
            .fish(FishingSpots.CAGE).build());
        ALL.add(b("Cage lobster - Fishing Guild", Kind.FISHING)
            .skill(Skills.FISHING).lvl(40, 76).at(2611, 3393).xp(45000).gp(40000)
            .fish(FishingSpots.CAGE).build());
        ALL.add(b("Harpoon shark - Fishing Guild", Kind.FISHING)
            .skill(Skills.FISHING).lvl(76, 99).at(2611, 3393).xp(55000).gp(80000)
            .fish(FishingSpots.HARPOON).build());
        ALL.add(b("Harpoon shark - Catherby", Kind.FISHING)
            .skill(Skills.FISHING).lvl(76, 99).at(2837, 3429).xp(55000).gp(80000)
            .fish(FishingSpots.HARPOON).build());

        // ---- Firemaking (process-skill: requires logs in inventory) ----
        // Single method with all log tiers in requiredItems - the action
        // picker (BotBrain.tryStartFiremaking) chooses the highest-tier
        // log the bot can light. Location is GE-area open ground; could
        // be done anywhere with a clear tile.
        ALL.add(b("Firemake logs - GE area", Kind.FIREMAKING)
            .skill(Skills.FIREMAKING).lvl(1, 99).at(3164, 3487).xp(45000).gp(0)
            .needs(1511, 1521, 1519, 1517, 1515, 1513, 6332, 3448).build());
        ALL.add(b("Firemake logs - Edgeville bank", Kind.FIREMAKING)
            .skill(Skills.FIREMAKING).lvl(1, 99).at(3094, 3491).xp(45000).gp(0)
            .needs(1511, 1521, 1519, 1517, 1515, 1513, 6332, 3448).build());
        ALL.add(b("Firemake logs - Varrock west", Kind.FIREMAKING)
            .skill(Skills.FIREMAKING).lvl(1, 99).at(3185, 3436).xp(45000).gp(0)
            .needs(1511, 1521, 1519, 1517, 1515, 1513, 6332, 3448).build());
        ALL.add(b("Firemake logs - Burthorpe", Kind.FIREMAKING)
            .skill(Skills.FIREMAKING).lvl(1, 99).at(2906, 3540).xp(45000).gp(0)
            .needs(1511, 1521, 1519, 1517, 1515, 1513, 6332, 3448).build());

        // ---- Prayer (process-skill: bones on altar) ----
        // Bot needs bones in inventory and an altar object. 4x XP via altar
        // vs burying. Common altar locations: Edgeville monastery, POH altars.
        ALL.add(b("Bones on altar - Edgeville monastery", Kind.PRAYER)
            .skill(Skills.PRAYER).lvl(1, 99).at(3056, 3484).xp(35000).gp(0)
            .needs(526, 528, 530, 532, 534, 536, 2859, 3183, 4812, 18830, 4834).build());
        ALL.add(b("Bones on altar - Falador church", Kind.PRAYER)
            .skill(Skills.PRAYER).lvl(1, 99).at(2995, 3372).xp(35000).gp(0)
            .needs(526, 528, 530, 532, 534, 536, 2859, 3183, 4812, 18830, 4834).build());
        ALL.add(b("Bones on altar - Burthorpe Saradomin altar", Kind.PRAYER)
            .skill(Skills.PRAYER).lvl(1, 99).at(2918, 3486).xp(35000).gp(0)
            .needs(526, 528, 530, 532, 534, 536, 2859, 3183, 4812, 18830, 4834).build());

        // ---- Crafting (process-skill: cut uncut gems with chisel) ----
        // Requires uncut gems (gem drops from mining or monster loot).
        // Bot picks highest-tier gem from inventory to cut.
        ALL.add(b("Cut gems - any bank", Kind.CRAFTING)
            .skill(Skills.CRAFTING).lvl(1, 99).at(3094, 3491).xp(40000).gp(0)
            .needs(1625, 1627, 1629, 1623, 1621, 1619, 1617, 1631, 6571).build());

        // ---- Smelting (process-skill: requires ores + furnace) ----
        // Bot finds a furnace nearby and smelts the highest-tier bar it
        // qualifies for from inventory ores.
        ALL.add(b("Smelt bars - Edgeville furnace", Kind.SMELTING)
            .skill(Skills.SMITHING).lvl(1, 99).at(3110, 3499).xp(50000).gp(0)
            .needs(436, 438, 440, 442, 444, 447, 449, 451).build());
        ALL.add(b("Smelt bars - Falador furnace", Kind.SMELTING)
            .skill(Skills.SMITHING).lvl(1, 99).at(2974, 3370).xp(50000).gp(0)
            .needs(436, 438, 440, 442, 444, 447, 449, 451).build());
        ALL.add(b("Smelt bars - Al-Kharid furnace", Kind.SMELTING)
            .skill(Skills.SMITHING).lvl(1, 99).at(3275, 3186).xp(50000).gp(0)
            .needs(436, 438, 440, 442, 444, 447, 449, 451).build());

        // ---- Cooking (process-skill: requires raw food + range nearby) ----
        // Kitchens at well-known towns. Bot scans for "range" / "stove" /
        // "fire" object near these coords, picks raw food from inventory,
        // setAction(new Cooking(...)).
        ALL.add(b("Cook food - Lumbridge kitchen", Kind.COOKING)
            .skill(Skills.COOKING).lvl(1, 99).at(3211, 3215).xp(40000).gp(0)
            .needs(317, 327, 321, 331, 359, 377, 371, 383, 7944, 15270).build());
        ALL.add(b("Cook food - Catherby kitchen", Kind.COOKING)
            .skill(Skills.COOKING).lvl(1, 99).at(2818, 3443).xp(40000).gp(0)
            .needs(317, 327, 321, 331, 359, 377, 371, 383, 7944, 15270).build());
        ALL.add(b("Cook food - Al-Kharid", Kind.COOKING)
            .skill(Skills.COOKING).lvl(1, 99).at(3271, 3180).xp(40000).gp(0)
            .needs(317, 327, 321, 331, 359, 377, 371, 383, 7944, 15270).build());

        // ---- Thieving (pickpocket targets clustered around Nails) ----
        // NPC IDs verified in 830 cache via examines:
        //   1=citizen (man), 4=citizen (woman), 7=farmer, 15=Al-Kharid warrior,
        //   187=rogue, 9=guard, 23=Ardougne knight, 1905=Menaphite thug,
        //   20=paladin, 21=hero, 2109=Dwarf trader, 3205=Baker
        // NPC 18 is NOT used here - it conflicts with the Tanner shop wiring.
        // NPC 296 is NOT a guard in 830 (it's a goblin) - using 9 instead.
        ALL.add(b("Pickpocket man - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(1, 9).at(2920, 3458).xp(8000).gp(2000)
            .npcs(1, 2, 3, 4, 5, 6, 16, 24, 170, 3205).build());
        ALL.add(b("Pickpocket farmer - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(10, 24).at(2922, 3458).xp(14500).gp(5000)
            .npcs(7, 1757, 1758, 1760).build());
        ALL.add(b("Pickpocket warrior - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(25, 31).at(2924, 3458).xp(26000).gp(8000)
            .npcs(15).build());
        ALL.add(b("Pickpocket rogue - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(32, 37).at(2920, 3460).xp(35500).gp(15000)
            .npcs(187, 2267, 2268, 2269, 8122).build());
        ALL.add(b("Pickpocket master farmer - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(38, 39).at(2923, 3459).xp(43000).gp(20000)
            .npcs(2234, 2235).build());
        ALL.add(b("Pickpocket guard - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(40, 54).at(2926, 3460).xp(46500).gp(30000)
            .npcs(9, 32, 33, 34).build());
        ALL.add(b("Pickpocket Ardougne knight - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(55, 64).at(2920, 3462).xp(84300).gp(50000)
            .npcs(23, 26).build());
        ALL.add(b("Pickpocket Menaphite thug - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(65, 69).at(2922, 3462).xp(137500).gp(60000)
            .npcs(1905).build());
        ALL.add(b("Pickpocket paladin - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(70, 79).at(2924, 3462).xp(151750).gp(80000)
            .npcs(20, 2256).build());
        ALL.add(b("Pickpocket hero - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(80, 89).at(2926, 3462).xp(275000).gp(200000)
            .npcs(21).build());
        ALL.add(b("Pickpocket Dwarf trader - Burthorpe", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(90, 99).at(2920, 3464).xp(556500).gp(400000)
            .npcs(2109, 2110, 2111, 2112, 2113, 2114, 2115, 2116, 2117, 2118).build());

        // ---- Combat training - level-tiered with multiple options per
        // bracket so bots scatter across the world like real players ----
        // Tier 1 (cb 1-30): cows, chickens, goblins, rats
        ALL.add(b("Train combat - Lumbridge cows", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(1, 30).at(3027, 3307).xp(10000).cb(1)
            .npcs(81, 11238).build());
        ALL.add(b("Train combat - Lumbridge chickens", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(1, 15).at(3231, 3296).xp(8000).cb(1)
            .npcs(41, 1017).build());
        ALL.add(b("Train combat - Goblin Village", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(1, 25).at(2956, 3506).xp(11000).cb(1)
            .npcs(4481, 4482, 4485, 4486, 4488, 4491, 4493).build());
        ALL.add(b("Train combat - Stronghold rats lvl 1", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(1, 15).at(2155, 5092).xp(8000).cb(1)
            .npcs(2854, 2855, 2856).build());
        // Burthorpe combat starter spots - Trolls / Imps / Goblins around
        // the city + the Death Plateau area. Anchors in the city itself
        // so bots aggro the right pack on arrival.
        ALL.add(b("Train combat - Burthorpe imps", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(1, 20).at(2898, 3554).xp(7000).cb(1)
            .npcs(414, 7878).build());
        ALL.add(b("Train combat - Burthorpe goblins", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(1, 25).at(2880, 3546).xp(9000).cb(1)
            .npcs(4481, 4482, 4485, 4486, 4488, 4491, 4493).build());
        ALL.add(b("Train combat - Death Plateau trolls", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(60, 99).at(2853, 3582).xp(35000).cb(60)
            .npcs(941, 942, 943, 944, 945).build());

        // Tier 2 (cb 30-60): guards, knights, hill giants
        ALL.add(b("Train combat - Falador guards", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(30, 60).at(2964, 3396).xp(25000).cb(30)
            .npcs(9, 32, 33, 34).build());
        ALL.add(b("Train combat - Edgeville Dungeon hill giants", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(30, 60).at(3117, 9844).xp(28000).cb(30)
            .npcs(2098, 117).build());
        ALL.add(b("Train combat - Ardougne guards", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(30, 50).at(2651, 3307).xp(24000).cb(30)
            .npcs(32, 33, 34).build());
        ALL.add(b("Train combat - Al-Kharid warriors", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(30, 55).at(3294, 3187).xp(25000).cb(30)
            .npcs(15, 18).build());

        // Tier 3 (cb 60-90): rock crabs, dagannoth, ankou, nechryael, kalphites
        ALL.add(b("Train combat - Rock crabs Rellekka", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(60, 90).at(2721, 3726).xp(45000).cb(60)
            .npcs(1265, 1267).build());
        ALL.add(b("Train combat - Ankou", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(60, 85).at(2701, 3471).xp(48000).cb(60)
            .npcs(98, 1798).build());
        ALL.add(b("Train combat - Experiment cave", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(50, 75).at(3539, 9963).xp(40000).cb(50)
            .npcs(1677, 1678).build());
        ALL.add(b("Train combat - Kalphite soldiers", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(60, 85).at(2676, 3666).xp(50000).cb(60)
            .npcs(1308, 1309, 1310, 1311).build());

        // Tier 4 (cb 90-120): nechryael, dust devils, abyssal demons, gargoyles
        ALL.add(b("Train combat - Slayer tower abyssal demons", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(85, 120).at(3417, 3563, 2).xp(70000).cb(85)
            .gp(180000)
            .npcs(1615, 1616, 13345).build());
        ALL.add(b("Train combat - Slayer tower gargoyles", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(75, 110).at(3437, 3552, 2).xp(65000).cb(75)
            .gp(120000)
            .npcs(1610, 1611).build());
        ALL.add(b("Train combat - Dust devils Smoke", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(65, 95).at(3309, 9376).xp(55000).cb(65)
            .gp(80000)
            .npcs(1624, 1625).build());

        // Tier 5 (cb 100+ bossing - profitable PvM)
        ALL.add(b("Boss - King Black Dragon", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(85, 99).at(2273, 4680).xp(80000).cb(95)
            .gp(800000)
            .npcs(50).dangerous().build());
        ALL.add(b("Boss - Kalphite Queen", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(90, 99).at(3485, 9510).xp(90000).cb(110)
            .gp(1500000)
            .npcs(1158, 1160).dangerous().build());
        ALL.add(b("Boss - GWD Bandos", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(70, 99).at(2864, 5354).xp(75000).cb(110)
            .gp(2000000)
            .npcs(6260).dangerous().build());
        ALL.add(b("Boss - GWD Armadyl", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(70, 99).at(2839, 5296).xp(75000).cb(110)
            .gp(2500000)
            .npcs(6222).dangerous().build());
        ALL.add(b("Boss - GWD Saradomin", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(70, 99).at(2913, 5273).xp(75000).cb(110)
            .gp(1800000)
            .npcs(6247).dangerous().build());
        ALL.add(b("Boss - GWD Zamorak", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(70, 99).at(2926, 5333).xp(75000).cb(110)
            .gp(1700000)
            .npcs(6203).dangerous().build());

        // ---- Thieving (canonical RS world spreads, alongside the existing
        // Burthorpe-DZ entries above. Same NPC IDs as Burthorpe block; if
        // those NPCs are also spawned at canonical spots from vanilla cache
        // spawns, bots will scatter naturally. If a canonical spot has no
        // spawn, SuccessTracker will mark it stuck and bots fall back to
        // the Burthorpe entries.)
        ALL.add(b("Pickpocket man - Lumbridge", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(1, 9).at(3225, 3220).xp(8000).gp(2000)
            .npcs(1, 2, 3, 4, 5, 6, 16, 24, 170, 3205).build());
        ALL.add(b("Pickpocket man - Varrock west", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(1, 9).at(3187, 3437).xp(8000).gp(2000)
            .npcs(1, 2, 3, 4, 5, 6, 16, 24, 170, 3205).build());
        ALL.add(b("Pickpocket man - Edgeville", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(1, 9).at(3079, 3490).xp(8000).gp(2000)
            .npcs(1, 2, 3, 4, 5, 6, 16, 24, 170, 3205).build());
        ALL.add(b("Pickpocket farmer - Lumbridge wheat field", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(10, 24).at(3236, 3288).xp(14500).gp(5000)
            .npcs(7, 1757, 1758, 1760).build());
        ALL.add(b("Pickpocket warrior - Ardougne south", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(25, 31).at(2658, 3300).xp(26000).gp(8000)
            .npcs(15).build());
        ALL.add(b("Pickpocket rogue - Wilderness rogue camp", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(32, 37).at(3024, 3700).xp(35500).gp(15000)
            .npcs(187, 2267, 2268, 2269, 8122).dangerous().build());
        ALL.add(b("Pickpocket master farmer - Draynor", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(38, 39).at(3079, 3257).xp(43000).gp(20000)
            .npcs(2234, 2235).build());
        ALL.add(b("Pickpocket master farmer - Ardougne market", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(38, 39).at(2659, 3380).xp(43000).gp(20000)
            .npcs(2234, 2235).build());
        ALL.add(b("Pickpocket guard - Varrock east", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(40, 54).at(3215, 3429).xp(46500).gp(30000)
            .npcs(9, 32, 33, 34).build());
        ALL.add(b("Pickpocket guard - Falador east", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(40, 54).at(3019, 3357).xp(46500).gp(30000)
            .npcs(9, 32, 33, 34).build());
        ALL.add(b("Pickpocket Ardougne knight - Ardougne castle", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(55, 64).at(2657, 3296).xp(84300).gp(50000)
            .npcs(23, 26).build());
        ALL.add(b("Pickpocket Menaphite thug - Pollnivneach", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(65, 69).at(3354, 2974).xp(137500).gp(60000)
            .npcs(1905).build());
        ALL.add(b("Pickpocket paladin - Ardougne castle", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(70, 79).at(2655, 3308).xp(151750).gp(80000)
            .npcs(20, 2256).build());
        ALL.add(b("Pickpocket hero - Ardougne", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(80, 89).at(2657, 3299).xp(275000).gp(200000)
            .npcs(21).build());
        ALL.add(b("Pickpocket Dwarf trader - Keldagrim", Kind.THIEVING)
            .skill(Skills.THIEVING).lvl(90, 99).at(2902, 10193).xp(556500).gp(400000)
            .npcs(2109).build());

        // ---- Herblore (clean herbs at major banks - bots walk between)
        ALL.add(b("Clean guam - Edgeville bank", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(1, 4).at(3094, 3491).xp(2200).gp(0).build());
        ALL.add(b("Clean marrentill - Lumbridge bank", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(5, 10).at(3208, 3220).xp(3800).gp(0).build());
        ALL.add(b("Clean tarromin - Varrock east bank", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(11, 19).at(3253, 3420).xp(5000).gp(0).build());
        ALL.add(b("Clean harralander - Falador east bank", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(20, 24).at(3013, 3355).xp(6500).gp(0).build());
        ALL.add(b("Clean ranarr - Grand Exchange", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(25, 39).at(3164, 3489).xp(8500).gp(0).build());
        ALL.add(b("Clean kwuarm - Catherby bank", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(40, 47).at(2810, 3441).xp(10500).gp(0).build());
        ALL.add(b("Clean cadantine - Ardougne bank", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(48, 53).at(2616, 3332).xp(13000).gp(0).build());
        ALL.add(b("Clean lantadyme - Yanille bank", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(54, 58).at(2613, 3092).xp(15500).gp(0).build());
        ALL.add(b("Clean dwarf weed - Edgeville bank", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(59, 74).at(3094, 3491).xp(18000).gp(0).build());
        ALL.add(b("Clean torstol - Grand Exchange", Kind.HERBLORE)
            .skill(Skills.HERBLORE).lvl(75, 99).at(3164, 3489).xp(22500).gp(0).build());

        // ---- Agility (course rotation by level - canonical RS rooftops)
        ALL.add(b("Gnome agility course - Tree Gnome Stronghold", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(1, 9).at(2476, 3438).xp(8000).gp(0).build());
        ALL.add(b("Draynor rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(10, 19).at(3103, 3279).xp(10500).gp(0).build());
        ALL.add(b("Al-Kharid rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(20, 29).at(3273, 3193).xp(13500).gp(0).build());
        ALL.add(b("Varrock rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(30, 39).at(3221, 3414).xp(16500).gp(0).build());
        ALL.add(b("Canifis rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(40, 49).at(3506, 3489).xp(20000).gp(0).build());
        ALL.add(b("Falador rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(50, 59).at(3036, 3341).xp(24000).gp(0).build());
        ALL.add(b("Wilderness agility course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(52, 99).at(3004, 3937).xp(45000).gp(0).dangerous().build());
        ALL.add(b("Werewolf agility course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(60, 99).at(3543, 9852).xp(31000).gp(0).build());
        ALL.add(b("Seers' rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(60, 69).at(2729, 3489).xp(28000).gp(0).build());
        ALL.add(b("Pollnivneach rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(70, 79).at(3358, 2997).xp(33000).gp(0).build());
        ALL.add(b("Rellekka rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(80, 89).at(2625, 3677).xp(38000).gp(0).build());
        ALL.add(b("Ardougne rooftop course", Kind.AGILITY)
            .skill(Skills.AGILITY).lvl(90, 99).at(2673, 3296).xp(45000).gp(0).build());

        // ---- Runecrafting (altar ruins; bot needs essence in inv. Coords
        // are the OUTSIDE ruin tiles, not the altar interior - bot walks to
        // ruin and clicks to enter, then crafts.)
        ALL.add(b("Air rune altar - Falador", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(1, 4).at(2986, 3293).xp(6000).gp(3000).build());
        ALL.add(b("Mind rune altar - north Goblin Village", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(2, 4).at(2980, 3514).xp(6500).gp(3500).build());
        ALL.add(b("Water rune altar - Lumbridge swamp", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(5, 8).at(3185, 3165).xp(7000).gp(4000).build());
        ALL.add(b("Earth rune altar - east Varrock", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(9, 13).at(3304, 3475).xp(7500).gp(4500).build());
        ALL.add(b("Fire rune altar - east Al-Kharid", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(14, 19).at(3315, 3255).xp(8000).gp(5000).build());
        ALL.add(b("Body rune altar - west Edgeville", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(20, 26).at(3055, 3445).xp(8500).gp(5500).build());
        ALL.add(b("Cosmic rune altar - Zanaris", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(27, 34).at(2406, 4380).xp(11000).gp(8000).build());
        ALL.add(b("Chaos rune altar - Wilderness", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(35, 43).at(3060, 3590).xp(13000).gp(10000).dangerous().build());
        ALL.add(b("Nature rune altar - Karamja", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(44, 53).at(2868, 3015).xp(15000).gp(20000).build());
        ALL.add(b("Law rune altar - Entrana", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(54, 64).at(2858, 3380).xp(17000).gp(15000).build());
        ALL.add(b("Death rune altar - Death Plateau", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(65, 76).at(1860, 4639).xp(19000).gp(20000).build());
        ALL.add(b("Blood rune altar - Meiyerditch", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(77, 89).at(3559, 9778).xp(22000).gp(40000).build());
        ALL.add(b("Soul rune altar - Soul Wars", Kind.RUNECRAFTING)
            .skill(Skills.RUNECRAFTING).lvl(90, 99).at(1815, 3855).xp(25000).gp(50000).build());

        // ---- Hunter (canonical trap and falconry locations)
        ALL.add(b("Trap crimson swift - Feldip Hills", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(1, 4).at(2548, 2920).xp(7000).gp(0).build());
        ALL.add(b("Trap polar kebbit - Rellekka hunter area", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(5, 10).at(2839, 3743).xp(9500).gp(0).build());
        ALL.add(b("Trap common kebbit - Piscatoris hunter area", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(11, 16).at(2330, 3580).xp(12000).gp(0).build());
        ALL.add(b("Trap feldip weasel - Feldip Hills", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(17, 22).at(2540, 2960).xp(14500).gp(0).build());
        ALL.add(b("Trap wild kebbit - Piscatoris", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(23, 26).at(2350, 3590).xp(17000).gp(0).build());
        ALL.add(b("Trap spined larupia - Feldip Hills", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(27, 32).at(2548, 2925).xp(20000).gp(0).build());
        ALL.add(b("Trap black warlock - Piscatoris", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(33, 42).at(2370, 3596).xp(24500).gp(0).build());
        ALL.add(b("Falconry - Piscatoris hunter area", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(43, 52).at(2371, 3604).xp(35000).gp(0).build());
        ALL.add(b("Trap carnivorous chinchompa - Feldip", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(53, 59).at(2554, 2912).xp(45000).gp(20000).build());
        ALL.add(b("Box-trap chinchompa - Feldip", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(60, 72).at(2556, 2916).xp(60000).gp(40000).build());
        ALL.add(b("Trap red chinchompa - Feldip far east", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(73, 79).at(2566, 2920).xp(80000).gp(60000).build());
        ALL.add(b("Trap black chinchompa - Wilderness", Kind.HUNTER)
            .skill(Skills.HUNTER).lvl(80, 99).at(3140, 3782).xp(120000).gp(120000).dangerous().build());

        // ---- Summoning (Pikkupstix Taverley + bank loop)
        ALL.add(b("Infuse pouches - Pikkupstix Taverley", Kind.SUMMONING)
            .skill(Skills.SUMMONING).lvl(1, 50).at(2933, 3433).xp(15000).gp(0).build());
        ALL.add(b("Infuse high pouches - Pikkupstix Taverley", Kind.SUMMONING)
            .skill(Skills.SUMMONING).lvl(51, 99).at(2933, 3433).xp(35000).gp(0).build());

        // ---- Farming (allotment, herb, tree, fruit-tree patches across regions)
        ALL.add(b("Farm allotments - Falador patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(1, 99).at(3052, 3308).xp(8000).gp(2000).build());
        ALL.add(b("Farm allotments - Catherby patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(1, 99).at(2807, 3464).xp(8500).gp(2500).build());
        ALL.add(b("Farm allotments - Ardougne patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(1, 99).at(2671, 3375).xp(8000).gp(2000).build());
        ALL.add(b("Farm allotments - Canifis patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(1, 99).at(3603, 3531).xp(8500).gp(2500).build());
        ALL.add(b("Farm herbs - Falador patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(9, 99).at(3056, 3308).xp(15000).gp(15000).build());
        ALL.add(b("Farm herbs - Catherby patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(9, 99).at(2813, 3463).xp(15000).gp(15000).build());
        ALL.add(b("Farm tree - Falador park patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(15, 99).at(3004, 3372).xp(20000).gp(0).build());
        ALL.add(b("Farm tree - Lumbridge swamp patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(15, 99).at(3193, 3231).xp(20000).gp(0).build());
        ALL.add(b("Farm tree - Varrock castle patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(15, 99).at(3228, 3458).xp(20000).gp(0).build());
        ALL.add(b("Farm tree - Taverley patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(15, 99).at(2937, 3437).xp(20000).gp(0).build());
        ALL.add(b("Farm fruit tree - Catherby patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(27, 99).at(2860, 3431).xp(28000).gp(8000).build());
        ALL.add(b("Farm fruit tree - Tree Gnome Stronghold patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(27, 99).at(2475, 3445).xp(28000).gp(8000).build());
        ALL.add(b("Farm fruit tree - Brimhaven patch", Kind.FARMING)
            .skill(Skills.FARMING).lvl(27, 99).at(2762, 3211).xp(28000).gp(8000).build());

        // ---- Construction (POH portals)
        ALL.add(b("Construction - Rimmington POH portal", Kind.CONSTRUCTION)
            .skill(Skills.CONSTRUCTION).lvl(1, 99).at(2954, 3224).xp(60000).gp(0).build());
        ALL.add(b("Construction - Yanille POH portal", Kind.CONSTRUCTION)
            .skill(Skills.CONSTRUCTION).lvl(50, 99).at(2543, 3097).xp(80000).gp(0).build());
        ALL.add(b("Construction - Pollnivneach POH portal", Kind.CONSTRUCTION)
            .skill(Skills.CONSTRUCTION).lvl(70, 99).at(3340, 3001).xp(100000).gp(0).build());

        // ---- Fletching (bank loop, knife in inventory)
        ALL.add(b("Fletch arrow shafts - Edgeville bank", Kind.FLETCHING)
            .skill(Skills.FLETCHING).lvl(1, 4).at(3094, 3491).xp(5000).gp(2000).build());
        ALL.add(b("Fletch shortbow - Grand Exchange", Kind.FLETCHING)
            .skill(Skills.FLETCHING).lvl(5, 14).at(3164, 3489).xp(10000).gp(3000).build());
        ALL.add(b("Fletch oak bow - Varrock west bank", Kind.FLETCHING)
            .skill(Skills.FLETCHING).lvl(15, 24).at(3185, 3437).xp(20000).gp(5000).build());
        ALL.add(b("Fletch willow bow - Draynor bank", Kind.FLETCHING)
            .skill(Skills.FLETCHING).lvl(25, 39).at(3092, 3245).xp(35000).gp(8000).build());
        ALL.add(b("Fletch maple bow - Seers' bank", Kind.FLETCHING)
            .skill(Skills.FLETCHING).lvl(40, 54).at(2725, 3493).xp(60000).gp(15000).build());
        ALL.add(b("Fletch yew bow - Edgeville bank", Kind.FLETCHING)
            .skill(Skills.FLETCHING).lvl(55, 69).at(3094, 3491).xp(95000).gp(30000).build());
        ALL.add(b("Fletch magic bow - Grand Exchange", Kind.FLETCHING)
            .skill(Skills.FLETCHING).lvl(70, 99).at(3164, 3489).xp(160000).gp(80000).build());

        // ---- Divination (wisp colonies; coords are guess based on canonical
        // RS - verify actual spawn coords on this server during audit)
        ALL.add(b("Harvest pale wisps - Falador", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(1, 9).at(2998, 3398).xp(7000).gp(2000).build());
        ALL.add(b("Harvest flickering wisps - Tirannwn", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(10, 19).at(2300, 3140).xp(11500).gp(3000).build());
        ALL.add(b("Harvest bright wisps - Mort Myre swamp", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(20, 29).at(3450, 3450).xp(16000).gp(4000).build());
        ALL.add(b("Harvest glowing wisps - Karamja", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(30, 39).at(2858, 3128).xp(21000).gp(5500).build());
        ALL.add(b("Harvest vibrant wisps - Asgarnia", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(40, 49).at(3015, 3338).xp(27000).gp(7500).build());
        ALL.add(b("Harvest sparkling wisps - Wilderness", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(50, 59).at(3170, 3645).xp(34000).gp(10000).dangerous().build());
        ALL.add(b("Harvest gleaming wisps - Tirannwn", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(60, 69).at(2310, 3160).xp(42000).gp(13000).build());
        ALL.add(b("Harvest vivid wisps - Mort Myre", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(70, 79).at(3475, 3470).xp(51000).gp(17000).build());
        ALL.add(b("Harvest lustrous wisps - Karamja", Kind.DIVINATION)
            .skill(Skills.DIVINATION).lvl(80, 99).at(2870, 3140).xp(62000).gp(22000).build());

        // ---- Dungeoneering (Daemonheim entrance)
        ALL.add(b("Dungeoneering - Daemonheim", Kind.DUNGEONEERING)
            .skill(Skills.DUNGEONEERING).lvl(1, 99).at(3460, 3717).xp(40000).gp(0).build());

        // ---- Smithing-anvil (smith items from bars; SMELTING above covers
        // ore -> bar at furnaces. Both train Skills.SMITHING but use
        // different actions.)
        ALL.add(b("Smith bronze items - Lumbridge anvil", Kind.SMITHING_ANVIL)
            .skill(Skills.SMITHING).lvl(1, 14).at(3225, 3220).xp(5000).gp(2000).build());
        ALL.add(b("Smith iron items - Varrock west anvil", Kind.SMITHING_ANVIL)
            .skill(Skills.SMITHING).lvl(15, 29).at(3186, 3424).xp(10000).gp(4000).build());
        ALL.add(b("Smith steel items - Varrock west anvil", Kind.SMITHING_ANVIL)
            .skill(Skills.SMITHING).lvl(30, 49).at(3186, 3424).xp(20000).gp(8000).build());
        ALL.add(b("Smith mithril items - Edgeville anvil", Kind.SMITHING_ANVIL)
            .skill(Skills.SMITHING).lvl(50, 69).at(3084, 3492).xp(35000).gp(20000).build());
        ALL.add(b("Smith adamant items - Varrock west anvil", Kind.SMITHING_ANVIL)
            .skill(Skills.SMITHING).lvl(70, 84).at(3186, 3424).xp(55000).gp(40000).build());
        ALL.add(b("Smith rune items - Varrock west anvil", Kind.SMITHING_ANVIL)
            .skill(Skills.SMITHING).lvl(85, 99).at(3186, 3424).xp(80000).gp(80000).build());

        // ---- Slayer-monster combat spots (treated as COMBAT methods - bots
        // pick these via their COMBAT goals. Legend bots may also receive
        // slayer tasks elsewhere; these locations also serve task-bound
        // kills.)
        ALL.add(b("Slayer - Cave bugs Lumbridge swamp cave", Kind.COMBAT)
            .skill(Skills.SLAYER).lvl(7, 30).at(3164, 9568).xp(8000).cb(20)
            .npcs(1832, 1833).build());
        ALL.add(b("Slayer - Rockslugs Fremennik dungeon", Kind.COMBAT)
            .skill(Skills.SLAYER).lvl(20, 50).at(2792, 10001).xp(20000).cb(30)
            .npcs(1631, 1632).build());
        ALL.add(b("Slayer - Pyrefiends Fremennik dungeon", Kind.COMBAT)
            .skill(Skills.SLAYER).lvl(30, 60).at(2780, 10000).xp(35000).cb(45)
            .npcs(1633, 1634, 1635).build());
        ALL.add(b("Slayer - Bloodvelds Slayer Tower", Kind.COMBAT)
            .skill(Skills.SLAYER).lvl(50, 75).at(3422, 3554, 1).xp(60000).cb(70)
            .npcs(1618, 1619).build());
        ALL.add(b("Slayer - Aberrant spectres Slayer Tower", Kind.COMBAT)
            .skill(Skills.SLAYER).lvl(60, 85).at(3441, 3565, 1).xp(75000).cb(80)
            .npcs(1604, 1605).build());
        ALL.add(b("Slayer - Dark beasts Mourner tunnels", Kind.COMBAT)
            .skill(Skills.SLAYER).lvl(75, 99).at(1648, 5292).xp(140000).cb(100)
            .npcs(2783).build());
        ALL.add(b("Slayer - TzHaar Fight Cave Jad", Kind.COMBAT)
            .skill(Skills.SLAYER).lvl(85, 99).at(2438, 5168).xp(180000).cb(110)
            .npcs(2745).dangerous().build());

        // ---- Minigame lobbies (citizens stand at lobby tiles for visual
        // population; per-minigame archetypes + actual queue-up logic is a
        // follow-up. Soul Wars passBarrier is currently a stub - players
        // join waiting list but the arena teleport is commented out.)
        ALL.add(b("Minigame - Castle Wars lobby", Kind.MINIGAME)
            .skill(-1).lvl(1, 99).at(2442, 3090).xp(0).gp(0).build());
        ALL.add(b("Minigame - Soul Wars lobby", Kind.MINIGAME)
            .skill(-1).lvl(1, 99).at(2210, 3056).xp(0).gp(0).build());
        ALL.add(b("Minigame - Stealing Creation outpost", Kind.MINIGAME)
            .skill(-1).lvl(1, 99).at(2860, 5567).xp(0).gp(0).build());
    }

    private static Builder b(String d, Kind k) { return new Builder(d, k); }

    // ===== Goal -> method selection =====

    /**
     * Pick the best method for this goal+bot. Returns null if no method
     * applies (e.g. unsupported goal type). Caller should fall back to
     * the legacy WorldKnowledge-only behaviour if this returns null.
     */
    /**
     * Find the first applicable method for a bot of a given Kind. Used by
     * ::botforce for manual triggering.
     */
    public static Method firstApplicable(AIPlayer bot, Kind kind) {
        if (bot == null || kind == null) return null;
        for (Method m : ALL) {
            if (m.kind != kind) continue;
            if (m.isApplicable(bot)) return m;
        }
        return null;
    }

    /**
     * Return ALL methods applicable to this bot+goal, ranked best-first.
     * Used by the fallback system: BotBrain tries Plan A, on stuck moves
     * to Plan B, etc. through this list.
     *
     * Tier-aware per-bot shuffle: methods within 5% of the top score are
     * grouped into a tier and shuffled by bot-index hash, so 10 bots on
     * the same goal don't all pick the same #1 method as Plan A. Each
     * bot still gets the same Plan A on subsequent ticks (deterministic
     * by hash) so they don't oscillate.
     */
    public static java.util.List<Method> rankedMethodsFor(Goal goal, AIPlayer bot) {
        java.util.List<Method> out = new java.util.ArrayList<Method>();
        if (goal == null || bot == null) return out;
        GoalType type = goal.getData("goalType", GoalType.class);
        Kind required = type == null ? null : kindForGoal(type);
        String key = type == null ? null : type.getRequirementKey();
        final boolean rankByGp = type != null && (
            type.getCategory() == Goal.GoalCategory.ECONOMIC
            || (key != null && (key.startsWith("equipment:") || key.startsWith("weapon:")))
        );
        java.util.List<Method> applicable = new java.util.ArrayList<Method>();
        for (Method m : ALL) {
            if (required != null && m.kind != required) continue;
            if (!m.isApplicable(bot)) continue;
            applicable.add(m);
        }
        if (applicable.isEmpty()) return out;
        // Sort descending by score, with a CROWDING PENALTY: each bot
        // already on a method shaves a chunk off the score. With 400 bots
        // chasing 'wealth', the highest-GP method sees its score drop fast
        // as bots pile on, making alternatives more attractive. Naturally
        // load-balances the world without needing explicit slot reservations.
        java.util.Collections.sort(applicable, new java.util.Comparator<Method>() {
            @Override public int compare(Method a, Method b) {
                int sa = (rankByGp ? a.rankForGp(bot) : a.rankFor(a.kind, bot)) - crowdPenalty(a);
                int sb = (rankByGp ? b.rankForGp(bot) : b.rankFor(b.kind, bot)) - crowdPenalty(b);
                return Integer.compare(sb, sa);
            }
        });
        // Group into tiers (within 5% of the leader of the tier).
        // Within each tier, shuffle by bot-index hash so different bots
        // pick different #1s.
        long h = (long) bot.getIndex() * 2654435761L;
        java.util.List<Method> tier = new java.util.ArrayList<Method>();
        int leaderScore = Integer.MIN_VALUE;
        for (Method m : applicable) {
            int s = rankByGp ? m.rankForGp(bot) : m.rankFor(m.kind, bot);
            if (leaderScore == Integer.MIN_VALUE) {
                leaderScore = s;
            }
            int threshold = leaderScore - Math.max(1, Math.abs(leaderScore) / 4); // within 25% of leader (was 5%)
            if (s >= threshold) {
                tier.add(m);
            } else {
                // Flush the previous tier (shuffled per bot) and start a new one.
                shuffleByHash(tier, h);
                out.addAll(tier);
                tier.clear();
                leaderScore = s;
                tier.add(m);
            }
        }
        if (!tier.isEmpty()) {
            shuffleByHash(tier, h);
            out.addAll(tier);
        }
        return out;
    }

    /** Deterministic shuffle by integer seed - same seed = same order. */
    private static void shuffleByHash(java.util.List<Method> list, long seed) {
        if (list.size() < 2) return;
        java.util.Random r = new java.util.Random(seed);
        java.util.Collections.shuffle(list, r);
    }

    /**
     * Penalty applied to a method's ranking score based on how many bots
     * are already pursuing it. Each bot on the method reduces the
     * method's effective score, making alternatives proportionally more
     * attractive. Sublinear growth so a 200-bot method isn't penalized
     * 200x - just enough to push the next-best alternatives over the line.
     */
    private static int crowdPenalty(Method m) {
        int n = activeBotsOn(m);
        if (n <= 2) return 0;          // first 2 bots are free
        if (n <= 10) return 1000 * (n - 2);    // mild penalty for small crowds
        if (n <= 30) return 8000 + 2500 * (n - 10);
        return 58000 + 4000 * (n - 30);  // steep for large crowds
    }

    public static Method bestMethodFor(Goal goal, AIPlayer bot) {
        if (goal == null || bot == null) return null;
        GoalType type = goal.getData("goalType", GoalType.class);
        if (type == null) return null;

        Kind required = kindForGoal(type);
        // Money-grind ranking for: explicit ECONOMIC goals (BUILD_*_BANK)
        // and equipment/weapon goals while shop interaction isn't wired -
        // the bot grinds gold meanwhile, and once we wire shopping the
        // gold gets converted into the actual armor/weapon.
        String key = type.getRequirementKey();
        boolean rankByGp = type.getCategory() == Goal.GoalCategory.ECONOMIC
            || (key != null && (key.startsWith("equipment:") || key.startsWith("weapon:")));

        // Two-pass: find the top score, then collect every method within
        // 5% of it. Pick one of those at random per-bot so 10 bots on the
        // same goal scatter across the alternatives instead of all going
        // to the single highest-ranked location.
        int bestScore = Integer.MIN_VALUE;
        for (Method m : ALL) {
            if (required != null && m.kind != required) continue;
            if (!m.isApplicable(bot)) continue;
            int score = rankByGp ? m.rankForGp(bot) : m.rankFor(m.kind, bot);
            if (score > bestScore) bestScore = score;
        }
        if (bestScore == Integer.MIN_VALUE) return null;
        int threshold = bestScore - Math.max(1, Math.abs(bestScore) / 20);
        java.util.List<Method> contenders = new java.util.ArrayList<Method>();
        for (Method m : ALL) {
            if (required != null && m.kind != required) continue;
            if (!m.isApplicable(bot)) continue;
            int score = rankByGp ? m.rankForGp(bot) : m.rankFor(m.kind, bot);
            if (score >= threshold) contenders.add(m);
        }
        if (contenders.isEmpty()) return null;
        // Per-bot deterministic pick - same bot keeps the same method
        // per goal so it doesn't oscillate, but different bots pick
        // different ones from the contender pool.
        int pick = (int) ((bot.getIndex() * 2654435761L) >>> 1) % contenders.size();
        return contenders.get(pick);
    }

    /**
     * Map a GoalType to the activity Kind that satisfies it. Skill 99 goals
     * obviously map to that skill's gathering action. Wealth/Economic goals
     * map to whatever the bot is best at - we'll pick the highest-GP method
     * across all kinds, so the bot ends up doing whatever pays best given
     * its current stats. Combat training goals map to COMBAT.
     */
    private static Kind kindForGoal(GoalType type) {
        String key = type.getRequirementKey() == null ? "" : type.getRequirementKey();
        if (key.startsWith("skill:woodcutting")) return Kind.WOODCUTTING;
        if (key.startsWith("skill:mining"))      return Kind.MINING;
        if (key.startsWith("skill:fishing"))     return Kind.FISHING;
        if (key.startsWith("skill:thieving"))    return Kind.THIEVING;
        if (key.startsWith("skill:firemaking"))  return Kind.FIREMAKING;
        if (key.startsWith("skill:cooking"))     return Kind.COOKING;
        if (key.startsWith("skill:smithing"))    return Kind.SMELTING; // smelting bars covers most smithing xp
        if (key.startsWith("skill:crafting"))    return Kind.CRAFTING;
        if (key.startsWith("skill:prayer"))      return Kind.PRAYER;
        // Slayer = combat with monsters; existing combat library covers slayer monsters
        // (abyssal demons, gargoyles, dust devils, nechryael).
        if (key.startsWith("skill:slayer"))      return Kind.COMBAT;
        // Hitpoints and other combat-stat goals share combat methods (style differs).
        if (key.startsWith("skill:hitpoints"))   return Kind.COMBAT;
        if (key.startsWith("skill:attack")
            || key.startsWith("skill:strength")
            || key.startsWith("skill:defence")
            || key.startsWith("skill:hitpoints")
            || key.startsWith("skill:ranged")
            || key.startsWith("skill:magic")
            || key.startsWith("combat:"))         return Kind.COMBAT;
        // ECONOMIC goals and equipment/weapon goals fall through to "any
        // kind, ranked by gp" - the bot picks the best-paying activity it
        // qualifies for (the rank-by-GP path in bestMethodFor handles this).
        return null;
    }
}
