package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Alfred Stonemason - Construction. Opens shop 128 on Talk-to. */
public class ConstructionMaster extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		player.getPackets().sendGameMessage("Planks, marble, gold leaf, and a hammer. Build something grand.");
		ShopsHandler.openShop(player, 128);
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
