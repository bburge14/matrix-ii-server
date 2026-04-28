package com.rs.net.decoders.handlers;

import com.rs.game.Graphics;
import com.rs.Settings;
import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.ForceTalk;
import com.rs.game.World;
import com.rs.game.WorldObject;
//import com.rs.game.minigames.ectofuntus.Ectofuntus;
import com.rs.game.npc.NPC;
import com.rs.game.npc.familiar.Familiar;
import com.rs.game.npc.others.FireSpirit;
import com.rs.game.npc.others.LivingRock;
import com.rs.game.npc.pet.Pet;
import com.rs.game.npc.slayer.Strykewyrm;
import com.rs.game.player.CoordsEvent;
import com.rs.game.player.Player;
import com.rs.game.player.actions.Fishing;
import com.rs.game.player.actions.Listen;
import com.rs.game.player.actions.Fishing.FishingSpots;
import com.rs.game.player.actions.divination.HarvestWisp;
import com.rs.game.player.content.SheepShearing;
import com.rs.game.player.actions.mining.LivingMineralMining;
import com.rs.game.player.content.Hunter;
import com.rs.game.player.content.LividFarm;
import com.rs.game.player.content.PenguinEvent;
import com.rs.game.player.content.magic.Magic;
import com.rs.game.player.actions.mining.MiningBase;
import com.rs.game.player.actions.runecrafting.SiphonActionCreatures;
import com.rs.game.player.actions.thieving.PickPocketAction;
import com.rs.game.player.actions.thieving.PickPocketableNPC;
import com.rs.game.player.actions.thieving.PrifddinasPickpocketing;
import com.rs.game.player.content.PlayerLook;
import com.rs.game.player.content.dungeoneering.DungeonRewards;
import com.rs.game.player.content.quests.SwordOfWiseman;
import com.rs.game.player.dialogues.alkharid.FremennikShipmaster;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.io.InputStream;
import com.rs.utils.Logger;
import com.rs.utils.Misc;
import com.rs.utils.NPCExamines;
import com.rs.utils.ShopsHandler;
import com.rs.game.npc.others.WildyWyrm;
import com.rs.game.WorldTile;
import com.rs.game.player.SlayerManager;
import com.rs.game.player.QuestManager.Quests;
import com.rs.game.player.content.Slayer.SlayerMaster;
import com.rs.game.player.content.XPWell;
import com.rs.game.npc.others.ConditionalDeath;
import com.rs.game.npc.others.GraveStone;
import com.rs.game.player.content.ItemSets;
import com.rs.game.player.controlers.SorceressGarden;
import com.rs.game.minigames.CastleWars;
import com.rs.game.minigames.pest.CommendationExchange;
import com.rs.game.npc.others.MutatedZygomites;

public class NPCHandler {

