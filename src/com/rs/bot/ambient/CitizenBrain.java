package com.rs.bot.ambient;

import com.rs.bot.AIPlayer;
import com.rs.bot.BotBrain;
import com.rs.game.Animation;
import com.rs.game.ForceTalk;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.utils.Utils;

/**
 * Lightweight FSM brain for "Citizen" bots.
 *
 * Architecture: Citizens are AIPlayers (just like the existing Legend bots
 * we've built so far - they render with full Player appearance, equipment,
 * walking, animations). The ONLY difference is they get this brain instead
 * of the full BotBrain.
 *
 * What we trade:
 *   - No goal-driven planning (no LifetimeIdentity, no GoalStack, no
 *     ranked TrainingMethods, no TrainingActivity, no economic decisions).
 *   - No memory system, no personality dimensions, no burnout tracking.
 *   - tick() is ~10x cheaper than BotBrain.tick() at peak.
 *   - Ephemeral - Citizens are not added to BotPool, not saved to disk
 *     across restarts. They vanish on server shutdown.
 *
 * What we keep:
 *   - Full Player rendering (other players see them as players).
 *   - Walking via addWalkSteps, animations via setNextAnimation,
 *     overhead chat via setNextForceTalk.
 *   - The AIPlayer.processEntity() hookup that ticks brain + action manager.
 *
 * FSM states: IDLE / TRAVERSING / INTERACTING / PANICKING.
 *
 * Humanization (per the master prompt):
 *   - Gaussian state durations (sum of three uniforms approximation)
 *   - 0.08% chance/tick of 10-30s AFK pause
 *   - 1.2% chance/tick of misclick (single wrong-tile step)
 *   - 2% chance/IDLE-tick of overhead chatter
 *   - +/-1 path wobble on 30% of wander destinations
 *   - PANICKING when a real player gets within 6 tiles - bot flees
 */
public class CitizenBrain extends BotBrain {

    private static final int PLAYER_AWARENESS_TILES = 6;
    private static final double AFK_PROBABILITY = 0.0008;
    private static final double MISCLICK_PROBABILITY = 0.012;
    private static final double CHATTER_PROBABILITY = 0.02;

    public enum State { IDLE, TRAVERSING, INTERACTING, PANICKING }

    private final AmbientArchetype archetype;
    private final WorldTile homeAnchor;
    private final int homeRadius;

    private State state = State.IDLE;
    private int stateTicksRemaining = 0;
    private long afkUntilMs = 0;
    private long panicCooldownTicks = 0;

    /**
     * Currently-assigned training method. Citizens consume the same
     * TrainingMethods table Legends do, but pick a method at random
     * (no goal-driven scoring) and animate at it instead of firing real
     * actions. Stays null for archetypes with no matching kinds (socialite,
     * pure-minigamer) - those fall back to bare-scanner behaviour.
     */
    private com.rs.bot.ai.TrainingMethods.Method currentMethod;
    /** How many state cycles have elapsed since we picked currentMethod.
     *  Re-pick after METHOD_REPICK_CYCLES so citizens roam between locations. */
    private int methodCyclesElapsed = 0;
    /** ~10 INTERACTING/IDLE cycles before re-picking. With ~30s per cycle
     *  this rotates a citizen through a new method every 3-5 minutes -
     *  similar pacing to a real player's "okay, let's try a different
     *  spot" decision. */
    private static final int METHOD_REPICK_CYCLES = 10;

    public CitizenBrain(AIPlayer bot, AmbientArchetype archetype, WorldTile homeAnchor, int homeRadius) {
        super(bot);
        this.archetype = archetype;
        this.homeAnchor = new WorldTile(homeAnchor);
        this.homeRadius = Math.max(2, homeRadius);
        scheduleNextStateChange(State.IDLE);
    }

