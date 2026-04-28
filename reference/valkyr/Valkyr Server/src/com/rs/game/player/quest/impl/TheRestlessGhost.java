package com.rs.game.player.quest.impl;

import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.QuestManager.Quests;

public class TheRestlessGhost {

	public static void handleQuestCompleteInterface(final Player player) {
		player.getInterfaceManager().sendInterface(277);
		player.getPackets().sendIComponentText(277, 4, "You have completed The Restless Ghost.");
		player.getPackets().sendIComponentText(277, 7, "" + player.questPoints);
		player.getPackets().sendIComponentText(277, 9, "You are awarded:");
		player.getPackets().sendIComponentText(277, 10, "1 Quest Point");
		player.getPackets().sendIComponentText(277, 11, "1125 Prayer XP");
		player.getPackets().sendIComponentText(277, 12, "");
		player.getPackets().sendIComponentText(277, 13, "Two spins on the Squeal of Fortune");
		player.getPackets().sendIComponentText(277, 14, "");
		player.getPackets().sendIComponentText(277, 15, "");
		player.getPackets().sendIComponentText(277, 16, "");
		player.getPackets().sendIComponentText(277, 17, "");
		player.getPackets().sendItemOnIComponent(277, 5, 553, 1);
	}
	
}
