package com.rs.bot;

import com.rs.game.player.Player;

/**
 * Spawn/despawn orchestrator. Bots are auto-added to World by BotFactory
 * (via Player.init), so spawn just creates more bots and BotPool tracks them.
 */
public final class AIManager {

    private AIManager() {}

    /** Spawn `count` bots. The `nearby` arg is currently unused but kept for future placement logic. */
    public static synchronized int spawn(int count, Player nearby) {
        return BotPool.generate(count);
    }

    public static synchronized int despawnAll() {
        return BotPool.despawnAll();
    }

    public static int getOnlineCount() { return BotPool.getOnlineCount(); }
    public static int getTotalCount() { return BotPool.getTotalCount(); }
    public static int getOfflineCount() { return BotPool.getOfflineCount(); }
}
