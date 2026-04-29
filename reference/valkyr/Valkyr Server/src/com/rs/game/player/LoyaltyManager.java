package com.rs.game.player;

import java.util.TimerTask;
import com.rs.cores.CoresManager;

public class LoyaltyManager {

	private static final int INTERFACE_ID = 1143;
	private transient Player player;

	public LoyaltyManager(Player player) {
		this.player = player;
	}

	public void openLoyaltyStore(Player player) {
		player.getPackets().sendWindowsPane(INTERFACE_ID, 0);
	}

	public void startTimer() {
		CoresManager.fastExecutor.schedule(new TimerTask() {
			int timer = 1800;

			@Override
			public void run() {
				if (timer == 1) {
					if (player.gameMode == 3) {
						
					} else if (player.gameMode == 2) {
						
					} else if (player.gameMode == 1) {
						
					} else if (player.gameMode == 0) {
						
					}
					timer = 1800;
						}
				if (timer > 0) {
					timer--;
				}
			}
		}, 0L, 1000L);
	}
}