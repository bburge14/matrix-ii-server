package com.rs.bot.ambient;

import com.rs.game.Animation;
import com.rs.game.ForceTalk;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.utils.Utils;

/**
 * Lightweight "Citizen" bot - extends NPC instead of Player so we can run
 * 400+ of them at minimal memory + tick cost. The complex AIPlayer-backed
 * "Legend" bots stay for ~50 marquee characters; everything else uses this.
 *
 * Design tenets:
 *   - NO inventory / bank / skill state. Citizens don't earn XP or gather
 *     real loot - they play animations and walk paths to look real.
 *   - Finite state machine: IDLE / TRAVERSING / INTERACTING / PANICKING.
 *     Each tick advances one state and may transition.
 *   - Humanization: Gaussian wait times (not uniform), 1-tile path wobble,
 *     occasional misclicks, rare 20-second AFK pauses. Real-player
 *     detection within 6 tiles bumps to PANICKING (look natural under
 *     observation).
 *   - Archetype-driven content: a SKILLER tries to walk to "skill spots"
 *     and play gathering animations; a COMBATANT walks a combat zone and
 *     fakes a fight; a SOCIALITE bankstands and talks. Archetypes are just
 *     scripts mapped to FSM transitions - the FSM logic stays generic.
 *
 * The class delegates the heavy lifting to NPC.processNPC()'s superclass
 * machinery for: walking steps, animation rendering, chat bubbles, face-
 * entity packets. We just decide WHAT happens each state-tick.
 */
public class AmbientBot extends NPC {

    private static final long serialVersionUID = 1L;

    /** Real-player proximity that triggers PANICKING. 6 tiles ~ visible
     *  on the player's screen edge but not too close. */
    private static final int PLAYER_AWARENESS_TILES = 6;

    /** Probability per tick of an "AFK pause" - bot stops everything for
     *  10-30 seconds. Real players AFK constantly; this mimics that. */
    private static final double AFK_PROBABILITY = 0.0008;

    /** Probability per tick of a "misclick" - walks to a wrong nearby
     *  tile then continues. */
    private static final double MISCLICK_PROBABILITY = 0.012;

    /** Chatter probability per IDLE tick. Citizens with a chatter pool
     *  randomly speak overhead text. */
    private static final double CHATTER_PROBABILITY = 0.02;

    public enum State { IDLE, TRAVERSING, INTERACTING, PANICKING }

    private final transient AmbientArchetype archetype;
    /** Center of the bot's "home" zone - it wanders inside this circle. */
    private final transient WorldTile homeAnchor;
    /** Wander radius in tiles from homeAnchor. */
    private final transient int homeRadius;

    private transient State state = State.IDLE;
    private transient int stateTicksRemaining = 0;
    /** Resume time for AFK pause (ms epoch). 0 = not afking. */
    private transient long afkUntilMs = 0;
    /** Last forced "panic" trigger so we don't oscillate. */
    private transient long panicCooldownTicks = 0;

    public AmbientBot(int npcId, WorldTile spawn, AmbientArchetype archetype, int homeRadius) {
        // mapAreaNameHash=-1 disables the area-bound respawn; canBeAttackedFromOOA=false;
        // spawned=true so it renders properly.
        super(npcId, spawn, -1, false, true);
        this.archetype = archetype;
        this.homeAnchor = new WorldTile(spawn);
        this.homeRadius = Math.max(2, homeRadius);
        // Disable the standard NPC random walk - we drive movement via FSM.
        setRandomWalk(0);
        scheduleNextStateChange(State.IDLE);
    }

    @Override
    public void processNPC() {
        if (isDead() || hasFinished()) return;

        // AFK pause - skip everything until expiry.
        if (afkUntilMs > 0) {
            if (System.currentTimeMillis() < afkUntilMs) return;
            afkUntilMs = 0;
        }

        // Real-player proximity check - bumps to PANICKING. Only check
        // every few ticks for cost (proximity scan walks World.players).
        if (panicCooldownTicks > 0) panicCooldownTicks--;
        if (panicCooldownTicks <= 0 && state != State.PANICKING) {
            if (realPlayerNearby()) {
                transitionTo(State.PANICKING);
                panicCooldownTicks = 50; // ~30s before we can repanic
            }
        }

        // Chance per-tick to drop into AFK or commit a misclick. Skipping
        // both during PANICKING - panicked bots aren't AFKing.
        if (state != State.PANICKING) {
            if (Math.random() < AFK_PROBABILITY) {
                afkUntilMs = System.currentTimeMillis() + (long) gaussianRange(10000, 30000, 5000);
                return;
            }
            if (Math.random() < MISCLICK_PROBABILITY) {
                // Walk one tile in a random direction. The path wobble is
                // baked into pickWanderTarget already, but a misclick is a
                // visible "oops" - they take one wrong step.
                stepRandom();
            }
        }

        // FSM tick.
        if (--stateTicksRemaining <= 0) {
            advanceState();
        }
        switch (state) {
            case IDLE:        tickIdle();        break;
            case TRAVERSING:  tickTraversing();  break;
            case INTERACTING: tickInteracting(); break;
            case PANICKING:   tickPanicking();   break;
        }

        // Let NPC.processNPC handle walk-step consumption + face entity etc.
        // Actually we DON'T call super.processNPC() because that would
        // trigger the standard random-walk path. We've already moved this
        // bot's walk steps; the engine will consume them on the next tick
        // via the world processor.
    }

