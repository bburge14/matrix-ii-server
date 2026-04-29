package com.rs.game.player.dialogues;

import com.rs.Settings;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.game.WorldTile;
import com.rs.game.minigames.CastleWars;
import com.rs.game.player.Skills;
import com.rs.game.player.content.Magic;
import com.rs.game.player.controlers.FightCaves;
import com.rs.game.player.controlers.FightKiln;

public class MrEx extends Dialogue {

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
						"Hello, I can teleport you to all the monsters around " + Settings.SERVER_NAME + ",",
						" would you like to see them?" }, IS_NPC, npcId, 9827);
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (stage == -1) {
			sendEntityDialogue(SEND_1_TEXT_CHAT,
					new String[] { player.getDisplayName(), "Sure, why not." },
					IS_PLAYER, player.getIndex(), 9827);
			stage = 1;
		} else if (stage == 1) {
			sendOptionsDialogue("Where would you like to go?", "Bosses", "Monsters",
					"Minigames", "Slayer Monsters", "Skilling");
			stage = 2;
		} else if (stage == 2) {
			if (componentId == OPTION_1) {
				sendOptionsDialogue("Where would you like to go?", "Nex", "General Graardor", "Kree'Arra", "K'ril Tsutsaroth", "~ Next Page ~");
				stage = 6;
			}
			if (componentId == OPTION_2) {
				sendOptionsDialogue("Where would you like to go?", "Tormented Demons", "Ganodermic Beasts", "Glacors", "Mithril Dragons", "~ Next Page ~");
				stage = 15;
			}
			if (componentId == OPTION_3) {
				sendOptionsDialogue("Where would you like to go?", "Barrows", "Fight Caves", "Fight Kiln", "Castle Wars", "~ Next Page ~");
				stage = 19;
			}
			if (componentId == OPTION_4) {
				sendOptionsDialogue("Where would you like to go?", "Slayer Tower", "Taverly Dungeon", "Fremmenik Slayer Dungeon", "Ancient Cavern", "~Next Page~", "Kuradel's Dungeon");
				stage = 21;
			}
			if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Mining", "Smithing", "Construction", "LavaFlow Mine", "~ Next Page ~");
				stage = 24;
			}
		} else if (stage == 6) {
			if (componentId == OPTION_1) {
				telePlayerGwd(2905, 5203, 0);// Nex
				player.getPackets().sendConfigByFile(15184, 0);
			}
			else if (componentId == OPTION_2)
				telePlayerGwd(2870, 5363, 0);// Bandos
			else if (componentId == OPTION_3)
				telePlayerGwd(2834, 5302, 0);// Armadyl
			else if (componentId == OPTION_4)
				telePlayerGwd(2925, 5330, 0);// Zamorak
			else if (componentId == OPTION_5)
				sendOptionsDialogue("Where would you like to go?", "Commander Zilyana", "King Black Dragon", "Queen Black Dragon", "Corporeal Beast", "~ Next Page ~");
				stage = 7;
		} else if (stage == 7) {
			if (componentId == OPTION_1)
				telePlayerGwd(2925, 5249, 0);// Saradomin
			else if (componentId == OPTION_2)
				teleportPlayer(2273, 4681, 0);//King Black Dragon
			else if (componentId == OPTION_3) {
				end();
				if (player.getSkills().getLevelForXp(Skills.SUMMONING) < 60) {
					player.getPackets().sendGameMessage("You need a summoning level of 60 to go through this portal.");
					return;
				}
				player.getControlerManager().startControler("QueenBlackDragonControler"); //Queen Black Dragon
			}
			else if (componentId == OPTION_4)
				teleportPlayer(2966, 4383, 2);//Corporeal Beast
			else if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Kalphite Queen", "Vorago", "Legio", "Kalphite King", "Close");
				stage = 9;
			}
		} else if (stage == 9) {
			if (componentId == OPTION_1)
				teleportPlayer(3486, 9509, 0);//Kalphite Queen
			else if (componentId == OPTION_2) {
				teleportPlayer(3545, 9507, 0);//Vorago
			} else if (componentId == OPTION_3) {
				teleportPlayer(6291, 6282, 0); //Legio
			} else if (componentId == OPTION_4) {
				teleportPlayer(3219, 2803, 0); //Kalphite King
			} else if (componentId == OPTION_5) {
				end();
			}
		} else if (stage == 15) {
			if (componentId == OPTION_1)
				teleportPlayer(2562, 5739, 0);// Tormented Demons
			else if (componentId == OPTION_2)
				teleportPlayer(4628, 5404, 0);// Ganodermic Beasts
			else if (componentId == OPTION_3)
				teleportPlayer(4192, 5717, 0);// Glacors
			else if (componentId == OPTION_4)
				teleportPlayer(1759, 5341, 1);// Mithril Dragons
			else if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Frost Dragons", "Dagannoth Kings", "~ Back to main page ~");
				stage = 17;
			}
		} else if (stage == 17) {
			if (componentId == OPTION_1)
				teleportPlayer(1299, 4508, 0);// Frost Dragon
		    else if (componentId == OPTION_2)
				teleportPlayer(1913, 4367, 0);// dks
			else if (componentId == OPTION_3) {
			sendOptionsDialogue("Where would you like to go?", "Bosses", "Monsters",
					"Minigames", "Slayer Monsters", "Skilling");
				stage = 2;
			}
		} else if (stage == 19) {
			if (componentId == OPTION_1)
				teleportPlayer(3565, 3289, 0);//Barrows
			else if (componentId == OPTION_2)
				Magic.sendNormalTeleportSpell(player, 0, 0, FightCaves.OUTSIDE);//Fight Caves
			else if (componentId == OPTION_3)
				Magic.sendNormalTeleportSpell(player, 0, 0, FightKiln.OUTSIDE);//Fight Kiln
			else if (componentId == OPTION_4)
				teleportPlayer(2440, 3088, 0);//Castle Wars
			else if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Crucible", "Clan Wars", "Close");
				stage = 855;
			}
		} else if (stage == 855) {
			if (componentId == OPTION_1)
				teleportPlayer(3120, 3519, 0);
			else if (componentId == OPTION_2)
				teleportPlayer(2992, 9679, 0);
			else if (componentId == OPTION_3)
				end();
		} else if (stage == 21) {
			if (componentId == OPTION_1) { // Slayer Tower
				teleportPlayer(2223, 3340, 0);
			} else if (componentId == OPTION_2) { //Taverly Dungeon
				teleportPlayer(2884, 9798, 0);
			} else if (componentId == OPTION_3) { // Fremmenik Slayer Dungeon
				teleportPlayer(2805, 10002, 0);
			} else if (componentId == OPTION_4) { // Ancient Cavern
				teleportPlayer(1776, 5363, 0);
			} else if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Mos Le'Harmless Dungeon", "Smoking Dungeon", "Brimhaven Dungeon", "Jadinko Dungeon", "Celestial Dragons");
				stage = 22;
			}
		} else if (stage == 22) {
			if (componentId == OPTION_1) { 
				teleportPlayer(3745, 9374, 0); // Mos Le'Harmless Dungeon
			} else if (componentId == OPTION_2) { 
				teleportPlayer(3211, 9378, 0); // Smoking Dungeon
			} else if (componentId == OPTION_3) {
				teleportPlayer(2646, 9554, 0); //Brimhaven Dungeon
			} else if (componentId == OPTION_4) {
				teleportPlayer(3012, 9274, 0); //Jadinko Dungeon
			} else if (componentId == OPTION_5) {
				teleportPlayer(1889, 4957, 2); //Celestial Dragons
			}
		} else if (stage == 24) {
			if (componentId == OPTION_1)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3297, 3298, 0));
			else if (componentId == OPTION_2)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2710, 3493, 0));
			else if (componentId == OPTION_3)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2884, 3503, 0));
			else if (componentId == OPTION_4) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2179, 5663, 0));
                end();
			} else if (componentId == OPTION_5) {
				stage = 25;
				sendOptionsDialogue("Where would you like to go?",
						"Hunter", "Agility (Gnome Agility)", "Woodcutting.",
						"Thieving", "More Options");
			}
		} else if(stage == 69) {
			if(componentId == OPTION_1) {
				teleportPlayer(3993, 6108, 1);
				player.sm("<col=FF0000>Please use the home teleport to leave RuneSpan.");
				player.getControlerManager().startControler("RunespanControler");
				player.getInterfaceManager().closeChatBoxInterface();
				
			} else if(componentId == OPTION_2) {
				teleportPlayer(4137, 6089, 1);
				player.sm("<col=FF0000>Please use the home teleport to leave RuneSpan.");
				player.getControlerManager().startControler("RunespanControler");
				player.getInterfaceManager().closeChatBoxInterface();
				
			} else if(componentId == OPTION_3) {
				teleportPlayer(4295, 6038, 1);
				player.sm("<col=FF0000>Please use the home teleport to leave RuneSpan.");
				player.getControlerManager().startControler("RunespanControler");
				player.getInterfaceManager().closeChatBoxInterface();
			}
		} else if (stage == 25) {
			if (componentId == OPTION_1) {
				sendOptionsDialogue("Where would you like to go?", "Birds", "Red Chinchompas", "~Back to Beginning~");
				stage = 26;
			} else if (componentId == OPTION_2)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2470, 3436, 0));
			else if (componentId == OPTION_3)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2724, 3485, 0));
			else if (componentId == OPTION_4)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2662, 3305, 0));
			else if (componentId == OPTION_5) {
				stage = 29;
				sendOptionsDialogue("Where would you like to go?", "Farming", "Fishing", "Higher Fishing", "Advanced Barb (agility)", "More Options");
			}
		} else if (stage == 29) {
			if (componentId == OPTION_1) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3052, 3304, 0));
			} else if (componentId == OPTION_2) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3088, 3230, 0));
			} else if (componentId == OPTION_3)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2843, 3433, 0));
			else if (componentId == OPTION_4) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2551, 3557, 0));
			} else if (componentId == OPTION_5) {
				sendOptionsDialogue("Where would you like to go?", "Abyss (Runecrafting)", "Dungeoneering", "Tree Farm", "Lrc highlevel mining", "Summoning");
				stage = 28;
			}
		} else if (stage == 28) {
			if (componentId == OPTION_1) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3040, 4836, 0));
			} else if (componentId == OPTION_2) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3450, 3727, 0));
			} else if (componentId == OPTION_3) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3004, 3378, 0));
			} else if (componentId == OPTION_4) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3652, 5121, 0));   
			} else if (componentId == OPTION_5) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2928, 3448, 0));
			}
		} else if (stage == 26) {
			if (componentId == OPTION_1)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2608, 2931, 0));
			else if (componentId == OPTION_2)
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2556, 2935, 0));
			else if (componentId == OPTION_3) {
				sendOptionsDialogue("Where would you like to go?", "Mining", "Smithing", "Construction", "RuneSpan", "More Options");
				stage = 24;
			}
		}
	}

	private void teleportPlayer(int x, int y, int z) {
		player.setNextWorldTile(new WorldTile(x, y, z));
		player.stopAll();
	}
	
	private void telePlayerGwd(int x, int y, int z) {
		player.setNextWorldTile(new WorldTile(x, y, z));
		player.stopAll();
		player.getControlerManager().startControler("GodWars");
	}

	@Override
	public void finish() {

	}
}
