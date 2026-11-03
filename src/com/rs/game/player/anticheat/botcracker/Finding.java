package com.rs.game.player.anticheat.botcracker;

/**
 * Immutable result emitted by a detector. A finding carries a confidence in
 * [0, 1], a human readable explanation and the raw numbers that produced it so
 * a moderator can audit the decision before a ban wave runs.
 */
public final class Finding {

	public enum Source {
		IAT_RIGIDITY, TICK_ALIGNMENT, FATIGUE_INVARIANCE, EFFICIENCY_CEILING, SPATIAL_CLUSTERING, SPATIAL_UNIFORMITY
	}

	private final Source source;
	private final double confidence;
	private final String explanation;
	private final long timestamp;
	private final int sampleSize;

	public Finding(Source source, double confidence, int sampleSize, String explanation) {
		this.source = source;
		this.confidence = Math.max(0, Math.min(1, confidence));
		this.sampleSize = sampleSize;
		this.explanation = explanation;
		this.timestamp = System.currentTimeMillis();
	}

	public Source getSource() {
		return source;
	}

	public double getConfidence() {
		return confidence;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public String getExplanation() {
		return explanation;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return String.format("%s conf=%.2f n=%d :: %s", source, confidence, sampleSize, explanation);
	}
}
