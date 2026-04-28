package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/** Drill Sergeant Hartman - Agility. Opens shop 220 on Talk-to. */
public class AgilityMaster extends Dialogue {

	@Override
	public void start() {
		NPC npc = (NPC) parameters[0];
		player.getPackets().sendGameMessage("Stamina pots, summer pies, and run replenishers. Now drop and give me twenty.");
		ShopsHandler.openShop(player, 220);
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
