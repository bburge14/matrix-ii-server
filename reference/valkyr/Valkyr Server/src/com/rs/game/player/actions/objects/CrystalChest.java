package com.rs.game.player.actions.objects;

import com.rs.game.World;
import com.rs.game.WorldObject;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.utils.Utils;
import com.rs.cache.loaders.ItemDefinitions;


public class CrystalChest {
	
	public static void Chest(Player player, final WorldObject object) {
		int[][] Common = { { 556, 200}, { 559, 200}, { 562, 50}, { 564, 50}, { 557, 200}, { 563, 200}, { 558, 200}, { 561, 50}, { 555, 200} };
		int[][] Uncommon = { {1201, 1}, {3202, 1}, {1163, 1}, {1617, 3}, {1619, 3}, {2363, 1}, {1443, 5}, {985, 1}, {987, 1}, {31613, 50} };
		int[][] Rare = { {450, 10}, {454, 80}, {32262, 50}, {1079, 1}, {1093, 1}, {1445, 2}, {31612, 1} };
		int[][] SuperRare = { {28547, 1}, {28548, 1}, {28549, 1}, {5315, 1}, {5316, 1} };
		int[][] Legendary = { {28541, 1}, {28539, 1}, {28543, 1}, {28545, 1}, {28537, 1} };
		int rarity = Utils.getRandom(1000);
		if (rarity > 0 && rarity <= 600)  {
			for (int i = 0; i < Common.length - 1; i++) {
			    player.getInventory().addItemDrop(Common[i][0], Common[i][1]);
			}
			player.getInventory().addItemDrop(1631, 1);
		}		
		if (rarity > 600 && rarity <= 900)  {
		    int length = Uncommon.length;
			length--;
		    int reward = Utils.getRandom(length);
			player.getInventory().addItemDrop(Uncommon[reward][0], Uncommon[reward][1]);
			player.getInventory().addItemDrop(1631, 1);
		}
		if (rarity > 900 && rarity <= 970)  {
		    int length = Rare.length;
			length--;
		    int reward = Utils.getRandom(length);
		    if (Rare[reward][0] == 31612) {
		    	Chest(player, null);
		    	player.sm("Won the spring cleaner.");
		    	return;
		    }
			player.getInventory().addItemDrop(Rare[reward][0], Rare[reward][1]);
			player.getInventory().addItemDrop(1631, 1);		
		}
		if (rarity > 970 && rarity <= 997)  {
		    int length = SuperRare.length;
			length--;
		    int reward = Utils.getRandom(length);
			player.getInventory().addItemDrop(SuperRare[reward][0], SuperRare[reward][1]);
			player.getInventory().addItemDrop(1631, 1);	
		}
		if (rarity > 997 && rarity <= 1000)  {
		    int length = Legendary.length;
			length--;
		    int reward = Utils.getRandom(length);
			player.getInventory().addItemDrop(Legendary[reward][0], Legendary[reward][1]);
			player.getInventory().addItemDrop(1631, 1);
			String reward2 = ItemDefinitions.getItemDefinitions(Legendary[reward][0]).getName().toLowerCase();		
			World.sendWorldMessage("<img=5><col=ff7000>" + player.getDisplayName() + " has just recieved a " + reward2 + " from the Crystal chest!", false);
		}	
		removeKey(player);
        player.crystalChest++; 
	}
    
	private static void removeKey(Player player) {
		player.getInventory().deleteItem(new Item(989));
	}

	public static void handleObject(Player player, final WorldObject object) {

	}

}
