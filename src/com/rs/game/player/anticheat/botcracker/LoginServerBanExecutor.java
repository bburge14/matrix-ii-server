package com.rs.game.player.anticheat.botcracker;

import com.rs.game.World;
import com.rs.game.player.Player;
import com.rs.net.LoginProtocol;
import com.rs.net.encoders.LoginChannelsPacketEncoder;
import com.rs.net.LoginClientChannelManager;

/**
 * Default executor: files an offence with the login server (the same path the
 * ::punish dialogue uses) and kicks the account if it is online.
 */
public final class LoginServerBanExecutor implements BanExecutor {

	public static final String MODERATOR = "BotCracker";

	private final int offenceType;
	private final long durationMs;

	/** Permanent account ban. */
	public LoginServerBanExecutor() {
		this(LoginProtocol.OFFENCE_ADDTYPE_BAN, 0);
	}

	/**
	 * @param offenceType one of {@link LoginProtocol#OFFENCE_ADDTYPE_BAN} /
	 *            {@link LoginProtocol#OFFENCE_ADDTYPE_IPBAN}
	 * @param durationMs 0 for permanent
	 */
	public LoginServerBanExecutor(int offenceType, long durationMs) {
		this.offenceType = offenceType;
		this.durationMs = durationMs;
	}

	@Override
	public boolean execute(ShadowProfile profile, String reason) {
		long expires = durationMs <= 0 ? 0 : System.currentTimeMillis() + durationMs;
		LoginClientChannelManager.sendUnreliablePacket(LoginChannelsPacketEncoder
				.encodeAddOffence(offenceType, profile.getUsername(), MODERATOR, reason, expires).getBuffer());
		Player online = World.getPlayer(profile.getUsername());
		if (online != null)
			online.disconnect(true, false);
		return true;
	}
}
