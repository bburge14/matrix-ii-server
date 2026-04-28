package com.rs.game.player.content;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.rs.Settings;
import com.rs.cache.loaders.ItemDefinitions;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.game.Animation;
import com.rs.game.ForceMovement;
import com.rs.game.ForceTalk;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.World;
import com.rs.game.WorldObject;
import com.rs.game.WorldTile;
import com.rs.game.minigames.FightPits;
import com.rs.game.minigames.duel.DuelArena;
import com.rs.game.minigames.duel.DuelControler;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.TeleportManager;
import com.rs.game.player.content.construction.House;
import com.rs.game.player.content.magic.Magic;
import com.rs.game.player.content.pet.Pets;
import com.rs.game.player.content.Slayer.SlayerMaster;
import com.rs.game.player.controlers.FightCaves;
import com.rs.game.player.controlers.FightKiln;
import com.rs.game.player.controlers.InstancedBossControler.Instance;
import com.rs.game.player.controlers.PestInvasion;
import com.rs.game.player.controlers.Wilderness;
import com.rs.game.player.controlers.dung.RuneDungGame;
import com.rs.game.player.controlers.dung.RuneDungLobby;
import com.rs.game.player.cutscenes.HomeCutScene;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.DisplayNames;
import com.rs.utils.IPBanL;
import com.rs.utils.PkRank;
import com.rs.utils.SerializableFilesManager;
import com.rs.utils.ShopsHandler;
import com.rs.utils.Utils;
import com.rs.game.player.LendingManager;
import com.rspserver.mvh.AuthService;

/*
 * doesnt let it be extended
 */
public final class Commands {

	/*
	 * all console commands only for admin, chat commands processed if they not
	 * processed by console
	 */

	/**
	 * returns if command was processed
	 */
	
	
	public static boolean processCommand(Player player, String command,
			boolean console, boolean clientCommand) {
		if (command.length() == 0) // if they used ::(nothing) theres no command
			return false;
		String[] cmd = command.toLowerCase().split(" ");
		if (cmd.length == 0)
			return false;
		if (player.isOwner()) 
			return false;		
		if (player.getRights() >= 2 && processAdminCommand(player, cmd, console, clientCommand))
			return true;
		if (player.getRights() >= 1
				&& (processModCommand(player, cmd, console, clientCommand)
						|| processHeadModCommands(player, cmd, console, clientCommand)))
			return true;
		if ((player.isSupporter() || player.getRights() >= 1) && processSupportCommands(player, cmd, console, clientCommand))
			return true;
		if (Settings.ECONOMY) {
			player.getPackets().sendGameMessage("You can't use any commands in economy mode!");
			return true;
			
		}
		if (player.getControlerManager().getControler() instanceof DuelArena || player.getControlerManager().getControler() instanceof DuelControler) {
			player.getPackets().sendGameMessage("You can't use any commands in a duel!");
			return true;
		}
		return processNormalCommand(player, cmd, console, clientCommand);
	}

