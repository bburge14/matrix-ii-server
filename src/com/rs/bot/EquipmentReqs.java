package com.rs.bot;

import com.rs.game.player.Player;
import com.rs.game.player.Skills;

/**
 * Bot equipment level requirements. Maps item IDs to a (skill, level)
 * pair so BotEquipment can refuse to equip gear the bot can't actually
 * wear in-game.
 *
 * Without this, the wealth-tier system in BotEquipment would equip
 * level-3 bots in rune armor and dragon scimitars - the user noticed
 * and called it out. Real RS gates equipment by Defence (armor),
 * Attack (most weapons), Ranged (bows), Magic (spell-power gear), or
 * Strength (a few exotic weapons).
 *
 * Unknown item IDs default to "no requirement" so the bot can still
 * try them - safer than randomly stripping cosmetics. The tradeoff
 * is that a few odd items might slip through; flag them as we find
 * them.
 */
public final class EquipmentReqs {

    private EquipmentReqs() {}

    public static final int NONE = -1;

    /**
     * Returns true if the bot has the level required to wear this item.
     * Unknown items pass through.
     */
    public static boolean canWear(Player bot, int itemId) {
        int[] req = getRequirement(itemId);
        if (req[0] == NONE) return true;
        try {
            return bot.getSkills().getLevel(req[0]) >= req[1];
        } catch (Throwable t) {
            return true; // be permissive on lookup failure
        }
    }

