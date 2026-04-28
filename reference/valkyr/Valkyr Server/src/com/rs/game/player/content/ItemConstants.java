package com.rs.game.player.content;

import com.rs.cache.loaders.ItemDefinitions;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.SlayerManager;
import com.rs.game.player.QuestManager.Quests;
import com.rs.game.player.content.ItemConstants;
import com.rs.game.minigames.WarriorsGuild;
import com.rs.game.player.Equipment;
import com.rs.utils.Utils;

public class ItemConstants {
	
    public static boolean isDestroy(Item item) {
    	return item.getDefinitions().isDestroyItem() || item.getDefinitions().isLended();
    }
	
	public static int getAssassinModels(int slot, boolean male) {		
		switch (slot) {
			case Equipment.SLOT_FEET:
				return male ? 92050 : 71862;
			case Equipment.SLOT_LEGS:
				return male ? 56859 : 57746;
			case Equipment.SLOT_CHEST:
				return male ? 56895 : 57950;
			case Equipment.SLOT_HAT:
				return male ? 187 : 363;
			case Equipment.SLOT_CAPE:
				return male ? 95609 : 95622;
		}		
		return 0;
	}

	public static int getDegradeItemWhenWear(int id) {
		// pvp armors
		if (id == 13958 || id == 13961 || id == 13964 || id == 13967
				|| id == 13970 || id == 13973 || id == 13858 || id == 13861
				|| id == 13864 || id == 13867 || id == 13870 || id == 13873
				|| id == 13876 || id == 13884 || id == 13887 || id == 13890
				|| id == 13893 || id == 13896 || id == 13899 || id == 13902
				|| id == 13905 || id == 13908 || id == 13911 || id == 13914
				|| id == 13917 || id == 13920 || id == 13923 || id == 13926
				|| id == 13929 || id == 13932 || id == 13935 || id == 13938
				|| id == 13941 || id == 13944 || id == 13947 || id == 13950
				|| id == 13958)
			return id + 2; // if you wear it it becomes corrupted LOL
		return -1;
	}
	

	// return amt of charges
	public static int getItemDefaultCharges(int id) {
		// pvp armors
		if (id == 13910 || id == 13913 || id == 13916 || id == 13919
				|| id == 13922 || id == 13925 || id == 13928 || id == 13931
				|| id == 13934 || id == 13937 || id == 13940 || id == 13943
				|| id == 13946 || id == 13949 || id == 13952)
			return 1500;
		if (id == 13960 || id == 13963 || id == 13966 || id == 13969
				|| id == 13972 || id == 13975)
			return 3000;
		if (id == 13860 || id == 13863 || id == 13866 || id == 13869
				|| id == 13872 || id == 13875 || id == 13878 || id == 13886
				|| id == 13889 || id == 13892 || id == 13895 || id == 13898
				|| id == 13901 || id == 13904 || id == 13907 || id == 13960)
			return 6000; // 1hour
		// nex armors
		if (id == 20137 || id == 20141 || id == 20145 || id == 20149
				|| id == 20153 || id == 20157 || id == 20161 || id == 20165
				|| id == 20169 || id == 20173)
			return 60000;
		return -1;
	}

	// return what id it degrades to, -1 for disapear which is default so we
	// dont add -1
	public static int getItemDegrade(int id) {
		if (id == 11285) // DFS
			return 11283;
		// nex armors
		if (id == 20137 || id == 20141 || id == 20145 || id == 20149
				|| id == 20153 || id == 20157 || id == 20161 || id == 20165
				|| id == 20169 || id == 20173)
			return id + 1;
		return -1;
	}

	public static int getDegradeItemWhenCombating(int id) {
		// nex armors
		if (id == 20135 || id == 20139 || id == 20143 || id == 20147
				|| id == 20151 || id == 20155 || id == 20159 || id == 20163
				|| id == 20167 || id == 20171)
			return id + 2;
		return -1;
	}

	public static boolean itemDegradesWhileHit(int id) {
		if (id == 2550)
			return true;
		return false;
	}

	public static boolean itemDegradesWhileWearing(int id) {
		String name = ItemDefinitions.getItemDefinitions(id).getName().toLowerCase();
		if (name.contains("c. dragon") || name.contains("corrupt dragon") || name.contains("vesta's") || name.contains("statius'") || name.contains("morrigan's") || name.contains("zuriel's"))
			return true;
		return false;
	}

	public static boolean itemDegradesWhileCombating(int id) {
		String name = ItemDefinitions.getItemDefinitions(id).getName().toLowerCase();
		if (name.contains("torva") || name.contains("pernix") || name.contains("virtux") || name.contains("zaryte"))
			return true;
		return false;
	}
	
