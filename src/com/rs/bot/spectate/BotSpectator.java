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
        // Hide roofs so we can see indoor scenes through ceilings.
        setRoofsHidden(spectator, true);
        spectator.setNextWorldTile(new WorldTile(target.getX(), target.getY(), target.getPlane()));
        applySpectateCamera(spectator, target);
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
                        p.setNextWorldTile(new WorldTile(target.getX(), target.getY(), target.getPlane()));
                    }
                    applySpectateCamera(p, target);
                }
            }
        }, 0, 0); // run every tick forever
    }

    /**
     * Behind-and-above third-person camera over the target. Sits at ~900z
     * so we're under most roofs (which clip around 1100z), offset 6 tiles
     * SW so the angle reads as a normal RS isometric view rather than
     * a satellite map. Look target is on the bot's tile near ground level.
     */
    private static void applySpectateCamera(Player spectator, Player target) {
        try {
            // Camera position: 6 tiles SW of target, ~3 storeys up. The
            // diagonal offset gives a familiar isometric angle.
            int posX = target.getX() - 6;
            int posY = target.getY() - 6;
            int posLocalX = new WorldTile(posX, posY, target.getPlane()).getXInScene(spectator);
            int posLocalY = new WorldTile(posX, posY, target.getPlane()).getYInScene(spectator);
            spectator.getPackets().sendCameraPos(posLocalX, posLocalY, 900);
            // Look target: bot's tile, near ground.
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
