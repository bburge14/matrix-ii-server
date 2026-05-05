package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.game.WorldObject;
import com.rs.game.npc.NPC;
import com.rs.game.route.RouteFinder;
import com.rs.game.route.strategy.EntityStrategy;
import com.rs.game.route.strategy.FixedTileStrategy;
import com.rs.game.route.strategy.ObjectStrategy;
import com.rs.utils.Utils;

/**
 * Bot pathfinding wrapper around the engine's RouteFinder.
 *
 * Real players reach distant tiles by way of A*-ish routing (RouteFinder)
 * that walks around walls and other clipped tiles. Plain addWalkSteps with
 * check=true aborts at the first blocked tile, so we'd need to spam tiny
 * straight-line steps and hope nothing's in the way - which is what was
 * leaving bots stuck in Varrock alleys.
 *
 * walkTo(bot, x, y) and walkToObject(bot, obj) try the proper route first.
 * If the engine reports no path, we do a small "wiggle" - pick a random
 * nearby tile and try again next tick. That breaks bots out of corners
 * caused by a stale WorldKnowledge coord that ended up inside a wall.
 */
public final class BotPathing {

    /** Max steps to enqueue per call - keeps the bot reactive between brain ticks. */
    private static final int STEP_BUDGET = 25;

    private BotPathing() {}

    /**
     * Walk to a specific tile on the bot's plane. Returns true if a route
     * was found, false if we fell back to a clipped straight-line walk.
     * Either way the bot moves toward the target - the false return tells
     * the caller "we couldn't fully resolve, try again next tick."
     */
    public static boolean walkTo(AIPlayer bot, int targetX, int targetY) {
        if (bot.getX() == targetX && bot.getY() == targetY) return true;
        // Force-load the source + destination regions before routing.
        // AIPlayer.loadMapRegions is a no-op so a freshly-teleported bot's
        // destination region has no clipping data, RouteFinder returns 0
        // steps, and the fallback addWalkSteps queues nothing. Net result
        // was bots planted on the lodestone tile forever. Mirrors what
        // EnvironmentScanner already does for scan queries.
        ensureRegionLoaded(bot.getX(), bot.getY());
        ensureRegionLoaded(targetX, targetY);
        FixedTileStrategy strategy = new FixedTileStrategy(targetX, targetY);
        if (runRoute(bot, strategy)) return true;
        // RouteFinder failed - check if a closed door is what's blocking
        // us. Doors are clipped tiles that flip to walkable when opened.
        // If we find one within a few tiles, open it and tell the caller
        // we made progress so wiggle doesn't fire. Next tick's walkTo
        // will route through the now-open door.
        if (tryOpenNearbyObstacle(bot, 4)) return false;
        // Still no path - if the destination has a Y coord in the
        // underground band (>= 6400) and we're above ground, try a
        // climb-down. Same the other way for bots stranded on plane 1+.
        // No-op when bot + target share a plane and the underground
        // doesn't apply.
        if (tryUseNearbyLadder(bot, targetX, targetY, 4)) return false;
        // RouteFinder couldn't path us there - usually because the target
        // is outside our loaded scene (cross-region trips). Fall back to
        // a clip-aware straight-line walk: addWalkSteps with check=true
        // will queue tiles toward the target and stop at the first wall.
        // We make progress and the brain can re-route next tick.
        bot.addWalkSteps(targetX, targetY, STEP_BUDGET, true);
        return false;
    }

    /** Walk adjacent to an object so the bot can interact with it. */
    public static boolean walkToObject(AIPlayer bot, WorldObject object) {
        if (object != null) {
            ensureRegionLoaded(bot.getX(), bot.getY());
            ensureRegionLoaded(object.getX(), object.getY());
        }
        ObjectStrategy strategy = new ObjectStrategy(object);
        if (runRoute(bot, strategy)) return true;
        tryOpenNearbyObstacle(bot, 4);
        return false;
    }

    /** Walk adjacent to an NPC - used for fishing spots and combat. */
    public static boolean walkToEntity(AIPlayer bot, NPC npc) {
        if (npc != null) {
            ensureRegionLoaded(bot.getX(), bot.getY());
            ensureRegionLoaded(npc.getX(), npc.getY());
        }
        EntityStrategy strategy = new EntityStrategy(npc);
        if (runRoute(bot, strategy)) return true;
        tryOpenNearbyObstacle(bot, 4);
        return false;
    }

