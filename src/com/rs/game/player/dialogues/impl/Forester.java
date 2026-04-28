package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Lumberjack Leif (NPC 1401) - Woodcutting master + log buyer. */
public class Forester extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		String greet = "Got logs to sell or need a hatchet? I trade in lumber.";
		player.getPackets().sendGameMessage(greet);
		ShopsHandler.openShop(player, 200);
		end();
	}

	@Override
	public void run(int interfaceId, int componentId) {
		end();
	}

	@Override
	public void finish() {
	}
}
