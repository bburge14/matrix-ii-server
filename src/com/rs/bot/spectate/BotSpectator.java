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
        // Hide the spectator's character model so OTHER clients don't draw
        // them. The client always renders self locally regardless, so we
        // also snap the camera up high looking down at the target - the
        // self-render ends up directly under the lens, mostly invisible.
        try {
            spectator.getAppearence().setHidden(true);
        } catch (Throwable t) {
            // best-effort; spectate still works without the hide
        }
        spectator.setNextWorldTile(new WorldTile(target.getX(), target.getY(), target.getPlane()));
        applySpectateCamera(spectator, target);
        return true;
    }

    /** Stop spectating. */
    public static void stop(Player spectator) {
        spectator.getTemporaryAttributtes().remove(ATTR_KEY);
        try {
            spectator.getAppearence().setHidden(false);
        } catch (Throwable t) {
            // ignore
        }
        try {
            spectator.getPackets().sendResetCamera();
        } catch (Throwable t) {
            // ignore
        }
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
                        p.setNextWorldTile(new WorldTile(target.getX(), target.getY(), target.getPlane()));
                    }
                    applySpectateCamera(p, target);
                }
            }
        }, 0, 0); // run every tick forever
    }

    /**
     * Park the spectator's camera high above the target, looking straight
     * down. With the spectator's own tile being the same as the target's,
     * the local self-render ends up tiny and centered with the target -
     * the human eye reads it as "I'm watching from above."
     */
    private static void applySpectateCamera(Player spectator, Player target) {
        try {
            int sceneX = new WorldTile(target.getX(), target.getY(), target.getPlane()).getXInScene(spectator);
            int sceneY = new WorldTile(target.getX(), target.getY(), target.getPlane()).getYInScene(spectator);
            // Camera position: directly above target, ~3500 z high.
            spectator.getPackets().sendCameraPos(sceneX, sceneY, 3500);
            // Look at target near ground level.
            spectator.getPackets().sendCameraLook(sceneX, sceneY, 200);
        } catch (Throwable t) {
            // Camera packets are best-effort; spectate still works without them
        }
    }

    private static boolean samePos(Player a, Player b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getPlane() == b.getPlane();
    }
}
