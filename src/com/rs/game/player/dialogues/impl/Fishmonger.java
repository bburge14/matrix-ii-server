package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Master Fisher Harry (NPC 666) at Catherby - buys raw fish + sells fishing tools. */
public class Fishmonger extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		player.getPackets().sendGameMessage("Welcome to my fishing shop. Got fish to sell? Need a rod?");
		ShopsHandler.openShop(player, 201);
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
