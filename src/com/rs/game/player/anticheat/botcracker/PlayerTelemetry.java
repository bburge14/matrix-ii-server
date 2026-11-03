package com.rs.game.player.anticheat.botcracker;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * All per-account state BotCracker keeps in memory. One instance per online
 * player, created on login, discarded (after a final flush) on logout.
 * <p>
 * Thread model: packets arrive on Netty threads, skill actions and XP on the
 * world thread. Every mutator is {@code synchronized} on the instance; the
 * per-player lock is uncontended in practice so this is cheap.
 */
public final class PlayerTelemetry {

	/** Fixed-size circular buffer of longs. */
	public static final class LongRing {
		private final long[] data;
		private int head;
		private int size;

		public LongRing(int capacity) {
			data = new long[capacity];
		}

		public void add(long v) {
			data[head] = v;
			head = (head + 1) % data.length;
			if (size < data.length)
				size++;
		}

		public int size() {
			return size;
		}

		/** Oldest -> newest snapshot. */
		public long[] toArray() {
			long[] out = new long[size];
			int start = (head - size + data.length) % data.length;
			for (int i = 0; i < size; i++)
				out[i] = data[(start + i) % data.length];
			return out;
		}

		public void clear() {
			head = 0;
			size = 0;
		}
	}

	/** One recorded mouse click in screen space. */
	public static final class ClickSample {
		public final int x, y, button;
		public final long time;

		ClickSample(int x, int y, int button, long time) {
			this.x = x;
			this.y = y;
			this.button = button;
			this.time = time;
		}
	}

	/** Throughput accumulated over one {@link BotCrackerConfig#FATIGUE_BUCKET_MS} window. */
	public static final class ThroughputBucket {
		public final long startTime;
		public int actions;
		public int productiveActions; // completed SKILL_ACTION cycles only
		public double xp;
		public long activeMs; // ms in which at least one action happened (gap < BREAK_GAP)

		ThroughputBucket(long startTime) {
			this.startTime = startTime;
		}
	}

	private final String username;
	private final long loginTime;

	// ---- IAT -----------------------------------------------------------
	private final Map<ActionKind, LongRing> iat = new EnumMap<ActionKind, LongRing>(ActionKind.class);
	private final Map<ActionKind, LongRing> tickPhase = new EnumMap<ActionKind, LongRing>(ActionKind.class);
	private final Map<ActionKind, Long> lastActionTime = new EnumMap<ActionKind, Long>(ActionKind.class);
	private final Map<ActionKind, Long> totalActions = new EnumMap<ActionKind, Long>(ActionKind.class);

	// ---- Fatigue -------------------------------------------------------
	private final List<ThroughputBucket> buckets = new ArrayList<ThroughputBucket>();
	private long lastAnyActionTime;
	private long continuousSince; // start of the current no-break run
	private int breaksTaken;
	private double sessionXp;

	// ---- Spatial -------------------------------------------------------
	private final List<ClickSample> clicks = new ArrayList<ClickSample>();

	// ---- Scoring -------------------------------------------------------
	private final List<Finding> findings = new ArrayList<Finding>();
	private double score;
	private long lastEvaluationTick;

	public PlayerTelemetry(String username, long now) {
		this.username = username;
		this.loginTime = now;
		this.lastAnyActionTime = now;
		this.continuousSince = now;
	}

	// ------------------------------------------------------------------
	// Recording
	// ------------------------------------------------------------------

	/**
	 * Record an action. {@code now} is server ms, {@code tickStart} is the ms
	 * at which the current world cycle began (so phase = now - tickStart).
	 */
	public synchronized void recordAction(ActionKind kind, long now, long tickStart) {
		Long last = lastActionTime.get(kind);
		if (last != null) {
			long delta = now - last;
			if (delta >= 0 && delta < 60_000L) // ignore pauses; they are handled by fatigue tracker
				ring(iat, kind).add(delta);
		}
		lastActionTime.put(kind, now);
		Long total = totalActions.get(kind);
		totalActions.put(kind, total == null ? 1L : total + 1L);

		long phase = tickStart > 0 ? ((now - tickStart) % BotCrackerConfig.TICK_MS + BotCrackerConfig.TICK_MS)
				% BotCrackerConfig.TICK_MS : now % BotCrackerConfig.TICK_MS;
		ring(tickPhase, kind).add(phase);

		touchFatigue(now, kind == ActionKind.SKILL_ACTION, 0);
	}

