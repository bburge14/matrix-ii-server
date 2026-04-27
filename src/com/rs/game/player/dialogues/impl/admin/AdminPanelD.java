package com.rs.game.player.dialogues.impl.admin;

import java.util.ArrayList;
import java.util.List;

import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.dialogues.Dialogue;

public class AdminPanelD extends Dialogue {

    private static final String T_TARGET = "admin_target";
    private static final String T_ACTION = "admin_input_action";
    private static final String T_SKILL = "admin_input_skill";
    private static final String T_ITEMID = "admin_input_itemid";
    private static final String T_SELFTARGET = "admin_target_self";

    public static final String ACTION_ITEM_ID = "item_id";
    public static final String ACTION_ITEM_AMOUNT = "item_amount";
    public static final String ACTION_STAT_LEVEL = "stat_level";

    private static final int PAGE_SIZE = 4;

    @Override
    public void start() {
        if (player.getRights() < 2) {
            sendDialogue("You don't have permission to use the admin panel.");
            stage = 99;
            return;
        }
        openMainMenu();
    }

    private void openMainMenu() {
        stage = 0;
        sendOptionsDialogue("Admin Panel",
            "Select a player",
            "Self actions",
            "Server actions",
            "AI Management",
            "Close");
    }

    private List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<String>();
        for (Player p : World.getPlayers()) {
            if (p == null || p.hasFinished() || !p.hasStarted()) continue;
            names.add(p.getDisplayName());
        }
        return names;
    }

    private void openPlayerListPage(int page) {
        List<String> names = getOnlinePlayerNames();
        int start = page * PAGE_SIZE;
        if (start >= names.size() && page > 0) {
            openMainMenu();
            return;
        }
        String o1 = start + 0 < names.size() ? names.get(start + 0) : "-";
        String o2 = start + 1 < names.size() ? names.get(start + 1) : "-";
        String o3 = start + 2 < names.size() ? names.get(start + 2) : "-";
        String o4 = start + 3 < names.size() ? names.get(start + 3) : "-";
        String o5 = (start + PAGE_SIZE) < names.size() ? "Next page" : "Back";
        stage = (byte) (1 + page);
        sendOptionsDialogue("Select a player (page " + (page + 1) + ")", o1, o2, o3, o4, o5);
    }

    private void openTargetActionMenu(String targetName) {
        player.getTemporaryAttributtes().put(T_TARGET, targetName);
        player.getTemporaryAttributtes().remove(T_SELFTARGET);
        stage = 10;
        sendOptionsDialogue("Actions for " + targetName,
            "Teleport to them",
            "Bring them here",
            "Give them an item",
            "More options");
    }

    private void openTargetActionMenuPage2(String targetName) {
        stage = 11;
        sendOptionsDialogue("Actions for " + targetName,
            "Set one of their stats",
            "Kick",
            "Open offence dialog (ban/mute)",
            "Back");
    }

    private void openSelfMenu() {
        player.getTemporaryAttributtes().put(T_SELFTARGET, Boolean.TRUE);
        player.getTemporaryAttributtes().remove(T_TARGET);
        stage = 20;
        sendOptionsDialogue("Self actions",
            "Give yourself an item",
            "Set one of your stats",
            "Max all stats",
            "Reset all stats",
            "Back");
    }

    private void openServerMenu() {
        stage = 30;
        sendOptionsDialogue("Server actions",
            "Show player count",
            "Broadcast a yell",
            "Back");
    }

    /**
     * Paginated skill picker. 4 skills per page + Next, 26 skills total = 7 pages (0..6).
     * Last page shows just Cancel instead of Next.
     * Stage = 40 + pageIndex (40..46).
     */
    private static final int SKILLS_PER_PAGE = 4;
    private static final int TOTAL_SKILLS = 26;

    private void openSkillSelect() {
        openSkillSelectPage(0);
    }

    private void openSkillSelectPage(int page) {
        int start = page * SKILLS_PER_PAGE;
        if (start >= TOTAL_SKILLS) { openMainMenu(); return; }
        stage = (byte) (40 + page);
        String o1 = start + 0 < TOTAL_SKILLS ? Skills.SKILL_NAME[start + 0] : "-";
        String o2 = start + 1 < TOTAL_SKILLS ? Skills.SKILL_NAME[start + 1] : "-";
        String o3 = start + 2 < TOTAL_SKILLS ? Skills.SKILL_NAME[start + 2] : "-";
        String o4 = start + 3 < TOTAL_SKILLS ? Skills.SKILL_NAME[start + 3] : "-";
        boolean hasMore = (start + SKILLS_PER_PAGE) < TOTAL_SKILLS;
        String o5 = hasMore ? "Next page" : "Cancel";
        sendOptionsDialogue("Choose skill (page " + (page + 1) + ")", o1, o2, o3, o4, o5);
    }

    private Player resolveTarget() {
        Boolean selfTarget = (Boolean) player.getTemporaryAttributtes().get(T_SELFTARGET);
        if (selfTarget != null && selfTarget) return player;
        String name = (String) player.getTemporaryAttributtes().get(T_TARGET);
        if (name == null) return null;
        return World.getPlayerByDisplayName(name);
    }

    @Override
    public void run(int interfaceId, int componentId) {
        if (stage == 99) { end(); return; }

        if (stage == 0) {
            if (componentId == OPTION_1) openPlayerListPage(0);
            else if (componentId == OPTION_2) openSelfMenu();
            else if (componentId == OPTION_3) openServerMenu();
            else if (componentId == OPTION_4) {
                end();
                player.getDialogueManager().startDialogue("AIManagementD");
                return;
            }
            else end();
            return;
        }

        if (stage >= 1 && stage <= 3) {
            int page = stage - 1;
            int slotClicked = -1;
            if (componentId == OPTION_1) slotClicked = 0;
            else if (componentId == OPTION_2) slotClicked = 1;
            else if (componentId == OPTION_3) slotClicked = 2;
            else if (componentId == OPTION_4) slotClicked = 3;
            else if (componentId == OPTION_5) {
                List<String> names = getOnlinePlayerNames();
                if ((page + 1) * PAGE_SIZE < names.size()) {
                    openPlayerListPage(page + 1);
                } else {
                    openMainMenu();
                }
                return;
            }
            List<String> names = getOnlinePlayerNames();
            int idx = page * PAGE_SIZE + slotClicked;
            if (slotClicked >= 0 && idx < names.size()) {
                openTargetActionMenu(names.get(idx));
            } else {
                openMainMenu();
            }
            return;
        }

        if (stage == 10) {
            String targetName = (String) player.getTemporaryAttributtes().get(T_TARGET);
            Player target = resolveTarget();
            if (target == null) {
                sendDialogue("That player is no longer online.");
                stage = 99;
                return;
            }
            if (componentId == OPTION_1) {
                player.setNextWorldTile(new WorldTile(target));
                player.getPackets().sendGameMessage("Teleported to " + targetName + ".");
                openMainMenu();
            } else if (componentId == OPTION_2) {
                target.setNextWorldTile(new WorldTile(player));
                player.getPackets().sendGameMessage("Brought " + targetName + " to you.");
                if (target.getPackets() != null) target.getPackets().sendGameMessage("You have been summoned by an admin.");
                openMainMenu();
            } else if (componentId == OPTION_3) {
                player.getTemporaryAttributtes().put(T_ACTION, ACTION_ITEM_ID);
                end(); player.getPackets().sendInputIntegerScript("Enter item ID to give to " + targetName + ":");
            } else if (componentId == OPTION_4) {
                openTargetActionMenuPage2(targetName);
            }
            return;
        }

        if (stage == 11) {
            String targetName = (String) player.getTemporaryAttributtes().get(T_TARGET);
            Player target = resolveTarget();
            if (componentId == OPTION_4) {
                openTargetActionMenu(targetName);
                return;
            }
            if (target == null) {
                sendDialogue("That player is no longer online.");
                stage = 99;
                return;
            }
            if (componentId == OPTION_1) {
                openSkillSelect();
            } else if (componentId == OPTION_2) {
                target.disconnect(true, false);
                player.getPackets().sendGameMessage("Kicked " + targetName + ".");
                openMainMenu();
            } else if (componentId == OPTION_3) {
                end();
                player.getDialogueManager().startDialogue("AddOffenceD", targetName);
            }
            return;
        }

        if (stage == 20) {
            if (componentId == OPTION_1) {
                player.getTemporaryAttributtes().put(T_SELFTARGET, Boolean.TRUE);
                player.getTemporaryAttributtes().put(T_ACTION, ACTION_ITEM_ID);
                end(); player.getPackets().sendInputIntegerScript("Enter item ID to give yourself:");
            } else if (componentId == OPTION_2) {
                openSkillSelect();
            } else if (componentId == OPTION_3) {
                for (int i = 0; i < 26; i++) {
                    int cap = (i == Skills.DUNGEONEERING) ? 120 : 99;
                    player.getSkills().set(i, cap);
                    player.getSkills().setXp(i, Skills.getXPForLevel(cap));
                }
                player.getAppearence().generateAppearenceData();
                player.getPackets().sendGameMessage("All stats maxed (Dung=120, others=99).");
                openMainMenu();
            } else if (componentId == OPTION_4) {
                for (int i = 0; i < 26; i++) {
                    player.getSkills().set(i, i == Skills.HITPOINTS ? 10 : 1);
                    player.getSkills().setXp(i, i == Skills.HITPOINTS ? 1184 : 0);
                }
                player.getAppearence().generateAppearenceData();
                player.getPackets().sendGameMessage("All stats reset.");
                openMainMenu();
            } else if (componentId == OPTION_5) {
                openMainMenu();
            }
            return;
        }

        if (stage == 30) {
            if (componentId == OPTION_1) {
                int count = 0;
                for (Player p : World.getPlayers())
                    if (p != null && p.hasStarted() && !p.hasFinished()) count++;
                sendDialogue("There are " + count + " players online.");
                stage = 99;
            } else if (componentId == OPTION_2) {
                World.sendWorldMessage("<col=ff0000>[ADMIN] " + player.getDisplayName() + " used the admin panel.</col>", false);
                player.getPackets().sendGameMessage("Broadcast sent.");
                openMainMenu();
            } else {
                openMainMenu();
            }
            return;
        }

        // Skill select — stages 40..46 (7 pages)
        if (stage >= 40 && stage <= 46) {
            int page = stage - 40;
            int start = page * SKILLS_PER_PAGE;
            int skill = -1;
            if (componentId == OPTION_1 && start + 0 < TOTAL_SKILLS) skill = start + 0;
            else if (componentId == OPTION_2 && start + 1 < TOTAL_SKILLS) skill = start + 1;
            else if (componentId == OPTION_3 && start + 2 < TOTAL_SKILLS) skill = start + 2;
            else if (componentId == OPTION_4 && start + 3 < TOTAL_SKILLS) skill = start + 3;
            else if (componentId == OPTION_5) {
                boolean hasMore = (start + SKILLS_PER_PAGE) < TOTAL_SKILLS;
                if (hasMore) openSkillSelectPage(page + 1);
                else openMainMenu();
                return;
            }
            if (skill >= 0) {
                int cap = (skill == Skills.DUNGEONEERING) ? 120 : 99;
                player.getTemporaryAttributtes().put(T_SKILL, skill);
                player.getTemporaryAttributtes().put(T_ACTION, ACTION_STAT_LEVEL);
                end(); player.getPackets().sendInputIntegerScript("Enter level (1-" + cap + "):");
            }
            return;
        }
    }

    @Override
    public void finish() {
        // Don't wipe temp attributes — handleIntegerInput needs them to survive
        // dialogue close until the user types their number.
        // The handler cleans up each attribute itself after consuming.
    }

    public static boolean handleIntegerInput(Player player, int value) {
        String action = (String) player.getTemporaryAttributtes().get(T_ACTION);
        System.err.println("[AdminPanelD] handleIntegerInput value=" + value + " action=" + action);
        if (action == null) return false;

        if (ACTION_ITEM_ID.equals(action)) {
            if (value <= 0 || value > 30000) {
                player.getPackets().sendGameMessage("Invalid item ID.");
                player.getTemporaryAttributtes().remove(T_ACTION);
                return true;
            }
            player.getTemporaryAttributtes().put(T_ITEMID, value);
            player.getTemporaryAttributtes().put(T_ACTION, ACTION_ITEM_AMOUNT);
            player.getDialogueManager().finishDialogue(); player.getPackets().sendInputIntegerScript("Enter amount:");
            return true;
        }

        if (ACTION_ITEM_AMOUNT.equals(action)) {
            Integer itemId = (Integer) player.getTemporaryAttributtes().remove(T_ITEMID);
            player.getTemporaryAttributtes().remove(T_ACTION);
            if (itemId == null || value <= 0) {
                player.getPackets().sendGameMessage("Invalid amount.");
                return true;
            }
            Boolean isSelf = (Boolean) player.getTemporaryAttributtes().get(T_SELFTARGET);
            Player target = player;
            if (isSelf == null || !isSelf) {
                String name = (String) player.getTemporaryAttributtes().get(T_TARGET);
                target = name != null ? World.getPlayerByDisplayName(name) : null;
            }
            if (target == null) {
                player.getPackets().sendGameMessage("Target no longer online.");
                return true;
            }
            target.getInventory().addItem(new Item(itemId, value));
            player.getPackets().sendGameMessage("Gave " + value + "x item " + itemId + " to " + target.getDisplayName() + ".");
            player.getDialogueManager().finishDialogue();
            player.getDialogueManager().startDialogue("AdminPanelD");
            return true;
        }

        if (ACTION_STAT_LEVEL.equals(action)) {
            Integer skill = (Integer) player.getTemporaryAttributtes().remove(T_SKILL);
            player.getTemporaryAttributtes().remove(T_ACTION);
            if (skill == null) return true;
            int cap = (skill == Skills.DUNGEONEERING) ? 120 : 99;
            if (value < 1 || value > cap) {
                player.getPackets().sendGameMessage("Level must be between 1 and " + cap + ".");
                return true;
            }
            Boolean isSelf = (Boolean) player.getTemporaryAttributtes().get(T_SELFTARGET);
            Player target = player;
            if (isSelf == null || !isSelf) {
                String name = (String) player.getTemporaryAttributtes().get(T_TARGET);
                target = name != null ? World.getPlayerByDisplayName(name) : null;
            }
            if (target == null) {
                player.getPackets().sendGameMessage("Target no longer online.");
                return true;
            }
            target.getSkills().set(skill, value);
            target.getSkills().setXp(skill, Skills.getXPForLevel(value));
            target.getAppearence().generateAppearenceData();
            player.getPackets().sendGameMessage("Set " + Skills.SKILL_NAME[skill] + " to " + value + " for " + target.getDisplayName() + ".");
            player.getDialogueManager().finishDialogue();
            player.getDialogueManager().startDialogue("AdminPanelD");
            return true;
        }

        return false;
    }
}
