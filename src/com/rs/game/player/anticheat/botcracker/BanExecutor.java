package com.rs.game.player.anticheat.botcracker;

/**
 * Strategy the ban wave calls for each account that crossed the threshold.
 * Decoupled so a server can plug in a permanent ban, a temp ban, a
 * "flag for manual review" webhook, or a dry run.
 */
public interface BanExecutor {

	/**
	 * @return true if the account was actioned (and should leave the queue),
	 *         false to keep it shadowed.
	 */
	boolean execute(ShadowProfile profile, String reason);
}
