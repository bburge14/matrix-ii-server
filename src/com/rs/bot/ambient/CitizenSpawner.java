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

                // Stats + gear: each Citizen gets archetype-appropriate combat
                // level + tier-appropriate loadout. A pure-skiller stays low
                // combat in skiller gear, a combatant rolls 60-110 cb with
                // mid-to-high tier melee gear, etc. Variety within the role
                // keeps them visually distinct.
                int targetCb = pickCombatLevel(arch);
                applyArchetypeStats(bot, arch, targetCb);
                try {
                    com.rs.bot.BotEquipment.applyLoadout(bot,
                        archetypeToLoadoutString(arch), targetCb);
                } catch (Throwable t) {
                    System.err.println("[CitizenSpawner] applyLoadout failed for " + name + ": " + t);
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
     * Set the bot's skills to roughly match the target combat level. We can't
     * just call Skills.set blindly - that might NPE if packets aren't fully
     * wired - so we wrap each call in try/catch and skip on failure.
     *
     * Combat level formula approximation: sets attack/strength/defence/HP
     * to a level appropriate for the target cb, with some scatter so two
     * combatants at cb 80 have slightly different builds.
     */
    private static void applyArchetypeStats(AIPlayer bot, AmbientArchetype arch, int targetCb) {
        if (arch == null) return;
        try {
            com.rs.game.player.Skills s = bot.getSkills();
            // Combat skills: scale toward targetCb with archetype tilt
            int meleeCore = clamp(targetCb - 5, 1, 99);
            int rangeCore = clamp(targetCb - 5, 1, 99);
            int magicCore = clamp(targetCb - 5, 1, 99);
            int defCore   = clamp(targetCb - 10, 1, 99);
            int hpCore    = clamp(targetCb - 5, 10, 99);

            // Per-archetype tilt
            if (arch == AmbientArchetype.COMBATANT_PURE) {
                defCore = 1; // pures stay 1 def
            } else if (arch == AmbientArchetype.COMBATANT_TANK) {
                defCore = clamp(targetCb, 1, 99);
            } else if (arch == AmbientArchetype.COMBATANT_HYBRID) {
                rangeCore = clamp(targetCb - 3, 1, 99);
                magicCore = clamp(targetCb - 3, 1, 99);
            } else if (arch.isSkiller()) {
                // skillers keep combat low, push gathering skills high
                meleeCore = 1; rangeCore = 1; magicCore = 1; defCore = 1;
                hpCore = 10;
                int skillCap = arch == AmbientArchetype.SKILLER_EFFICIENT ? 99
                             : arch == AmbientArchetype.SKILLER_CASUAL ? 85
                             : 50;
                trySet(s, com.rs.game.player.Skills.WOODCUTTING, 30 + Utils.random(skillCap - 30));
                trySet(s, com.rs.game.player.Skills.MINING,      30 + Utils.random(skillCap - 30));
                trySet(s, com.rs.game.player.Skills.FISHING,     30 + Utils.random(skillCap - 30));
                trySet(s, com.rs.game.player.Skills.COOKING,     30 + Utils.random(skillCap - 30));
                trySet(s, com.rs.game.player.Skills.CRAFTING,    20 + Utils.random(skillCap - 20));
            }

            trySet(s, com.rs.game.player.Skills.ATTACK,    meleeCore + Utils.random(6) - 3);
            trySet(s, com.rs.game.player.Skills.STRENGTH,  meleeCore + Utils.random(6) - 3);
            trySet(s, com.rs.game.player.Skills.DEFENCE,   defCore   + Utils.random(6) - 3);
            trySet(s, com.rs.game.player.Skills.HITPOINTS, hpCore);
            trySet(s, com.rs.game.player.Skills.RANGE,     rangeCore + Utils.random(6) - 3);
            trySet(s, com.rs.game.player.Skills.MAGIC,     magicCore + Utils.random(6) - 3);
            trySet(s, com.rs.game.player.Skills.PRAYER,    clamp(targetCb / 2, 1, 95));
        } catch (Throwable t) {
            // best-effort - if Skills isn't fully wired, citizen just spawns at default
        }
    }

    private static void trySet(com.rs.game.player.Skills s, int skill, int level) {
        if (level < 1) level = 1;
        if (level > 99) level = 99;
        try { s.set(skill, level); } catch (Throwable ignored) {}
        try { s.setXp(skill, levelToXp(level)); } catch (Throwable ignored) {}
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    /** Approximate XP for a level (Jagex's exact formula for 1-99). */
    private static int levelToXp(int level) {
        if (level <= 1) return 0;
        double sum = 0;
        for (int l = 1; l < level; l++) {
            sum += Math.floor(l + 300.0 * Math.pow(2.0, l / 7.0));
        }
        return (int) (sum / 4);
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
        if (arch.isSocialite()) return "main"; // socialites flash gear
        return "main";
    }
}
