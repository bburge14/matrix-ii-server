package com.rs.game.player.quests;

import com.rs.game.player.Player;
import com.rs.game.player.QuestManager.Quests;

public class QuestGuides {
	
	public static void handleQuestGuide(Player player, int slotId) {
		switch (slotId) {
		
			case 1: //Cooks Assistant
				if (!player.startedCooksAssistant) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendRunScript(1207, 3);
					player.getPackets().sendIComponentText(275, 1, "Cook's Assistant");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<col=330099>I can start this quest by speaking to the</col> <col=660000>cook</col> <col=330099>in the</col>");
					player.getPackets().sendIComponentText(275, 12, "<col=660000>kitchen</col> <col=330099>on the ground floor of</col> <col=660000>Lumbridge Castle.</col>");
					for (int i = 13; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				} 
				else if (player.completedCooksAssistantQuest) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendRunScript(1207, 12);
					player.getPackets().sendIComponentText(275, 1, "Cook's Assistant");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str><col=330099>I can start this quest by speaking to the</col> <col=660000>cook</col> <col=330099>in the</col>");
					player.getPackets().sendIComponentText(275, 12, "<str><col=660000>kitchen</col> <col=330099>on the ground floor of</col> <col=660000>Lumbridge Castle.</col>");
					player.getPackets().sendIComponentText(275, 13, "");
					player.getPackets().sendIComponentText(275, 14, "<str><col=330099>It's the</col> <col=660000>Duke of Lumbridge's</col> <col=330099>birthday and I have to help</col>");
					player.getPackets().sendIComponentText(275, 15, "<str><col=330099>his</col> <col=660000>Cook</col> <col=330099>make him a</col> <col=660000>birthday cake.</col> <col=330099>To do this I need to</col>");
					player.getPackets().sendIComponentText(275, 16, "<str><col=330099>bring him the following ingredients:</col>");
					player.getPackets().sendIComponentText(275, 17, "<str><col=330099>I have found a</col> <col=660000>bucket of milk</col><col=330099> to give the cook.</col>");
					player.getPackets().sendIComponentText(275, 18, "<str><col=330099>I have found a</col> <col=660000>pot of flour</col> <col=330099>to give the cook.</col>");
					player.getPackets().sendIComponentText(275, 19, "<str><col=330099>I have found an</col> <col=660000>egg</col> <col=330099>to give the cook.</col>");
					player.getPackets().sendIComponentText(275, 20, "");
					player.getPackets().sendIComponentText(275, 21, "<col=660000>QUEST COMPLETE</col>");	
				} 
				else {			
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendRunScript(1207, 10);
					player.getPackets().sendIComponentText(275, 1, "Cook's Assistant");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str><col=330099>I can start this quest by speaking to the</col> <col=660000>cook</col> <col=330099>in the</col>");
					player.getPackets().sendIComponentText(275, 12, "<str><col=660000>kitchen</col> <col=330099>on the ground floor of</col> <col=660000>Lumbridge Castle.</col>");
					player.getPackets().sendIComponentText(275, 13, "");
					player.getPackets().sendIComponentText(275, 14, "<col=330099>It's the</col> <col=660000>Duke of Lumbridge's</col> <col=330099>birthday and I have to help</col>");
					player.getPackets().sendIComponentText(275, 15, "<col=330099>his</col> <col=660000>Cook</col> <col=330099>make him a</col> <col=660000>birthday cake.</col> <col=330099>To do this I need to</col>");
					player.getPackets().sendIComponentText(275, 16, "<col=330099>bring him the following ingredients:</col>");
					player.getPackets().sendIComponentText(275, 17, (player.getInventory().containsItem(1927, 1) ? 
							"<str><col=330099>I have found a</col> <col=660000>bucket of milk</col> <col=330099>to give the cook.</col>"
							: "<col=330099>I need a</col> <col=660000>bucket of milk</col> <col=330099>to give the cook.</col>"));
					player.getPackets().sendIComponentText(275, 18, (player.getInventory().containsItem(1933, 1) ? 
							"<str><col=330099>I have found a</col> <col=660000>pot of flour</col> <col=330099>to give the cook.</col>"
							: "<col=330099>I need a</col> <col=660000>pot of flour</col> <col=330099>to give the cook.</col>"));
					player.getPackets().sendIComponentText(275, 19, (player.getInventory().containsItem(1944, 1) ? 
							"<str><col=330099>I have found an</col> <col=660000>egg</col> <col=330099>to give the cook.</col>"
							: "<col=330099>I need to find an</col> <col=660000>egg</col> <col=330099>to give the cook.</col>"));
					for (int i = 20; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				}
			break;
			
			case 2: //Demon Slayer
				player.getInterfaceManager().sendInterface(275);
				player.getPackets().sendIComponentText(275, 1, "Demon Slayer");
				player.getPackets().sendIComponentText(275, 10, "");
				player.getPackets().sendIComponentText(275, 11, "Talk to the Gypsy located in a tent around Varrock's centre.");
				for (int i = 12; i < 300; i++) {
					player.getPackets().sendIComponentText(275, i, "");
				}
			break;
			
			case 3: //Doric's Quest
				player.getInterfaceManager().sendInterface(275);
				player.getPackets().sendIComponentText(275, 1, "Doric's Quest");
				player.getPackets().sendIComponentText(275, 10, "");
				player.getPackets().sendIComponentText(275, 11, "The small house north of Falador with anvils inside, outside the ");
				player.getPackets().sendIComponentText(275, 12, "east gate of Taverley. Once inside, talk with Doric.");
				for (int i = 13; i < 300; i++) {
					player.getPackets().sendIComponentText(275, i, "");
				}
			break;
			
			case 4: //Dragon Slayer
				player.getInterfaceManager().sendInterface(275);
				player.getPackets().sendIComponentText(275, 1, "Dragon Slayer");
				player.getPackets().sendIComponentText(275, 10, "");
				player.getPackets().sendIComponentText(275, 11, "Talk to the Guildmaster, located inside the Champions' Guild.");
				for (int i = 12; i < 300; i++) {
					player.getPackets().sendIComponentText(275, i, "");
				}
			break; 

			case 5: //Ernest the Chicken
				player.getInterfaceManager().sendInterface(275);
				player.getPackets().sendIComponentText(275, 1, "Ernest the Chicken");
				player.getPackets().sendIComponentText(275, 10, "");
				player.getPackets().sendIComponentText(275, 11, "Speak to Veronica, north of Draynor Village");
				player.getPackets().sendIComponentText(275, 12, "and south of Draynor Manor.");
				for (int i = 13; i < 300; i++) {
					player.getPackets().sendIComponentText(275, i, "");
				}
			break;
			
			case 6: //Goblin Diplomacy
				player.getInterfaceManager().sendInterface(275);
				player.getPackets().sendIComponentText(275, 1, "Goblin Diplomacy");
				player.getPackets().sendIComponentText(275, 10, "");
				player.getPackets().sendIComponentText(275, 11, "Go to Goblin Village, and talk to General Bentnoze");
				player.getPackets().sendIComponentText(275, 12, "or General Wartface.");
				for (int i = 13; i < 300; i++) {
					player.getPackets().sendIComponentText(275, i, "");
				}
			break;
			
			case 7: //Imp catcher && DONE
				if (player.getQuestManager().getQuestStage(Quests.IMP_CATCHER) <= 0) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendRunScript(1207, 3);
					player.getPackets().sendIComponentText(275, 1, "Imp Catcher");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<col=330099>I can start this quest by speaking to </col> <col=660000>Wizard Mizgog</col> <col=330099>who is</col>");
					player.getPackets().sendIComponentText(275, 12, "<col=330099>in the</col> <col=660000>Wizard's Tower.</col>");
					player.getPackets().sendIComponentText(275, 13, "");
					player.getPackets().sendIComponentText(275, 14, "<col=330099>There aren't any requirements for this quest.</col>");
					for (int i = 15; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				}
				else if (player.getQuestManager().getQuestStage(Quests.IMP_CATCHER) == 1) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendRunScript(1207, 10);
					player.getPackets().sendIComponentText(275, 1, "Imp Catcher");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str><col=330099>I can start this quest by speaking to </col> <col=660000>Wizard Mizgog</col> <col=330099>who is</col>");
					player.getPackets().sendIComponentText(275, 12, "<str><col=330099>in the</col> <col=660000>Wizard's Tower.</col>");
					player.getPackets().sendIComponentText(275, 13, "");
					player.getPackets().sendIComponentText(275, 14, "<col=330099>I need to collect some items by killing</col> <col=660000>Imps</col>");
					player.getPackets().sendIComponentText(275, 15, (player.getInventory().containsItem(1474, 1) ? 
							"<str><col=660000>1 Black Bead</col>"
							: "<col=660000>1 Black Bead</col>"));
					player.getPackets().sendIComponentText(275, 16, (player.getInventory().containsItem(1470, 1) ? 
							"<str><col=660000>1 Red Bead</col>"
							: "<col=660000>1 Red Bead</col>"));
					player.getPackets().sendIComponentText(275, 17, (player.getInventory().containsItem(1476, 1) ? 
							"<str><col=660000>1 White Bead</col>"
							: "<col=660000>1 White Bead</col>"));
					player.getPackets().sendIComponentText(275, 18, (player.getInventory().containsItem(1472, 1) ? 
							"<str><col=660000>1 Yellow Bead</col>"
							: "<col=660000>1 Yellow Bead</col>"));	
					for (int i = 19; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				} else if (player.getQuestManager().completedQuest(Quests.IMP_CATCHER)) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendRunScript(1207, 12);
					player.getPackets().sendIComponentText(275, 1, "Imp Catcher");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str>I've spoken to Wizard Mizgog.</col>");
					player.getPackets().sendIComponentText(275, 12, "");
					player.getPackets().sendIComponentText(275, 13, "<str>I have collected all the beads.</col>");
					player.getPackets().sendIComponentText(275, 14, "");
					player.getPackets().sendIComponentText(275, 15, "<str>Wizard Mizgog thanked me for finding his beads and gave</col>");
					player.getPackets().sendIComponentText(275, 16, "<str>me an Amulet of Accuracy</col>");
					player.getPackets().sendIComponentText(275, 17, "");
					player.getPackets().sendIComponentText(275, 18, "<col=660000>QUEST COMPLETE</col>");
					for (int i = 19; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				}
			break;
			
			case 8:
				//The knights sword
			break;
			
			case 9:
				//pirates tresure
			break;
			
			case 10:
				//prince ali rescue
			break;
		
			case 11: //The Restless Ghost
				if (player.getQuestManager().getQuestStage(Quests.THE_RESTLESS_GHOST) <= 0) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendIComponentText(275, 1, "The Restless Ghost");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "I can start this quest by speaking to the <col=660000>priest</col> in <col=660000>Lumbridge church</col>.");
					player.getPackets().sendIComponentText(275, 12, "");
					player.getPackets().sendIComponentText(275, 13, "Quest Requirements:");
					player.getPackets().sendIComponentText(275, 14, "I can't be afraid of a level 13 <col=660000>skeleton</col>.");
					for (int i = 15; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				}
				else if (player.getQuestManager().getQuestStage(Quests.THE_RESTLESS_GHOST) == 1) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendIComponentText(275, 1, "The Restless Ghost");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str>I can start this quest by speaking to the <col=660000>priest</col> in <col=660000>Lumbridge church</col>.</str>");
					player.getPackets().sendIComponentText(275, 12, "");
					player.getPackets().sendIComponentText(275, 13, "I've spoke to Father Aereck, he said there is a");
					player.getPackets().sendIComponentText(275, 14, "ghost haunting Lumbridge graveyard.");
					player.getPackets().sendIComponentText(275, 15, "He needs me to speak to Father Urhney located in a");
					player.getPackets().sendIComponentText(275, 16, "house south of Lumbridge swamp.");
					for (int i = 17; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				} 
				else if (player.getQuestManager().getQuestStage(Quests.THE_RESTLESS_GHOST) == 2) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendIComponentText(275, 1, "The Restless Ghost");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str>I can start this quest by speaking to the <col=660000>priest</col> in <col=660000>Lumbridge church</col>.</str>");
					player.getPackets().sendIComponentText(275, 12, "");
					player.getPackets().sendIComponentText(275, 13, "<str>I've spoke to Father Aereck, he said there is a ghost haunting Lumbridge graveyard.</str>");
					player.getPackets().sendIComponentText(275, 14, "<str>He needs me to speak to Father Urhney located in a house south of Lumbridge swamp.</str>");
					for (int i = 15; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				}
				else if (player.getQuestManager().getQuestStage(Quests.THE_RESTLESS_GHOST) == 3) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendIComponentText(275, 1, "The Restless Ghost");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str>I can start this quest by speaking to the <col=660000>priest</col> in <col=660000>Lumbridge church</col>.</str>");
					player.getPackets().sendIComponentText(275, 12, "");
					player.getPackets().sendIComponentText(275, 13, "<str>I've spoke to Father Aereck, he said there is a ghost haunting Lumbridge graveyard.</str>");
					player.getPackets().sendIComponentText(275, 14, "<str>He needs me to speak to Father Urhney located in a house south of Lumbridge swamp.</str>");
					player.getPackets().sendIComponentText(275, 15, "");
					player.getPackets().sendIComponentText(275, 16, "Father Urhney has given me a Amulet of Ghostspeak, this will allow me");
					player.getPackets().sendIComponentText(275, 17, "to talk to the Ghost in Lumbridge graveyard.");
					for (int i = 18; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				}
				else if (player.getQuestManager().getQuestStage(Quests.THE_RESTLESS_GHOST) == 4) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendIComponentText(275, 1, "The Restless Ghost");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str>I can start this quest by speaking to the <col=660000>priest</col> in <col=660000>Lumbridge church</col>.</str>");
					player.getPackets().sendIComponentText(275, 12, "");
					player.getPackets().sendIComponentText(275, 13, "<str>I've spoke to Father Aereck, he said there is a ghost haunting Lumbridge graveyard.</str>");
					player.getPackets().sendIComponentText(275, 14, "<str>He needs me to speak to Father Urhney located in a house south of Lumbridge swamp.</str>");
					player.getPackets().sendIComponentText(275, 15, "");
					player.getPackets().sendIComponentText(275, 16, "<str>Father Urhney has given me a Amulet of Ghostspeak, this will allow me</str>");
					player.getPackets().sendIComponentText(275, 17, "<str>to talk to the Ghost in Lumbridge graveyard.</str>");
					player.getPackets().sendIComponentText(275, 18, "");
					player.getPackets().sendIComponentText(275, 19, "I've talked to the Ghost and he has told me he is missing his skull and");
					player.getPackets().sendIComponentText(275, 20, "he wants me to retrieve it for him. He says last remembers being");
					player.getPackets().sendIComponentText(275, 21, "attacked by a warlock while mining south of here.");
					player.getPackets().sendIComponentText(275, 22, "");
					player.getPackets().sendIComponentText(275, 23, (player.getInventory().containsItem(553, 1) ? "I've retrieved the Ghosts skull, I should bring it back to him." : "I need to get the Ghost's skull back."));
					for (int i = 24; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				}
				else if (player.getQuestManager().completedQuest(Quests.THE_RESTLESS_GHOST)) {
					player.getInterfaceManager().sendInterface(275);
					player.getPackets().sendIComponentText(275, 1, "The Restless Ghost");
					player.getPackets().sendIComponentText(275, 10, "");
					player.getPackets().sendIComponentText(275, 11, "<str>I can start this quest by speaking to the <col=660000>priest</col> in <col=660000>Lumbridge church</col>.</str>");
					player.getPackets().sendIComponentText(275, 12, "");
					player.getPackets().sendIComponentText(275, 13, "<str>I've spoke to Father Aereck, he said there is a ghost haunting Lumbridge graveyard.</str>");
					player.getPackets().sendIComponentText(275, 14, "<str>He needs me to speak to Father Urhney located in a house south of Lumbridge swamp.</str>");
					player.getPackets().sendIComponentText(275, 15, "");
					player.getPackets().sendIComponentText(275, 16, "<str>Father Urhney has given me a Amulet of Ghostspeak, this will allow me</str>");
					player.getPackets().sendIComponentText(275, 17, "<str>to talk to the Ghost in Lumbridge graveyard.</str>");
					player.getPackets().sendIComponentText(275, 18, "");
					player.getPackets().sendIComponentText(275, 19, "<str>I've talked to the Ghost and he has told me he is missing his skull and</str>");
					player.getPackets().sendIComponentText(275, 20, "<str>he wants me to retrieve it for him. He says last remembers being</str>");
					player.getPackets().sendIComponentText(275, 21, "<str>attacked by a warlock while mining south of here.</str>");
					player.getPackets().sendIComponentText(275, 22, "");
					player.getPackets().sendIComponentText(275, 23, "I've returned the Ghost's skull to his coffin.");
					player.getPackets().sendIComponentText(275, 24, "");
					player.getPackets().sendIComponentText(275, 25, "<col=660000>QUEST COMPLETE<col>");
					for (int i = 26; i < 300; i++) {
						player.getPackets().sendIComponentText(275, i, "");
					}
				}
			break;
			
			default:
				return;
		}
	}

}
