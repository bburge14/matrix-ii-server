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
    public enum Kind { WOODCUTTING, MINING, FISHING, COMBAT }

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
        }

        public boolean isApplicable(AIPlayer bot) {
            try {
                if (skill >= 0) {
                    int lvl = bot.getSkills().getLevel(skill);
                    if (lvl < minLevel) return false;
                }
                if (requiredCombatLevel > 0
                        && bot.getSkills().getCombatLevel() < requiredCombatLevel) return false;
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
        TreeDefinitions treeDef;
        RockDefinitions rockDef;
        FishingSpots fishDef;
        int[] npcIds = new int[0];
        Builder(String d, Kind k) { description = d; kind = k; }
        Builder skill(int s) { skill = s; return this; }
        Builder lvl(int min, int max) { minLevel = min; maxLevel = max; return this; }
        Builder at(int x, int y) { location = new WorldTile(x, y, 0); return this; }
        Builder xp(int xph) { xpRate = xph; return this; }
        Builder gp(int gph) { gpRate = gph; return this; }
        Builder needs(int... itemIds) { requiredItems = itemIds; return this; }
        Builder cb(int cbLvl) { requiredCombatLevel = cbLvl; return this; }
        Builder tree(TreeDefinitions t) { treeDef = t; return this; }
        Builder rock(RockDefinitions r) { rockDef = r; return this; }
        Builder fish(FishingSpots f) { fishDef = f; return this; }
        Builder npcs(int... ids) { npcIds = ids; return this; }
        Method build() { return new Method(this); }
    }

    // ===== Method registry =====

    private static final List<Method> ALL = new ArrayList<>();

    static {
        // ---- Woodcutting (location + level + tree-def filter) ----
        // Note: WorldKnowledge.LUMBRIDGE_TREES (3096, 3468) is mislabeled -
        // it's actually near Edgeville/Barbarian Village and full of
        // willows, not normal trees. Use the Draynor coord instead which
        // is real normal trees outside Draynor village.
        ALL.add(b("Chop normal trees - Draynor", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(1, 15).at(3101, 3263).xp(15000).gp(2000)
            .tree(TreeDefinitions.NORMAL).build());
        ALL.add(b("Chop oak trees - Varrock", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(15, 30).at(3280, 3420).xp(22000).gp(8000)
            .tree(TreeDefinitions.OAK).build());
        ALL.add(b("Chop willow trees - Draynor", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(30, 45).at(3087, 3234).xp(40000).gp(15000)
            .tree(TreeDefinitions.WILLOW).build());
        ALL.add(b("Chop maple trees - Seers'", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(45, 60).at(2730, 3502).xp(60000).gp(18000)
            .tree(TreeDefinitions.MAPLE).build());
        ALL.add(b("Chop yew trees - Edgeville", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(60, 75).at(3087, 3481).xp(70000).gp(45000)
            .tree(TreeDefinitions.YEW).build());
        ALL.add(b("Chop magic trees - Tree Gnome", Kind.WOODCUTTING)
            .skill(Skills.WOODCUTTING).lvl(75, 99).at(2692, 3425).xp(80000).gp(80000)
            .tree(TreeDefinitions.MAGIC).build());

        // ---- Mining ----
        ALL.add(b("Mine clay - Draynor", Kind.MINING)
            .skill(Skills.MINING).lvl(1, 15).at(3142, 3305).xp(15000).gp(5000)
            .rock(RockDefinitions.Clay_Ore).build());
        ALL.add(b("Mine tin - Varrock east", Kind.MINING)
            .skill(Skills.MINING).lvl(1, 15).at(3289, 3372).xp(15000).gp(2000)
            .rock(RockDefinitions.Tin_Ore).build());
        ALL.add(b("Mine iron - Varrock east", Kind.MINING)
            .skill(Skills.MINING).lvl(15, 45).at(3289, 3372).xp(35000).gp(20000)
            .rock(RockDefinitions.Iron_Ore).build());
        ALL.add(b("Mine coal - Barbarian Village", Kind.MINING)
            .skill(Skills.MINING).lvl(30, 60).at(3082, 3420).xp(50000).gp(25000)
            .rock(RockDefinitions.Coal_Ore).build());
        ALL.add(b("Mine mithril - Heroes' Guild area", Kind.MINING)
            .skill(Skills.MINING).lvl(55, 75).at(2916, 3506).xp(60000).gp(40000)
            .rock(RockDefinitions.Mithril_Ore).build());
        ALL.add(b("Mine adamantite - Heroes' Guild", Kind.MINING)
            .skill(Skills.MINING).lvl(70, 85).at(2916, 3506).xp(65000).gp(60000)
            .rock(RockDefinitions.Adamant_Ore).build());
        ALL.add(b("Mine runite - Wilderness", Kind.MINING)
            .skill(Skills.MINING).lvl(85, 99).at(3058, 3884).xp(45000).gp(120000)
            .rock(RockDefinitions.Runite_Ore).build());

        // ---- Fishing ----
        ALL.add(b("Net shrimp - Lumbridge swamp", Kind.FISHING)
            .skill(Skills.FISHING).lvl(1, 20).at(3238, 3241).xp(10000).gp(2000)
            .fish(FishingSpots.NET).build());
        ALL.add(b("Bait fly fishing - Barbarian Village", Kind.FISHING)
            .skill(Skills.FISHING).lvl(20, 40).at(3109, 3436).xp(30000).gp(8000)
            .fish(FishingSpots.NET).build());
        ALL.add(b("Cage lobster - Karamja", Kind.FISHING)
            .skill(Skills.FISHING).lvl(40, 76).at(2924, 3178).xp(45000).gp(40000)
            .fish(FishingSpots.CAGE).build());
        ALL.add(b("Harpoon shark - Fishing Guild", Kind.FISHING)
            .skill(Skills.FISHING).lvl(76, 99).at(2611, 3393).xp(55000).gp(80000)
            .fish(FishingSpots.HARPOON).build());

        // ---- Combat training ----
        // Coords/IDs verified against spawnsList.txt.
        ALL.add(b("Train combat - Lumbridge cows", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(1, 30).at(3027, 3307).xp(10000).cb(1)
            .npcs(81, 11238).build());
        ALL.add(b("Train combat - Falador guards", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(30, 60).at(2964, 3396).xp(25000).cb(30)
            .npcs(9).build());
        ALL.add(b("Train combat - Rock crabs", Kind.COMBAT)
            .skill(Skills.ATTACK).lvl(60, 99).at(2721, 3726).xp(45000).cb(60)
            .npcs(1265).build());
    }

    private static Builder b(String d, Kind k) { return new Builder(d, k); }

    // ===== Goal -> method selection =====

    /**
     * Pick the best method for this goal+bot. Returns null if no method
     * applies (e.g. unsupported goal type). Caller should fall back to
     * the legacy WorldKnowledge-only behaviour if this returns null.
     */
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

        Method best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Method m : ALL) {
            if (required != null && m.kind != required) continue;
            if (!m.isApplicable(bot)) continue;
            int score = rankByGp ? m.rankForGp(bot) : m.rankFor(m.kind, bot);
            if (score > bestScore) { bestScore = score; best = m; }
        }
        return best;
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
