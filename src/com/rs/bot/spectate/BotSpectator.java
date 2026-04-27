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
     * Elevated third-person follow-cam. Sits 6 tiles behind the bot in
     * the direction they're facing, raised to z=1300 so we clear wall
     * tops (typically ~600-1000z) and avoid clipping into the bot's
     * model. Looks at the bot's tile near ground level - bot is centered
     * in foreground, world visible around them.
     *
     * As the bot turns, the camera orbits with them so the bot is
     * always shown from "behind" relative to their facing direction.
     */
    private static void applySpectateCamera(Player spectator, Player target) {
        try {
            // Convert the bot's 14-bit direction angle back into a unit
            // vector. direction = atan2(-dx, -dy) * 2607.59 so reverse it:
            double angle = target.getDirection() / 2607.5945876176133;
            double dx = -Math.sin(angle);
            double dy = -Math.cos(angle);

            // Camera position: 6 tiles behind the bot, elevated. Far
            // enough back to keep the bot in shot, high enough to clear
            // walls and the bot's body.
            int posWorldX = (int) Math.round(target.getX() - dx * 6);
            int posWorldY = (int) Math.round(target.getY() - dy * 6);
            int posLocalX = new WorldTile(posWorldX, posWorldY, target.getPlane()).getXInScene(spectator);
            int posLocalY = new WorldTile(posWorldX, posWorldY, target.getPlane()).getYInScene(spectator);
            spectator.getPackets().sendCameraPos(posLocalX, posLocalY, 1300);

            // Look target: bot's tile, ground level.
            int lookLocalX = new WorldTile(target.getX(), target.getY(), target.getPlane()).getXInScene(spectator);
            int lookLocalY = new WorldTile(target.getX(), target.getY(), target.getPlane()).getYInScene(spectator);
            spectator.getPackets().sendCameraLook(lookLocalX, lookLocalY, 200);
        } catch (Throwable t) {
            // Camera packets are best-effort; spectate still works without them
        }
    }

    private static boolean samePos(Player a, Player b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getPlane() == b.getPlane();
    }
}
