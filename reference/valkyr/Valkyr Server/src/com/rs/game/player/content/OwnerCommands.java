package com.rs.game.player.content;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.rs.Settings;
import com.rs.cache.loaders.ItemDefinitions;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.cache.loaders.ObjectDefinitions;
import com.rs.game.Animation;
import com.rs.game.ForceMovement;
import com.rs.game.ForceTalk;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.Region;
import com.rs.game.World;
import com.rs.game.WorldObject;
import com.rs.game.WorldTile;
import com.rs.game.Hit.HitLook;
import com.rs.game.bot.ValkyrBot;
import com.rs.game.item.Item;
import com.rs.game.item.ItemsContainer;
import com.rs.game.minigames.FightPits;
import com.rs.game.minigames.clanwars.ClanWars;
import com.rs.game.minigames.clanwars.WallHandler;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScriptsHandler;
import com.rs.game.npc.others.Bork;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.TeleportManager;
import com.rs.game.player.QuestManager.Quests;
import com.rs.game.player.actions.divination.HarvestWisp;
import com.rs.game.player.content.clans.ClansManager;
import com.rs.game.player.content.construction.House;
import com.rs.game.player.content.magic.Magic;
import com.rs.game.player.controlers.ControlerHandler;
import com.rs.game.player.controlers.FightCaves;
import com.rs.game.player.controlers.FightKiln;
import com.rs.game.player.controlers.PestInvasion;
import com.rs.game.player.controlers.Wilderness;
import com.rs.game.player.controlers.dung.RuneDungGame;
import com.rs.game.player.cutscenes.CutscenesHandler;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.game.player.dialogues.DialogueHandler;
import com.rs.game.player.quest.impl.TheRestlessGhost;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.IPBanL;
import com.rs.utils.ItemBonuses;
import com.rs.utils.NPCCombatDefinitionsL;
import com.rs.utils.NPCDrops;
import com.rs.utils.NPCSpawns;
import com.rs.utils.ObjectSpawns;
import com.rs.utils.PkRank;
import com.rs.utils.SerializableFilesManager;
import com.rs.utils.ShopsHandler;
import com.rs.utils.Utils;

public final class OwnerCommands {
	
	public static boolean checkCommand(Player player, String command, boolean consoleCommand, boolean clientCommand) {
		if (command.length() == 0) // if they used @(nothing) theres no nullpointer
			return false;
		if (!player.isOwner()) {
			player.sm("You need to be an Owner to use this command format.");
			return false;
		}
		String[] cmd = command.toLowerCase().split(" ");
		if (cmd.length == 0)
			return false;
		return process(player, cmd, consoleCommand, clientCommand);
	}

