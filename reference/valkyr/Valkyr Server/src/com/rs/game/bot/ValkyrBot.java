package com.rs.game.bot;

import com.rs.game.World;
import com.rs.game.player.Player;
import com.rs.net.Session;

@SuppressWarnings("serial")
public class ValkyrBot extends Player {
	
	public ValkyrBot(String password) {
		super(password);
		final Session session = new Session(null);
		init(session, "valkyr Bot_" + World.getPlayers().size(), 0, 0, 0, null, null);
		session.getLoginPackets().sendLoginDetails(this);
		session.setDecoder(3, this);
		session.setEncoder(2, this);
		start();
	}
	
}
