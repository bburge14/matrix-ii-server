package com.rs.game.player.actions.objects;

import com.rs.game.World;
import com.rs.game.WorldObject;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.utils.Utils;
import com.rs.cache.loaders.ItemDefinitions;


public class TCrystalChest {
	
	public static void Chest(Player player, final WorldObject object) {
		int[][] Common = { {1632, 5}, {5303, 5}, {5302, 5}, {5304, 5} };
		int[][] Common2 = { {1632, 5}, {5290, 3}, {5289, 3}, {5288, 3} };
		int[][] Commom3 = { {1632, 5}, {989, 1}, {987, 2}, {985, 2}, {1513, 150} };
		int[][] Uncommon = { {1632, 1}, {5316, 1}, {5315, 1} };
		int[][] Uncommon2 = { {450, 125}, {1632, 5}, {454, 750} };
		int[][] Uncommon3 = {};
		int[][] Uncommon4 = {};
		int[][] Rare = { {1632, 5}, {6571, 1} };
		int[][] SuperRare = { {15259, 1}, {6739, 1} };
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
			World.sendWorldMessage("<img=5><col=ff7000>" + player.getDisplayName() + " has just recieved a " + reward2 + " from the Crystal triskelion chest!", false);
		}	
		removeKey(player);
        player.crystalChest++; 
	}
    
	private static void removeKey(Player player) {
		player.getInventory().deleteItem(new Item(28550));
	}

	public static void handleObject(Player player, final WorldObject object) {

	}

}
