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
        setRoofsHidden(spectator, true);
        // Park spectator 20 tiles SW of the bot so their character model
        // ends up behind/below the spectate camera. The camera packets
        // (sent every tick) keep the lens focused on the bot from a
        // closer SW-elevated angle, so the spectator's local self-render
        // is off-screen.
        spectator.setNextWorldTile(parkTile(target));
        applySpectateCamera(spectator, target);
        return true;
    }

    private static WorldTile parkTile(Player target) {
        return new WorldTile(target.getX() - 20, target.getY() - 20, target.getPlane());
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
                    WorldTile park = parkTile(target);
                    if (p.getX() != park.getX() || p.getY() != park.getY() || p.getPlane() != park.getPlane()) {
                        p.setNextWorldTile(park);
                    }
                    applySpectateCamera(p, target);
                }
            }
        }, 0, 0); // run every tick forever
    }

    /**
     * Camera packets that focus the lens on the target while the
     * spectator sits 20 tiles SW. Camera position is just 5 tiles SW of
     * the target (close in), elevated to z=1200 so the lens clears
     * walls and looks down-northeast onto the bot. The spectator's local
     * character (parked 20 SW of bot, i.e. 15 tiles further SW than the
     * camera position) is therefore behind and below the camera frame -
     * effectively invisible from this angle.
     */
    private static void applySpectateCamera(Player spectator, Player target) {
        try {
            int posWorldX = target.getX() - 5;
            int posWorldY = target.getY() - 5;
            int posLocalX = new WorldTile(posWorldX, posWorldY, target.getPlane()).getXInScene(spectator);
            int posLocalY = new WorldTile(posWorldX, posWorldY, target.getPlane()).getYInScene(spectator);
            spectator.getPackets().sendCameraPos(posLocalX, posLocalY, 1200);

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
