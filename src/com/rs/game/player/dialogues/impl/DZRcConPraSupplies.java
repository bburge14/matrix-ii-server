package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

public class DZRcConPraSupplies extends Dialogue {

	private NPC npc;

	@Override
	public void start() {
		npc = (NPC) parameters[0];
		if (!player.isDonator()) {
			sendNPCDialogue(npc.getId(), 9764, "Donator-only zone supplies. Get out.");
			stage = 99;
			return;
		}
		sendNPCDialogue(npc.getId(), 9827, "Essence, talismans, planks, marble blocks, holy symbols, bones - all the stuff to runecraft, build, or pray.");
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
				ShopsHandler.openShop(player, 211);
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