    @Override
    public void tick(AIPlayer bot) {
        if (bot == null || bot.hasFinished()) return;

        // AFK pause - skip all logic until expiry.
        if (afkUntilMs > 0) {
            if (System.currentTimeMillis() < afkUntilMs) return;
            afkUntilMs = 0;
        }

        // Real-player proximity -> PANICKING. Cooldown so we don't oscillate.
        if (panicCooldownTicks > 0) panicCooldownTicks--;
        if (panicCooldownTicks <= 0 && state != State.PANICKING) {
            if (realPlayerNearby(bot)) {
                transitionTo(State.PANICKING);
                panicCooldownTicks = 50; // ~30s before re-panic allowed
            }
        }

        // Random AFK / misclick (skip while panicking).
        if (state != State.PANICKING) {
            if (Math.random() < AFK_PROBABILITY) {
                afkUntilMs = System.currentTimeMillis() + (long) gaussianRange(10000, 30000, 5000);
                return;
            }
            if (Math.random() < MISCLICK_PROBABILITY) {
                stepRandom(bot);
            }
        }

        // FSM advance.
        if (--stateTicksRemaining <= 0) {
            advanceState();
        }
        switch (state) {
            case IDLE:        tickIdle(bot);        break;
            case TRAVERSING:  tickTraversing(bot);  break;
            case INTERACTING: tickInteracting(bot); break;
            case PANICKING:   tickPanicking(bot);   break;
        }
    }

    // === FSM transitions ===

    private void advanceState() {
        State next;
        switch (state) {
            case IDLE: {
                int r = Utils.random(100);
                if (r < 60) next = State.TRAVERSING;
                else if (r < 90) next = State.INTERACTING;
                else next = State.IDLE;
                break;
            }
            case TRAVERSING:
                next = Utils.random(100) < 70 ? State.INTERACTING : State.IDLE;
                break;
            case INTERACTING: next = State.IDLE; break;
            case PANICKING:   next = State.IDLE; break;
            default:          next = State.IDLE;
        }
        transitionTo(next);
    }

    private void transitionTo(State next) {
        state = next;
        scheduleNextStateChange(next);
    }

    private void scheduleNextStateChange(State s) {
        switch (s) {
            case IDLE:        stateTicksRemaining = (int) gaussianRange(3, 12, 3); break;
            case TRAVERSING:  stateTicksRemaining = (int) gaussianRange(8, 20, 5); break;
            case INTERACTING: stateTicksRemaining = (int) gaussianRange(15, 40, 8); break;
            case PANICKING:   stateTicksRemaining = (int) gaussianRange(5, 15, 3); break;
        }
    }

    // === Per-state behavior ===

    private void tickIdle(AIPlayer bot) {
        if (Math.random() < CHATTER_PROBABILITY) {
            String line = archetype.randomChatter();
            if (line != null) {
                try { bot.setNextForceTalk(new ForceTalk(line)); }
                catch (Throwable ignored) {}
            }
        }
    }

    private void tickTraversing(AIPlayer bot) {
        if (bot.hasWalkSteps()) return;
        // Try to walk toward a real interaction target; fall back to random
        // wander if there's nothing scannable in the home radius.
        WorldTile target = findInteractionDestination(bot);
        if (target == null) target = pickWanderTarget();
        bot.addWalkSteps(target.getX(), target.getY(), 8, true);
    }

    private void tickInteracting(AIPlayer bot) {
        // Stand next to a real world object/NPC and animate. Refresh the
        // target each interacting tick so a felled tree / dead npc / despawned
        // fishing spot doesn't lock the citizen into an empty animation.
        if (faceAndAnimateTarget(bot)) return;
        // Fallback: if we can't find a target nearby, just play the anim
        // in place. That's the old behaviour - looks like skill training in
        // mid-air but at least keeps the citizen visually active.
        int anim = archetype.randomInteractAnimation();
        if (anim > 0) {
            try { bot.setNextAnimation(new Animation(anim)); }
            catch (Throwable ignored) {}
        }
    }

