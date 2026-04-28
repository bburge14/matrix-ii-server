package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Head Farmer Jones - Farming. Opens shop 111 on Talk-to. */
public class FarmingMaster extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		player.getPackets().sendGameMessage("Seeds, watering cans, rakes, compost. Dig in.");
		ShopsHandler.openShop(player, 111);
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
