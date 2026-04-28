package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** DZ Firemaking supply (donator-gated). Opens shop 218 (same as free-zone). */
public class DZFiremakingSupplies extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		if (!player.isDonator()) {
			player.getPackets().sendGameMessage("This shop is reserved for donators.");
			end();
			return;
		}
		player.getPackets().sendGameMessage("DZ firemaking supply - logs aplenty, tinderboxes, lanterns.");
		ShopsHandler.openShop(player, 218);
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
