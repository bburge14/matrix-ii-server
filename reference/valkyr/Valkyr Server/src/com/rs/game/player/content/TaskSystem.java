package com.rs.game.player.content;

import com.rs.game.player.Player;

public class TaskSystem {
	
	public static void sendTab(final Player player) {
		player.getInterfaceManager().sendInterface(1157);
		player.getPackets().sendIComponentText(1157, 0, "Task System");
		player.getPackets().sendIComponentText(1157, 3, "");	
	}
}
