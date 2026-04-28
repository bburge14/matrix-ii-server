package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

public class DZCombatMaster extends Dialogue {

	private NPC npc;

	@Override
	public void start() {
		npc = (NPC) parameters[0];
		if (!player.isDonator()) {
			sendNPCDialogue(npc.getId(), 9764, "Donator-only. Earn your keep first.");
			stage = 99;
			return;
		}
		sendNPCDialogue(npc.getId(), 9827, "Bandos, Pernix, Torva, Virtus - I've got the whole rack. Donator pricing only.");
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
				ShopsHandler.openShop(player, 205);
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