	public synchronized void recordXp(double xp, long now) {
		sessionXp += xp;
		touchFatigue(now, false, xp);
	}

	public synchronized void recordClick(int x, int y, int button, long now) {
		clicks.add(new ClickSample(x, y, button, now));
		int max = BotCrackerConfig.CLICK_WINDOW;
		if (clicks.size() > max)
			clicks.subList(0, clicks.size() - max).clear();
	}

	private void touchFatigue(long now, boolean productive, double xp) {
		long gap = now - lastAnyActionTime;
		if (gap > BotCrackerConfig.BREAK_GAP_MS) {
			breaksTaken++;
			continuousSince = now;
			gap = 0;
		}
		lastAnyActionTime = now;

		ThroughputBucket b = buckets.isEmpty() ? null : buckets.get(buckets.size() - 1);
		if (b == null || now - b.startTime >= BotCrackerConfig.FATIGUE_BUCKET_MS) {
			long start = b == null ? now : b.startTime + BotCrackerConfig.FATIGUE_BUCKET_MS;
			// fast-forward across idle windows so buckets stay contiguous in time
			while (now - start >= BotCrackerConfig.FATIGUE_BUCKET_MS)
				start += BotCrackerConfig.FATIGUE_BUCKET_MS;
			b = new ThroughputBucket(start);
			buckets.add(b);
			if (buckets.size() > BotCrackerConfig.FATIGUE_BUCKETS)
				buckets.remove(0);
		}
		if (xp == 0) {
			b.actions++;
			if (productive)
				b.productiveActions++;
			b.activeMs += gap;
		} else
			b.xp += xp;
	}

	private static LongRing ring(Map<ActionKind, LongRing> m, ActionKind k) {
		LongRing r = m.get(k);
		if (r == null) {
			r = new LongRing(BotCrackerConfig.IAT_WINDOW);
			m.put(k, r);
		}
		return r;
	}

	// ------------------------------------------------------------------
	// Snapshots (copied so detectors can run lock-free)
	// ------------------------------------------------------------------

	public synchronized long[] iatSnapshot(ActionKind kind) {
		LongRing r = iat.get(kind);
		return r == null ? new long[0] : r.toArray();
	}

	public synchronized long[] phaseSnapshot(ActionKind kind) {
		LongRing r = tickPhase.get(kind);
		return r == null ? new long[0] : r.toArray();
	}

	public synchronized List<ThroughputBucket> bucketSnapshot() {
		List<ThroughputBucket> copy = new ArrayList<ThroughputBucket>(buckets.size());
		for (ThroughputBucket b : buckets) {
			ThroughputBucket c = new ThroughputBucket(b.startTime);
			c.actions = b.actions;
			c.productiveActions = b.productiveActions;
			c.xp = b.xp;
			c.activeMs = b.activeMs;
			copy.add(c);
		}
		return copy;
	}

	public synchronized List<ClickSample> clickSnapshot() {
		return new ArrayList<ClickSample>(clicks);
	}

	public synchronized long getTotalActions(ActionKind kind) {
		Long t = totalActions.get(kind);
		return t == null ? 0 : t;
	}

	public synchronized long getContinuousSpanMs(long now) {
		return now - continuousSince;
	}

	public synchronized long getLastAnyActionTime() {
		return lastAnyActionTime;
	}

	public synchronized int getBreaksTaken() {
		return breaksTaken;
	}

	public synchronized double getSessionXp() {
		return sessionXp;
	}

	// ------------------------------------------------------------------
	// Scoring state
	// ------------------------------------------------------------------

	public synchronized void setEvaluation(double score, List<Finding> latest, long tick) {
		this.score = score;
		this.findings.clear();
		this.findings.addAll(latest);
		this.lastEvaluationTick = tick;
	}

	public synchronized double getScore() {
		return score;
	}

	public synchronized List<Finding> getFindings() {
		return new ArrayList<Finding>(findings);
	}

	public synchronized long getLastEvaluationTick() {
		return lastEvaluationTick;
	}

	public String getUsername() {
		return username;
	}

	public long getLoginTime() {
		return loginTime;
	}
}
