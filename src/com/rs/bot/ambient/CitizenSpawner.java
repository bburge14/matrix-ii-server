package com.rs.bot.ambient;

import com.rs.bot.AIPlayer;
import com.rs.bot.BotFactory;
import com.rs.game.WorldTile;
import com.rs.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spawns and despawns Citizen-tier bots: AIPlayers with a CitizenBrain
 * (FSM) instead of the full BotBrain (goal-driven AI).
 *
 * Citizens vs Legends:
 *   - Both extend AIPlayer (= Player). Both render with full appearance.
 *   - Legends use BotBrain - full goals, training methods, memory, etc.
 *   - Citizens use CitizenBrain - FSM only, no save state.
 *   - Citizens are EPHEMERAL - not added to BotPool, no JSON on disk,
 *     they vanish on server restart.
 *
 * Naming: every Citizen gets a "Citizen-NNNN" name, distinguishable from
 * BotPool's themed Legend names. Detect a Citizen at runtime via:
 *   bot.getBrain() instanceof CitizenBrain
 * or via the live-set tracking maintained here.
 */
public final class CitizenSpawner {

    private CitizenSpawner() {}

    /** Live-tracking. AIPlayers we spawned via this class. clearAll walks the set. */
    private static final Set<AIPlayer> liveCitizens = Collections.synchronizedSet(new HashSet<>());

    /** Monotonic counter so each spawn gets a unique-ish display name. */
    private static final AtomicLong NAME_SEQ = new AtomicLong(System.currentTimeMillis() / 1000);

    /**
     * Spawn N Citizens of the given archetype, scattered around an anchor.
     *
     * @param count       number to spawn (capped 1..500)
     * @param category    "skiller"/"combatant"/"socialite"/"minigamer"/null=mixed
     * @param anchor      world tile to scatter around
     * @param scatter     radius in tiles to spread the spawn over
     * @return list of newly-spawned bots
     */
    public static List<AIPlayer> spawnBatch(int count, String category,
                                             WorldTile anchor, int scatter) {
        if (count <= 0 || anchor == null) return Collections.emptyList();
        count = Math.max(1, Math.min(count, 500));
        scatter = Math.max(2, scatter);

        List<AIPlayer> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int dx = gaussianOffset(scatter);
            int dy = gaussianOffset(scatter);
            WorldTile spawn = new WorldTile(
                anchor.getX() + dx,
                anchor.getY() + dy,
                anchor.getPlane()
            );
            AmbientArchetype arch = AmbientArchetype.randomFor(category);
            int wanderRadius = 4 + Utils.random(8);

            String name = nextName();
            AIPlayer bot = BotFactory.create(name);
            if (bot == null) {
                System.err.println("[CitizenSpawner] BotFactory.create returned null for " + name);
                continue;
            }
            try {
                // Hydrate puts the bot in the world via Player.init/World.addPlayer.
                bot.hydrate(name);

                // Place the bot at the spawn tile. (BotFactory's hydrate spawns
                // at START_PLAYER_LOCATION; we re-position immediately.)
                try {
                    bot.setNextWorldTile(spawn);
                } catch (Throwable t) {
                    // Best-effort - if setNextWorldTile isn't available, the bot
                    // will start at the default spawn and walk to the area.
                }

                bot.setBrain(new CitizenBrain(bot, arch, spawn, wanderRadius));
                liveCitizens.add(bot);
                out.add(bot);
            } catch (Throwable t) {
                System.err.println("[CitizenSpawner] failed to spawn " + name + " at " + spawn + ": " + t);
                t.printStackTrace();
            }
        }
        System.out.println("[CitizenSpawner] spawned " + out.size() + " "
            + (category == null ? "mixed" : category) + " citizens at " + anchor
            + " (live total: " + liveCitizens.size() + ")");
        return out;
    }

    /** Despawn every Citizen we've spawned. */
    public static int clearAll() {
        int removed = 0;
        synchronized (liveCitizens) {
            for (AIPlayer bot : liveCitizens) {
                try {
                    bot.setBrain(null); // stop the FSM
                    bot.finish();       // request finish
                    removed++;
                } catch (Throwable t) {
                    // ignore - we want to despawn as many as possible
                }
            }
            liveCitizens.clear();
        }
        System.out.println("[CitizenSpawner] cleared " + removed + " citizens");
        return removed;
    }

    /** Snapshot of the live citizen list. */
    public static List<AIPlayer> getLive() {
        synchronized (liveCitizens) {
            return new ArrayList<>(liveCitizens);
        }
    }

    public static int liveCount() {
        synchronized (liveCitizens) {
            return liveCitizens.size();
        }
    }

    /** Generate a unique-ish display name for a Citizen. */
    private static String nextName() {
        long n = NAME_SEQ.incrementAndGet();
        // 6-char alphanumeric tail. "Citizen-AB12CD"
        String tail = Long.toString(n, 36);
        if (tail.length() > 6) tail = tail.substring(tail.length() - 6);
        return "Citizen-" + tail.toUpperCase();
    }

    /** Symmetric Gaussian offset clamped to [-radius, radius]. */
    private static int gaussianOffset(int radius) {
        double v = (Math.random() + Math.random() + Math.random() - 1.5) / 1.5 * radius;
        if (v < -radius) v = -radius;
        if (v > radius)  v = radius;
        return (int) v;
    }
}
