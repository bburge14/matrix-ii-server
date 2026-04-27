package com.rs.game.player.dialogues.impl.admin;

import com.rs.bot.AIPlayer;
import com.rs.bot.BotPool;
import com.rs.game.player.dialogues.Dialogue;

/**
 * Phase 3a admin menu for the persistent bot pool.
 *
 * Three views:
 *   stage 0 = main menu (status + actions)
 *   stage 1 = online list (paginated)
 *   stage 2 = offline list (paginated)
 *
 * Each "page" is a 5-option dialogue (4 names + a "next/back" navigator).
 */
public class AIManagementD extends Dialogue {

    private static final int PAGE_SIZE = 4;

    private int stage = 0;
    private int page = 0;
    private String lastAction = "";

    @Override
    public void start() {
        renderMain();
    }

    private void renderMain() {
        stage = 0;
        String header = "AI Bots: " + (lastAction.isEmpty() ? "ready" : lastAction)
                      + " | Online: " + BotPool.getOnlineCount()
                      + " | Offline: " + BotPool.getOfflineCount();
        sendOptionsDialogue(header,
            "Spawn 5 from pool",
            "Spawn 20 from pool",
            "Despawn All Online",
            "Manage / Generate / Delete...",
            "Close");
    }

    private void renderManageMenu() {
        stage = 3;
        sendOptionsDialogue("Bot Pool Management",
            "Generate 10 new bots",
            "Generate 50 new bots",
            "List Online (" + BotPool.getOnlineCount() + ")",
            "List Offline (" + BotPool.getOfflineCount() + ")",
            "Delete ALL Bots [DESTRUCTIVE]");
    }

    private void renderOnlineList() {
        stage = 1;
        java.util.List<String> names = BotPool.snapshotOnlineNames();
        renderListPage(names, "Online bots");
    }

    private void renderOfflineList() {
        stage = 2;
        java.util.List<String> names = BotPool.snapshotOfflineNames();
        renderListPage(names, "Offline bots");
    }

    private void renderListPage(java.util.List<String> names, String label) {
        int totalPages = Math.max(1, (int) Math.ceil(names.size() / (double) PAGE_SIZE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, names.size());
        String header = label + " (" + names.size() + " total, page " + (page + 1) + "/" + totalPages + ")";

        String[] opts = new String[5];
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            opts[i] = (idx < end) ? names.get(idx) : "-";
        }
        opts[4] = (page + 1 < totalPages) ? "Next page >>" : "<< Back";

        sendOptionsDialogue(header, opts[0], opts[1], opts[2], opts[3], opts[4]);
    }

    @Override
    public void run(int interfaceId, int componentId) {
        switch (stage) {
            case 0: handleMain(componentId); break;
            case 1: handleListNav(componentId, true); break;
            case 2: handleListNav(componentId, false); break;
            case 3: handleManage(componentId); break;
            case 4: handleBotAction(componentId); break;
        }
    }

    private void handleMain(int componentId) {
        switch (componentId) {
            case OPTION_1:
                lastAction = "Spawned " + BotPool.spawn(5);
                renderMain();
                break;
            case OPTION_2:
                lastAction = "Spawned " + BotPool.spawn(20);
                renderMain();
                break;
            case OPTION_3:
                lastAction = "Despawned " + BotPool.despawnAll();
                renderMain();
                break;
            case OPTION_4:
                renderManageMenu();
                break;
            case OPTION_5:
                end();
                break;
        }
    }

    private void handleManage(int componentId) {
        switch (componentId) {
            case OPTION_1:
                lastAction = "Generated " + BotPool.generate(10);
                renderMain();
                break;
            case OPTION_2:
                lastAction = "Generated " + BotPool.generate(50);
                renderMain();
                break;
            case OPTION_3:
                page = 0;
                renderOnlineList();
                break;
            case OPTION_4:
                page = 0;
                renderOfflineList();
                break;
            case OPTION_5:
                lastAction = "DELETED ALL: " + BotPool.deleteAll();
                renderMain();
                break;
        }
    }

    private void handleListNav(int componentId, boolean online) {
        java.util.List<String> names = online
            ? BotPool.snapshotOnlineNames()
            : BotPool.snapshotOfflineNames();
        int totalPages = Math.max(1, (int) Math.ceil(names.size() / (double) PAGE_SIZE));

        if (componentId == OPTION_5) {
            // Last option is either "Next" or "Back"
            if (page + 1 < totalPages) {
                page++;
                if (online) renderOnlineList(); else renderOfflineList();
            } else {
                renderMain();
            }
            return;
        }

        // OPTION_1..4 = bot at that slot in the page
        int slot = componentId - OPTION_1; // 0..3
        int idx = page * PAGE_SIZE + slot;
        if (idx < 0 || idx >= names.size()) {
            renderMain();
            return;
        }
        String botName = names.get(idx);
        showBotActions(botName, online);
    }

    private String selectedBot;
    private boolean selectedWasOnline;

    private void showBotActions(String name, boolean wasOnline) {
        selectedBot = name;
        selectedWasOnline = wasOnline;
        stage = 4;
        sendOptionsDialogue("Bot: " + name + " [" + (wasOnline ? "ONLINE" : "OFFLINE") + "]",
            wasOnline ? "Despawn this bot (saves)" : "Spawn this bot",
            "DELETE this bot [PERMANENT]",
            "Back to main menu",
            "-",
            "-");
    }

    @Override
    public void finish() {}

    // Override to handle stage 4 (single-bot actions) without adding a case to run()
    private void handleBotAction(int componentId) {
        if (selectedBot == null) {
            renderMain();
            return;
        }
        switch (componentId) {
            case OPTION_1:
                if (selectedWasOnline) {
                    // Despawn just this one - no API for single despawn yet, so use deleteBot? No - that deletes.
                    // For now, despawn-all and re-spawn-others is too heavy. Phase 3b will add per-bot.
                    lastAction = "Single despawn not yet supported - use Despawn All";
                } else {
                    // Single-bot spawn - reach into BotPool via spawn(1) which picks random.
                    // Phase 3b will add spawnByName(). For now: spawn 1.
                    lastAction = "Spawned " + BotPool.spawn(1) + " (random pick)";
                }
                selectedBot = null;
                renderMain();
                break;
            case OPTION_2:
                boolean ok = BotPool.deleteBot(selectedBot);
                lastAction = ok ? ("Deleted " + selectedBot) : ("Delete failed for " + selectedBot);
                selectedBot = null;
                renderMain();
                break;
            case OPTION_3:
            default:
                selectedBot = null;
                renderMain();
                break;
        }
    }
}
