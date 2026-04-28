package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Donator-zone supply NPC. Opens shop 211 directly on Talk-to. */
public class DZRcConPraSupplies extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		if (!player.isDonator()) {
			player.getPackets().sendGameMessage("This shop is reserved for donators.");
			end();
			return;
		}
		player.getPackets().sendGameMessage("DZ runecraft/construction/prayer supply.");
		ShopsHandler.openShop(player, 211);
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
