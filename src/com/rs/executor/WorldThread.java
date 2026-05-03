package com.rs.executor;

import com.rs.Settings;
import com.rs.game.World;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Logger;
import com.rs.utils.Utils;
import com.rs.bot.AIPlayer;

public final class WorldThread extends Thread {

	public static volatile long WORLD_CYCLE;

	protected WorldThread() {
		setPriority(Thread.MAX_PRIORITY);
		setName("World Thread");
	}

	@Override
	public final void run() {
		while (!GameExecutorManager.executorShutdown) {
			WORLD_CYCLE++; //made the cycle update at begin instead of end cuz at end theres 600ms then to next cycle
			long currentTime = Utils.currentTimeMillis();
			//     long debug = Utils.currentTimeMillis();
			WorldTickProfiler.start("worldTasks");
			WorldTasksManager.processTasks();
			WorldTickProfiler.end("worldTasks");
			try {
				WorldTickProfiler.start("processEntity");
				for (Player player : World.getPlayers()) {
					if (!player.hasStarted() || player.hasFinished())
						continue;
					player.processEntity();
				}
				for (NPC npc : World.getNPCs()) {
					if (npc == null || npc.hasFinished())
						continue;
					npc.processEntity();
				}
				WorldTickProfiler.end("processEntity");
			} catch (Throwable e) {
				Logger.handle(e);
			}
			try {
				WorldTickProfiler.start("processEntityUpdate");
				for (Player player : World.getPlayers()) {
					if (!player.hasStarted() || player.hasFinished())
						continue;
					player.processEntityUpdate();
				}
				for (NPC npc : World.getNPCs()) {
					if (npc == null || npc.hasFinished())
						continue;
					npc.processEntityUpdate();
				}
				WorldTickProfiler.end("processEntityUpdate");
			} catch (Throwable e) {
				Logger.handle(e);
			}
			try {
				// //
				// System.out.print(" ,NPCS PROCESS: "+(Utils.currentTimeMillis()-debug));
				// debug = Utils.currentTimeMillis();
				WorldTickProfiler.start("sendLocalUpdates");
				for (Player player : World.getPlayers()) {
					if (!player.hasStarted() || player.hasFinished())
						continue;
					if (player instanceof AIPlayer)
						continue;
					player.getPackets().sendLocalPlayersUpdate();
					player.getPackets().sendLocalNPCsUpdate();
					player.processProjectiles();//waits for player to walk and so on
				}
				WorldTickProfiler.end("sendLocalUpdates");
			} catch (Throwable e) {
				Logger.handle(e);
			}
			try {
				World.removeProjectiles();
			} catch (Throwable e) {
				Logger.handle(e);
			}
			try {
				// System.out.print(" ,PLAYER UPDATE: "+(Utils.currentTimeMillis()-debug)+", "+World.getPlayers().size()+", "+World.getNPCs().size());
				// debug = Utils.currentTimeMillis();
				WorldTickProfiler.start("resetMasks");
				for (Player player : World.getPlayers()) {
					if (!player.hasStarted() || player.hasFinished())
						continue;
					player.resetMasks();
				}
				for (NPC npc : World.getNPCs()) {
					if (npc == null || npc.hasFinished())
						continue;
					npc.resetMasks();
				}
				WorldTickProfiler.end("resetMasks");
			} catch (Throwable e) {
				Logger.handle(e);
			}

			try {
				WorldTickProfiler.start("connectionCheck");
				for (Player player : World.getPlayers()) {
					if (!player.hasStarted() || player.hasFinished())
						continue;
					if (player instanceof AIPlayer)
						continue;
					if (!player.getSession().getChannel().isConnected())
						player.finish(); //requests finish, wont do anything if already requested btw
				}
				for (Player player : World.getLobbyPlayers()) {
					if (!player.hasStarted() || player.hasFinished())
						continue;
					if (player instanceof AIPlayer)
						continue;
					if (!player.getSession().getChannel().isConnected())
						player.finish(); //requests finish, wont do anything if already requested btw
				}
				WorldTickProfiler.end("connectionCheck");
			} catch (Throwable e) {
				Logger.handle(e);
			}

			WorldTickProfiler.onTickComplete();
			// //
			// Logger.log(this, "TOTAL: "+(Utils.currentTimeMillis()-currentTime));
			long sleepTime = Settings.WORLD_CYCLE_TIME + currentTime - Utils.currentTimeMillis();
			if (sleepTime <= 0)
				continue;
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				Logger.handle(e);
			}
		}
	}

}
