package com.rs.game.player.anticheat.botcracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Spatial and clustering heuristics over raw screen clicks.
 * <p>
 * Clicks are greedily assigned to clusters (first centre within
 * {@link BotCrackerConfig#CLICK_CLUSTER_RADIUS_PX}). For each cluster with
 * enough members we measure the standard deviation of click offsets from the
 * running centre. Human clicks on a fixed hitbox (an inventory slot, a bank
 * booth) scatter with sd of several pixels; a remapper that re-sends the same
 * coordinate, or a bot with no jitter, produces sd near 0 and a very high
 * pixel-duplicate ratio.
 * <p>
 * Conversely, tools that add jitter usually draw from a uniform box, often on
 * an integer stride. We flag "synthetic uniformity" when a large share of
 * clicks sit on a coarse grid, or when the per-cluster offset distribution has
 * the flat (platykurtic) shape of a uniform box rather than the peaked shape
 * of a human aim distribution.
 */
public final class SpatialClusterDetector implements Detector {

	private static final class Cluster {
		double cx, cy;
		final List<PlayerTelemetry.ClickSample> members = new ArrayList<PlayerTelemetry.ClickSample>();

		Cluster(PlayerTelemetry.ClickSample s) {
			cx = s.x;
			cy = s.y;
			members.add(s);
		}

		void add(PlayerTelemetry.ClickSample s) {
			members.add(s);
			int n = members.size();
			cx += (s.x - cx) / n;
			cy += (s.y - cy) / n;
		}
	}

	@Override
	public String name() {
		return "spatial";
	}

	@Override
	public void evaluate(PlayerTelemetry t, long now, List<Finding> out) {
		List<PlayerTelemetry.ClickSample> clicks = t.clickSnapshot();
		if (clicks.size() < BotCrackerConfig.CLICK_MIN_SAMPLES)
			return;

		// --- duplicates -------------------------------------------------------
		// Consecutive pixel-identical clicks. A human re-clicking one hitbox
		// repeats pixels occasionally; a replayed coordinate repeats every time.
		int dup = 0;
		for (int i = 1; i < clicks.size(); i++) {
			PlayerTelemetry.ClickSample a = clicks.get(i - 1), b = clicks.get(i);
			if (a.x == b.x && a.y == b.y)
				dup++;
		}
		double dupRatio = (double) dup / (clicks.size() - 1);

		// --- clustering ---------------------------------------------------------
		List<Cluster> clusters = new ArrayList<Cluster>();
		int radius = BotCrackerConfig.CLICK_CLUSTER_RADIUS_PX;
		long r2 = (long) radius * radius;
		outer: for (PlayerTelemetry.ClickSample s : clicks) {
			for (Cluster c : clusters) {
				double dx = s.x - c.cx, dy = s.y - c.cy;
				if (dx * dx + dy * dy <= r2) {
					c.add(s);
					continue outer;
				}
			}
			clusters.add(new Cluster(s));
		}

		int bigClusters = 0, bigMembers = 0;
		double weightedSd = 0;
		double flatness = 0; // kurtosis-based uniform-box indicator
		for (Cluster c : clusters) {
			if (c.members.size() < 20)
				continue;
			bigClusters++;
			bigMembers += c.members.size();
			double sx = 0, sy = 0, m4 = 0;
			for (PlayerTelemetry.ClickSample s : c.members) {
				double dx = s.x - c.cx, dy = s.y - c.cy;
				sx += dx * dx;
				sy += dy * dy;
			}
			double var = (sx + sy) / (2.0 * c.members.size());
			double sd = Math.sqrt(var);
			weightedSd += sd * c.members.size();
			if (var > 0.5) {
				for (PlayerTelemetry.ClickSample s : c.members) {
					double dx = s.x - c.cx, dy = s.y - c.cy;
					double d2 = (dx * dx + dy * dy) / (2 * var);
					m4 += d2 * d2;
				}
				// radial kurtosis of a 2-D gaussian ~ 2.0; uniform disc ~ 1.33
				double kurt = m4 / c.members.size();
				flatness += Stats.ramp(kurt, 1.9, 1.4) * c.members.size();
			}
		}
		if (bigMembers > 0) {
			weightedSd /= bigMembers;
			flatness /= bigMembers;
		}

		// --- grid alignment -----------------------------------------------------
		int onGrid = 0;
		for (PlayerTelemetry.ClickSample s : clicks)
			if (s.x % 5 == 0 && s.y % 5 == 0)
				onGrid++;
		double gridShare = (double) onGrid / clicks.size();
		// a uniform human distribution lands on the 5x5 grid 4% of the time
		double grid = Stats.ramp(gridShare, 0.15, BotCrackerConfig.CLICK_GRID_SHARE);

		// --- findings -------------------------------------------------------------
		double tight = bigMembers == 0 ? 0 : Stats.ramp(weightedSd, BotCrackerConfig.CLICK_CLUSTER_STDDEV_PX * 3, BotCrackerConfig.CLICK_CLUSTER_STDDEV_PX);
		double dupes = Stats.ramp(dupRatio, BotCrackerConfig.CLICK_DUPLICATE_RATIO * 0.5, BotCrackerConfig.CLICK_DUPLICATE_RATIO);
		// duplicates only count when the surrounding scatter is also tight,
		// otherwise a human hammering one inventory slot would trip this
		if (bigMembers > 0)
			dupes *= Stats.ramp(weightedSd, BotCrackerConfig.CLICK_CLUSTER_STDDEV_PX * 3, BotCrackerConfig.CLICK_CLUSTER_STDDEV_PX * 1.5);
		double clustering = Stats.noisyOr(tight, dupes);
		if (clustering > 0.25) {
			out.add(new Finding(Finding.Source.SPATIAL_CLUSTERING, clustering, clicks.size(), String.format(
					"%d clusters (%d dense, %d clicks) weighted sd=%.2fpx duplicate ratio=%.2f", clusters.size(),
					bigClusters, bigMembers, weightedSd, dupRatio)));
		}
		double uniformity = Stats.noisyOr(grid, flatness * 0.8);
		if (uniformity > 0.25) {
			out.add(new Finding(Finding.Source.SPATIAL_UNIFORMITY, uniformity, clicks.size(), String.format(
					"grid(5px) share=%.2f box-flatness=%.2f across %d dense clusters", gridShare, flatness,
					bigClusters)));
		}
	}
}
