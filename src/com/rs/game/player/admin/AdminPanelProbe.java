package com.rs.game.player.admin;

import com.rs.game.player.Player;

/**
 * Phase 3c admin panel - probe stage.
 *
 * Opens interface 1164 (Dominion Tower mode-select panel) and writes labeled
 * text into a NARROW range of components to discover slot layout safely.
 * Spamming all 1-150 crashes the client.
 *
 * Usage: ::adminprobe <startId> <endId>
 *   ::adminprobe          - defaults to range 20-35 (covers DT known good slots)
 *   ::adminprobe 1 15     - probe components 1-15
 *   ::adminprobe 50 65    - probe components 50-65
 */
public final class AdminPanelProbe {

    private AdminPanelProbe() {}

    private static final int IFACE = 1164;

    public static void open(Player player) {
        open(player, 20, 35);
    }

    public static void open(Player player, int start, int end) {
        try {
            player.getInterfaceManager().sendCentralInterface(IFACE);
            // Cap range so we never go too wide
            if (end - start > 20) end = start + 20;
            for (int c = start; c <= end; c++) {
                tryWrite(player, IFACE, c, "[" + c + "]");
            }
            player.getPackets().sendGameMessage("Admin probe: iface " + IFACE + " components " + start + "-" + end);
        } catch (Throwable t) {
            player.getPackets().sendGameMessage("Probe failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void tryWrite(Player player, int iface, int comp, String text) {
        try {
            player.getPackets().sendIComponentText(iface, comp, text);
        } catch (Throwable ignored) {}
    }
}
