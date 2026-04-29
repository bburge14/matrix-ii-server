package com.rs.game.player;

import com.rs.Settings;
import com.rs.game.GameEngine;
import com.rs.game.WorldTile;
import com.rs.game.player.content.Magic;
import com.rs.game.task.Task;

public class TeleportManager {
	
	public static void teleportGodwars(Player player, WorldTile location) {
		Magic.sendNormalTeleportSpell(player, 0, 0, location);
		GameEngine.get().addTask(new Task() {

			int delay;
			
			@Override
			protected void execute() {
				
				if (delay == 5) {
					player.getControlerManager().startControler("GodWars");	
					stop();
				} else {
					delay++;
				}
			}
			
		});
	}
	
	public static void sendHomeTeleport(Player player, boolean combatCheck) {
		
		if (player.inCombat() && combatCheck && !player.isOwner()) {
			player.getPackets().sendGameMessage("You can't teleport while in combat.", false);
			return;
		}
		
		player.getPackets().sendStopCameraShake(); //Removes camera shake from barrows.
		player.getPackets().closeInterface(player.getInterfaceManager().hasRezizableScreen() ? 11 : 0); //Removes barrows interface
		player.getControlerManager().forceStop(); //Ends all controlers
		
		Magic.sendNormalTeleportSpell(player, 0, 0, Settings.START_PLAYER_LOCATION);	
		
		player.getPackets().sendGameMessage("Welcome Home " + player.getDisplayName() + ".", false);
		
	}
	
	public static void teleportNormal(Player player, WorldTile location, boolean combatCheck) {
		
		player.getInterfaceManager().closeScreenInterface();
		
		if (player.inCombat() && combatCheck) {
			player.getPackets().sendGameMessage("You can't teleport while in combat.", false);
			return;
		}
		
		GameEngine.get().addTask(new Task() {

			int delay;
			
			@Override
			public void execute() {
				if (delay == 0) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 5 seconds.");
				}
				else if (delay == 1) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 4 seconds.");
				}
				else if (delay == 2) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 3 seconds.");
				}
				else if (delay == 3) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 2 seconds.");
				}
				else if (delay == 4) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 1 seconds.");
				}
				else if (delay == 5) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 0 seconds.");
				}
				else if (delay == 6) {
					Magic.sendNormalTeleportSpell(player, 0, 0, location);
					stop();
				}
				delay++;
			}			
		});		
	}
	
	public static void teleportWilderness(Player player, WorldTile location, boolean combatCheck) {
		
		player.getInterfaceManager().closeScreenInterface();
		
		if (player.inCombat() && combatCheck) {
			player.getPackets().sendGameMessage("You can't teleport while in combat.", false);
			return;
		}
		
		GameEngine.get().addTask(new Task() {

			int delay;
			
			@Override
			public void execute() {
				if (delay == 0) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 3 seconds.");
				}
				else if (delay == 1) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 2 seconds.");
				}
				else if (delay == 2) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 1 seconds.");
				}
				else if (delay == 3) {
					if (player.getNextWalkDirection() != -1) {
						player.getDialogueManager().startDialogue("SimpleMessage", "Teleport Canceled...");
						stop();
						return;
					}
					player.getDialogueManager().startDialogue("SimpleMessage", "Teleporting in ... 0 seconds.");
				}
				else if (delay == 4) {
					Magic.sendNormalTeleportSpell(player, 0, 0, location);
					player.getControlerManager().startControler("Wilderness");
					stop();
				}
				delay++;
			}			
		});	
	}
	
	
}