    public static void handleExamine(final Player player, InputStream stream) {
	int npcIndex = stream.readUnsignedShort128();
	boolean forceRun = stream.read128Byte() == 1;
	if (forceRun)
	    player.setRun(forceRun);
	final NPC npc = World.getNPCs().get(npcIndex);
	if (npc == null || npc.hasFinished() || !player.getMapRegionsIds().contains(npc.getRegionId()))
	    return;
		player.getPackets().sendNPCMessage(0, npc, "NPC Info: " + npc.getDefinitions().name + ". <col=ff0000> Hitpoints: " + npc.getHitpoints() + "/" + npc.getMaxHitpoints());
	if (player.getRights() == 2)
		player.getPackets().sendGameMessage("NPC Info: " + npc + ", ConfigByFile: null");
    }

	
	public static void handleOption1(final Player player, InputStream stream) {
		int npcIndex = stream.readUnsignedShort128();
		boolean forceRun = stream.read128Byte() == 1;
		final NPC npc = World.getNPCs().get(npcIndex);
		if (npc == null || npc.isCantInteract() || npc.isDead()
				|| !player.getMapRegionsIds().contains(npc.getRegionId()))
			return;
		player.stopAll(false);
		if(forceRun)
			player.setRun(forceRun);
		if (npc.getId() == 745) {
			player.faceEntity(npc);
			if (!player.withinDistance(npc, 4))
				return;
			npc.faceEntity(player);
			player.getDialogueManager().startDialogue("Wormbrain", npc.getId());
			return;
		}
		switch (npc.getId()) {
			case 20312: // Ithell
			case 20313:
			case 20314:
			case 20315:
			case 20328:
	
			case 20316: // Amlodd
			case 20317:
			case 20318:
			case 20319:
	
			case 20320: // Hefin
			case 20321:
			case 20322:
			case 20323:
	
			case 20324: // Meilyr
			case 20325:
			case 20326:
			case 20327:
	
			case 20113: // Iorwerth
			case 20114:
			case 20115:
			case 20116:
	
			case 20125: // Trahaearn
			case 20126:
			case 20127:
			case 20128:
	
			case 20121: // Cadarn
			case 20122:
			case 20123:
			case 20124:
	
			case 20117: // Crwys
			case 20118:
			case 20119:
			case 20120:
				player.getActionManager().setAction(new PrifddinasPickpocketing(npc));
			break;
		}
		if (npc.getDefinitions().name.contains("Banker")
				|| npc.getDefinitions().name.contains("banker")) {
			player.faceEntity(npc);
			if (!player.withinDistance(npc, 5))
				return;
			npc.faceEntity(player);
			player.getDialogueManager().startDialogue("Banker", npc.getId());
			return;
		}
		
		if (npc.getDefinitions().name.toLowerCase().equals("grand exchange clerk")) {
            if (player.isIronMan())
                return;
		    player.faceEntity(npc);
		    if (!player.withinDistance(npc, 2))
				return;
		    npc.faceEntity(player);
		    player.getDialogueManager().startDialogue("GrandExchange", npc.getId());
		    return;
		}
		if (npc.getDefinitions().name.contains("Circus")
				|| npc.getDefinitions().name.contains("circus")) {
			player.faceEntity(npc);
			if (!player.withinDistance(npc, 2))
				return;
			npc.faceEntity(player);
			player.getPackets().sendGameMessage("The circus is not at " + Settings.SERVER_NAME + " currently, sorry!");
			return;
		}
		if (npc.getDefinitions().name.contains("Death")
				|| npc.getDefinitions().name.contains("Death")) {
			player.faceEntity(npc);
			if (!player.withinDistance(npc, 2))
				return;
			npc.faceEntity(player);
			player.getDialogueManager().startDialogue("DeathTaskMaster", 18517);
			//player.getPackets().sendGameMessage("test");
			return;
		}
		if(SiphonActionCreatures.siphon(player, npc)) 
			return;
		// DIVINATION \\
		else if (npc.getId() == 18173) { // pale spring
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18174) { // FLICKERING_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18176) { // BRIGHT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18178) { //GLOWING_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18180) { // SPARKLING_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18182) { // GLEAMING_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18184) { //VIBRANT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18186) { // LUSTROUS_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 13616) { // ELDER_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18188) { // BRILLIANT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18190) { // RADIANT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18192) { // LUMINOUS_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18194) { // INCANDESCENT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18175) { // ENRICHED_FLICKERING_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18177) { // ENRICHED_BRIGHT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18179) { // ENRICHED_GLOWING_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18181) { // ENRICHED_SPARKLING_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18183) { // ENRICHED_GLEAMING_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18185) { // ENRICHED_VIBRANT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18187) { // ENRICHED_LUSTROUS_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 13627) { // ENRICHED_ELDER_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18189) { // ENRICHED_BRILLIANT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18191) { // ENRICHED_RADIANT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18193) { // ENRICHED_LUMINOUS_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18195) { // ENRICHED_INCANDESCENT_SPRING
			HarvestWisp.siphon(player, npc);
		}
		else if (npc.getId() == 18150) { // PALE_WISP
			HarvestWisp.beginharvest(player, npc, 18173);
		}
		else if (npc.getId() == 18151) { // FLICKERING_WISP
			HarvestWisp.beginharvest(player, npc, 18174);
		}
		else if (npc.getId() == 18153) { // BRIGHT_WISP
			HarvestWisp.beginharvest(player, npc, 18176);
		}
		else if (npc.getId() == 18155) { // GLOWING_WISP
			HarvestWisp.beginharvest(player, npc, 18178);
		}
		else if (npc.getId() == 18157) { // SPARKLING_WISP
			HarvestWisp.beginharvest(player, npc, 18180);
		}
		else if (npc.getId() == 18159) { // GLEAMING_WISP
			HarvestWisp.beginharvest(player, npc, 18182);
		}
		else if (npc.getId() == 18161) { // VIBRANT_WISP
			HarvestWisp.beginharvest(player, npc, 18184);
		}
		else if (npc.getId() == 18163) { // LUSTROUS_WISP
			HarvestWisp.beginharvest(player, npc, 18186);
		}
		else if (npc.getId() == 13614) { // ELDER_WISP
			HarvestWisp.beginharvest(player, npc, 13616);
		}
		else if (npc.getId() == 18165) { // BRILLIANT_WISP
			HarvestWisp.beginharvest(player, npc, 18188);
		}
		else if (npc.getId() == 18167) { // RADIANT_WISP
			HarvestWisp.beginharvest(player, npc, 18190);
		}
		else if (npc.getId() == 18169) { // LUMINOUS_WISP
			HarvestWisp.beginharvest(player, npc, 18192);
		}
		else if (npc.getId() == 18171) { // INCANDESCENT_WISP
			HarvestWisp.beginharvest(player, npc, 18194);
		}
		else if (npc.getId() == 18171) { // ENRICHED_PALE_WISP
			HarvestWisp.beginharvest(player, npc, 18194);
		}
		else if (npc.getId() == 18152) { // ENRICHED_FLICKERING_WISP
			HarvestWisp.beginharvest(player, npc, 18175);
		}
		else if (npc.getId() == 18154) { // ENRICHED_BRIGHT_WISP
			HarvestWisp.beginharvest(player, npc, 18177);
		}
		else if (npc.getId() == 18156) { // ENRICHED_GLOWING_WISP
			HarvestWisp.beginharvest(player, npc, 18179);
		}
		else if (npc.getId() == 18158) { // ENRICHED_SPARKLING_WISP
			HarvestWisp.beginharvest(player, npc, 18181);
		}
		else if (npc.getId() == 18160) { // ENRICHED_GLEAMING_WISP
			HarvestWisp.beginharvest(player, npc, 18183);
		}
		else if (npc.getId() == 18162) { // ENRICHED_VIBRANT_WISP
			HarvestWisp.beginharvest(player, npc, 18185);
		}
		else if (npc.getId() == 18164) { // ENRICHED_LUSTROUS_WISP
			HarvestWisp.beginharvest(player, npc, 18187);
		}
		else if (npc.getId() == 18171) { // ENRICHED_ELDER_WISP
			HarvestWisp.beginharvest(player, npc, 13627);
		}
		else if (npc.getId() == 18166) { // ENRICHED_BRILLIANT_WISP
			HarvestWisp.beginharvest(player, npc, 18189);
		}
		else if (npc.getId() == 18168) { // ENRICHED_RADIANT_WISP
			HarvestWisp.beginharvest(player, npc, 18191);
		}
		else if (npc.getId() == 18170) { // ENIRCHED_LUMINOUS_WISP
			HarvestWisp.beginharvest(player, npc, 18193);
		}
		else if (npc.getId() == 18172) { // ENRICHED_INCANDESCENT_WISP
			HarvestWisp.beginharvest(player, npc, 18195);
		}
		// END OF DIVINATION \\
		player.setCoordsEvent(new CoordsEvent(npc, new Runnable() {
			@Override
			public void run() {
				npc.resetWalkSteps();
				player.faceEntity(npc);
				
				if (npc.getId() == 6601) {
					return;
				}
				
				if (!player.getControlerManager().processNPCClick1(npc))
					return;
				FishingSpots spot = FishingSpots.forId(npc.getId() | 1 << 24);
				if (spot != null) {
					player.getActionManager().setAction(new Fishing(spot, npc));
					return; // its a spot, they wont face us
				}else if (npc.getId() >= 8837 && npc.getId() <= 8839) {
					player.getActionManager().setAction(new LivingMineralMining((LivingRock) npc));
					return;
				} else if (npc instanceof GraveStone) {
				    GraveStone grave = (GraveStone) npc;
				    grave.sendGraveInscription(player);
				    return;
				}
				npc.faceEntity(player);
				if (npc.getId() == 3709)
					player.getInterfaceManager().openTeleportInterface(player, 1);
				if (npc.getId() == 3006)
					player.getDialogueManager().startDialogue("spiritshard",
							npc.getId());			
				if (npc.getId() == 15582)
					player.getDialogueManager().startDialogue("Pkstores",
							npc.getId());			
				if (npc.getId() == 1283)
					player.getDialogueManager().startDialogue("Max", npc.getId());
				if (npc.getId() == 9159)
					player.getDialogueManager().startDialogue("Allshops", npc.getId());	
				if (npc.getId() == 1)
					player.getDialogueManager().startDialogue("BankTest", npc.getId());
				if (npc.getId() == 1513)
					PlayerLook.openMageMakeOver(player);								
				else if (npc.getId() == 5563)
					player.getDialogueManager().startDialogue("SorceressGardenNPCs", npc);
				else if (npc.getId() == 208)
					player.getDialogueManager().startDialogue("Lawgof");
				else if (npc.getId() == 2340)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 41);
                    }
				else if (npc.getId() == 209)
					player.getDialogueManager().startDialogue("Nulodion");
                else if (npc.getId() == 2932)
                    player.getDialogueManager().startDialogue("ShipManager");
				else if (npc.getId() == 5559) 
					player.sendDeath(npc);
				else if (npc.getId() == 15451 && npc instanceof FireSpirit) {
					FireSpirit spirit = (FireSpirit) npc;
					spirit.giveReward(player);
				}
				else if (npc.getId() >= 1 && npc.getId() <= 6 || npc.getId() >= 7875 && npc.getId() <= 7884)
					player.getDialogueManager().startDialogue("Man", npc.getId());
				else if (npc.getId() == 198)
					player.getDialogueManager().startDialogue("Guildmaster", npc.getId());
				else if (npc.getId() == 9462)
					Strykewyrm.handleStomping(player, npc);
				else if (npc.getId() == 9707)
					player.getDialogueManager().startDialogue("FremennikShipmaster", npc.getId(), true);
				else if (npc.getId() == 9708)
					player.getDialogueManager().startDialogue("FremennikShipmaster", npc.getId(), false);
				else if (npc.getId() == 11270)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 19);
                    }
				else if (npc.getId() == 519)
                    if (player.isIronMan()) {
                        
                    } else {
					    ShopsHandler.openShop(player, 2);
                    }
				else if (npc.getId() == 550)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 14);
                    }
				else if (npc.getId() == 11475)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 9);
                    }
				else if (npc.getId() == 546)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 10);
                    }
				else if (npc.getId() == 585 || npc.getId() == 530 || npc.getId() == 531)
                    if (player.isIronMan()) {
                        ShopsHandler.openShop(player, 150);
                    } else {
					    ShopsHandler.openShop(player, 15);
                    }
				else if (npc.getId() == 538)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 6);
                    }
				else if (npc.getId() == 551)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 13);
                    }
				else if (npc.getId() == 15549)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 74);	
                    }
				else if (npc.getId() == 15147)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 63);
                    }
                else if (npc.getId() == 7530) {
                    player.getDialogueManager().startDialogue("SimpleNPCMessage", 7530, "Here you are! Now get started.");
                    player.getInventory().addItem(6950, 1);
                    player.sm("The prostitute hands you a magic orb. Who knows where its been..");
                }
				else if (npc.getId() == 7569)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 64);
                    }		
				else if (npc.getId() == 3381)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 54);
                    }
				else if (npc.getId() == 789)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 51);
                    }
				else if (npc.getId() == 6539)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 84);
                    }
				else if (npc.getId() == 13482)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 49);
                    }
				else if (npc.getId() == 556)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 50);
                    }	
				else if (npc.getId() == 3122)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 100);
                    }
				else if (npc.getId() == 422)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 47);
                    }
				else if (npc.getId() == 15469)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 22);
                    }
				else if (npc.getId() == 9713) {
				    ShopsHandler.openShop(player, 85);
                }
				else if (npc.getId() == 11226) {
					player.getDialogueManager().startDialogue("DungLeaving");
					player.lock(3);
				}
				else if (npc.getId() == 2620)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 53);
                    }
				else if (npc.getId() == 6988)
					player.getDialogueManager().startDialogue("SummoningShop");
				else if (npc.getId() == 6970)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 26);	
                    }	
				else if (npc.getId() == 100)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 45);
                    }
				else if (npc.getId() == 9102)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 46);
                    }
				else if (npc.getId() == 2323)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 42);
                    }
				else if (npc.getId() == 560)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 66);
                    }
				else if (npc.getId() == 598)
					player.getDialogueManager().startDialogue("Hairdresser", npc.getId());
				else if (npc.getId() == 548)
					player.getDialogueManager().startDialogue("Thessalia", npc.getId());
				else if (npc.getId() == 8091)
					player.getDialogueManager().startDialogue("StarSprite");
				else if (npc.getId() == 4243)
					player.getDialogueManager().startDialogue("Butler", npc.getId());
				else if (npc.getId() == 2191)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 41);
                    }
				else if (npc.getId() == 1526)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 60);
                    }
				else if (npc.getId() == 456) {
					if (player.RG <= 2)
					player.getDialogueManager().startDialogue("FatherAereck1", npc.getId());
					else if (player.RG >= 3 && player.RG <= 5)
					player.getDialogueManager().startDialogue("FatherAereck2", npc.getId());
					else
					player.getDialogueManager().startDialogue("FatherAereck", npc.getId());
				} else if (npc.getId() == 457) {
					if (player.getQuestManager().getQuestStage(Quests.THE_RESTLESS_GHOST) == 3) {
						if (player.getEquipment().getAmuletId() == 552)
							player.getDialogueManager().startDialogue("Ghost", npc.getId());
						else
							player.getDialogueManager().startDialogue("GhostWo", npc.getId());
					} 
					else if (player.getQuestManager().getQuestStage(Quests.THE_RESTLESS_GHOST) == 4) {
						player.getDialogueManager().startDialogue("GhostFind", npc.getId());
					} 
					else {
						player.getPackets().sendGameMessage("The ghost does not seem interested in you.");
					}
				}
				else if (npc.getId() == 1916)
					player.getPackets().sendGameMessage("My mom told me I shouldn't talk to vampyres...");
				else if (npc.getId() == 13172 || npc.getId() == 13173)
					player.getPackets().sendGameMessage("I heard that Leela is a spy, maybe I shouldn't interact with her...");
				 //Halloween event
				else if (npc.getId() == 12377)
					  player.getDialogueManager().startDialogue("PumpkinPete", npc.getId());
				else if (npc.getId() == 12378)
					  player.getDialogueManager().startDialogue("PumpkinPete2", npc.getId());	
				else if (npc.getId() == 12375 && player.cake == 0)
					  player.getDialogueManager().startDialogue("Zabeth", npc.getId());
				else if (npc.getId() == 12375 && player.drink == 0)
					  player.getDialogueManager().startDialogue("Zabeth2", npc.getId());	
				else if (npc.getId() == 12375 && player.drink == 1)
					  player.getDialogueManager().startDialogue("Zabeth3", npc.getId());
				else if (npc.getId() == 12379 && player.drink == 0) {
					if (player.talked == 0)
						player.getPackets().sendGameMessage("The Grim Reaper isn't interested in you at the moment.");
					else
					  player.getDialogueManager().startDialogue("GrimReaper", npc.getId());
				} else if (npc.getId() == 12379 && player.dust1 == 0)
					  player.getDialogueManager().startDialogue("GrimReaper2", npc.getId());
				else if (npc.getId() == 12379 && player.dust1 == 1 && player.dust2 == 1 && player.dust3 == 1)
					  player.getDialogueManager().startDialogue("GrimReaper3", npc.getId());
				else if (npc.getId() == 12375 && player.doneevent == 1)
					  player.getDialogueManager().startDialogue("PumpkinPete2", npc.getId());
				else if (npc.getId() == 4250)
					player.getDialogueManager().startDialogue("SawMillOperator", npc.getId());
				else if (npc.getId() == 12379 && player.doneevent == 1)
					  player.getDialogueManager().startDialogue("PumpkinPete2", npc.getId());
				else if (npc.getId() == 12392)
					  player.getDialogueManager().startDialogue("PumpkinPete2", npc.getId());
				else if (npc.getId() == 8266)
				    player.getDialogueManager().startDialogue("Ghommel");
				//
				else if (npc.getId() == 2237)
					player.getPackets().sendGameMessage("The annoyed farmer does not bother with you. For some reason he is in a bad mood.");
				else if (npc.getId() == 13942)
					player.getPackets().sendGameMessage("Heroes are part of a future update.");
				else if (npc.getId() == 4585)
					player.getPackets().sendGameMessage("The gnome is too caught up in his studies to pay attention to you.");
				else if (npc.getId() == 706)
					player.getDialogueManager().startDialogue("WizardMizgog", npc.getId());
				else if (npc.getId() == 458)
					player.getDialogueManager().startDialogue("FatherUrhney");
				else if (npc.getId() == 300)
					player.getDialogueManager().startDialogue("Sedridor", npc);
				else if (npc.getId() == 278)
					player.getDialogueManager().startDialogue("LumbridgeCook", npc.getId());
				else if (npc.getId() == 198)
					player.getDialogueManager().startDialogue("GuildMaster", npc.getId());
				else if (npc.getId() == 755)
					player.getDialogueManager().startDialogue("Morgan", npc.getId());
				else if (npc.getId() == 747)
					player.getDialogueManager().startDialogue("Oziach", npc.getId());
				else if (npc.getId() == 15907)
				    player.getDialogueManager().startDialogue("OsmanDialogue", npc.getId());
				else if (npc.getId() == 746)
					player.getDialogueManager().startDialogue("Oracle", npc.getId());
				else if (npc.getId() == 918)
					player.getDialogueManager().startDialogue("Ned1", npc.getId());
				else if (npc.getId() == 4475)
					player.getDialogueManager().startDialogue("Ned2", npc.getId());
				else if (npc.getId() == 583 || npc.getId() == 9395)
					player.getDialogueManager().startDialogue("Betty", npc.getId());
				else if (npc.getId() == 285)
					player.getDialogueManager().startDialogue("Veronica", npc.getId());
				else if (npc.getId() == 3705)
					ShopsHandler.openShop(player, 152);
				else if (npc.getId() == 654)
					player.getDialogueManager().startDialogue("Shamus", npc.getId());
				else if (npc.getId() == 650)
					player.getDialogueManager().startDialogue("Warrior", npc.getId());
				else if (npc.getId() == 2729)
					player.getDialogueManager().startDialogue("MonkOfEntrana", npc.getId());
				else if (npc.getId() == 579)
					player.getDialogueManager().startDialogue("DrogoDwarf", npc.getId());
				else if (npc.getId() == 13280) //Crossbow Shop
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 28);
                    }
				else if (npc.getId() == 3000) //Skillcape Shop
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 8);
                    }
				else if (npc.getId() == 582) //dwarves general store
					player.getDialogueManager().startDialogue("GeneralStore", npc.getId(), 31);
                else if (npc.getId() == 529)
                    if (player.isIronMan()) {
                        ShopsHandler.openShop(player, 151);
                    } else {
                        ShopsHandler.openShop(player, 1);
                    }
				else if (npc.getId() == 522 || npc.getId() == 523) //varrock
					player.getDialogueManager().startDialogue("GeneralStore", npc.getId(), 8);
				else if (npc.getId() == 520 || npc.getId() == 521) //lumbridge
					player.getDialogueManager().startDialogue("GeneralStore", npc.getId(), 4);
				else if (npc.getId() == 594)
					player.getDialogueManager().startDialogue("Nurmof", npc.getId());
				else if (npc.getId() == 665)
					player.getDialogueManager().startDialogue("BootDwarf", npc.getId());
				else if (npc.getId() == 382 || npc.getId() == 3294 || npc.getId() == 4316)
					player.getDialogueManager().startDialogue("MiningGuildDwarf", npc.getId(), false);
				else if (npc.getId() == 3295)
					player.getDialogueManager().startDialogue("MiningGuildDwarf", npc.getId(), true);
				else if (npc.getId() == 537)
					player.getDialogueManager().startDialogue("Scavvo", npc.getId());
				else if (npc.getId() == 536)
					player.getDialogueManager().startDialogue("Valaine", npc.getId());
				else if (npc.getId() == 4563) //Crossbow Shop
					player.getDialogueManager().startDialogue("Hura", npc.getId());
				else if (npc.getId() == 2617)
					player.getDialogueManager().startDialogue("TzHaarMejJal", npc.getId());
				else if (npc.getId() == 2618)
					player.getDialogueManager().startDialogue("TzHaarMejKah", npc.getId());
				else if(npc.getId() == 15149)
					player.getDialogueManager().startDialogue("MasterOfFear", 0);
				else if (npc.getId() == 4247)
					player.getDialogueManager().startDialogue("EstateAgent", npc.getId());
				else if (npc instanceof Pet) {
					Pet pet = (Pet) npc;
					if (pet != player.getPet()) {
						player.getPackets().sendGameMessage("This isn't your pet.");
						return;
					}
					player.setNextAnimation(new Animation(827));
					pet.pickup();
				}
				else {
					//player.getPackets().sendGameMessage(
							//"Nothing interesting happens.");
					if (Settings.DEBUG) {
						System.out.println("cliked 1 at npc id : "
								+ npc.getId() + ", " + npc.getX() + ", "
								+ npc.getY() + ", " + npc.getPlane());
						Logger.logMessage("cliked 1 at npc id : "
								+ npc.getId() + ", " + npc.getX() + ", "
								+ npc.getY() + ", " + npc.getPlane());
					}
				}
			}
		}, npc.getSize()));
	
	}
	public static void handleOption2(final Player player, InputStream stream) {
		int npcIndex = stream.readUnsignedShort128();
		boolean forceRun = stream.read128Byte() == 1;
		final NPC npc = World.getNPCs().get(npcIndex);
		if (npc == null || npc.isCantInteract() || npc.isDead()
				|| npc.hasFinished()
				|| !player.getMapRegionsIds().contains(npc.getRegionId()))
			return;
		player.stopAll(false);
		if(forceRun)
			player.setRun(forceRun);
		if (npc.getId() == 745) {
			player.faceEntity(npc);
			if (!player.withinDistance(npc, 4))
				return;
			npc.faceEntity(player);
			player.getDialogueManager().startDialogue("Wormbrain", npc.getId());
			return;
		}
		if (npc.getDefinitions().name.contains("Banker")
				|| npc.getDefinitions().name.contains("banker")) {
			player.faceEntity(npc);
			if (!player.withinDistance(npc, 2))
				return;
			npc.faceEntity(player);
			player.getBank().openBank();
			return;
		} if (npc.getId() == 6362) {
			player.faceEntity(npc);
			if (!player.withinDistance(npc, 2))
				return;
			npc.faceEntity(player);
			player.getBank().openBank();
			return;
		}
		if (npc.getDefinitions().name.toLowerCase().equals("grand exchange clerk")) {
			    if (player.isIronMan()) {
				    player.sm("You are not allowed to use the Grand Exchange.");
				    return;
			    }
			    player.faceEntity(npc);
			    if (!player.withinDistance(npc, 2))
				return;
			    npc.faceEntity(player);
			    player.getGeManager().openGrandExchange();
			    //player.sm("For testing purposes, this is currently disabled");
			    return;
		}
	    if (npc instanceof GraveStone) {
	        GraveStone grave = (GraveStone) npc;
	        grave.repair(player, false);
	        return;
	    }
		player.setCoordsEvent(new CoordsEvent(npc, new Runnable() {
			@Override
			public void run() {
				npc.resetWalkSteps();
				player.faceEntity(npc);
				FishingSpots spot = FishingSpots.forId(npc.getId() | (2 << 24));
				if (spot != null) {
					player.getActionManager().setAction(new Fishing(spot, npc));
					return;
				}
				PickPocketableNPC pocket = PickPocketableNPC.get(npc.getId());
				if (pocket != null) {
					player.getActionManager().setAction(
							new PickPocketAction(npc, pocket));
					return;
				}
				switch (npc.getDefinitions().name.toLowerCase()) {
			    case "void knight":
				CommendationExchange.openExchangeShop(player);
				break;
			}
				if (npc instanceof Familiar) {
					if (npc.getDefinitions().hasOption("store")) {
						if (player.getFamiliar() != npc) {
							player.getPackets().sendGameMessage(
									"That isn't your familiar.");
							return;
						}
						player.getFamiliar().store();
					} else if (npc.getDefinitions().hasOption("cure")) {
						if (player.getFamiliar() != npc) {
							player.getPackets().sendGameMessage(
									"That isn't your familiar.");
							return;
						}
						if (!player.getPoison().isPoisoned()) {
							player.getPackets().sendGameMessage(
									"Your arent poisoned or diseased.");
							return;
						} else {
							player.getFamiliar().drainSpecial(2);
							player.addPoisonImmune(120);
						}
					}
					return;
				}
				npc.faceEntity(player);
				if (!player.getControlerManager().processNPCClick2(npc))
					return;
				if(npc.getDefinitions().name.contains("Musician") || npc.getId() == 3509) {
					player.getDialogueManager().startDialogue("Musicians", npc.getId()); //All musicians around the world.
					return;
				}
                if (npc.getId() == 5160 || npc.getId() == 8876) {
			        SheepShearing.shearAttempt(player, npc);
                }
				if (npc.getId() == 9707)
					FremennikShipmaster.sail(player, true);
				else if (npc.getId() == 9708)
					FremennikShipmaster.sail(player, false);
				else if (npc.getId() == 14849 && npc instanceof ConditionalDeath)
				    ((ConditionalDeath) npc).useHammer(player);
				else if (npc.getId() >= 2291 && npc.getId() <= 2294)
					player.getDialogueManager().startDialogue("RugMerchant", true);
				else if (npc.getId() == 1686) {
					if (player.getInventory().hasFreeSlots() && player.unclaimedEctoTokens > 0) {
					//	player.getInventory().addItem(Ectofuntus.ECTO_TOKEN, player.unclaimedEctoTokens);
						player.unclaimedEctoTokens = 0;
					}
				}
				else if (npc.getId() == 13455 || npc.getId() == 2617 || npc.getId() == 2618 || npc.getId() == 15194)
					player.getBank().openBank();
				else if ((npc.getId() >= 3809 && npc.getId() <= 3812) || npc.getId() == 1800)
					player.getInterfaceManager().sendInterface(138);
				else if (npc.getId() == 5915)
				    player.getDialogueManager().startDialogue("ClaimClanItem", npc.getId(), 20709);
				else if (npc.getId() == 13633)
				    player.getDialogueManager().startDialogue("ClaimClanItem", npc.getId(), 20708);
				else if (SlayerMaster.startInteractionForId(player, npc.getId(), 2))
				    return;
				else if (npc.getId() == 1595)
					player.getDialogueManager().startDialogue("Saniboch", npc.getId());
				else if (npc.getId() == 922 || npc.getId() == 8207)
					player.getPackets().sendGameMessage("I think I should talk to Aggie first...");
				else if (npc.getId() == 4250) //sawmill interface
	                player.getInterfaceManager().sendInterface(403);	
				else if (npc.getId() >= 376 && npc.getId() <= 378)
					player.getDialogueManager().startDialogue("KaramjaTrip", npc.getId());
				else if (npc.getId() == 300) {
					npc.setNextForceTalk(new ForceTalk("Senventior disthine molenko!"));
					npc.setNextAnimation(new Animation(1818));
					npc.faceEntity(player);
					World.sendProjectile(npc, player, 110, 1, 1, 1, 1, 1, 1);
					player.setNextGraphics(new Graphics(110));
					player.setNextWorldTile(new WorldTile(2910, 4832, 0));
				} else if (npc.getId() == 5913) {
					npc.setNextForceTalk(new ForceTalk("Senventior disthine molenko!"));
					npc.setNextAnimation(new Animation(1818));
					npc.faceEntity(player);
					World.sendProjectile(npc, player, 110, 1, 1, 1, 1, 1, 1);
					player.setNextGraphics(new Graphics(110));
					player.setNextWorldTile(new WorldTile(2910, 4832, 0));
				}
				else if (npc.getDefinitions().name.contains("Fisherman")) {
					Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2854, 3430, 0));
					player.getPackets().sendGameMessage("The Fisherman teleports you to Catherby.");
				}
				else if (npc.getId() == 3680) {
					player.getPackets().sendGameMessage("You steal a flier from the poor boy, you are a cruel person.");
					player.getInventory().addItem(956, 1);
				} else if (npc.getDefinitions().name.contains("H.A.M. Guard")) {
					if (player.getEquipment().getAmuletId() == 4306 && player.getEquipment().getChestId() == 4298 && player.getEquipment().getLegsId() == 4300 && player.getEquipment().getHatId() == 4302 && player.getEquipment().getCapeId() == 4304 && player.getEquipment().getGlovesId() == 4308) {
						player.getDialogueManager().startDialogue("Ham", npc.getId());
					} else {
						npc.setNextForceTalk(new ForceTalk("Hey, what are you doing down here?"));
						npc.setTarget(player);
					}
				}
				else if (npc.getId() == 3777)
					player.getDialogueManager().startDialogue("ToggleGraves", npc);
				else if (npc.getId() == 2824)
					player.getDialogueManager().startDialogue("TanningD",
							npc.getId());	
				else if (npc.getId() == 7868) {
					player.quickWork = true;
					player.getDialogueManager().startDialogue("Iain", npc.getId());
				}
				else if (npc.getId() == 4904) {
					player.quickWork = true;
					player.getDialogueManager().startDialogue("ApprenticeSmith", npc.getId());
				}
				else if (npc.getId() == 7869) {
					player.quickWork = true;
					player.getDialogueManager().startDialogue("Julian", npc.getId());
				}
				else if (npc.getId() == 4903) {
					player.quickWork = true;
					player.getDialogueManager().startDialogue("PriestYauchomi", npc.getId());
				}
				else if(npc.getId() == 15149)
					player.getDialogueManager().startDialogue("MasterOfFear", 3);
				else if (npc.getId() == 2676)
					PlayerLook.openMageMakeOver(player);
				else if (npc.getId() == 598)
					PlayerLook.openHairdresserSalon(player);
				else if (npc instanceof Pet) {
					if (npc != player.getPet()) {
						player.getPackets().sendGameMessage("This isn't your pet!");
						return;
					}
					Pet pet = player.getPet();
					player.getPackets().sendMessage(99, "Pet [id=" + pet.getId() 
							+ ", hunger=" + pet.getDetails().getHunger()
							+ ", growth=" + pet.getDetails().getGrowth()
							+ ", stage=" + pet.getDetails().getStage() + "].", player);
				}
				else {
					//player.getPackets().sendGameMessage(
							//"Nothing interesting happens.");
					if (Settings.DEBUG) {
						System.out.println("cliked 2 at npc id : "
								+ npc.getId() + ", " + npc.getX() + ", "
								+ npc.getY() + ", " + npc.getPlane());
						Logger.logMessage("cliked 2 at npc id : "
								+ npc.getId() + ", " + npc.getX() + ", "
								+ npc.getY() + ", " + npc.getPlane());
					}
				}
			}
		}, npc.getSize()));
	}

	public static void handleOption3(final Player player, InputStream stream) {
		int npcIndex = stream.readUnsignedShort128();
		boolean forceRun = stream.read128Byte() == 1;
		final NPC npc = World.getNPCs().get(npcIndex);
		if (npc == null || npc.isCantInteract() || npc.isDead()
				|| npc.hasFinished()
				|| !player.getMapRegionsIds().contains(npc.getRegionId()))
			return;
		if (npc.getId() == 745) {
			player.faceEntity(npc);
			if (!player.withinDistance(npc, 4))
				return;
			npc.faceEntity(player);
			player.getDialogueManager().startDialogue("Wormbrain", npc.getId());
			return;
		}
		if (npc.getDefinitions().name.toLowerCase().equals("grand exchange clerk")) {
			if (player.isPker) {
				player.sm("You are not allowed to use the Grand Exchange.");
				return;
			}
			    player.faceEntity(npc);
			    if (!player.withinDistance(npc, 2))
				return;
			    npc.faceEntity(player);
			    player.getGeManager().openHistory();
			    return;
		}
		if (npc.getDefinitions().name.toLowerCase().contains("banker")) {
			if (player.isIronMan()) {
				player.sm("You are not allowed to use the Grand Exchange.");
				return;
			}
		    player.faceEntity(npc);
		    if (!player.withinDistance(npc, 2))
			return;
		    npc.faceEntity(player);
		    player.getGeManager().openCollectionBox();
		    return;
	    }
	    if (npc instanceof GraveStone) {
            GraveStone grave = (GraveStone) npc;
            grave.repair(player, true);
            return;
	    }
		player.stopAll(false);
		if(forceRun)
			player.setRun(forceRun);
		player.setCoordsEvent(new CoordsEvent(npc, new Runnable() {
			@Override
			public void run() {
				npc.resetWalkSteps();
				if (!player.getControlerManager().processNPCClick3(npc))
					return;
				player.faceEntity(npc);
				if (npc.getId() >= 8837 && npc.getId() <= 8839) {
					MiningBase.propect(player, "You examine the remains...", "The remains contain traces of living minerals.");
					return;
					
				}
				npc.faceEntity(player);
				
				if (npc.getId() == 9085) {
                    
				}
				if (npc.getId() == 8462) {
                    
				}
				if (npc.getId() == 8464) {
                    
				}
				if (npc.getId() == 4250) {
					ShopsHandler.openShop(player, 168);
				}
				if (npc.getId() == 970) {
					player.getPackets().sendGameMessage("This option is not available...");
				}
				else if (npc.getId() == 548)
					PlayerLook.openThessaliasMakeOver(player);
				//else if(npc.getId() == 9085)
	            //    player.getInterfaceManager().sendSlayerShop();
				else if (npc.getDefinitions().name.contains("Fisherman"))
					player.getPackets().sendGameMessage("You deserve no rewards....");
				//else if(npc.getId() == 9273)
	            //    player.getInterfaceManager().sendSlayerShop();
				else if (npc.getId() == 4250)
                    if (player.isIronMan()) {
                        player.sm("You can't open this shop as an Ironman.");
                        return;
                    } else {
					    ShopsHandler.openShop(player, 130);
                    }
				else if (SlayerMaster.startInteractionForId(player, npc.getId(), 3))
				    ShopsHandler.openShop(player, 29);
				else if (npc.getDefinitions().name.contains("H.A.M. Guard")) {
					if (player.getEquipment().getAmuletId() == 4306 && player.getEquipment().getChestId() == 4298 && player.getEquipment().getLegsId() == 4300 && player.getEquipment().getHatId() == 4302 && player.getEquipment().getCapeId() == 4304 && player.getEquipment().getGlovesId() == 4308) {
						player.getDialogueManager().startDialogue("Ham", npc.getId());
					} else {
						npc.setNextForceTalk(new ForceTalk("Hey, what are you doing down here?"));
						npc.setTarget(player);
					}
				}
				//else if(npc.getId() == 8274)
	            //    player.getInterfaceManager().sendSlayerShop();
				else if (npc.getId() >= 376 && npc.getId() <= 378)
					player.getDialogueManager().startDialogue("KaramjaTrip", npc.getId());
				else if ((npc.getId() >= 4650 && npc.getId() <= 4656) || npc.getId() == 7077)
					player.getDialogueManager().startDialogue("Sailing", npc);
				//else if(npc.getId() == 1598)
	            //    player.getInterfaceManager().sendSlayerShop();
				//else if(npc.getId() == 8275)
	            //    player.getInterfaceManager().sendSlayerShop();
				else if (npc.getId() == 7868) {
					player.quickWork = true;
					player.getDialogueManager().startDialogue("Iain", npc.getId());
				}
				else if (npc.getId() == 4904) {
					player.quickWork = true;
					player.getDialogueManager().startDialogue("ApprenticeSmith", npc.getId());
				}
				else if (npc.getId() == 7869) {
					player.quickWork = true;
					player.getDialogueManager().startDialogue("Julian", npc.getId());
				}
				else if (npc.getId() == 4903) {
					player.quickWork = true;
					player.getDialogueManager().startDialogue("PriestYauchomi", npc.getId());
				}
				else if (npc.getId() == 5532) {
				    SorceressGarden.teleportToSorceressGardenNPC(npc, player);
					
				} else
					;
					//player.getPackets().sendGameMessage(
							//"Nothing interesting happens.");
			}

		}, npc.getSize()));
		if (Settings.DEBUG) {
			System.out.println("cliked 3 at npc id : "
					+ npc.getId() + ", " + npc.getX() + ", "
					+ npc.getY() + ", " + npc.getPlane());
			Logger.logMessage("cliked 3 at npc id : "
					+ npc.getId() + ", " + npc.getX() + ", "
					+ npc.getY() + ", " + npc.getPlane());
		}
	}
		public static void handleOption4(final Player player, InputStream stream) {
			int npcIndex = stream.readUnsignedShort128();
			boolean forceRun = stream.read128Byte() == 1;
			final NPC npc = World.getNPCs().get(npcIndex);
			if (npc == null || npc.isCantInteract() || npc.isDead()
					|| npc.hasFinished()
					|| !player.getMapRegionsIds().contains(npc.getRegionId()))
				return;
			if (npc.getId() == 745) {
				player.faceEntity(npc);
				if (!player.withinDistance(npc, 4))
					return;
				npc.faceEntity(player);
				player.getDialogueManager().startDialogue("Wormbrain", npc.getId());
				return;
			}
			if (npc instanceof GraveStone) {
			    GraveStone grave = (GraveStone) npc;
			    grave.demolish(player);
			    return;
			}
			if (npc.getDefinitions().name.toLowerCase().equals("grand exchange clerk")) {
				    player.faceEntity(npc);
				    if (!player.withinDistance(npc, 2))
					return;
				    npc.faceEntity(player);
				    ItemSets.openSets(player);
				    return;
			}
			if (SlayerMaster.startInteractionForId(player, npc.getId(), 4)){
				player.deathShop = false;
			    player.getSlayerManager().sendSlayerInterface(SlayerManager.BUY_INTERFACE);
			}
			player.stopAll(false);
			if(forceRun)
				player.setRun(forceRun);
			player.setCoordsEvent(new CoordsEvent(npc, new Runnable() {
				@Override
				public void run() {
					npc.resetWalkSteps();
					if (!player.getControlerManager().processNPCClick4(npc))
						return;
					player.faceEntity(npc);
					npc.faceEntity(player);
					if (npc.getId() == 5913) {
						npc.setNextForceTalk(new ForceTalk("Senventior disthine molenko!"));
						npc.setNextAnimation(new Animation(1818));
						npc.faceEntity(player);
						World.sendProjectile(npc, player, 110, 1, 1, 1, 1, 1, 1);
						player.setNextGraphics(new Graphics(110));
						player.setNextWorldTile(new WorldTile(2910, 4832, 0));
					}
					if (npc.getId() == 970) {
						player.getPackets().sendGameMessage("This option is not available...");
					}
					if (SlayerMaster.startInteractionForId(player, npc.getId(), 4))
					    player.getSlayerManager().sendSlayerInterface(SlayerManager.BUY_INTERFACE);
					else if (npc.getId() == 5532) {
						npc.setNextForceTalk(new ForceTalk("Senventior Disthinte Molesko!"));
						player.getControlerManager().startControler("SorceressGarden");
						
					} else
						;
						//player.getPackets().sendGameMessage(
						//		"Nothing interesting happens.");
				}

			}, npc.getSize()));
			if (Settings.DEBUG) {
				System.out.println("cliked 4 at npc id : "
						+ npc.getId() + ", " + npc.getX() + ", "
						+ npc.getY() + ", " + npc.getPlane());
				Logger.logMessage("cliked 4 at npc id : "
						+ npc.getId() + ", " + npc.getX() + ", "
						+ npc.getY() + ", " + npc.getPlane());
			}
	}
}
