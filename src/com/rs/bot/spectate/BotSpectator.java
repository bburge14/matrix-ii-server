package com.rs.bot.spectate;

import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

/**
 * Lets an admin "ride along" with another player or bot. Each game tick we
 * teleport the spectator to the target's tile, then let the client's NATIVE
 * RuneScape camera take over. The admin can middle-mouse-drag, scroll-zoom,
 * arrow-key etc. exactly like normal - we don't override the camera at all.
 *
 * Trade-off: because the spectator's character is at the bot's tile, the
 * RS client always renders the local self there. setHidden(true) suppresses
 * rendering for OTHER clients (so other players see the admin go invisible)
 * but the local client always draws itself. That's a hardcoded client
 * behavior we can't bypass without a client patch.
 *
 * Bonus: with the spectator physically inside whatever building the bot is
 * in, the client's auto-roof-remove triggers naturally so indoor scenes
 * are visible.
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
        // Reset any leftover custom camera so the native one takes over.
        try { spectator.getPackets().sendResetCamera(); } catch (Throwable ignore) {}
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
     * Toggle the client's "Remove roofs" graphic option. Tries varbit 4084
     * (the canonical 718 ID); a no-op if this server's cache uses a
     * different ID. Public so the ::roofs command can call it directly.
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
                        try { p.getAppearence().setHidden(false); } catch (Throwable ignore) {}
                        continue;
                    }
                    if (target == p) continue; // can't spectate self
                    if (!samePos(p, target)) {
                        // Snap to the target's tile. Native camera follows
                        // the player, so by being on the bot's tile the
                        // admin's normal camera ends up centered on the bot.
                        // No camera overrides - admin can drag it freely.
                        p.setNextWorldTile(new WorldTile(target.getX(), target.getY(), target.getPlane()));
                    }
                }
            }
        }, 0, 0); // run every tick forever
    }

    private static boolean samePos(Player a, Player b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getPlane() == b.getPlane();
    }
}
