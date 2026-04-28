package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

public class DZSummoningSupplies extends Dialogue {

	private NPC npc;

	@Override
	public void start() {
		npc = (NPC) parameters[0];
		if (!player.isDonator()) {
			sendNPCDialogue(npc.getId(), 9764, "Donator-only obelisk supplies.");
			stage = 99;
			return;
		}
		sendNPCDialogue(npc.getId(), 9827, "Spirit shards, charms of every colour, pouches, and starter scrolls. Train hard at the obelisk.");
	}

	@Override
	public void run(int interfaceId, int componentId) {
		switch (stage) {
		case -1:
			stage = 0;
			sendOptionsDialogue(DEFAULT_OPTIONS_TITLE, "Open the shop.", "Maybe later.");
			break;
		case 0:
			if (componentId == OPTION_1) {
				ShopsHandler.openShop(player, 215);
			}
			end();
			break;
		default:
			end();
			break;
		}
	}

	@Override
	public void finish() {
	}
}
