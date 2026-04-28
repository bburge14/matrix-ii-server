package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

public class DZBossingBuffet extends Dialogue {

	private NPC npc;

	@Override
	public void start() {
		npc = (NPC) parameters[0];
		if (!player.isDonator()) {
			sendNPCDialogue(npc.getId(), 9764, "The buffet is for paying customers only.");
			stage = 99;
			return;
		}
		sendNPCDialogue(npc.getId(), 9827, "Brews, restores, prayer, supers, sharks, rocktails - all you can carry. Heads down and crush some bosses.");
	}

	@Override
	public void run(int interfaceId, int componentId) {
		switch (stage) {
		case -1:
			stage = 0;
			sendOptionsDialogue(DEFAULT_OPTIONS_TITLE, "Open the buffet.", "Maybe later.");
			break;
		case 0:
			if (componentId == OPTION_1) {
				ShopsHandler.openShop(player, 206);
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
