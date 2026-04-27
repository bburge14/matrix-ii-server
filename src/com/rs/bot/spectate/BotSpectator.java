package com.rs.bot.spectate;

import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

/**
 * Lets an admin "ride along" with another player or bot. While spectating,
 * the spectator gets teleported to their target's tile every game tick so
 * they're always co-located. The spectator's character is still visible, so
 * this is more "follow-cam" than true cinematic spectate, but it's the
 * simplest reliable way to watch a moving bot from anywhere on the map.
 *
 * Toggle via temporary attributes on the spectator's Player object - we
 * store the target's display name there. Call start(spectator, targetName)
 * to begin and stop(spectator) to release.
 *
 * A single global per-tick task scans for spectators across all online
 * players, so this scales without per-player task registration.
 */
public final class BotSpectator {

    /** Key under which we stash the target's display name on the spectator. */
    public static final String ATTR_KEY = "BotSpectator:target";

    private static volatile boolean tickerStarted = false;

    private BotSpectator() {}

    /** Start spectating <targetName>. Replaces any previous target. */
    public static boolean start(Player spectator, String targetName) {
        Player target = findOnlinePlayer(targetName);
        if (target == null) return false;
        spectator.getTemporaryAttributtes().put(ATTR_KEY, target.getDisplayName());
        ensureTicker();
        try { spectator.getAppearence().setHidden(true); } catch (Throwable ignore) {}
        // Try the canonical 718 roof varbit anyway. Costs nothing if the
        // cache uses a different ID; helps if it ever does respect 4084.
        setRoofsHidden(spectator, true);
        spectator.setNextWorldTile(new WorldTile(target.getX(), target.getY(), target.getPlane()));
        return true;
    }

    /** Stop spectating. */
    public static void stop(Player spectator) {
        spectator.getTemporaryAttributtes().remove(ATTR_KEY);
        try { spectator.getAppearence().setHidden(false); } catch (Throwable ignore) {}
        try { spectator.getPackets().sendResetCamera(); } catch (Throwable ignore) {}
        setRoofsHidden(spectator, false);
    }

    /**
     * Toggle the client's "Remove roofs" graphic option. The 718 client
     * reads this from varbit 4084 (0 = show, 1 = hide). Public so the
     * ::roofs command can call it directly without going through spectate.
     */
    public static void setRoofsHidden(Player p, boolean hidden) {
        try {
            p.getVarsManager().sendVarBit(4084, hidden ? 1 : 0);
        } catch (Throwable ignore) {}
    }

    /** True if this player is currently spectating someone. */
    public static boolean isSpectating(Player spectator) {
        return spectator.getTemporaryAttributtes().get(ATTR_KEY) != null;
    }

    /** Get the name they're spectating, or null. */
    public static String getTargetName(Player spectator) {
        Object v = spectator.getTemporaryAttributtes().get(ATTR_KEY);
        return v == null ? null : v.toString();
    }

    private static Player findOnlinePlayer(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase().replace('_', ' ').trim();
        for (Player p : World.getPlayers()) {
            if (p == null || p.hasFinished()) continue;
            String dn = p.getDisplayName();
            if (dn == null) continue;
            if (dn.toLowerCase().equals(lower)) return p;
        }
        return null;
    }

    private static synchronized void ensureTicker() {
        if (tickerStarted) return;
        tickerStarted = true;
        WorldTasksManager.schedule(new WorldTask() {
            @Override
            public void run() {
                for (Player p : World.getPlayers()) {
                    if (p == null || p.hasFinished()) continue;
                    Object attr = p.getTemporaryAttributtes().get(ATTR_KEY);
                    if (attr == null) continue;
                    Player target = findOnlinePlayer(attr.toString());
                    if (target == null) {
                        // Target logged out / despawned; release the spectate.
                        p.getTemporaryAttributtes().remove(ATTR_KEY);
                        try { p.getPackets().sendResetCamera(); } catch (Throwable ignore) {}
                        try { p.getAppearence().setHidden(false); } catch (Throwable ignore) {}
                        continue;
                    }
                    if (target == p) continue; // can't spectate self
                    if (!samePos(p, target)) {
                        // Snap to the target's tile. Keeping the spectator
                        // physically AT the target means the client's natural
                        // auto-roof-remove triggers for whatever building
                        // the target is in.
                        p.setNextWorldTile(new WorldTile(target.getX(), target.getY(), target.getPlane()));
                    }
                    applySpectateCamera(p, target);
                }
            }
        }, 0, 0); // run every tick forever
    }

    /**
     * First-person follow-cam over the target's shoulder. Camera sits 1
     * tile behind the bot in the direction they're facing, slightly higher
     * than head height, looking at a point ~8 tiles ahead. As the bot
     * turns, the camera turns with them - feels like "watching through
     * the bot's eyes."
     *
     * Avoids the wall-clip problem because the camera is elevated above
     * wall geometry (z=500 is roughly head height + a bit). Auto-roof-
     * remove still triggers because the spectator stays on the bot's tile.
     */
    private static void applySpectateCamera(Player spectator, Player target) {
        try {
            // Convert the bot's 14-bit angle back into a unit (dx, dy).
            // direction = atan2(-dx, -dy) * 2607.59... so reverse it:
            double angle = target.getDirection() / 2607.5945876176133;
            double dx = -Math.sin(angle);
            double dy = -Math.cos(angle);

            // Camera position: 1 tile behind the bot in their facing
            // direction, ~head height + a bit so we can see over them.
            int posWorldX = (int) Math.round(target.getX() - dx);
            int posWorldY = (int) Math.round(target.getY() - dy);
            int posLocalX = new WorldTile(posWorldX, posWorldY, target.getPlane()).getXInScene(spectator);
            int posLocalY = new WorldTile(posWorldX, posWorldY, target.getPlane()).getYInScene(spectator);
            spectator.getPackets().sendCameraPos(posLocalX, posLocalY, 500);

            // Look target: 8 tiles ahead of the bot, at chest height.
            int lookWorldX = (int) Math.round(target.getX() + dx * 8);
            int lookWorldY = (int) Math.round(target.getY() + dy * 8);
            int lookLocalX = new WorldTile(lookWorldX, lookWorldY, target.getPlane()).getXInScene(spectator);
            int lookLocalY = new WorldTile(lookWorldX, lookWorldY, target.getPlane()).getYInScene(spectator);
            spectator.getPackets().sendCameraLook(lookLocalX, lookLocalY, 300);
        } catch (Throwable t) {
            // Camera packets are best-effort; spectate still works without them
        }
    }

    private static boolean samePos(Player a, Player b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getPlane() == b.getPlane();
    }
}
