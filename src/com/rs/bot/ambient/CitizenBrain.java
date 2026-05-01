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
     * Pick a real interaction destination based on archetype. Returns null if
     * nothing's available in the home radius - caller falls back to random.
     */
    private WorldTile findInteractionDestination(AIPlayer bot) {
        try {
            int radius = Math.max(homeRadius, 12);
            switch (archetype) {
                case SKILLER_EFFICIENT:
                case SKILLER_CASUAL:
                case SKILLER_NOOB: {
                    // Pick one of the gathering targets at random
                    int pick = Utils.random(3);
                    if (pick == 0) {
                        com.rs.bot.ai.EnvironmentScanner.TreeMatch tm =
                            com.rs.bot.ai.EnvironmentScanner.findNearestTree(homeAnchor, radius);
                        if (tm != null && tm.object != null) return tileNextTo(tm.object);
                    } else if (pick == 1) {
                        com.rs.bot.ai.EnvironmentScanner.RockMatch rm =
                            com.rs.bot.ai.EnvironmentScanner.findNearestRock(homeAnchor, radius);
                        if (rm != null && rm.object != null) return tileNextTo(rm.object);
                    } else {
                        com.rs.bot.ai.EnvironmentScanner.FishMatch fm =
                            com.rs.bot.ai.EnvironmentScanner.findNearestFishingSpot(homeAnchor, radius);
                        if (fm != null && fm.npc != null) return tileNextTo(fm.npc);
                    }
                    break;
                }
                case COMBATANT_PURE:
                case COMBATANT_TANK:
                case COMBATANT_HYBRID:
                case MINIGAMER_RUSHER:
                case MINIGAMER_DEFENDER: {
                    // Walk toward any nearby non-aggressive NPC. We don't
                    // actually engage combat - just stand near to look like
                    // we're contemplating an attack.
                    com.rs.game.npc.NPC n =
                        com.rs.bot.ai.EnvironmentScanner.findNearestNPC(homeAnchor, radius);
                    if (n != null) return tileNextTo(n);
                    break;
                }
                case SOCIALITE_BANKSTAND:
                case SOCIALITE_GE_TRADER:
                case SOCIALITE_GAMBLER: {
                    // Bank booth / GE clerk. Generic name match against
                    // common words.
                    com.rs.game.WorldObject o =
                        com.rs.bot.ai.EnvironmentScanner.findNearestObjectByName(
                            homeAnchor, radius, "bank booth", "grand exchange", "ge clerk");
                    if (o != null) return tileNextTo(o);
                    break;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Find a real interaction target near the bot, face it, and play the
     * archetype animation. Returns true if we found one, false otherwise.
     */
    private boolean faceAndAnimateTarget(AIPlayer bot) {
        try {
            int radius = 4; // close-range scan - bot should already be at the target
            switch (archetype) {
                case SKILLER_EFFICIENT:
                case SKILLER_CASUAL:
                case SKILLER_NOOB: {
                    com.rs.bot.ai.EnvironmentScanner.TreeMatch tm =
                        com.rs.bot.ai.EnvironmentScanner.findNearestTree(bot, radius);
                    if (tm != null && tm.object != null) {
                        try { bot.faceObject(tm.object); } catch (Throwable ignored) {}
                        playAnim(bot);
                        return true;
                    }
                    com.rs.bot.ai.EnvironmentScanner.RockMatch rm =
                        com.rs.bot.ai.EnvironmentScanner.findNearestRock(bot, radius);
                    if (rm != null && rm.object != null) {
                        try { bot.faceObject(rm.object); } catch (Throwable ignored) {}
                        playAnim(bot);
                        return true;
                    }
                    com.rs.bot.ai.EnvironmentScanner.FishMatch fm =
                        com.rs.bot.ai.EnvironmentScanner.findNearestFishingSpot(bot, radius);
                    if (fm != null && fm.npc != null) {
                        try { bot.setNextFaceEntity(fm.npc); } catch (Throwable ignored) {}
                        playAnim(bot);
                        return true;
                    }
                    break;
                }
                case COMBATANT_PURE:
                case COMBATANT_TANK:
                case COMBATANT_HYBRID:
                case MINIGAMER_RUSHER:
                case MINIGAMER_DEFENDER: {
                    com.rs.game.npc.NPC n =
                        com.rs.bot.ai.EnvironmentScanner.findNearestNPC(bot, radius);
                    if (n != null) {
                        try { bot.setNextFaceEntity(n); } catch (Throwable ignored) {}
                        playAnim(bot);
                        return true;
                    }
                    break;
                }
                case SOCIALITE_BANKSTAND:
                case SOCIALITE_GE_TRADER:
                case SOCIALITE_GAMBLER: {
                    com.rs.game.WorldObject o =
                        com.rs.bot.ai.EnvironmentScanner.findNearestObjectByName(
                            bot, radius, "bank booth", "grand exchange", "ge clerk");
                    if (o != null) {
                        try { bot.faceObject(o); } catch (Throwable ignored) {}
                        playAnim(bot);
                        return true;
                    }
                    break;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

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
