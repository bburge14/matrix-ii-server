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
    /** How many world ticks to wait between each Citizen spawn. 5 ticks = 3
     *  seconds. Each AIPlayer.hydrate() does a real-player-equivalent login -
     *  appearance generation, packet broadcast to all visible players,
     *  region/equipment/inventory init. Doing more than ~1 of those per tick
     *  blows packet buffers under load and drops sessions. 5 tick gap matches
     *  what real-player login pacing looks like in practice. */
    private static final int TICKS_BETWEEN_SPAWNS = 5;

    /**
     * Spawn N Citizens, one at a time across multiple ticks. Returns
     * immediately with an EMPTY list (the spawns happen later) - callers
     * track progress via liveCount() / getLive(). This is intentional:
     * trying to spawn N synchronously locked the world tick for too long
     * and dropped real-player sessions.
     *
     * Total elapsed time = N * TICKS_BETWEEN_SPAWNS * 600ms.
     * 100 citizens = 100 * 5 * 600ms = ~5 minutes (slow but safe).
     * 10 citizens = ~30 seconds.
     *
     * If you need an immediate spawn for testing, call spawnOne() directly.
     */
    public static List<AIPlayer> spawnBatch(final int count, final String category,
                                             final WorldTile anchor, final int scatter) {
        if (count <= 0 || anchor == null) return Collections.emptyList();
        final int safeCount = Math.max(1, Math.min(count, 500));
        final int safeScatter = Math.max(2, scatter);

        // Spawn the first one synchronously so the caller sees IMMEDIATE
        // feedback; the rest drip in over the next safeCount-1 spawn ticks.
        List<AIPlayer> out = new ArrayList<>();
        AIPlayer first = spawnOne(category, anchor, safeScatter);
        if (first != null) out.add(first);

        if (safeCount > 1) {
            com.rs.game.tasks.WorldTasksManager.schedule(
                new com.rs.game.tasks.WorldTask() {
                    int left = safeCount - 1;
                    @Override public void run() {
                        if (left <= 0) { stop(); return; }
                        spawnOne(category, anchor, safeScatter);
                        left--;
                        if (left <= 0) stop();
                    }
                }, TICKS_BETWEEN_SPAWNS, TICKS_BETWEEN_SPAWNS);
        }

        System.out.println("[CitizenSpawner] queued " + safeCount + " "
            + (category == null ? "mixed" : category) + " citizens around " + anchor
            + " (one every " + TICKS_BETWEEN_SPAWNS + " ticks; first one spawned now)");
        return out;
    }

    /**
     * Spawn a single Citizen synchronously. Internal helper for the
     * scheduled drip in spawnBatch. Public for tests + admin "spawn 1
     * right now" use. Each call does a full AIPlayer hydrate which is
     * comparable to a real player login - safe to call once but NOT in
     * a tight loop.
     */
    public static AIPlayer spawnOne(String category, WorldTile anchor, int scatter) {
        if (anchor == null) return null;
        AmbientArchetype arch = AmbientArchetype.randomFor(category);
        // Per-minigame archetypes pin the spawn to their lobby tile regardless
        // of the caller's anchor. Lets admin panel spawn castlewars/soulwars/sc
        // citizens via category=castlewars etc and have them appear at the
        // correct minigame lobby instead of wherever the caller picked.
        com.rs.game.WorldTile minigameLobby = arch == null ? null : arch.lobbyTile();
        if (minigameLobby != null) anchor = minigameLobby;
        scatter = Math.max(2, scatter);
        int dx = gaussianOffset(scatter);
        int dy = gaussianOffset(scatter);
        WorldTile spawn = new WorldTile(
            anchor.getX() + dx, anchor.getY() + dy, anchor.getPlane());
        int wanderRadius = 4 + Utils.random(8);

        String name = com.rs.bot.BotNames.generate();
        // === SAME PIPELINE AS LEGENDS ===
        // BotPool.generate() does:
        //   profile = BotSkillProfile.build(mode, targetCb, archetype)
        //   bot     = BotFactory.createOffline(name, profile, archetype)
        //   ... save to disk ...
        //   bot.hydrate(name); bot.start(); bot.setBrain(...);
        // We mirror exactly. Stats baked in BEFORE hydrate so:
        //   - the saved-on-disk shape matches Legends (ephemeral but identical)
        //   - applyLoadout can read true levels and gate gear correctly
        //   - skill checks (Mining/Fishing/etc level requirements) pass
        int targetCb = pickCombatLevel(arch);
        String archetypeStr = archetypeToLoadoutString(arch);
        // "set" mode = build to the target combat level via the archetype.
        // "default" mode RETURNS NULL (preserves vanilla level-1 stats), which
        // is why earlier Citizens were spawning at cb 3 regardless of role.
        int[] profile;
        try {
            profile = com.rs.bot.BotSkillProfile.build("set", targetCb, archetypeStr);
        } catch (Throwable t) {
            System.err.println("[CitizenSpawner] BotSkillProfile.build failed: " + t);
            profile = null;
        }
        AIPlayer bot = BotFactory.createOffline(name, profile, archetypeStr);
        if (bot == null) {
            System.err.println("[CitizenSpawner] BotFactory.createOffline returned null for " + name);
            return null;
        }
        try {
            // Match BotPool.spawn() order exactly: hydrate -> start -> setBrain.
            bot.hydrate(name); // single init() - same path Legends use
            bot.start();       // bot.start() finalizes the entry into world
            try {
                bot.setNextWorldTile(spawn);
            } catch (Throwable ignore) {}

            // Apply gear loadout. BotEquipment.applyLoadout reads the skill
            // levels we just baked in via BotSkillProfile.build, so gear is
            // already correctly gated (no rune armor on a cb-3 bot, no
            // dragon dagger without 60 attack).
            try {
                com.rs.bot.BotEquipment.applyLoadout(bot, archetypeStr, targetCb);
            } catch (Throwable t) {
                System.err.println("[CitizenSpawner] applyLoadout failed for " + name + ": " + t);
            }

            // Pre-stock trader bots so they have inventory at first trade.
            // Otherwise the trade handler tries to materialize at trade-time
            // which silently failed (full inv from toolkit, non-stackable
            // items > slots, etc) and traders cancelled "sold out".
            if (arch == AmbientArchetype.SOCIALITE_GE_TRADER) {
                try {
                    com.rs.bot.ambient.BotTradeHandler.preStockTrader(bot);
                } catch (Throwable ignored) {}
            }

            bot.setBrain(new CitizenBrain(bot, arch, spawn, wanderRadius));
            liveCitizens.add(bot);
            System.out.println("[CitizenSpawner] spawned " + name + " (" + arch.name()
                + ", cb=" + targetCb + ", as=" + archetypeStr + ") at " + spawn
                + " (live=" + liveCitizens.size() + ")");
            return bot;
        } catch (Throwable t) {
            System.err.println("[CitizenSpawner] failed to spawn " + name + " at " + spawn + ": " + t);
            t.printStackTrace();
            return null;
        }
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

    /**
     * Pick a target combat level per archetype. Distribution is intentionally
     * varied within each archetype so 50 combatants don't all show up at cb 80.
     * Skillers stay low to match their non-combat focus.
     */
    private static int pickCombatLevel(AmbientArchetype arch) {
        if (arch == null) return 3 + Utils.random(50);
        if (arch.isSkiller()) {
            // Skillers rarely train combat - a few up to 60, most 3-30.
            return Utils.random(100) < 80 ? 3 + Utils.random(28) : 30 + Utils.random(30);
        }
        if (arch.isCombatant()) {
            switch (arch) {
                case COMBATANT_PURE:   return 40 + Utils.random(60);  // 40-99
                case COMBATANT_TANK:   return 60 + Utils.random(50);  // 60-109
                case COMBATANT_HYBRID: return 70 + Utils.random(50);  // 70-119
                default: return 50 + Utils.random(60);
            }
        }
        if (arch.isMinigamer()) return 70 + Utils.random(50);
        if (arch.isSocialite()) {
            // GE traders + bankstanders run the gamut; gamblers tend mid-high.
            return arch == AmbientArchetype.SOCIALITE_GAMBLER
                ? 80 + Utils.random(40)
                : 30 + Utils.random(80);
        }
        return 30 + Utils.random(60);
    }

    /**
     * Map our internal AmbientArchetype to the string keys BotEquipment
     * expects (archetype-aware loadout dispatch).
     */
    private static String archetypeToLoadoutString(AmbientArchetype arch) {
        if (arch == null) return "main";
        if (arch.isCombatant()) {
            switch (arch) {
                case COMBATANT_PURE:   return "pure";
                case COMBATANT_TANK:   return "tank";
                case COMBATANT_HYBRID: return "hybrid";
                default: return "melee";
            }
        }
        if (arch.isSkiller())   return "skiller";
        if (arch.isMinigamer()) return "main"; // minigamers carry standard combat gear
        // Socialites get fancy/holiday-rare gear (cape, robes, partyhat) -
        // they're standing at GE/dicing areas and should look the part, not
        // wear bronze plate. BotEquipment "socialite" branch picks from a
        // small fashionscape pool incl. holiday rares.
        if (arch.isSocialite()) return "socialite";
        return "main";
    }
}
