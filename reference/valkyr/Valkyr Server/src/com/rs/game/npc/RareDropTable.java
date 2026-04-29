package com.rs.game.npc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.rs.game.item.Item;
import com.rs.utils.Utils;

/**
 * 
 * @author Andy || ReverendDread Dec 7, 2016
 *
 */

public class RareDropTable {
	
	public enum RareTable {
		
		COINS(new Item(995, Utils.random(250, 500))),
		LOOP_HALF_OF_A_KEY(new Item(987)),
		TOOTH_HALF_OF_A_KEY(new Item(985)),
		UNCUT_SAPPIRE(new Item(1624)),
		UNCUT_EMERALD(new Item(1621)),	
		UNCUT_DIAMOND(new Item(1617, Utils.random(1, 2))),
		UNCUT_DIAMOND_NOTED(new Item(1618, Utils.random(45, 55))),
		UNCUT_RUBY(new Item(1619, 1)),
		UNCUT_DRAGONSTONE(new Item(1631)),
		UNCUT_DRAGONSTONE_NOTED(new Item(1632, Utils.random(45, 55))),
		CHAOS_TALISMAN(new Item(1452)),
		NATURE_TALISMAN(new Item(1462)),
		RUNE_JAVELIN(new Item(830, 5)),
		BIG_BONES_NOTED(new Item(532, Utils.random(68, 82))),
		FLAX_NOTED(new Item(1780, Utils.random(450, 550))),
		TEAK_PLANK_NOTED(new Item(8780, Utils.random(45, 55))),
		MOLTEN_GLASS_NOTED(new Item(1776, Utils.random(45, 55))),
		RUNE_ARROWHEADS(new Item(44, Utils.random(113, 230))),
		DRAGON_HELM(new Item(1149)),
		RUNE_PLATEBODY(new Item(1127)),
		AGILITY_BRAWLING_GLOVES(new Item(13849)),
		COOKING_BRAWLING_GLVOES(new Item(13857)),
		FISHING_BRAWLING_GLOVES(new Item(13856)),
		FM_BRAWLING_GLOVES(new Item(13851)),
		HUNTER_BRAWLING_GLOVES(new Item(13853)),
		MAGIC_BRAWLING_GLOVES(new Item(13847)),
		MELEE_BRAWLING_GLOVES(new Item(13845)),
		MINING_BRAWLNG_GLOVES(new Item(13852)),
		PRAYER_BRAWLING_GLOVES(new Item(13848)),
		RANGED_BRAWLING_GLOVES(new Item(13846)),
		SMITHING_BRAWLING_GLOVES(new Item(13855)),
		THEIVING_BRAWLING_GLOVES(new Item(13854)),
		WOODCUTTING_BRAWLING_GLOVES(new Item(13850)),
		RUNE_PLATEBODY_NOTED(new Item(1128, Utils.random(15, 25))),
		ADAMANT_BAR_NOTED(new Item(2362, Utils.random(14, 16))),
		RUNE_BAR_NOTED(new Item(2363, 3)),
		YEW_LOGS_NOTED(new Item(1516, Utils.random(68, 747))),
		GOLD_ORE_NOTED(new Item(445, Utils.random(90, 110))),
		RAW_LOBSTER_NOTED(new Item(377, Utils.random(135, 165))),
		DRAGON_LONGSWORD(new Item(1305)),
		MAHOGANY_PLANK_NOTED(new Item(8783, Utils.random(270, 330))),
		DRAGON_BONES_NOTED(new Item(537, Utils.random(180, 220))),
		SOFT_CLAY_NOTED(new Item(1762, Utils.random(450, 550))),
		ONYX_BOLTS(new Item(9342, Utils.random(135, 165))),
		SHIELD_LEFT_HALF(new Item(2366)),
		CRYSTAL_TRISKELION_FRAGMENT_1(new Item(28547)),
		CRYSTAL_TRISKELION_FRAGMENT_2(new Item(28548)),
		CRYSTAL_TRISKELION_FRAGMENT_3(new Item(28549)),	
		PRAYER_POTION_NOTED(new Item(2435, Utils.random(45, 55))),
		SUPER_RESTORE_NOTED(new Item(3025, Utils.random(45, 55))),
		RUNITE_ORE_NOTED(new Item(452, Utils.random(90, 110))),
		COAL_NOTED(new Item(454, Utils.random(200, 1100))),
		GRIMY_SNAPDRAGON_NOTED(new Item(3052, Utils.random(90, 110))),
		GRIMY_TORSTOL_NOTED(new Item(220, Utils.random(90, 110))),
		RAW_SHARK_NOTED(new Item(384, Utils.random(225, 275))),
		DWARF_WEED_SEED(new Item(5303, Utils.random(14, 16))),
		LANTADYME_SEED(new Item(5302, Utils.random(14, 16))),
		MAGIC_SEED(new Item(5316, Utils.random(3, 7))),
		PALM_TREE_SEED(new Item(5289, 10)),
		EARTH_TALISMAN(new Item(1440, Utils.random(65, 82))),
		FIRE_TALISMAN(new Item(1442, Utils.random(25, 35))),
		WATER_TALISMAN(new Item(1444, Utils.random(65, 82))),
		BATTLESTAFF(new Item(1392, Utils.random(180, 220))),
		DRAGON_SPEAR(new Item(1249));
		
		private final Item item;
		
		private static final List<RareTable> items = Collections.unmodifiableList(Arrays.asList(RareTable.values()));
		private static final int rareDropTableSize = items.size();
		private static final Random random = new Random();	
		
		private RareTable(Item item) {
			this.item = item;		
		}
		
		public Item getItem() {
			return item;
		}	
		
		public static RareTable randomDrop() {
			return items.get(random.nextInt(rareDropTableSize));
		}
		
	}
	
	/**
	 * @param rt RareTable
	 * @return random RareTable Item
	 */
	public static Item getRandomDrop(RareTable rt) {
		Item randomItem = rt.item;
		return randomItem;
	}
	
}
