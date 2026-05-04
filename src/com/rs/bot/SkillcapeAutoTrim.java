package com.rs.bot;

import com.rs.game.item.Item;
import com.rs.game.player.Equipment;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;

/**
 * Auto-trim a player's untrimmed skillcape when they gain a 2nd 99.
 * Mirrors the real RS implicit behaviour - own a cape of accomplishment
 * + reach 99 in another skill = the cape becomes trimmed.
 *
 * Hooked from Skills.addXp() at the moment any skill hits level 99 or
 * 120 (dungeoneering). Walks inventory + equipped cape slot, replaces
 * any untrimmed cape with its trimmed variant (id+1).
 *
 * Manual trim path lives in DZSkillingMaster dialogue for players who
 * already had multiple 99s when this code shipped.
 */
public final class SkillcapeAutoTrim {

    private SkillcapeAutoTrim() {}

    /** Skillcape untrimmed-id range. Trimmed = untrim+1, hood = untrim+2.
     *  Same table as DZSkillingMaster keeps. */
    private static final int[] UNTRIMMED_CAPE_IDS = {
        9747, 9750, 9753, 9756, 9759, 9762, 9765, 9768, 9771, 9774,
        9777, 9780, 9783, 9786, 9789, 9792, 9795, 9798, 9801, 9804,
        9807, 9810, 9948, 12169, 18508
    };

    /** Run once when a level-up to 99 fires. Trims if the player has
     *  2+ skills at 99 AND owns an untrimmed cape. */
    public static void maybeTrim(Player player) {
        if (player == null) return;
        if (countNinetyNines(player) < 2) return;

        // Inventory pass
        for (int untrim : UNTRIMMED_CAPE_IDS) {
            if (player.getInventory().containsItem(untrim, 1)) {
                player.getInventory().deleteItem(untrim, 1);
                player.getInventory().addItem(new Item(untrim + 1, 1));
                announce(player);
                return;
            }
        }
        // Equipped cape slot
        try {
            Item cape = player.getEquipment().getItem(Equipment.SLOT_CAPE);
            if (cape != null) {
                int id = cape.getId();
                for (int untrim : UNTRIMMED_CAPE_IDS) {
                    if (id == untrim) {
                        player.getEquipment().getItems().set(
                            Equipment.SLOT_CAPE, new Item(untrim + 1, 1));
                        player.getEquipment().refresh(Equipment.SLOT_CAPE);
                        player.getAppearence().generateAppearenceData();
                        announce(player);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static int countNinetyNines(Player player) {
        int n = 0;
        try {
            for (int sk = 0; sk < Skills.SKILL_NAME.length; sk++) {
                if (player.getSkills().getLevelForXp(sk) >= 99) {
                    n++;
                    if (n >= 2) return n;
                }
            }
        } catch (Throwable ignored) {}
        return n;
    }

    private static void announce(Player player) {
        try {
            player.getPackets().sendGameMessage(
                "Your untrimmed skillcape has been trimmed!");
        } catch (Throwable ignored) {}
    }
}
