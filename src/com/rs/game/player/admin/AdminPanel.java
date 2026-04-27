package com.rs.game.player.admin;

import com.rs.GameLauncher;
import com.rs.bot.BotPool;
import com.rs.game.World;
import com.rs.game.player.Player;
import com.rs.utils.SerializableFilesManager;
import com.rs.utils.ShopsHandler;

import java.util.ArrayList;
import java.util.List;

public final class AdminPanel {

    private AdminPanel() {}

    public static final int IFACE = 1164;

    // Text slots
    private static final int T_TITLE  = 52;
    private static final int T_STATUS = 1;
    private static final int T_BTN1   = 32;
    private static final int T_BTN2   = 73;
    private static final int T_BTN3   = 68;
    private static final int T_BTN5   = 26;
    private static final int T_BTN6   = 24;

    // Click componentIds
    private static final int CLICK_BTN1 = 0;
    private static final int CLICK_BTN2 = 57;
    private static final int CLICK_BTN3 = 58;
    private static final int CLICK_BTN4 = 56;
    private static final int CLICK_BTN5 = 5;
    private static final int CLICK_BTN6 = 6;

    private static final String VIEW_KEY = "admin_panel_view";
    private static final String VIEW_ROOT          = "ROOT";
    private static final String VIEW_MORE          = "MORE";
    private static final String VIEW_BOTS          = "BOTS";
    private static final String VIEW_BOT_DETAIL    = "BOT_DETAIL";
    private static final String VIEW_SERVER        = "SERVER";
    private static final String VIEW_BACKUPS       = "BACKUPS";
    private static final String VIEW_PLAYERS       = "PLAYERS";
    private static final String VIEW_PLAYER_DETAIL = "PLAYER_DETAIL";

    // Admin input action codes (used with admin_input_action attr)
    private static final String IN_BROADCAST       = "broadcast";
    private static final String IN_SEARCH_PLAYER   = "search_player";
    private static final String IN_SPAWN_ITEM_ID   = "spawn_item_id";
    private static final String IN_SPAWN_ITEM_AMT  = "spawn_item_amt";
    private static final String IN_SET_STAT_ID     = "set_stat_id";
    private static final String IN_SET_STAT_LVL    = "set_stat_lvl";

    // ===== Open / view switch =====