	public static boolean hasCompletionistCapeTrimmedReqs(Player player) {
		if (!hasCompletionistCapeReqs(player)) {
			player.sm("You need to be able to wear the Completionists cape untrimmed to wear this.");
			return false;
		}
		if (!(player.getDominionTower().getKilledBossesCount() < 500)) {
			player.sm("You need to kill at least 500 dominion tower boss kills to wear this.");
			return false;
		}		
		return true;
	}
	
	public static boolean hasCompletionistCapeReqs(Player player) {
		for (int skill = 0; skill < 25; skill++) {
			if (player.getSkills().getLevelForXp(skill) < (skill == Skills.DUNGEONEERING  || skill == Skills.SLAYER ? 120 : 99)) {
				player.sm("You must have the maximum level of each skill in order to wear this.");
				return false;
			}
		}
		if (!player.isKilledQueenBlackDragon()) {
			player.sm("You need to kill the Queen Black Dragon at least once to wear this.");
			return false;
		}
		if (!player.hasCompletedFightCaves()) {
			player.sm("You need to complete at least once Fight Caves minigame to wear this.");
			return false;
		}
		if (!player.hasCompletedFightKiln()) {
			player.sm("You need to complete at least once Fight Kiln minigame to wear this.");
			return false;
		}
		if (player.burntLogs < 2500) {
			player.sm("You need to burn at least 2,500 logs to wear this.");
			player.sm("Your currently at: " + player.burntLogs);
			return false;
		}
		if (player.completedDungeons < 30) {
			player.sm("You need to complete at least 25 dungoneering dungeons to wear this.");
			player.sm("Your currently at: " + player.completedDungeons);
			return false;
		}
		if (player.choppedIvy < 2000) {
			player.sm("You need to cut at least 2000 choaking ivy to wear this.");
			player.sm("Your currently at: " + player.choppedIvy);
			return false;
		}
		if (player.crystalChest < 100) {
			player.sm("You need to open at least 100 crystal chests to wear this.");
			player.sm("Your currently at: " + player.crystalChest);
			return false;
		}
		if (player.runiteOre < 200) {
			player.sm("You need to mine at least 200 runite ore to wear this.");
			player.sm("Your currently at: " + player.runiteOre);
			return false;
		}
		if (player.cannonBall < 2000) {
			player.sm("You need to make at least 8,000 cannon balls to wear this.");
			return false;
		}
		if (!player.hasAgileSet(player)) {
			player.sm("You need to own at least one set of Agile gear.");
			return false;
		}
		if (learnedSlayerReq(player)) {
			player.sm("You need to purchase all slayer perks and own a slayer helmet to wear this.");
			return false;
		}
		return true;
	}
	
	public static boolean learnedSlayerReq(Player player) {
		SlayerManager sm = player.getSlayerManager();
		if (!sm.hasLearnedBroad())
			return false;
		if (!sm.hasLearnedQuickBlows())
			return false;
		if (!sm.hasLearnedRing())
			return false;
		if (!sm.hasLearnedSlayerHelmet())
			return false;
		if (!player.getBank().containsItem(13263, 1) || !player.getBank().containsItem(15492, 1))
			return false;
		return true;
	}

