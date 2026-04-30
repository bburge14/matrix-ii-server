package com.rs.bot.ai;

import java.util.List;

import com.rs.cache.loaders.ObjectDefinitions;
import com.rs.game.Region;
import com.rs.game.World;
import com.rs.game.WorldObject;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.player.actions.Fishing.FishingSpots;
import com.rs.game.player.actions.Woodcutting.TreeDefinitions;
import com.rs.game.player.actions.mining.Mining.RockDefinitions;

/**
 * Lets a bot find real game objects (trees, rocks) and NPCs (fishing spots,
 * combat targets) near its current position.
 *
 * Why this exists: WorldKnowledge gives us hardcoded coordinates for "where
 * the trees are", but the bot still needs to look at the actual game state
 * once it arrives so it can interact with the right object - one that's
 * standing, not felled, and within reach. This scanner closes that loop.
 *
 * The matching is name-based against ObjectDefinitions because the codebase
 * already uses that pattern (see ObjectHandler tree/rock branches).
 */
public final class EnvironmentScanner {

    private EnvironmentScanner() {}

    /** Pair of (object, definition) for skilling routing. */
    public static final class TreeMatch {
        public final WorldObject object;
        public final TreeDefinitions definition;
        public TreeMatch(WorldObject o, TreeDefinitions d) { this.object = o; this.definition = d; }
    }

    public static final class RockMatch {
        public final WorldObject object;
        public final RockDefinitions definition;
        public RockMatch(WorldObject o, RockDefinitions d) { this.object = o; this.definition = d; }
    }

    public static final class FishMatch {
        public final NPC npc;
        public final FishingSpots definition;
        public FishMatch(NPC n, FishingSpots d) { this.npc = n; this.definition = d; }
    }

    /**
     * Find the nearest tree-like object the bot can chop, plus its TreeDefinitions.
     * Returns null if nothing matches within radius.
     */
    public static TreeMatch findNearestTree(WorldTile from, int radius) {
        return findNearestTree(from, radius, null);
    }

    /**
     * Find the nearest tree of a specific type. Used when a TrainingMethod
     * directs the bot to e.g. yew trees so we don't grab a regular tree
     * that happens to be closer.
     */
    public static TreeMatch findNearestTree(WorldTile from, int radius, TreeDefinitions only) {
        WorldObject best = null;
        TreeDefinitions bestDef = null;
        int bestDist = Integer.MAX_VALUE;
        for (WorldObject o : nearbyObjects(from, radius)) {
            String name = nameOf(o);
            if (name == null) continue;
            TreeDefinitions def = matchTree(name);
            if (def == null) continue;
            if (only != null && def != only) continue;
            int d = manhattan(from, o);
            if (d < bestDist) {
                bestDist = d;
                best = o;
                bestDef = def;
            }
        }
        return best == null ? null : new TreeMatch(best, bestDef);
    }

    /**
     * Find the nearest rock the bot can mine. Matches by object name keyword
     * (Copper, Tin, Iron, Coal, Mithril, Adamant, Runite, ...).
     */
    public static RockMatch findNearestRock(WorldTile from, int radius) {
        return findNearestRock(from, radius, null);
    }

    public static RockMatch findNearestRock(WorldTile from, int radius, RockDefinitions only) {
        WorldObject best = null;
        RockDefinitions bestDef = null;
        int bestDist = Integer.MAX_VALUE;
        for (WorldObject o : nearbyObjects(from, radius)) {
            String name = nameOf(o);
            if (name == null) continue;
            RockDefinitions def = matchRock(name);
            if (def == null) continue;
            if (only != null && def != only) continue;
            int d = manhattan(from, o);
            if (d < bestDist) {
                bestDist = d;
                best = o;
                bestDef = def;
            }
        }
        return best == null ? null : new RockMatch(best, bestDef);
    }

    /**
     * Find the nearest fishing-spot NPC. Fishing spots are NPCs (id 327, 312,
     * 313, 6267, ...) not world objects.
     */
    public static FishMatch findNearestFishingSpot(WorldTile from, int radius) {
        return findNearestFishingSpot(from, radius, null);
    }

    public static FishMatch findNearestFishingSpot(WorldTile from, int radius, FishingSpots only) {
        NPC best = null;
        FishingSpots bestDef = null;
        int bestDist = Integer.MAX_VALUE;
        for (NPC n : World.getNPCs()) {
            if (n == null || n.hasFinished()) continue;
            if (n.getPlane() != from.getPlane()) continue;
            if (!from.withinDistance(n, radius)) continue;
            FishingSpots def = matchFishingSpot(n.getId());
            if (def == null) continue;
            if (only != null && def != only) continue;
            int d = manhattan(from, n);
            if (d < bestDist) {
                bestDist = d;
                best = n;
                bestDef = def;
            }
        }
        return best == null ? null : new FishMatch(best, bestDef);
    }

    /**
     * Generic NPC scan for combat targets. Matches by allowed NPC IDs.
     */
    public static NPC findNearestNPC(WorldTile from, int radius, int... allowedIds) {
        NPC best = null;
        int bestDist = Integer.MAX_VALUE;
        for (NPC n : World.getNPCs()) {
            if (n == null || n.hasFinished() || n.isDead()) continue;
            if (n.getPlane() != from.getPlane()) continue;
            if (!from.withinDistance(n, radius)) continue;
            if (allowedIds.length > 0 && !contains(allowedIds, n.getId())) continue;
            int d = manhattan(from, n);
            if (d < bestDist) {
                bestDist = d;
                best = n;
            }
        }
        return best;
    }

