package com.rs.game.player.admin;

import com.rs.game.player.Player;

public final class ListPanelProbe {

    private ListPanelProbe() {}

    public static void open(Player player) {
        open(player, 735, 1, 50);
    }

    public static void open(Player player, int iface) {
        open(player, iface, 1, 50);
    }

    public static void open(Player player, int iface, int start, int end) {
        openStep(player, iface, start, end, 1);
    }

    public static void openStep(Player player, int iface, int start, int end, int step) {
        try {
            player.getInterfaceManager().sendCentralInterface(iface);
            int cap = Math.min(end, start + 200);
            int written = 0;
            for (int c = start; c <= cap; c += step) {
                tryWrite(player, iface, c, "[" + iface + ":" + c + "]");
                written++;
            }
            player.getPackets().sendGameMessage("Probe: iface " + iface + " comps " + start + "-" + cap + " step " + step + " (" + written + " writes)");
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
