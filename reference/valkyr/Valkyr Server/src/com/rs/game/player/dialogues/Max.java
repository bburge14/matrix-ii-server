package com.rs.game.player.dialogues;

import com.rs.Settings;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.game.WorldTile;
import com.rs.game.minigames.CastleWars;
import com.rs.game.player.Skills;
import com.rs.game.player.content.Magic;
import com.rs.game.player.controlers.FightCaves;
import com.rs.game.player.controlers.FightKiln;

public class Max extends Dialogue {

	private int npcId;

	@Override
	public void start() {
		if (Settings.ECONOMY) {
			player.getPackets().sendGameMessage("Mr.Ex is in no mood to talk to you.");
			end();
			return;
		}
		sendEntityDialogue(SEND_2_TEXT_CHAT,
				new String[] { NPCDefinitions.getNPCDefinitions(npcId).name,
						"Hello I can teleport you to all the skilling area's,",
						" would you like to?" }, IS_NPC, npcId, 9827);
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (stage == -1) {
			sendEntityDialogue(SEND_1_TEXT_CHAT,
					new String[] { player.getDisplayName(), "Sure, why not." },
					IS_PLAYER, player.getIndex(), 9827);
			stage = 1;
		} else if (stage == 1) {
			sendOptionsDialogue("Where would you like to go?", "Mining",
					"Smithing", "Construction", "Rune Span", "More Options");
			stage = 2;
		} else if (stage == 2) {
			if (componentId == OPTION_1)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3297, 3298, 0)); //Mining
			else if (componentId == OPTION_2)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2710, 3493, 0)); //Smithing
			else if (componentId == OPTION_3)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2544, 3095, 0)); //Construction
			else if (componentId == OPTION_4) {
				sendOptionsDialogue("The RuneSpan", "1st Level", "2nd Level",  "3rd Level" ); //RuneSpan
				stage = 69;
			} else if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Hunter", "Agility (Gnome Agility)", "Woodcutting.", "Thieving", "More Options");
				stage = 3;
			}
		} else if (stage == 3) {
			if (componentId == OPTION_1) {
				sendOptionsDialogue("Where would you like to go?", "Birds", "Red Chinchompas", "~Back to Beginning~");
				stage = 6;
			} else if (componentId == OPTION_2)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2470, 3436, 0));
			else if (componentId == OPTION_3)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2724, 3485, 0));
			else if (componentId == OPTION_4)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2662, 3305, 0));
			else if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Farming", "Fishing", "Higher Fishing", "Advanced Barb (agility)", "More Options");
				stage = 4;
			}
		} else if (stage == 4) {
			if (componentId == OPTION_1) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3052, 3304, 0));
			} else if (componentId == OPTION_2) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3088, 3230, 0));
			} else if (componentId == OPTION_3)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2843, 3433, 0));
			else if (componentId == OPTION_4) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2551, 3557, 0));
			} else if (componentId == OPTION_5) {
				stage = 5;
				sendOptionsDialogue("Where would you like to go?",
						"Abyss (Runecrafting)", "Dungeoneering", "Tree Farm", "Lrc highlevel mining",
						"Back to the first page");
			}
		} else if (stage == 5) {
			if (componentId == OPTION_1) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2610, 2928, 0));
			}	else if (componentId == OPTION_2) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3449, 3743, 0));
			}	 else if (componentId == OPTION_3) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3004, 3378, 0));
			} else if (componentId == OPTION_4) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3652, 5121, 0));   
			} else if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Mining", "Smithing", "Construction", "Removed", "More Options");
				stage = 2;
			}
		} else if (stage == 6) {
			if (componentId == OPTION_1)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2608, 2931, 0));
			else if (componentId == OPTION_2)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2556, 2935, 0));
			else if (componentId == OPTION_3) {
				sendOptionsDialogue("Where would you like to go?", "Mining", "Smithing", "Construction", "Removed", "More Options");
				stage = 2;
			}
		} else if(stage == 69) {
			if(componentId == OPTION_1) {
				teleportPlayerGwd(3993, 6108, 1);
				//player.sm("<col=FF0000>PLEASE USE THE HOME TELEPORT USING TELEPORT CYRSTAL TO LEAVE RUNESPAN!");
			} else if(componentId == OPTION_2) {
				teleportPlayerGwd(4137, 6089, 1);
				//player.sm("<col=ff0000>Please use the ::home command to leave RuneSpan.");
			} else if(componentId == OPTION_3) {
				teleportPlayerGwd(4295, 6038, 1);
				//player.sm("<col=ff0000>Please use the ::home command to leave RuneSpan.");
			}
		}
	}

	private void teleportPlayer(int x, int y, int z) {
		player.setNextWorldTile(new WorldTile(x, y, z));
		player.stopAll();
	}
	
	private void teleportPlayerGwd(int x, int y, int z) {
		player.setNextWorldTile(new WorldTile(x, y, z));
		player.stopAll();
		player.getControlerManager().startControler("GodWars");
	}

	@Override
	public void finish() {

	}
}