    /** Find a walkable tile adjacent to an object (1 tile cardinal offset). */
    public static WorldTile adjacentTile(WorldObject o) {
        // Cardinal first, then diagonals. The clip-aware addWalkSteps will
        // reject blocked picks; the BotBrain falls back to the next pick.
        int[][] offsets = { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {-1,1}, {1,-1}, {-1,-1} };
        for (int[] off : offsets) {
            int nx = o.getX() + off[0];
            int ny = o.getY() + off[1];
            if (World.isTileFree(o.getPlane(), nx, ny, 1)) {
                return new WorldTile(nx, ny, o.getPlane());
            }
        }
        // Fallback to the object tile itself if nothing adjacent passes the clip
        return new WorldTile(o.getX() + 1, o.getY(), o.getPlane());
    }

    // ===== Internals =====

    /**
     * Generic name-substring object scanner. Returns the nearest WorldObject
     * whose definition name contains any of the given substrings (case-
     * insensitive). Used for cooking ranges, anvils, furnaces, etc.
     */
    public static WorldObject findNearestObjectByName(WorldTile from, int radius, String... nameSubstrings) {
        WorldObject best = null;
        int bestDist = Integer.MAX_VALUE;
        for (WorldObject o : nearbyObjects(from, radius)) {
            String name = nameOf(o);
            if (name == null) continue;
            String lower = name.toLowerCase();
            boolean match = false;
            for (String s : nameSubstrings) {
                if (s != null && lower.contains(s.toLowerCase())) { match = true; break; }
            }
            if (!match) continue;
            int d = manhattan(from, o);
            if (d < bestDist) {
                bestDist = d;
                best = o;
            }
        }
        return best;
    }

    private static List<WorldObject> nearbyObjects(WorldTile from, int radius) {
        // The bot's region (and the 8 surrounding regions) is the right scan
        // radius for objects. For a 10-tile radius the bot's own region is
        // almost always sufficient (regions are 64x64); we fall back to all
        // map regions the bot has loaded for edge-of-region cases.
        List<Integer> regionIds = new java.util.ArrayList<Integer>();
        regionIds.add(from.getRegionId());
        // Scan the 8 surrounding regions too so we don't miss objects right
        // across a region boundary.
        int regionX = (from.getRegionId() >> 8) & 0xff;
        int regionY = from.getRegionId() & 0xff;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int rx = regionX + dx;
                int ry = regionY + dy;
                if (rx < 0 || ry < 0) continue;
                regionIds.add((rx << 8) | ry);
            }
        }
        List<WorldObject> out = new java.util.ArrayList<WorldObject>();
        for (int id : regionIds) {
            try {
                Region r = World.getRegion(id, false);
                if (r == null) continue;
                java.util.List<WorldObject> objs = r.getAllObjects();
                if (objs == null) continue;
                for (WorldObject o : objs) {
                    if (o == null) continue;
                    if (o.getPlane() != from.getPlane()) continue;
                    if (!from.withinDistance(o, radius)) continue;
                    out.add(o);
                }
            } catch (Throwable ignored) {
                // Region not loaded / partially-initialized - skip silently.
                // The audit reports 'no X in 24' which is correct semantics.
            }
        }
        return out;
    }

    private static String nameOf(WorldObject o) {
        try {
            ObjectDefinitions def = ObjectDefinitions.getObjectDefinitions(o.getId());
            return def == null ? null : def.name;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int manhattan(WorldTile a, WorldTile b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private static boolean contains(int[] arr, int v) {
        for (int x : arr) if (x == v) return true;
        return false;
    }

    /**
     * Map an object name to a TreeDefinitions value. Names come from cache and
     * are typically "Tree", "Oak", "Willow tree", "Yew", "Magic tree", etc.
     */
    private static TreeDefinitions matchTree(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("magic"))  return TreeDefinitions.MAGIC;
        if (lower.contains("yew"))    return TreeDefinitions.YEW;
        if (lower.contains("maple"))  return TreeDefinitions.MAPLE;
        if (lower.contains("teak"))   return TreeDefinitions.TEAK;
        if (lower.contains("willow")) return TreeDefinitions.WILLOW;
        if (lower.contains("oak"))    return TreeDefinitions.OAK;
        if (lower.contains("evergreen") || lower.contains("dead tree")) return TreeDefinitions.EVERGREEN;
        // Plain "Tree" -> NORMAL. Avoid matching unrelated objects whose names
        // happen to contain "tree" by requiring the word to start the name or
        // be exact.
        if (lower.equals("tree") || lower.startsWith("tree ") || lower.endsWith(" tree")) return TreeDefinitions.NORMAL;
        return null;
    }

    private static RockDefinitions matchRock(String name) {
        String lower = name.toLowerCase();
        if (!(lower.contains("rocks") || lower.contains("ore") || lower.contains("rock"))) return null;
        if (lower.contains("runite") || lower.contains("rune")) return RockDefinitions.Runite_Ore;
        if (lower.contains("adamant")) return RockDefinitions.Adamant_Ore;
        if (lower.contains("mithril")) return RockDefinitions.Mithril_Ore;
        if (lower.contains("gold"))    return RockDefinitions.Gold_Ore;
        if (lower.contains("coal"))    return RockDefinitions.Coal_Ore;
        if (lower.contains("silver"))  return RockDefinitions.Silver_Ore;
        if (lower.contains("iron"))    return RockDefinitions.Iron_Ore;
        if (lower.contains("tin"))     return RockDefinitions.Tin_Ore;
        if (lower.contains("copper"))  return RockDefinitions.Copper_Ore;
        if (lower.contains("clay"))    return RockDefinitions.Clay_Ore;
        return null;
    }

    private static FishingSpots matchFishingSpot(int npcId) {
        for (FishingSpots s : FishingSpots.values()) {
            if (s.getId() == npcId) return s;
        }
        return null;
    }
}
