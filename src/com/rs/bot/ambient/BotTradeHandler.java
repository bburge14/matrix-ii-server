package com.rs.bot.ambient;

import com.rs.bot.AIPlayer;
import com.rs.game.World;
import com.rs.game.item.Item;
import com.rs.game.item.ItemsContainer;
import com.rs.game.player.Player;
import com.rs.game.player.Trade;
import com.rs.utils.Utils;

/**
 * Trade lifecycle for SOCIALITE bots.
 *
 * Two flows:
 *   - SOCIALITE_GAMBLER: 50/50 dice. Player offers GP -> bot rolls dice ->
 *     if win, bot adds 2x GP -> both confirm -> player either gets 2x or
 *     loses their stake.
 *   - SOCIALITE_GE_TRADER: bot stocks one item with a price. If player adds
 *     exactly the price in GP, bot confirms; otherwise declines.
 *
 * Tick-driven: every CitizenBrain tick calls handleBot() once. State machine
 * lives entirely on the bot's Trade and TemporaryAttributtes - we read player
 * offers, decide, write our own offer, accept stages.
 */
public final class BotTradeHandler {

    /** Coins item id - the only thing players bet/pay with for now. */
    private static final int COINS = 995;

    /** Min/max bet a gambler bot will accept. Caps blast radius if a player
     *  tries to dump 500M into a dice game and break our economy. */
    private static final int MIN_BET = 100;
    private static final int MAX_BET = 1_000_000;

    /** Gambling dice modes - per-bot, deterministic by display-name hash.
     *  RuneScape host convention: NxX means "roll, win if >= N for X payout".
     *  e.g. 55x2 = win at 55+, get 2x bet. Lower N = better odds for player. */
    private static final DiceMode[] DICE_MODES = new DiceMode[] {
        new DiceMode("55x2", 55, 2),
        new DiceMode("65x2", 65, 2),
        new DiceMode("75x2", 75, 2),
    };

    /** How often (ms) gambler/trader bots broadcast their service line when
     *  not actively in a trade. Lets nearby players see them as live hosts/
     *  vendors. 30s = avoids spam, still visible. */
    private static final long BROADCAST_INTERVAL_MS = 30_000;

    /** How many ticks the bot waits after entering trade before acting. Lets
     *  the player see the trade screen + add items / GP before bot responds. */
    private static final int RESPONSE_DELAY_TICKS = 4;

    private BotTradeHandler() {}

    public static void tick(AIPlayer bot, AmbientArchetype arch) {
        if (bot == null || arch == null) return;
        if (!arch.isSocialite()) return;
        // Only gambler + GE trader subtypes do trade. Bankstanders ignore.
        boolean isGambler = arch == AmbientArchetype.SOCIALITE_GAMBLER;
        boolean isTrader  = arch == AmbientArchetype.SOCIALITE_GE_TRADER;
        if (!isGambler && !isTrader) return;

        Trade trade = bot.getTrade();
        if (trade == null) return;

        if (!trade.isTrading()) {
            // Periodic service broadcast - so players walking by see what
            // the bot offers. Throttled by BROADCAST_INTERVAL_MS per-bot.
            maybeBroadcast(bot, isGambler, isTrader);
            // Look for a player who right-clicked us and chose Trade.
            Player requester = findInboundTradeRequest(bot);
            if (requester != null) {
                acceptInboundTrade(bot, requester);
            }
            return;
        }

        // We're in a trade. Pace ourselves so the player can finish adding
        // items before we react.
        Object started = bot.getTemporaryAttributtes().get("BotTradeStartMs");
        long now = System.currentTimeMillis();
        if (started == null) {
            bot.getTemporaryAttributtes().put("BotTradeStartMs", now);
            return;
        }
        // 4 ticks = ~2.4s. Lets the player add items + GP before bot reacts.
        if (now - ((Long) started) < RESPONSE_DELAY_TICKS * 600L) return;

        if (isGambler) handleGambler(bot, trade);
        else if (isTrader) handleTrader(bot, trade);
    }

