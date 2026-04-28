package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

public class DZSkillingMaster extends Dialogue {

	private NPC npc;

	@Override
	public void start() {
		npc = (NPC) parameters[0];
		if (!player.isDonator()) {
			sendNPCDialogue(npc.getId(), 9764, "This shop is reserved for donators. Get out of my zone.");
			stage = 99;
			return;
		}
		sendNPCDialogue(npc.getId(), 9827, "Welcome to the donator skilling supply, friend. Top-tier tools and every skillcape on the rack.");
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
				ShopsHandler.openShop(player, 204);
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
