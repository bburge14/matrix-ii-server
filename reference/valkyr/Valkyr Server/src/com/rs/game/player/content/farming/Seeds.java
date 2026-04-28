package com.rs.game.player.content.farming;

import com.rs.game.item.Item;

public class Seeds {

	public enum Seed {
		
		/**
		 * Allotments
		 */
		 
		POTATO(new Item(5318, 3), new int[] { 6, 7, 8, 9, 10 }, 1, new Item(1942, 1), new int[] { 8553, 8552, 8550, 8551 }, 60),
		ONIONS(new Item(5319, 3), new int[] { 13, 14, 15, 16, 17 }, 5, new Item(1957, 1), new int[] { 8553, 8552, 8550, 8551 }, 60), 			
		CABBAGE(new Item(5324, 3), new int[] { 20, 21, 22, 23, 24 }, 7, new Item(1965, 1), new int[] { 8553, 8552, 8550, 8551 }, 60),   			
		TOMATOES(new Item(5322, 3), new int[] { 27, 28, 29, 30, 31 }, 12, new Item(1982, 1), new int[] { 8553, 8552, 8550, 8551 }, 60), 			
		SWEETCORN(new Item(5320, 3), new int[] { 34, 35, 36, 37, 38, 39, 40 }, 20, new Item(5986, 1), new int[] { 8553, 8552, 8550, 8551 }, 60),
		STRAWERRIES(new Item(5323 ,3), new int[] { 43, 44, 45, 46, 47, 48, 49 }, 31, new Item(5504, 1), new int[] { 8553, 8552, 8550, 8551 }, 60),
		WATERMELON(new Item(5321, 3), new int [] { 52, 53, 54, 55, 56, 57, 58, 59, 60 }, 47, new Item(5982, 1), new int[] { 8553, 8552, 8550, 8551 }, 60),
		
		/**
		 * Flower Patches
		 */
		 
		MARIGOLD(new Item(5096, 1), new int[] { 8, 9, 10, 11, 12 }, 2, new Item(6010, 1), new int[] { 7848, 7847 }, 60),
		ROSEMARY(new Item(5097, 1), new int[] { 13, 14, 15, 16, 17 }, 11, new Item(6014, 1), new int[] { 7848, 7847 }, 60),
		NASTURTIUM(new Item(5098, 1), new int[] { 18, 19, 20, 21, 22 }, 24, new Item(6012, 1), new int[] { 7848, 7847 }, 60),
		WOAD(new Item(5099, 1), new int[] { 23, 24, 25, 26, 27 }, 25, new Item(5738, 1), new int[] { 7848, 7847 }, 60),
		LIMPWURT(new Item(5100, 1), new int[] { 28, 29, 30, 31, 32 }, 26, new Item(225, 1), new int[] {7848, 7847}, 60),
		LILY(new Item(14589, 1), new int[] { 37, 38, 39, 40, 41 }, 52, new Item(14583, 1), new int[] {7848, 7847}, 60),
		
		/**
		 * Herb Patches
		 */
		 
		GUAM(new Item(5291, 1), new int[] { 4, 5, 6, 7, 8 }, 9, new Item(199, 4), new int[] { 8151, 8150 }, 60),
		MARRENTILL(new Item(5292, 1), new int[] {11, 12, 13, 14, 15}, 14, new Item(201, 1), new int[] { 8151, 8150 }, 60),
		TARROMIN(new Item(5293, 1), new int[] { 18, 19, 20, 21, 22 }, 19, new Item(203, 1), new int[] { 8151, 8150 }, 60),
		HARRALANDER(new Item(5294, 1), new int[] { 25, 26, 27, 28, 29 }, 19, new Item(205, 1), new int[] { 8151, 8150 }, 60),
		RANNAR(new Item(5295, 1), new int[] { 32, 33, 34, 35, 36 }, 32, new Item(207, 1), new int[] { 8151, 8150 }, 60),
		SPIRITWEED(new Item(12176, 1), new int[] { 39, 40, 41, 42, 43 }, 36, new Item(12174, 1), new int[] { 8151, 8150 }, 60),
		TOADFLAX(new Item(5296), new int[] { 46, 47, 48, 49, 50 }, 38, new Item(2998, 1), new int[] { 8151, 8150 }, 60),
		IRIT(new Item(5297, 1), new int[] { 53, 54, 55, 56, 57 }, 44, new Item(209, 1), new int[] { 8151, 8150 }, 60),
		WERGALI(new Item(14870, 1), new int[] { 60, 61, 62, 63, 64 }, 46, new Item(14836, 1), new int[] { 8151, 8150 }, 60),
		AVANTOE(new Item(5298, 1), new int[] { 68, 69, 70, 71, 72 }, 50, new Item(211, 1), new int[] { 8151, 8150 }, 60),
		KWUARM(new Item(5299, 1), new int[] { 75, 76, 77, 78, 79 }, 56, new Item (213, 1), new int[] {}, 60),
		SNAPDRAGON(new Item(5300, 1), new int[] { 82, 83, 84, 85, 86 }, 62, new Item(3051, 1), new int[] { 8151, 8150 }, 60),
		CADANTINE(new Item(5301, 1), new int[] { 89, 90, 91, 92, 93 }, 67, new Item(215, 1), new int[] { 8151, 8150 }, 60),
		LANTADYME(new Item(5302, 1), new int[] { 96, 97, 98, 99, 100 }, 73, new Item(2481, 1), new int[] { 8151, 8150 }, 60),
		DWARFWEED(new Item(5303, 1), new int[] { 103, 104, 105, 106, 107 }, 79, new Item(217, 1), new int[] { 8151, 8150 }, 60),
		TORSTOL(new Item(5304, 1), new int[] { 67, 110, 111, 112, 113 }, 85, new Item(219, 1), new int[] { 8151, 8150 }, 60),
		FELLSTALK(new Item(21621, 1), new int[] { 204, 205, 206, 207, 208 }, 91, new Item(21626, 1), new int[] { 8151, 8150 }, 60);
		
		private Item item;
		private int[] configValues;
		private int level;
		private Item produce;
		private int[] suitablePatches;
		private int time; //Time to grow (In Seconds)
		
		Seed(Item item, int[] configValues, int level, Item produce, int[] suitablePatches, int time) {
			this.item = item;
			this.configValues = configValues;
			this.level = level;
			this.produce = produce;
			this.suitablePatches = suitablePatches;
			this.time = time;
		}
		
		public Item getItem() {
			return item;
		}
		
		public int[] getConfigValues() {
			return configValues;
		}
		
		public int getLevel() {
			return level;
		}
		
		public Item getProduce() {
			return produce;
		}
		
		public int[] getSuitablePatch() {
			return suitablePatches;
		}
		
		public int getTime() {
			return time;
		}
	}
	
}