package com.rs.game.player.anticheat.botcracker;

import java.util.List;

/**
 * Inter-Arrival Time (IAT) and tick-alignment analysis.
 * <p>
 * For every {@link ActionKind} we keep the last N deltas between consecutive
 * actions and the phase of each action inside the 600 ms world cycle. Four
 * independent signals are extracted:
 * <ol>
 * <li><b>Rigidity</b>: coefficient of variation of the IATs. Human spam-click
 * timing over hundreds of actions is noisy (CV 0.25+). Timers, even with
 * jitter, collapse well below 0.1.</li>
 * <li><b>Tick lock</b>: a histogram of the arrival phase inside the tick. A
 * human's packets land anywhere in the tick (high entropy). Tools that pace
 * themselves on the tick, or on a fixed period that is a multiple of 600 ms,
 * pile up in one or two bins.</li>
 * <li><b>Periodicity</b>: lag-1 autocorrelation of the IAT series. Alternating
 * or sawtooth "fatigue" generators produce strong |r1|.</li>
 * <li><b>Quantisation</b>: fraction of unique millisecond deltas. PRNG timing
 * generators that round to coarse steps, or hardware remappers with a fixed
 * clock, produce very few distinct values.</li>
 * </ol>
 * Signals are combined with noisy-OR so a single clean signal is enough to
 * raise a finding, while several weak ones also accumulate.
 */
public final class IATAnomalyDetector implements Detector {

	@Override
	public String name() {
		return "iat";
	}

	@Override
	public void evaluate(PlayerTelemetry t, long now, List<Finding> out) {
		for (ActionKind kind : ActionKind.values()) {
			long[] deltas = t.iatSnapshot(kind);
			if (deltas.length < BotCrackerConfig.IAT_MIN_SAMPLES)
				continue;
			evaluateKind(kind, deltas, t.phaseSnapshot(kind), out);
		}
	}

	private void evaluateKind(ActionKind kind, long[] deltas, long[] phases, List<Finding> out) {
		double mean = Stats.mean(deltas);
		double sd = Stats.stddev(deltas, mean);
		double cv = mean == 0 ? 0 : sd / mean;
		double r1 = Stats.lag1Autocorrelation(deltas);
		double unique = Stats.uniqueRatio(deltas);

		// --- rigidity / periodicity / quantisation ------------------------
		double rigid = Stats.ramp(cv, BotCrackerConfig.IAT_CV_RIGID * 2, BotCrackerConfig.IAT_CV_RIGID);
		double periodic = Stats.ramp(Math.abs(r1), BotCrackerConfig.IAT_AUTOCORR_SUSPICIOUS * 0.7, BotCrackerConfig.IAT_AUTOCORR_SUSPICIOUS);
		double quantised = Stats.ramp(unique, BotCrackerConfig.IAT_UNIQUE_RATIO_LOW * 3, BotCrackerConfig.IAT_UNIQUE_RATIO_LOW);
		// quantisation is meaningless when the mean IAT itself is tiny
		if (mean < 50)
			quantised = 0;

		double rigidity = Stats.noisyOr(rigid, periodic * 0.8, quantised * 0.7);
		if (rigidity > 0.25) {
			out.add(new Finding(Finding.Source.IAT_RIGIDITY, rigidity, deltas.length, String.format(
					"%s IAT mean=%.0fms sd=%.1fms cv=%.3f r1=%.2f unique=%.3f", kind, mean, sd, cv, r1, unique)));
		}

		// --- tick alignment -----------------------------------------------
		if (phases.length >= BotCrackerConfig.IAT_MIN_SAMPLES) {
			int[] hist = new int[BotCrackerConfig.PHASE_BINS];
			int binWidth = BotCrackerConfig.TICK_MS / BotCrackerConfig.PHASE_BINS;
			for (long p : phases)
				hist[(int) Math.min(hist.length - 1, p / binWidth)]++;
			double entropy = Stats.normalisedEntropy(hist);
			int peak = Stats.argmax(hist);
			double peakShare = (double) hist[peak] / phases.length;

			double locked = Stats.ramp(entropy, BotCrackerConfig.PHASE_ENTROPY_LOCKED + 0.25, BotCrackerConfig.PHASE_ENTROPY_LOCKED);
			double peaked = Stats.ramp(peakShare, BotCrackerConfig.PHASE_PEAK_SHARE - 0.25, BotCrackerConfig.PHASE_PEAK_SHARE);
			double alignment = Stats.noisyOr(locked, peaked);
			if (alignment > 0.25) {
				out.add(new Finding(Finding.Source.TICK_ALIGNMENT, alignment, phases.length, String.format(
						"%s phase entropy=%.3f peakBin=%d (%d-%dms) share=%.2f", kind, entropy, peak, peak * binWidth,
						(peak + 1) * binWidth, peakShare)));
			}
		}
	}
}
