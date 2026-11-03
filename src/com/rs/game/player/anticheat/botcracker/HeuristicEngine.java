package com.rs.game.player.anticheat.botcracker;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.rs.utils.Logger;

/**
 * Runs every registered {@link Detector} over a player's telemetry and folds
 * the findings into a single suspicion score.
 * <p>
 * Scoring: per source we keep the strongest finding, weight it, and combine
 * with noisy-OR. Noisy-OR means two independent 0.6 signals become 0.84: the
 * engine rewards agreement between orthogonal detectors (timing <i>and</i>
 * spatial <i>and</i> fatigue) far more than a single loud one. The number of
 * distinct sources that contributed is also surfaced so the ban wave can
 * require corroboration.
 */
public final class HeuristicEngine {

	/** Outcome of one evaluation pass. */
	public static final class Verdict {
		public final double score;
		public final int sources;
		public final List<Finding> findings;

		Verdict(double score, int sources, List<Finding> findings) {
			this.score = score;
			this.sources = sources;
			this.findings = findings;
		}

		public boolean exceeds(double threshold) {
			return score >= threshold;
		}
	}

	private final List<Detector> detectors = new ArrayList<Detector>();
	private final Map<Finding.Source, Double> weights = new EnumMap<Finding.Source, Double>(Finding.Source.class);

	public HeuristicEngine() {
		weights.put(Finding.Source.IAT_RIGIDITY, 1.0);
		weights.put(Finding.Source.TICK_ALIGNMENT, 0.9);
		weights.put(Finding.Source.FATIGUE_INVARIANCE, 0.8);
		weights.put(Finding.Source.EFFICIENCY_CEILING, 0.8);
		weights.put(Finding.Source.SPATIAL_CLUSTERING, 0.9);
		weights.put(Finding.Source.SPATIAL_UNIFORMITY, 0.6);
	}

	public HeuristicEngine register(Detector d) {
		detectors.add(d);
		return this;
	}

	public void setWeight(Finding.Source source, double weight) {
		weights.put(source, weight);
	}

	public List<Detector> getDetectors() {
		return detectors;
	}

	public Verdict evaluate(PlayerTelemetry t, long now) {
		List<Finding> findings = new ArrayList<Finding>();
		for (Detector d : detectors) {
			try {
				d.evaluate(t, now, findings);
			} catch (Throwable e) {
				Logger.handle(e);
			}
		}
		Map<Finding.Source, Double> best = new EnumMap<Finding.Source, Double>(Finding.Source.class);
		for (Finding f : findings) {
			Double cur = best.get(f.getSource());
			if (cur == null || f.getConfidence() > cur)
				best.put(f.getSource(), f.getConfidence());
		}
		double[] evidence = new double[best.size()];
		int i = 0, strong = 0;
		for (Map.Entry<Finding.Source, Double> e : best.entrySet()) {
			Double w = weights.get(e.getKey());
			evidence[i++] = e.getValue() * (w == null ? 1 : w);
			if (e.getValue() >= 0.5)
				strong++;
		}
		return new Verdict(Stats.noisyOr(evidence), strong, findings);
	}
}
