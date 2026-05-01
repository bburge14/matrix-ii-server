package com.rs.bot.ambient;

import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages the Citizen population - spawns and despawns lightweight
 * AmbientBots in batches, by archetype.
 *
 * Why not just use World.spawnNPC directly: we want to track which NPCs
 * are "ours" so admin commands like ::clearcitizens don't accidentally
 * delete real game NPCs. We keep a Set of spawned indexes; clearAll()
 * walks that set and despawns each.
 *
 * The "human-looking NPC" pool is the trick: Citizens render as ordinary
 * man/woman/farmer NPCs so they blend in with the player population.
 * Mixing real-NPC IDs in lets us reuse models without writing custom ones.
 */
public final class CitizenSpawner {

    private CitizenSpawner() {}

    /** NPC IDs that look human-ish - hardcoded fallback when we don't have
     *  archetype-specific picks. These are bog-standard generic NPCs from
     *  the 718 codebase: man, woman, peasant, merchant, hero variants, etc.
     *  The exact IDs don't matter for behaviour; visuals only. */
    private static final int[] CITIZEN_NPC_POOL = {
        1, 2, 3, 4, 5, 6, 7, 8,                  // man/woman variants
        9, 10, 11, 12, 13, 14, 15,                // guards, knights, farmers
        16, 17, 18, 19, 20, 21,                   // misc citizens
        24, 25, 26, 27, 28, 29,                   // dwarves
        296, 489, 490, 491, 492, 493              // tutorial-ish models
    };

    /** Track every Citizen we've spawned so clearAll() can walk them. */
    private static final Set<AmbientBot> liveCitizens = Collections.synchronizedSet(new HashSet<>());

    /**
     * Spawn N Citizens of the given archetype, scattered around an anchor
     * point. Each gets a small wander radius and Gaussian-jittered start
     * tile so they don't clump on top of each other.
     *
     * @param count       how many to spawn
     * @param category    "skiller"/"combatant"/"socialite"/"minigamer"/null=mixed
     * @param anchor      world tile to scatter around
     * @param scatter     radius in tiles to spread the spawn over
     * @return list of newly-spawned bots
     */
    public static List<AmbientBot> spawnBatch(int count, String category,
                                              WorldTile anchor, int scatter) {
        if (count <= 0 || anchor == null) return Collections.emptyList();
        scatter = Math.max(2, scatter);
        List<AmbientBot> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // Gaussian scatter - clusters near the anchor with thin tails.
            int dx = gaussianOffset(scatter);
            int dy = gaussianOffset(scatter);
            WorldTile spawn = new WorldTile(
                anchor.getX() + dx,
                anchor.getY() + dy,
                anchor.getPlane()
            );
            AmbientArchetype arch = AmbientArchetype.randomFor(category);
            int npcId = pickNpcIdFor(arch);
            int wanderRadius = 4 + Utils.random(8); // 4-12 tile wander
            try {
                AmbientBot bot = new AmbientBot(npcId, spawn, arch, wanderRadius);
                World.addNPC(bot);
                liveCitizens.add(bot);
                out.add(bot);
            } catch (Throwable t) {
                System.err.println("[CitizenSpawner] failed to spawn at " + spawn + ": " + t);
            }
        }
        System.out.println("[CitizenSpawner] spawned " + out.size() + " " + category + " citizens at " + anchor);
        return out;
    }

    /** Remove every Citizen we've spawned. Called by ::clearcitizens. */
    public static int clearAll() {
        int removed = 0;
        synchronized (liveCitizens) {
            for (AmbientBot bot : liveCitizens) {
                try {
                    World.removeNPC(bot);
                    bot.finish();
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

    /** Snapshot of the live citizen list (read-only). */
    public static List<AmbientBot> getLive() {
        synchronized (liveCitizens) {
            return new ArrayList<>(liveCitizens);
        }
    }

    public static int liveCount() {
        return liveCitizens.size();
    }

    /** Per-archetype NPC ID picker. The pool here can be expanded as we
     *  identify better-fitting models per archetype - e.g. combat citizens
     *  could use guard/warrior NPC IDs, gamblers could use suspicious-looking
     *  NPCs, etc. For now we use the generic pool and let chatter +
     *  animations carry the personality. */
    private static int pickNpcIdFor(AmbientArchetype arch) {
        // Future: archetype-specific NPC lists. e.g.:
        //   if (arch == COMBATANT_PURE) return GUARD_IDS[Utils.random(...)];
        //   if (arch == SOCIALITE_GE_TRADER) return MERCHANT_IDS[...];
        return CITIZEN_NPC_POOL[Utils.random(CITIZEN_NPC_POOL.length)];
    }

    /** Symmetric Gaussian offset clamped to [-radius, radius]. */
    private static int gaussianOffset(int radius) {
        // Sum of three uniforms approximates a normal distribution.
        double v = (Math.random() + Math.random() + Math.random() - 1.5) / 1.5 * radius;
        if (v < -radius) v = -radius;
        if (v > radius)  v = radius;
        return (int) v;
    }
}
