package com.rs.game.player.anticheat.botcracker;

import java.util.List;

/**
 * Macro-behavioural entropy and fatigue invariance.
 * <p>
 * Throughput (actions, productive actions, XP) is accumulated in fixed
 * 10-minute buckets covering the last six hours. Once a player has been
 * continuously active (no gap longer than {@link BotCrackerConfig#BREAK_GAP_MS})
 * for at least {@link BotCrackerConfig#FATIGUE_MIN_SPAN_MS}, three checks run:
 * <ul>
 * <li><b>Efficiency ceiling</b>: share of buckets in which productive
 * throughput exceeded {@code EFFICIENCY_CEILING} of the theoretical maximum
 * for the dominant action kind. Humans dip below ~90% almost every window
 * (misclicks, chat, inventory management).</li>
 * <li><b>Invariance</b>: coefficient of variation of bucket throughput. A
 * human's rate drifts as they tire, read chat, get distracted. A state
 * machine with "fatigue" baked in still produces a suspiciously smooth curve
 * because the variance is scripted rather than emergent.</li>
 * <li><b>No drop-off</b>: throughput in the last quarter of the continuous
 * span versus the first quarter. Biological actors slow down; tools do not.</li>
 * </ul>
 * Each check is reported separately so moderators can see which one fired.
 */
public final class FatigueInvarianceDetector implements Detector {

	@Override
	public String name() {
		return "fatigue";
	}

	/**
	 * Theoretical maximum actions/hour. Starts from the static ceiling on
	 * {@link ActionKind#SKILL_ACTION} and tightens it to the player's own
	 * fastest observed cadence (5th percentile IAT), so a 4-tick fletching
	 * action is judged against 1500/h rather than 6000/h.
	 */
	static double adaptiveCeiling(PlayerTelemetry t) {
		double ceiling = ActionKind.SKILL_ACTION.getTheoreticalMaxPerHour();
		long[] iat = t.iatSnapshot(ActionKind.SKILL_ACTION);
		if (iat.length >= 50) {
			long[] sorted = iat.clone();
			java.util.Arrays.sort(sorted);
			long p05 = Math.max(BotCrackerConfig.TICK_MS, sorted[sorted.length / 20]);
			ceiling = Math.min(ceiling, 3600000.0 / p05);
		}
		return ceiling;
	}

	@Override
	public void evaluate(PlayerTelemetry t, long now, List<Finding> out) {
		long span = t.getContinuousSpanMs(now);
		if (span < BotCrackerConfig.FATIGUE_MIN_SPAN_MS)
			return;
		// only buckets inside the current continuous run
		List<PlayerTelemetry.ThroughputBucket> all = t.bucketSnapshot();
		long runStart = now - span;
		int first = 0;
		while (first < all.size() && all.get(first).startTime + BotCrackerConfig.FATIGUE_BUCKET_MS < runStart)
			first++;
		List<PlayerTelemetry.ThroughputBucket> run = all.subList(first, all.size());
		// drop the still-filling last bucket unless it's the only one
		if (run.size() > 1)
			run = run.subList(0, run.size() - 1);
		int minBuckets = (int) (BotCrackerConfig.FATIGUE_MIN_SPAN_MS / BotCrackerConfig.FATIGUE_BUCKET_MS) - 1;
		if (run.size() < Math.max(3, minBuckets))
			return;

		double[] perHour = new double[run.size()];
		double maxPerHour = adaptiveCeiling(t);
		for (int i = 0; i < run.size(); i++)
			perHour[i] = run.get(i).productiveActions * (3600000.0 / BotCrackerConfig.FATIGUE_BUCKET_MS);

		// --- efficiency ceiling --------------------------------------------
		int above = 0;
		for (double p : perHour)
			if (p / maxPerHour >= BotCrackerConfig.EFFICIENCY_CEILING)
				above++;
		double aboveShare = (double) above / perHour.length;
		double meanEff = Stats.mean(perHour) / maxPerHour;
		if (aboveShare >= BotCrackerConfig.EFFICIENCY_BUCKET_SHARE * 0.6) {
			double conf = Stats.ramp(aboveShare, BotCrackerConfig.EFFICIENCY_BUCKET_SHARE * 0.6, BotCrackerConfig.EFFICIENCY_BUCKET_SHARE);
			out.add(new Finding(Finding.Source.EFFICIENCY_CEILING, conf, perHour.length, String.format(
					"%.0f%% of %d windows >= %.0f%% of theoretical max (mean eff %.1f%%, span %.1fh, breaks %d)",
					aboveShare * 100, perHour.length, BotCrackerConfig.EFFICIENCY_CEILING * 100, meanEff * 100,
					span / 3600000.0, t.getBreaksTaken())));
		}

		// --- invariance -------------------------------------------------------
		if (Stats.mean(perHour) > maxPerHour * 0.2) { // ignore idle-ish sessions
			double cv = Stats.cv(perHour);
			int q = Math.max(1, perHour.length / 4);
			double early = 0, late = 0;
			for (int i = 0; i < q; i++) {
				early += perHour[i];
				late += perHour[perHour.length - 1 - i];
			}
			double lateEarly = early == 0 ? 0 : late / early;

			double flat = Stats.ramp(cv, BotCrackerConfig.FATIGUE_CV_INVARIANT * 3, BotCrackerConfig.FATIGUE_CV_INVARIANT);
			double noDrop = Stats.ramp(lateEarly, BotCrackerConfig.FATIGUE_LATE_EARLY_RATIO - 0.15, BotCrackerConfig.FATIGUE_LATE_EARLY_RATIO);
			// no drop-off alone is weak evidence; weight it by how long the run is
			double longRun = Stats.ramp(span, BotCrackerConfig.FATIGUE_MIN_SPAN_MS, BotCrackerConfig.FATIGUE_MIN_SPAN_MS * 2.5);
			double conf = Stats.noisyOr(flat, noDrop * 0.6 * longRun);
			if (conf > 0.25) {
				out.add(new Finding(Finding.Source.FATIGUE_INVARIANCE, conf, perHour.length, String.format(
						"throughput cv=%.3f late/early=%.2f over %d windows, %.1fh continuous, %d breaks, %.0f xp",
						cv, lateEarly, perHour.length, span / 3600000.0, t.getBreaksTaken(), t.getSessionXp())));
			}
		}
	}
}
