package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Jacquelyn Manslaughter - Slayer. Opens shop 29 on Talk-to. */
public class SlayerSupplies extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		player.getPackets().sendGameMessage("Slayer helms, gems, antipoison, leather - everything for the hunt.");
		ShopsHandler.openShop(player, 29);
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