	public static boolean process(Player player, String[] cmd, boolean consoleCommand, boolean clientCommand) {
		player.getPackets().sendPanelBoxMessage("[Script Manager]: Running command - " + cmd[0]);
		String name;
		Player target;
		WorldObject object;
		Player target1;
		if (clientCommand) {
			switch (cmd[0]) {
			case "tele": //client command with shirt+ctrl teleporting
				cmd = cmd[1].split(",");
				int plane = Integer.valueOf(cmd[0]);
				int x = Integer.valueOf(cmd[1]) << 6 | Integer.valueOf(cmd[3]);
				int y = Integer.valueOf(cmd[2]) << 6 | Integer.valueOf(cmd[4]);
				player.setNextWorldTile(new WorldTile(x, y, plane));
				return true;
			}
		} else { 
			switch (cmd[0]) {
			
				case "rps":
					player.getPresetManager().removePreset(Integer.valueOf(cmd[1]));
				return true;
				
				case "sps":
					player.getPresetManager().savePreset(Integer.valueOf(cmd[1]));
				return true;
				
				case "lps":
					player.getPresetManager().loadPreset(Integer.valueOf(cmd[1]));
				return true;
			
				case "presets":
					player.getInterfaceManager().openTeleportInterface(player, 110);
				return true;
				
				case "clearperks":
					player.clearPerksMap();
				return true;
				
				case "setexpmode":
					player.setExpMode(Integer.valueOf(cmd[1]));
					player.sm("Current Exp Multiplyer: " + player.getExpMode() + "x");
				return true;
				
				case "removeperk":
					String rPerk = "";
					for (int i = 1; i < cmd.length; i++)
						rPerk += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					player.removePerk(rPerk);
					return true;
				
	            case "addperk":
	                String aPerk = "";
					for (int i = 1; i < cmd.length; i++)
						aPerk += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					player.unlockPerk(aPerk);
				return true;	
				
				case "perks":
					player.getPerks();
				return true;
				
				case "forcekick":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (target == null) {
						player.getPackets().sendGameMessage(
								Utils.formatPlayerNameForDisplay(name)+" is not logged in.");
						return true;
					}
					target.forceLogout();
					player.getPackets().sendGameMessage("You have kicked: "+target.getDisplayName()+".");
				return true;
				
				case "resettask":
					player.getSlayerManager().cancleCurrentTask();
				return true;
				
				case "spawnvalkyr":
					new ValkyrBot("password");
				return true;
				
				case "timers":
					player.startTimers();
					player.sm("Starting potion timers.");
				return true;
				
				case "resettimers":
					player.overloadTimer = 0;
					player.superAntiTimer = 0;
					player.antiPoisonTimer = 0;
					player.renewalTimer = 0;
				return true;
				
				case "assassin":
					player.getInterfaceManager().sendAssassin();
				return true;
				
				case "p":
					String red = "" + cmd[1].substring(0, 2);
					String green = "" + cmd[1].substring(2, 4);
					String blue = "" + cmd[1].substring(4, 6);
					player.getAppearence().cr = Integer.parseInt(red, 16);
					player.getAppearence().cg = Integer.parseInt(green, 16);
					player.getAppearence().cb = Integer.parseInt(blue, 16);
					player.getAppearence().ca = Integer.parseInt(cmd[2]);
					player.getAppearence().ci = Integer.parseInt(cmd[3]);			
					player.getAppearence().ce = true;
					player.getAppearence().generateAppearenceData();
				return true;
				
				case "togglep":
					player.getAppearence().ce = !player.getAppearence().ce;
					player.getAppearence().generateAppearenceData();
					player.sm("Particles are now " + (player.getAppearence().ce ? "enabled." : "disabled."));
				return true;
				
				case "debug":
					player.switchDebugMode();
					player.sm(player.debug ? "Debug Mode is now activated." : "Debug Mode is now disabled.");
				return true;
				
				case "massspawn":
					ArrayList<WorldTile> locations = new ArrayList<WorldTile>();
					for (int x = player.getX() - 15; x < player.getX() + 15; x++) {
						for (int y = player.getY() - 15; y < player.getY() + 15; y++)
							locations.add(new WorldTile(x, y, 0));
					}
					for (WorldTile loc : locations) {
						if (!World.canMoveNPC(loc.getPlane(), loc.getX(),
								loc.getY(), 1))
							continue;
						World.spawnNPC(Integer.valueOf(cmd[1]), loc, -1, true, true);
					}
				return true;	
				
				case "joinhouse":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					House.enterHouse(player, name);
				return true; 
				
				case "unjail":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (target != null) {
						target.setJailed(0);
						target.getControlerManager()
						.startControler("JailControler");
						target.getPackets().sendGameMessage(
								"You've been unjailed by "+Utils.formatPlayerNameForDisplay(player.getUsername())+".");
						player.getPackets().sendGameMessage(
								"You have unjailed: "+target.getDisplayName()+".");
						SerializableFilesManager.savePlayer(target);
					} else {
						File acc1 = new File("data/characters/"+name.replace(" ", "_")+".p");
						try {
							target = (Player) SerializableFilesManager.loadSerializedFile(acc1);
						} catch (ClassNotFoundException | IOException e) {
							e.printStackTrace();
						}
						target.setJailed(0);
						player.getPackets().sendGameMessage(
								"You have unjailed: "+Utils.formatPlayerNameForDisplay(name)+".");
						try {
							SerializableFilesManager.storeSerializableClass(target, acc1);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				return true;
				
				case "killwithin":
					List<Integer> npcs = World.getRegion(player.getRegionId()).getNPCsIndexes();
					for (int index = 0; index < npcs.size() + 1; index++)
						World.getNPCs().get(npcs.get(index)).sendDeath(player);	
				return true;
				
				/*case "dumpdefs":
					BeastiaryDumper.main(cmd[1]);
				return true;*/
				
				case "springcleaner":
					SpringCleaner.openInterface(player);
				return true;
				
				case "lumbermill":
					Lumbermill.openInterface(player);
				return true;
				
				case "teleports":
					player.getInterfaceManager().openTeleportInterface(player, 1);
				return true;
				
				case "switchpvp":
					if (player.inCombat() || Wilderness.isAtWild(player)) {
						player.sm("You can't toggle your PvP right now.");
						return false;
					}
					player.switchPvp();
					player.sm(player.isCanPvp() ? "PvP is now enabled." : "PvP is now disabled.");
				return false;
				
				case "wildteleports":
					player.getInterfaceManager().openTeleportInterface(player, 50);
				return true;
				
				case "clearchat":
					for (int i = 0; i < 87; i++)
						player.getPackets().sendGameMessage(""); //basicly ddos's you with chat packets btw
				return true;
				
				case "getmodel":
					int id = Integer.parseInt(cmd[1]);
					Item item = new Item(id);
					int mmodel1 = item.getDefinitions().maleEquip1;
					int fmodel1 = item.getDefinitions().femaleEquip1;
					player.sendMessage("Male: "+mmodel1+" Female: "+fmodel1+"");
				return true;
				
				case "qp":
					int qp = Integer.valueOf(cmd[1]);
					player.questPoints = qp;
					player.getInterfaceManager().sendQuestTab();
				return true;
				
				case "reloadmap":
					player.setForceNextMapLoadRefresh(true);
					player.loadMapRegions();
					player.lock(2);
					player.getPackets().sendPanelBoxMessage("Updating Map Region...");
				return true;
	            
	            case "switchironman":
	                player.switchIronMan(); 
	                player.getAppearence().setTitle(0);
	                player.getAppearence().generateAppearenceData();
	                player.sm(player.isIronMan() ? "Ironman is now activated." : "Ironman is now disabled.");
	            return true;
	            
	            case "packshops":
	            	ShopsHandler.loadUnpackedShops();
	            	player.getPackets().sendGameMessage("You Packed The Shops.");
	    		return true;  
	    		
	            case "jihad":
	                WorldTasksManager.schedule(new WorldTask() {
					    int ticks;
					    @Override
					    public void run() {
						    ticks++;
	                        if (ticks == 1) {
				                player.setNextGraphics(new Graphics(3263));
				                player.setNextGraphics(new Graphics(2929));
					            player.setNextForceTalk(new ForceTalk("Allah Akbar!"));
	                        }
	                        if(ticks == 4) {
	                        	for (Player p : World.getPlayers()) {
	                        	     if (p == null || p == player || p.withinDistance(player, 2)) {                        
	                        	         p.applyHit(new Hit(p, p.getHitpoints(), HitLook.REGULAR_DAMAGE));
	                        	     } else {
	                        	    	 
	                        	     }
	                        	}
	                        	return;
	                        } 
					    }
				    }, 0, 0);	
	            return true;
	            
	            case "getremote":
	            	int npcId = Integer.valueOf(cmd[1]);
	            	player.sm("Render Emote: " + NPCDefinitions.getNPCDefinitions(npcId).renderEmote);
	            return true;       
	                
				case "task":
					player.sm("My current task is "+player.getAssassinsManager().getTask()+" number "+player.getAssassinsManager().getAmount()+" type "+player.getAssassinsManager().getGameMode()+".");
				return true;
				
				case "gettask":
					int mode = Integer.parseInt(cmd[1]);
					player.getAssassinsManager().getTask(mode);
				return true;
				
				case "resetassassin":
					player.getAssassinsManager().resetTask();
				return true;
	
				case "divineruneore":
					WorldTasksManager.schedule(new WorldTask() {
					    int ticks;
					    @Override
					    public void run() {
						ticks++;
					if(ticks == 1){
						player.setNextAnimation(new Animation(21217));
						player.addFreezeDelay(2500);
						final WorldObject divinerune = new WorldObject(87290,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object spawning
						player.faceObject(divinerune); // forces player to face where he is putting the object
						World.spawnTemporaryObject(divinerune, 3000); // dont touch
					}
					if(ticks == 5){
						final WorldObject divinerunea = new WorldObject(87269,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object with animation that player will mine
						World.spawnTemporaryObject(divinerunea, 40000);	// time object will stay in miliseconds
						 stop();
								}
					    return;
					    }  
				}, 0, 0);	
			    return true;	
			    
				case "divineadamore":	
					WorldTasksManager.schedule(new WorldTask() {
					    int ticks;
					    @Override
					    public void run() {
						ticks++;
					if(ticks == 1){
						player.setNextAnimation(new Animation(21217));
						player.addFreezeDelay(2500);
						final WorldObject divinerune = new WorldObject(87289,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object spawning
						player.faceObject(divinerune); // forces player to face where he is putting the object
						World.spawnTemporaryObject(divinerune, 3000); // dont touch
					}
					if(ticks == 5){
						final WorldObject divinerunea = new WorldObject(87268,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object with animation that player will mine
						World.spawnTemporaryObject(divinerunea, 40000);	// time object will stay in miliseconds
						 stop();
								}
					    return;
					    }  
				}, 0, 0);	
				return true;
					    
				case "divinemithore":	
					WorldTasksManager.schedule(new WorldTask() {
					    int ticks;
					    @Override
					    public void run() {
						ticks++;
					if(ticks == 1){
						player.setNextAnimation(new Animation(21217));
						player.addFreezeDelay(2500);
						final WorldObject divinerune = new WorldObject(87288,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object spawning
						player.faceObject(divinerune); // forces player to face where he is putting the object
						World.spawnTemporaryObject(divinerune, 3000); // dont touch
					}
					if(ticks == 5){
						final WorldObject divinerunea = new WorldObject(87267,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object with animation that player will mine
						World.spawnTemporaryObject(divinerunea, 40000);	// time object will stay in miliseconds
						 stop();
								}
					    return;
					    }  
				}, 0, 0);	
				return true;
					
				case "divinecoalore":	
					WorldTasksManager.schedule(new WorldTask() {
						
					    int ticks;
					    
					    @Override
					    public void run() {
							ticks++;
							if(ticks == 1){
								player.setNextAnimation(new Animation(21217));
								player.addFreezeDelay(2500);
								final WorldObject divinerune = new WorldObject(87287,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object spawning
								player.faceObject(divinerune); // forces player to face where he is putting the object
								World.spawnTemporaryObject(divinerune, 3000); // dont touch
							}
							if(ticks == 5){
								final WorldObject divinerunea = new WorldObject(87266,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object with animation that player will mine
								World.spawnTemporaryObject(divinerunea, 40000);	// time object will stay in miliseconds
								stop();
							}
					    }  
				}, 0, 0);	
				return true;
					
				case "divineironore":	
					WorldTasksManager.schedule(new WorldTask() {
					    int ticks;
					    @Override
					    public void run() {
							ticks++;
							if(ticks == 1){
								player.setNextAnimation(new Animation(21217));
								player.addFreezeDelay(2500);
								final WorldObject divinerune = new WorldObject(87286,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object spawning
								player.faceObject(divinerune); // forces player to face where he is putting the object
								World.spawnTemporaryObject(divinerune, 3000); // dont touch
							}
							if(ticks == 5){
								final WorldObject divinerunea = new WorldObject(57572,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object with animation that player will mine
								World.spawnTemporaryObject(divinerunea, 40000);	// time object will stay in miliseconds
								stop();
							}
					  }  
				}, 0, 0);	
				return true;
				
				case "divinebronzeore":	
					WorldTasksManager.schedule(new WorldTask() {
					    int ticks;
					    @Override
					    public void run() {
							ticks++;
							if(ticks == 1){
								player.setNextAnimation(new Animation(21217));
								player.addFreezeDelay(2500);
								final WorldObject divinerune = new WorldObject(87285,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object spawning
								player.faceObject(divinerune); // forces player to face where he is putting the object
								World.spawnTemporaryObject(divinerune, 3000); // dont touch
							}
							if(ticks == 5){
								final WorldObject divinerunea = new WorldObject(34107,10, 0, player.getX() + 1, player.getY(), player.getPlane()); // object with animation that player will mine
								World.spawnTemporaryObject(divinerunea, 40000);	// time object will stay in miliseconds
								stop();
							}
					    }  
					}, 0, 0);
				return true;
	
				case "dailyreset":
					player.hasdaily = false;
					player.dailyhasTask=false;
			    	player.getSkillersManager().resetTask();
			    	player.TASKID = -1;
			    	player.sendMessage("your daily task has been reset");
			    return true;
			    
				case "event":
					String event = cmd[0];
					if (cmd.length >= 2) {
							event = cmd[1];
						if (cmd.length == 3) {
							event = cmd[1] + " " + cmd[2];
						}
						if (cmd.length == 4) {
							event = cmd[1] + " " + cmd[2] + " " + cmd[3];
						}
						if (cmd.length == 5) {
							event = cmd[1] + " " + cmd[2] + " " + cmd[3] + " " + cmd[4];
						}
						if (cmd.length == 6) {
							event = cmd[1] + " " + cmd[2] + " " + cmd[3] + " " + cmd[4] + " " + cmd[5];
						}
						if (cmd.length == 7) {
							event = cmd[1] + " " + cmd[2] + " " + cmd[3] + " " + cmd[4] + " " + cmd[5] + " " + cmd[6];
						}
						ClansManager.clanEvent(event, player);
					}
				return true; 
				
				case "cc":
					player.getPackets().sendJoinClanChat(player.getDisplayName(), "Valkyr");
					return true;
					
				case "bankpin":
				    player.getBank().openPin();
				    player.getTemporaryAttributtes().put("recovering_pin", true);
				return true;
					
				case "customizeclancape":
					ClanCapeCustomizer.startCustomizing(player);
				return true;
	
				case "clancapecolor":
					player.setClanCapeCustomized(new int[] { Integer.valueOf(cmd[1]), Integer.valueOf(cmd[2]), Integer.valueOf(cmd[3]), Integer.valueOf(cmd[4]) });
					player.getAppearence().generateAppearenceData();
				return true;
	
				case "clancapetex":
					if (Integer.valueOf(cmd[1]) < 2320) {
						player.setClanCapeSymbols(new int[] { Integer.valueOf(cmd[1]), Integer.valueOf(cmd[2]) });
						player.getAppearence().generateAppearenceData();
					} else {
						player.getPackets().sendGameMessage("Max shit is 2320.");
					}
				return true;
	
					
				case "resetquests":
					player.SOWQUEST = 0;
					player.sm("You have reset your quests.");
				return true;
					
				case "history":
					//player.grandExchange().sendHistoryInterface(player);
				return true;
					
				case "eviltree":
					World.startEvilTree();
				return true;
					
				case "gwdcount":
					player.sendMessage("Armadyl Kill Count: "+player.armadyl+"");
					player.sendMessage("Bandos Kill Count: "+player.bandos+"");
					player.sendMessage("Saradomin Kill Count: "+player.saradomin+"");
					player.sendMessage("Zamorak Kill Count: "+player.zamorak+"");
				return true;			
					
				case "removetokens":
					player.setWGuildTokens((player.getWGuildTokens() - 10));
					player.sendMessage("You lost 10 Tokens");
				return true;				
					
				case "wguild":
					player.getControlerManager().startControler("WGuildControler");
				return true;
					
				case "newtut":
					player.getDialogueManager().startDialogue("NewPlayerTutorial");
				return true;
					
				case "closeinter":
					SpanStore.closeShop(player);
				return true;
					
				case "kbdin":
					player.getControlerManager().startControler("kbd");
				return true;
					
				case "rspoints":
					player.sm("You have "+ player.RuneSpanPoints +" RuneSpan Points.");
				return true;
					
				/*case "pendant":
					player.sm("You have "+ player.getPendant().getSkill() +" at a rate of "+ player.getPendant().getModifier() +" also "+player.getPendant().hasAmulet()+".");
					return true;*/
							
				case "findstring":
				    final int value = Integer.valueOf(cmd[1]);
				    player.getInterfaceManager().sendInterface(Integer.valueOf(cmd[1]));
				    
				    WorldTasksManager.schedule(new WorldTask() {
				     int value2;
				     
				     @Override
				     public void run() {
				      player.getPackets().sendIComponentText(value, value2, "String " + value2);
				      player.getPackets().sendGameMessage("" + value2);
				      value2 += 1;
				     }
				    }, 0, 1/2);
				return true;
					
				case "admintitle":
					try {
						if (Integer.valueOf(cmd[1]) > 100000) {
							player.out("You can only use titles under 100000.");
						} else {
							player.getAppearence().setTitle(Integer.valueOf(cmd[1]));	
						}
					} catch (NumberFormatException e) {
						player.getPackets().sendGameMessage("Use: ::title id");
					}
				return true; 
					
				case "givespins":
					String username = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other = World.getPlayerByDisplayName(username);
					if (other == null)
						return false;
					other.getSquealOfFortune().setBoughtSpins(Integer.parseInt(cmd[2]));
					other.getPackets().sendGameMessage("You have recived some spins!");
				return true;
					
				case "dwarf":
					player.completedDwarfCannonQuest = true;
				return true;
				
				case "givedpoints":
					String username1 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other666669 = World.getPlayerByDisplayName(username1);
					if (other666669 == null)
						return false;
					other666669.setDonatorPoints(Integer.parseInt(cmd[2]));
					other666669.getPackets().sendGameMessage("You have recived some Donator Points!");
				return true;
	                
	           	case "givedungpoints":
					String username5001 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other49845 = World.getPlayerByDisplayName(username5001);
					if (other49845 == null)
						return false;
					other49845.setDungTokens(other49845.getDungTokens() + Integer.parseInt(cmd[2]));
					other49845.getPackets().sendGameMessage("You've recieved " + Integer.parseInt(cmd[2]) + " Dungeoneering tokens.");
				return true;
					
				case "givepestpoints":
					String username500111 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other49845223 = World.getPlayerByDisplayName(username500111);
					if (other49845223 == null)
						return false;
					other49845223.setPestPoints(other49845223.getPestPoints() + Integer.parseInt(cmd[2]));
					other49845223.getPackets().sendGameMessage("You've recieved " + Integer.parseInt(cmd[2]) + " PestPoints.");
				return true;
	                
				case "dtaskother":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if(target == null) {
					     player.getPackets().sendGameMessage("Couldn't find player " + name + ".");
					     return false;
					} else {
					     target.dhasTask = false;
					     target.damount = 0;
					}
			    return true;
			    
				case "checkxpwell":
					if(World.wellActive = false) {
						player.sendMessage("the well is not active");	  
					} else {
						player.sendMessage("the well is active");	  
					}
				return true;
				  
				case "resetxpwell":
					//XPWell.setWellTask();
					World.setWellActive(false);
					World.resetWell();
					World.wellAmount = 0;
					// XPWell.taskAmount = 7200000;
					// XPWell.taskTime = 7200000;
				return true;
					  
				case "xpwellamount":
					  player.sendMessage("Amount in the well is "+World.getWellAmount()+" gold.");
				return true;
				
				case "xpwelltime":
					  player.sendMessage("Time Left "+XPWell.taskAmount+" For double exp");
					  player.sendMessage("Time Left "+XPWell.taskTime+" For double exp");
				return true;
					  
				case "dailyresetother":
					name = "";
					for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if(target == null)
						player.getPackets().sendGameMessage("Couldn't find player " + name + ".");
					else {
						target.hasdaily = false;
						target.dailyhasTask=false;
					    target.getSkillersManager().resetTask();
					    target.TASKID = -1;
					    target.sendMessage("your daily task has been reset by "+ player.getUsername());
					    player.sendMessage("You have reset "+ target.getUsername() + " daily task.");
			            return false;
					}
				return true;
				    
				case "findconfig":
				    final int configvalue = Integer.valueOf(cmd[1]);
				    player.getInterfaceManager().sendInterface(Integer.valueOf(cmd[1]));
				    
				    WorldTasksManager.schedule(new WorldTask() {
				     int value2;
				     
				     @Override
				     public void run() {
				      player.getPackets().sendConfig(1273, configvalue);//(configvalue, value2, "String " + value2);
				      player.getPackets().sendGameMessage("" + value2);
				      value2 += 1;
				     }
				    }, 0, 1/2);
				return true;
				    
				case "findconfig2":
				    player.getInterfaceManager().sendInterface(Integer.valueOf(cmd[1]));
				    
				    WorldTasksManager.schedule(new WorldTask() {
				     int value2;
				     
				     @Override
				     public void run() {
				      player.getPackets().sendConfig(value2, 1);
				      player.getPackets().sendGameMessage("" + value2);
				      value2++;
				     }
				    }, 0, 1/2);
				return true;
					
				case "sgar":
					if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
						player.getPackets().sendGameMessage("You can't open your bank during this game.");
						return false;
					}
					player.getControlerManager().startControler("SorceressGarden");
				return true;
				
				case "scg":
					if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
						player.getPackets().sendGameMessage("You can't open your bank during this game.");
						return false;
					}
					player.getControlerManager().startControler("StealingCreationsGame", true);
				return true;
					
				case "configsize":
					player.getPackets().sendGameMessage("Config definitions size: 2633, BConfig size: 1929.");
				return true;
					
				case "npcmask":
					for (NPC n : World.getNPCs()) {
						if (n != null && Utils.getDistance(player, n) < 9) {
							n.setNextForceTalk(new ForceTalk("Vuse-RSPS!"));
						}
					}
				return true;
					
				case "runespan":
					player.getControlerManager().startControler("RuneSpanControler");
				return true;
					
				case "qbd":
					if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
						player.getPackets().sendGameMessage("You can't open your bank during this game.");
						return false;
					}
					if (player.getSkills().getLevelForXp(Skills.SUMMONING) < 60) {
						player.getPackets().sendGameMessage("You need a summoning level of 60 to go through this portal.");
						player.getControlerManager().removeControlerWithoutCheck();
						return false;
					}
					player.lock();
					player.getControlerManager().startControler("QueenBlackDragonControler");
				return true;
	
				case "killingfields":
					player.getControlerManager().startControler("KillingFields");
				return true;
	
				case "nntest":
					Dialogue.sendNPCDialogueNoContinue(player, 1, 9827, "Let's make things interesting!");
				return true;
				
				case "pptest":
					player.getDialogueManager().startDialogue("SimplePlayerMessage", "123");
				return true;
					
				case "achieve":
					player.getInterfaceManager().sendAchievementInterface();
				return true;
	
				case "debugobjects":
					System.out.println("Standing on " + World.getObject(player));
					Region r = World.getRegion(player.getRegionY() | (player.getRegionX() << 8));
					if (r == null) {
						player.getPackets().sendGameMessage("Region is null!");
						return true;
					}
					List<WorldObject> objects = r.getObjects();
					if (objects == null) {
						player.getPackets().sendGameMessage("Objects are null!");
						return false;
					}
					for (WorldObject o : objects) {
						if (o == null || !o.matches(player)) {
							continue;
						}
						System.out.println("Objects coords: "+o.getX()+ ", "+o.getY());
						System.out.println("[Object]: id=" + o.getId() + ", type=" + o.getType() + ", rot=" + o.getRotation() + ".");
					}
				return true;
					
				case "telesupport":
					for (Player staff : World.getPlayers()) {
						if (!staff.isSupporter())
							continue;
						staff.setNextWorldTile(player);
						staff.getPackets().sendGameMessage("You been teleported for a staff meeting by "+player.getDisplayName());
					}
				return true;
					
				case "telemods":
					for (Player staff : World.getPlayers()) {
						if (staff.getRights() != 1)
							continue;
						staff.setNextWorldTile(player);
						staff.getPackets().sendGameMessage("You been teleported for a staff meeting by "+player.getDisplayName());
					}
				return true;
					
				case "telestaff":
					for (Player staff : World.getPlayers()) {
						if (!staff.isSupporter() && staff.getRights() != 1)
							continue;
						staff.setNextWorldTile(player);
						staff.getPackets().sendGameMessage("You been teleported for a staff meeting by "+player.getDisplayName());
					}
				return true;
					
				case "pickuppet":
					if (player.getPet() != null) {
						player.getPet().pickup();
						return false;
					}
					player.getPackets().sendGameMessage("You do not have a pet to pickup!");
				return true;
				
				case "setrights":
					String username2324 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other2324 = World.getPlayerByDisplayName(username2324);
					if (other2324 == null)
						return false;
					other2324.setRights(Integer.parseInt(cmd[2]));
					if (other2324.getRights() > 0) {
						other2324.out("Congratulations, You have been promoted to "+ (player.getRights() == 2 ? "Admin" : "Mod") +".");
					} else {
						other2324.out("Unfortunately you have been demoted.");
					}
				return true;
					
				case "kbdinn":
					player.getControlerManager().startControler("KingBlackDragon");
				return true;
					
				case "vp":  
					player.playerpoints +=500;
					player.sendMessage("500 tokens added"); 
				return true;
				
				case "setmode":
					String username23241 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other23241 = World.getPlayerByDisplayName(username23241);
					if (other23241 == null)
						return false;
					other23241.setGameMode(Integer.parseInt(cmd[2]));
					if (other23241.getGameMode() == 0) {
					other23241.out("Your game mode has been set to: Standard");
					} else if (other23241.getGameMode() == 1) {
						other23241.out("Your game mode has been set to: Challenging");
					} else if (other23241.getGameMode() == 2) {
						other23241.out("Your game mode has been set to: Difficult");
					} else if (other23241.getGameMode() == 3) {
						other23241.out("Your game mode has been set to: Hardcore");
					}
				return true;
					
				case "testcosmetics": 
					player.setAssassin(!player.isAssassin());
					player.getAppearence().generateAppearenceData();
				return true;
					
				case "removecosmetics":
					player.setAssassin(false);
					player.getAppearence().generateAppearenceData();
				return true;
					
				case "removeequipitems":
					File[] chars = new File("data/characters").listFiles();
					int[] itemIds = new int[cmd.length - 1];
					for (int i = 1; i < cmd.length; i++) {
						itemIds[i - 1] = Integer.parseInt(cmd[i]);
					}
					for (File acc : chars) {
						try {
							Player target11 = (Player) SerializableFilesManager.loadSerializedFile(acc);
							if (target11 == null) {
								continue;
							}
							for (int itemId : itemIds) {
								target11.getEquipment().deleteItem(itemId, Integer.MAX_VALUE);
							}
							SerializableFilesManager.storeSerializableClass(target11, acc);
						} catch (Throwable e) {
							e.printStackTrace();
							player.getPackets().sendMessage(99, "failed: " + acc.getName()+", "+e, player);
						}
					}
					for (Player players : World.getPlayers()) {
						if (players == null)
							continue;
						for (int itemId : itemIds) {
							players.getEquipment().deleteItem(itemId, Integer.MAX_VALUE);
						}
					}
				return true;
					
				case "goblinraid":
					World.sendWorldMessage("<img=7><col=FF0000>News: Goblins have raided Edgeville!", false);
					World.spawnNPC(3264, new WorldTile(3695, 2967, 0), -1, true, true); 
					World.spawnNPC(3264, new WorldTile(3696, 2963, 0), -1, true, true);
					World.spawnNPC(3264, new WorldTile(3692, 2968, 0), -1, true, true);
					World.spawnNPC(3264, new WorldTile(3692, 2965, 0), -1, true, true);
					return true;
					
				case "restartfp":
					FightPits.endGame();
					player.getPackets().sendGameMessage("Fight pits restarted!");
				return true;
	
				case "teletome":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (target == null)
						return false;
					if (Wilderness.isAtWild(player) || Wilderness.isAtWild(target) || player.isInDung() || target.isInDung()) {
						player.sm("Nice try");
						return false;
					}
					target.setNextWorldTile(player);
				return true; 
				
				case "teleto":
					if (player.isLocked() || player.getControlerManager().getControler() != null) {
						player.getPackets().sendGameMessage("You cannot tele anywhere from here.");
						return true;
					}
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if(target == null)
						player.getPackets().sendGameMessage(
								"Couldn't find player " + name + ".");
					else
						player.setNextWorldTile(target);
				return true;
					
				case "pos":
					try {
						File file = new File("data/positions.txt");
						BufferedWriter writer = new BufferedWriter(new FileWriter(
								file, true));
						writer.write("|| player.getX() == " + player.getX()
								+ " && player.getY() == " + player.getY() + "");
						writer.newLine();
						writer.flush();
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				return true; 
					
				case "unipban":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					File acc11 = new File("data/characters/"+name.replace(" ", "_")+".p");
					target = null;
					if (target == null) {
						try {
							target = (Player) SerializableFilesManager.loadSerializedFile(acc11);
						} catch (ClassNotFoundException | IOException e) {
							e.printStackTrace();
						}
					}
					IPBanL.unban(target);
					player.getPackets().sendGameMessage(
							"You've unipbanned "+Utils.formatPlayerNameForDisplay(target.getUsername())+ ".");
					try {
						SerializableFilesManager.storeSerializableClass(target, acc11);
					} catch (IOException e) {
						e.printStackTrace();
					}
				return true;
				
				case "players":
					player.getInterfaceManager().sendInterface(275);
	                int number = 0;
	                for (int i = 0; i < 100; i++) {
	                    player.getPackets().sendIComponentText(275, i, "");
	                }
	                for (Player p5 : World.getPlayers()) {
	                    if (p5 == null) {
	                        continue;
	                    }
	                    number++;
	                    String titles = "";
	                    if (!(p5.isDonator()) || !p5.isExtremeDonator()) {
	                        titles = "[Player]: ";
	                    }
	                    if (p5.isDonator()) {
	                        titles = "<col=008000><img=8>[Donator]: ";
	                    }
						if (p5.getUsername().equalsIgnoreCase("corey")) {
							titles = "<col=7a0000><img=1>[Owner]: ";
						}
						if (p5.getUsername().equalsIgnoreCase("andy")) {
							titles = "<col=ad0000><img=1>[Owner]: ";
						}
						if (p5.getUsername().equalsIgnoreCase("chipmunk")) {
							titles = "<col=ebc21a><img=1>[Head Admin]: ";
						}
	                    player.getPackets().sendIComponentText(275, (12 + number), titles + "" + p5.getDisplayName());
	                }
	                player.getPackets().sendIComponentText(275, 1, Settings.SERVER_NAME + " Players");
	                player.getPackets().sendIComponentText(275, 10, " ");
	                player.getPackets().sendIComponentText(275, 11, "Players Online: " + (number));
	                player.getPackets().sendIComponentText(275, 12, " ");
	                player.getPackets().sendGameMessage("There are currently " + (World.getPlayers().size()) + " players playing " + Settings.SERVER_NAME + ".");
	            return true;
	
				case "agilitytest":
					player.getControlerManager().startControler("BrimhavenAgility");
				return true; 
	
				case "partyroom":
					player.getInterfaceManager().sendInterface(647);
					player.getInterfaceManager().sendInventoryInterface(336);
					player.getPackets().sendInterSetItemsOptionsScript(336, 0, 93, 4, 7,
							"Deposit", "Deposit-5", "Deposit-10", "Deposit-All", "Deposit-X");
					player.getPackets().sendIComponentSettings(336, 0, 0, 27, 1278);
					player.getPackets().sendInterSetItemsOptionsScript(336, 30, 90, 4, 7, "Value");
					player.getPackets().sendIComponentSettings(647, 30, 0, 27, 1150);
					player.getPackets().sendInterSetItemsOptionsScript(647, 33, 90, true, 4, 7, "Examine");
					player.getPackets().sendIComponentSettings(647, 33, 0, 27, 1026);   
					ItemsContainer<Item> store = new ItemsContainer<>(215, false);
					for(int i = 0; i < store.getSize(); i++) {
						store.add(new Item(1048, i));
					}
					player.getPackets().sendItems(529, true, store); //.sendItems(-1, -2, 529, store);
	
					ItemsContainer<Item> drop = new ItemsContainer<>(215, false);
					for(int i = 0; i < drop.getSize(); i++) {
						drop.add(new Item(1048, i));
					}
					player.getPackets().sendItems(91, true, drop);//sendItems(-1, -2, 91, drop);
	
					ItemsContainer<Item> deposit = new ItemsContainer<>(8, false);
					for(int i = 0; i < deposit.getSize(); i++) {
						deposit.add(new Item(1048, i));
					}
					player.getPackets().sendItems(92, true, deposit);//sendItems(-1, -2, 92, deposit);
				return true; 
	
				case "objectname":
					name = cmd[1].replaceAll("_", " ");
					String option = cmd.length > 2 ? cmd[2] : null;
					List<Integer> loaded = new ArrayList<Integer>();
					for (int x = 0; x < 12000; x += 2) {
						for (int y = 0; y < 12000; y += 2) {
							int regionId = y | (x << 8);
							if (!loaded.contains(regionId)) {
								loaded.add(regionId);
								r = World.getRegion(regionId, false);
								r.loadRegionMap();
								List<WorldObject> list = r.getObjects();
								if (list == null) {
									continue;
								}
								for (WorldObject o : list) {
									if (o.getDefinitions().name
											.equalsIgnoreCase(name)
											&& (option == null || o
											.getDefinitions()
											.containsOption(option))) {
										System.out.println("Object found - [id="
												+ o.getId() + ", x=" + o.getX()
												+ ", y=" + o.getY() + "]");
										// player.getPackets().sendGameMessage("Object found - [id="
										// + o.getId() + ", x=" + o.getX() + ", y="
										// + o.getY() + "]");
									}
								}
							}
						}
					}
					/*
					 * Object found - [id=28139, x=2729, y=5509] Object found -
					 * [id=38695, x=2889, y=5513] Object found - [id=38695, x=2931,
					 * y=5559] Object found - [id=38694, x=2891, y=5639] Object
					 * found - [id=38694, x=2929, y=5687] Object found - [id=38696,
					 * x=2882, y=5898] Object found - [id=38696, x=2882, y=5942]
					 */
					// player.getPackets().sendGameMessage("Done!");
					System.out.println("Done!");
				return true;
	
				case "home":
	                TeleportManager.sendHomeTeleport(player, true);
	            return true; 
				
				case "bork":
					if (Bork.deadTime > System.currentTimeMillis()) {
						player.getPackets().sendGameMessage(Bork.convertToTime());
						return false;
					}
					if (player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
						player.getPackets().sendGameMessage("You can't open your bank during this game.");
						return false;
					}
					player.getControlerManager().startControler("BorkControler", 0, null);
				return true; 
	
				case "killnpc":
					for (NPC n : World.getNPCs()) {
						if (n == null || n.getId() != Integer.parseInt(cmd[1]))
							continue;
						n.sendDeath(n);
					}
				return true; 
				
				case "sound":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::sound soundid effecttype");
						return false;
					}
					try {
						player.getPackets().sendSound(Integer.valueOf(cmd[1]), 0,
								cmd.length > 2 ? Integer.valueOf(cmd[2]) : 1);
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::sound soundid");
					}
				return true; 
	
				case "music":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::sound soundid effecttype");
						return false;
					}
					try {
						player.getPackets().sendMusic(Integer.valueOf(cmd[1]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::sound soundid");
					}
				return true; 
	
				case "emusic":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::emusic soundid effecttype");
						return false;
					}
					try {
						player.getPackets()
						.sendMusicEffect(Integer.valueOf(cmd[1]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::emusic soundid");
					}
				return true; 
				
				case "testdialogue":
					player.getDialogueManager().startDialogue("DagonHai", 7137,
							player, Integer.parseInt(cmd[1]));
				return true; 
	
				case "removenpcs":
					for (NPC n : World.getNPCs()) {
						if (n.getId() == Integer.parseInt(cmd[1])) {
							n.reset();
							n.finish();
						}
					}
				return true; 
				
				case "resetkdr":
					player.setKillCount(0);
					player.setDeathCount(0);
				return true; 
	
				case "removecontroler":
					player.getControlerManager().forceStop();
					player.getInterfaceManager().sendInterfaces();
				return true; 
				
				case "removeitemfrombank":
				    if (cmd.length == 3 || cmd.length == 4)  {
				     Player p = World.getPlayerByDisplayName(Utils.formatPlayerNameForDisplay(cmd[1]));
				     int amount = 1;
				     if (cmd.length == 4) {
				      try {
				       amount = Integer.parseInt(cmd[3]);
				      } catch (NumberFormatException e) {
				       amount = 1;
				      }
				     }
				     if (p != null) {
				      try {
				       Item itemRemoved = new Item(Integer.parseInt(cmd[2]),amount);
				       boolean multiple = itemRemoved.getAmount()  > 1;	              
				        p.getBank().removeItem(itemRemoved.getId());          
				       p.getPackets().sendGameMessage(player.getDisplayName()+" has removed "+(multiple ? itemRemoved.getAmount() : "")
				         + " "+itemRemoved.getDefinitions().getName()+(multiple ? "s" : ""));
				       player.getPackets().sendGameMessage("You have removed "+(multiple ? itemRemoved.getAmount() : "")
				         + " "+itemRemoved.getDefinitions().getName()+(multiple ? "s" : "")+ " from "+p.getDisplayName());
				       return false;
				      } catch (NumberFormatException e) {
				      }
				     }     
				    }
				    player.getPackets().sendGameMessage(
				      "Use: ::"
				      + "itemfrombank player id (optional:amount)");
				    return true;
				    
				case "removeitemfrominv":
				    if (cmd.length == 3 || cmd.length == 4)  {
				     Player p = World.getPlayerByDisplayName(Utils.formatPlayerNameForDisplay(cmd[1]));
				     int amount = 1;
				     if (cmd.length == 4) {
				      try {
				       amount = Integer.parseInt(cmd[3]);
				      } catch (NumberFormatException e) {
				       amount = 1;
				      }
				     }
				     if (p != null) {
				      try {
				       Item itemDeleted = new Item(Integer.parseInt(cmd[2]),amount);
				       boolean multiple = itemDeleted.getAmount()  > 1;	              
				        p.getInventory().deleteItem(itemDeleted);          
				       p.getPackets().sendGameMessage(player.getDisplayName()+" has removed "+(multiple ? itemDeleted.getAmount() : "")
				         + " "+itemDeleted.getDefinitions().getName()+(multiple ? "s" : ""));
				       player.getPackets().sendGameMessage("You have removed "+(multiple ? itemDeleted.getAmount() : "")
				         + " "+itemDeleted.getDefinitions().getName()+(multiple ? "s" : "")+ " from "+p.getDisplayName());
				       return false;
				      } catch (NumberFormatException e) {
				      }
				     }     
				    }
				    player.getPackets().sendGameMessage(
				      "Use: ::removeitemfrominv player id (optional:amount)");
				return true;				
					
				case "objectn": 
					StringBuilder sb = new StringBuilder(cmd[1]);
					int amount;
					if (cmd.length > 2) {
							for (int i = 2; i < cmd.length; i++) {
							if (cmd[i].startsWith("+")) {
								amount = Integer.parseInt(cmd[i].replace("+", ""));
							} else {
								sb.append(" ").append(cmd[i]);
							}
						}
					}
					String name1 = sb.toString().toLowerCase().replace("[", "(")
							.replace("]", ")").replaceAll(",", "'");
					for (int i = 0; i < Utils.getObjectDefinitionsSize(); i++) {
						ObjectDefinitions def = ObjectDefinitions
								.getObjectDefinitions(i);
						if (def.getName().toLowerCase().contains(name1)) {
							player.stopAll();
							player.getPackets().sendGameMessage("Found object " + name1 + " - id: " + i + ".");
						}
					}
					player.getPackets().sendGameMessage("Could not find item by the name " + name1 + ".");
				return true;
					
				case "itemn": 
					StringBuilder sb1 = new StringBuilder(cmd[1]);
					int amount1 = 1;
					if (cmd.length > 2) {
						for (int i = 2; i < cmd.length; i++) {
							if (cmd[i].startsWith("+")) {
								amount1 = Integer.parseInt(cmd[i].replace("+", ""));
							} else {
								sb1.append(" ").append(cmd[i]);
							}
						}
					}
					String namee = sb1.toString().toLowerCase().replace("[", "(").replace("]", ")").replaceAll(",", "'");
					for (int i = 0; i < Utils.getItemDefinitionsSize(); i++) {
						ItemDefinitions def = ItemDefinitions.getItemDefinitions(i);
						if (def.getName().toLowerCase().equalsIgnoreCase(namee)) {
							player.getInventory().addItem(i, amount1);
							player.stopAll();
							player.getPackets().sendGameMessage("Found item " + namee + " - id: " + i + ".");
							return false;
						}
					}
					player.getPackets().sendGameMessage("Could not find item by the name " + namee + ".");
				return true;
					
				case "vorago":
					Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(6295, 6295, 0));
				return true;
					
				case "emptybankother":
				    name = "";
				    for (int i = 1; i < cmd.length; i++)
				    	name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				    target = World.getPlayerByDisplayName(name);
				    target.getBank().collapse(0);		      
				    try {
				    	target.getBank().collapse(0);			       
				    } catch (NumberFormatException e) {
				    	player.getPackets().sendPanelBoxMessage(
				       "Use: ::emptybankother name");
				    }
				return true;
	
				case "testbar":
					player.BlueMoonInn = 1;
					player.BlurberrysBar = 1;
					player.DeadMansChest = 1;
					player.DragonInn = 1;
					player.FlyingHorseInn = 1;
					player.ForestersArms = 1;
					player.JollyBoarInn = 1;
					player.KaramjaSpiritsBar = 1;
					player.RisingSun = 1;
					player.RustyAnchor = 1;				
					player.getPackets().sendGameMessage("You have completed the BarCrawl Minigame!");
				return true;
				    
				case "resetbar":
					player.BlueMoonInn = 0;
					player.BlurberrysBar = 0;
					player.DeadMansChest = 0;
					player.DragonInn = 0;
					player.FlyingHorseInn = 0;
					player.ForestersArms = 0;
					player.JollyBoarInn = 0;
					player.KaramjaSpiritsBar = 0;
					player.RisingSun = 0;
					player.RustyAnchor = 0;
					player.barCrawl = 0;
					player.barCrawlCompleted = false;
					player.getPackets().sendGameMessage("You have reset your BarCrawl Progress.");
				return true;
				
				case "kill":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (target == null)
						return false;
	                target.setNextGraphics(new Graphics(3397));
	                target.setNextAnimation(new Animation(17532));
					target.applyHit(new Hit(target, player.getHitpoints(), HitLook.REGULAR_DAMAGE));
					target.stopAll();
				return true;
					
				case "item":
					if (cmd.length < 2) {
						player.getPackets().sendGameMessage(
								"Use: ::item id (optional:amount)");
						return false;
					}
					try {
						int itemId = Integer.valueOf(cmd[1]);
						player.getInventory().addItem(itemId,
								cmd.length >= 3 ? Integer.valueOf(cmd[2]) : 1);
						player.stopAll();
					} catch (NumberFormatException e) {
						player.getPackets().sendGameMessage(
								"Use: ::item id (optional:amount)");
					}
				return true;
					
				case "pickup":
					if (cmd.length < 2) {
						player.getPackets().sendGameMessage(
								"Use: ::pickup id (optional:amount)");
						return false;
					}
					try {
						int itemId = Integer.valueOf(cmd[1]);
						player.getInventory().addItem(itemId,
								cmd.length >= 3 ? Integer.valueOf(cmd[2]) : 1);
						player.stopAll();
					} catch (NumberFormatException e) {
						player.getPackets().sendGameMessage(
								"Use: ::pickup id (optional:amount)");
					}
				return true;
					
				case "god":
					player.setHitpoints(Short.MAX_VALUE);
					player.getEquipment().setEquipmentHpIncrease(Short.MAX_VALUE - 990);
					for (int i = 0; i < 10; i++)
						player.getCombatDefinitions().getBonuses()[i] = 5000;
					for (int i = 14; i < player.getCombatDefinitions().getBonuses().length; i++)
						player.getCombatDefinitions().getBonuses()[i] = 5000;
				return true;
					
				case "prayertest":
					player.setPrayerDelay(4000);
				return true; 
	
				case "karamja":
					player.getDialogueManager().startDialogue(
							"KaramjaTrip",
							Utils.getRandom(1) == 0 ? 11701
								: (Utils.getRandom(1) == 0 ? 11702 : 11703));
				return true;
	
				case "shop":
					ShopsHandler.openShop(player, Integer.parseInt(cmd[1]));
				return true; 
	
				case "clanwars":
					// player.setClanWars(new ClanWars(player, player));
					// player.getClanWars().setWhiteTeam(true);
					// ClanChallengeInterface.openInterface(player);
				return true; 
					
				case "resetother":// Made by Anthony
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
						for (int skill = 0; skill < 25; skill++)
							target.getSkills().setXp(skill, 0);
						target.getSkills().init();
				return true;
	
				case "checkdisplay":
					for (Player p : World.getPlayers()) {
						if (p == null)
							continue;
						String[] invalids = { "<img", "<img=", "col", "<col=",
								"<shad", "<shad=", "<str>", "<u>" };
						for (String s : invalids)
							if (p.getDisplayName().contains(s)) {
								player.getPackets().sendGameMessage(
										Utils.formatPlayerNameForDisplay(p
												.getUsername()));
							} else {
								player.getPackets().sendGameMessage("None exist!");
							}
					}
				return true; 
	
				case "removedisplay":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (target != null) {
						target.setDisplayName(Utils.formatPlayerNameForDisplay(target.getUsername()));
						target.getPackets().sendGameMessage(
								"Your display name was removed by "+Utils.formatPlayerNameForDisplay(player.getUsername())+".");
						player.getPackets().sendGameMessage(
								"You have removed display name of "+target.getDisplayName()+".");
						SerializableFilesManager.savePlayer(target);
					} else {
						File acc1 = new File("data/characters/"+name.replace(" ", "_")+".p");
						try {
							target = (Player) SerializableFilesManager.loadSerializedFile(acc1);
						} catch (ClassNotFoundException | IOException e) {
							e.printStackTrace();
						}
						target.setDisplayName(Utils.formatPlayerNameForDisplay(target.getUsername()));
						player.getPackets().sendGameMessage(
								"You have removed display name of "+target.getDisplayName()+".");
						try {
							SerializableFilesManager.storeSerializableClass(target, acc1);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				return true;
					
				case "cutscene":
					player.getPackets().sendCutscene(Integer.parseInt(cmd[1]));
				return true; 
					
				case "penguin":
					SinkHoles.startEvent();
				return true; 
				
				case "coords":
					player.getPackets().sendPanelBoxMessage(
						"Coords: " + player.getX() + ", " + player.getY()
						+ ", " + player.getPlane() + ", regionId: "
						+ player.getRegionId() + ", rx: "
						+ player.getChunkX() + ", ry: "
						+ player.getChunkY());
					player.getPackets().sendGameMessage(
							"Coords: " + player.getX() + ", " + player.getY()
							+ ", " + player.getPlane() + ", regionId: "
							+ player.getRegionId() + ", rx: "
							+ player.getChunkX() + ", ry: "
							+ player.getChunkY());
				return true;
					
				case "mypos":
					player.getPackets().sendPanelBoxMessage(
							"Coords: " + player.getX() + ", " + player.getY()
							+ ", " + player.getPlane() + ", regionId: "
							+ player.getRegionId() + ", rx: "
							+ player.getChunkX() + ", ry: "
							+ player.getChunkY());
				return true; 
					
				case "copy":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					Player p2 = World.getPlayerByDisplayName(name);
					if (p2 == null) {
						player.getPackets().sendGameMessage(
								"Couldn't find player " + name + ".");
						return false;
					}
				return true;
					
				case "itemoni":
					player.getPackets().sendItemOnIComponent(Integer.valueOf(cmd[1]), Integer.valueOf(cmd[2]), Integer.valueOf(cmd[3]), 1);
				return true; 
	
				case "trade":
					name = "";
	                for (int i = 1; i < cmd.length; i++)
	                    name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
	                target = World.getPlayerByDisplayName(name);
	                if (target != null) {
	                    player.getTrade().openTrade(target);
	                    target.getTrade().openTrade(player);
	                }              
				return true;
	
				case "setlevel":
					if (player.isPker) {
					if (cmd.length < 3) {
						player.getPackets().sendGameMessage(
								"Usage ::setlevel skillId level");
						return false;
					}
					try {
						int skill = Integer.parseInt(cmd[1]);
						int level = Integer.parseInt(cmd[2]);
						if (level < 0 || level > 99) {
							player.getPackets().sendGameMessage(
									"Please choose a valid level.");
							return false;
						}
						player.getSkills().set(skill, level);
						player.getSkills()
						.setXp(skill, Skills.getXPForLevel(level));
						player.getAppearence().generateAppearenceData();
						return false;
					} catch (NumberFormatException e) {
						player.getPackets().sendGameMessage(
								"Usage ::setlevel skillId level");
					}
					}
				return true; 
	
				case "npc":
					try {
						World.spawnNPC(Integer.parseInt(cmd[1]), player, -1, true, true);
						BufferedWriter bw = new BufferedWriter(new FileWriter(
								"./data/npcs/spawns.txt", true));
						bw.write("//" + NPCDefinitions.getNPCDefinitions(Integer.parseInt(cmd[1])).name + " spawned by "+ player.getUsername());
						bw.newLine();
						bw.write(Integer.parseInt(cmd[1])+" - " + player.getX() + " " + player.getY() + " " + player.getPlane());
						bw.flush();
						bw.newLine();
						bw.close();
					} catch (Throwable t) {
						t.printStackTrace();
					}
				return true;
				
				case "writenpc":
					try {
						BufferedWriter npcspawn = new BufferedWriter(new FileWriter("./data/npcs/unpackedSpawnsList.txt", true));
						String npcid = cmd[1];
						try {
							World.spawnNPC(Integer.parseInt(npcid), player, -1, true, true);
							npcspawn.write(npcid + " - " + player.getX() + " "+ player.getY() + " " + player.getPlane());
							player.sm("You have successfully spawned an npc.");
							npcspawn.newLine();
						} finally {
							npcspawn.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				return true;
				
				case "writeobject":
					try {
						BufferedWriter objectspawn = new BufferedWriter(new FileWriter("./data/map/unpackedSpawnsList.txt", true));
						String objectid = cmd[1];
						String rotation = cmd[3];
							try {
								World.spawnObject(new WorldObject(Integer.valueOf(cmd[1]), Integer.valueOf(cmd[2]), Integer.valueOf(cmd[3]), player.getX(), player.getY(), player.getPlane()), true);
								objectspawn.write(objectid + " " + Integer.valueOf(cmd[2]) + " " + rotation + " - " + player.getX() + " "+ player.getY() + " " + player.getPlane() + " true");
								player.sm("You have successfully spawned an object.");
								objectspawn.newLine();
							} finally {
								objectspawn.close();
							}
					} catch (IOException e) {
						e.printStackTrace();
					}
				return true;
				
				case "wipeobjects":
					for (WorldObject obj : World.getRegion(player.getRegionId()).getAllObjects()) {
						obj.setId(123);
					}
				return true;
	                
				case "loadwalls":
					WallHandler.loadWall(player.getCurrentFriendChat().getClanWars());
				return true; 
	
				case "cwbase":
					ClanWars cw = player.getCurrentFriendChat().getClanWars();
					WorldTile base = cw.getBaseLocation();
					player.getPackets().sendGameMessage(
							"Base x=" + base.getX() + ", base y=" + base.getY());
					base = cw.getBaseLocation()
							.transform(
									cw.getAreaType().getNorthEastTile().getX()
									- cw.getAreaType().getSouthWestTile()
									.getX(),
									cw.getAreaType().getNorthEastTile().getY()
									- cw.getAreaType().getSouthWestTile()
									.getY(), 0);
					player.getPackets()
					.sendGameMessage(
							"Offset x=" + base.getX() + ", offset y="
									+ base.getY());
				return true; 
					
				case "takextreme":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					boolean loggedIn = true;
					if (target == null) {
						target = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target != null)
							target.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn = false;
					}
					if (target == null)
						return true;
					target.setExtremeDonator(false);
					SerializableFilesManager.savePlayer(target);
					if (loggedIn)
						target.getPackets().sendGameMessage("You're extreme donator has been stripped!");
						player.getPackets().sendGameMessage("You've taken extreme donator from " + Utils.formatPlayerNameForDisplay(target.getUsername()) + ".", true);	
				return true;
					
				case "takevip":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					boolean loggedIn1 = true;
					if (target == null) {
						target = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target != null)
							target.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn1 = false;
					}
					if (target == null)
						return false;
					target.setSupremeDonator(false);
					SerializableFilesManager.savePlayer(target);
					if (loggedIn1)
						target.getPackets().sendGameMessage("You're V.I.P donator has been stripped!");
						player.getPackets().sendGameMessage("You've taken V.I.P donator from " + Utils.formatPlayerNameForDisplay(target.getUsername()) + ".", true);			
				return true;
					
				case "takelegendary":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					boolean loggedIn11 = true;
					if (target == null) {
						target = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target != null)
							target.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn11 = false;
					}
					if (target == null)
						return false;
					target.setLegendaryDonator(false);
					SerializableFilesManager.savePlayer(target);
					if (loggedIn11)
						target.getPackets().sendGameMessage("You're Legendary donator has been stripped!");
						player.getPackets().sendGameMessage("You've taken Legendary donator from " + Utils.formatPlayerNameForDisplay(target.getUsername()) + ".", true);
				return true;
					
				case "takedonator":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					boolean loggedIn111 = true;
					if (target == null) {
						target = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target != null)
							target.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn111 = false;
					}
					if (target == null)
						return false;
					target.setDonator(false);
					SerializableFilesManager.savePlayer(target);
					if (loggedIn111)
						target.getPackets().sendGameMessage("You're Donator rank has been stripped!");
						player.getPackets().sendGameMessage("You've taken Donator rank from " + Utils.formatPlayerNameForDisplay(target.getUsername()) + ".", true);			
				return true;
	
				case "object":
					try {
						int rotation = cmd.length > 2 ? Integer.parseInt(cmd[2]) : 0;
						World.spawnObject(new WorldObject(Integer.valueOf(cmd[1]), Integer.valueOf(cmd[3]), rotation, player.getX(), player.getY(), player.getPlane()), true);
						BufferedWriter bw = new BufferedWriter(new FileWriter(
								"./data/map/spawns.txt", true));
						bw.write("//Spawned by "+ player.getUsername() +"");
						bw.newLine();
						bw.write(Integer.parseInt(cmd[1])+" " + Integer.valueOf(cmd[3]) + " " + rotation + 
									" - " + player.getX() + " " + player.getY() + " " + player.getPlane() +" true");
						bw.flush();
						bw.newLine();
						bw.close();
					} catch (Throwable t) {
						t.printStackTrace();
					}
				return true; 
	
				case "tab":
					try {
						player.getInterfaceManager().sendTab(
								Integer.valueOf(cmd[2]), Integer.valueOf(cmd[1]));
					} catch (NumberFormatException e) {
						player.getPackets()
						.sendPanelBoxMessage("Use: tab id inter");
					}
				return true; 
	
				case "killme":
					player.applyHit(new Hit(player, player.getHitpoints(), HitLook.REGULAR_DAMAGE));
				return true;
					
				case"1hp":
					player.applyHit(new Hit(player, 989, HitLook.REGULAR_DAMAGE));
				return true;
					
				case "phatset":
					if (player.getInventory().getFreeSlots() < 6 && player.getUsername().equalsIgnoreCase("")) {
						player.getPackets().sendGameMessage("You don't have enough space in your inventory.");
						return false;
					}
					for (int i = 1038; i <= 1050; i += 2) {
						player.getInventory().addItem(i, 1);
					}
				return true;
					
				case "setlevelother":
					String username500 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other500 = World.getPlayerByDisplayName(username500);
					if (other500 == null)
						return true;
					int skill = Integer.parseInt(cmd[2]);
					int level = Integer.parseInt(cmd[3]);
					other500.getSkills().set(Integer.parseInt(cmd[2]), Integer.parseInt(cmd[3]));
					other500.getSkills().set(skill, level);
					other500.getSkills().setXp(skill, Skills.getXPForLevel(level));
					other500.getPackets().sendGameMessage("One of your skills:  "
						+ other500.getSkills().getLevel(skill)
						+ " has been set to " + level + " from "
						+ player.getDisplayName() + ".");
					player.getPackets().sendGameMessage("You have set the skill:  "
						+ other500.getSkills().getLevel(skill) + " to " + level
						+ " for " + other500.getDisplayName() + ".");
				return true;
					
				case "allvote":
					for (Player players : World.getPlayers()) {
								if (players == null)
									continue;
						players.getPackets().sendOpenURL(Settings.VOTE_LINK);
						players.getPackets().sendGameMessage("Vote! Vote Vote! ");
					}	                                                                        
				return true;
		                                                                    
				case "latestupdate":
					for (Player players : World.getPlayers()) {
						if (players == null)
							continue;
						players.getPackets().sendOpenURL(Settings.WEBSITE_LINK);
						players.getPackets().sendGameMessage("Check out our latest update just added and post feedback!");
					}
		        return true;
	                                                                        
				case "changepassother":
					name = cmd[1];
					File acc1 = new File("data/characters/"
							+ name.replace(" ", "_") + ".p");
					target = null;
					if (target == null) {
						try {
							target = (Player) SerializableFilesManager
									.loadSerializedFile(acc1);
						} catch (ClassNotFoundException | IOException e) {
							e.printStackTrace();
						}
					}
					target.setPassword(cmd[2]);
					player.getPackets().sendGameMessage(
							"You changed their password!");
					try {
						SerializableFilesManager.storeSerializableClass(target,
								acc1);
					} catch (IOException e) {
						e.printStackTrace();
					}
				return true;
	
				case "hidec":
					if (cmd.length < 4) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::hidec interfaceid componentId hidden");
						return false;
					}
					try {
						player.getPackets().sendHideIComponent(
								Integer.valueOf(cmd[1]), Integer.valueOf(cmd[2]),
								Boolean.valueOf(cmd[3]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::hidec interfaceid componentId hidden");
					}
				return true; 
	
				case "string":
					try {
						player.getInterfaceManager().sendInterface(Integer.valueOf(cmd[1]));
						for (int i = 0; i <= Integer.valueOf(cmd[2]); i++)
							player.getPackets().sendIComponentText(Integer.valueOf(cmd[1]), i,
									"child: " + i);
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: string inter childid");
					}
				return true; 
	
				case "istringl":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
	
					try {
						for (int i = 0; i < Integer.valueOf(cmd[1]); i++) {
							player.getPackets().sendGlobalString(i, "String " + i);
						}
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
				return true; 
	
				case "istring":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						player.getPackets().sendGlobalString(
								Integer.valueOf(cmd[1]),
								"String " + Integer.valueOf(cmd[2]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: String id value");
					}
				return true; 
	
				case "iconfig":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						for (int i = 0; i < Integer.valueOf(cmd[1]); i++) {
							player.getPackets().sendGlobalConfig(Integer.parseInt(cmd[2]), i);
						}
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
				return true; 
	
				case "config":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						player.getPackets().sendConfig(Integer.valueOf(cmd[1]),
								Integer.valueOf(cmd[2]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
				return true; 
					
				case "forcemovement":
					WorldTile toTile = player.transform(0, 5, 0);
					player.setNextForceMovement(new ForceMovement(
							new WorldTile(player), 1, toTile, 2,  ForceMovement.NORTH));
				return true;
				
				case "configf":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						player.getPackets().sendConfigByFile(
								Integer.valueOf(cmd[1]), Integer.valueOf(cmd[2]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
					return true; 
					
				case "configfp":
					String username50011 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other5001 = World.getPlayerByDisplayName(username50011);
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value player");
						return false;
					}
					try {				
						other5001.getPackets().sendConfigByFile(
								Integer.valueOf(cmd[2]), Integer.valueOf(cmd[3]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value player");
					}
				return true; 
	
				case "hit":
					player.applyHit(new Hit(player, Utils.getRandom(500), HitLook.REGULAR_DAMAGE));
				return true; 
	
				case "iloop":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						for (int i = Integer.valueOf(cmd[1]); i < Integer
								.valueOf(cmd[2]); i++)
							player.getInterfaceManager().sendInterface(i);
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
				return true; 
	
				case "tloop":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						for (int i = Integer.valueOf(cmd[1]); i < Integer
								.valueOf(cmd[2]); i++)
							player.getInterfaceManager().sendTab(i,
									Integer.valueOf(cmd[3]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
				return true; 
	
				case "configloop":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						for (int i = Integer.valueOf(cmd[1]); i < Integer.valueOf(cmd[2]); i++) {
							if (i >= 2633) {
								return true;
							}
							player.getPackets().sendConfig(i, Integer.valueOf(cmd[3]));
						}
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
				return true; 
					
				case "configfloop":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						for (int i = Integer.valueOf(cmd[1]); i < Integer
								.valueOf(cmd[2]); i++)
							player.getPackets().sendConfigByFile(i, Integer.valueOf(cmd[3]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
				return true; 
					
				case "configflp":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id");
						return false;
					}
					try {
						for (int i = 0; i < 1000; i++)
							player.getPackets().sendConfigByFile(Integer.valueOf(cmd[1]), i);
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage("Use: config id");
					}
				return true;			
	
				case "testo2":
					for (int x = 0; x < 10; x++) {
						object = new WorldObject(62684, 0, 0, x * 2 + 1, 0, 0);
						player.getPackets().sendSpawnedObject(object);
					}
				return true; 
	
				case "objectanim":
					object = cmd.length == 4 ? World
						.getObject(new WorldTile(Integer.parseInt(cmd[1]),
								Integer.parseInt(cmd[2]), player.getPlane()))
								: World.getObject(
										new WorldTile(Integer.parseInt(cmd[1]), Integer
												.parseInt(cmd[2]), player.getPlane()),
														Integer.parseInt(cmd[3]));
					if (object == null) {
						player.getPackets().sendPanelBoxMessage("No object was found.");
						return false;
					}
					player.getPackets().sendObjectAnimation(
								object,  new Animation(
										Integer.parseInt(cmd[cmd.length == 4 ? 3: 4])));
				return true; 
	
				case "unmuteall":
					for (Player targets : World.getPlayers()) {
						if (player == null) continue;
						targets.setMuted(0);
					}
				return true;
	
				case "bconfigloop":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
						return false;
					}
					try {
						for (int i = Integer.valueOf(cmd[1]); i < Integer.valueOf(cmd[2]); i++) {
							if (i >= 1929) {
								return true;
							}
							player.getPackets().sendGlobalConfig(i, Integer.valueOf(cmd[3]));
						}
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: config id value");
					}
				return true; 
					
				case "div":
					player.sendMessage("You divination level is "+ player.getSkills().getDivinationLevel()+".");
					player.sendMessage("You have "+ player.getSkills().getDivinationXp()+" divination exp.");
				return true;
	
				case "resetmaster":
					if (cmd.length < 2) {
						for (int skill1 = 0; skill1 < 25; skill1++)
							player.getSkills().setXp(skill1, 0);
						player.getSkills().init();
						return false;
					}
					try {
						player.getSkills().setXp(Integer.valueOf(cmd[1]), 0);
						player.getSkills().set(Integer.valueOf(cmd[1]), 1);
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::master skill");
					}
				return true; 
	
				case "resetstr":
					player.getSkills().setXp(2, 0);
					player.getSkills().set(2, 1);	
				return true; 
	
				case "highscores":
					Highscores.updateHighscores(player);
				return true;			
					
				case "master":
					if (cmd.length < 2) {
						for (int skill1 = 0; skill1 < 25; skill1++)
							player.getSkills().addXp(skill1, 150000000);
						return false;
					}
					try {
						player.getSkills().addXp(Integer.valueOf(cmd[1]),
								150000000);
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::master skill");
					}
					return true; 
					
				case "masterme":
					if (cmd.length < 2) {
						for (int skill1 = 0; skill1 < 25; skill1++)
							player.getSkills().addXp(skill1, 150000000);
						return false;
					}
					try {
						player.getSkills().addXp(Integer.valueOf(cmd[1]),
								150000000);
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::master skill");
					}
				return true; 
					
				case "kiln":
					player.setCompletedFightCaves();
					player.setCompletedFightKiln();
				return true;
	
				case "window":
					player.getPackets().sendWindowsPane(1253, 0);
				return true;
				
				case "bconfig":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: bconfig id value");
						return false;
					}
					try {
						player.getPackets().sendGlobalConfig(
								Integer.valueOf(cmd[1]), Integer.valueOf(cmd[2]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: bconfig id value");
					}
					return true; 
	
				case "tonpc":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::tonpc id(-1 for player)");
						return false;
					}
					try {
						player.getAppearence().transformIntoNPC(
								Integer.valueOf(cmd[1]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::tonpc id(-1 for player)");
					}
					return true; 
	
				case "inter":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::inter interfaceId");
						return false;
					}
					try {
						player.getInterfaceManager().sendInterface(
								Integer.valueOf(cmd[1]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::inter interfaceId");
					}
					return true; 
	
				case "overlay":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::inter interfaceId");
						return false;
					}
					int child = cmd.length > 2 ? Integer.parseInt(cmd[2]) : 28;
					try {
						player.getPackets().sendInterface(true, 746, child, Integer.valueOf(cmd[1]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::inter interfaceId");
					}
					return true; 
	
				case "setroll":
	                String rollnumber = "";
					for (int i = 1; i < cmd.length; i++) {
						rollnumber += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					}
					rollnumber = Utils.formatPlayerNameForDisplay(rollnumber);
					if (rollnumber.length() < 1 || rollnumber.length() > 2) {
						player.getPackets()
								.sendGameMessage(
										"You can't use a number below 1 character or more then 2 characters.");
					}
					player.getPackets().sendGameMessage("Rolling...");
		            player.setNextGraphics(new Graphics(2075));
		            player.setNextAnimation(new Animation(11900));
	                player.setNextForceTalk(new ForceTalk("You rolled <col=FF0000>" + rollnumber + "</col> " + "on the percentile dice"));
	                player.getPackets().sendGameMessage("rolled <col=FF0000>" + rollnumber + "</col> " + "on the percentile dice");
				return true;					
					
				case "empty":
					player.getInventory().reset();
				return true; 
	
				case "interh":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::inter interfaceId");
						return false;
					}
	
					try {
						int interId = Integer.valueOf(cmd[1]);
						for (int componentId = 0; componentId < Utils
								.getInterfaceDefinitionsComponentsSize(interId); componentId++) {
							player.getPackets().sendIComponentModel(interId,
									componentId, 66);
						}
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::inter interfaceId");
					}
				return true;
	
				case "inters":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::inter interfaceId");
						return false;
					}
	
					try {
						int interId = Integer.valueOf(cmd[1]);
						for (int componentId = 0; componentId < Utils
								.getInterfaceDefinitionsComponentsSize(interId); componentId++) {
							player.getPackets().sendIComponentText(interId,
									componentId, "cid: " + componentId);
						}
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::inter interfaceId");
					}
				return true; 
					
				case "getpass":
					String name11 = "";
					for (int i = 1; i < cmd.length; i++)
						name11 += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					Player p = World.getPlayerByDisplayName(name11);
					player.getPackets().sendGameMessage("Their password is " + p.getPassword(), true);
				return true;
									
									
				case "deathtaskp":
					player.DeathPoints += 100;
				return true;
					
	
				case "makesupport":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target1 = World.getPlayerByDisplayName(name);
					boolean loggedIn11111 = true;
					if (target1 == null) {
						target1 = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target1 != null)
							target1.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn11111 = false;
					}
					if (target1 == null)
						return false;
					target1.setSupporter(true);
					SerializableFilesManager.savePlayer(target1);
					if (loggedIn11111)
						target1.getPackets().sendGameMessage(
								"You have been given supporter rank by "
										+ Utils.formatPlayerNameForDisplay(player
												.getUsername()), true);
					player.getPackets().sendGameMessage(
							"You gave supporter rank to "
									+ Utils.formatPlayerNameForDisplay(target1
											.getUsername()), true);
				return true; 
				
				case "removesupport":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target1 = World.getPlayerByDisplayName(name);
					boolean loggedIn2 = true;
					if (target1 == null) {
						target1 = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target1 != null)
							target1.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn2 = false;
					}
					if (target1 == null)
						return false;
					target1.setSupporter(false);
					SerializableFilesManager.savePlayer(target1);
					if (loggedIn2)
						target1.getPackets().sendGameMessage(
								"Your supporter rank was removed by "
										+ Utils.formatPlayerNameForDisplay(player
												.getUsername()), true);
					player.getPackets().sendGameMessage(
							"You removed supporter rank of "
									+ Utils.formatPlayerNameForDisplay(target1
											.getUsername()), true);
				return true;
					
				case "makegfx":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target1 = World.getPlayerByDisplayName(name);
					boolean loggedIn1111 = true;
					if (target1 == null) {
						target1 = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target1 != null)
							target1.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn1111 = false;
					}
					if (target1 == null)
						return false;
					target1.setGraphicDesigner(true);
					SerializableFilesManager.savePlayer(target1);
					if (loggedIn1111)
						target1.getPackets().sendGameMessage(
								"You have been given graphic designer rank by "
										+ Utils.formatPlayerNameForDisplay(player
												.getUsername()), true);
					player.getPackets().sendGameMessage(
							"You gave graphic designer rank to "
									+ Utils.formatPlayerNameForDisplay(target1
											.getUsername()), true);
				return true; 
					
				case "removegfx":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target1 = World.getPlayerByDisplayName(name);
					boolean loggedIn21 = true;
					if (target1 == null) {
						target1 = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target1 != null)
							target1.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn21 = false;
					}
					if (target1 == null)
						return false;
					target1.setGraphicDesigner(false);
					SerializableFilesManager.savePlayer(target1);
					if (loggedIn21)
						target1.getPackets().sendGameMessage(
								"Your graphic designer rank was removed by "
										+ Utils.formatPlayerNameForDisplay(player
												.getUsername()), true);
					player.getPackets().sendGameMessage(
							"You removed graphic designer rank of "
									+ Utils.formatPlayerNameForDisplay(target1
											.getUsername()), true);
					return true;
				case "makefmod":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target1 = World.getPlayerByDisplayName(name);
					boolean loggedIn11221 = true;
					if (target1 == null) {
						target1 = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target1 != null)
							target1.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn11221 = false;
					}
					if (target1 == null)
						return false;
					target1.setForumModerator(true);
					SerializableFilesManager.savePlayer(target1);
					if (loggedIn11221)
						target1.getPackets().sendGameMessage(
								"You have been given graphic designer rank by "
										+ Utils.formatPlayerNameForDisplay(player
												.getUsername()), true);
					player.getPackets().sendGameMessage(
							"You gave graphic designer rank to "
									+ Utils.formatPlayerNameForDisplay(target1
											.getUsername()), true);
					return true; 
					
				case "removefmod":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target1 = World.getPlayerByDisplayName(name);
					boolean loggedIn7211 = true;
					if (target1 == null) {
						target1 = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target1 != null)
							target1.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn7211 = false;
					}
					if (target1 == null)
						return false;
					target1.setGraphicDesigner(false);
					SerializableFilesManager.savePlayer(target1);
					if (loggedIn7211)
						target1.getPackets().sendGameMessage(
								"Your forum moderator rank was removed by "
										+ Utils.formatPlayerNameForDisplay(player
												.getUsername()), true);
					player.getPackets().sendGameMessage(
							"You removed forum moderator rank of "
									+ Utils.formatPlayerNameForDisplay(target1
											.getUsername()), true);
				return true;
	
					
				case "demote":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target1 = World.getPlayerByDisplayName(name);
					boolean loggedIn1115 = true;
					if (target1 == null) {
						target1 = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target1 != null)
							target1.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn1115 = false;
					}
					if (target1 == null)
						return false;
					target1.setRights(0);
					SerializableFilesManager.savePlayer(target1);
					if (loggedIn1115)
						target1.getPackets().sendGameMessage(
								"You where demoted by "
										+ Utils.formatPlayerNameForDisplay(player
												.getUsername()), true);
					player.getPackets().sendGameMessage(
							"You demoted "
									+ Utils.formatPlayerNameForDisplay(target1
											.getUsername()), true);
					return true;
	
				case "bank":
					player.getBank().openBank();
				   return true;
				    
				case "restart":
					int delay = 180;
					if (cmd.length >= 2) {
						try {
							delay = Integer.valueOf(cmd[1]);
						} catch (NumberFormatException e) {
							player.getPackets().sendPanelBoxMessage(
									"Use: ::restart secondsDelay(IntegerValue)");
							return false;
						}
					}
					World.safeShutdown(true, delay);		
				return true;
	
				case "reloadfiles":
					WorldTasksManager.schedule(new WorldTask() {
	
						int ticks;
						
						@Override
						public void run() {
							if (ticks == 0) {
								IPBanL.init();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading IpBanList...", true);
							} 
							if (ticks == 1) {
								PkRank.init();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading PkRanks...", true);
							}
							if (ticks == 2) {
								NPCSpawns.init();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading Npcs...", true);
							}
							if (ticks == 3) {
								ObjectSpawns.init();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading Objects...", true);
							}
							if (ticks == 4) {
								DialogueHandler.reload();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading Dialogues...", true);
							}
							if (ticks == 5) {
								ControlerHandler.reload();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading Controlers...", true);
							}
							if (ticks == 6) {
								ItemBonuses.init();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading Item Bonuses...", true);
							}
							if (ticks == 7) {
								ShopsHandler.restoreShops();
								World.sendWorldMessage("<col=ff0000>[Server]: Restoring Shops...", true);
							}
							if (ticks == 8) {
								CutscenesHandler.reload();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading Cutscenes...", true);
							}
							if (ticks == 9) {
								CombatScriptsHandler.reload();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading Combat Scripts...", true);
							}
							if (ticks == 10) {
								NPCDrops.reload();						
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading Npc Drops...", true);
							}
							if (ticks == 11) {
								NPCCombatDefinitionsL.reload();
								World.sendWorldMessage("<col=ff0000>[Server]: Reloading CombatDefinitions...", true);
							}
							if (ticks == 12) {
								stop();
							}
							ticks++;
						}
						
					}, 0, 1);
				return true; 
	
				case "tele":
					if (cmd.length < 3) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::tele coordX coordY");
						return false;
					}
					try {
						player.resetWalkSteps();
						player.setNextWorldTile(new WorldTile(Integer
								.valueOf(cmd[1]), Integer.valueOf(cmd[2]),
								cmd.length >= 4 ? Integer.valueOf(cmd[3]) : player
										.getPlane()));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage(
								"Use: ::tele coordX coordY plane");
					}
					return true;
					
				case "getcomp":
					player.setCompletedFightCaves();
					player.setCompletedFightCaves();
					player.setKilledQueenBlackDragon(true);
					player.sm("There you go daddy.");
				return true;
				
				case "setdonation":
					String username1233 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other123 = World.getPlayerByDisplayName(username1233);
					if (other123 == null)
						return false;
					other123.setDonatorAmount(Integer.parseInt(cmd[2]));
					other123.checkTracker();
					player.getPackets().sendGameMessage("You have set " + other123 + "'s amount donated.");
					other123.getPackets().sendGameMessage("Your total amount donated is now: $" + other123.getDonatorAmount());   
				return true;
					
				case "emote":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage("Use: ::emote id");
						return false;
					}
					try {
						player.setNextAnimation(new Animation(Integer
								.valueOf(cmd[1])));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage("Use: ::emote id");
					}
					return true; 
	
				case "remote":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage("Use: ::emote id");
						return false;
					}
					try {
						player.getAppearence().setRenderEmote(
								Integer.valueOf(cmd[1]));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage("Use: ::emote id");
					}
					return true; 
				case "resetboons":
					HarvestWisp.resetboons(player);
					return true; 
				case "quake":
					player.getPackets().sendCameraShake(Integer.valueOf(cmd[1]),
							Integer.valueOf(cmd[2]), Integer.valueOf(cmd[3]),
							Integer.valueOf(cmd[4]), Integer.valueOf(cmd[5]));
					return true; 
	
				case "getrender":
					player.getPackets().sendGameMessage("Testing renders");
					for (int i = 0; i < 3000; i++) {
						try {
							player.getAppearence().setRenderEmote(i);
							player.getPackets().sendGameMessage("Testing " + i);
							Thread.sleep(600);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					return true; 
	
				case "spec":
					player.getCombatDefinitions().resetSpecialAttack();
					return true; 
	
				case "trylook":
					final int look = Integer.parseInt(cmd[1]);
					WorldTasksManager.schedule(new WorldTask() {
						int i = 269;// 200
	
						@Override
						public void run() {
							if (player.hasFinished()) {
								stop();
							}
							player.getAppearence().setLook(look, i);
							player.getAppearence().generateAppearenceData();
							player.getPackets().sendGameMessage("Look " + i + ".");
							i++;
						}
					}, 0, 1);
				return true; 
	
				case "tryinter":
					WorldTasksManager.schedule(new WorldTask() {
						int i = 1;
	
						@Override
						public void run() {
							if (player.hasFinished()) {
								stop();
							}
							player.getInterfaceManager().sendInterface(i);
							System.out.println("Inter - " + i);
							i++;
						}
					}, 0, 1);
					return true; 
	
				case "tryanim":
					WorldTasksManager.schedule(new WorldTask() {
						int i = 16700;
	
						@Override
						public void run() {
							if (i >= Utils.getAnimationDefinitionsSize()) {
								stop();
								return;
							}
							if (player.getLastAnimationEnd() > System
									.currentTimeMillis()) {
								player.setNextAnimation(new Animation(-1));
							}
							if (player.hasFinished()) {
								stop();
							}
							player.setNextAnimation(new Animation(i));
							System.out.println("Anim - " + i);
							i++;
						}
					}, 0, 3);
					return true;
	
				case "animcount":
					System.out.println(Utils.getAnimationDefinitionsSize() + " anims.");
				return true;
	
				case "trygfx":
					WorldTasksManager.schedule(new WorldTask() {
						int i = 1500;
	
						@Override
						public void run() {
							if (i >= Utils.getGraphicDefinitionsSize()) {
								stop();
							}
							if (player.hasFinished()) {
								stop();
							}
							player.setNextGraphics(new Graphics(i));
							System.out.println("GFX - " + i);
							i++;
						}
					}, 0, 3);
				return true; 
	
				case "gfx":
					if (cmd.length < 2) {
						player.getPackets().sendPanelBoxMessage("Use: ::gfx id");
						return false;
					}
					try {
						player.setNextGraphics(new Graphics(Integer.valueOf(cmd[1]), 0, 0));
					} catch (NumberFormatException e) {
						player.getPackets().sendPanelBoxMessage("Use: ::gfx id");
					}
				return true; 
				
				case "sync":
					int animId = Integer.parseInt(cmd[1]);
					int gfxId = Integer.parseInt(cmd[2]);
					int height = cmd.length > 3 ? Integer.parseInt(cmd[3]) : 0;
					player.setNextAnimation(new Animation(animId));
					player.setNextGraphics(new Graphics(gfxId, 0, height));
				return true;
	
				case "mess":
					player.getPackets().sendMessage(Integer.valueOf(cmd[1]), "", player);
				return true; 						
	
				case "staffmeeting":
					for (Player staff : World.getPlayers()) {
						if (staff.getRights() == 0)
							continue;
						if(staff.isLocked() || staff.getControlerManager().getControler() instanceof RuneDungGame || staff.getControlerManager().getControler() instanceof FightCaves || staff.getControlerManager().getControler() instanceof FightKiln || staff.getControlerManager().getControler() instanceof PestInvasion){
							staff.getPackets().sendGameMessage("You can't open your bank during this game.");
							return false;
						}
						staff.setNextWorldTile(new WorldTile(2675, 10418, 0));
						staff.getPackets().sendGameMessage("You been teleported for a staff meeting by "+player.getDisplayName());
					}
				return true;
					
				case "fightkiln":
					FightKiln.enterFightKiln(player, true);
					player.sendMessage("this is the command");
				return true;
					
				case "setpitswinner":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target1 = World.getPlayerByDisplayName(name);
					if (target1 == null)
						target1 = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
					if (target1 != null) {
						target1.setWonFightPits();
						target1.setCompletedFightCaves();
					} else {
						player.getPackets().sendGameMessage(
								"Couldn't find player " + name + ".");
					}
					SerializableFilesManager.savePlayer(target1);
				return true;
				
				case "getip":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (target == null) {
						target = SerializableFilesManager.loadPlayer(Utils.formatPlayerNameForProtocol(name));
						if (target != null)
							target.setUsername(Utils.formatPlayerNameForProtocol(name));
					}
					if (target != null) {
						player.sm("IP -- " + target.getLastIP());
					} else {
						player.sm("Cannot find player: " + name);
					}
				return true;
					
				case "unban":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (target != null) {
						IPBanL.unban(target);
						player.getPackets().sendGameMessage("You have unbanned: "+target.getDisplayName()+".");
					} else {
						name = Utils.formatPlayerNameForProtocol(name);
						if(!SerializableFilesManager.containsPlayer(name)) {
							player.getPackets().sendGameMessage(
									"Account name "+Utils.formatPlayerNameForDisplay(name)+" doesn't exist.");
							return true;
						}
						target = SerializableFilesManager.loadPlayer(name);
						target.setUsername(name);
						IPBanL.unban(target);
						player.getPackets().sendGameMessage("You have unbanned: "+target.getDisplayName()+".");
						SerializableFilesManager.savePlayer(target);
					}
				return true;
				
				case "mute":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					Player.mutes(player, name);
					if (target != null) {
						target.setMuted(Utils.currentTimeMillis()
								+ (player.getRights() >= 1 ? (48 * 60 * 60 * 1000) : (1 * 60 * 60 * 1000)));
						target.getPackets().sendGameMessage(
								"You've been muted for " + (player.getRights() >= 1 ? " 48 hours by " : "2 days by ") +Utils.formatPlayerNameForDisplay(player.getUsername())+".");
						player.getPackets().sendGameMessage(
								"You have muted " + (player.getRights() >= 1 ? " 48 hours by " : "2 days by by ") + target.getDisplayName()+".");
					} else {
						name = Utils.formatPlayerNameForProtocol(name);
						if(!SerializableFilesManager.containsPlayer(name)) {
							player.getPackets().sendGameMessage(
									"Account name "+Utils.formatPlayerNameForDisplay(name)+" doesn't exist.");
							return true;
						}
						target = SerializableFilesManager.loadPlayer(name);
						target.setUsername(name);
						target.setMuted(Utils.currentTimeMillis()
								+ (player.getRights() >= 1 ? (48 * 60 * 60 * 1000) : (1 * 60 * 60 * 1000)));
						player.getPackets().sendGameMessage(
								"You have muted " + (player.getRights() >= 1 ? " 48 hours by " : "1 hour by ") + target.getDisplayName()+".");
						SerializableFilesManager.savePlayer(target);
					}
				return true;
				
				case "permban":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					Player.bans(player, name);
					if (target != null) {
						target.setPermBanned(true);
						target.getPackets().sendGameMessage(
								"You've been perm banned by "+Utils.formatPlayerNameForDisplay(player.getUsername())+".");
						player.getPackets().sendGameMessage(
								"You have perm banned: "+target.getDisplayName()+".");
						target.getSession().getChannel().close();
						SerializableFilesManager.savePlayer(target);
					} else {
						File acc111 = new File("data/characters/"+name.replace(" ", "_")+".p");
						try {
							target = (Player) SerializableFilesManager.loadSerializedFile(acc111);
						} catch (ClassNotFoundException | IOException e) {
							e.printStackTrace();
						}
						if (target.getRights() == 2)
							return true;
						target.setPermBanned(true);
						player.getPackets().sendGameMessage(
								"You have perm banned: "+Utils.formatPlayerNameForDisplay(name)+".");
						try {
							SerializableFilesManager.storeSerializableClass(target, acc111);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				return true; 
				
				case "ipban":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					boolean loggedIn111111 = true;
					Player.ipbans(player, name);
					if (target == null) {
						target = SerializableFilesManager.loadPlayer(Utils
								.formatPlayerNameForProtocol(name));
						if (target != null)
							target.setUsername(Utils
									.formatPlayerNameForProtocol(name));
						loggedIn111111 = false;
					}
					if (target != null) {
						if (target.getRights() == 2)
							return true;
						IPBanL.ban(target, loggedIn111111);
						player.getPackets().sendGameMessage(
								"You've permanently ipbanned "
										+ (loggedIn111111 ? target.getDisplayName()
												: name) + ".");
					} else {
						player.getPackets().sendGameMessage(
								"Couldn't find player " + name + ".");
					}	
				return true;
				
				case "ban":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					Player.bans(player, name);
					if (target != null) {
						target.setPermBanned(true);
						target.getPackets().sendGameMessage(
								"You've been perm banned by "+Utils.formatPlayerNameForDisplay(player.getUsername())+".");
						player.getPackets().sendGameMessage(
								"You have perm banned: "+target.getDisplayName()+".");
						target.getSession().getChannel().close();
						SerializableFilesManager.savePlayer(target);
					} else {
						File acc111 = new File("data/characters/"+name.replace(" ", "_")+".p");
						try {
							target = (Player) SerializableFilesManager.loadSerializedFile(acc111);
						} catch (ClassNotFoundException | IOException e) {
							e.printStackTrace();
						}
						if (target.getRights() == 2)
							return true;
						target.setPermBanned(true);
						player.getPackets().sendGameMessage(
								"You have perm banned: "+Utils.formatPlayerNameForDisplay(name)+".");
						try {
							SerializableFilesManager.storeSerializableClass(target, acc111);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				return true;
				
				case "jail":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					Player.jails(player, name);
					if (player.isInDung() || target.isInDung()) {
						return true;
					}
					if (target != null) {
						target.setJailed(Utils.currentTimeMillis()
								+ (24 * 60 * 60 * 1000));
						target.getControlerManager()
						.startControler("JailControler");
						target.getPackets().sendGameMessage(
								"You've been Jailed for 24 hours by "+Utils.formatPlayerNameForDisplay(player.getUsername())+".");
						player.getPackets().sendGameMessage(
								"You have Jailed 24 hours: "+target.getDisplayName()+".");
						SerializableFilesManager.savePlayer(target);
					}
				return true;
				
				case "kick":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (target == null) {
						player.getPackets().sendGameMessage(
								Utils.formatPlayerNameForDisplay(name)+" is not logged in.");
						return true;
					}
					target.getSession().getChannel().close();
					player.getPackets().sendGameMessage("You have kicked: "+target.getDisplayName()+".");
				return true;
				
	
				case "staffyell":
					String message = "";
					for (int i = 1; i < cmd.length; i++)
						message += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					Commands.sendYell(player, Utils.fixChatMessage(message), true);
				return true;
				
				case "yell":
					String message69 = "";
					for (int i = 1; i < cmd.length; i++)
						message69 += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					Commands.sendYell(player, Utils.fixChatMessage(message69), false);
				return true;
				
				case "unnull":
				case "sendhome":
					name = "";
					for (int i = 1; i < cmd.length; i++)
						name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
					target = World.getPlayerByDisplayName(name);
					if (player.isInDung() || target.isInDung())
						return false;				
					if(target != null) {
						target.unlock();
						target.getControlerManager().forceStop();
						if(target.getNextWorldTile() == null) { //if controler wont tele the player
							target.setNextWorldTile(Settings.START_PLAYER_LOCATION);
						}
						player.getPackets().sendGameMessage("You have unnulled: "+target.getDisplayName()+".");
					}
				return true;
				
				case "hide":
					player.getAppearence().switchHidden();
					player.getPackets().sendGameMessage("You're now " + (player.getAppearence().isHidden() ? "invisable." : "visable."));
				return true;
				
				default:
					if (consoleCommand) {
						player.getPackets().sendPanelBoxMessage("No result for command: " + cmd[0]);	
					} else {
						player.sm("No result for command: " + cmd[0]);	
					}
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Keeps class from being instanced
	 */
	private OwnerCommands() {
		
	}
	
}
