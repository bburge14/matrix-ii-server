package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Master Pet Shop - sells every pet in the cache. Opens shop 226. */
public class MasterPetShop extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		player.getPackets().sendGameMessage("Welcome to the Master Pet Shop. I have every pet your heart could desire.");
		ShopsHandler.openShop(player, 226);
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
