package com.rs.game.player.dialogues.impl;

import com.rs.game.item.Item;
import com.rs.game.npc.NPC;
import com.rs.game.player.Skills;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.ShopsHandler;

/**
 * Donator-zone supply + skillcape trim NPC. Same id (2253) used for the
 * Burthorpe bank clone so both NPCs share dialogue. On Talk-to:
 *   - Donators get the option set: shop / trim cape / nevermind
 *   - Non-donators get a trim-only option since the shop is gated
 *
 * Trim mechanic: player must have 99 in TWO or more skills + an
 * untrimmed skillcape (in inventory or equipped). The cape is replaced
 * with the trimmed variant (id+1). Hood (id+2) is left alone.
 */
public class DZSkillingMaster extends Dialogue {

    /** Skillcape untrimmed-id range. Trimmed = untrim+1, hood = untrim+2.
     *  All caps that auto-promote to trim follow this pattern. */
    private static final int[] UNTRIMMED_CAPE_IDS = {
        9747, 9750, 9753, 9756, 9759, 9762, 9765, 9768, 9771, 9774,
        9777, 9780, 9783, 9786, 9789, 9792, 9795, 9798, 9801, 9804,
        9807, 9810, 9948, 12169, 18508
    };

    @Override
    public void start() {
        boolean donator = player.isDonator();
        boolean canTrim = qualifiesForTrim();
        if (donator && canTrim) {
            sendOptionsDialogue("What would you like?",
                "Open shop", "Trim my skillcape", "Nevermind");
            stage = 1;
        } else if (donator) {
            player.getPackets().sendGameMessage("DZ skilling supply - top-tier tools and every skillcape.");
            ShopsHandler.openShop(player, 204);
            end();
        } else if (canTrim) {
            sendOptionsDialogue("Greetings.",
                "Trim my skillcape", "Nevermind");
            stage = 2;
        } else {
            player.getPackets().sendGameMessage("Reach 99 in 2+ skills first to trim a cape; donators may also use the shop.");
            end();
        }
    }

    @Override
    public void run(int interfaceId, int componentId) {
        if (stage == 1) {
            switch (componentId) {
                case OPTION_1:
                    ShopsHandler.openShop(player, 204);
                    end();
                    return;
                case OPTION_2:
                    trimCape();
                    end();
                    return;
                default:
                    end();
                    return;
            }
        }
        if (stage == 2) {
            if (componentId == OPTION_1) trimCape();
            end();
            return;
        }
        end();
    }

    @Override
    public void finish() {}

    /** True if player has at least 2 skills at 99 (or 99 base level via XP). */
    private boolean qualifiesForTrim() {
        try {
            int count99 = 0;
            for (int sk = 0; sk < Skills.SKILL_NAME.length; sk++) {
                if (player.getSkills().getLevelForXp(sk) >= 99) count99++;
                if (count99 >= 2) return true;
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    /** Find an untrimmed skillcape on the player + replace with trimmed.
     *  Plays a brief trim animation so it feels like a ceremony, not a
     *  silent inventory swap. */
    private void trimCape() {
        // Find what we'd trim before animating - if there's nothing, bail
        // immediately rather than waste a 5s animation lock-in.
        int untrimSlot = -1;
        int untrimId = -1;
        for (int untrim : UNTRIMMED_CAPE_IDS) {
            if (player.getInventory().containsItem(untrim, 1)) {
                untrimId = untrim;
                untrimSlot = 0;  // marker: in inventory
                break;
            }
        }
        if (untrimId == -1) {
            try {
                Item cape = player.getEquipment().getItem(
                    com.rs.game.player.Equipment.SLOT_CAPE);
                if (cape != null) {
                    for (int untrim : UNTRIMMED_CAPE_IDS) {
                        if (cape.getId() == untrim) {
                            untrimId = untrim;
                            untrimSlot = 1;  // marker: equipped
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (untrimId == -1) {
            player.getPackets().sendGameMessage(
                "I don't see an untrimmed skillcape on you.");
            return;
        }
        // Animation 4276 = "trim cape" cast, GFX 818 = trim sparkle.
        // Schedule the actual swap 4 ticks later so the animation plays.
        try {
            player.setNextAnimation(new com.rs.game.Animation(4276));
            player.setNextGraphics(new com.rs.game.Graphics(818));
            player.lock(4);
        } catch (Throwable ignored) {}
        final int finalUntrim = untrimId;
        final boolean inInv = untrimSlot == 0;
        com.rs.game.tasks.WorldTasksManager.schedule(
            new com.rs.game.tasks.WorldTask() {
                @Override public void run() {
                    try {
                        if (inInv) {
                            player.getInventory().deleteItem(finalUntrim, 1);
                            player.getInventory().addItem(new Item(finalUntrim + 1, 1));
                        } else {
                            player.getEquipment().getItems().set(
                                com.rs.game.player.Equipment.SLOT_CAPE,
                                new Item(finalUntrim + 1, 1));
                            player.getEquipment().refresh(
                                com.rs.game.player.Equipment.SLOT_CAPE);
                            player.getAppearence().generateAppearenceData();
                        }
                        player.getPackets().sendGameMessage(
                            "You have trimmed your skillcape.");
                    } catch (Throwable ignored) {}
                    stop();
                }
            }, 4);
    }
}