    /**
     * (skillId, level) for an item. Skill = -1 means no skill requirement.
     */
    public static int[] getRequirement(int itemId) {
        switch (itemId) {
            // ---- ARMOR (Defence requirement) ----
            // Bronze - no req
            case 1155: case 1117: case 1075: case 1189: case 1321:
                return new int[] { NONE, 1 };
            // Iron - 1 def for armor, no req for weapons
            case 1153: case 1115: case 1067: case 1191: case 4121:
                return new int[] { Skills.DEFENCE, 1 };
            // Steel - 5 def
            case 1157: case 1119: case 1069: case 1193:
                return new int[] { Skills.DEFENCE, 5 };
            // Black armor - 10 def
            case 1165: case 1125: case 1077: case 1197:
                return new int[] { Skills.DEFENCE, 10 };
            // Mithril - 20 def
            case 1159: case 1121: case 1071: case 1199: case 1129: case 1095:
                return new int[] { Skills.DEFENCE, 20 };
            // Adamant - 30 def
            case 1161: case 1123: case 1073: case 1167: case 1131: case 1099:
                return new int[] { Skills.DEFENCE, 30 };
            // Rune armor - 40 def
            case 1163: case 1127: case 1113: case 1079: case 1093:
            case 1201: case 4131:
                return new int[] { Skills.DEFENCE, 40 };
            // Rune boots variant
            case 4097: case 4101: case 4103: case 4105: case 4107: case 4099: // mystic gear
                return new int[] { Skills.MAGIC, 40 };
            // Dragon armor - 60 def
            case 1149: case 3140: case 4087: case 1187: case 11732:
                return new int[] { Skills.DEFENCE, 60 };
            // Bandos - 65 def
            case 11724: case 11726: case 11728:
                return new int[] { Skills.DEFENCE, 65 };
            // Armadyl ranged - 70 ranged
            case 11716: case 11718: case 11720:
                return new int[] { Skills.RANGE, 70 };
            // Barrows armor - 70 def
            case 4716: case 4718: case 4720: case 4722:
            case 4724: case 4726: case 4728: case 4730:
            case 4745: case 4747: case 4749: case 4751:
            case 4753: case 4755: case 4757: case 4759:
                return new int[] { Skills.DEFENCE, 70 };
            // Statius - 78 def
            case 13884: case 13890: case 13896: case 13902:
                return new int[] { Skills.DEFENCE, 78 };
            // Torva - 80 def + 80 con (we just check defence)
            case 20137: case 20141: case 20145: case 24977:
                return new int[] { Skills.DEFENCE, 80 };
            // Proselyte - 30 def + 30 prayer (use defence)
            case 9672: case 9674: case 9676:
                return new int[] { Skills.DEFENCE, 30 };

            // ---- MELEE WEAPONS (Attack requirement) ----
            case 1323:                      return new int[] { Skills.ATTACK, 1 };  // Iron scim
            case 1325:                      return new int[] { Skills.ATTACK, 5 };  // Steel scim
            case 1327:                      return new int[] { Skills.ATTACK, 10 }; // Black scim
            case 1329:                      return new int[] { Skills.ATTACK, 20 }; // Mithril scim
            case 1331:                      return new int[] { Skills.ATTACK, 30 }; // Adamant scim
            // Rune weapons - 40 attack
            case 1333: case 1303: case 1213: case 1432: case 1373:
                return new int[] { Skills.ATTACK, 40 };
            // Dragon weapons - 60 attack
            case 4587: case 1305: case 1377: case 1434: case 5698:
                return new int[] { Skills.ATTACK, 60 };
            // Abyssal whip / vine whip - 70 attack
            case 4151: case 21371: case 21372: case 21373: case 21374: case 21375:
                return new int[] { Skills.ATTACK, 70 };
            // Godsword - 75 attack
            case 11694: case 11696: case 11698: case 11700:
                return new int[] { Skills.ATTACK, 75 };
            // Staff of Light - 75 attack + 75 magic, gate on attack
            case 15486:
                return new int[] { Skills.ATTACK, 75 };
            // Chaotic weapons - 80 attack + 80 dungeoneering (use attack)
            case 18349: case 18351: case 18353:
                return new int[] { Skills.ATTACK, 80 };
            // Drygore - 90 attack
            case 26579: case 26587: case 26595:
                return new int[] { Skills.ATTACK, 90 };

            // ---- RANGED ----
            // Magic shortbow - 50 ranged, willow short - 20, etc.
            case 841: case 839:             return new int[] { Skills.RANGE, 1 };  // shortbow / longbow
            case 843: case 845:             return new int[] { Skills.RANGE, 5 };  // oak
            case 849: case 847:             return new int[] { Skills.RANGE, 20 }; // willow
            case 853: case 851:             return new int[] { Skills.RANGE, 30 }; // maple
            case 859: case 857:             return new int[] { Skills.RANGE, 40 }; // yew
            case 861: case 863:             return new int[] { Skills.RANGE, 50 }; // magic
            // Crystal bow / dark bow / dark beast bow
            case 9185:                      return new int[] { Skills.RANGE, 70 }; // dark bow
            // Karil's crossbow
            case 4734:                      return new int[] { Skills.RANGE, 70 };

            // ---- MAGIC ----
            // Staff of air/water/earth/fire - no req
            case 1381: case 1383: case 1385: case 1387:
                return new int[] { NONE, 1 };
            // Mystic gear handled in armor block above.

            // ---- SHIELDS / OFFHAND ----
            case 1540:  // Anti-dragon shield - no req
                return new int[] { NONE, 1 };
            case 11283: // Dragonfire shield - 75 def
                return new int[] { Skills.DEFENCE, 75 };
            case 13738: case 13740: case 13742: case 13744: // Spirit shields
                return new int[] { Skills.DEFENCE, 75 };

            // ---- AMULETS / RINGS / CAPES (mostly no level req) ----
            case 1725: case 1727: case 1731: case 1712:  // amulets of strength/power/glory/fury
            case 6568: case 9748:                         // capes
            case 1733:                                    // ring of dueling
                return new int[] { NONE, 1 };

            // ---- SKILLCAPES - each requires level 99 in the matching skill ----
            // Untrimmed and trimmed share the same level req. The "trimmed"
            // variant in vanilla RS only unlocks after a SECOND 99, but we
            // gate on the primary skill only.
            case 9747: case 9748:  return new int[] { Skills.ATTACK,        99 };
            case 9750: case 9751:  return new int[] { Skills.DEFENCE,       99 };
            case 9753: case 9754:  return new int[] { Skills.STRENGTH,      99 };
            case 9756: case 9757:  return new int[] { Skills.HITPOINTS,     99 };
            case 9759: case 9760:  return new int[] { Skills.RANGE,         99 };
            case 9762: case 9763:  return new int[] { Skills.PRAYER,        99 };
            case 9765: case 9766:  return new int[] { Skills.MAGIC,         99 };
            case 9768: case 9769:  return new int[] { Skills.COOKING,       99 };
            case 9771: case 9772:  return new int[] { Skills.WOODCUTTING,   99 };
            case 9774: case 9775:  return new int[] { Skills.FLETCHING,     99 };
            case 9777: case 9778:  return new int[] { Skills.FISHING,       99 };
            case 9780: case 9781:  return new int[] { Skills.FIREMAKING,    99 };
            case 9783: case 9784:  return new int[] { Skills.CRAFTING,      99 };
            case 9786: case 9787:  return new int[] { Skills.SMITHING,      99 };
            case 9789: case 9790:  return new int[] { Skills.MINING,        99 };
            case 9792: case 9793:  return new int[] { Skills.HERBLORE,      99 };
            case 9795: case 9796:  return new int[] { Skills.AGILITY,       99 };
            case 9798: case 9799:  return new int[] { Skills.THIEVING,      99 };
            case 9801: case 9802:  return new int[] { Skills.SLAYER,        99 };
            case 9804: case 9805:  return new int[] { Skills.FARMING,       99 };
            case 9807: case 9808:  return new int[] { Skills.RUNECRAFTING,  99 };
            case 9810: case 9811:  return new int[] { Skills.HUNTER,        99 };
            case 9813: case 9814:  return new int[] { Skills.CONSTRUCTION,  99 };
            case 9948: case 9949:  return new int[] { Skills.SUMMONING,     99 };
            case 12169: case 12170:return new int[] { Skills.DUNGEONEERING, 120 };
            case 18508: case 18509:return new int[] { Skills.DIVINATION,    99 };
            // Max cape / completionist cape - hardcode a high defence
            // requirement as a stand-in. The fully-correct check is "99
            // in every skill" but a per-cb threshold is fine for the bot's
            // purpose (only max-tier bots roll into these slots anyway).
            case 20767: case 20768: case 20769: case 20770: case 20771:
                return new int[] { Skills.DEFENCE, 99 };
            case 19708: case 19709:  // Wilderness sword cape (req varies)
                return new int[] { Skills.ATTACK, 60 };
            // Quest point cape (15706 / 15707) - normally requires all
            // quests complete, but we use a high-defence stand-in.
            case 15706: case 15707:
                return new int[] { Skills.DEFENCE, 80 };
            // Achievement / task cape (29185 in some 718 caches)
            case 29185:
                return new int[] { Skills.DEFENCE, 80 };

            // ---- DEFAULT ----
            default: return new int[] { NONE, 1 };
        }
    }
}
