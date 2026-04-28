package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Ellis the Tanner (NPC 18) at Al Kharid - buys hides+bones, sells tanning tools. */
public class TannerBonesman extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		player.getPackets().sendGameMessage("Welcome. I tan hides and buy bones. Browse my supplies.");
		ShopsHandler.openShop(player, 203);
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
