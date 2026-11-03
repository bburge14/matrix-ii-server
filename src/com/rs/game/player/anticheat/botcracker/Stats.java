package com.rs.game.player.anticheat.botcracker;

import java.util.HashSet;
import java.util.Set;

/** Small, allocation-light statistics helpers shared by the detectors. */
final class Stats {

	private Stats() {
	}

	static double mean(long[] v) {
		if (v.length == 0)
			return 0;
		double s = 0;
		for (long x : v)
			s += x;
		return s / v.length;
	}

	static double mean(double[] v) {
		if (v.length == 0)
			return 0;
		double s = 0;
		for (double x : v)
			s += x;
		return s / v.length;
	}

	static double stddev(long[] v, double mean) {
		if (v.length < 2)
			return 0;
		double s = 0;
		for (long x : v) {
			double d = x - mean;
			s += d * d;
		}
		return Math.sqrt(s / (v.length - 1));
	}

	static double stddev(double[] v, double mean) {
		if (v.length < 2)
			return 0;
		double s = 0;
		for (double x : v) {
			double d = x - mean;
			s += d * d;
		}
		return Math.sqrt(s / (v.length - 1));
	}

	/** Coefficient of variation; 0 when mean is 0. */
	static double cv(long[] v) {
		double m = mean(v);
		return m == 0 ? 0 : stddev(v, m) / m;
	}

	static double cv(double[] v) {
		double m = mean(v);
		return m == 0 ? 0 : stddev(v, m) / m;
	}

	/** Lag-1 autocorrelation in [-1, 1]. */
	static double lag1Autocorrelation(long[] v) {
		if (v.length < 3)
			return 0;
		double m = mean(v);
		double num = 0, den = 0;
		for (int i = 0; i < v.length; i++) {
			double d = v[i] - m;
			den += d * d;
			if (i > 0)
				num += d * (v[i - 1] - m);
		}
		return den == 0 ? 0 : num / den;
	}

	/**
	 * Normalised Shannon entropy (0..1) of a histogram. 1 means perfectly
	 * spread across bins, 0 means everything in one bin.
	 */
	static double normalisedEntropy(int[] hist) {
		long total = 0;
		for (int h : hist)
			total += h;
		if (total == 0 || hist.length < 2)
			return 1;
		double e = 0;
		for (int h : hist) {
			if (h == 0)
				continue;
			double p = (double) h / total;
			e -= p * Math.log(p);
		}
		return e / Math.log(hist.length);
	}

	static int argmax(int[] hist) {
		int best = 0;
		for (int i = 1; i < hist.length; i++)
			if (hist[i] > hist[best])
				best = i;
		return best;
	}

	static double uniqueRatio(long[] v) {
		if (v.length == 0)
			return 1;
		Set<Long> set = new HashSet<Long>();
		for (long x : v)
			set.add(x);
		return (double) set.size() / v.length;
	}

	/** Linear ramp: 0 at {@code lo}, 1 at {@code hi} (or reversed when hi < lo). */
	static double ramp(double x, double lo, double hi) {
		if (lo == hi)
			return x >= hi ? 1 : 0;
		double t = (x - lo) / (hi - lo);
		return Math.max(0, Math.min(1, t));
	}

	/** Noisy-OR combination of independent evidences. */
	static double noisyOr(double... p) {
		double q = 1;
		for (double x : p)
			q *= (1 - Math.max(0, Math.min(1, x)));
		return 1 - q;
	}
}