    /**
     * Scan the area around the bot for a closed door / gate / metal door
     * and "click" it programmatically via ObjectHandler.handleDoor. Returns
     * true if we opened something. Closed doors block clipping so RouteFinder
     * gives up on otherwise-reachable destinations - opening them lets the
     * next walkTo tick succeed.
     *
     * Lock + quest gates are intentionally not handled - those need keys /
     * dialogue steps a bot doesn't have. We just look for plain "Open" /
     * "Unlock" options on door-named objects.
     */
    public static boolean tryOpenNearbyObstacle(AIPlayer bot, int radius) {
        try {
            int botRegion = ((bot.getX() >> 6) << 8) + (bot.getY() >> 6);
            com.rs.game.Region region = com.rs.game.World.getRegion(botRegion, true);
            if (region == null) return false;
            java.util.List<WorldObject> objs = region.getAllObjects();
            if (objs == null) return false;
            int plane = bot.getPlane();
            int bx = bot.getX(), by = bot.getY();
            WorldObject best = null;
            int bestDist = Integer.MAX_VALUE;
            for (WorldObject obj : objs) {
                if (obj == null || obj.getPlane() != plane) continue;
                int dx = Math.abs(obj.getX() - bx);
                int dy = Math.abs(obj.getY() - by);
                if (dx > radius || dy > radius) continue;
                com.rs.cache.loaders.ObjectDefinitions def = obj.getDefinitions();
                if (def == null || def.name == null) continue;
                String name = def.name.toLowerCase();
                if (!isDoorLike(name)) continue;
                if (!def.containsOption(0, "Open")
                        && !def.containsOption(0, "Unlock")) continue;
                // Don't re-flip a door we already opened on this tick.
                if (com.rs.game.World.isSpawnedObject(obj)) continue;
                int d = dx * dx + dy * dy;
                if (d < bestDist) { bestDist = d; best = obj; }
            }
            if (best == null) return false;
            com.rs.net.decoders.handlers.ObjectHandler.handleDoor(bot, best, 60000);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Climb a nearby ladder / staircase if one exists and it'd shift the
     * bot toward the target's plane. Mirrors the engine's handleLadder
     * logic (Climb-up = plane+1 same XY, Climb-down = plane-1 same XY)
     * since that's not directly callable - it's a private method that
     * reads from the network packet path.
     *
     * Special case: a target with Y >= 6400 is the underground band - bots
     * standing above ground in a building with a trapdoor nearby get a
     * Climb-down at the trapdoor's tile. Plane-shift ladders inside multi-
     * level buildings (mage tower, fally castle, etc.) work the same way.
     */
    /** Default arity: infer target plane from Y coord (>= 6400 = underground). */
    public static boolean tryUseNearbyLadder(AIPlayer bot, int targetX, int targetY, int radius) {
        // Infer target plane: Y >= 6400 is the underground band, plane 0.
        // Otherwise default to 0 (most surface targets). Multi-storey
        // building targets (Slayer Tower, Wizard Tower) MUST use the
        // 5-arg overload below to pass the actual plane.
        int inferredPlane = (targetY >= 6400) ? 0 : 0;
        return tryUseNearbyLadder(bot, targetX, targetY, inferredPlane, radius);
    }

    /**
     * Climb a nearby ladder / staircase if one exists and it'd shift the
     * bot toward the target's plane. Direction = up if target plane >
     * bot plane, down if lower; underground (Y>=6400) is treated as
     * needing a climb-down even when the target plane field is 0.
     */
    public static boolean tryUseNearbyLadder(AIPlayer bot, int targetX, int targetY, int targetPlane, int radius) {
        try {
            int botPlane = bot.getPlane();
            int targetUndergroundFromAbove =
                (targetY >= 6400 && bot.getY() < 6400) ? 1 : 0;
            int targetAboveFromUnderground =
                (targetY < 6400 && bot.getY() >= 6400) ? 1 : 0;
            // If both are on the same plane and same band, no ladder needed.
            if (botPlane == targetPlane && targetUndergroundFromAbove == 0
                    && targetAboveFromUnderground == 0) {
                return false;
            }
            boolean wantUp = false;
            boolean wantDown = false;
            if (targetPlane > botPlane) {
                // Target is on a higher floor of the same building.
                wantUp = true;
            } else if (targetPlane < botPlane && targetY < 6400 && bot.getY() < 6400) {
                // Stranded on a higher plane at the surface, target is
                // a lower floor of the same building.
                wantDown = true;
            } else if (targetUndergroundFromAbove == 1) {
                wantDown = true;
            } else if (targetAboveFromUnderground == 1) {
                wantUp = true;
            } else {
                wantUp = botPlane < targetPlane;
                wantDown = botPlane > targetPlane;
            }
            int botRegion = ((bot.getX() >> 6) << 8) + (bot.getY() >> 6);
            com.rs.game.Region region = com.rs.game.World.getRegion(botRegion, true);
            if (region == null) return false;
            java.util.List<WorldObject> objs = region.getAllObjects();
            if (objs == null) return false;
            int bx = bot.getX(), by = bot.getY();
            WorldObject best = null;
            String bestOpt = null;
            int bestDist = Integer.MAX_VALUE;
            for (WorldObject obj : objs) {
                if (obj == null || obj.getPlane() != botPlane) continue;
                int dx = Math.abs(obj.getX() - bx);
                int dy = Math.abs(obj.getY() - by);
                if (dx > radius || dy > radius) continue;
                com.rs.cache.loaders.ObjectDefinitions def = obj.getDefinitions();
                if (def == null || def.name == null) continue;
                String name = def.name.toLowerCase();
                if (!isLadderLike(name)) continue;
                String pickedOpt = pickLadderOption(def, wantUp, wantDown);
                if (pickedOpt == null) continue;
                int d = dx * dx + dy * dy;
                if (d < bestDist) { bestDist = d; best = obj; bestOpt = pickedOpt; }
            }
            if (best == null) return false;
            return executeLadderClimb(bot, best, bestOpt);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isLadderLike(String name) {
        return name.equals("ladder")
            || name.equals("staircase")
            || name.equals("stairs")
            || name.equals("trapdoor")
            || name.equals("manhole")
            || name.endsWith(" ladder")
            || name.endsWith(" stairs")
            || name.endsWith(" staircase")
            || name.endsWith(" trapdoor");
    }

    /** Pick the option string (Climb-up / Climb-down / Climb / Walk-up /
     *  Walk-down) on this ladder def that matches the requested direction.
     *  Returns null if the ladder doesn't have a matching option. */
    private static String pickLadderOption(com.rs.cache.loaders.ObjectDefinitions def,
                                           boolean wantUp, boolean wantDown) {
        String[] candidatesUp = { "Climb-up", "Walk-up", "Climb up" };
        String[] candidatesDown = { "Climb-down", "Walk-down", "Climb down", "Open" };
        if (wantUp) {
            for (String c : candidatesUp) {
                if (defContainsOption(def, c)) return c;
            }
        }
        if (wantDown) {
            for (String c : candidatesDown) {
                if (defContainsOption(def, c)) return c;
            }
        }
        // "Climb" = ambiguous; engine pops a dialogue. Prefer up if we
        // can't tell, since that matches normal ladder UX.
        if (defContainsOption(def, "Climb")) return "Climb";
        return null;
    }

    private static boolean defContainsOption(com.rs.cache.loaders.ObjectDefinitions def, String opt) {
        if (def == null) return false;
        for (int i = 0; i < 5; i++) {
            if (def.containsOption(i, opt)) return true;
        }
        return false;
    }

    /** Execute the climb. Mirrors ObjectHandler.handleLadder behaviour for
     *  the generic plane-shift case: climb-up moves the bot to plane+1
     *  same XY, climb-down to plane-1 same XY. The hand-rolled hard cases
     *  (trapdoors with explicit dest tiles like Edge dungeon) are the
     *  ObjectHandler's concern; for bots we just plane-shift, which gets
     *  most multi-level buildings + Mining Guild ladder right. */
    private static boolean executeLadderClimb(AIPlayer bot, WorldObject obj, String option) {
        try {
            int plane = bot.getPlane();
            com.rs.game.WorldTile dest;
            String opt = option == null ? "" : option;
            if (opt.startsWith("Climb-up") || opt.startsWith("Walk-up")
                    || opt.startsWith("Climb up")) {
                if (plane >= 3) return false;
                dest = new com.rs.game.WorldTile(bot.getX(), bot.getY(), plane + 1);
            } else if (opt.startsWith("Climb-down") || opt.startsWith("Walk-down")
                    || opt.startsWith("Climb down")) {
                if (plane <= 0) return false;
                dest = new com.rs.game.WorldTile(bot.getX(), bot.getY(), plane - 1);
            } else if (opt.equalsIgnoreCase("Climb")) {
                // Ambiguous - guess up unless we're already at the top.
                if (plane >= 3) {
                    dest = new com.rs.game.WorldTile(bot.getX(), bot.getY(), plane - 1);
                } else {
                    dest = new com.rs.game.WorldTile(bot.getX(), bot.getY(), plane + 1);
                }
            } else if (opt.equalsIgnoreCase("Open")) {
                // Trapdoor fallthrough - generic Open is mostly for
                // doors but trapdoors expose it too. Without an explicit
                // dest mapping we don't move; just open the lid (a real
                // human would then click Climb-down on the now-opened
                // tile, which we'll catch on the next walkTo call).
                com.rs.net.decoders.handlers.ObjectHandler.handleDoor(bot, obj, 60000);
                return true;
            } else {
                return false;
            }
            // Force-load the destination region BEFORE the lock fires so
            // RouteFinder has clipping the moment we land. Same trick we
            // use for teleports.
            ensureRegionLoaded(dest.getX(), dest.getY());
            bot.useStairs(828, dest, 1, 2);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isDoorLike(String name) {
        return name.equals("door")
            || name.equals("gate")
            || name.equals("large door")
            || name.equals("metal door")
            || name.equals("oak door")
            || name.equals("wooden door")
            || name.endsWith(" door")
            || name.endsWith(" gate");
    }

    /** Force-load the region containing (x,y) so RouteFinder has clipping. */
    private static void ensureRegionLoaded(int x, int y) {
        try {
            int regionId = ((x >> 6) << 8) + (y >> 6);
            com.rs.game.World.getRegion(regionId, true);
        } catch (Throwable ignored) {}
    }

    private static boolean runRoute(AIPlayer bot, com.rs.game.route.RouteStrategy strategy) {
        int steps;
        try {
            steps = RouteFinder.findRoute(
                RouteFinder.WALK_ROUTEFINDER,
                bot.getX(), bot.getY(), bot.getPlane(),
                bot.getSize(), strategy, true);
        } catch (Throwable t) {
            return false;
        }
        if (steps <= 0) return false; // already there or no route

        int[] bufX = RouteFinder.getLastPathBufferX();
        int[] bufY = RouteFinder.getLastPathBufferY();
        bot.resetWalkSteps();
        // Buffers are reverse-ordered (destination at index 0, next-step-from-source at index steps-1).
        for (int i = steps - 1; i >= 0 && i >= steps - STEP_BUDGET; i--) {
            if (!bot.addWalkSteps(bufX[i], bufY[i], 25, true)) break;
        }
        return true;
    }

    /**
     * Random wiggle - pick a random walkable tile within radius and try to
     * walk to it. Used as fallback when the bot is stuck (no route found
     * to its goal). Stops bots from spinning in place forever.
     */
    public static void wiggle(AIPlayer bot, int radius) {
        for (int attempt = 0; attempt < 6; attempt++) {
            int dx = Utils.random(-radius, radius + 1);
            int dy = Utils.random(-radius, radius + 1);
            if (dx == 0 && dy == 0) continue;
            int tx = bot.getX() + dx;
            int ty = bot.getY() + dy;
            if (walkTo(bot, tx, ty)) return;
        }
        // If everything fails the bot stays put for this tick. Better than
        // a noclip teleport - a real human would also be momentarily stuck.
    }
}
