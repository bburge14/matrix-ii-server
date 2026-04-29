package com.rs.game.player.content;

import com.rs.Settings;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.actions.Smithing;
import com.rs.utils.Utils;

/**
 * 
 * @author Andy || ReverendDread Dec 9, 2016
 *
 */

public class SpringCleaner {
	
	/**
	 * Interface Id
	 */
	private static final int cleanerInterface = 422;

	/**
	 * Opens spring cleaner interface
	 * The player
	 * @param player
	 */
	public static void openInterface(Player player) {
		player.getInterfaceManager().sendInterface(cleanerInterface);
		player.getPackets().sendIComponentText(cleanerInterface, 1999, "Spring cleaner | Springs: " + player.cleanerSprings);
		player.getPackets().sendIComponentText(cleanerInterface, 15, "Current Mode: " + (player.springDismantle ? "Dismantle" : "Research"));
		player.getPackets().sendIComponentText(cleanerInterface, 17, "<center>|- Switch Mode -|</center>");
		player.getPackets().sendIComponentText(cleanerInterface, 19, "Toggle - Leather Items - " + (player.springLeatherArmor ? "Enabled" : "Disabled"));
		player.getPackets().sendIComponentText(cleanerInterface, 21, "Toggle - Metal Bars - " + (player.springMetalBars ? "Enabled" : "Disabled"));
		player.getPackets().sendIComponentText(cleanerInterface, 23, "Toggle - Metal Armor - " + (player.springMetalArmor ? "Enabled" : "Disabled"));
		player.getPackets().sendIComponentText(cleanerInterface, 25, "Toggle - Metal Weapons / Arrows - " + (player.springMetalWeaponsAndArrows ? "Enabled" : "Disabled"));
		player.getPackets().sendIComponentText(cleanerInterface, 27, "Toggle - Bows - " + (player.springBows ? "Enabled" : "Disabled"));
		player.getPackets().sendIComponentText(cleanerInterface, 29, "Toggle - Crossbows - " + (player.springCrossbows ? "Enabled" : "Disabled"));
		player.getPackets().sendIComponentText(cleanerInterface, 31, "Toggle - Jewllery - " + (player.springJewllery ? "Enabled" : "Disabled"));
		player.getPackets().sendIComponentText(cleanerInterface, 33, "Toggle - Battlestaves - " + (player.springBattlestaves ? "Enabled" : "Disabled"));
		//for (int i = 0; i < 100; i++)
		//	player.getPackets().sendIComponentText(cleanerInterface, i, "cid: " + i);
	}
	