    public static void open(Player player) {
        if (player.getRights() < 2) return;
        try {
            player.getInterfaceManager().sendCentralInterface(IFACE);
            setView(player, VIEW_ROOT);
        } catch (Throwable t) {
            player.getPackets().sendGameMessage("AdminPanel open failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void setView(Player player, String view) {
        player.getTemporaryAttributtes().put(VIEW_KEY, view);
        try { player.getInterfaceManager().sendCentralInterface(IFACE); } catch (Throwable ignored) {}

        if (VIEW_ROOT.equals(view))               renderRoot(player);
        else if (VIEW_MORE.equals(view))          renderMore(player);
        else if (VIEW_BOTS.equals(view))          renderBots(player);
        else if (VIEW_BOT_DETAIL.equals(view))    renderBotDetail(player);
        else if (VIEW_SERVER.equals(view))        renderServer(player);
        else if (VIEW_BACKUPS.equals(view))       renderBackups(player);
        else if (VIEW_PLAYERS.equals(view))       renderPlayers(player);
        else if (VIEW_PLAYER_DETAIL.equals(view)) renderPlayerDetail(player);
    }

    // ===== Render methods =====

    private static void renderRoot(Player player) {
        write(player, T_TITLE,  "<col=ffaa00>ADMIN PANEL</col>");
        write(player, T_STATUS, "Bots: " + BotPool.getOnlineCount() + " online, " + BotPool.getOfflineCount() + " offline");
        write(player, T_BTN1,   "Bot Management");
        write(player, T_BTN2,   "Player List");
        write(player, T_BTN3,   "Server Control");
        write(player, T_BTN5,   " ");
        write(player, T_BTN6,   "<col=cccccc>More Options</col>");
    }

    private static void renderMore(Player player) {
        write(player, T_TITLE,  "<col=ffaa00>MORE OPTIONS</col>");
        write(player, T_STATUS, "Additional admin tools");
        write(player, T_BTN1,   " ");
        write(player, T_BTN2,   " ");
        write(player, T_BTN3,   " ");
        write(player, T_BTN5,   " ");
        write(player, T_BTN6,   "<col=cccccc>Back to Main</col>");
    }

    private static void renderBots(Player player) {
        write(player, T_TITLE,  "<col=ffaa00>BOT MANAGEMENT</col>");
        write(player, T_STATUS, "Online: " + BotPool.getOnlineCount() + " | Offline: " + BotPool.getOfflineCount());
        write(player, T_BTN1,   "Generate 10 New");
        write(player, T_BTN2,   "Generate 50 New");
        write(player, T_BTN3,   "Spawn 5 from Pool");
        // slot 4 ("Cancel" hardcoded label) - Despawn All Online
        write(player, T_BTN5,   "View Bot Pool");
        write(player, T_BTN6,   "<col=cccccc>Back to Main</col>");
    }

    private static void renderBotDetail(Player player) {
        Object botName = player.getTemporaryAttributtes().get("admin_target_bot");
        String name = botName == null ? "(none)" : botName.toString();
        boolean online = BotPool.isOnline(name);
        write(player, T_TITLE,  "<col=ffaa00>BOT: " + name + "</col>");
        write(player, T_STATUS, online ? "Status: Online" : "Status: Offline");
        write(player, T_BTN1,   online ? "Despawn" : "Spawn");
        write(player, T_BTN2,   "Teleport to Bot");
        write(player, T_BTN3,   "Teleport Bot Here");
        // slot 4 - Delete Bot (DESTRUCTIVE)
        write(player, T_BTN5,   " ");
        write(player, T_BTN6,   "<col=cccccc>Back to Bot List</col>");
    }

    private static void renderServer(Player player) {
        write(player, T_TITLE,  "<col=ffaa00>SERVER CONTROL</col>");
        write(player, T_STATUS, "Server administration");
        write(player, T_BTN1,   "Backups");
        write(player, T_BTN2,   "Save All Now");
        write(player, T_BTN3,   "Restart Server (60s)");
        // slot 4 - Reload Shops
        write(player, T_BTN5,   "Broadcast Message");
        write(player, T_BTN6,   "<col=cccccc>Back to Main</col>");
    }

    private static void renderBackups(Player player) {
        write(player, T_TITLE,  "<col=ffaa00>BACKUPS</col>");
        write(player, T_STATUS, "Snapshot management");
        write(player, T_BTN1,   "Take Snapshot Now");
        write(player, T_BTN2,   "List Snapshots");
        write(player, T_BTN3,   "Toggle Scheduler");
        // slot 4 - Cleanup Old (placeholder)
        write(player, T_BTN5,   " ");
        write(player, T_BTN6,   "<col=cccccc>Back to Server Control</col>");
    }

    private static void renderPlayers(Player player) {
        int online = World.getPlayers().size();
        int realPlayers = 0;
        for (Player p : World.getPlayers()) if (p != null && !p.isHeadless()) realPlayers++;
        write(player, T_TITLE,  "<col=ffaa00>PLAYER LIST</col>");
        write(player, T_STATUS, realPlayers + " real players online (" + online + " total incl. bots)");
        write(player, T_BTN1,   "Apply to Self");
        write(player, T_BTN2,   "List Online Players");
        write(player, T_BTN3,   "Search Player...");
        // slot 4 - Broadcast to Players
        write(player, T_BTN5,   " ");
        write(player, T_BTN6,   "<col=cccccc>Back to Main</col>");
    }

    private static void renderPlayerDetail(Player player) {
        Object targetObj = player.getTemporaryAttributtes().get("admin_target_player");
        String targetName = targetObj == null ? "(none)" : targetObj.toString();
        Player target = World.getPlayerByDisplayName(targetName);
        if (target == null) target = player;

        write(player, T_TITLE,  "<col=ffaa00>PLAYER: " + target.getDisplayName() + "</col>");
        write(player, T_STATUS, "Combat lvl " + target.getSkills().getCombatLevel() + " | Rights " + target.getRights());
        write(player, T_BTN1,   "Teleport to Player");
        write(player, T_BTN2,   "Teleport Player Here");
        write(player, T_BTN3,   "Spawn Item to Inv");
        // slot 4 - Set Stat
        write(player, T_BTN5,   "Heal Player");
        write(player, T_BTN6,   "<col=cccccc>Back to Player List</col>");
    }

    // ===== Click handling =====

    public static boolean handleClick(Player player, int componentId) {
        if (player.getRights() < 2) return false;
        Object viewObj = player.getTemporaryAttributtes().get(VIEW_KEY);
        String view = viewObj == null ? VIEW_ROOT : viewObj.toString();

        if (VIEW_ROOT.equals(view)) {
            switch (componentId) {
                case CLICK_BTN1: setView(player, VIEW_BOTS); return true;
                case CLICK_BTN2: setView(player, VIEW_PLAYERS); return true;
                case CLICK_BTN3: setView(player, VIEW_SERVER); return true;
                case CLICK_BTN4: doReload(player); return true; // Reload Shops (slot 4 = "Cancel" label)
                case CLICK_BTN5: return true;
                case CLICK_BTN6: setView(player, VIEW_MORE); return true;
            }
        } else if (VIEW_MORE.equals(view)) {
            switch (componentId) {
                case CLICK_BTN1: case CLICK_BTN2: case CLICK_BTN3:
                case CLICK_BTN4: case CLICK_BTN5: return true;
                case CLICK_BTN6: setView(player, VIEW_ROOT); return true;
            }
        } else if (VIEW_BOTS.equals(view)) {
            switch (componentId) {
                case CLICK_BTN1: { int n = BotPool.generate(10); msg(player, "Generated " + n + " new bots."); renderBots(player); return true; }
                case CLICK_BTN2: { int n = BotPool.generate(50); msg(player, "Generated " + n + " new bots."); renderBots(player); return true; }
                case CLICK_BTN3: { int n = BotPool.spawn(5);     msg(player, "Spawned " + n + " bots.");      renderBots(player); return true; }
                case CLICK_BTN4: { int n = BotPool.despawnAll(); msg(player, "Despawned " + n + " bots.");   renderBots(player); return true; }
                case CLICK_BTN5: openBotPoolList(player); return true;
                case CLICK_BTN6: setView(player, VIEW_ROOT); return true;
            }
        } else if (VIEW_BOT_DETAIL.equals(view)) {
            switch (componentId) {
                case CLICK_BTN1: toggleBotSpawn(player); return true;
                case CLICK_BTN2: teleportToBot(player); return true;
                case CLICK_BTN3: teleportBotHere(player); return true;
                case CLICK_BTN4: deleteBot(player); return true;
                case CLICK_BTN5: return true;
                case CLICK_BTN6: openBotPoolList(player); return true;
            }
        } else if (VIEW_SERVER.equals(view)) {
            switch (componentId) {
                case CLICK_BTN1: setView(player, VIEW_BACKUPS); return true;
                case CLICK_BTN2: doSaveAll(player); return true;
                case CLICK_BTN3: doRestart(player); return true;
                case CLICK_BTN4: doReload(player); return true;
                case CLICK_BTN5: promptBroadcast(player); return true;
                case CLICK_BTN6: setView(player, VIEW_ROOT); return true;
            }
        } else if (VIEW_BACKUPS.equals(view)) {
            switch (componentId) {
                case CLICK_BTN1: doTakeSnapshot(player); return true;
                case CLICK_BTN2: openSnapshotsList(player); return true;
                case CLICK_BTN3: doToggleScheduler(player); return true;
                case CLICK_BTN4: msg(player, "Cleanup Old: not yet implemented"); return true;
                case CLICK_BTN5: return true;
                case CLICK_BTN6: setView(player, VIEW_SERVER); return true;
            }
        } else if (VIEW_PLAYERS.equals(view)) {
            switch (componentId) {
                case CLICK_BTN1: applyToSelf(player); return true;
                case CLICK_BTN2: openOnlinePlayersList(player); return true;
                case CLICK_BTN3: promptSearchPlayer(player); return true;
                case CLICK_BTN4: promptBroadcast(player); return true;
                case CLICK_BTN5: return true;
                case CLICK_BTN6: setView(player, VIEW_ROOT); return true;
            }
        } else if (VIEW_PLAYER_DETAIL.equals(view)) {
            switch (componentId) {
                case CLICK_BTN1: teleportToPlayer(player); return true;
                case CLICK_BTN2: teleportPlayerHere(player); return true;
                case CLICK_BTN3: promptSpawnItem(player); return true;
                case CLICK_BTN4: promptSetStat(player); return true;
                case CLICK_BTN5: healTarget(player); return true;
                case CLICK_BTN6: setView(player, VIEW_PLAYERS); return true;
            }
        }
        return false;
    }

    // ===== Input handling =====

    public static boolean handleNameInput(Player player, String value) {
        Object actionObj = player.getTemporaryAttributtes().get("admin_input_action");
        if (actionObj == null) return false;
        String action = actionObj.toString();
        if (IN_SEARCH_PLAYER.equals(action)) {
            player.getTemporaryAttributtes().remove("admin_input_action");
            doSearchPlayer(player, value);
            return true;
        }
        return false;
    }

    public static boolean handleLongTextInput(Player player, String value) {
        Object actionObj = player.getTemporaryAttributtes().get("admin_input_action");
        if (actionObj == null) return false;
        String action = actionObj.toString();
        if (IN_BROADCAST.equals(action)) {
            player.getTemporaryAttributtes().remove("admin_input_action");
            doBroadcast(player, value);
            return true;
        }
        if ("spawn_item_one_shot".equals(action)) {
            player.getTemporaryAttributtes().remove("admin_input_action");
            String[] parts = value.trim().split("\\s+");
            try {
                int itemId = Integer.parseInt(parts[0]);
                int amount = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
                doSpawnItem(player, itemId, amount);
            } catch (NumberFormatException nfe) {
                msg(player, "Bad input. Use: <itemId> <amount>");
            }
            return true;
        }
        return false;
    }

    public static boolean handleIntegerInput(Player player, int value) {
        Object actionObj = player.getTemporaryAttributtes().get("admin_input_action");
        if (actionObj == null) return false;
        String action = actionObj.toString();
        // (item spawn handled via long-text input one-shot path)
        if (IN_SET_STAT_ID.equals(action)) {
            player.getTemporaryAttributtes().put("admin_set_stat_id", Integer.valueOf(value));
            schedulePrompt(player, IN_SET_STAT_LVL, "Enter target level (1-99):");
            return true;
        }
        if (IN_SET_STAT_LVL.equals(action)) {
            player.getTemporaryAttributtes().remove("admin_input_action");
            Integer skill = (Integer) player.getTemporaryAttributtes().remove("admin_set_stat_id");
            if (skill != null) doSetStat(player, skill.intValue(), value);
            return true;
        }
        return false;
    }

    // ===== Action implementations =====

    private static void doSaveAll(Player player) {
        try {
            int bots = BotPool.saveOnline();
            SerializableFilesManager.flush();
            msg(player, "Saved " + bots + " bots and player files to disk.");
            World.sendWorldMessage("<col=ffaa00>[Server] Save initiated by admin.</col>", false);
        } catch (Throwable t) {
            msg(player, "Save failed: " + t.getMessage());
            t.printStackTrace();
        }
        renderServer(player);
    }

    private static void doRestart(Player player) {
        try {
            boolean started = GameLauncher.initDelayedShutdown(60);
            if (started) {
                msg(player, "Restart initiated. Server shutting down in 60 seconds.");
                World.sendWorldMessage("<col=ff0000>[Server] Restart in 60 seconds. Please log out safely.</col>", false);
            } else {
                msg(player, "Restart already in progress.");
            }
        } catch (Throwable t) {
            msg(player, "Restart failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void doReload(Player player) {
        try {
            ShopsHandler.forceReload();
            msg(player, "Shops reloaded.");
            World.sendWorldMessage("<col=ffaa00>[Server] Shop definitions reloaded.</col>", false);
        } catch (Throwable t) {
            msg(player, "Reload failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void promptBroadcast(Player player) {
        player.getTemporaryAttributtes().put("admin_input_action", IN_BROADCAST);
        player.getPackets().sendInputLongTextScript("Enter broadcast message:");
    }

    private static void doBroadcast(Player player, String message) {
        if (message == null || message.trim().isEmpty()) return;
        World.sendWorldMessage("<col=ffaa00>[Broadcast] " + message + "</col>", false);
        msg(player, "Broadcast sent.");
    }

    private static void doTakeSnapshot(Player player) {
        try {
            new ProcessBuilder("/home/brad/matrix/backup-scheduler.sh", "do_backup")
                .redirectErrorStream(true).start();
            msg(player, "Snapshot triggered. Check /home/brad/backups for output.");
        } catch (Throwable t) {
            msg(player, "Snapshot failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void doToggleScheduler(Player player) {
        try {
            // Use the server-control.sh menu option 11 (toggle scheduler) - approximate via the .sh
            new ProcessBuilder("/home/brad/matrix/server-control.sh", "toggle_scheduler")
                .redirectErrorStream(true).start();
            msg(player, "Scheduler toggle requested.");
        } catch (Throwable t) {
            msg(player, "Scheduler toggle failed: " + t.getMessage());
        }
    }

    private static void promptSearchPlayer(Player player) {
        player.getTemporaryAttributtes().put("admin_input_action", IN_SEARCH_PLAYER);
        player.getPackets().sendInputNameScript("Enter player name (partial OK):");
    }

    private static void doSearchPlayer(final Player player, String query) {
        final String q = query.toLowerCase();
        List<String> matches = new ArrayList<String>();
        for (Player p : World.getPlayers()) {
            if (p == null || p.isHeadless()) continue;
            if (p.getDisplayName().toLowerCase().contains(q)) {
                matches.add(p.getDisplayName());
            }
        }
        if (matches.isEmpty()) {
            msg(player, "No players matched: " + query);
            setView(player, VIEW_PLAYERS);
            return;
        }
        if (matches.size() == 1) {
            player.getTemporaryAttributtes().put("admin_target_player", matches.get(0));
            setView(player, VIEW_PLAYER_DETAIL);
            return;
        }
        ListPanel.show(player, "Search: " + query + " (" + matches.size() + " results)", matches, new ListPanel.ListCallback() {
            @Override public void onRowClicked(Player viewer, String item) {
                viewer.getTemporaryAttributtes().put("admin_target_player", item);
                setView(viewer, VIEW_PLAYER_DETAIL);
            }
        });
    }

    private static void applyToSelf(Player player) {
        player.getTemporaryAttributtes().put("admin_target_player", player.getDisplayName());
        setView(player, VIEW_PLAYER_DETAIL);
    }

    private static void openOnlinePlayersList(final Player player) {
        List<String> names = new ArrayList<String>();
        for (Player p : World.getPlayers()) {
            if (p == null || p.isHeadless()) continue;
            names.add(p.getDisplayName());
        }
        if (names.isEmpty()) {
            msg(player, "No real players online.");
            return;
        }
        ListPanel.show(player, "Online Players", names, new ListPanel.ListCallback() {
            @Override public void onRowClicked(Player viewer, String item) {
                viewer.getTemporaryAttributtes().put("admin_target_player", item);
                setView(viewer, VIEW_PLAYER_DETAIL);
            }
        });
    }

    private static void openBotPoolList(final Player player) {
        List<String> names = BotPool.snapshotAllNames();
        if (names.isEmpty()) {
            msg(player, "Bot pool is empty. Generate some bots first.");
            return;
        }
        ListPanel.show(player, "Bot Pool (" + names.size() + ")", names, new ListPanel.ListCallback() {
            @Override public void onRowClicked(Player viewer, String item) {
                viewer.getTemporaryAttributtes().put("admin_target_bot", item);
                setView(viewer, VIEW_BOT_DETAIL);
            }
        });
    }

    private static void openSnapshotsList(final Player player) {
        List<String> snapshots = new ArrayList<String>();
        try {
            collectSnapshots(snapshots, new java.io.File("/home/brad/backups/regular"));
            collectSnapshots(snapshots, new java.io.File("/home/brad/backups/daily"));
        } catch (Throwable t) {
            msg(player, "Failed to read snapshot dir: " + t.getMessage());
            return;
        }
        if (snapshots.isEmpty()) {
            msg(player, "No snapshots found.");
            return;
        }
        ListPanel.show(player, "Snapshots (" + snapshots.size() + ")", snapshots, new ListPanel.ListCallback() {
            @Override public void onRowClicked(Player viewer, String item) {
                msg(viewer, "Selected: " + item + " (restore not yet implemented - use shell)");
                setView(viewer, VIEW_BACKUPS);
            }
        });
    }

    private static void collectSnapshots(List<String> out, java.io.File dir) {
        if (dir == null || !dir.isDirectory()) return;
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files, new java.util.Comparator<java.io.File>() {
            @Override public int compare(java.io.File a, java.io.File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        for (java.io.File f : files) {
            if (f.isFile()) out.add(dir.getName() + "/" + f.getName());
        }
    }

    // ===== Player Detail actions =====

    private static Player getTargetPlayer(Player viewer) {
        Object o = viewer.getTemporaryAttributtes().get("admin_target_player");
        if (o == null) return null;
        return World.getPlayerByDisplayName(o.toString());
    }

    private static void teleportToPlayer(Player player) {
        Player t = getTargetPlayer(player);
        if (t == null) { msg(player, "Target offline."); return; }
        player.setNextWorldTile(new com.rs.game.WorldTile(t.getX(), t.getY(), t.getPlane()));
        msg(player, "Teleported to " + t.getDisplayName());
    }

    private static void teleportPlayerHere(Player player) {
        Player t = getTargetPlayer(player);
        if (t == null) { msg(player, "Target offline."); return; }
        if (t == player) { msg(player, "Cannot teleport yourself to yourself."); return; }
        t.setNextWorldTile(new com.rs.game.WorldTile(player.getX(), player.getY(), player.getPlane()));
        t.getPackets().sendGameMessage("You have been teleported by an admin.");
        msg(player, "Teleported " + t.getDisplayName() + " to your location.");
    }

    private static void healTarget(Player player) {
        Player t = getTargetPlayer(player);
        if (t == null) { msg(player, "Target offline."); return; }
        t.heal(t.getMaxHitpoints());
        msg(player, "Healed " + t.getDisplayName());
        renderPlayerDetail(player);
    }

    private static void promptSpawnItem(Player player) {
        player.getTemporaryAttributtes().put("admin_input_action", "spawn_item_one_shot");
        player.getPackets().sendInputLongTextScript("Enter: <itemId> <amount>");
    }

    private static void doSpawnItem(Player player, int itemId, int amount) {
        Player t = getTargetPlayer(player);
        if (t == null) { msg(player, "Target offline."); return; }
        if (itemId < 0 || amount <= 0) { msg(player, "Invalid item ID or amount."); return; }
        try {
            t.getInventory().addItem(itemId, amount);
            msg(player, "Spawned " + amount + "x item " + itemId + " to " + t.getDisplayName());
            if (t != player) t.getPackets().sendGameMessage("An admin gave you " + amount + "x item " + itemId);
        } catch (Throwable th) {
            msg(player, "Spawn failed: " + th.getMessage());
        }
    }

    private static void promptSetStat(Player player) {
        player.getTemporaryAttributtes().put("admin_input_action", IN_SET_STAT_ID);
        player.getPackets().sendInputIntegerScript("Enter skill ID (0=attack, 1=defence, 2=strength, 3=hp, 4=ranged, 5=prayer, 6=magic, 7=cooking, 8=woodcutting, 9=fletching, 10=fishing, 11=firemaking, 12=crafting, 13=smithing, 14=mining, 15=herblore, 16=agility, 17=thieving, 18=slayer, 19=farming, 20=runecraft, 21=hunter, 22=construction, 23=summoning, 24=dungeoneering, 25=divination):");
    }

    private static void doSetStat(Player player, int skillId, int level) {
        Player t = getTargetPlayer(player);
        if (t == null) { msg(player, "Target offline."); return; }
        if (skillId < 0 || skillId > 25) { msg(player, "Invalid skill ID."); return; }
        if (level < 1 || level > 99) { msg(player, "Level must be 1-99."); return; }
        try {
            t.getSkills().set(skillId, level);
            t.getSkills().setXp(skillId, com.rs.game.player.Skills.getXPForLevel(level));
            msg(player, "Set skill " + skillId + " of " + t.getDisplayName() + " to " + level);
        } catch (Throwable th) {
            msg(player, "Set stat failed: " + th.getMessage());
        }
    }

    // ===== Bot Detail actions =====

    private static String getTargetBot(Player viewer) {
        Object o = viewer.getTemporaryAttributtes().get("admin_target_bot");
        return o == null ? null : o.toString();
    }

    private static void toggleBotSpawn(Player player) {
        String name = getTargetBot(player);
        if (name == null) return;
        if (BotPool.isOnline(name)) {
            if (BotPool.despawnByName(name)) msg(player, "Despawned " + name);
            else msg(player, "Despawn failed for " + name);
        } else {
            if (BotPool.spawnByName(name)) msg(player, "Spawned " + name);
            else msg(player, "Spawn failed for " + name);
        }
        renderBotDetail(player);
    }

    private static void teleportToBot(Player player) {
        String name = getTargetBot(player);
        if (name == null) return;
        if (!BotPool.isOnline(name)) { msg(player, "Bot is offline. Spawn it first."); return; }
        Player bot = findHeadlessByName(name);
        if (bot != null) {
            player.setNextWorldTile(new com.rs.game.WorldTile(bot.getX(), bot.getY(), bot.getPlane()));
            msg(player, "Teleported to " + name);
            return;
        }
        diagnoseBotsInWorld(player, name);
    }

    private static void teleportBotHere(Player player) {
        String name = getTargetBot(player);
        if (name == null) return;
        if (!BotPool.isOnline(name)) { msg(player, "Bot is offline. Spawn it first."); return; }
        Player bot = findHeadlessByName(name);
        if (bot != null) {
            bot.setNextWorldTile(new com.rs.game.WorldTile(player.getX(), player.getY(), player.getPlane()));
            msg(player, "Teleported " + name + " to your location.");
            return;
        }
        diagnoseBotsInWorld(player, name);
    }

    private static void deleteBot(Player player) {
        String name = getTargetBot(player);
        if (name == null) return;
        if (BotPool.deleteBot(name)) {
            msg(player, "Deleted bot " + name);
            player.getTemporaryAttributtes().remove("admin_target_bot");
            openBotPoolList(player);
        } else {
            msg(player, "Delete failed for " + name);
        }
    }

    // ===== Helpers =====

    private static void schedulePrompt(final Player player, final String action, final String prompt) {
        com.rs.executor.GameExecutorManager.slowExecutor.schedule(new Runnable() {
            @Override public void run() {
                try {
                    player.getTemporaryAttributtes().put("admin_input_action", action);
                    player.getPackets().sendInputIntegerScript(prompt);
                } catch (Throwable t) { t.printStackTrace(); }
            }
        }, 600, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private static Player findHeadlessByName(String name) {
        if (name == null) return null;
        for (Player p : World.getPlayers()) {
            if (p == null || !p.isHeadless()) continue;
            String dn = p.getDisplayName();
            String un = p.getUsername();
            if (dn != null && (name.equals(dn) || name.equalsIgnoreCase(dn))) return p;
            if (un != null && (name.equals(un) || name.equalsIgnoreCase(un))) return p;
        }
        return null;
    }

    private static void diagnoseBotsInWorld(Player viewer, String wantedName) {
        int total = 0;
        StringBuilder sb = new StringBuilder("[diag] Looking for '").append(wantedName).append("'. Headless in world: ");
        for (Player p : World.getPlayers()) {
            if (p == null || !p.isHeadless()) continue;
            total++;
            sb.append("[dn=").append(p.getDisplayName()).append(" un=").append(p.getUsername()).append("] ");
            if (total >= 5) { sb.append("..."); break; }
        }
        if (total == 0) sb.append("(none)");
        msg(viewer, sb.toString());
    }

    private static void write(Player player, int comp, String text) {
        try { player.getPackets().sendIComponentText(IFACE, comp, text); } catch (Throwable ignored) {}
    }

    private static void msg(Player player, String text) {
        try { player.getPackets().sendGameMessage(text); } catch (Throwable ignored) {}
    }
}
