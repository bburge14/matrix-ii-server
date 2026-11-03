package com.rs.game.player.anticheat.botcracker;

/**
 * Categories of player-originated events that BotCracker samples.
 * <p>
 * Each kind carries a {@code theoreticalMaxPerHour}: the hard ceiling a
 * perfectly efficient actor could reach on this server. Fatigue heuristics
 * compare observed throughput against this ceiling, so the numbers should be
 * tuned per-server (e.g. if your fletching action fires once per tick the
 * ceiling is 6000/h; if it fires every other tick it is 3000/h).
 */
public enum ActionKind {

	/** Any gameplay packet that expresses intent (clicks, walks, item use). */
	INPUT_PACKET(6000, false),
	/** Raw mouse click with screen coordinates (CLICK_PACKET). */
	MOUSE_CLICK(6000, false),
	/** A completed production cycle of a repetitive skill (fletch, craft, cook...). */
	SKILL_ACTION(6000, true),
	/** Item-on-item / interface-on-interface (what a dart fletcher spams). */
	ITEM_ON_ITEM(6000, true),
	/** Walk request. */
	WALK(6000, false),
	/** Object / NPC interaction click. */
	WORLD_INTERACT(6000, true);

	private final int theoreticalMaxPerHour;
	private final boolean productive;

	ActionKind(int theoreticalMaxPerHour, boolean productive) {
		this.theoreticalMaxPerHour = theoreticalMaxPerHour;
		this.productive = productive;
	}

	/** Upper bound of actions per hour a flawless actor could sustain. */
	public int getTheoreticalMaxPerHour() {
		return theoreticalMaxPerHour;
	}

	/** Whether this action advances a skill (used for "efficiency" scoring). */
	public boolean isProductive() {
		return productive;
	}
}