	/**
	 * Handles interface buttons
	 * The player
	 * @param player
	 * Interface componentId
	 * @param buttonId
	 */
	public static void handleButtons(Player player, int buttonId) {
		switch (buttonId) {
		
			case 16:
				//nothing
			break;
		
			case 18:
				player.springDismantle = !player.springDismantle;
				openInterface(player);
			break;
		
			case 20:
				player.springLeatherArmor = !player.springLeatherArmor;
				openInterface(player);
			break;
			
			case 22:
				player.springMetalBars = !player.springMetalBars;
				openInterface(player);
			break;
			
			case 24:
				player.springMetalArmor = !player.springMetalArmor;
				openInterface(player);
			break;
			
			case 26:
				player.springMetalWeaponsAndArrows = !player.springMetalWeaponsAndArrows;
				openInterface(player);
			break;
			
			case 28:
				player.springBows = !player.springBows;
				openInterface(player);
			break;
			
			case 30:
				player.springCrossbows = !player.springCrossbows;
				openInterface(player);
			break;
			
			case 32:
				player.springJewllery = !player.springJewllery;
				openInterface(player);
			break;
			
			case 34:
				player.springBattlestaves = !player.springBattlestaves;
				openInterface(player);
			break;
			
			default:
				player.sm("Button is null.");
		}
	}
	
	
	/**
	 * Processes the item to see which catagory to sort in.
	 * The player
	 * @param player
	 * Item to be processed
	 * @param item
	 * Item material filter type
	 * @param type
	 */
	public static void processItem(Player player, Item item, int type) {		
		if (player.springDismantle) {
			switch (type) {
				case 1: // leather shit
					if (item.getName().contains("leather body")) {
						player.getBank().addItem(1739, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Cowhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("leather chaps")) {
						player.getBank().addItem(1739, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Cowhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("leather vambraces") || item.getName().contains("leather gloves")) {
						player.getBank().addItem(1739, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Cowhide</col> - to your bank.");
					}		
					if (item.getName().equalsIgnoreCase("hard leather body")) {
						player.getBank().addItem(1739, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Cowhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("hard leather chaps")) {
						player.getBank().addItem(1739, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Cowhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("hard leather vambraces")) {
						player.getBank().addItem(1739, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Cowhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("snakeskin body")) {
						player.getBank().addItem(6287, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Snake hide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("snakeskin chaps")) {
						player.getBank().addItem(6287, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Snake hide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("snakeskin vambraces")) {
						player.getBank().addItem(6287, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Snake hide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("green d'hide body")) {
						player.getBank().addItem(1753, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Green dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("green d'hide chaps")) {
						player.getBank().addItem(1753, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Green dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("green d'hide vambraces")) {
						player.getBank().addItem(1753, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Green dragonhide</col> - to your bank.");
					}		
					if (item.getName().equalsIgnoreCase("blue d'hide body")) {
						player.getBank().addItem(1751, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Blue dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("blue d'hide chaps")) {
						player.getBank().addItem(1751, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Blue dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("blue d'hide vambraces")) {
						player.getBank().addItem(1751, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Blue dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("red d'hide body")) {
						player.getBank().addItem(1749, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Red dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("red d'hide chaps")) {
						player.getBank().addItem(1749, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Red dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("red d'hide vambraces")) {
						player.getBank().addItem(1749, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Red dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("black d'hide body")) {
						player.getBank().addItem(1747, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Black dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("black d'hide chaps")) {
						player.getBank().addItem(1747, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Black dragonhide</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("black d'hide vambraces")) {
						player.getBank().addItem(1747, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Black dragonhide</col> - to your bank.");
					}
				break;
				
				case 2: //metal bars
					if (item.getName().equalsIgnoreCase("bronze bar")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Copper ore, 1x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron bar")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel bar")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("mithril bar")) {
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant bar")) {
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune bar")) {
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
				break;
				
				case 3: //metal armor
					if (item.getName().equalsIgnoreCase("bronze platebody")) {
						player.getBank().addItem(438, 3 * item.getAmount(), true);
						player.getBank().addItem(436, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Copper ore, 3x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze platelegs")) {
						player.getBank().addItem(438, 2 * item.getAmount(), true);
						player.getBank().addItem(436, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Copper ore, 2x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze plateskirt")) {
						player.getBank().addItem(438, 2 * item.getAmount(), true);
						player.getBank().addItem(436, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Copper ore, 2x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze full helm") ) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Copper ore, 1x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze helm")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Copper ore, 1x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze chainbody")) {
						player.getBank().addItem(438, 2 * item.getAmount(), true);
						player.getBank().addItem(436, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Copper ore, 2x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze sq shield")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Copper ore, 1x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze kiteshield")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Copper ore, 1x Tin Ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron platebody")) {
						player.getBank().addItem(440, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron platelegs")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron plateskirt")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron full helm") ) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron helm")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron chainbody")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron sq shield")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron kiteshield")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel platebody")) {
						player.getBank().addItem(440, 3 * item.getAmount(), true);
						player.getBank().addItem(453, 3 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Iron ore, 3x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel platelegs")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel plateskirt")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel full helm") ) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel helm")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel chainbody")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore, 2x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel sq shield")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel kiteshield")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril platebody")) {
						player.getBank().addItem(447, 3 * item.getAmount(), true);
						player.getBank().addItem(453, 15 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Mithril ore, 15x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril platelegs")) {
						player.getBank().addItem(447, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Mithril ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril plateskirt")) {
						player.getBank().addItem(447, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Mithril ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril mithril full helm") ) {
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril mithril helm")) {
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril chainbody")) {
						player.getBank().addItem(447, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Mithril ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril sq shield")) {
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril kiteshield")) {
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
	
					if (item.getName().equalsIgnoreCase("adamant platebody")) {
						player.getBank().addItem(449, 3 * item.getAmount(), true);
						player.getBank().addItem(453, 15 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Adamantite ore, 15x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant platelegs")) {
						player.getBank().addItem(449, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Adamantite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant plateskirt")) {
						player.getBank().addItem(449, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Adamantite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant full helm") ) {
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant adamant helm")) {
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant chainbody")) {
						player.getBank().addItem(449, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Adamantite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant sq shield")) {
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant kiteshield")) {
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune platebody")) {
						player.getBank().addItem(451, 3 * item.getAmount(), true);
						player.getBank().addItem(453, 15 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>3x Runite ore, 15x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune platelegs")) {
						player.getBank().addItem(451, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Runite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune plateskirt")) {
						player.getBank().addItem(451, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Runite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune full helm") ) {
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune helm")) {
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune chainbody")) {
						player.getBank().addItem(451, 2 * item.getAmount(), true);
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Runite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune sq shield")) {
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune kiteshield")) {
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
				break;			
				
				case 4: //metal weps and arrows
					if (item.getName().equalsIgnoreCase("bronze longsword")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze mace")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze sword")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze halbard")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze dagger")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze warhammer")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze scimitar")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze spear")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze battleaxe")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze hatchet")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("bronze pickaxe")) {
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore</col> - to your bank.");
					}			
					if (item.getName().equalsIgnoreCase("Iron longsword")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron mace")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron sword")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron halbard")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron dagger")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron warhammer")) {
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron scimitar")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron spear")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron battleaxe")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron hatchet")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Iron pickaxe")) {
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel longsword")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel mace")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel sword")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel halbard")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel dagger")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel warhammer")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel scimitar")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel spear")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel battleaxe")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel hatchet")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel pickaxe")) {
						player.getBank().addItem(453, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril longsword")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(447, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Mithril ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril mace")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(447, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Mithril ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("mithril sword")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril halbard")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(447, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Mithril ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril dagger")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril warhammer")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(447, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Mithril ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril scimitar")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril spear")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril battleaxe")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril hatchet")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("Mithril pickaxe")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant longsword")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(449, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Adamantite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant mace")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(449, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Adamantite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant sword")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant halbard")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(449, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Adamantite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant dagger")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant warhammer")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(449, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Adamantite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant scimitar")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant spear")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant battleaxe")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant hatchet")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant pickaxe")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune longsword")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(451, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Runite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune mace")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(451, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Runite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune sword")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune halbard")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(451, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Runite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune dagger")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune warhammer")) {
						player.getBank().addItem(453, 10 * item.getAmount(), true);
						player.getBank().addItem(451, 2 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>2x Runite ore, 10x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune scimitar")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune spear")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune battleaxe")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune hatchet")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune pickaxe")) {
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal</col> - to your bank.");
					}	
				break;		
				
				case 5: // bows
					if (item.getName().contains("shortbow") || item.getName().contains("longbow")) {
						player.getBank().addItem(1511, 1 * item.getAmount(), true);
						player.getBank().addItem(1777, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Logs, 1x Bowstring</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("oak shieldbow") || item.getName().equalsIgnoreCase("oak shortbow")) {
						player.getBank().addItem(1521, 1 * item.getAmount(), true);
						player.getBank().addItem(1777, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Oak logs, 1x Bowstring</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("willow shieldbow") || item.getName().equalsIgnoreCase("willow shortbow")) {
						player.getBank().addItem(1519, 1 * item.getAmount(), true);
						player.getBank().addItem(1777, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Willow logs, 1x Bowstring</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("maple shieldbow") || item.getName().equalsIgnoreCase("maple shortbow")) {
						player.getBank().addItem(1517, 1 * item.getAmount(), true);
						player.getBank().addItem(1777, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Maple logs, 1x Bowstring</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("yew shieldbow") || item.getName().equalsIgnoreCase("yew shortbow")) {
						player.getBank().addItem(1515, 1 * item.getAmount(), true);
						player.getBank().addItem(1777, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Yew logs, 1x Bowstring</col> - to your bank.");					
					}
					if (item.getName().contains("magic shieldbow") || item.getName().equalsIgnoreCase("magic shortbow")) {
						player.getBank().addItem(1513, 1 * item.getAmount(), true);
						player.getBank().addItem(1777, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Magic logs, 1x Bowstring</col> - to your bank.");
					}
				break;
				
				case 6: //crossbows
					if (item.getName().equalsIgnoreCase("bronze crossbow")) {
						player.getBank().addItem(9436, 1 * item.getAmount(), true);
						player.getBank().addItem(438, 1 * item.getAmount(), true);
						player.getBank().addItem(436, 1 * item.getAmount(), true);
						player.getBank().addItem(1511, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Tin ore, 1x Copper ore, 1x Sinew, 1x Logs</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("iron crossbow")) {
						player.getBank().addItem(9436, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.getBank().addItem(1519, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Sinew, 1x Teak logs</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("steel crossbow")) {
						player.getBank().addItem(9436, 1 * item.getAmount(), true);
						player.getBank().addItem(440, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(6333, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Iron ore, 1x Coal, 1x Sinew, 1x Teak logs</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("mithril crossbow")) {
						player.getBank().addItem(9436, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(447, 1 * item.getAmount(), true);
						player.getBank().addItem(1517, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Mithril ore, 5x Coal, 1x Sinew, 1x Maple logs</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("adamant crossbow")) {
						player.getBank().addItem(9436, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(449, 1 * item.getAmount(), true);
						player.getBank().addItem(6332, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Adamantie ore, 5x Coal, 1x Sinew, 1x Mahogany logs</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("rune crossbow")) {
						player.getBank().addItem(9436, 1 * item.getAmount(), true);
						player.getBank().addItem(453, 5 * item.getAmount(), true);
						player.getBank().addItem(451, 1 * item.getAmount(), true);
						player.getBank().addItem(1515, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Runite ore, 5x Coal, 1x Sinew, 1x Yew logs</col> - to your bank.");
					}
				break;
				
				case 7: //jewllery
					if (item.getName().equalsIgnoreCase("gold amulet")) {
						player.getBank().addItem(1759, 1 * item.getAmount(), true);
						player.getBank().addItem(444, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>x1 Gold ore, and 1x Ball of wool</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("sapphire amulet")) {
						player.getBank().addItem(1759, 1 * item.getAmount(), true);
						player.getBank().addItem(444, 1 * item.getAmount(), true);
						player.getBank().addItem(1623, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>x1 Gold ore, and 1x Ball of wool, 1x Uncut sapphire</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("emerald amulet")) {
						player.getBank().addItem(1759, 1 * item.getAmount(), true);
						player.getBank().addItem(444, 1 * item.getAmount(), true);
						player.getBank().addItem(1621, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>x1 Gold ore, and 1x Ball of wool, 1x Uncut emerald</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("ruby amulet")) {
						player.getBank().addItem(1759, 1 * item.getAmount(), true);
						player.getBank().addItem(444, 1 * item.getAmount(), true);
						player.getBank().addItem(1619, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>x1 Gold ore, and 1x Ball of wool, 1x Uncut ruby</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("diamond amulet")) {
						player.getBank().addItem(1759, 1 * item.getAmount(), true);
						player.getBank().addItem(444, 1 * item.getAmount(), true);
						player.getBank().addItem(1617, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>x1 Gold ore, and 1x Ball of wool, 1x Uncut diamond</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("dragonstone amulet")) {
						player.getBank().addItem(1759, 1 * item.getAmount(), true);
						player.getBank().addItem(444, 1 * item.getAmount(), true);
						player.getBank().addItem(1631, 1 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>x1 Gold ore, and 1x Ball of wool, 1x Uncut dragonstone</col> - to your bank.");
					}
				break;
				
				case 8: //battlestaves
					if (item.getName().equalsIgnoreCase("water battlestaff")) {
						player.getBank().addItem(1391, 1 * item.getAmount(), true);
						player.getBank().addItem(567, 1 * item.getAmount(), true);
						player.getBank().addItem(555, 30 * item.getAmount(), true);
						player.getBank().addItem(564, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Battlestaff, 1x Unpowered orb, 5x Cosmic rune, 30x Water rune</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("earth battlestaff")) {
						player.getBank().addItem(1391, 1 * item.getAmount(), true);
						player.getBank().addItem(567, 1 * item.getAmount(), true);
						player.getBank().addItem(557, 30 * item.getAmount(), true);
						player.getBank().addItem(564, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Battlestaff, 1x Unpowered orb, 5x Cosmic rune, 30x Earth rune</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("air battlestaff")) {
						player.getBank().addItem(1391, 1 * item.getAmount(), true);
						player.getBank().addItem(567, 1 * item.getAmount(), true);
						player.getBank().addItem(556, 30 * item.getAmount(), true);
						player.getBank().addItem(564, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Battlestaff, 1x Unpowered orb, 5x Cosmic rune, 30x Air rune</col> - to your bank.");
					}
					if (item.getName().equalsIgnoreCase("fire battlestaff")) {
						player.getBank().addItem(1391, 1 * item.getAmount(), true);
						player.getBank().addItem(567, 1 * item.getAmount(), true);
						player.getBank().addItem(554, 30 * item.getAmount(), true);
						player.getBank().addItem(564, 5 * item.getAmount(), true);
						player.sm("[Spring Cleaner]: Your Spring cleaner broke down a <col=ffff00>" + item.getName() + ".");
						player.sm("[Spring Cleaner]: Your Spring cleaner sends - <col=ffff00>1x Battlestaff, 1x Unpowered orb, 5x Cosmic rune, 30x Fire rune</col> - to your bank.");
					}
				break;
				
				default:
					player.sm("[Spring Cleaner]: Failed Item - " + item.getName());
					return;
			}
		} else {
			switch (type) {
			
				case 1:
					player.getSkills().addXp(Skills.CRAFTING, (getExpModifier(item) * 20) * item.getAmount());
					player.sm("[Spring Cleaner]: Your Spring cleaner breaks down <col=00ffff>x" + item.getAmount() + " "
							+ item.getName() + " into " + Utils.formatNumber((int)(getExpModifier(item) * 20) * item.getAmount() * Settings.XP_RATE) + "</col> Crafting XP.");
				break;
				
				case 2:
					player.getSkills().addXp(Skills.SMITHING, (getExpModifier(item) * 20) * item.getAmount());
					player.sm("[Spring Cleaner]: Your Spring cleaner breaks down <col=00ffff>x" + item.getAmount() + " " 
							+ item.getName() + " into " + Utils.formatNumber((int)(getExpModifier(item) * 20) * item.getAmount() * Settings.XP_RATE) + "</col> Smithing XP.");
				break;
				
				case 3:
					player.getSkills().addXp(Skills.SMITHING, (getExpModifier(item) * 20) * item.getAmount());
					player.sm("[Spring Cleaner]: Your Spring cleaner breaks down <col=00ffff>x" + item.getAmount() + " " 
							+ item.getName() + " into " + Utils.formatNumber((int)(getExpModifier(item) * 20) * item.getAmount() * Settings.XP_RATE) + "</col> Smithing XP.");
				break;
				
				case 4:
					player.getSkills().addXp(Skills.SMITHING, (getExpModifier(item) * 20) * item.getAmount());
					player.sm("[Spring Cleaner]: Your Spring cleaner breaks down <col=00ffff>x" + item.getAmount() + " " 
							+ item.getName() + " into " + Utils.formatNumber((int)(getExpModifier(item) * 20) * item.getAmount() * Settings.XP_RATE) + "</col> Smithing XP.");
				break;
				
				case 5:
					player.getSkills().addXp(Skills.FLETCHING, (getExpModifier(item) * 20) * item.getAmount());
					player.sm("[Spring Cleaner]: Your Spring cleaner breaks down <col=00ffff>x" + item.getAmount() + " " 
							+ item.getName() + " into " + Utils.formatNumber((int)(getExpModifier(item) * 20) * item.getAmount() * Settings.XP_RATE) + "</col> Fletching XP.");
				break;
				
				case 6:
					player.getSkills().addXp(Skills.FLETCHING, (getExpModifier(item) * 20) * item.getAmount());
					player.sm("[Spring Cleaner]: Your Spring cleaner breaks down <col=00ffff>x" + item.getAmount() + " " 
							+ item.getName() + " into " + Utils.formatNumber((int)(getExpModifier(item) * 20) * item.getAmount() * Settings.XP_RATE) + "</col> Fletching XP.");
				break;
				
				case 7:
					player.getSkills().addXp(Skills.CRAFTING, (getExpModifier(item) * 20) * item.getAmount());
					player.sm("[Spring Cleaner]: Your Spring cleaner breaks down <col=00ffff>x" + item.getAmount() + " " 
							+ item.getName() + " into " + Utils.formatNumber((int)(getExpModifier(item) * 20) * item.getAmount() * Settings.XP_RATE) + "</col> Crafting XP.");
				break;
				
				case 8:
					player.getSkills().addXp(Skills.CRAFTING, (getExpModifier(item) * 20) * item.getAmount());
					player.sm("[Spring Cleaner]: Your Spring cleaner breaks down <col=00ffff>x" + item.getAmount() + " "
							+ item.getName() + " into " + Utils.formatNumber((int)(getExpModifier(item) * 20) * item.getAmount() * Settings.XP_RATE) + "</col> Crafting XP.");
				break;
			}
		}
		removeCharges(player, item.getAmount());
	}

	
	/**
	 * Returns amount of base experience from item materials.
	 * The item
	 * @param item
	 * @return xp modifier
	 */
	public static double getExpModifier(Item item) {
		if (item.getName().contains("platebody") || item.getName().contains("body")) 
			return 5;
		if (item.getName().contains("platelegs") || item.getName().contains("plateskirt") 
					|| item.getName().contains("chainbody") || item.getName().contains("2h sword") 
							|| item.getName().contains("sq shield") || item.getName().contains("warhammer") 
									|| item.getName().contains("kiteshield") || item.getName().contains("battleaxe") 
											|| item.getName().contains("crossbow") || item.getName().contains("longbow") 
													|| item.getName().contains("chaps"))
			return 3;
		if (item.getName().contains("full helm") || item.getName().contains("longsword") 
					|| item.getName().contains("scimitar") || item.getName().contains("pickaxe") 
							|| item.getName().contains("arrowheads") || item.getName().contains("vambraces") 
									|| item.getName().contains("gloves") || item.getName().contains("spear"))
			return 2;
		if (item.getName().contains("knife") || item.getName().contains("claw") 
					|| item.getName().contains("longsword") || item.getName().contains("sword") 
							|| item.getName().contains("runite limbs") || item.getName().contains("mace")
									|| item.getName().contains("hatchet") || item.getName().contains("dagger") 
											| item.getName().contains("nails") || item.getName().contains("dart tip") 
													|| item.getName().contains("bar"))
			return 1;
		return 0;
	}
	
	
	/**
	 * Handles charging the spring cleaner
	 * The player
	 * @param player
	 * Item used
	 * @param item
	 */
	public static void chargeCleaner(Player player, Item item) {
		if ((player.cleanerSprings += item.getAmount()) >= Integer.MAX_VALUE) {
			player.sm("You can't go over " + Utils.formatNumber(Integer.MAX_VALUE) + " springs.");
			return;
		}
		//player.cleanerSprings += item.getAmount();
		player.getInventory().deleteItem(item);
		player.sm("[Spring Cleaner]: Your Spring cleaner now has: <col=ffff00>" 
				+ Utils.formatNumber(player.cleanerSprings) + "</col> springs.");
	}

	/**
	 * Gets current springs in cleaner.
	 * The player
	 * @param player
	 */
	public static void getCharges(Player player) {
		player.sm("[Spring Cleaner]: Your Spring cleaner has has: <col=ffff00>" 
				+ Utils.formatNumber(player.cleanerSprings) + "</col> springs.");	
	}
	
	/**
	 * Handles removing charges from spring cleaner
	 * The player
	 * @param player
	 * How many charges are removed
	 * @param removedCharges
	 */
	public static void removeCharges(Player player, int removedCharges) {
		player.cleanerSprings -= removedCharges;
		getCharges(player);
	}
	
	/**
	 * Checks if player has spring cleaner
	 * The player
	 * @param player
	 */
	public static boolean hasSpringCleaner(Player player) {
		if (player.getBank().containsItem(31612, 1) || player.getInventory().contains(new Item(31612)) 
					|| player.getFamiliar().getBob().containsOneItem(31612))
			return true;
		return false;
	}
	
}
