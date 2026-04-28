package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

public class OreTrader extends Dialogue {

	private NPC npc;

	@Override
	public void start() {
		npc = (NPC) parameters[0];
		sendNPCDialogue(npc.getId(), 9827, "Got ore to sell or need a pickaxe? I trade in everything from copper to runite.");
	}

	@Override
	public void run(int interfaceId, int componentId) {
		switch (stage) {
		case -1:
			stage = 0;
			sendOptionsDialogue(DEFAULT_OPTIONS_TITLE, "Open the shop.", "Just browsing.");
			break;
		case 0:
			if (componentId == OPTION_1) {
				ShopsHandler.openShop(player, 202);
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
