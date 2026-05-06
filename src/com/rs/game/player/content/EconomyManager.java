package com.rs.game.player.content;

import java.util.TimerTask;

import com.rs.Settings;
import com.rs.cache.loaders.ClientScriptMap;
import com.rs.executor.GameExecutorManager;
import com.rs.game.ForceTalk;
import com.rs.game.Graphics;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.item.Item;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.content.surpriseevents.SurpriseEvent;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.net.LoginClientChannelManager;
import com.rs.net.LoginProtocol;
import com.rs.net.encoders.LoginChannelsPacketEncoder;
import com.rs.utils.Logger;
import com.rs.utils.ShopsHandler;
import com.rs.utils.Utils;

public class EconomyManager {
	
	private static int[] ROOT_COMPONENTS = new int[] { 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
	private static int[] TEXT_COMPONENTS = new int[] { 38, 46, 54, 62, 70, 78, 86, 94, 102, 110, 118, 126, 134, 142, 150, 158, 166, 174, 182, 190 };
	private static int[] CLICK_COMPONENTS = new int[] { 35, 43, 51, 59, 67, 75, 83, 91, 99, 107, 115, 123, 131, 139, 147, 155, 163, 171, 179, 187 };

	private static String[] SHOPS_NAMES = new String[]
	{
		"General store",
		"Vote shop",
		"PKP shop 1",
		"PKP shop 2",
		"Weapons 1",
		"Weapons 2",
		"Melee armor",
		"Ranged armor",
		"Magic armor",
		"Food & Potions",
		"Runes",
		"Ammo",
		"Summoning items",
		"Capes",
		"Jewelry",
		"Quest items",
		"Skilling stuff 1",
		"Skilling stuff 2",
		"Heblore Secundaries 1",
		"Heblore Secundaries 2",
		"Back" };

	private static int[] SHOPS_IDS = new int[]
	{ 1200, 1201, 500, 501, 1202, 1203, 1205, 1207, 1208, 1209, 1210, 1211, 1212, 1213, 1214, 1215, 1216, 1217, 1218, 1219, -1 };

	public static int[] MANAGER_NPC_IDS = new int[]
	{ 13930, 15158 };

	/** Cached count of cache items that have an old-look variant - either
	 *  via cache opcodes 242-251 (def.hasOldLook()) OR via a name pair
	 *  (item starts with "retro " / "replica " AND a base item with the
	 *  remaining name exists). Computed lazily, cached for JVM lifetime. */
	private static int oldLookCount = -1;
	public static synchronized int countItemsWithOldLook() {
		if (oldLookCount >= 0) return oldLookCount;
		int n = 0;
		try {
			int max = Math.min(50000, com.rs.utils.Utils.getItemDefinitionsSize());
			java.util.Map<String, Integer> nameToId = new java.util.HashMap<>();
			for (int id = 0; id < max; id++) {
				if (!com.rs.utils.Utils.itemExists(id)) continue;
				com.rs.cache.loaders.ItemDefinitions def =
					com.rs.cache.loaders.ItemDefinitions.getItemDefinitions(id);
				if (def == null || def.isNoted()) continue;
				String name = def.getName();
				if (name == null || name.isEmpty()
						|| name.equalsIgnoreCase("null")) continue;
				nameToId.putIfAbsent(name.toLowerCase(), id);
			}
			for (int id = 0; id < max; id++) {
				if (!com.rs.utils.Utils.itemExists(id)) continue;
				com.rs.cache.loaders.ItemDefinitions def =
					com.rs.cache.loaders.ItemDefinitions.getItemDefinitions(id);
				if (def == null || def.isNoted()) continue;
				String name = def.getName();
				if (name == null || name.isEmpty()) continue;
				if (def.hasOldLook()) { n++; continue; }
				String lower = name.toLowerCase();
				String[] prefixes = { "retro ", "replica " };
				for (String p : prefixes) {
					if (lower.startsWith(p)) {
						String tail = lower.substring(p.length()).trim();
						Integer bid = nameToId.get(tail);
						if (bid != null && bid != id) { n++; break; }
					}
				}
			}
		} catch (Throwable ignored) {}
		oldLookCount = n;
		return n;
	}
	public static String[] MANAGER_NPC_TEXTS = new String[]
	{ "I seek the evil power!", "I smell the darkness...", "I sense the darkness...", "Evil forces are getting stronger...", "Come to me, traveler!" };

	private static String[] NEWBIE_LOC_NAMES = new String[]
	{ "Stronghold of security", "Karamja & Crandor", "Rock Crabs", "Back" };
	private static WorldTile[] NEWBIE_LOCATIONS = new WorldTile[]
	{ new WorldTile(3080, 3418, 0), new WorldTile(2861, 9570, 0), new WorldTile(2674, 3710, 0), null };

	private static String[] CITIES_NAMES = new String[]
	{
		"Lumbridge",
		"Varrock",
		"Edgeville",
		"Falador",
		"Seer's village",
		"Ardougne",
		"Yannile",
		"Keldagrim",
		"Dorgesh-Kan",
		"Lletya",
		"Etceteria",
		"Daemonheim",
		"Canifis",
		"Tzhaar area",
		"Burthrope",
		"Al-Kharid",
		"Draynor village",
		"Zanaris",
		"Shilo village",
		"Darkmeyer",
		"Back" };
	private static WorldTile[] CITIES_LOCATIONS = new WorldTile[]
	{
		new WorldTile(3222, 3219, 0),
		new WorldTile(3212, 3422, 0),
		new WorldTile(3094, 3502, 0),
		new WorldTile(2965, 3386, 0),
		new WorldTile(2725, 3491, 0),
		new WorldTile(2662, 3305, 0),
		new WorldTile(2605, 3093, 0),
		new WorldTile(2845, 10210, 0),
		new WorldTile(2720, 5351, 0),
		new WorldTile(2341, 3171, 0),
		new WorldTile(2614, 3894, 0),
		new WorldTile(3450, 3718, 0),
		new WorldTile(3496, 3489, 0),
		new WorldTile(4651, 5151, 0),
		new WorldTile(2889, 3528, 0),
		new WorldTile(3275, 3166, 0),
		new WorldTile(3079, 3250, 0),
		new WorldTile(2386, 4458, 0),
		new WorldTile(2849, 2958, 0),
		new WorldTile(3613, 3371, 0),
		null };

	private static String[] DUNGEON_NAMES = new String[]
	{
		"God Wars",
		"King black dragon",
		"Corporeal beast",
		"Tormented demons",
		"Stronghold of security",
		"Karamja & Crandor",
		"Brimhaven dungeon",
		"TzHaar",
		"Jungle Strykewyrms",
		"Desert Skrykewyrms",
		"Ice Strykewyrms",
		"Kalphite hive",
		"Asgarnia ice dungeon",
		"Mos le harmless jungle",
		"Gorak",
		"Lumbridge swamp caves",
		"Grotworm lair (QBD)",
		"Framenik slayer dungeon",
		"Back" };
	private static WorldTile[] DUNGEON_LOCATIONS = new WorldTile[]
	{
		new WorldTile(2908, 3707, 0),
		new WorldTile(3051, 3519, 0),
		new WorldTile(2966, 4383, 2),
		new WorldTile(2562, 5739, 0),
		new WorldTile(3080, 3418, 0),
		new WorldTile(2861, 9570, 0),
		new WorldTile(2745, 3152, 0),
		new WorldTile(4673, 5116, 0),
		new WorldTile(2450, 2898, 0),
		new WorldTile(3381, 3162, 0),
		new WorldTile(3508, 5516, 0),
		new WorldTile(3228, 3106, 0),
		new WorldTile(3010, 3150, 0),
		new WorldTile(3731, 3039, 0),
		new WorldTile(3035, 5346, 0),
		new WorldTile(3169, 3171, 0),
		new WorldTile(2990, 3237, 0),
		new WorldTile(2794, 3615, 0),
		null };

	private static String[] MINIGAMES_NAMES = new String[]
	{
		"Duel arena",
		"Dominion tower",
		"God Wars",
		"Barrows",
		"Fight pits",
		"Fight caves",
		"Kiln",
		"Puro-puro",
		"Clan wars & Stealing creations",
		"High & Low runespan",
		"Sorceror's garden",
		"Crucible",
		"Pest Control",
		"Back" };
	private static WorldTile[] MINIGAMES_LOCATIONS = new WorldTile[]
	{
		new WorldTile(3370, 3270, 0),
		new WorldTile(3361, 3082, 0),
		new WorldTile(2857, 3573, 0),
		new WorldTile(3565, 3306, 0),
		new WorldTile(4602, 5062, 0),
		new WorldTile(4615, 5129, 0),
		new WorldTile(4743, 5170, 0),
		new WorldTile(2428, 4441, 0),
		new WorldTile(2961, 9675, 0),
		new WorldTile(3106, 3160, 0),
		new WorldTile(3323, 3139, 0),
		new WorldTile(3120, 3519, 0),
		new WorldTile(2659, 2676, 0),
		null };

	private static String[] OTHER_NAMES = new String[]
	{ "Mages bank", "Multi pvp (Wilderness)", "Wests (Wilderness)", "Easts (Wilderness)", "Oracle of darkness (Wilderness)", "Back" };
	private static WorldTile[] OTHER_LOCATIONS = new WorldTile[]
	{ new WorldTile(2538, 4715, 0), new WorldTile(3240, 3611, 0), new WorldTile(2984, 3596, 0), new WorldTile(3360, 3658, 0), new WorldTile(3194, 3922, 0), null };

	/**
	 * Whether task was submitted.
	 */
	private static boolean eventTaskSubmitted;
	/**
	 * Current surprise event.
	 */
	private static SurpriseEvent surpriseEvent;
	/**
	 * Whether event is happening.
	 */
	private static boolean tileEventHappening;
	/**
	 * The location of event.
	 */
	private static WorldTile eventTile;
	/**
	 * The invite text of event.
	 */
	private static String eventText;


	public static synchronized void startEvent(String text, WorldTile tile, SurpriseEvent event) {
		if (!eventTaskSubmitted) {
			eventTaskSubmitted = true;
			GameExecutorManager.fastExecutor.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						if (tileEventHappening) {
							for (NPC npc : World.getNPCs()) {
								if (npc == null || npc.isDead() || npc.getNextForceTalk() != null)
									continue;
								int deltaX = npc.getX() - eventTile.getX();
								int deltaY = npc.getY() - eventTile.getY();
								if (npc.getPlane() == eventTile.getPlane() && !(deltaX < -25 || deltaX > 25 || deltaY < -25 || deltaY > 25))
									continue;
								if (Utils.random(10) != 0)
									continue;
	
								String message = "An event: " + eventText + " is currently happening! Talk to Oracle of Dawn to get there!";
								if (isEconomyManagerNpc(npc.getId()))
									message = message.replace("Oracle of Dawn", "me");
								npc.setNextForceTalk(new ForceTalk(message));
							}
						}
						else if (surpriseEvent != null) {
							for (NPC npc : World.getNPCs()) {
								if (npc == null || npc.isDead() || npc.getNextForceTalk() != null)
									continue;
								if (Utils.random(10) != 0)
									continue;
	
								String message = "An event: " + eventText + " is currently happening! Talk to Oracle of Dawn to get there!";
								if (isEconomyManagerNpc(npc.getId()))
									message = message.replace("Oracle of Dawn", "me");
								npc.setNextForceTalk(new ForceTalk(message));
							}
						}
					} catch (Throwable e) {
						Logger.handle(e);
					}
				}
			}, 0, 600);
		}

		eventText = text;
		if (tile != null) {
			tileEventHappening = true;
			eventTile = tile;
		}
		else {
			surpriseEvent = event;
			event.start();
		}
	}

	public static synchronized void stopEvent() {
		tileEventHappening = false;
		surpriseEvent = null;
	}

	public static boolean isEconomyManagerNpc(int id) {
		for (int i = 0; i < MANAGER_NPC_IDS.length; i++)
			if (MANAGER_NPC_IDS[i] == id)
				return true;
		return false;
	}

	private static void sendOptionsInterface(Player player) {
		player.getInterfaceManager().sendCentralInterface(1312);
		player.getPackets().sendHideIComponent(1312, 26, true);
	}

	public static void setupInterface(final Player player, final String[] options) {
		writeOptionsImmediate(player, options);
		// Interface 1312 has a cs2 onLoad that pre-populates the option
		// text with fortune-cookie / motivational lines from the cache.
		// Without re-applying our text after the cs2 runs, those lines
		// bleed through and the menu shows "Working towards a party
		// hat" / "Born to PK" interleaved with our real options. One
		// tick later we KNOW the cs2 has finished, so we write again -
		// our text wins.
		com.rs.game.tasks.WorldTasksManager.schedule(
			new com.rs.game.tasks.WorldTask() {
				@Override public void run() {
					if (player == null || player.hasFinished()) { stop(); return; }
					writeOptionsImmediate(player, options);
					stop();
				}
			}, 1);
	}

	private static void writeOptionsImmediate(Player player, String[] options) {
		for (int i = 0; i < ROOT_COMPONENTS.length; i++) {
			if (options[i] == null) {
				player.getPackets().sendHideIComponent(1312, ROOT_COMPONENTS[i], true);
			} else {
				player.getPackets().sendHideIComponent(1312, ROOT_COMPONENTS[i], false);
				player.getPackets().sendIComponentText(1312, TEXT_COMPONENTS[i], options[i]);
			}
		}
	}

	public static void processManagerNpcClick(final Player player, final int npcId) {
		if (!player.getBank().hasVerified(11))
			return;
		player.getDialogueManager().startDialogue(new Dialogue() {
			private int pageId = 0;
			private String[] currentOptions;
			private int currentOptionsOffset;
			private String currentTip;

			// Page sizes for the chatbox sendOptionsDialogue (max 5 slots).
			// If the page has more than 5 options, we show 4 + a "Next ▶"
			// button that advances by 4. Last partial page shows up to 5
			// without a Next button.
			private static final int CHATBOX_PAGE_FIRST  = 4;  // 4 options + Next
			private static final int CHATBOX_PAGE_LAST   = 5;  // 5 options if final

			@Override
			public void start() {
				// Skip the cs2-conflicting central interface (1312) entirely
				// and use chatbox sendOptionsDialogue (interface 1188) which
				// has no fortune-cookie text bleeding through.
				setTitlePage();
			}

			@Override
			public void run(int interfaceId, int componentId) {
				if (currentOptions == null) return;
				// chatbox option ids are 3..7 = OPTION_1..OPTION_5
				int slot;
				if      (componentId == OPTION_1) slot = 0;
				else if (componentId == OPTION_2) slot = 1;
				else if (componentId == OPTION_3) slot = 2;
				else if (componentId == OPTION_4) slot = 3;
				else if (componentId == OPTION_5) slot = 4;
				else return;
				int total = currentOptions.length;
				int remaining = total - currentOptionsOffset;
				boolean hasNext = remaining > CHATBOX_PAGE_LAST;
				if (hasNext && slot == CHATBOX_PAGE_FIRST) {
					// Next button - advance by 4, wrap to 0 at the end.
					currentOptionsOffset += CHATBOX_PAGE_FIRST;
					if (currentOptionsOffset >= total) currentOptionsOffset = 0;
					updateCurrentPage();
					return;
				}
				int absolute = currentOptionsOffset + slot;
				if (absolute >= total || currentOptions[absolute] == null) return;
				handlePage(absolute);
			}

			private void setPage(int page, String tip, String... options) {
				pageId = page;
				currentOptions = options;
				currentOptionsOffset = 0;
				currentTip = tip;
				updateCurrentPage();
			}

			private void updateCurrentPage() {
				int total = currentOptions.length;
				int remaining = total - currentOptionsOffset;
				boolean hasNext = remaining > CHATBOX_PAGE_LAST;
				int show = hasNext ? CHATBOX_PAGE_FIRST : Math.min(remaining, CHATBOX_PAGE_LAST);
				String[] buffer = new String[hasNext ? 5 : show];
				for (int i = 0; i < show; i++) {
					buffer[i] = currentOptions[currentOptionsOffset + i];
				}
				if (hasNext) buffer[CHATBOX_PAGE_FIRST] = "Next ▶";
				// Title shows tip + page indicator if multi-page.
				String title = currentTip;
				if (total > CHATBOX_PAGE_LAST) {
					int page = currentOptionsOffset / CHATBOX_PAGE_FIRST + 1;
					int pages = (total + CHATBOX_PAGE_FIRST - 1) / CHATBOX_PAGE_FIRST;
					title = title + "  (page " + page + "/" + pages + ")";
				}
				sendOptionsDialogue(title, buffer);
			}

			private void handlePage(int optionId) {
				if (pageId == 0) { // title page
					if (optionId == 0) // information & links
						setPage(1, "This section contains links to our websites and wiki<br>If you are beginner, it is strongly advisted to read our beginners guide.", "Website & Forums", "Wiki", "Beginners guide", "Back");
					else if (optionId == 1) // Account & character management.
						setManagementPage();
					else if (optionId == 2) // Teleports
						setTeleportsTitlePage();
					else if (optionId == 3) { // Dungeoneering
						player.setNextGraphics(new Graphics(3224));
						Magic.sendTeleportSpell(player, 17108, -2, 3225, 3019, 1, 0, new WorldTile(3448, 3698, 0), 18, true, 0);
					} else if (optionId == 4) // Shops
						setPage(4, "Here you can access various global shops.", SHOPS_NAMES);
					else if (optionId == 5) // Vote
						player.getPackets().sendOpenURL(Settings.VOTE_LINK);
					else if (optionId == 6) // Donate
						player.getPackets().sendOpenURL(Settings.DONATE_LINK);
					else if (optionId == 7) { // Ticket
						if (player.isMuted()) {
							player.getPackets().sendGameMessage("You can't submit ticket when you are muted.");
							return;
						}
						end();
						player.getDialogueManager().startDialogue("TicketDialouge");
					} else if (optionId == 8) // nevermind
						end();
				} else if (pageId == 1) { // information & links
					if (optionId == 0)
						player.getPackets().sendOpenURL(Settings.WEBSITE_LINK);
					else if (optionId == 1)
						player.getPackets().sendOpenURL(Settings.WIKI_LINK);
					else if (optionId == 2)
						player.getPackets().sendOpenURL(Settings.HELP_LINK);
					else if (optionId == 3)
						setTitlePage();
				} else if (pageId == 2) { // character management
					if (optionId == 0) { // change your password
						player.getPackets().sendOpenURL(Settings.PASSWORD_LINK);
					} else if (optionId == 1) { // auth forum acc
						player.getTemporaryAttributtes().put("forum_authuserinput", true);
						player.getPackets().sendInputLongTextScript("Enter your forum username:");
					} else if (optionId == 2) { // display name
						setPage(10, "Here you can set your display name or remove it.", "Set display name", "Remove display name", "Back");
					} else if (optionId == 3) { // switch items look (render-time swap)
						player.switchItemsLook();
						// Force inventory + equipment + appearance to re-send
						// so the swap is visible immediately rather than on
						// the next slot mutation. RetroSwaps.toOld is a pure
						// id rewrite at packet-encode time - the underlying
						// ItemsContainer is unchanged.
						try { player.getInventory().refresh(); } catch (Throwable ignored) {}
						try { player.getEquipment().refresh(); } catch (Throwable ignored) {}
						try { player.getAppearence().generateAppearenceData(); } catch (Throwable ignored) {}
						setManagementPage();
					} else if (optionId == 4) { // title select
						String[] page = getTitlesPage();
						setPage(11, "Here you can set your title, which will be displayed before or after your characters name.", page);
					} else if (optionId == 5) { // lock xp
						player.setXpLocked(!player.isXpLocked());
						setManagementPage();
					} else if (optionId == 6) { // toogle yellf
						player.setYellOff(!player.isYellOff());
						setManagementPage();
					} else if (optionId == 7) { // set yell color
						if (!player.isExtremeDonator()) {
							player.getPackets().sendGameMessage("This feature is only available to extreme donators!");
							return;
						}
						player.getTemporaryAttributtes().put("yellcolor", Boolean.TRUE);
						player.getPackets().sendInputLongTextScript("Please enter the yell color in HEX format.");
					} else if (optionId == 8) { // set baby troll name
						if (!player.isExtremeDonator()) {
							player.getPackets().sendGameMessage("This feature is only available to extreme donators!");
							return;
						}
						player.getTemporaryAttributtes().put("change_troll_name", true);
						player.getPackets().sendInputLongTextScript("Enter your baby troll name (type none for default):");
					} else if (optionId == 9) { // redesign character
						if (!player.isExtremeDonator()) {
							player.getPackets().sendGameMessage("This feature is only available to extreme donators!");
							return;
						}
						end();
						PlayerLook.openCharacterCustomizing(player);
					} else if (optionId == 10) { // combat mode (legacy / standard)
						setPage(20, "Combat mode controls whether abilities are used.<br>"
							+ "<col=ffaa55>Standard / EOC</col>: full ability bar, adrenaline, momentum.<br>"
							+ "<col=ffaa55>Legacy</col>: classic auto-attacks, no abilities.<br>"
							+ "Current: " + (player.isLegacyMode() ? "Legacy" : "Standard / EOC"),
							player.isLegacyMode() ? "Switch to Standard / EOC" : "Switch to Legacy",
							"Back");
					} else if (optionId == 11) { // xp rate
						setPage(21, "Choose your xp + drop rate.<br>"
							+ "Current: x" + Settings.getXpRate(player)
							+ " xp, x" + Settings.getCombatXpRate(player) + " combat xp.<br>"
							+ "<col=ff5555>Picking a new rate stays for the rest of your account's life.</col>",
							"x5 xp, x5 combat xp, x2.5 drop rate",
							"x20 xp, x40 combat xp, x2 drop rate",
							"x40 xp, x100 combat xp, x1 drop rate (Recommended)",
							"x100 xp, x500 combat xp, x0.4 drop rate",
							"Back");
					} else if (optionId == 12) { // back
						setTitlePage();
					}
				} else if (pageId == 20) { // combat mode picker
					if (optionId == 0) {
						player.switchLegacyMode();
						player.getPackets().sendGameMessage("Combat mode set to "
							+ (player.isLegacyMode() ? "Legacy" : "Standard / EOC") + ".");
					}
					setManagementPage();
				} else if (pageId == 21) { // xp rate picker
					if (optionId == 0)      player.setXpRateMode(4); // x5
					else if (optionId == 1) player.setXpRateMode(1); // x20/x40
					else if (optionId == 2) player.setXpRateMode(2); // x40/x100 (recommended)
					else if (optionId == 3) player.setXpRateMode(3); // x100/x500
					if (optionId >= 0 && optionId <= 3) {
						player.getPackets().sendGameMessage("Your xp rate mode is now: x"
							+ Settings.getXpRate(player) + " xp, x"
							+ Settings.getCombatXpRate(player) + " combat xp.");
					}
					setManagementPage();
				} else if (pageId == 3) { // teleports
					if (optionId == 0) { // current event
						if (tileEventHappening) {
							Magic.sendNormalTeleportSpell(player, 0, 0, eventTile);
						}
						else if (surpriseEvent != null) {
							end();
							surpriseEvent.tryJoin(player);
						}
						else {
							player.getPackets().sendGameMessage("No official event is currently happening.");
						}
					} else if (optionId == 1) { // current starter town
						Magic.sendNormalTeleportSpell(player, 0, 0, Settings.START_PLAYER_LOCATION);
					} else if (optionId == 2) { // safe pvp
						end();
						player.setNextWorldTile(new WorldTile(2815, 5511, 0));
						player.getControlerManager().startControler("clan_wars_ffa", false);
					} else if (optionId == 3) { // Combat training spots
						setPage(12, "This section contains various teleports to locations recommended for beginners.", NEWBIE_LOC_NAMES);
					} else if (optionId == 4) { // cities & towns
						setPage(13, "This section contains teleports to various cities & towns.", CITIES_NAMES);
					} else if (optionId == 5) { // dungeons & pvm
						setPage(14, "This section contains teleports to various pvm locations.", DUNGEON_NAMES);
					} else if (optionId == 6) { // minigames
						setPage(15, "This section contains teleports to various minigames locations.", MINIGAMES_NAMES);
					} else if (optionId == 7) { // others
						setPage(16, "This section contains various miscellaneous teleports.", OTHER_NAMES);
					} else if (optionId == 8) { // back
						setTitlePage();
					}
				} else if (pageId == 4) { // shops
					int shopId = SHOPS_IDS[optionId];
					if (shopId < 0) { // back
						setTitlePage();
					} else {
						end();
						ShopsHandler.openShop(player, shopId);
					}
				} else if (pageId == 10) { // display name management
					if (optionId == 0) { // set display name
						if (!player.isDonator()) {
							player.getPackets().sendGameMessage("This feature is only available to donators!");
							return;
						}
						player.getTemporaryAttributtes().put("setdisplay", Boolean.TRUE);
						player.getPackets().sendInputLongTextScript("Enter display name you want to be set:");
					} else if (optionId == 1) { // remove display name
						LoginClientChannelManager.sendReliablePacket(LoginChannelsPacketEncoder.encodeAccountVarUpdate(player.getUsername(), LoginProtocol.VAR_TYPE_DISPLAY_NAME, Utils.formatPlayerNameForDisplay(player.getUsername())).getBuffer());
					} else if (optionId == 2) { // back
						setManagementPage();
					}
				} else if (pageId == 11) { // titles page
					int[] ids = getTitlesIds();
					if (currentOptions.length != ids.length) {
						// error
						setManagementPage();
						return;
					}

					int titleId = ids[optionId];
					if (titleId == -2) { // back button
						setManagementPage();
					} else if (titleId == -1) { // no title
						player.getAppearence().setTitle(0);
						setManagementPage();
					} else if (titleId > 0) {
						player.getAppearence().setTitle(titleId);
						setManagementPage();
					} else {
						setManagementPage();
					}
				} else if (pageId == 12) { // newbie teles
					if (NEWBIE_LOCATIONS[optionId] == null) { // back
						setTeleportsTitlePage();
					} else {
						Magic.sendLunarTeleportSpell(player, 0, 0, NEWBIE_LOCATIONS[optionId]);
					}
				} else if (pageId == 13) { // teleports cities & towns
					if (CITIES_LOCATIONS[optionId] == null) { // back
						setTeleportsTitlePage();
					} else {
						Magic.sendLunarTeleportSpell(player, 0, 0, CITIES_LOCATIONS[optionId]);
					}
				} else if (pageId == 14) { // dungeons
					if (DUNGEON_LOCATIONS[optionId] == null) { // back
						setTeleportsTitlePage();
					} else {
						if (DUNGEON_NAMES[optionId].contains("(GWD)")) {
							player.setNextWorldTile(DUNGEON_LOCATIONS[optionId]);
							player.stopAll();
							player.getControlerManager().startControler("GodWars");
						} else {
							Magic.sendLunarTeleportSpell(player, 0, 0, DUNGEON_LOCATIONS[optionId]);
						}
					}
				} else if (pageId == 15) { // minigames
					if (MINIGAMES_LOCATIONS[optionId] == null) { // back
						setTeleportsTitlePage();
					} else {
						Magic.sendLunarTeleportSpell(player, 0, 0, MINIGAMES_LOCATIONS[optionId]);
					}
				} else if (pageId == 16) { // others
					if (OTHER_LOCATIONS[optionId] == null) { // back
						setTeleportsTitlePage();
					} else {
						Magic.sendLunarTeleportSpell(player, 0, 0, OTHER_LOCATIONS[optionId]);
						if (OTHER_NAMES[optionId].contains("(Wilderness")) {
							player.getControlerManager().startControler("Wilderness");
						}
					}
				} else if (pageId == 99) { // temp page
					setTeleportsTitlePage();
				}
			}

			private void setTitlePage() {
				setPage(0, "Welcome to " + Settings.SERVER_NAME + "!<br>I provide various services to make your life here easier.", "Information & Links", "Account & Character management", (tileEventHappening || surpriseEvent != null) ? "Teleports (Click here for event)" : "Teleports", "Dungeoneering", "Shops", "Vote", "—", "Submit a ticket", "Close Oracle");
			}

			private void setManagementPage() {
				// "Switch to old/new items look" - render-time swap via
				// RetroSwaps. Inventory / equipment / appearance encoders
				// rewrite item ids on the way out based on the player flag,
				// no actual storage mutation.
				setPage(2, "This section contains features, which will help you to manage your account easier.", "Change password", "Authenticate your forum account", "Display name management", player.isOldItemsLook() ? "Switch to new items look" : "Switch to retro items look", "Set your title", player.isXpLocked() ? "Unlock XP" : "Lock XP", player.isYellOff() ? "Toogle yell on" : "Toogle yell off", "Set yell color", "Set baby troll name", "Redesign character", "Combat mode (" + (player.isLegacyMode() ? "Legacy" : "Standard / EOC") + ")", "XP rate (current: x" + Settings.getXpRate(player) + " / x" + Settings.getCombatXpRate(player) + " combat)", "Back");
			}

			private void setTeleportsTitlePage() {
				setPage(3, "This section contains teleports to various different locations.", "Current event", "Home", "Safe PvP", "Combat training spots", "Cities & Towns", "Dungeons & PVM Locations", "Minigames", "Others", "Back");
			}

			private String[] getTitlesPage() {
				String[] buffer = new String[102];
				int count = 0;

				buffer[count++] = "No title";

				ClientScriptMap map = player.getAppearence().isMale() ? ClientScriptMap.getMap(1093) : ClientScriptMap.getMap(3872);
				for (Object value : map.getValues().values()) {
					if (value instanceof String && ((String) value).length() > 0) {
						buffer[count++] = (String) value;
					}

					if (count >= (buffer.length - 2))
						break;
				}

				buffer[count++] = "Back";

				if (count != buffer.length) {
					String[] rebuff = new String[count];
					System.arraycopy(buffer, 0, rebuff, 0, rebuff.length);
					return rebuff;
				} else {
					return buffer;
				}
			}

			private int[] getTitlesIds() {
				int[] buffer = new int[102];
				int count = 0;

				buffer[count++] = -1;

				ClientScriptMap map = player.getAppearence().isMale() ? ClientScriptMap.getMap(1093) : ClientScriptMap.getMap(3872);
				for (Object value : map.getValues().values()) {
					if (value instanceof String && ((String) value).length() > 0) {
						buffer[count++] = (int) map.getKeyForValue(value);
					}

					if (count >= (buffer.length - 2))
						break;
				}

				buffer[count++] = -2;

				if (count != buffer.length) {
					int[] rebuff = new int[count];
					System.arraycopy(buffer, 0, rebuff, 0, rebuff.length);
					return rebuff;
				} else {
					return buffer;
				}
			}

			@Override
			public void finish() {
				closeNoContinueDialogue(player);
				player.getInterfaceManager().removeCentralInterface();

			}

		});
	}

	public static final void processStorePurchase(final Player player, String item) {
		if (item.equals("Random nex set")) {
			int[][] sets = new int[][]
			{ new int[]
			{ 20159, 20163, 20167 }, new int[]
			{ 20147, 20151, 20155 }, new int[]
			{ 20135, 20139, 20143 } };
			int[] set = sets[Utils.random(sets.length)];
			for (int itemid : set)
				player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Random chaotic item")) {
			int[] items = new int[]
			{ 18349, 18351, 18353, 18355, 18357, 18359, };
			int itemid = items[Utils.random(items.length)];
			player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Random spirit shield")) {
			int[] items = new int[]
			{ 13738, 13740, 13742, 13744 };
			int itemid = items[Utils.random(items.length)];
			player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Random godsword")) {
			int[] items = new int[]
			{ 11694, 11696, 11698, 11700 };
			int itemid = items[Utils.random(items.length)];
			player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Random partyhat")) {
			int[] items = new int[]
			{ 1038, 1040, 1042, 1044, 1046, 1048 };
			int itemid = items[Utils.random(items.length)];
			player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Random haloween mask")) {
			int[] items = new int[]
			{ 1053, 1055, 1057, };
			int itemid = items[Utils.random(items.length)];
			player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Experience (Random skill)")) {
			int skill = Utils.random(Skills.SKILL_NAME.length);
			player.getSkills().addXpStore(skill, 3000000.0D);
		} else if (item.equals("All barrows sets")) {
			int[] items = new int[]
			{ 11846, 11848, 11850, 11852, 11854, 11856 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Bandos set (With godsword)")) {
			int[] items = new int[]
			{ 11696, 11724, 11726, 11728 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Armadyl set (With godsword)")) {
			int[] items = new int[]
			{ 11694, 11718, 11720, 11722 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Divine spirit shield")) {
			int[] items = new int[]
			{ 13740 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Dragon claws")) {
			int[] items = new int[]
			{ 14484 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Abyssal whip")) {
			int[] items = new int[]
			{ 4151 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Coins")) {
			int[] items = new int[]
			{ 995 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 100000000);
		} else if (item.equals("Vote tokens")) {
			int[] items = new int[]
			{ Settings.VOTE_TOKENS_ITEM_ID };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 10000000);
		} else if (item.equals("Fire cape")) {
			player.setCompletedFightCaves();
			int[] items = new int[]
			{ 6570 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 1);
		} else if (item.equals("Kiln cape")) {
			player.setCompletedFightKiln();
			int[] items = new int[]
			{ 23659 };
			for (int itemid : items)
				player.getInventory().addItemDrop(itemid, 1);
		}

		else if (item.startsWith("vote_tokens:")) {
			if (!player.hasVotedInLast12Hours())
				player.setVoteCount(0);
			int votes = player.getVoteCount();
			if (votes >= 3) {
				player.getPackets().sendGameMessage("You may only claim a vote three times a day. This auth has been terminated.");
				player.getPackets().sendGameMessage("For more news please refer to ::thread 75672.");
				return;
			}
			player.setVoteCount(player.getVoteCount() + 1);
			int amount = Integer.parseInt(item.substring(12));
			Item tokens = new Item(Settings.VOTE_TOKENS_ITEM_ID, amount);
			if (player.getBank().addItems(new Item[] {tokens}, true) == 0)
				player.getInventory().addItemDrop(tokens.getId(), tokens.getAmount());
			if (amount >= Settings.VOTE_MIN_AMOUNT)
				player.refreshLastVote();
			World.sendNews(player, Utils.formatPlayerNameForDisplay(player.getDisplayName()) + " has just voted and received " + amount + " vote tokens! (::vote)", 0);
		} else {
			player.getPackets().sendGameMessage("Unknown purchase:" + item);
		}
	}
}
