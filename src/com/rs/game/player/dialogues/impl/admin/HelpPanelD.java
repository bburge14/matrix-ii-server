package com.rs.game.player.dialogues.impl.admin;

import com.rs.game.player.Player;
import com.rs.game.player.dialogues.Dialogue;

/**
 * Help/commands panel. Shows commands available to the player based on rights.
 * Opened via ::commands or ::help.
 *
 * Stages:
 *   0  = main menu
 *   10 = paginated command list (actual page stored in temp attribute)
 *   20 = detail view for a selected command
 */
public class HelpPanelD extends Dialogue {

    private static final String T_PAGE = "help_page";
    private static final String T_SELECTED_IDX = "help_selected_idx";
    private static final int CMDS_PER_PAGE = 4;

    /** Command entry: name, syntax/usage, description, required rights (0=player, 1=mod, 2=admin/owner). */
    private static final Object[][] COMMANDS = {
        // Player-tier
        {"home",         "::home",                        "Teleport to your home location.", 0},
        {"commands",     "::commands",                    "Open this help panel.", 0},
        {"help",         "::help",                        "Open this help panel.", 0},
        {"players",      "::players",                     "Show the online player count.", 0},
        {"kdr",          "::kdr",                         "Show your kill/death ratio.", 0},
        {"ticket",       "::ticket <message>",            "Submit a support ticket.", 0},
        {"yell",         "::yell <message>",              "Send a message to the yell channel.", 0},
        {"vote",         "::vote",                        "Open the vote page (currently unavailable).", 0},
        {"mode",         "::mode",                        "Switch between EoC and Legacy combat.", 0},
        {"spellbook",    "::spellbook",                   "Open the spellbook switcher.", 0},

        // Mod-tier (rights >= 1)
        {"mute",         "::mute <name>",                 "Mute a player.", 1},
        {"unmute",       "::unmute <name>",               "Unmute a player.", 1},

        // Admin-tier (rights >= 2)
        {"admin",        "::admin",                       "Open the admin panel (GUI).", 2},
        {"teleto",       "::teleto <name>",               "Teleport to a player.", 2},
        {"teletome",     "::teletome <name>",             "Bring a player to you.", 2},
        {"tele",         "::tele <x,y,z>",                "Teleport to coordinates.", 2},
        {"item",         "::item <id> <amount>",          "Spawn an item.", 2},
        {"forceitem",    "::forceitem <id>",              "Force spawn an item (ignores checks).", 2},
        {"setlevel",     "::setlevel <skill> <level>",    "Set a skill level on yourself.", 2},
        {"god",          "::god",                         "Toggle god mode.", 2},
        {"kill",         "::kill <name>",                 "Instant-kill a player.", 2},
        {"forcekick",    "::forcekick <name>",            "Force-kick a player offline.", 2},
        {"unban",        "::unban <name>",                "Remove a ban.", 2},
        {"punish",       "::punish <name>",               "Open punish dialog for a player.", 2},
        {"reloadshops",  "::reloadshops",                 "Reload all shop data.", 2},
        {"recalcprices", "::recalcprices",                "Recalculate GE prices.", 2},
        {"shop",         "::shop <id>",                   "Open a specific shop.", 2},
        {"master",       "::master",                      "Master login / admin bypass mode.", 2},
        {"hide",         "::hide",                        "Toggle invisibility.", 2},
        {"shutdown",     "::shutdown",                    "Shut down the server cleanly.", 2},
        {"resetbarrows", "::resetbarrows",                "Reset your Barrows kill count.", 2},
        {"startevent",   "::startevent <id>",             "Start a world event.", 2},
        {"stopevent",    "::stopevent",                   "Stop the current event.", 2},
    };

    @Override
    public void start() {
        openMainMenu();
    }

    private void openMainMenu() {
        stage = 0;
        String lvl = "player";
        if (player.getRights() == 1) lvl = "mod";
        else if (player.getRights() >= 2) lvl = "admin/owner";
        sendOptionsDialogue("Help Panel (your rank: " + lvl + ")",
            "Browse my commands",
            "About the server",
            "Close");
    }