    /**
     * Pick a real interaction destination via the SAME TrainingMethods data
     * Legends consume. The flow:
     *   1) If we don't have a current method, pick one at random from the
     *      bot's applicable pool (filtered by archetype kinds + bot stats).
     *   2) Return method.location as the travel destination.
     *
     * Falls back to bare-scanner behaviour for archetypes that don't have
     * matching method kinds (socialite at GE booth, etc).
     */
    private WorldTile findInteractionDestination(AIPlayer bot) {
        // Try the TrainingMethods route first (shared with Legends)
        if (currentMethod == null || ++methodCyclesElapsed >= METHOD_REPICK_CYCLES) {
            com.rs.bot.ai.TrainingMethods.Method old = currentMethod;
            currentMethod = pickRandomMethodForRole(bot);
            methodCyclesElapsed = 0;
            if (currentMethod != null) {
                debug(bot, "picked method '" + currentMethod.description + "' @ "
                    + currentMethod.location.getX() + "," + currentMethod.location.getY()
                    + (old == null ? " (initial)" : " (was: " + old.description + ")"));
            } else if (old != null) {
                debug(bot, "no applicable method - cleared previous '" + old.description + "'");
            } else {
                debug(bot, "no applicable method for role " + archetype);
            }
        }
        if (currentMethod != null && currentMethod.location != null) {
            return currentMethod.location;
        }

        // Fallback: archetypes without matching method kinds (socialites,
        // bankstanders) hit the world objects directly via name match.
        try {
            int radius = Math.max(homeRadius, 12);
            if (archetype.isSocialite()) {
                com.rs.game.WorldObject o =
                    com.rs.bot.ai.EnvironmentScanner.findNearestObjectByName(
                        homeAnchor, radius, "bank booth", "grand exchange", "ge clerk");
                if (o != null) return tileNextTo(o);
            }
            if (archetype.isMinigamer()) {
                com.rs.game.npc.NPC n =
                    com.rs.bot.ai.EnvironmentScanner.findNearestNPC(homeAnchor, radius);
                if (n != null) return tileNextTo(n);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Pick a TrainingMethods.Method at random from the pool of methods
     * compatible with this citizen's archetype + stats. The "random"
     * here is intentional - Citizens don't optimize like Legends do;
     * they roam through level-appropriate methods to look populated.
     *
     * Filters:
     *   - method.kind must match one of the role's allowed kinds
     *   - method.isApplicable(bot) gates on level + items + danger
     *
     * Returns null when nothing applies (e.g. archetype with no kind
     * mapping, or bot's stats don't qualify for any method yet).
     */
    private com.rs.bot.ai.TrainingMethods.Method pickRandomMethodForRole(AIPlayer bot) {
        java.util.Set<com.rs.bot.ai.TrainingMethods.Kind> allowedKinds = allowedKindsFor(archetype);
        if (allowedKinds.isEmpty()) return null;
        java.util.List<com.rs.bot.ai.TrainingMethods.Method> applicable = new java.util.ArrayList<>();
        for (com.rs.bot.ai.TrainingMethods.Method m : com.rs.bot.ai.TrainingMethods.getAll()) {
            if (m.kind == null || !allowedKinds.contains(m.kind)) continue;
            if (m.location == null) continue;
            try {
                if (!m.isApplicable(bot)) continue;
            } catch (Throwable ignored) { continue; }
            applicable.add(m);
        }
        if (applicable.isEmpty()) return null;
        return applicable.get(Utils.random(applicable.size()));
    }

    /**
     * Map an AmbientArchetype to the set of TrainingMethods.Kind values
     * that "fit" the role. Skiller does WC/Mining/Fishing/Cooking/etc
     * (no combat). Combatant does COMBAT (and not gathering - they may
     * have skill levels but the role's flavor is fighting).
     */
    private static java.util.Set<com.rs.bot.ai.TrainingMethods.Kind> allowedKindsFor(AmbientArchetype arch) {
        java.util.EnumSet<com.rs.bot.ai.TrainingMethods.Kind> set =
            java.util.EnumSet.noneOf(com.rs.bot.ai.TrainingMethods.Kind.class);
        if (arch == null) return set;
        if (arch.isSkiller()) {
            set.add(com.rs.bot.ai.TrainingMethods.Kind.WOODCUTTING);
            set.add(com.rs.bot.ai.TrainingMethods.Kind.MINING);
            set.add(com.rs.bot.ai.TrainingMethods.Kind.FISHING);
            set.add(com.rs.bot.ai.TrainingMethods.Kind.COOKING);
            set.add(com.rs.bot.ai.TrainingMethods.Kind.FIREMAKING);
            set.add(com.rs.bot.ai.TrainingMethods.Kind.CRAFTING);
            set.add(com.rs.bot.ai.TrainingMethods.Kind.SMELTING);
            set.add(com.rs.bot.ai.TrainingMethods.Kind.PRAYER);
        }
        if (arch.isCombatant()) {
            set.add(com.rs.bot.ai.TrainingMethods.Kind.COMBAT);
        }
        return set;
    }

    /**
     * Find a real interaction target near the bot, face it, and play the
     * archetype animation. When we have a currentMethod, scan for THAT
     * method's specific resource (so a willow-method skiller doesn't
     * randomly chop a regular tree if both are nearby). Falls back to
     * generic scanning + name-match for socialites/minigamers.
     */
    private boolean faceAndAnimateTarget(AIPlayer bot) {
        try {
            int radius = 5; // close-range - bot should already be at the target
            // === TrainingMethods route (shared with Legends) ===
            if (currentMethod != null) {
                com.rs.bot.ai.TrainingMethods.Method m = currentMethod;
                if (m.kind == com.rs.bot.ai.TrainingMethods.Kind.WOODCUTTING) {
                    com.rs.bot.ai.EnvironmentScanner.TreeMatch tm =
                        com.rs.bot.ai.EnvironmentScanner.findNearestTree(bot, radius, m.treeDef);
                    if (tm != null && tm.object != null) {
                        try { bot.faceObject(tm.object); } catch (Throwable ignored) {}
                        playAnim(bot);
                        return true;
                    }
                } else if (m.kind == com.rs.bot.ai.TrainingMethods.Kind.MINING) {
                    com.rs.bot.ai.EnvironmentScanner.RockMatch rm =
                        com.rs.bot.ai.EnvironmentScanner.findNearestRock(bot, radius, m.rockDef);
                    if (rm != null && rm.object != null) {
                        try { bot.faceObject(rm.object); } catch (Throwable ignored) {}
                        playAnim(bot);
                        return true;
                    }
                } else if (m.kind == com.rs.bot.ai.TrainingMethods.Kind.FISHING) {
                    com.rs.bot.ai.EnvironmentScanner.FishMatch fm =
                        com.rs.bot.ai.EnvironmentScanner.findNearestFishingSpot(bot, radius, m.fishDef);
                    if (fm != null && fm.npc != null) {
                        try { bot.setNextFaceEntity(fm.npc); } catch (Throwable ignored) {}
                        playAnim(bot);
                        return true;
                    }
                } else if (m.kind == com.rs.bot.ai.TrainingMethods.Kind.COMBAT
                        || m.kind == com.rs.bot.ai.TrainingMethods.Kind.THIEVING) {
                    if (m.npcIds != null && m.npcIds.length > 0) {
                        com.rs.game.npc.NPC n =
                            com.rs.bot.ai.EnvironmentScanner.findNearestNPC(bot, radius, m.npcIds);
                        if (n != null) {
                            try { bot.setNextFaceEntity(n); } catch (Throwable ignored) {}
                            playAnim(bot);
                            return true;
                        }
                    }
                } else if (m.kind == com.rs.bot.ai.TrainingMethods.Kind.COOKING
                        || m.kind == com.rs.bot.ai.TrainingMethods.Kind.SMELTING
                        || m.kind == com.rs.bot.ai.TrainingMethods.Kind.PRAYER
                        || m.kind == com.rs.bot.ai.TrainingMethods.Kind.FIREMAKING
                        || m.kind == com.rs.bot.ai.TrainingMethods.Kind.CRAFTING) {
                    // No specific def to match - just animate in place for now.
                    // (cooking range / furnace / altar object scan can be added
                    // when method.requiredObjects is wired through.)
                    playAnim(bot);
                    return true;
                }
                // currentMethod's resource not findable here - log + fall through
                // to generic scanner. Fires if the method's location is right
                // but the world object/NPC is missing - exactly what BotAuditor
                // catches for Legends. Same data, same diagnostic.
                debug(bot, "method " + m.description + " has no nearby " + m.kind
                    + " resource - falling back to scanner");
            }

            // === Fallback generic-scanner route (no method, or method missed) ===
            if (archetype.isSocialite()) {
                com.rs.game.WorldObject o =
                    com.rs.bot.ai.EnvironmentScanner.findNearestObjectByName(
                        bot, radius, "bank booth", "grand exchange", "ge clerk");
                if (o != null) {
                    try { bot.faceObject(o); } catch (Throwable ignored) {}
                    playAnim(bot);
                    return true;
                }
            }
            if (archetype.isMinigamer()) {
                com.rs.game.npc.NPC n =
                    com.rs.bot.ai.EnvironmentScanner.findNearestNPC(bot, radius);
                if (n != null) {
                    try { bot.setNextFaceEntity(n); } catch (Throwable ignored) {}
                    playAnim(bot);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Per-citizen debug line into the audit log. Shared with Legend logging
     * so both tiers' activity flows into one tail-able stream.
     */
    private void debug(AIPlayer bot, String msg) {
        try {
            com.rs.bot.AuditLog.log("[Citizen] " + bot.getDisplayName()
                + " (" + archetype + ") " + msg);
        } catch (Throwable ignored) {}
    }

    public com.rs.bot.ai.TrainingMethods.Method getCurrentMethod() { return currentMethod; }

    private void playAnim(AIPlayer bot) {
        int anim = archetype.randomInteractAnimation();
        if (anim > 0) {
            try { bot.setNextAnimation(new Animation(anim)); }
            catch (Throwable ignored) {}
        }
    }

    /** Pick a tile cardinally adjacent to a world object (for skiller targets). */
    private static WorldTile tileNextTo(com.rs.game.WorldObject o) {
        if (o == null) return null;
        // Prefer the south tile (most objects are interactable from the south);
        // fall back to other cardinals if the world is weird.
        int[][] offsets = {{0,-1},{0,1},{-1,0},{1,0}};
        int idx = Utils.random(offsets.length);
        int[] off = offsets[idx];
        return new WorldTile(o.getX() + off[0], o.getY() + off[1], o.getPlane());
    }

    /** Pick a tile cardinally adjacent to an NPC (fishing spot, combat target). */
    private static WorldTile tileNextTo(com.rs.game.npc.NPC n) {
        if (n == null) return null;
        int[][] offsets = {{0,-1},{0,1},{-1,0},{1,0}};
        int idx = Utils.random(offsets.length);
        int[] off = offsets[idx];
        return new WorldTile(n.getX() + off[0], n.getY() + off[1], n.getPlane());
    }

    private void tickPanicking(AIPlayer bot) {
        Player nearest = nearestPlayer(bot, PLAYER_AWARENESS_TILES * 2);
        if (nearest == null) return;
        int dx = bot.getX() - nearest.getX();
        int dy = bot.getY() - nearest.getY();
        int sx = dx == 0 ? (Utils.random(2) == 0 ? 1 : -1) : Integer.signum(dx);
        int sy = dy == 0 ? (Utils.random(2) == 0 ? 1 : -1) : Integer.signum(dy);
        int dist = 5 + Utils.random(4);
        WorldTile flee = new WorldTile(bot.getX() + sx * dist, bot.getY() + sy * dist, bot.getPlane());
        bot.addWalkSteps(flee.getX(), flee.getY(), 10, true);
        if (Math.random() < 0.15) {
            try { bot.setNextForceTalk(new ForceTalk("...!")); }
            catch (Throwable ignored) {}
        }
    }

    // === Helpers ===

    private WorldTile pickWanderTarget() {
        int dx = (int) gaussianRange(-homeRadius, homeRadius, homeRadius / 2.0);
        int dy = (int) gaussianRange(-homeRadius, homeRadius, homeRadius / 2.0);
        if (Math.random() < 0.3) dx += Utils.random(-1, 2);
        if (Math.random() < 0.3) dy += Utils.random(-1, 2);
        return new WorldTile(homeAnchor.getX() + dx, homeAnchor.getY() + dy, homeAnchor.getPlane());
    }

    private void stepRandom(AIPlayer bot) {
        if (bot.hasWalkSteps()) return;
        int sx = Utils.random(3) - 1;
        int sy = Utils.random(3) - 1;
        if (sx == 0 && sy == 0) sx = 1;
        bot.addWalkSteps(bot.getX() + sx, bot.getY() + sy, 1, true);
    }

    private boolean realPlayerNearby(AIPlayer bot) {
        for (Player p : World.getPlayers()) {
            if (p == null) continue;
            if (p instanceof AIPlayer) continue; // ignore other bots
            if (p.getPlane() != bot.getPlane()) continue;
            if (bot.withinDistance(p, PLAYER_AWARENESS_TILES)) return true;
        }
        return false;
    }

    private Player nearestPlayer(AIPlayer bot, int radius) {
        Player best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Player p : World.getPlayers()) {
            if (p == null) continue;
            if (p instanceof AIPlayer) continue;
            if (p.getPlane() != bot.getPlane()) continue;
            if (!bot.withinDistance(p, radius)) continue;
            int d = Math.abs(p.getX() - bot.getX()) + Math.abs(p.getY() - bot.getY());
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    /** Sum-of-three-uniforms approximation - cheap, looks Gaussian enough. */
    private static double gaussianRange(double min, double max, double stdDev) {
        if (max <= min) return min;
        double mid = (min + max) / 2.0;
        double v = mid + (Math.random() + Math.random() + Math.random() - 1.5) / 1.5 * stdDev;
        if (v < min) v = min;
        if (v > max) v = max;
        return v;
    }

    public State getState() { return state; }
    public AmbientArchetype getArchetype() { return archetype; }
    public WorldTile getHomeAnchor() { return homeAnchor; }
}