    // === FSM transitions ===

    private void advanceState() {
        State next;
        switch (state) {
            case IDLE:
                // 60% chance to traverse, 30% to interact, 10% to keep idling
                int r = Utils.random(100);
                if (r < 60) next = State.TRAVERSING;
                else if (r < 90) next = State.INTERACTING;
                else next = State.IDLE;
                break;
            case TRAVERSING:
                // Arrived (or close enough) - prefer interact over idle.
                next = Utils.random(100) < 70 ? State.INTERACTING : State.IDLE;
                break;
            case INTERACTING:
                next = State.IDLE;
                break;
            case PANICKING:
                // After panic cooldown, settle back to idle.
                next = State.IDLE;
                break;
            default: next = State.IDLE;
        }
        transitionTo(next);
    }

    private void transitionTo(State next) {
        state = next;
        scheduleNextStateChange(next);
    }

    /** Schedule how long this state lasts before advancing. Gaussian distribution
     *  matches real-player rhythms better than uniform random. */
    private void scheduleNextStateChange(State s) {
        switch (s) {
            case IDLE:        stateTicksRemaining = (int) gaussianRange(3, 12, 3); break;
            case TRAVERSING:  stateTicksRemaining = (int) gaussianRange(8, 20, 5); break;
            case INTERACTING: stateTicksRemaining = (int) gaussianRange(15, 40, 8); break;
            case PANICKING:   stateTicksRemaining = (int) gaussianRange(5, 15, 3); break;
        }
    }

    // === Per-state behavior ===

    private void tickIdle() {
        // Occasionally chatter or play an idle emote. Most ticks we just stand.
        if (Math.random() < CHATTER_PROBABILITY) {
            String line = archetype.randomChatter();
            if (line != null) setNextForceTalk(new ForceTalk(line));
        }
    }

    private void tickTraversing() {
        if (hasWalkSteps()) return; // already walking
        WorldTile target = pickWanderTarget();
        if (target == null) return;
        addWalkSteps(target.getX(), target.getY(), 8, true);
    }

    private void tickInteracting() {
        // Face a random direction or play an archetype-specific animation.
        // Without a real target object we just play the anim.
        int anim = archetype.randomInteractAnimation();
        if (anim > 0) setNextAnimation(new Animation(anim));
    }

    private void tickPanicking() {
        // Walk away from the nearest player. Pick a tile 5-8 tiles in the
        // opposite direction.
        Player nearest = nearestPlayer(PLAYER_AWARENESS_TILES * 2);
        if (nearest == null) return;
        int dx = getX() - nearest.getX();
        int dy = getY() - nearest.getY();
        // Normalize and scale.
        int sx = dx == 0 ? Utils.random(2) == 0 ? 1 : -1 : Integer.signum(dx);
        int sy = dy == 0 ? Utils.random(2) == 0 ? 1 : -1 : Integer.signum(dy);
        int dist = 5 + Utils.random(4);
        WorldTile flee = new WorldTile(getX() + sx * dist, getY() + sy * dist, getPlane());
        addWalkSteps(flee.getX(), flee.getY(), 10, true);
        if (Math.random() < 0.15) setNextForceTalk(new ForceTalk("...!"));
    }

    // === Helpers ===

    /** Pick a target tile within the home radius, with 1-tile path wobble
     *  to break up grid alignment. */
    private WorldTile pickWanderTarget() {
        int dx = (int) gaussianRange(-homeRadius, homeRadius, homeRadius / 2.0);
        int dy = (int) gaussianRange(-homeRadius, homeRadius, homeRadius / 2.0);
        // Path wobble: small additional jitter so two bots from the same
        // anchor don't pick identical tiles even with same hash seeds.
        if (Math.random() < 0.3) dx += Utils.random(-1, 2);
        if (Math.random() < 0.3) dy += Utils.random(-1, 2);
        return new WorldTile(homeAnchor.getX() + dx, homeAnchor.getY() + dy, homeAnchor.getPlane());
    }

    private void stepRandom() {
        if (hasWalkSteps()) return;
        int sx = Utils.random(3) - 1; // -1, 0, 1
        int sy = Utils.random(3) - 1;
        if (sx == 0 && sy == 0) sx = 1;
        addWalkSteps(getX() + sx, getY() + sy, 1, true);
    }

    private boolean realPlayerNearby() {
        for (Player p : World.getPlayers()) {
            if (p == null) continue;
            // Skip AIPlayers - they're our own bots, no need to flinch.
            if (isOurBot(p)) continue;
            if (p.getPlane() != getPlane()) continue;
            if (withinDistance(p, PLAYER_AWARENESS_TILES)) return true;
        }
        return false;
    }

    private Player nearestPlayer(int radius) {
        Player best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Player p : World.getPlayers()) {
            if (p == null) continue;
            if (isOurBot(p)) continue;
            if (p.getPlane() != getPlane()) continue;
            if (!withinDistance(p, radius)) continue;
            int d = Math.abs(p.getX() - getX()) + Math.abs(p.getY() - getY());
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private static boolean isOurBot(Player p) {
        // Try to detect AIPlayer without a hard dep on the bot package.
        // (Class.getName() avoids classloader issues.)
        return p.getClass().getName().contains("AIPlayer");
    }

    /** Sample a Gaussian-distributed value clamped to [min, max] with the
     *  given std-dev. Falls back to uniform if values are degenerate. */
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
