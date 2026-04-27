package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.game.item.Item;
import com.rs.game.player.Bank;
import com.rs.game.player.Equipment;
import com.rs.game.player.Inventory;
import com.rs.game.player.Skills;

/**
 * GoalStateChecker - real game-state checks for whether a goal is met.
 *
 * Each Goal carries a requirementKey + requirementValue from its GoalType
 * (e.g. "skill:attack" 99, "equipment:rune", "wealth:10m" 10000000). This
 * class interprets those strings against actual bot state - skill levels,
 * items in equipment/inventory/bank, total wealth - and returns whether
 * the bot has already accomplished it.
 *
 * The point: stop bots from "completing" goals on a fake timer. A bot
 * with full rune already shouldn't be told to go get rune; a bot at
 * 99 attack shouldn't be told to train attack to 99.
 *
 * Unknown / unsupported requirementKeys return false ("not yet met") so
 * goals stay generatable rather than vanishing silently.
 */
public final class GoalStateChecker {

    private GoalStateChecker() {}

    /**
     * Returns true if the bot has already met the requirement.
     * False = not yet met (so goal is still relevant).
     */
    public static boolean isMet(AIPlayer bot, String key, int value) {
        if (key == null) return false;
        int colon = key.indexOf(':');
        if (colon <= 0 || colon >= key.length() - 1) return false;
        String kind = key.substring(0, colon).toLowerCase();
        String subject = key.substring(colon + 1).toLowerCase();

        try {
            switch (kind) {
                case "skill":   return checkSkill(bot, subject, value);
                case "combat":  return bot.getSkills().getCombatLevel() >= value;
                case "wealth":  return totalWealth(bot) >= value;
                case "equipment": return ownsArmorSet(bot, subject);
                case "weapon": return ownsWeapon(bot, subject);
                case "item": return ownsItemById(bot, parseInt(subject), Math.max(1, value));
                default: return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    // ===== Skill checks =====

    private static boolean checkSkill(AIPlayer bot, String name, int target) {
        int skillId = skillIdByName(name);
        if (skillId < 0) return false;
        return bot.getSkills().getLevel(skillId) >= target;
    }

    private static int skillIdByName(String name) {
        switch (name) {
            case "attack":       return Skills.ATTACK;
            case "defence":      return Skills.DEFENCE;
            case "strength":     return Skills.STRENGTH;
            case "hitpoints":
            case "constitution": return Skills.HITPOINTS;
            case "ranged":
            case "range":        return Skills.RANGE;
            case "prayer":       return Skills.PRAYER;
            case "magic":        return Skills.MAGIC;
            case "cooking":      return Skills.COOKING;
            case "woodcutting":  return Skills.WOODCUTTING;
            case "fletching":    return Skills.FLETCHING;
            case "fishing":      return Skills.FISHING;
            case "firemaking":   return Skills.FIREMAKING;
            case "crafting":     return Skills.CRAFTING;
            case "smithing":     return Skills.SMITHING;
            case "mining":       return Skills.MINING;
            case "herblore":     return Skills.HERBLORE;
            case "agility":      return Skills.AGILITY;
            case "thieving":     return Skills.THIEVING;
            case "slayer":       return Skills.SLAYER;
            case "farming":      return Skills.FARMING;
            case "runecrafting": return Skills.RUNECRAFTING;
            case "construction": return Skills.CONSTRUCTION;
            case "hunter":       return Skills.HUNTER;
            case "summoning":    return Skills.SUMMONING;
            case "dungeoneering":return Skills.DUNGEONEERING;
            case "divination":   return Skills.DIVINATION;
            default:             return -1;
        }
    }

    // ===== Wealth =====

    /**
     * Sum of coins in inventory + money pouch + bank. Item-value pricing
     * is a future improvement; for now the wealth threshold checks only
     * actual gold.
     */
    public static long totalWealth(AIPlayer bot) {
        long coins = 0;
        try {
            Inventory inv = bot.getInventory();
            if (inv != null) {
                coins += inv.getCoinsAmount(); // includes money pouch
            }
        } catch (Throwable ignore) {}
        try {
            Bank bank = bot.getBank();
            if (bank != null) {
                Item bankCoins = bank.getItem(995); // 995 = coins
                if (bankCoins != null) coins += bankCoins.getAmount();
            }
        } catch (Throwable ignore) {}
        return coins;
    }

    // ===== Item ownership =====

    /**
     * True if the bot has at least 'count' of itemId across equipment,
     * inventory, money pouch (for coins), or bank.
     */
    public static boolean ownsItemById(AIPlayer bot, int itemId, int count) {
        if (itemId <= 0) return false;
        int total = 0;
        // Equipment
        try {
            Equipment eq = bot.getEquipment();
            if (eq != null) {
                for (int slot = 0; slot < 14; slot++) {
                    Item it = eq.getItem(slot);
                    if (it != null && it.getId() == itemId) total += it.getAmount();
                    if (total >= count) return true;
                }
            }
        } catch (Throwable ignore) {}
        // Inventory
        try {
            Inventory inv = bot.getInventory();
            if (inv != null) {
                total += inv.getAmountOf(itemId);
                if (total >= count) return true;
            }
        } catch (Throwable ignore) {}
        // Bank
        try {
            Bank bank = bot.getBank();
            if (bank != null) {
                Item bi = bank.getItem(itemId);
                if (bi != null) total += bi.getAmount();
            }
        } catch (Throwable ignore) {}
        return total >= count;
    }

    // ===== Armor sets =====

    /**
     * Owns at least helm + body + legs of the named set. We don't require
     * shield/weapon for the set check - those are tracked as separate
     * goals. "owns" = anywhere across equipment/inventory/bank.
     */
    private static boolean ownsArmorSet(AIPlayer bot, String set) {
        int[] pieces = armorPieces(set);
        if (pieces == null || pieces.length == 0) return false;
        // pieces is [helm, body, legs]; require all three.
        for (int piece : pieces) {
            if (!ownsItemById(bot, piece, 1)) return false;
        }
        return true;
    }

    /**
     * Helm, body, legs item IDs for each set we recognize. Conservative
     * baseline IDs from BotEquipment.java; extend as needed for variants
     * (chain vs plate, skirt vs platelegs, set keepsakes, etc.).
     */
    private static int[] armorPieces(String set) {
        switch (set) {
            // Bronze tier
            case "bronze":  return new int[] { 1155, 1117, 1075 };
            // Iron tier
            case "iron":    return new int[] { 1153, 1115, 1067 };
            // Steel tier
            case "steel":   return new int[] { 1157, 1119, 1069 };
            // Mithril tier
            case "mithril": return new int[] { 1159, 1121, 1071 };
            // Adamant tier
            case "adamant": return new int[] { 1161, 1123, 1073 };
            // Rune tier
            case "rune":    return new int[] { 1163, 1127, 1079 };
            // Dragon (chain body baseline)
            case "dragon":  return new int[] { 1149, 3140, 4087 };
            // Bandos (high-tier melee)
            case "bandos":  return new int[] { 11724, 11725, 11726 };
            // Armadyl (high-tier ranged)
            case "armadyl": return new int[] { 11716, 11718, 11720 };
            // Barrows / void / firecape are tracked as single keystone items
            // rather than full sets; return null to fall through.
            default: return null;
        }
    }

    // ===== Weapons =====

    private static boolean ownsWeapon(AIPlayer bot, String name) {
        int[] ids = weaponIds(name);
        if (ids == null) return false;
        for (int id : ids) {
            if (ownsItemById(bot, id, 1)) return true;
        }
        return false;
    }

    private static int[] weaponIds(String name) {
        switch (name) {
            case "whip":     return new int[] { 4151 }; // Abyssal whip
            case "godsword": return new int[] { 11694, 11696, 11698, 11700 }; // AGS, BGS, SGS, ZGS
            case "sol":      return new int[] { 15486 }; // Staff of Light
            case "chaotic":  return new int[] { 18349, 18351, 18353 }; // chaotic longsword/maul/staff
            case "bis":      return new int[] { 4151, 11694, 18349 }; // any best-in-slot weapon
            default:         return null;
        }
    }

    // ===== Misc =====

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }
}
