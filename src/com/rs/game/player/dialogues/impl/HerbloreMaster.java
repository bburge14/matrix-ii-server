package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Poltax - Herblore. Opens shop 26 on Talk-to. */
public class HerbloreMaster extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		player.getPackets().sendGameMessage("Vials, herbs, secondaries. Mix carefully.");
		ShopsHandler.openShop(player, 26);
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