    private int[] getVisibleIndexes() {
        // Indexes into COMMANDS that this player can see
        int rights = player.getRights();
        int count = 0;
        for (Object[] row : COMMANDS) if ((Integer) row[3] <= rights) count++;
        int[] out = new int[count];
        int w = 0;
        for (int i = 0; i < COMMANDS.length; i++) {
            if ((Integer) COMMANDS[i][3] <= rights) out[w++] = i;
        }
        return out;
    }

    private void openCommandsPage(int page) {
        int[] idxs = getVisibleIndexes();
        int start = page * CMDS_PER_PAGE;
        if (start >= idxs.length) { openMainMenu(); return; }
        player.getTemporaryAttributtes().put(T_PAGE, page);
        stage = 10;
        String o1 = start + 0 < idxs.length ? (String) COMMANDS[idxs[start + 0]][1] : "-";
        String o2 = start + 1 < idxs.length ? (String) COMMANDS[idxs[start + 1]][1] : "-";
        String o3 = start + 2 < idxs.length ? (String) COMMANDS[idxs[start + 2]][1] : "-";
        String o4 = start + 3 < idxs.length ? (String) COMMANDS[idxs[start + 3]][1] : "-";
        boolean hasMore = (start + CMDS_PER_PAGE) < idxs.length;
        String o5 = hasMore ? "Next page" : "Back to menu";
        sendOptionsDialogue("Commands (page " + (page + 1) + " of " + ((idxs.length + CMDS_PER_PAGE - 1) / CMDS_PER_PAGE) + ")", o1, o2, o3, o4, o5);
    }

    private void openCommandDetail(int commandIdx) {
        if (commandIdx < 0 || commandIdx >= COMMANDS.length) { openMainMenu(); return; }
        stage = 20;
        String name = (String) COMMANDS[commandIdx][0];
        String syntax = (String) COMMANDS[commandIdx][1];
        String desc = (String) COMMANDS[commandIdx][2];
        sendDialogue(syntax + "<br><br>" + desc);
    }

    private void openAboutPage() {
        stage = 20;
        sendDialogue("Welcome to Brad's Playground.<br><br>Type ::commands to browse available commands.<br>For admins: ::admin opens the admin panel.");
    }

    @Override
    public void run(int interfaceId, int componentId) {
        if (stage == 0) {
            if (componentId == OPTION_1) openCommandsPage(0);
            else if (componentId == OPTION_2) openAboutPage();
            else end();
            return;
        }

        if (stage == 10) {
            Integer page = (Integer) player.getTemporaryAttributtes().get(T_PAGE);
            if (page == null) page = 0;
            int[] idxs = getVisibleIndexes();
            int start = page * CMDS_PER_PAGE;
            int clickedSlot = -1;
            if (componentId == OPTION_1) clickedSlot = 0;
            else if (componentId == OPTION_2) clickedSlot = 1;
            else if (componentId == OPTION_3) clickedSlot = 2;
            else if (componentId == OPTION_4) clickedSlot = 3;
            else if (componentId == OPTION_5) {
                boolean hasMore = (start + CMDS_PER_PAGE) < idxs.length;
                if (hasMore) openCommandsPage(page + 1);
                else openMainMenu();
                return;
            }
            int cmdListIdx = start + clickedSlot;
            if (clickedSlot >= 0 && cmdListIdx < idxs.length) {
                player.getTemporaryAttributtes().put(T_SELECTED_IDX, idxs[cmdListIdx]);
                openCommandDetail(idxs[cmdListIdx]);
            } else {
                openMainMenu();
            }
            return;
        }

        if (stage == 20) {
            // Any click on a detail/info page goes back to main menu
            Integer page = (Integer) player.getTemporaryAttributtes().get(T_PAGE);
            if (page != null) {
                openCommandsPage(page);
            } else {
                openMainMenu();
            }
            return;
        }
    }

    @Override
    public void finish() {
        player.getTemporaryAttributtes().remove(T_PAGE);
        player.getTemporaryAttributtes().remove(T_SELECTED_IDX);
    }
}