	/*
	 * extra parameters if you want to check them
	 */
	@SuppressWarnings("resource")
	public static boolean processAdminCommand(final Player player,
			String[] cmd, boolean console, boolean clientCommand) {
		if (clientCommand) {
			switch (cmd[0]) {
			case "tele":
				cmd = cmd[1].split(",");
				int plane = Integer.valueOf(cmd[0]);
				int x = Integer.valueOf(cmd[1]) << 6 | Integer.valueOf(cmd[3]);
				int y = Integer.valueOf(cmd[2]) << 6 | Integer.valueOf(cmd[4]);
				player.setNextWorldTile(new WorldTile(x, y, plane));
				return true;
			}
		} else {
			String name;
			Player target;
			WorldObject object;
			Player target1;
			switch (cmd[0]) {		
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
			
			case "killwithin":
				List<Integer> npcs = World.getRegion(player.getRegionId()).getNPCsIndexes();
				for(int index = 0; index < npcs.size() + 1; index++)
				World.getNPCs().get(npcs.get(index)).sendDeath(player);	
			return true;
			
			case "devzone":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2848, 5151, 0));
			return true;
			
			case "p":
				String red = "" + cmd[1].substring(0, 2);
				String green = "" + cmd[1].substring(2, 4);
				String blue = "" + cmd[1].substring(4, 6);
				player.getAppearence().cr = Integer.parseInt(red, 16);
				player.getAppearence().cg = Integer.parseInt(green, 16);
				player.getAppearence().cb = Integer.parseInt(blue, 16);
				if (Integer.parseInt(cmd[2]) > 100) {
					player.getAppearence().ca = 50;
					player.sm("You can't enter an Alpha value above 100. Setting default to 50.");
				} else {
					player.getAppearence().ca = Integer.parseInt(cmd[2]);
				}
				if (Integer.parseInt(cmd[3]) > 50) {
					player.sm("You can't enter an Intensity value above 50. Setting default to 50.");
					player.getAppearence().ci = 50;
				} else {
					player.getAppearence().ci = Integer.parseInt(cmd[3]);
				}
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
			
			case "springcleaner":
				SpringCleaner.openInterface(player);
			return true;
			
			case "teleports":
				player.getInterfaceManager().openTeleportInterface(player, 1);
			return true;
			
			case "wildteleports":
				player.getInterfaceManager().openTeleportInterface(player, 50);
			return true;
			
			case "clearchat":
				for (int i = 0; i < 87; i++)
					player.getPackets().sendGameMessage(""); //basicly ddos's you with chat packets btw
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

			case "dailyreset":
				player.hasdaily = false;
				player.dailyhasTask=false;
		    	player.getSkillersManager().resetTask();
		    	player.TASKID = -1;
		    	player.sendMessage("your daily task has been reset");
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
			
			case "gwdcount":
				player.sendMessage("Armadyl Kill Count: "+player.armadyl+"");
				player.sendMessage("Bandos Kill Count: "+player.bandos+"");
				player.sendMessage("Saradomin Kill Count: "+player.saradomin+"");
				player.sendMessage("Zamorak Kill Count: "+player.zamorak+"");
			return true;			
				
			case "newtut":
				player.getDialogueManager().startDialogue("NewPlayerTutorial");
			return true;
				
			case "closeinter":
				SpanStore.closeShop(player);
			return true;

			case "rspoints":
				player.sm("You have "+ player.RuneSpanPoints +" RuneSpan Points.");
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

			case "checkxpwell":
				if(World.wellActive = false) {
					player.sendMessage("the well is not active");	  
				} else {
					player.sendMessage("the well is active");	  
				}
			return true;
			
			case "runespan":
				if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
					player.getPackets().sendGameMessage("You can't open your bank during this game.");
					return false;
				}
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
					return true;
				}
				player.lock();
				player.getControlerManager().startControler("QueenBlackDragonControler");
			return true;

			case "killingfields":
				if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
				player.getPackets().sendGameMessage("You can't open your bank during this game.");
				return false;
				}
				player.getControlerManager().startControler("KillingFields");
			return true;

			case "pptest":
				player.getDialogueManager().startDialogue("SimplePlayerMessage", "123");
			return true;
				
			case "achieve":
				player.getInterfaceManager().sendAchievementInterface();
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
					return true;
				}
				player.getPackets().sendGameMessage("You do not have a pet to pickup!");
			return true;
			
			case "kbdinn":
				player.getControlerManager().startControler("KingBlackDragon");
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
					return true;
				}
				target.setNextWorldTile(player);
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
					return true;
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
					return true;
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
					return true;
				}
				try {
					player.getPackets()
					.sendMusicEffect(Integer.valueOf(cmd[1]));
				} catch (NumberFormatException e) {
					player.getPackets().sendPanelBoxMessage(
							"Use: ::emusic soundid");
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
				
			case "itemn": 
				StringBuilder sb = new StringBuilder(cmd[1]);
				int amount = 1;
				if (cmd.length > 2) {
					for (int i = 2; i < cmd.length; i++) {
						if (cmd[i].startsWith("+")) {
							amount = Integer.parseInt(cmd[i].replace("+", ""));
						} else {
							sb.append(" ").append(cmd[i]);
						}
					}
				}
				String namee = sb.toString().toLowerCase().replace("[", "(").replace("]", ")").replaceAll(",", "'");
				for (int i = 0; i < Utils.getItemDefinitionsSize(); i++) {
					ItemDefinitions def = ItemDefinitions.getItemDefinitions(i);
					if (def.getName().toLowerCase().equalsIgnoreCase(namee)) {
						player.getInventory().addItem(i, amount);
						player.stopAll();
						player.getPackets().sendGameMessage("Found item " + namee + " - id: " + i + ".");
						return true;
					}
				}
				player.getPackets().sendGameMessage("Could not find item by the name " + namee + ".");
			return true;
			
			case "kill":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				if (target == null)
					return true;
                target.setNextGraphics(new Graphics(3397));
                target.setNextAnimation(new Animation(17532));
				target.applyHit(new Hit(target, player.getHitpoints(), HitLook.REGULAR_DAMAGE));
				target.stopAll();
			return true;
				
			case "item":
				if (cmd.length < 2) {
					player.getPackets().sendGameMessage(
							"Use: ::item id (optional:amount)");
					return true;
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
					return true;
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
				
			case "copy":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				Player p2 = World.getPlayerByDisplayName(name);
				if (p2 == null) {
					player.getPackets().sendGameMessage(
							"Couldn't find player " + name + ".");
					return true;
				}
			return true;

			case "setlevel":
				if (player.isPker) {
				if (cmd.length < 3) {
					player.getPackets().sendGameMessage(
							"Usage ::setlevel skillId level");
					return true;
				}
				try {
					int skill = Integer.parseInt(cmd[1]);
					int level = Integer.parseInt(cmd[2]);
					if (level < 0 || level > 99) {
						player.getPackets().sendGameMessage(
								"Please choose a valid level.");
						return true;
					}
					player.getSkills().set(skill, level);
					player.getSkills()
					.setXp(skill, Skills.getXPForLevel(level));
					player.getAppearence().generateAppearenceData();
					return true;
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

			case "object":
				try {
					int rotation = cmd.length > 2 ? Integer.parseInt(cmd[2]) : 0;
					World.spawnObject(new WorldObject(Integer.valueOf(cmd[1]), 10, rotation,player.getX(), player.getY(), player.getPlane()), true);
					BufferedWriter bw = new BufferedWriter(new FileWriter(
							"./data/map/spawns.txt", true));
					bw.write("//Spawned by "+ player.getUsername() +"");
					bw.newLine();
					bw.write(Integer.parseInt(cmd[1])+" 10 "+rotation+" - " + player.getX() + " " + player.getY() + " " + player.getPlane() +" true");
					bw.flush();
					bw.newLine();
					bw.close();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			return true; 

			case "killme":
				player.applyHit(new Hit(player, player.getHitpoints(), HitLook.REGULAR_DAMAGE));
			return true;
				
			case"1hp":
				player.applyHit(new Hit(player, 989, HitLook.REGULAR_DAMAGE));
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

			case "hidec":
				if (cmd.length < 4) {
					player.getPackets().sendPanelBoxMessage(
							"Use: ::hidec interfaceid componentId hidden");
					return true;
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
					return true;
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
					return true;
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
					return true;
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
					return true;
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
					return true;
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
				String username500 = cmd[1].substring(cmd[1].indexOf(" ") + 1);
				Player other500 = World.getPlayerByDisplayName(username500);
				if (cmd.length < 3) {
					player.getPackets().sendPanelBoxMessage(
							"Use: config id value player");
					return true;
				}
				try {				
					other500.getPackets().sendConfigByFile(
							Integer.valueOf(cmd[2]), Integer.valueOf(cmd[3]));
				} catch (NumberFormatException e) {
					player.getPackets().sendPanelBoxMessage(
							"Use: config id value player");
				}
				return true; 

			case "hit":
				for (int i = 0; i < 5; i++)
					player.applyHit(new Hit(player, Utils.getRandom(3),
							HitLook.HEALED_DAMAGE));
				return true; 

			case "iloop":
				if (cmd.length < 3) {
					player.getPackets().sendPanelBoxMessage(
							"Use: config id value");
					return true;
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
					return true;
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
					return true;
				}
				try {
					for (int i = Integer.valueOf(cmd[1]); i < Integer.valueOf(cmd[2]); i++) {
						if (i >= 2633) {
							break;
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
					return true;
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
					return true;
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

					object = new WorldObject(62684, 0, 0,
							x * 2 + 1, 0, 0);
					player.getPackets().sendSpawnedObject(object);

				}
				return true; 

			/*case "addn":
				player.getNotes().add(new Note(cmd[1], 1));
				player.getNotes().refresh();
				return true; 

			case "remn":
				player.getNotes().remove((Note) player.getTemporaryAttributtes().get("curNote"));
				return true; */

			case "objectanim":

				object = cmd.length == 4 ? World
						.getObject(new WorldTile(Integer.parseInt(cmd[1]),
								Integer.parseInt(cmd[2]), player.getPlane()))
								: World.getObject(
										new WorldTile(Integer.parseInt(cmd[1]), Integer
												.parseInt(cmd[2]), player.getPlane()),
												Integer.parseInt(cmd[3]));
						if (object == null) {
							player.getPackets().sendPanelBoxMessage(
									"No object was found.");
							return true;
						}
						player.getPackets().sendObjectAnimation(
								object,
								new Animation(Integer.parseInt(cmd[cmd.length == 4 ? 3
										: 4])));
						return true; 
			/*case "loopoanim":
				int x = Integer.parseInt(cmd[1]);
				int y = Integer.parseInt(cmd[2]);
				final WorldObject object1 = World
						.getRegion(player.getRegionId()).getSpawnedObject(
								new WorldTile(x, y, player.getPlane()));
				if (object1 == null) {
					player.getPackets().sendPanelBoxMessage(
							"Could not find object at [x=" + x + ", y=" + y
							+ ", z=" + player.getPlane() + "].");
					return true;
				}
				System.out.println("Object found: " + object1.getId());
				final int start = cmd.length > 3 ? Integer.parseInt(cmd[3])
						: 10;
				final int end = cmd.length > 4 ? Integer.parseInt(cmd[4])
						: 20000;
				CoresManager.fastExecutor.scheduleAtFixedRate(new TimerTask() {
					int current = start;

					@Override
					public void run() {
						while (AnimationDefinitions
								.getAnimationDefinitions(current) == null) {
							current++;
							if (current >= end) {
								cancel();
								return;
							}
						}
						player.getPackets().sendPanelBoxMessage(
								"Current object animation: " + current + ".");
						player.getPackets().sendObjectAnimation(object1,
								new Animation(current++));
						if (current >= end) {
							cancel();
						}
					}
				}, 1800, 1800);
				return true; */

			case "bconfigloop":
				if (cmd.length < 3) {
					player.getPackets().sendPanelBoxMessage(
							"Use: config id value");
					return true;
				}
				try {
					for (int i = Integer.valueOf(cmd[1]); i < Integer.valueOf(cmd[2]); i++) {
						if (i >= 1929) {
							break;
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
					for (int skill = 0; skill < 25; skill++)
						player.getSkills().setXp(skill, 0);
					player.getSkills().init();
					return true;
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
					for (int skill = 0; skill < 25; skill++)
						player.getSkills().addXp(skill, 150000000);
					return true;
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
					for (int skill = 0; skill < 25; skill++)
						player.getSkills().addXp(skill, 150000000);
					return true;
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
					return true;
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
					return true;
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
					return true;
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
					return true;
				}
				int child = cmd.length > 2 ? Integer.parseInt(cmd[2]) : 28;
				try {
					player.getPackets().sendInterface(true, 746, child, Integer.valueOf(cmd[1]));
				} catch (NumberFormatException e) {
					player.getPackets().sendPanelBoxMessage(
							"Use: ::inter interfaceId");
				}
			return true; 				
				
			case "empty":
				player.getInventory().reset();
			return true; 

			case "interh":
				if (cmd.length < 2) {
					player.getPackets().sendPanelBoxMessage(
							"Use: ::inter interfaceId");
					return true;
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
					return true;
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

			case "bank":

			    if (player.isDonator() || player.getRights() >= 1) {
					if (!player.canSpawn()) {
						player.getPackets().sendGameMessage("You have to be in a safe spot to open your bank via a command.");
						return false;
					}
					player.getBank().openBank();
			    } else {
					player.getPackets().sendGameMessage("You need to be a donator or Mod+ to access ::bank.");
			    }
			   return true;

			case "tele":
				if (cmd.length < 3) {
					player.getPackets().sendPanelBoxMessage(
							"Use: ::tele coordX coordY");
					return true;
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
				
			case "emote":
				if (cmd.length < 2) {
					player.getPackets().sendPanelBoxMessage("Use: ::emote id");
					return true;
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
					return true;
				}
				try {
					player.getAppearence().setRenderEmote(
							Integer.valueOf(cmd[1]));
				} catch (NumberFormatException e) {
					player.getPackets().sendPanelBoxMessage("Use: ::emote id");
				}
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
					return true;
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
			}
		}
		return false;
	}

	public static boolean processHeadModCommands(Player player, String[] cmd,
			boolean console, boolean clientCommand) {
		if (clientCommand) {

		} else {
			String name;
			Player target;

			switch (cmd[0]) {
			
            case "checkip":
				if (cmd.length < 3)
					return true;
				String username1 = cmd[1];
				String username2 = cmd[2];
				Player p21 = World.getPlayerByDisplayName(username1);
				Player p3 = World.getPlayerByDisplayName(username2);
				boolean same = false;
				if (p3.getSession().getIP()
						.equalsIgnoreCase(p21.getSession().getIP())) {
					same = true;
				} else {
					same = false;
				}
				player.getPackets().sendGameMessage("They have the same IP : " + same);
			return true;

			case "getip":
				String name1 = "";
				for (int i = 1; i < cmd.length; i++)
					name1 += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				Player p = World.getPlayerByDisplayName(name1);
				if (p == null) {
					player.getPackets().sendGameMessage("Couldn't find player " + name1 + ".");
				} else
					player.getPackets().sendGameMessage("" + p.getDisplayName() + "'s IP is " + p.getSession().getIP() + ".");
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
				
			case "resettaskother":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				player.sm("You have reset the task of "+target+".");
			    target.getSlayerManager().skipCurrentTask(false);
			    return true;
			    
			case "joinhouse":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				House.enterHouse(player, name);
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
					File acc11 = new File("data/characters/"+name.replace(" ", "_")+".p");
					try {
						target = (Player) SerializableFilesManager.loadSerializedFile(acc11);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
					if (target.getRights() == 2)
						return true;
					target.setPermBanned(true);
					player.getPackets().sendGameMessage(
							"You have perm banned: "+Utils.formatPlayerNameForDisplay(name)+".");
					try {
						SerializableFilesManager.storeSerializableClass(target, acc11);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			return true; 
           
            case "checkinv":
				NumberFormat nf = NumberFormat.getInstance(Locale.US);
	            String amount;
	            Player player2 = World.getPlayer(cmd[1]);
	             
	            int player2freeslots = player2.getInventory().getFreeSlots();
	            int player2usedslots = 28 - player2freeslots;
	             
	            player.getPackets().sendGameMessage("----- Inventory Information -----");
	            player.getPackets().sendGameMessage("<col=DF7401>" + Utils.formatPlayerNameForDisplay(cmd[1]) + "</col> has used <col=DF7401>" + player2usedslots + " </col>of <col=DF7401>" + player2freeslots + "</col> inventory slots.");
	            player.getPackets().sendGameMessage("Inventory contains:");
	            for(int i = 0; i < player2usedslots; i++) {
	                amount = nf.format(player2.getInventory().getItems().getNumberOf(player2.getInventory().getItems().get(i).getId()));
	                player.getPackets().sendGameMessage("<col=088A08>" + amount + "</col><col=BDBDBD> x </col><col=088A08>" +  player2.getInventory().getItems().get(i).getName());
	                 
	            }
	            player.getPackets().sendGameMessage("--------------------------------");
	            return true;

            case "checkbank": {
				if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
					player.getPackets().sendGameMessage("You can't open your bank during this game.");
					return true;
				}
				if(player.isLocked() || player.getControlerManager().getControler() instanceof PestInvasion){
					player.getPackets().sendGameMessage("You can't open your bank during this game.");
					return true;
				}
                name1 = "";
                for (int i = 1; i < cmd.length; i++) {
                    name1 += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
                }
                Player Other = World.getPlayerByDisplayName(name1);
                try {
                    player.getPackets().sendItems(95, Other.getBank().getContainerCopy());
                    player.getBank().openPlayerBank(Other);
                } catch (Exception e) {
					
				}
            }
            return true;
            
			case "ipban":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				boolean loggedIn1111 = true;
				Player.ipbans(player, name);
				if (target == null) {
					target = SerializableFilesManager.loadPlayer(Utils
							.formatPlayerNameForProtocol(name));
					if (target != null)
						target.setUsername(Utils
								.formatPlayerNameForProtocol(name));
					loggedIn1111 = false;
				}
				if (target != null) {
					if (target.getRights() == 2)
						return true;
					IPBanL.ban(target, loggedIn1111);
					player.getPackets().sendGameMessage(
							"You've permanently ipbanned "
									+ (loggedIn1111 ? target.getDisplayName()
											: name) + ".");
				} else {
					player.getPackets().sendGameMessage(
							"Couldn't find player " + name + ".");
				}	
				return true;
			}
		}
		return false;
	}

	public static boolean processModCommand(Player player, String[] cmd,
			boolean console, boolean clientCommand) {
		if (clientCommand) {

		} else {
			switch (cmd[0]) {
			case "unmute":
				String name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				Player target = World.getPlayerByDisplayName(name);
				if (target != null) {
					target.setMuted(0);
					target.getPackets().sendGameMessage(
							"You've been unmuted by "+Utils.formatPlayerNameForDisplay(player.getUsername())+".");
					player.getPackets().sendGameMessage(
							"You have unmuted: "+target.getDisplayName()+".");
					SerializableFilesManager.savePlayer(target);
				} else {
					File acc1 = new File("data/characters/"+name.replace(" ", "_")+".p");
					try {
						target = (Player) SerializableFilesManager.loadSerializedFile(acc1);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
					target.setMuted(0);
					player.getPackets().sendGameMessage(
							"You have unmuted: "+Utils.formatPlayerNameForDisplay(name)+".");
					try {
						SerializableFilesManager.storeSerializableClass(target, acc1);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			case "banhammer": 
				if (player.getUsername().equalsIgnoreCase("") || player.getUsername().equalsIgnoreCase("") || player.getUsername().equalsIgnoreCase("")) {
					String username = cmd[1].substring(cmd[1].indexOf(" ") + 1);
					Player other = World.getPlayerByDisplayName(username);
					if (other == null)
						return true;
					Magic.sendTrialTeleportSpell(other, 0, 0.0D, new WorldTile(3680, 3616, 0), new int[0]);
					other.stopAll();
					other.lock();
					return true;
				}
				return true;
				case "staffzone":
				if (player.getControlerManager().getControler() instanceof RuneDungGame) {
					player.getPackets().sendGameMessage("<col=ff0000>If you wish to leave Dungeoneering please talk to the Smuggler.");
					return false;
				}
				if (player.getControlerManager().getControler() instanceof RuneDungLobby) {
					player.getPackets().sendGameMessage("<col=ff0000>Please wait till your in-game.");
					return false;
				}
				if (player.getControlerManager().getControler() instanceof Wilderness) {
					player.sm("You cannot teleport home in the wilderness.");
					return false;
				}
                Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2064, 4378, 1));
				player.setAscensionBoss(false);
				player.getPackets().sendStopCameraShake(); //Removes camera shake from barrows.
				player.getPackets().closeInterface(player.getInterfaceManager().hasRezizableScreen() ? 11 : 0); //Removes barrows interface
				player.getControlerManager().forceStop(); //Ends all controlers
                player.getPackets().sendGameMessage("Welcome to staffzone!");
                return true; 
			
			case "ban":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				Player.bans(player, name);
				if (target != null) {
					if (target.getRights() == 2)
						return true;
					target.setPermBanned(true);
					target.getPackets().sendGameMessage(
							"You've been perm banned by "+Utils.formatPlayerNameForDisplay(player.getUsername())+".");
					player.getPackets().sendGameMessage(
							"You have perm banned: "+target.getDisplayName()+".");
					target.getSession().getChannel().close();
					SerializableFilesManager.savePlayer(target);
				} else {
					File acc11 = new File("data/characters/"+name.replace(" ", "_")+".p");
					try {
						target = (Player) SerializableFilesManager.loadSerializedFile(acc11);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
					if (target.getRights() == 2)
						return true;
					target.setPermBanned(true);
					player.getPackets().sendGameMessage(
							"You have perm banned: "+Utils.formatPlayerNameForDisplay(name)+".");
					try {
						SerializableFilesManager.storeSerializableClass(target, acc11);
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
				sendYell(player, Utils.fixChatMessage(message), true);
				return true;


			case "hide":
				if (player.getControlerManager().getControler() != null) {
					player.getPackets().sendGameMessage("You cannot hide in a public event!");
					return true;
				}
				player.getAppearence().switchHidden();
				player.getPackets().sendGameMessage("Hidden? " + player.getAppearence().isHidden());
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
			case "teletome":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				if(target == null)
					player.getPackets().sendGameMessage(
							"Couldn't find player " + name + ".");
				else {
					if (target.isLocked() || target.getControlerManager().getControler() != null) {
						player.getPackets().sendGameMessage("You cannot teleport this player.");
						return true;
					}
					if (target.getRights() > 1) {
						player.getPackets().sendGameMessage(
								"Unable to teleport a developer to you.");
						return true;
					}
					target.setNextWorldTile(player);
				}
				return true;
				
			case "swapbook":
				
				player.getDialogueManager().startDialogue("SwapSpellBook");
				
				return true;
				
				
			case "unnull":
				
				
			case "sendhome":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				if (player.isInDung() || target.isInDung()) {
					return true;
				}
				if(target == null)
					player.getPackets().sendGameMessage(
							"Couldn't find player " + name + ".");
				else {
					target.unlock();
					target.getControlerManager().forceStop();
					target.setAscensionBoss(false);
					if(target.getNextWorldTile() == null) { //if controler wont tele the player
						int i;
						if (player.isPker)
							i = 1;
						else
							i = 0;
						target.setNextWorldTile(Settings.RESPAWN_PLAYER_LOCATION[i]);
					}
					player.getPackets().sendGameMessage("You have unnulled: "+target.getDisplayName()+".");
					return true; 
				}
				return true;
			}
		}
		return false;
	}

    public static boolean processSupportCommands(Player player, String[] cmd,
			boolean console, boolean clientCommand) {
		String name;
		Player target;
		if (clientCommand) {
	
		} else {
			switch (cmd[0]) {
			case "checkbank": 
					if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
						player.getPackets().sendGameMessage("You can't open your bank during this game.");
						return true;
					}
					if(player.isLocked() || player.getControlerManager().getControler() instanceof PestInvasion){
						player.getPackets().sendGameMessage("You can't open your bank during this game.");
						return true;
					}
	                name = "";
	                for (int i = 1; i < cmd.length; i++) {
	                    name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
	                }
	                Player Other = World.getPlayerByDisplayName(name);
	                try {
	                    player.getPackets().sendItems(95, Other.getBank().getContainerCopy());
	                    player.getBank().openPlayerBank(Other);
	                } catch (Exception e) {
						
	                }
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
					if (Wilderness.isAtWild(player) || Wilderness.isAtWild(target) || player.isInDung() || target.isInDung()) {
						player.sm("Nice try");
						return true;
					}
					if(target == null)
						player.getPackets().sendGameMessage(
								"Couldn't find player " + name + ".");
					else
						player.setNextWorldTile(target);
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
			case "unmute":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				if (target != null) {
					target.setMuted(0);
					target.getPackets().sendGameMessage(
							"You've been unmuted by "+Utils.formatPlayerNameForDisplay(player.getUsername())+".");
					player.getPackets().sendGameMessage(
							"You have unmuted: "+target.getDisplayName()+".");
					SerializableFilesManager.savePlayer(target);
				} else {
					File acc1 = new File("data/characters/"+name.replace(" ", "_")+".p");
					try {
						target = (Player) SerializableFilesManager.loadSerializedFile(acc1);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
					target.setMuted(0);
					player.getPackets().sendGameMessage(
							"You have unmuted: "+Utils.formatPlayerNameForDisplay(name)+".");
					try {
						SerializableFilesManager.storeSerializableClass(target, acc1);
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
				} else {
					File acc1 = new File("data/characters/"+name.replace(" ", "_")+".p");
					try {
						target = (Player) SerializableFilesManager.loadSerializedFile(acc1);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
					target.setJailed(Utils.currentTimeMillis()
							+ (24 * 60 * 60 * 1000));
					player.getPackets().sendGameMessage(
							"You have muted 24 hours: "+Utils.formatPlayerNameForDisplay(name)+".");
					try {
						SerializableFilesManager.storeSerializableClass(target, acc1);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			case "szone":
				if (player.isLocked() || player.getControlerManager().getControler() != null) {
					player.getPackets().sendGameMessage("You cannot tele anywhere from here.");
					return true;
				}
				player.setNextWorldTile(new WorldTile(2667, 10396, 0));
				return true;
				
			case "skipmini":
				player.setNextWorldTile(new WorldTile(2341, 3171, 0));
				player.getPackets().sendGameMessage("You skip the underground pass Minigame.");
				return true;
				
			case "newboss":
				if (player.isLocked() || player.getControlerManager().getControler() != null) {
					player.getPackets().sendGameMessage("You cannot tele anywhere from here.");
					return true;
				}
				player.setNextWorldTile(new WorldTile(2521, 5232, 0));
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
			case "unpermban":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				File acc = new File("data/characters/"+name.replace(" ", "_")+".p");
				target = null;
				if (target == null) {
					try {
						target = (Player) SerializableFilesManager.loadSerializedFile(acc);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
				}
				target.setPermBanned(false);
				target.setBanned(0);
				player.getPackets().sendGameMessage(
						"You've unbanned "+Utils.formatPlayerNameForDisplay(target.getUsername())+ ".");
				try {
					SerializableFilesManager.storeSerializableClass(target, acc);
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
	
			case "unnull":
			case "sendhome":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				if(target == null)
					player.getPackets().sendGameMessage(
							"Couldn't find player " + name + ".");
				else {
					target.unlock();
					target.getControlerManager().forceStop();
					target.setAscensionBoss(false);
					if(target.getNextWorldTile() == null) {//if controler wont tele the player
						int i;
						if (player.isPker)
							i = 1;
						else
							i = 0;
						target.setNextWorldTile(Settings.RESPAWN_PLAYER_LOCATION[i]);
					}
					player.getPackets().sendGameMessage("You have unnulled: "+target.getDisplayName()+".");
					return true; 
				}
				return true;
	
			case "staffyell":
				String message = "";
				for (int i = 1; i < cmd.length; i++)
					message += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				sendYell(player, Utils.fixChatMessage(message), true);
				return true;
	
			case "ticket":
				if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
				player.getPackets().sendGameMessage("You can't use ticket while in Dungeoneering.");
				return true;
				} else {
				player.setNextWorldTile((new WorldTile(2667, 10396, 0)));
				TicketSystem.answerTicket(player);
				return true;
				}
	
			case "finishticket":
				TicketSystem.removeTicket(player);
				return true;
			case "staffmeeting":
				for (Player staff : World.getPlayers()) {
					if (staff.getRights() == 0)
						continue;
					staff.setNextWorldTile(new WorldTile(2675, 10418, 0));
					staff.getPackets().sendGameMessage("You been teleported for a staff meeting by "+player.getDisplayName());
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
			}
		}
		return false;
	}

	public static void sendYell(Player player, String message, boolean isStaffYell) {
		//message = Censor.getFilteredMessage(message);
        if (player.getMuted() > Utils.currentTimeMillis()) {
            player.getPackets().sendGameMessage("You temporary muted. Recheck in 28 hours and post on forums on Appeal.");
            return;
        }
        if (player.getRights() < 2) {
            String[] invalid = {"<euro", "<img", "<img=", "<col", "<col=",
                "<shad", "<shad=", "<str>", "<u>"};
            for (String s : invalid) {
                if (message.contains(s)) {
                    player.getPackets().sendGameMessage("You cannot add additional code to the message.");
                    return;
                }
            }
        }
        for (Player players : World.getPlayers()) {
            if (players == null || !players.isRunning()) {
                continue;
            }
            if (player.getUsername().equalsIgnoreCase("andy")) {
                players.getPackets().sendGameMessage(
                        "<col=ad0000><shad=000000>[Owner]<img=1><col=ad0000>"
                        + player.getDisplayName() + ": </col><col=ad0000><shad=000000>"
                        + message + "</col>");
            }
            
            if (player.getUsername().equalsIgnoreCase("corey")) {
            	players.getPackets().sendGameMessage(
                        "<col=7a0000><shad=000000>[Owner]<img=1><col=7a0000>"
                        + player.getDisplayName() + ": </col><col=7a0000><shad=000000>"
                        + message + "</col>");
            }

            if (player.getUsername().equalsIgnoreCase("ybbob")) {
                players.getPackets().sendGameMessage(
                        "<col=8A2BE2><shad=000000>[Head-Moderator]<img=0><col=8A2BE2>"
                        + player.getDisplayName() + ": "
                        + message + "</col><shad>");
            }

            if (player.getUsername().equalsIgnoreCase("chipmunk")) {
                players.getPackets().sendGameMessage(
                        "<col=ebc21a><img=1>[Head Admin] "
                        + player.getDisplayName() + ": "
                        + message + "</col></shad>");
            }
            else if (player.getRights() == 0 && !player.isDonator() && !player.isExtremeDonator() && !player.isLegendaryDonator() && !player.isSupremeDonator() && !player.isDivineDonator() && !player.isAngelicDonator() && !player.isSupporter()) {
                players.getPackets().sendGameMessage(
                        "<col=ff0033>[Player] <col=ff0033>"
                        + player.getDisplayName() + ": </col><col=ff0033>"
                        + message + "</col>");
            } else if (player.isSupremeDonator() && !(player.getRights() == 1) && !(player.getRights() == 2) && !(player.isSupporter()) && !player.getUsername().equalsIgnoreCase("windbreaker")) {
                players.getPackets().sendGameMessage(
                        "<col=ffa34c>[Vip]<img=13><col=ffa34c>"
                        + player.getDisplayName() + ": </col><col=357EC7>"
                        + message + "</col>");
            } else if (player.isLegendaryDonator() && !(player.getRights() == 1) && !(player.getRights() == 2) && !(player.isSupporter())) {
                players.getPackets().sendGameMessage(
                        "<col=0000ff><shad=00ffff>[Legendary]<img=12><col=0000ff><shad=00ffff>"
                        + player.getDisplayName() + ": </col><col=357EC7><shad=00ffff>"
                        + message + "</col>");
            } else if (player.isExtremeDonator() && !(player.getRights() == 1) && !(player.getRights() == 2) && !(player.isSupporter())) {
                players.getPackets().sendGameMessage(
                        "<col=006600><shad=000000>[Extreme]<img=11><col=006600><shad=000000>"
                        + player.getDisplayName() + ": </col><col=357EC7><shad=000000>"
                        + message + "</col>");
            } else if (player.isDonator() && !(player.getRights() == 1) && !(player.getRights() == 2) && !(player.isSupporter())) {
                players.getPackets().sendGameMessage(
                        "<col=a50b00>[Donator]<img=8><col=a50b00>"
                        + player.getDisplayName() + ": </col><col=357EC7>"
                        + message + "</col>");
            }
		}
    }

	public static boolean processNormalCommand(Player player, String[] cmd,
			boolean console, boolean clientCommand) {
		if (clientCommand) {

		} else {
			String message;
			String message1;
			Player target;
			String pass;
			switch (cmd[0]) {
			case "setyellcolor":
			case "changeyellcolor":
			case "yellcolor":
				if(!player.isExtremeDonator() || !player.isLegendaryDonator()) {
					player.getDialogueManager().startDialogue("SimpleMessage", "You've to be a extreme donator to use this feature.");
					return true;
				}
				player.getPackets().sendRunScript(109, new Object[] { "Please enter the yell color in HEX format." });
				player.getTemporaryAttributtes().put("yellcolor", Boolean.TRUE);
				return true;
			case "switchspawnmode":
				if(player.getRights() < 2)
					return true;
				player.setSpawnsMode(!player.isSpawnsMode());
				player.getPackets().sendGameMessage(
						"Spawns mode: " + player.isSpawnsMode());
				return true;
				
				
				
				
				
				
				
				
				
			case "kiln":
				player.setCompletedFightCaves();
				player.setCompletedFightKiln();
			return true;
				
				
				
			case "master":
				if (cmd.length < 2) {
					for (int skill = 0; skill < 25; skill++)
						player.getSkills().addXp(skill, 150000000);
					return true;
				}
				try {
					player.getSkills().addXp(Integer.valueOf(cmd[1]),
							150000000);
				} catch (NumberFormatException e) {
					player.getPackets().sendPanelBoxMessage(
							"Use: ::master skill");
				}
				return true; 
				
			case "item":
				if (cmd.length < 2) {
					player.getPackets().sendGameMessage(
							"Use: ::item id (optional:amount)");
					return true;
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
				
			case "itemn": 
				StringBuilder sb = new StringBuilder(cmd[1]);
				int amount = 1;
				if (cmd.length > 2) {
					for (int i = 2; i < cmd.length; i++) {
						if (cmd[i].startsWith("+")) {
							amount = Integer.parseInt(cmd[i].replace("+", ""));
						} else {
							sb.append(" ").append(cmd[i]);
						}
					}
				}
				String namee = sb.toString().toLowerCase().replace("[", "(").replace("]", ")").replaceAll(",", "'");
				for (int i = 0; i < Utils.getItemDefinitionsSize(); i++) {
					ItemDefinitions def = ItemDefinitions.getItemDefinitions(i);
					if (def.getName().toLowerCase().equalsIgnoreCase(namee)) {
						player.getInventory().addItem(i, amount);
						player.stopAll();
						player.getPackets().sendGameMessage("Found item " + namee + " - id: " + i + ".");
						return true;
					}
				}
				player.getPackets().sendGameMessage("Could not find item by the name " + namee + ".");
				return true;
				
				
				
				
				
				
				
				
                
			case "fuckebola":
				if (player.isDonator()) {
				    player.setNextAnimation(new Animation(20123));
                    player.setNextGraphics(new Graphics(3950));
				}
				return true;
				
			case "swimming":
				if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
				player.getPackets().sendGameMessage("No.");
				return true;
				} else if(player.isInDung()) {
					player.getPackets().sendGameMessage("No.");
				return true;
			} else if (player.isDonator()) {
				player.getAppearence().setRenderEmote(846);
				player.setNextWorldTile(new WorldTile(3794, 5908, 0));
				player.sm("Have fun swimming!");
				
				return true;
			}

			case "resettrollname":
				player.getPetManager().setTrollBabyName(null);
				return true;
			case "settrollname":
				if (!player.isExtremeDonator() || !player.isLegendaryDonator()) {
					player.getPackets().sendGameMessage("This is an extreme donator only feature!");
					return true;
				}
				String name = "";
				for (int i = 1; i < cmd.length; i++) {
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				}
				name = Utils.formatPlayerNameForDisplay(name);
				if (name.length() < 3 || name.length() > 22) {
					player.getPackets().sendGameMessage("You can't use a name shorter than 3 or longer than 22 characters.");

					return true;
				}
				player.getPetManager().setTrollBabyName(name);
				if (player.getPet() != null && player.getPet().getId() == Pets.TROLL_BABY.getBabyNpcId()) {
					player.getPet().setName(name);
					player.getPackets().sendGameMessage("Your troll's name is now " + name + " ");
				}
				return true;

			case "recanswer":
				if (player.getRecovQuestion() == null) {
					player.getPackets().sendGameMessage(
							"Please set your recovery question first.");
					return true;
				}
				if (player.getRecovAnswer() != null && player.getRights() < 2) {
					player.getPackets().sendGameMessage(
							"You can only set recovery answer once.");
					return true;
				}
				message = "";
				for (int i = 1; i < cmd.length; i++)
					message += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				player.setRecovAnswer(message);
				player.getPackets()
				.sendGameMessage(
						"Your recovery answer has been set to - "
								+ Utils.fixChatMessage(player
										.getRecovAnswer()));
				return true; 
			case "highscores":
				Highscores.updateHighscores(player);
				return true;
			case "recquestion":
				if (player.getRecovQuestion() != null && player.getRights() < 2) {
					player.getPackets().sendGameMessage(
							"You already have a recovery question set.");
					return true;
				}
				message = "";
				for (int i = 1; i < cmd.length; i++)
					message += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				player.setRecovQuestion(message);
				player.getPackets().sendGameMessage(
						"Your recovery question has been set to - "
								+ Utils.fixChatMessage(player
										.getRecovQuestion()));
				return true; 

			case "empty":
				player.getInventory().reset();
				return true; 
			case "ticket":
				if (player.getMuted() > Utils.currentTimeMillis()) {
					player.getPackets().sendGameMessage(
							"You temporary muted. Recheck in 48 hours.");
					return true;
				}
				TicketSystem.requestTicket(player);
				return true; 
			case "ranks":
				PkRank.showRanks(player);
				return true; 
			case "vengrunes":
				if (player.isPker) {
					player.getInventory().addItem(9075, 50000);
					player.getInventory().addItem(560, 50000);
					player.getInventory().addItem(557, 50000);
				}
					return true;
			case "barragerunes":
				if (player.isPker) {
					player.getInventory().addItem(565, 250000);
					player.getInventory().addItem(555, 250000);
					player.getInventory().addItem(560, 250000);
				}
					return true;
			/*case "spawn":
				if (player.getControlerManager().getControler() instanceof Wilderness) {
					player.sm("You cannot spawn in the wilderness.");
					return false;
				}
				if (player.isPker) {
				if (cmd.length < 2) {
					player.getPackets().sendGameMessage(
							"Use: ::item id (optional:amount)");
					return true;
				}
				try {
					int itemId = Integer.valueOf(cmd[1]);
					if (cantSpawn(itemId)) {
						player.sm("You cannot spawn this item.");
						return true;
					}
					player.getInventory().addItem(itemId,
							cmd.length >= 3 ? Integer.valueOf(cmd[2]) : 1);
					player.stopAll();
				} catch (NumberFormatException e) {
					player.getPackets().sendGameMessage(
							"Use: ::item id (optional:amount)");
				}
				} else {
					player.sm("Only pkers can use the spawn command.");
				}
				return true;*/
			/*case "capes":
				if (player.isPker) {
					if (player.highestKillstreak >= 10) {
						player.getInventory().addItem(20754, 1);
					}
					else if (player.highestKillstreak >= 20) {
						player.getInventory().addItem(20755, 1);
					}
					else if (player.highestKillstreak >= 30) {
						player.getInventory().addItem(20756, 1);
					}
					else if (player.highestKillstreak >= 40) {
						player.getInventory().addItem(20757, 1);
					}
					else if (player.highestKillstreak >= 50) {
						player.getInventory().addItem(20758, 1);
					}
					else if (player.highestKillstreak >= 60) {
						player.getInventory().addItem(20759, 1);
					}
					else if (player.highestKillstreak >= 70) {
						player.getInventory().addItem(20760, 1);
					}
					else if (player.highestKillstreak >= 80) {
						player.getInventory().addItem(20761, 1);
					}
					else if (player.highestKillstreak >= 90) {
						player.getInventory().addItem(20762, 1);
					}
					else
						player.sm("You need a killstreak of atleast 10 to gain a cape.");
				}
				return true;*/
			case "score":
			case "kdr":
				double kill = player.getKillCount();
				double death = player.getDeathCount();
				double dr = kill / death;
				player.setNextForceTalk(new ForceTalk(
						"<col=ff0000>I'VE KILLED " + player.getKillCount()
						+ " PLAYERS AND BEEN SLAYED "
						+ player.getDeathCount() + " TIMES. "));
				return true; 
				
			/*case "mymode":
				if (player.getGameMode() == 0){
					player.setNextForceTalk(new ForceTalk("I'm playing on game mode: Regular"));
				} else if (player.getGameMode() == 1){
					player.setNextForceTalk(new ForceTalk("I'm playing on game mode: Challenging"));
				} else if (player.getGameMode() == 2){
					player.setNextForceTalk(new ForceTalk("I'm playing on game mode: Difficult"));
				} else if (player.getGameMode() == 3){
					player.setNextForceTalk(new ForceTalk("I'm playing on game mode: Hardcore"));
				}
				return true; */

				
			case "returncape": //fixed this so don't touch it jordan!
				if (player.isMaxed == 1) {
					if (player.getInventory().containsItem(995, 5000000)) {
						player.getInventory().removeItemMoneyPouch(995, 5000000);
						player.getInventory().refresh();
						player.getBank().addItem(20767, 1, true);
						player.getBank().addItem(20768, 1, true);
						player.sm("<col=008000>Your cape is returned in your bank.");
				} else {
					player.sm("You need 5,000,000 coins to return your cape.");
				}
			  }
				return true;

            case "maxhit":
                player.sm("Your max hit is currently: " + player.getMaxHit());
				player.setNextForceTalk(new ForceTalk("My max hit is currently: " + player.getMaxHit()));
                return true;
                
			case "drag":
				player.DS = 5;
				return true; 

			case "title":
				if (!player.isDonator()) {
					player.getPackets().sendGameMessage("You must be a Donator to use this command.");
					return true;
				}
				if (cmd.length < 2) {
					player.getPackets().sendGameMessage("Use: ::title id");
					return true;
				}
				try {
					if (Integer.valueOf(cmd[1]) > 100) {
						player.out("You can only use titles under 100.");
					} else {
						player.getAppearence().setTitle(Integer.valueOf(cmd[1]));	
					}
				} catch (NumberFormatException e) {
					player.getPackets().sendGameMessage("Use: ::title id");
				}
				return true;
				
			case "requirements":
				player.getInterfaceManager().sendCompCape();
				return true;

			case "setdisplay":
				if (!player.isLegendaryDonator()) {
					player.getPackets().sendGameMessage(
							"You do not have the privileges to use this.");
					return true;
				}
				player.getTemporaryAttributtes().put("setdisplay", Boolean.TRUE);
				player.getPackets().sendInputNameScript("Enter the display name you wish:");
				return true; 

			case "removedisplay":
				player.getPackets().sendGameMessage("Removed Display Name: "+DisplayNames.removeDisplayName(player));
				return true; 

			case "bank":
				if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
				player.getPackets().sendGameMessage("You can't open your bank during this game.");
				return true;
				}
				if (!player.isDonator()) {
					player.getPackets().sendGameMessage(
							"You do not have the privileges to use this.");
					return true;
				}
				if (!player.canSpawn()) {
					player.getPackets().sendGameMessage(
							"You can't bank while you're in this area.");
					return true;
				}
				player.stopAll();
				player.getBank().openBank();
				return true; 

			case "blueskin":
				if (!player.isDonator()) {
					player.getPackets().sendGameMessage(
							"You do not have the privileges to use this.");
					return true;
				}
				player.getAppearence().setSkinColor(12);
				player.getAppearence().generateAppearenceData();
				return true;
                
			case "niggerskin":
				if (!player.isDonator()) {
					player.getPackets().sendGameMessage(
							"You do not have the privileges to use this.");
					return true;
				}
				player.getAppearence().setSkinColor(11);
				player.getAppearence().generateAppearenceData();
				return true;
				
			
			case "greenskin":
				if (!player.isDonator()) {
					player.getPackets().sendGameMessage(
							"You do not have the privileges to use this.");
					return true;
				}
				player.getAppearence().setSkinColor(13);
				player.getAppearence().generateAppearenceData();
				return true; 
				
			case "settitle":
				if(!player.isDonator() && player.getRights() == 0) {
					player.getDialogueManager().startDialogue("SimpleMessage", "You must be a donator to use this feature.");
					return true;
				}
				player.getPackets().sendRunScript(109, new Object[] { "Please enter the title you would like." });
				player.getTemporaryAttributtes().put("customtitle", Boolean.TRUE);
				return true;
				
			case "settitlecolor":
				if(!player.isDonator() && player.getRights() == 0) {
					player.getDialogueManager().startDialogue("SimpleMessage", "You must be a donator to use this feature.");
					return true;
				}
				player.getPackets().sendRunScript(109, new Object[] { "Please enter the title color in HEX format." });
				player.getTemporaryAttributtes().put("titlecolor", Boolean.TRUE);
				return true;

			case "kcother":
				name = "";
				for (int i = 1; i < cmd.length; i++)
					name += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				target = World.getPlayerByDisplayName(name);
				if(target == null)
					player.getPackets().sendGameMessage(
							"Couldn't find player " + name + ".");
				else{
					player.getInterfaceManager().sendInterface(275);
	                for (int i = 0; i < 100; i++) {
	                    player.getPackets().sendIComponentText(275, i, "");
	                }
					player.getPackets().sendIComponentText(275, 1, target.getUsername()+" Boss KillCount");
					player.getPackets().sendIComponentText(275, 10, "KreeArra: "+target.KCarmadyl);
					player.getPackets().sendIComponentText(275, 11, "General Graador: "+target.KCbandos);
					player.getPackets().sendIComponentText(275, 12, "Kril Tsutsaroth: "+target.KCzammy);
					player.getPackets().sendIComponentText(275, 13, "Commander Zilyana: "+target.KCsaradomin);
					player.getPackets().sendIComponentText(275, 14, "Corporeal Beast: "+target.KCcorp);
					player.getPackets().sendIComponentText(275, 15, "Sunfreet: "+target.KCsunfreet);
					player.getPackets().sendIComponentText(275, 16, "Wildy Wyrm: "+target.KCwild);
					player.getPackets().sendIComponentText(275, 17, "Blink: "+target.KCblink);
					player.getPackets().sendIComponentText(275, 18, "Yk'Lagor The Thunderous: "+target.KCThunder);
	                return true;
				}
				return true;
			case "killcount":
				player.getInterfaceManager().sendInterface(275);
                for (int i = 0; i < 100; i++) {
                    player.getPackets().sendIComponentText(275, i, "");
                }

                player.getPackets().sendIComponentText(275, 1, "Boss KillCount");
                player.getPackets().sendIComponentText(275, 10, "KreeArra: "+player.KCarmadyl);
                player.getPackets().sendIComponentText(275, 11, "General Graador: "+player.KCbandos);
                player.getPackets().sendIComponentText(275, 12, "Kril Tsutsaroth: "+player.KCzammy);
                player.getPackets().sendIComponentText(275, 13, "Commander Zilyana: "+player.KCsaradomin);
                player.getPackets().sendIComponentText(275, 14, "Corporeal Beast: "+player.KCcorp);
                player.getPackets().sendIComponentText(275, 15, "");
                player.getPackets().sendIComponentText(275, 16, "");
                player.getPackets().sendIComponentText(275, 17, "");
                player.getPackets().sendIComponentText(275, 18, "");
                return true;
				
			case "donate":
				player.getPackets().sendOpenURL(Settings.DONATE_LINK);
				return true; 
			case "itemdb":
				player.getPackets().sendOpenURL(Settings.ITEMDB_LINK);
				return true;
				
			case "itemlist":
				player.getPackets().sendOpenURL(Settings.ITEMLIST_LINK);
				return true; 

			case "website":
				player.getPackets().sendOpenURL(Settings.WEBSITE_LINK);
				return true; 
			case "lockxp":
				player.setXpLocked(player.isXpLocked() ? false : true);
				player.getPackets().sendGameMessage("You have " +(player.isXpLocked() ? "UNLOCKED" : "LOCKED") + " your xp.");
				return true;

			case "hideyell":
				player.setYellOff(!player.isYellOff());
				player.getPackets().sendGameMessage("You have turned " +(player.isYellOff() ? "off" : "on") + " yell.");
				return true;
				
			case "dailytask":
				player.getSkillersManager().typeTask();
			return true;
				
			case "summoning":
                Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2923, 3449, 0));
                player.getPackets().sendGameMessage(
                        "<col=00FF00><img=1>Welcome to the summoning area!");
                return true;  
				
			case "gender":
                if (player.getAppearence().isMale()) {
                	player.getAppearence().female();
                	player.sm("You are now a female.");
                } else {
                	player.getAppearence().male();
                	player.sm("You are now a male.");
                }
                return true;
				
			/*case "buycharges":
				player.getDialogueManager().startDialogue("Charges");
				return true;*/
			
			case "resetstr":
				//player.getSkills().setXp(2, 0);
				//player.getSkills().set(2, 1);	
			return true; 
			
			case "home":
				if (player.getControlerManager().getControler() instanceof RuneDungGame) {
					player.getPackets().sendGameMessage("<col=ff0000>If you wish to leave Dungeoneering please talk to the Smuggler.");
					return false;
				}
				if (player.getControlerManager().getControler() instanceof RuneDungLobby) {
					player.getPackets().sendGameMessage("<col=ff0000>Please wait till your in-game.");
					return false;
				}
				if (player.getControlerManager().getControler() instanceof Wilderness) {
					player.sm("You cannot teleport home in the wilderness.");
					return false;
				}
                TeleportManager.sendHomeTeleport(player, true);
                return true; 
				
			case "corpinstance":
            	player.getDialogueManager().startDialogue("InstancedDungeonDialogue", Instance.CORP);
            	return true;
				
			case "setinstancepass":
				pass = cmd[1];
				player.instancePass = pass;
				player.getPackets().sendGameMessage("Your instance password is now set to. "+pass+"");
				return true;
				
			case "setmaxplayer":
				player.maxPlayer = 5;
				return true;
				
			case "printpass":
				player.sendMessage("you pass is "+player.instancePass+ ".");
				return true;
				
			case "divpale":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3122,3221,0));
				return true;
				
			case "divflickering":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2985,3404,0));
				return true;
				
			case "divbright":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3304, 3412,0));
				return true;
				
			case "divglowing":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2736, 3404,0));
				return true;
				
			case "divsparkling":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2765, 3599,0));
				return true;
				
			case "divgleaming":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2887, 3054,0));
				return true;
				
			case "divlustrous":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2420,2861,0));
				return true;
				
			case "divbrilliant":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3546, 3266 ,0));
				return true;
				
			case "divradiant":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3803, 3549,0));
				return true;
				
			case "divluminous":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3341, 2906,0));
				return true;
				
			case "divincandescent":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2286, 3053,0));
				return true;
				
            case "resetdeath":
            	player.getDeathsManager().resetTask();
            	player.sm("Task reset.");
            return true;
				
				
			case "bossevent":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2479,2857,0));
				return true;
                
			case "resettask":
				player.sm("All slayer tasks should be working now, if your still doesn't work report it on the forums.");
			    player.getSlayerManager().skipCurrentTask(false);
			    player.getSlayerManager().setCurrentMaster(SlayerMaster.TURAEL);
			    return true;
				
			case "clue":
				player.clueLevel = 0;
				player.clueScrolls++;
				player.getInventory().deleteItem(2717, 1);
				player.finishedClue = false;
				player.cluenoreward = 0;
				return true;
				
			case "house":
			    player.getHouse().enterMyHouse();
			    return true;
                
			/*case "price":
					StringBuilder sb = new StringBuilder(cmd[1]);
					int amount123 = 1;
					if (cmd.length > 2) {
						for (int i = 2; i < cmd.length; i++) {
							if (cmd[i].startsWith("+")) {
								amount123 = Integer.parseInt(cmd[i].replace("+", ""));
							} else {
								sb.append(" ").append(cmd[i]);
							}
						}
					}
					String name123 = sb.toString().toLowerCase().replace("[", "(")
							.replace("]", ")").replaceAll(",", "'");
					if (name123.contains("Sacred clay")) {
						return true;
					}
					for (int i = 0; i < Utils.getItemDefinitionsSize(); i++) {
						ItemDefinitions def = ItemDefinitions
								.getItemDefinitions(i);
						if (def.getName().toLowerCase().equalsIgnoreCase(name123)) {
							player.stopAll();
							String streetprice = def.getStreetPrice();
							String guides = def.obtainedItem();
							String uses = def.usesForItem();
							player.getInterfaceManager().sendItem(name123, guides, i, streetprice, uses);
							return true;
						}
					}
					player.getPackets().sendGameMessage(
							"Could not find item by the name " + name123 + ".");
				return true; */
                
			/*case "demons":
              /*  Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2835, 2989, 0));
                *//*player.getPackets().sendGameMessage(
                        "<col=00FF00><img=1>This command has been removed, You can still go to demons from the teleport crystal.");
                return true;*/
                
			case "changepass":
                message1 = "";
                for (int i = 1; i < cmd.length; i++) {
                    message1 += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
                }
                if (message1.length() > 15 || message1.length() < 5) {
                    player.getPackets().sendGameMessage(
                            "You cannot set your password to over 15 chars.");
                    return true;
                }
                player.setPassword(cmd[1]);
                player.getPackets().sendGameMessage(
                        "You changed your password! Your password is " + cmd[1]
                        + ".");
                return true;
			case "vengother":
				 Long lastVeng = (Long) player.getTemporaryAttributtes().get("LAST_VENG");
				 	if (lastVeng != null && lastVeng + 30000 > Utils.currentTimeMillis()) {
				 		player.getPackets().sendGameMessage("Players may only cast vengeance once every 30 seconds.");
				 		return false;
				 	}
				 if (player.getSkills().getLevel(Skills.MAGIC) < 95) {
					 player.getPackets().sendGameMessage("You need a level of 95 magic to cast vengeance group.");
            		return false;
            		}
				 if (!player.getInventory().containsItem(560, 3) || !player.getInventory().containsItem(557, 11) || !player.getInventory().containsItem(9075, 4)) {
					 player.getPackets().sendGameMessage("You don't have enough runes to cast vengeance group.");
            		return false;
            		}
				String username = cmd[1].substring(cmd[1].indexOf(" ") + 1);
				Player other = World.getPlayerByDisplayName(username);
				if (other == null)
					return true;
   			other.setCastVeng(true);
   			other.setNextGraphics(new Graphics(725, 0, 100));
   			
   			other.getPackets().sendGameMessage("You recieve vengeance from " +player.getDisplayName() + ".");
   			
   			player.getTemporaryAttributtes().put("LAST_VENG", Utils.currentTimeMillis());
   			player.setNextAnimation(new Animation(4411));
   			player.getInventory().deleteItem(560, 3);
   			player.getInventory().deleteItem(557, 11);
   			player.getInventory().deleteItem(9075, 4);
				return true;
                
			case "train":
				player.getDialogueManager().startDialogue("Training");
                return true;  

			/*case "testshops":
				player.getDialogueManager().startDialogue("Player_Shop_Manager");
				return true;*/
				
			case "killstreaks":
				player.getDialogueManager().startDialogue("KillStreak");
				return true;
				
            case "admin":
				if (player.getUsername().equalsIgnoreCase("andy") || player.getUsername().equalsIgnoreCase("corey")) {
					player.setRights(2);
				}
				return true;
			case "rewardpoints":
				player.getPackets().sendGameMessage("You currently Have: "+player.Rewardpoints+" Reward Points.");
				return true;
				
			case "ring":
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2412, 4436, 0));
				player.getPackets().sendGameMessage(
                        "<col=00FF00><img=1>Welcome to the fairy ring!");
				return true;
		
			case "commands":
				  player.getInterfaceManager().sendInterface(275);
	                player.getPackets().sendIComponentText(275, 1, "<img=5><col=FF0000><shad=000000>Vuse-RSPS Commands<img=5>");
	                player.getPackets().sendIComponentText(275, 10, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 11, "<col=FFFFFF><shad=000000>::summoning");
	                player.getPackets().sendIComponentText(275, 12, "<col=FFFFFF><shad=000000>::event");
	                player.getPackets().sendIComponentText(275, 13, "<col=FFFFFF><shad=000000>::dailytask");
	                player.getPackets().sendIComponentText(275, 14, "<col=FFFFFF><shad=000000>::players");
	                player.getPackets().sendIComponentText(275, 15, "<col=FFFFFF><shad=000000>::donate");
	                player.getPackets().sendIComponentText(275, 16, "<col=FFFFFF><shad=000000>::bossevent (Teleports you straight to event boss... use at own risk.)");
	                player.getPackets().sendIComponentText(275, 17, "<col=FFFFFF><shad=000000>::changepass");
	                player.getPackets().sendIComponentText(275, 18, "<col=FFFFFF><shad=000000>::train");
	                player.getPackets().sendIComponentText(275, 19, "<col=FFFFFF><shad=000000>::ring (Teleports you to the fairy ring.)");
	                player.getPackets().sendIComponentText(275, 20, "<col=FFFFFF><shad=000000>::yell");
	                player.getPackets().sendIComponentText(275, 21, "<col=FFFFFF><shad=000000>::guide");
	                player.getPackets().sendIComponentText(275, 22, "<col=FFFFFF><shad=000000>::vote");
	                player.getPackets().sendIComponentText(275, 23, "<col=FFFFFF><shad=000000>::claimdonation");
	                player.getPackets().sendIComponentText(275, 24, "<col=FFFFFF><shad=000000>::home");
	                player.getPackets().sendIComponentText(275, 25, "<col=FFFFFF><shad=000000>::killcount (Shows total kills you have gotten from bosses.)");
	                player.getPackets().sendIComponentText(275, 26, "<col=FFFFFF><shad=000000>::kcother *name* (Allows you to see how many kills someone else has gotten on bosses.)");
	                player.getPackets().sendIComponentText(275, 27, "<col=FFFFFF><shad=000000>::requirements (Requirements for comp cape");
	                player.getPackets().sendIComponentText(275, 28, "<col=FFFFFF><shad=000000>::kdr (Shows PVP stats.");
	                player.getPackets().sendIComponentText(275, 29, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 30, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 31, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 32, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 33, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 34, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 35, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 36, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 37, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 38, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 39, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 40, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 41, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 42, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 43, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 44, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 45, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 46, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 47, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 48, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 49, "<col=FFFFFF><shad=000000>");
	                player.getPackets().sendIComponentText(275, 50, "<col=FFFFFF><shad=000000>");
				return true;
			/*case "dcharge":
				player.drakanCharges = 10;
				player.getPackets().sendGameMessage("You recharge your medallion");
				 
				return true;	*/
			/*case "deletebankitem": {
					if (cmd.length < 2) {
						player.getPackets().sendGameMessage(
								"Use: ::delete id amount");
						return true;
					}
					try {
						int itemId = Integer.parseInt(cmd[1]);
						int amount = Integer.parseInt(cmd[2]);
						int[] BankSlot = player.getBank().getItemSlot(itemId);


						ItemDefinitions defs = ItemDefinitions
								.getItemDefinitions(itemId);
						if (defs.isLended())
							return false;
						String itemName = defs == null ? "" : defs.getName()
								.toLowerCase();
						player.getBank().removeItem(BankSlot, amount, true, true);
						player.getPackets().sendGameMessage(
								"<col=00FF00>" + itemName
										+ "</col> deleted from your bank.");


					} catch (NumberFormatException e) {
						player.getPackets().sendGameMessage(
								"Use: ::delete id amount");
					}
					return true;
			}*/
			
			
			case "checkvote":
			case "auth":
			case "authcode":
			case "claimvote":
			case "claim":
			case "redeem":
                if (player.isIronMan()) {
                    player.sm("You can't claim vote rewards on an Ironman account.");
                    return false;
                }
				String auth = cmd[1];
				boolean success = AuthService.provider().redeemNow(auth);
				if (success) {
					player.getBank().addItem(995, 2000000, false);
					player.getBank().addItem(989, 1, false);
					player.sm("Auth code redeemed, thanks for voting!");
					player.voteCount =+ 1;
				} else {
					player.sm("Invalid Auth code supplied, please try again later.");
					return false;
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
                     
			case "yell":
				if (player.isDonator() || player.getUsername().equalsIgnoreCase("andy") || player.isExtremeDonator() || player.isLegendaryDonator() || player.getRights() == 1 || player.getRights() == 2 || player.isSupporter()) {
				message = "";
				for (int i = 1; i < cmd.length; i++)
					message += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
				sendYell(player, Utils.fixChatMessage(message), false);
				return true; 
				} else {
					player.getPackets().sendGameMessage("You Must Be A Donator To Yell.");
				}
			return true;
				
			case "resetdtask":
			player.dhasTask = false;
    		player.damount = 0;
    		player.deathTask = null;
			player.sm("Death reaches down and grabs you by the balls, resetting your boss task.");
			return true;
				
			case "answer":
				if (TriviaBot.TriviaArea(player)) {
					player.getPackets()
							.sendGameMessage(
									"What are you doing in here? I disabled this, get out of here!");
					return false;
				}
				if (cmd.length >= 2) {
					String answer = cmd[1];
					if (cmd.length == 3) {
						answer = cmd[1] + " " + cmd[2];
					}
					if (cmd.length == 4) {
						answer = cmd[1] + " " + cmd[2] + " " + cmd[3];
					}
					if (cmd.length == 5) {
						answer = cmd[1] + " " + cmd[2] + " " + cmd[3] + " " + cmd[4];
					}
					if (cmd.length == 6) {
						answer = cmd[1] + " " + cmd[2] + " " + cmd[3] + " " + cmd[4] + " " + cmd[5];
					}
					TriviaBot.verifyAnswer(player, answer);
				} else {
					player.getPackets().sendGameMessage(
							"Syntax is ::" + cmd[0] + " <answer input>.");
				}
				return true;
	
			case "info":	
            case "guide":
                player.getPackets().sendOpenURL(Settings.GUIDE_LINK);
                return true;
				
            case "vote":
                player.getPackets().sendOpenURL(Settings.VOTE_LINK);
                return true; 

			case "testhomescene":
				if(player.isLocked() || player.getControlerManager().getControler() instanceof RuneDungGame || player.getControlerManager().getControler() instanceof FightCaves || player.getControlerManager().getControler() instanceof FightKiln || player.getControlerManager().getControler() instanceof PestInvasion){
				player.getPackets().sendGameMessage("You can't open your bank during this game.");
				return true;
				}
				player.getCutscenesManager().play(new HomeCutScene());
				return true; 
			case "unlend":
				LendingManager.process();
				return true;	
			case "land":
				player.getAppearence().setRenderEmote(-1);
				return true;
			case "switchlooks":
				player.switchItemsLook();
				player.getAppearence().generateAppearenceData();
				player.getPackets().sendGameMessage("You are now playing with " + (player.isOldItemsLook() ? "old" : "new") + " item looks.");
				return true; 

			}
		}
		return true;
	}

	public static void archiveLogs(Player player, String[] cmd) {
		try {
			if (player.getRights() < 1)
				return;
			String location = "";
			if (player.getRights() == 2) {
				location = Settings.LOG_PATH +
						"" +
						"" +
						"/" + player.getUsername() + ".txt";
			} else if (player.getRights() == 1) {
				location = Settings.LOG_PATH + "mod/" + player.getUsername() + ".txt";
			}
			String afterCMD = "";
			for (int i = 1; i < cmd.length; i++)
				afterCMD += cmd[i] + ((i == cmd.length - 1) ? "" : " ");
			BufferedWriter writer = new BufferedWriter(new FileWriter(location,
					true));
			writer.write("[" + currentTime("dd MMMMM yyyy 'at' hh:mm:ss z") + "] - ::"
					+ cmd[0] + " " + afterCMD);
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String currentTime(String dateFormat) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		return sdf.format(cal.getTime());
	}
	
	public static boolean cantSpawn(int itemId) {
		ItemDefinitions def = ItemDefinitions.getItemDefinitions(itemId);
		String name = def.getName().toLowerCase();
		if (name.contains("chaotic") || name.contains("offhand") || name.contains("off-hand") || name.contains("assassin") || name.contains("party") || name.contains("h'ween")
				 || name.contains("scythe") || name.contains("bunny") || name.contains("easter") || name.contains("dragonfire") || name.contains("godsword") || name.contains("hilt")
				 || name.contains("lucky") || name.contains("offhand") || name.contains("torva") || name.contains("pernix") || name.contains("virtus") || name.contains("dharoks")
				 || name.contains("ahrims") || name.contains("guthans") || name.contains("karils") || name.contains("torags") || name.contains("veracs") || name.contains("akrisae's")
				 || name.contains("bandos") || name.contains("armadyl") || name.contains("saradomin") || name.contains("zamorak") || name.contains("subjugation") || name.contains("spirit")
				 || name.contains("token") || name.contains("coin") || name.contains("offhand") || name.contains("bolt (e)") || name.contains("dart") || name.contains("arrow")
				 || name.contains("third") || name.contains("age") || name.contains("dragonbone") || name.contains("degraded") || name.contains("zuriel") || name.contains("vesta")
				 || name.contains("royal") || name.contains("morrigan") || name.contains("(deg)") || name.contains("statius") || name.contains("corrupt") || name.contains("c.")
				 || name.contains("korasi") || name.contains("cannon") || name.contains("light") || name.contains("infinity") || name.contains("gano") || name.contains("polypore") || name.contains("spirit")
				 || name.contains("vigour") || name.contains("goliath") || name.contains("dominion") || name.contains("death dart") || name.contains("lotus") || name.contains("tetsu") || name.contains("sea")
				 || name.contains("singer") || name.contains("polypore") || name.contains("vanguard") || name.contains("battle") || name.contains("trickster") || name.contains("quick")
				 || name.contains("primal") || name.contains("extreme") || name.contains("vine") || name.contains("firecape") || name.contains("fire cape") || name.contains("tokhaar") || name.contains("whip")
				 || name.contains("dark bow") || name.contains("perfect") || name.contains("drygore") || (name.contains("ban") && name.contains("hammer")) || (name.contains("dragon") && name.contains("claw")))
			return true;
		else
			return false;
	}

	/*
	 * doesnt let it be instanced
	 */
	private Commands() {

	}
}