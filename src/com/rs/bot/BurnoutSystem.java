package com.rs.bot;

import com.rs.bot.ai.Goal;
import com.rs.bot.ai.GoalStack;
import com.rs.bot.ai.GoalType;
import com.rs.utils.Utils;

import java.io.Serializable;

/**
 * Simulates the realistic "I've been training attack for two hours, I need a
 * break" pattern. Bots stick to long goals via GoalStack commitment timers,
 * but every now and then real players abandon their grind to go pking, kill
 * a boss, run a minigame, or just lap around banks. This system fires those
 * vacations.
 *
 * Mechanic:
 *   - A "burnout meter" rises as the bot stays on the same goal.
 *   - At low meter the bot is locked in (commitment timer also enforces this).
 *   - Past the commitment window, every brain tick rolls a small chance to
 *     fire burnout. Probability scales with the meter.
 *   - When burnout fires, the current goal is force-abandoned and a "fun"
 *     goal is injected. The fun goal has its own commitment timer so the
 *     bot rides it for a while before returning to its grind.
 *   - After burnout fires the meter resets, so it won't fire again
 *     immediately.
 *
 * Personality + emotional state can dial how easily a bot snaps. A high-
 * efficiency / focused bot needs more time to burn out; a chaotic / restless
 * one snaps fast. Hooks are kept minimal here so callers can layer policy.
 */
public final class BurnoutSystem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Meter goes 0.0 (fresh) to 1.0 (snapped). */
    private double meter;
    /** Wallclock time we last checked - used to advance meter even across save/restore. */
    private long lastTickWall;
    /** Wallclock time the last burnout fired. Prevents back-to-back vacations. */
    private long lastBurnoutFiredAt;

    public BurnoutSystem() {
        this.meter = 0.0;
        this.lastTickWall = System.currentTimeMillis();
        this.lastBurnoutFiredAt = 0L;
    }

    /**
     * Evaluate the meter for one brain tick and return true if burnout should
     * fire. Returns false if commitment is still active or if a vacation
     * already fired recently.
     */
    public boolean tick(AIPlayer bot, GoalStack stack) {
        long now = System.currentTimeMillis();
        long delta = Math.max(0, now - lastTickWall);
        lastTickWall = now;

        Goal current = stack.getCurrentGoal();
        if (current == null) {
            // No goal => no grinding => decay to fresh.
            meter = Math.max(0.0, meter - delta / 600_000.0);
            return false;
        }

        // Build burnout based on time on this goal. The meter approaches 1.0
        // asymptotically over ~3 hours of continuous work, so a normal
        // commitment window passes mostly relaxed.
        long timeOnGoal = stack.getTimeOnCurrentGoal();
        double targetMeter = 1.0 - Math.exp(-timeOnGoal / (90.0 * 60_000.0));
        // Smoothly chase the target so spikes don't fire instantly.
        meter = meter * 0.97 + targetMeter * 0.03;

        // Don't even consider firing inside the commitment window. The bot
        // is committed - that's the point.
        if (!stack.canSwitchGoal()) return false;

        // Don't fire two vacations in a row. Min 30 wallclock minutes.
        if (now - lastBurnoutFiredAt < 30 * 60_000L) return false;

        // Probability per brain tick scales with meter. At meter=1.0 about
        // 1 in 200 ticks (~2 minutes if brain ticks at 600ms). Plenty of
        // time for a real switch decision to feel natural rather than
        // instant.
        int chancePerTen_thousand = (int) (meter * 50.0);
        if (chancePerTen_thousand <= 0) return false;
        if (Utils.random(10000) >= chancePerTen_thousand) return false;

        lastBurnoutFiredAt = now;
        meter = 0.2; // partial reset - vacation feels good but not full clean
        return true;
    }

    /** For HUD / botinfo display. */
    public double getMeter() { return meter; }

    public long getLastBurnoutAt() { return lastBurnoutFiredAt; }

    /**
     * Pool of "fun" goal types the bot may switch to during a burnout. Mix
     * of short money-making jaunts, combat trips, exploration, and bossing.
     * We deliberately avoid skill 99 goals here - those ARE what the bot
     * was burning out from. Pick something that feels like a break.
     */
    public static GoalType pickVacationGoal() {
        GoalType[] pool = {
            // Money-making short trips (fights through items in inventory)
            GoalType.BUILD_1M_BANK,
            // Equipment hunts feel like a "treat yourself" break
            GoalType.GET_RUNE_ARMOR,
            // Combat trip
            GoalType.GET_FIRE_CAPE,
            // Bossing
            GoalType.GET_BANDOS_ARMOR,
            // Generic combat refresher
            GoalType.MAX_COMBAT_STATS
        };
        return pool[Utils.random(pool.length)];
    }
}