    /** Scan nearby players for one whose TradeTarget == this bot. */
    private static Player findInboundTradeRequest(AIPlayer bot) {
        for (Player p : World.getPlayers()) {
            if (p == null || p.hasFinished()) continue;
            if (p == bot) continue;
            if (p.getPlane() != bot.getPlane()) continue;
            int dx = p.getX() - bot.getX();
            int dy = p.getY() - bot.getY();
            if (dx * dx + dy * dy > 25) continue; // ~5 tile range
            try {
                Object tgt = p.getTemporaryAttributtes().get("TradeTarget");
                if (tgt == bot) return p;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void acceptInboundTrade(AIPlayer bot, Player player) {
        try {
            // Mirror the player's TradeTarget so the trade decoder paths work.
            bot.getTemporaryAttributtes().put("TradeTarget", player);
            player.getTemporaryAttributtes().remove("TradeTarget");
            bot.getTrade().openTrade(player);
            player.getTrade().openTrade(bot);
            bot.getTemporaryAttributtes().remove("BotTradeStartMs");
            bot.getTemporaryAttributtes().remove("BotTradeDecided");
        } catch (Throwable t) {
            // Swallow - player retries on rejection.
        }
    }

    // === SOCIALITE_GAMBLER: 50/50 dice ===

    private static void handleGambler(AIPlayer bot, Trade trade) {
        Player target = trade.getTarget();
        if (target == null) return;

        // Read what the player offered.
        ItemsContainer<Item> offered = target.getTrade().getItemsContainer();
        if (offered == null) return;
        int playerGp = countItem(offered, COINS);

        // Stage 2 confirmation: if we already accepted stage 1, watch for the
        // trade to advance to confirmation (player accepted too). Then call
        // accept(false) to seal it.
        Boolean stage1Done = (Boolean) bot.getTemporaryAttributtes().get("BotTradeStage1");
        if (Boolean.TRUE.equals(stage1Done)) {
            try {
                // accept(false) only completes when both sides are at stage 2.
                // Calling it before then is a no-op; calling it after both
                // stage-1-accept seals the trade.
                bot.getTrade().accept(false);
            } catch (Throwable ignored) {}
            return;
        }

        Boolean decided = (Boolean) bot.getTemporaryAttributtes().get("BotTradeDecided");
        if (decided == null) decided = Boolean.FALSE;

        if (!decided) {
            if (playerGp < MIN_BET) {
                // Wait for them to add a real bet.
                return;
            }
            DiceMode mode = pickDiceModeForBot(bot);
            int bet = Math.min(playerGp, MAX_BET);
            long payout = (long) bet * mode.payoutMultiplier;
            int roll = Utils.random(100);
            boolean win = roll >= mode.winThreshold;
            try {
                if (win) {
                    // Move coins from MoneyPouch -> inventory so addItem
                    // works. accumulatedWealth puts cb-scaled GP in the
                    // pouch, not inventory, so without this gamblers
                    // always cancel "no funds".
                    ensureInvCoins(bot, payout);
                    if (bot.getInventory().getAmountOf(COINS) >= payout) {
                        bot.getTrade().addItem(new Item(COINS, (int) Math.min(Integer.MAX_VALUE, payout)));
                        sayBoth(bot, mode.name + " - rolled " + roll + " - you win " + payout + "!");
                    } else {
                        sayBoth(bot, "no funds, can't gamble that high");
                        bot.getTrade().cancelTrade();
                        return;
                    }
                } else {
                    sayBoth(bot, mode.name + " - rolled " + roll + " - you lose");
                }
            } catch (Throwable ignored) {}
            bot.getTemporaryAttributtes().put("BotTradeDecided", Boolean.TRUE);
            bot.getTemporaryAttributtes().put("BotTradeBet", bet);
            return;
        }

        // Accept stage 1 once decided. Trade is 2-stage: both sides accept
        // stage 1 (Trade interface) -> advances to stage 2 (Confirmation) ->
        // both sides accept stage 2 -> trade completes. We mark stage1Done
        // so next tick handles stage 2.
        try {
            if (!bot.getTrade().hasAccepted()) {
                bot.getTrade().accept(true);
                bot.getTemporaryAttributtes().put("BotTradeStage1", Boolean.TRUE);
            }
        } catch (Throwable ignored) {}
    }

    // === SOCIALITE_GE_TRADER: stock + price ===

    private static void handleTrader(AIPlayer bot, Trade trade) {
        Player target = trade.getTarget();
        if (target == null) return;

        // First time: pick what we're selling + price, add to our offer.
        StockEntry stock = (StockEntry) bot.getTemporaryAttributtes().get("BotTraderStock");
        if (stock == null) {
            stock = pickStockForBot(bot);
            if (stock == null) {
                // No stockable item - just close trade.
                bot.getTrade().cancelTrade();
                return;
            }
            try {
                if (bot.getInventory().getAmountOf(stock.itemId) < 1) {
                    // Self-stock: give bot 1 of the item to sell. (Trader
                    // bots get pre-stocked at spawn time eventually; for now
                    // we materialize on-demand.)
                    bot.getInventory().addItem(stock.itemId, 1);
                }
                bot.getTrade().addItem(new Item(stock.itemId, 1));
                sayBoth(bot, "selling " + stock.name + " for " + stock.priceGp + "gp");
            } catch (Throwable ignored) {}
            bot.getTemporaryAttributtes().put("BotTraderStock", stock);
            return;
        }

        // Check if player paid the right amount.
        ItemsContainer<Item> offered = target.getTrade().getItemsContainer();
        if (offered == null) return;
        int playerGp = countItem(offered, COINS);

        if (playerGp >= stock.priceGp) {
            try {
                if (!bot.getTrade().hasAccepted()) {
                    bot.getTrade().accept(true);
                    bot.getTemporaryAttributtes().put("BotTradeStage1", Boolean.TRUE);
                }
            } catch (Throwable ignored) {}
        }
    }

    /** Per-bot stock pick. Stable per bot: same bot keeps offering the same
     *  thing for the trade lifetime. Picks a low-tier item the bot can plausibly
     *  acquire. Real economy comes later. */
    private static StockEntry pickStockForBot(AIPlayer bot) {
        // Curated by category - skilling supplies, food, runes, gear, gems.
        // Each bot deterministically picks one by display-name hash so the
        // same bot consistently sells the same thing.
        int idx = Math.abs((bot.getDisplayName() == null ? 0 : bot.getDisplayName().hashCode())) % CATALOG.length;
        return CATALOG[idx];
    }

    /** Trader stock catalog - 30+ entries across categories so trader bots
     *  feel varied. Prices are rough; tune per server economy later. */
    private static final StockEntry[] CATALOG = new StockEntry[] {
        // Logs (Woodcutting drops + Fletching)
        new StockEntry(1511, 50,     "logs"),
        new StockEntry(1521, 200,    "oak logs"),
        new StockEntry(1519, 400,    "willow logs"),
        new StockEntry(1517, 600,    "maple logs"),
        new StockEntry(1515, 800,    "yew logs"),
        new StockEntry(1513, 1500,   "magic logs"),
        // Ores (Mining + Smelting)
        new StockEntry(436,  50,     "copper ore"),
        new StockEntry(438,  50,     "tin ore"),
        new StockEntry(440,  150,    "iron ore"),
        new StockEntry(453,  150,    "coal"),
        new StockEntry(447,  250,    "mithril ore"),
        new StockEntry(449,  500,    "adamantite ore"),
        new StockEntry(451,  10000,  "runite ore"),
        new StockEntry(444,  500,    "gold ore"),
        // Raw fish (Fishing + Cooking)
        new StockEntry(317,  100,    "raw shrimps"),
        new StockEntry(331,  300,    "raw salmon"),
        new StockEntry(335,  200,    "raw trout"),
        new StockEntry(341,  150,    "raw herring"),
        new StockEntry(353,  150,    "raw mackerel"),
        new StockEntry(371,  500,    "raw swordfish"),
        new StockEntry(377,  500,    "raw lobster"),
        new StockEntry(383,  900,    "raw shark"),
        // Cooked food
        new StockEntry(379,  600,    "lobster"),
        new StockEntry(385,  1200,   "shark"),
        // Runes
        new StockEntry(554,  20,     "fire runes"),
        new StockEntry(555,  20,     "water runes"),
        new StockEntry(556,  20,     "air runes"),
        new StockEntry(557,  20,     "earth runes"),
        new StockEntry(558,  10,     "mind runes"),
        new StockEntry(559,  100,    "body runes"),
        new StockEntry(560,  300,    "death runes"),
        new StockEntry(562,  150,    "chaos runes"),
        new StockEntry(565,  250,    "blood runes"),
        // Bones (Prayer)
        new StockEntry(526,  50,     "bones"),
        new StockEntry(532,  150,    "big bones"),
        new StockEntry(536,  3500,   "dragon bones"),
        // Herbs (Herblore)
        new StockEntry(199,  100,    "grimy guam"),
        new StockEntry(207,  4000,   "grimy ranarr"),
        new StockEntry(219,  9000,   "grimy torstol"),
        // Gems (Crafting)
        new StockEntry(1623, 400,    "uncut sapphire"),
        new StockEntry(1621, 500,    "uncut emerald"),
        new StockEntry(1619, 1000,   "uncut ruby"),
        new StockEntry(1617, 2500,   "uncut diamond"),
    };

    private static int countItem(ItemsContainer<Item> c, int itemId) {
        int total = 0;
        for (int i = 0; i < c.getSize(); i++) {
            Item it = c.get(i);
            if (it != null && it.getId() == itemId) total += it.getAmount();
        }
        return total;
    }

    public static final class StockEntry {
        public final int itemId, priceGp;
        public final String name;
        public StockEntry(int itemId, int priceGp, String name) {
            this.itemId = itemId; this.priceGp = priceGp; this.name = name;
        }
    }

    public static final class DiceMode {
        public final String name;
        public final int winThreshold;     // win if random(100) >= this
        public final int payoutMultiplier; // bet * this on win
        public DiceMode(String name, int winThreshold, int payoutMultiplier) {
            this.name = name;
            this.winThreshold = winThreshold;
            this.payoutMultiplier = payoutMultiplier;
        }
    }

    /** Per-bot deterministic mode pick so the same gambler always advertises
     *  the same game. Hash on display name. */
    private static DiceMode pickDiceModeForBot(AIPlayer bot) {
        int idx = Math.abs(bot.getDisplayName() == null ? 0
            : bot.getDisplayName().hashCode()) % DICE_MODES.length;
        return DICE_MODES[idx];
    }

    /** Periodic chatter so players see what the bot offers without trading. */
    private static void maybeBroadcast(AIPlayer bot, boolean isGambler, boolean isTrader) {
        Object lastMs = bot.getTemporaryAttributtes().get("BotTradeBroadcastMs");
        long now = System.currentTimeMillis();
        if (lastMs != null && now - ((Long) lastMs) < BROADCAST_INTERVAL_MS) return;
        bot.getTemporaryAttributtes().put("BotTradeBroadcastMs", now);
        String line = null;
        if (isGambler) {
            DiceMode mode = pickDiceModeForBot(bot);
            line = "host " + mode.name + " - trusted dicer";
        } else if (isTrader) {
            StockEntry stock = (StockEntry) bot.getTemporaryAttributtes().get("BotTraderStock");
            if (stock == null) {
                stock = pickStockForBot(bot);
                bot.getTemporaryAttributtes().put("BotTraderStock", stock);
            }
            if (stock != null) line = "selling " + stock.name + " " + stock.priceGp + "gp";
        }
        if (line != null) sayBoth(bot, line);
    }

    /** If bot's inventory has fewer coins than needed, withdraw from the
     *  MoneyPouch to top up. accumulatedWealth at create-time puts coins in
     *  the pouch (cb-scaled), not inventory - this bridges the two so trade
     *  payouts have something to addItem(). */
    private static void ensureInvCoins(AIPlayer bot, long needed) {
        try {
            long invHas = bot.getInventory().getAmountOf(COINS);
            if (invHas >= needed) return;
            long shortfall = needed - invHas;
            int pouch = bot.getMoneyPouch().getCoinsAmount();
            if (pouch <= 0) return;
            int withdraw = (int) Math.min(pouch, shortfall);
            bot.getMoneyPouch().setCoinsAmount(pouch - withdraw);
            bot.getInventory().addItem(COINS, withdraw);
        } catch (Throwable ignored) {}
    }

    /** Force-talk (overhead balloon) + public chat message (chat box). The
     *  overhead is what real players see in proximity; the public chat
     *  message is what shows in the chat box for nearby visible players.
     *  Bots' chat goes through neither by default - we have to manually
     *  broadcast so chat-box readers see the line. */
    static void sayBoth(AIPlayer bot, String text) {
        try { bot.setNextForceTalk(new com.rs.game.ForceTalk(text)); }
        catch (Throwable ignored) {}
        try {
            com.rs.game.player.PublicChatMessage msg =
                new com.rs.game.player.PublicChatMessage(text, 0);
            int botX = bot.getX(), botY = bot.getY(), botPlane = bot.getPlane();
            for (com.rs.game.player.Player real : com.rs.game.World.getPlayers()) {
                if (real == null) continue;
                if (real instanceof AIPlayer) continue;
                if (!real.hasStarted() || real.hasFinished()) continue;
                if (real.getPlane() != botPlane) continue;
                int dx = real.getX() - botX;
                int dy = real.getY() - botY;
                if (dx * dx + dy * dy > 196) continue; // ~14 tiles, std visibility
                try { real.getPackets().sendPublicMessage(bot, msg); }
                catch (Throwable ignored2) {}
            }
        } catch (Throwable ignored) {}
    }
}