	@SuppressWarnings("unused")
	public static boolean canWear(Item item, Player player) {
		String itemName = item.getName();
		if (player.getRights() == 2)
			return true;
        if (item.getId() == 18337 || item.getId() == 27996) {
            player.sm("You can't wear this item.");
            return false;
        } else if (item.getId() == 23659 || item.getId() == 31610 || item.getId() == 31611) {
        	if (!player.hasCompletedFightKiln()) {
        		player.getPackets().sendGameMessage("You must complete the Fight Kiln to wear this.");
        		return false;
        	}
		} else if (item.getId() == 6570) {
			if (!player.hasCompletedFightCaves()) {
				player.getPackets().sendGameMessage("You must complete the Fight Caves to wear this.");
				return false;
			}
		} else if (item.getId() == 32153 || item.getId() == 20772 || item.getId() == 20771) {
			if (!hasCompletionistCapeTrimmedReqs(player))
				return false;
		} else if (item.getId() == 8856) {
		    if (!WarriorsGuild.inCatapultArea(player)) {
				player.getPackets().sendGameMessage("You may not equip this shield outside of the catapult room in the Warrior's Guild.");
				return false;
		    }
		} else if (item.getId() == 19709) {
			if (player.getSkills().getXp(Skills.DUNGEONEERING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Dungeoneering experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31282) {
			if (player.getSkills().getXp(Skills.SLAYER) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Slayer experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31272) {
			if (player.getSkills().getXp(Skills.PRAYER) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Prayer experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31292) {
			if (player.getSkills().getXp(Skills.SUMMONING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Summoning experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31275) {
			if (player.getSkills().getXp(Skills.CONSTRUCTION) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Construction experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31283) {
			if (player.getSkills().getXp(Skills.HUNTER) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Hunter experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31277) {
			if (player.getSkills().getXp(Skills.AGILITY) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Agility experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31268) {
			if (player.getSkills().getXp(Skills.ATTACK) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Attack experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31288) {
			if (player.getSkills().getXp(Skills.COOKING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Cooking experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31270) {
			if (player.getSkills().getXp(Skills.DEFENCE) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Defence experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31291) {
			if (player.getSkills().getXp(Skills.FARMING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Farming experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31289) {
			if (player.getSkills().getXp(Skills.FIREMAKING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Firemaking experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31287) {
			if (player.getSkills().getXp(Skills.FISHING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Fishing experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31273) {
			if (player.getSkills().getXp(Skills.MAGIC) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Magic experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31281) {
			if (player.getSkills().getXp(Skills.FLETCHING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Fletching experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31276) {
			if (player.getSkills().getXp(Skills.HITPOINTS) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Constitution experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31278) {
			if (player.getSkills().getXp(Skills.HERBLORE) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Herblore experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31285) {
			if (player.getSkills().getXp(Skills.MINING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Mining experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31271) {
			if (player.getSkills().getXp(Skills.RANGE) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Ranged experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31274) {
			if (player.getSkills().getXp(Skills.RUNECRAFTING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Runecrafting experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31286) {
			if (player.getSkills().getXp(Skills.SMITHING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Smithing experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31269) {
			if (player.getSkills().getXp(Skills.STRENGTH) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Strength experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31279) {
			if (player.getSkills().getXp(Skills.THIEVING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Thieving experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31280) {
			if (player.getSkills().getXp(Skills.CRAFTING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Crafting experience to wear this cape.");
				return false;
			}
		} else if (item.getId() == 31290) {
			if (player.getSkills().getXp(Skills.WOODCUTTING) <= 104273167) {
				player.getPackets().sendGameMessage("You need 104,273,167 Woodcutting experience to wear this cape.");
				return false;
			}
		}
		return true;
	}

	public static boolean isTradeable(Item item) {
		String name = ItemDefinitions.getItemDefinitions(item.getId()).getName().toLowerCase();
		if (name.contains("flaming skull"))
			return false;
		if (name.contains("overload"))
			return false;
		if (name.contains("flask"))
			return false;
		if (item.getDefinitions().isDestroyItem()
				|| item.getDefinitions().isLended()
				|| ItemConstants.getItemDefaultCharges(item.getId()) != -1)
			return false;
		if (name.contains("fire cape")
				|| name.contains("tokhaar") || name.contains("defender")
				|| name.contains("lucky")|| name.contains("lamp")
				|| name.contains("spin ticket") || name.contains("ceremonial")
				|| name.contains("diamond jubilee") || name.contains("souvenir")
				|| name.contains("diamond sceptre") || name.contains("diamond crown") || name.contains("token") || name.contains("master cape"))
			return false;
		if (name.contains("aura") || name.contains("supreme"))
			return false;
		if (name.contains("goliath") || name.contains("swift") || name.contains("spellcaster"))
			return false;
		if (name.contains("charm")) 
			return false;
		switch (item.getId()) {
		case 6570:
			return false;
		case 15440:
			return false;
		case 15439:
			return false;
		case 14936:
			return false;
		case 14937:
			return false;
		case 14938:
			return false;
		case 14939:
			return false;
		case 11323:
			return false;
		case 4251:
			return false;
		case 11328:
			return false;
		case 11329:
			return false;
		case 11330:
			return false;
		case 11331:
			return false;
		case 11332:
			return false;
		case 11333:
			return false;
		case 30065:
			return false;
		case 30064:
			return false;
		case 30368:
			return false;
		case 28833:
			return false;
		case 28828:
			return false;
		case 26567:
			return false;
		case 27215:
			return false;
		case 26569:
			return false;
		case 26568:
			return false;
		case 28685:
			return false;
		case 28630:
			return false;
		case 31753:
			return false;
		case 31752:
			return false;
		case 31751:
			return false;
		case 31750:
			return false;
		case 31749:
			return false;
		case 31748:
			return false;
		case 33809:
			return false;
		case 19983:
			return false;
		case 33717:
			return false;
		case 33803:
			return false;
		case 33802:
			return false;
		case 33800:
			return false;
		case 33801:
			return false;
		case 33824:
			return false;
		case 33823:
			return false;
		case 33822:
			return false;
		case 33821:
			return false;
		case 33820:
			return false;
		case 33819:
			return false;
		case 33785:
			return false;
		case 33786:
			return false;
		case 33789:
			return false;
		case 33790:
			return false;
		case 33799:
			return false;
		case 33792:
			return false;
		case 33788:
			return false;
		case 33813:
			return false;
		case 33808:
			return false;
		case 33805:
			return false;
		case 33806:
			return false;
		case 33807:
			return false;
		case 33778:
			return false;
		case 773:
			return false;		
		case 455:
			return false;
		default:
			return true;
		}
	}
}
