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
        FixedTileStrategy strategy = new FixedTileStrategy(targetX, targetY);
        if (runRoute(bot, strategy)) return true;
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
        ObjectStrategy strategy = new ObjectStrategy(object);
        return runRoute(bot, strategy);
    }

    /** Walk adjacent to an NPC - used for fishing spots and combat. */
    public static boolean walkToEntity(AIPlayer bot, NPC npc) {
        EntityStrategy strategy = new EntityStrategy(npc);
        return runRoute(bot, strategy);
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
