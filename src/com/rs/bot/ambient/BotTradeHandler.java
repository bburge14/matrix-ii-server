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
            clearTradeState(bot);
        } catch (Throwable t) {
            // Swallow - player retries on rejection.
        }
    }

    /** Clear all trade-machine attributes. Critical: a leak from a previous
     *  trade (BotTradeDecided=TRUE, BotTradeStage1=TRUE) made the next trade
     *  skip the dice roll AND skip stage 1 accept, leading to "always wins"
     *  apparent behavior. Called on every fresh trade open. */
    private static void clearTradeState(AIPlayer bot) {
        bot.getTemporaryAttributtes().remove("BotTradeStartMs");
        bot.getTemporaryAttributtes().remove("BotTradeDecided");
        bot.getTemporaryAttributtes().remove("BotTradeStage1");
        bot.getTemporaryAttributtes().remove("BotTradeStage2");
        bot.getTemporaryAttributtes().remove("BotTradeBet");
        bot.getTemporaryAttributtes().remove("BotTradeStockOffered");
        bot.getTemporaryAttributtes().remove("BotTradeWasInTrade");
    }

    // === SOCIALITE_GAMBLER: dice + payout ===
    //
    // Flow (matches RS dice host convention):
    //   1. Player adds GP to their side of trade
    //   2. PLAYER clicks Accept (commits the bet)
    //   3. Bot detects player.accepted=true -> rolls dice -> adds payout
    //      if win or nothing if lose -> announces result -> bot.accept(true)
    //      -> trade advances to stage 2 (Confirmation)
    //   4. Player accepts stage 2 -> bot accepts stage 2 -> trade completes
    //
    // Bot does NOT roll until the player has clicked Accept. Prior version
    // rolled as soon as ANY GP was visible, which felt like the bot was
    // pre-deciding the outcome.

    private static void handleGambler(AIPlayer bot, Trade trade) {
        Player target = trade.getTarget();
        if (target == null) return;
        Trade playerTrade = target.getTrade();
        if (playerTrade == null) return;

        // Already at confirmation stage: accept stage 2 to seal.
        Boolean stage2Done = (Boolean) bot.getTemporaryAttributtes().get("BotTradeStage2");
        if (Boolean.TRUE.equals(stage2Done)) {
            try { bot.getTrade().accept(false); } catch (Throwable ignored) {}
            return;
        }

        // Already accepted stage 1, waiting for player to accept their stage 1
        // (which advances both to stage 2). Bot is idle here.
        Boolean stage1Done = (Boolean) bot.getTemporaryAttributtes().get("BotTradeStage1");
        if (Boolean.TRUE.equals(stage1Done)) {
            // Detect stage advancement: after player accepts stage 1, both
            // sides' accepted reset to false (nextStage). Bot's hasAccepted
            // == false again is the signal we're at stage 2.
            if (!bot.getTrade().hasAccepted()) {
                bot.getTemporaryAttributtes().put("BotTradeStage2", Boolean.TRUE);
                try { bot.getTrade().accept(false); } catch (Throwable ignored) {}
            }
            return;
        }

        // Wait for player to accept stage 1 before rolling. This is the
        // commitment - player clicked Accept, locking in their bet.
        if (!playerTrade.hasAccepted()) return;

        // Player committed. Read their bet, roll, decide payout.
        int playerGp = countItem(playerTrade.getItemsContainer(), COINS);
        String pname = target.getDisplayName();
        if (playerGp < MIN_BET) {
            sayBoth(bot, "min bet " + MIN_BET + "gp - cancelling");
            bot.getTrade().cancelTrade();
            return;
        }
        DiceMode mode = pickDiceModeForBot(bot);
        int bet = Math.min(playerGp, MAX_BET);
        long payout = (long) bet * mode.payoutMultiplier;
        int roll = Utils.random(100);
        boolean win = roll >= mode.winThreshold;
        try {
            // Three-line "host announcement" pattern matching real RS
            // dicing convention: who-bet-what, what-rolled, who-won-what.
            sayBoth(bot, pname + " gave " + bet + "gp");
            if (win) {
                ensureInvCoins(bot, payout);
                if (bot.getInventory().getAmountOf(COINS) >= payout) {
                    bot.getTrade().addItem(new Item(COINS,
                        (int) Math.min(Integer.MAX_VALUE, payout)));
                    sayBoth(bot, mode.name + " rolled " + roll + " - WIN");
                    sayBoth(bot, pname + " wins " + payout + "gp");
                } else {
                    sayBoth(bot, "no funds, can't gamble that high");
                    bot.getTrade().cancelTrade();
                    return;
                }
            } else {
                sayBoth(bot, mode.name + " rolled " + roll + " - LOSE");
                sayBoth(bot, "house wins " + bet + "gp");
            }
            // Now accept stage 1 (locks in our offer + payout).
            bot.getTrade().accept(true);
            bot.getTemporaryAttributtes().put("BotTradeStage1", Boolean.TRUE);
            bot.getTemporaryAttributtes().put("BotTradeBet", bet);
        } catch (Throwable ignored) {}
    }

    // === SOCIALITE_GE_TRADER: stock + price ===

    private static void handleTrader(AIPlayer bot, Trade trade) {
        Player target = trade.getTarget();
        if (target == null) return;
        Trade playerTrade = target.getTrade();
        if (playerTrade == null) return;

        // Stage 2 (Confirmation): seal the trade.
        Boolean stage2Done = (Boolean) bot.getTemporaryAttributtes().get("BotTradeStage2");
        if (Boolean.TRUE.equals(stage2Done)) {
            try { bot.getTrade().accept(false); } catch (Throwable ignored) {}
            return;
        }

        // Already accepted stage 1; watch for advancement to stage 2.
        Boolean stage1Done = (Boolean) bot.getTemporaryAttributtes().get("BotTradeStage1");
        if (Boolean.TRUE.equals(stage1Done)) {
            if (!bot.getTrade().hasAccepted()) {
                bot.getTemporaryAttributtes().put("BotTradeStage2", Boolean.TRUE);
                try { bot.getTrade().accept(false); } catch (Throwable ignored) {}
            }
            return;
        }

        // Persistent per-bot stock - assigned at spawn (loadout), depleted as
        // trades complete. Read from bot inventory each trade so quantity
        // reflects what's actually still in stock.
        StockEntry stock = ensureBotStockAssigned(bot);
        if (stock == null) {
            sayBoth(bot, "out of stock");
            bot.getTrade().cancelTrade();
            return;
        }
        int onHand = bot.getInventory().getAmountOf(stock.itemId);
        if (onHand <= 0) {
            sayBoth(bot, "sold out of " + stock.name);
            bot.getTrade().cancelTrade();
            return;
        }

        // Check if player paid the right amount AND committed (accepted).
        // Only roll-and-accept after player commits, like the gambler flow.
        if (!playerTrade.hasAccepted()) {
            // First-time setup: add an item to our offer + announce price so
            // the player knows what to do.
            Boolean offered = (Boolean) bot.getTemporaryAttributtes().get("BotTradeStockOffered");
            if (!Boolean.TRUE.equals(offered)) {
                int saleQty = Math.min(onHand, stock.bundleSize);
                try {
                    bot.getTrade().addItem(new Item(stock.itemId, saleQty));
                    long price = (long) saleQty * stock.priceGp;
                    sayBoth(bot, "selling " + saleQty + "x " + stock.name + " for " + price + "gp");
                    bot.getTemporaryAttributtes().put("BotTradeStockOffered", Boolean.TRUE);
                    bot.getTemporaryAttributtes().put("BotTradeSaleQty", saleQty);
                } catch (Throwable ignored) {}
            }
            return;
        }

        // Player accepted - check their GP against our asking price for the
        // bundle on offer.
        int saleQty = (Integer) bot.getTemporaryAttributtes().getOrDefault("BotTradeSaleQty", 1);
        long askingPrice = (long) saleQty * stock.priceGp;
        int playerGp = countItem(playerTrade.getItemsContainer(), COINS);
        if (playerGp < askingPrice) {
            sayBoth(bot, "need " + askingPrice + "gp for " + saleQty + "x " + stock.name);
            // Don't cancel - player can still add more GP and re-accept.
            return;
        }
        try {
            bot.getTrade().accept(true);
            bot.getTemporaryAttributtes().put("BotTradeStage1", Boolean.TRUE);
        } catch (Throwable ignored) {}
    }

    /** Per-bot stock pick. Stable per bot: same bot keeps offering the same
     *  thing for the trade lifetime. Picks a low-tier item the bot can plausibly
     *  acquire. Real economy comes later. */
    private static StockEntry pickStockForBot(AIPlayer bot) {
        // Curated by category - skilling supplies, food, runes, gear, gems,
        // high-tier weapons + armor. Each bot deterministically picks one by
        // display-name hash so the same bot consistently sells the same thing.
        int idx = Math.abs((bot.getDisplayName() == null ? 0 : bot.getDisplayName().hashCode())) % CATALOG.length;
        return CATALOG[idx];
    }

    /** Pick stock + materialize starting inventory if not already done.
     *  Returns null only on catastrophic failure. */
    private static StockEntry ensureBotStockAssigned(AIPlayer bot) {
        StockEntry stock = (StockEntry) bot.getTemporaryAttributtes().get("BotTraderStock");
        if (stock == null) {
            stock = pickStockForBot(bot);
            if (stock == null) return null;
            bot.getTemporaryAttributtes().put("BotTraderStock", stock);
            stockBot(bot, stock);
        }
        return stock;
    }

    /** Spawn-time stock for SOCIALITE_GE_TRADER bots. Called by
     *  CitizenSpawner.spawnOne so the trader actually has inventory before
     *  any player initiates a trade. Pre-trade-time materialise was failing
     *  silently because toolkit ran first and ate inventory slots. */
    public static void preStockTrader(AIPlayer bot) {
        StockEntry stock = pickStockForBot(bot);
        if (stock == null) return;
        bot.getTemporaryAttributtes().put("BotTraderStock", stock);
        stockBot(bot, stock);
    }

    /** Materialise stock into the bot's inventory. Stackable items get a big
     *  pile in 1 slot; non-stackable get a few copies (limited by slots). */
    private static void stockBot(AIPlayer bot, StockEntry stock) {
        try {
            boolean stackable = isStackable(stock.itemId);
            int already = bot.getInventory().getAmountOf(stock.itemId);
            int target;
            if (stackable) {
                // 8 bundles for stackables (logs, runes, fish, ore, etc.) -
                // plenty of stock in 1 slot.
                target = stock.bundleSize * 8;
            } else {
                // Non-stackable (weapons, armor): cap at 3 to avoid eating
                // half the inventory. bundleSize is always 1 for these.
                target = 3;
            }
            int give = Math.max(0, target - already);
            if (give > 0) bot.getInventory().addItem(stock.itemId, give);
        } catch (Throwable ignored) {}
    }

    private static boolean isStackable(int itemId) {
        if (itemId == COINS) return true;
        try {
            com.rs.cache.loaders.ItemDefinitions def =
                com.rs.cache.loaders.ItemDefinitions.getItemDefinitions(itemId);
            return def != null && def.isStackable();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Trader stock catalog. Each entry includes a bundleSize - how many
     *  units the bot offers per sale. Bulk consumables (logs/runes/arrows)
     *  sell in 100s; high-tier weapons/armor sell as 1.
     *  Prices roughly match RS economy; tune per server later. */
    private static final StockEntry[] CATALOG = new StockEntry[] {
        // === Bulk skilling supplies (sell 100s) ===
        new StockEntry(1511, 50,     "logs",         100),
        new StockEntry(1521, 200,    "oak logs",     100),
        new StockEntry(1519, 400,    "willow logs",  100),
        new StockEntry(1517, 600,    "maple logs",   100),
        new StockEntry(1515, 800,    "yew logs",     100),
        new StockEntry(1513, 1500,   "magic logs",   100),
        new StockEntry(436,  50,     "copper ore",   100),
        new StockEntry(438,  50,     "tin ore",      100),
        new StockEntry(440,  150,    "iron ore",     100),
        new StockEntry(453,  150,    "coal",         100),
        new StockEntry(447,  250,    "mithril ore",  100),
        new StockEntry(449,  500,    "adamantite ore", 50),
        new StockEntry(451,  10000,  "runite ore",   10),
        new StockEntry(444,  500,    "gold ore",     50),
        // Raw + cooked fish
        new StockEntry(317,  100,    "raw shrimps",  100),
        new StockEntry(331,  300,    "raw salmon",   50),
        new StockEntry(335,  200,    "raw trout",    50),
        new StockEntry(371,  500,    "raw swordfish", 50),
        new StockEntry(377,  500,    "raw lobster",  50),
        new StockEntry(383,  900,    "raw shark",    50),
        new StockEntry(379,  600,    "lobster",      50),
        new StockEntry(385,  1200,   "shark",        50),
        // Runes
        new StockEntry(554,  20,     "fire runes",   500),
        new StockEntry(555,  20,     "water runes",  500),
        new StockEntry(556,  20,     "air runes",    500),
        new StockEntry(557,  20,     "earth runes",  500),
        new StockEntry(560,  300,    "death runes",  100),
        new StockEntry(562,  150,    "chaos runes",  100),
        new StockEntry(565,  250,    "blood runes",  100),
        // Bones (Prayer)
        new StockEntry(526,  50,     "bones",        100),
        new StockEntry(532,  150,    "big bones",    100),
        new StockEntry(536,  3500,   "dragon bones", 25),
        // Herbs
        new StockEntry(199,  100,    "grimy guam",   50),
        new StockEntry(207,  4000,   "grimy ranarr", 25),
        new StockEntry(219,  9000,   "grimy torstol", 10),
        // Gems
        new StockEntry(1623, 400,    "uncut sapphire", 25),
        new StockEntry(1619, 1000,   "uncut ruby",   25),
        new StockEntry(1617, 2500,   "uncut diamond", 10),

        // === High-tier weapons (sell as 1) ===
        new StockEntry(4587, 60_000,    "dragon scimitar", 1),
        new StockEntry(1305, 100_000,   "dragon longsword", 1),
        new StockEntry(7158, 120_000,   "dragon 2h sword", 1),
        new StockEntry(4151, 1_500_000, "abyssal whip", 1),
        new StockEntry(11696, 18_000_000, "armadyl godsword", 1),
        new StockEntry(11694, 12_000_000, "bandos godsword", 1),
        new StockEntry(11698, 8_000_000,  "saradomin godsword", 1),
        new StockEntry(11700, 5_000_000,  "zamorak godsword", 1),
        new StockEntry(13902, 15_000_000, "primal 2h sword", 1),
        new StockEntry(15039, 25_000_000, "drygore longsword", 1),
        // Bows / range
        new StockEntry(861,  600,       "magic shortbow", 1),
        new StockEntry(11212, 6_000,    "dragon arrows",  100),
        new StockEntry(4734, 800_000,   "karil's pistol crossbow", 1),
        new StockEntry(15241, 5_000_000, "hand cannon",   1),
        // Armor sets (high-tier)
        new StockEntry(11724, 18_000_000, "bandos chestplate", 1),
        new StockEntry(11726, 12_000_000, "bandos tassets",    1),
        new StockEntry(11722, 30_000_000, "armadyl chestplate", 1),
        new StockEntry(11720, 25_000_000, "armadyl chainskirt", 1),
        new StockEntry(11283, 1_500_000,  "dragonfire shield", 1),
        new StockEntry(1187,  100_000,    "dragon sq shield",  1),
        new StockEntry(1149,  60_000,     "dragon med helm",   1),
        // Mage robes (high-tier)
        new StockEntry(4708,  1_200_000, "ahrim's hood",      1),
        new StockEntry(4712,  3_000_000, "ahrim's robe top",  1),
        new StockEntry(4714,  2_500_000, "ahrim's robe bottom", 1),
        new StockEntry(6914,  5_000_000, "master wand",       1),
        new StockEntry(18353, 15_000_000, "virtus mask",      1),
        new StockEntry(18355, 25_000_000, "virtus robe top",  1),
        new StockEntry(18357, 22_000_000, "virtus robe legs", 1),
        // Amulets / jewelry / capes
        new StockEntry(1712, 12_000,    "amulet of glory(4)", 1),
        new StockEntry(1725, 4_000,     "amulet of strength", 1),
        new StockEntry(1731, 5_000,     "amulet of power",    1),
        new StockEntry(6585, 4_000_000, "amulet of fury",     1),
        new StockEntry(11128, 4_500_000, "berserker necklace", 1),
        new StockEntry(20000, 3_000_000, "fire cape",         1),
        // Barrows pieces
        new StockEntry(4716, 600_000,   "dharok's helm",      1),
        new StockEntry(4720, 1_200_000, "dharok's platebody", 1),
        new StockEntry(4722, 1_000_000, "dharok's platelegs", 1),
        new StockEntry(4718, 1_400_000, "dharok's greataxe",  1),
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
        public final int itemId, priceGp, bundleSize;
        public final String name;
        public StockEntry(int itemId, int priceGp, String name) {
            this(itemId, priceGp, name, 1);
        }
        public StockEntry(int itemId, int priceGp, String name, int bundleSize) {
            this.itemId = itemId; this.priceGp = priceGp; this.name = name;
            this.bundleSize = Math.max(1, bundleSize);
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
