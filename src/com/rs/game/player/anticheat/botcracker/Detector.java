package com.rs.game.player.anticheat.botcracker;

import java.util.List;

/** A stateless heuristic that inspects a telemetry snapshot and emits findings. */
public interface Detector {

	/** Short identifier for logs and the admin report. */
	String name();

	/**
	 * Evaluate the player and append zero or more findings. Implementations
	 * must only read snapshots from {@code t} (never hold its lock while doing
	 * heavy maths) and must never throw.
	 */
	void evaluate(PlayerTelemetry t, long now, List<Finding> out);
}
