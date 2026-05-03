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
     *  not actively in a trade. 10s = active "barker" pacing real RS hosts
     *  use. Each bot has a randomized phase offset so a cluster doesn't all
     *  shout at the same instant. */
    private static final long BROADCAST_INTERVAL_MS = 10_000;

    /** How many ticks the bot waits after entering trade before acting. Lets
     *  the player see the trade screen + add items / GP before bot responds. */
    private static final int RESPONSE_DELAY_TICKS = 4;

    private BotTradeHandler() {}

    public static void tick(AIPlayer bot, AmbientArchetype arch) {
        if (bot == null || arch == null) return;
        if (!arch.isSocialite()) return;
        // Only gambler + (any tier of) GE trader subtypes do trade.
        // Bankstanders ignore.
        boolean isGambler = arch == AmbientArchetype.SOCIALITE_GAMBLER;
        boolean isTrader  = arch.isTrader();
        if (!isGambler && !isTrader) return;

        Trade trade = bot.getTrade();
        if (trade == null) return;

        // Detect "trade just closed" - was trading last tick, now isn't.
        // For gamblers: this is when we stash the bet info to start the
        // PACED announcement phase. Was previously rolling + announcing
        // 3 lines + opening payout trade all in one tick - the user
        // reported it felt "WAY too quick".
        boolean inTrade = trade.isTrading();
        boolean wasInTrade = Boolean.TRUE.equals(
            bot.getTemporaryAttributtes().get("BotTradeWasInTrade"));
        if (wasInTrade && !inTrade && isGambler) {
            startGambleAnnouncePhase(bot);
        }
        bot.getTemporaryAttributtes().put("BotTradeWasInTrade", inTrade);

        if (!inTrade) {
            // Defensive: ensure no trade-state leaks from a previous trade.
            clearTradeState(bot);
            // Drive the paced post-trade announcement state machine. Each
            // phase fires ~3s after the prior so the player can READ:
            //   P0 -> "<name> gave Xgp"
            //   P1 -> "rolled Y"
            //   P2 -> "WIN!" or "LOSE"
            //   P3 -> if win, open payout trade
            if (isGambler && tickGambleAnnouncePhase(bot)) return;
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

    /** Step delay between paced announcement phases (ms). 2.5s ≈ enough time
     *  to read each line before the next appears. */
    private static final long ANNOUNCE_STEP_MS = 2500;

    /** Captures dice roll + pending payout state when the bet trade closes.
     *  The actual paced announcements run from tickGambleAnnouncePhase across
     *  the next several ticks. */
    private static void startGambleAnnouncePhase(AIPlayer bot) {
        try {
            Integer betObj = (Integer) bot.getTemporaryAttributtes().get("BotPendingBet");
            Player p = (Player) bot.getTemporaryAttributtes().get("BotPendingPlayer");
            Boolean wasPayout = (Boolean) bot.getTemporaryAttributtes().get("BotIsPayoutTrade");
            bot.getTemporaryAttributtes().remove("BotIsPayoutTrade");
            bot.getTemporaryAttributtes().remove("BotPayoutAmount");
            bot.getTemporaryAttributtes().remove("BotPayoutOffered");
            if (Boolean.TRUE.equals(wasPayout)) return;
            if (betObj == null || p == null) return;

            DiceMode mode = pickDiceModeForBot(bot);
            int roll = Utils.random(100);
            boolean win = roll >= mode.winThreshold;
            long payout = (long) (int) betObj * mode.payoutMultiplier;
            // Stash phase data
            bot.getTemporaryAttributtes().put("GambleAnnouncePhase", 0);
            bot.getTemporaryAttributtes().put("GambleAnnounceNextMs", System.currentTimeMillis());
            bot.getTemporaryAttributtes().put("GambleBet", (int) betObj);
            bot.getTemporaryAttributtes().put("GamblePlayer", p);
            bot.getTemporaryAttributtes().put("GambleRoll", roll);
            bot.getTemporaryAttributtes().put("GambleMode", mode);
            bot.getTemporaryAttributtes().put("GambleWin", Boolean.valueOf(win));
            bot.getTemporaryAttributtes().put("GamblePayout", payout);
            // Clear bet stash (now stored as Gamble*).
            bot.getTemporaryAttributtes().remove("BotPendingBet");
            bot.getTemporaryAttributtes().remove("BotPendingPlayer");
        } catch (Throwable ignored) {}
    }

    /** Drive the paced gamble announcement state machine. Returns true if
     *  the bot is currently in the paced phase (so the caller skips its
     *  other not-in-trade behavior like broadcasts). */
    private static boolean tickGambleAnnouncePhase(AIPlayer bot) {
        Integer phaseObj = (Integer) bot.getTemporaryAttributtes().get("GambleAnnouncePhase");
        if (phaseObj == null) return false;
        Long nextMsObj = (Long) bot.getTemporaryAttributtes().get("GambleAnnounceNextMs");
        long now = System.currentTimeMillis();
        if (nextMsObj != null && now < nextMsObj) return true;

        int phase = phaseObj;
        Player p = (Player) bot.getTemporaryAttributtes().get("GamblePlayer");
        Integer betObj = (Integer) bot.getTemporaryAttributtes().get("GambleBet");
        Integer rollObj = (Integer) bot.getTemporaryAttributtes().get("GambleRoll");
        DiceMode mode = (DiceMode) bot.getTemporaryAttributtes().get("GambleMode");
        Boolean winObj = (Boolean) bot.getTemporaryAttributtes().get("GambleWin");
        Long payoutObj = (Long) bot.getTemporaryAttributtes().get("GamblePayout");

        if (p == null || betObj == null || rollObj == null || mode == null
                || winObj == null || payoutObj == null) {
            clearGambleAnnounce(bot);
            return false;
        }
        String pname = p.getDisplayName();
        int bet = betObj;
        int roll = rollObj;
        boolean win = winObj;
        long payout = payoutObj;

        switch (phase) {
            case 0:
                sayBoth(bot, pname + " gave " + bet + "gp");
                bot.getTemporaryAttributtes().put("GambleAnnouncePhase", 1);
                bot.getTemporaryAttributtes().put("GambleAnnounceNextMs", now + ANNOUNCE_STEP_MS);
                return true;
            case 1:
                sayBoth(bot, "rolling " + mode.name + "...");
                bot.getTemporaryAttributtes().put("GambleAnnouncePhase", 2);
                bot.getTemporaryAttributtes().put("GambleAnnounceNextMs", now + ANNOUNCE_STEP_MS);
                return true;
            case 2:
                sayBoth(bot, "rolled " + roll + " (" + mode.name + ")");
                bot.getTemporaryAttributtes().put("GambleAnnouncePhase", 3);
                bot.getTemporaryAttributtes().put("GambleAnnounceNextMs", now + ANNOUNCE_STEP_MS);
                return true;
            case 3:
                if (win) {
                    sayBoth(bot, pname + " WINS " + payout + "gp!");
                } else {
                    sayBoth(bot, "house wins " + bet + "gp - better luck next time");
                }
                bot.getTemporaryAttributtes().put("GambleAnnouncePhase", 4);
                bot.getTemporaryAttributtes().put("GambleAnnounceNextMs", now + ANNOUNCE_STEP_MS);
                return true;
            case 4:
                if (win) {
                    // Stash for tryStartPayoutTrade in the next outer tick.
                    bot.getTemporaryAttributtes().put("BotPayoutPendingPlayer", p);
                    bot.getTemporaryAttributtes().put("BotPayoutPendingAmount", payout);
                    sayBoth(bot, "opening trade for payout...");
                }
                clearGambleAnnounce(bot);
                // Push next-payout-trade attempt one more step (don't open
                // trade in same tick as final chat).
                bot.getTemporaryAttributtes().put("PayoutOpenAfterMs", now + ANNOUNCE_STEP_MS);
                return true;
        }
        clearGambleAnnounce(bot);
        return false;
    }

    private static void clearGambleAnnounce(AIPlayer bot) {
        bot.getTemporaryAttributtes().remove("GambleAnnouncePhase");
        bot.getTemporaryAttributtes().remove("GambleAnnounceNextMs");
        bot.getTemporaryAttributtes().remove("GambleBet");
        bot.getTemporaryAttributtes().remove("GamblePlayer");
        bot.getTemporaryAttributtes().remove("GambleRoll");
        bot.getTemporaryAttributtes().remove("GambleMode");
        bot.getTemporaryAttributtes().remove("GambleWin");
        bot.getTemporaryAttributtes().remove("GamblePayout");
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

    /** Clear all per-trade attributes. Critical: a leak from a previous
     *  trade (BotTradeDecided=TRUE, BotTradeStage1=TRUE) made the next trade
     *  skip the dice roll AND skip stage 1 accept, leading to "always wins"
     *  apparent behavior. Called on every fresh trade open and defensively
     *  when not trading.
     *  Does NOT clear BotTraderStock - that's per-bot lifetime stock and
     *  persists across multiple trades until the bot is despawned. */
    private static void clearTradeState(AIPlayer bot) {
        // NOTE: do NOT clear Gamble* / BotPayoutPending* / PayoutOpenAfterMs
        // here. Those carry across the gap between bet trade and payout
        // trade and the paced-announce phase reads them.
        bot.getTemporaryAttributtes().remove("BotTradeStartMs");
        bot.getTemporaryAttributtes().remove("BotTradeDecided");
        bot.getTemporaryAttributtes().remove("BotTradeStage1");
        bot.getTemporaryAttributtes().remove("BotTradeStage2");
        bot.getTemporaryAttributtes().remove("BotTradeBet");
        bot.getTemporaryAttributtes().remove("BotTradeStockOffered");
        bot.getTemporaryAttributtes().remove("BotTradeSaleQty");
        bot.getTemporaryAttributtes().remove("BotTradeUnitsOffered");
        bot.getTemporaryAttributtes().remove("BotPayoutOffered");
        bot.getTemporaryAttributtes().remove("BotTraderInitialOffered");
    }

    // === SOCIALITE_GAMBLER: 2-trade dice flow ===
    //
    // Authentic RS dicing convention:
    //   Trade 1 (bet collection):
    //     - Player adds GP, clicks Accept
    //     - Bot accepts (no items added). Both confirm. Trade closes; bot
    //       has the bet.
    //   After trade close:
    //     - Bot rolls dice + announces 3-line result in chat
    //   Trade 2 (payout, only if win):
    //     - Bot opens trade with player, adds 2x bet
    //     - Player accepts, bot accepts. Both confirm. Player gets payout.
    //
    // Lose = bot keeps the gold, no second trade. Win = second trade with
    // 2x payout. State carried via BotPendingBet/BotPendingPlayer attrs
    // between trade close and payout-trade start.

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

        // Detect PAYOUT trade FIRST (before any wait-for-player gating). The
        // bot must add the winnings to its offer immediately so the player
        // can see them and accept; previously this was gated behind the
        // playerTrade.hasAccepted() check below, which meant the player saw
        // an empty trade window, never accepted, and the bot walked off.
        Boolean isPayoutTrade = (Boolean) bot.getTemporaryAttributtes().get("BotIsPayoutTrade");
        if (Boolean.TRUE.equals(isPayoutTrade)) {
            handlePayoutTrade(bot, trade);
            return;
        }

        // Bet trade: wait for player to commit (accept stage 1) before locking
        // in the bet amount. They can keep adding gp until they hit Accept.
        if (!playerTrade.hasAccepted()) return;

        int playerGp = countItem(playerTrade.getItemsContainer(), COINS);
        String pname = target.getDisplayName();
        if (playerGp < MIN_BET) {
            sayBoth(bot, "min bet is " + MIN_BET + "gp - try again");
            bot.getTrade().cancelTrade();
            return;
        }
        int bet = Math.min(playerGp, MAX_BET);
        // Stash bet info so processGambleAfterTradeClose can roll once the
        // trade closes. We do NOT add anything to our offer; just accept.
        bot.getTemporaryAttributtes().put("BotPendingBet", bet);
        bot.getTemporaryAttributtes().put("BotPendingPlayer", target);
        try {
            bot.getTrade().accept(true);
            bot.getTemporaryAttributtes().put("BotTradeStage1", Boolean.TRUE);
        } catch (Throwable ignored) {}
    }

    /** Second trade: bot has already opened it with player. Add payout to
     *  our offer IMMEDIATELY so player can see what they're getting, then
     *  wait for player to accept and accept ourselves. */
    private static void handlePayoutTrade(AIPlayer bot, Trade trade) {
        Player target = trade.getTarget();
        if (target == null) return;
        Long payoutObj = (Long) bot.getTemporaryAttributtes().get("BotPayoutAmount");
        if (payoutObj == null) {
            // Shouldn't happen but be safe.
            bot.getTrade().cancelTrade();
            return;
        }
        long payout = payoutObj;

        Boolean offered = (Boolean) bot.getTemporaryAttributtes().get("BotPayoutOffered");
        if (!Boolean.TRUE.equals(offered)) {
            // Add winnings IMMEDIATELY, BEFORE player accepts. Player needs
            // to see what's being offered to decide whether to accept.
            ensureInvCoins(bot, payout);
            if (bot.getInventory().getAmountOf(COINS) < payout) {
                sayBoth(bot, "couldn't pay out, sorry");
                bot.getTrade().cancelTrade();
                bot.getTemporaryAttributtes().remove("BotIsPayoutTrade");
                bot.getTemporaryAttributtes().remove("BotPayoutAmount");
                return;
            }
            try {
                bot.getTrade().addItem(new Item(COINS,
                    (int) Math.min(Integer.MAX_VALUE, payout)));
                sayBoth(bot, "here's your " + payout + "gp - hit accept");
                bot.getTemporaryAttributtes().put("BotPayoutOffered", Boolean.TRUE);
            } catch (Throwable ignored) {}
            return;
        }

        // Wait for player accept then accept ourselves.
        if (!target.getTrade().hasAccepted()) return;
        try {
            bot.getTrade().accept(true);
            bot.getTemporaryAttributtes().put("BotTradeStage1", Boolean.TRUE);
        } catch (Throwable ignored) {}
    }

    /** Bot-initiated payout trade. Returns true if started one. Respects
     *  PayoutOpenAfterMs - the announce phase parks one step before opening
     *  the trade so the chat doesn't pile up with the trade-open packet. */
    private static boolean tryStartPayoutTrade(AIPlayer bot) {
        Long openAfter = (Long) bot.getTemporaryAttributtes().get("PayoutOpenAfterMs");
        if (openAfter != null && System.currentTimeMillis() < openAfter) return false;
        bot.getTemporaryAttributtes().remove("PayoutOpenAfterMs");
        Player p = (Player) bot.getTemporaryAttributtes().get("BotPayoutPendingPlayer");
        Long amt = (Long) bot.getTemporaryAttributtes().get("BotPayoutPendingAmount");
        if (p == null || amt == null) return false;
        try {
            // Player must be nearby + not busy.
            if (p.hasFinished() || p.isCantTrade()) return false;
            if (!p.withinDistance(bot, 14)) return false;
            // Also bot can't be in an interface or other trade.
            if (bot.getTrade().isTrading()) return false;
            // Player can't be in another trade either.
            if (p.getTrade() != null && p.getTrade().isTrading()) return false;
            // Open both sides. With the Trade null-safe fix, the bot side
            // skips packet/UI calls and just sets target.
            bot.getTrade().openTrade(p);
            p.getTrade().openTrade(bot);
            bot.getTemporaryAttributtes().put("BotIsPayoutTrade", Boolean.TRUE);
            bot.getTemporaryAttributtes().put("BotPayoutAmount", amt);
            bot.getTemporaryAttributtes().remove("BotPayoutPendingPlayer");
            bot.getTemporaryAttributtes().remove("BotPayoutPendingAmount");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // === SOCIALITE_GE_TRADER: per-unit pricing ===
    //
    // Flow: bot is a vendor. Player decides QUANTITY by adding GP to the
    // trade - we derive units = floor(playerGp / pricePerUnit), capped at
    // bot's on-hand stock. So if bot is selling logs at 50gp each:
    //   - player adds 500gp -> bot offers 10 logs
    //   - player adds 5000gp -> bot offers 100 logs
    //   - player adds 50000gp -> bot offers stock-cap logs (capped)
    // High-tier weapons are bundleSize=1 priced as 1 unit, so player adds
    // exactly the price to get exactly 1.
    //
    // Bot waits for player.accept (commitment) before locking in offer +
    // accepting stage 1 - same pattern as gambler.

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

        StockEntry stock = ensureBotStockAssigned(bot);
        if (stock == null) {
            sayBoth(bot, "no stock right now, sorry");
            bot.getTrade().cancelTrade();
            return;
        }
        int onHand = bot.getInventory().getAmountOf(stock.itemId);
        if (onHand <= 0) {
            sayBoth(bot, "ran out of " + stock.name);
            bot.getTrade().cancelTrade();
            return;
        }

        // First-time announcement + put 1 unit immediately so the player
        // sees what they're buying before they add any gp. User feedback
        // was that bots said they were selling but never put the item in.
        Boolean announced = (Boolean) bot.getTemporaryAttributtes().get("BotTradeStockOffered");
        if (!Boolean.TRUE.equals(announced)) {
            sayBoth(bot, "selling " + stock.name + " at " + stock.priceGp
                + "gp each, " + onHand + " in stock - add gp + accept");
            try {
                // Put 1 sample unit in our offer immediately - player can
                // SEE the item now without needing to add gp first.
                bot.getTrade().addItem(new Item(stock.itemId, 1));
                bot.getTemporaryAttributtes().put("BotTradeUnitsOffered", 1);
                bot.getTemporaryAttributtes().put("BotTraderInitialOffered", Boolean.TRUE);
            } catch (Throwable ignored) {}
            bot.getTemporaryAttributtes().put("BotTradeStockOffered", Boolean.TRUE);
            return;
        }

        // Update our offer to match the player's gp. Re-runs each tick so
        // as they add more gp, the bot's offered qty grows in sync.
        int playerGp = countItem(playerTrade.getItemsContainer(), COINS);
        int unitsRequested = (int) Math.min(Integer.MAX_VALUE, (long) playerGp / stock.priceGp);
        // If player has 0gp, keep showing the 1-unit preview so they see
        // what they're buying (don't yank it back to 0).
        int units = Math.max(unitsRequested == 0 ? 1 : 0, Math.min(unitsRequested, onHand));
        Integer lastOfferedObj = (Integer) bot.getTemporaryAttributtes().get("BotTradeUnitsOffered");
        int lastOffered = lastOfferedObj == null ? 0 : lastOfferedObj;
        if (units != lastOffered) {
            try {
                ItemsContainer<Item> botItems = bot.getTrade().getItemsContainer();
                if (botItems != null) {
                    for (int i = 0; i < botItems.getSize(); i++) {
                        Item it = botItems.get(i);
                        if (it != null && it.getId() == stock.itemId) {
                            botItems.set(i, null);
                        }
                    }
                }
                if (units > 0) {
                    bot.getTrade().addItem(new Item(stock.itemId, units));
                }
                bot.getTemporaryAttributtes().put("BotTradeUnitsOffered", units);
                // Refresh both views since we manually nulled slots.
                try { bot.getTrade().refresh(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27); } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        }

        // Wait for player to commit by clicking Accept.
        if (!playerTrade.hasAccepted()) return;
        if (unitsRequested <= 0) {
            sayBoth(bot, "need at least " + stock.priceGp + "gp for 1 " + stock.name);
            bot.getTrade().cancelTrade();
            return;
        }

        // Player accepted with qty matching their gp. Bot accepts.
        try {
            int actualUnits = Math.min(unitsRequested, onHand);
            long total = (long) actualUnits * stock.priceGp;
            sayBoth(bot, "deal! " + actualUnits + "x " + stock.name + " for " + total + "gp");
            bot.getTrade().accept(true);
            bot.getTemporaryAttributtes().put("BotTradeStage1", Boolean.TRUE);
            bot.getTemporaryAttributtes().put("BotTradeSaleQty", actualUnits);
        } catch (Throwable ignored) {}
    }

    /** Per-bot stock pick. Stable per bot: same bot keeps offering the same
     *  thing for the trade lifetime. Picks from the appropriate tier catalog
     *  for the bot's archetype; falls back to the mixed catalog for legacy
     *  SOCIALITE_GE_TRADER. Skips any catalog entry that isn't tradeable
     *  (deg PvP gear, charged items etc) - was rolling 13902 "primal 2h"
     *  which is actually Statius's warhammer + degrades = untradeable. */
    private static StockEntry pickStockForBot(AIPlayer bot) {
        StockEntry[] pool = catalogForBot(bot);
        if (pool == null || pool.length == 0) return null;
        int hash = bot.getDisplayName() == null ? 0
            : bot.getDisplayName().hashCode();
        int base = Math.abs(hash) % pool.length;
        // Walk the catalog from `base` and return the first tradeable entry.
        // Without this, a bot whose name happened to hash to an untradeable
        // entry would silently fail to stock and immediately cancel "sold out".
        for (int i = 0; i < pool.length; i++) {
            StockEntry e = pool[(base + i) % pool.length];
            if (isTradeableId(e.itemId)) return e;
        }
        return null;
    }

    /** Pick the right catalog tier for a bot's archetype. */
    private static StockEntry[] catalogForBot(AIPlayer bot) {
        try {
            if (bot.getBrain() instanceof com.rs.bot.ambient.CitizenBrain) {
                AmbientArchetype a = ((com.rs.bot.ambient.CitizenBrain) bot.getBrain()).getArchetype();
                if (a != null) {
                    int tier = a.traderTier();
                    if (tier == 0) return CATALOG_SKILL;
                    if (tier == 1) return CATALOG_COMBAT;
                    if (tier == 2) return CATALOG_RARE;
                }
            }
        } catch (Throwable ignored) {}
        // Legacy SOCIALITE_GE_TRADER falls back to the mixed pool.
        return CATALOG_MIXED;
    }

    /** Wraps ItemConstants.isTradeable so we don't bake bad IDs into stocks.
     *  Trade.addItem early-returns silently for untradeable items - was the
     *  "added gp, never got my masterwand" bug for catalog entries with charges. */
    private static boolean isTradeableId(int itemId) {
        try {
            return com.rs.game.player.content.ItemConstants.isTradeable(new Item(itemId, 1));
        } catch (Throwable t) {
            return false;
        }
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
        // Re-stock if depleted (bot sold everything between trades).
        try {
            if (bot.getInventory().getAmountOf(stock.itemId) <= 0) {
                stockBot(bot, stock);
            }
        } catch (Throwable ignored) {}
        return stock;
    }

    /** Spawn-time stock for trader bots. Called by CitizenSpawner.spawnOne
     *  so the trader actually has inventory before any player initiates a
     *  trade. Pre-trade-time materialise was failing silently because toolkit
     *  ran first and ate inventory slots. */
    public static void preStockTrader(AIPlayer bot) {
        StockEntry stock = pickStockForBot(bot);
        if (stock == null) {
            System.err.println("[BotTradeHandler] preStockTrader: no tradeable catalog entry for "
                + bot.getDisplayName());
            return;
        }
        bot.getTemporaryAttributtes().put("BotTraderStock", stock);
        stockBot(bot, stock);
        // Verify - if we couldn't stock anything (inv full), surface it.
        try {
            int onHand = bot.getInventory().getAmountOf(stock.itemId);
            if (onHand <= 0) {
                System.err.println("[BotTradeHandler] preStockTrader: " + bot.getDisplayName()
                    + " could not stock " + stock.name + " (id=" + stock.itemId
                    + ") - inv full?");
            }
        } catch (Throwable ignored) {}
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

    // ===== TIERED CATALOGS =====
    //
    // Three tiers, three trader archetypes, three GE-quadrant spawn anchors.
    // Each entry is (itemId, pricePerUnit, name, bundleSize). Prices tuned
    // to RSPS economy (~10% of OSRS GE), per-unit so player chooses qty by
    // amount of GP they add to the trade.
    //
    // ALL ENTRIES MUST BE TRADEABLE on this server. We filter via
    // ItemConstants.isTradeable at pick-time, but listing only known-good
    // ids keeps the bots' offered stock predictable. Removed previously:
    //   - 13902 "primal 2h sword" (actually Statius warhammer, degrading)
    //   - 18353/18355/18357 "virtus" (chaotic items, untradeable range)
    //   - 18359/18361 chaotic family (untradeable 18330-18374)
    //   - barrows raw 4708-4738 (degrade-on-wear; tradeable but unstable)
    //   - 13884/13890/13896 statius pvp gear (degrades)

    /** Tier 1: bulk skilling supplies (logs, ores, runes, raw/cooked fish,
     *  bones, herbs, gems). Per-unit pricing so a player adds GP and gets
     *  the matching quantity. */
    private static final StockEntry[] CATALOG_SKILL = new StockEntry[] {
        // Logs
        new StockEntry(1511, 50,     "logs",         100),
        new StockEntry(1521, 200,    "oak logs",     100),
        new StockEntry(1519, 400,    "willow logs",  100),
        new StockEntry(1517, 600,    "maple logs",   100),
        new StockEntry(1515, 800,    "yew logs",     100),
        new StockEntry(1513, 1500,   "magic logs",   100),
        // Ores
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
        // Arrows / ammo (consumables - bulk)
        new StockEntry(884,  20,     "steel arrows", 500),
        new StockEntry(886,  40,     "mithril arrows", 500),
        new StockEntry(888,  80,     "adamant arrows", 500),
        new StockEntry(892,  150,    "rune arrows",  500),
    };

    /** Tier 2: combat gear (mid). Dragon weapons, rune armor, basic mage
     *  staves, mid-tier amulets, F2P gear singles. */
    private static final StockEntry[] CATALOG_COMBAT = new StockEntry[] {
        // Dragon weapons (singles)
        new StockEntry(4587, 25_000,    "dragon scimitar", 1),
        new StockEntry(1305, 50_000,    "dragon longsword", 1),
        new StockEntry(7158, 60_000,    "dragon 2h sword", 1),
        new StockEntry(1377, 35_000,    "dragon battleaxe", 1),
        new StockEntry(1434, 25_000,    "dragon mace", 1),
        new StockEntry(1215, 30_000,    "dragon dagger", 1),
        new StockEntry(4151, 150_000,   "abyssal whip", 1),
        new StockEntry(861,  600,       "magic shortbow", 1),
        new StockEntry(859,  500,       "magic longbow", 1),
        // Rune armor (singles)
        new StockEntry(1163, 30_000,    "rune full helm", 1),
        new StockEntry(1127, 40_000,    "rune platebody", 1),
        new StockEntry(1079, 25_000,    "rune platelegs", 1),
        new StockEntry(1201, 25_000,    "rune kiteshield", 1),
        new StockEntry(1333, 18_000,    "rune scimitar", 1),
        new StockEntry(1373, 15_000,    "rune battleaxe", 1),
        new StockEntry(1303, 20_000,    "rune longsword", 1),
        // Dragon armor pieces (singles)
        new StockEntry(1149,  30_000,   "dragon med helm", 1),
        new StockEntry(1187,  50_000,   "dragon sq shield", 1),
        new StockEntry(11283, 150_000,  "dragonfire shield", 1),
        // Mage stuff
        new StockEntry(1389, 8_000,     "mystic staff", 1),
        new StockEntry(1391, 5_000,     "staff of air",  1),
        new StockEntry(6914, 80_000,    "master wand", 1),
        new StockEntry(4097, 35_000,    "mystic robe top (red)", 1),
        new StockEntry(4099, 30_000,    "mystic robe legs (red)", 1),
        // Amulets / jewelry
        new StockEntry(1712, 12_000,    "amulet of glory(4)", 1),
        new StockEntry(1725, 4_000,     "amulet of strength", 1),
        new StockEntry(1731, 5_000,     "amulet of power",    1),
        new StockEntry(11128, 450_000,  "berserker necklace", 1),
        new StockEntry(11105, 8_000,    "skills necklace(4)", 1),
        // Range
        new StockEntry(11212, 1_500,    "dragon arrows", 100),
        new StockEntry(4734, 80_000,    "karil's crossbow", 1),
        // Boots / gloves
        new StockEntry(11732, 60_000,   "dragon boots",  1),
        new StockEntry(2577,  25_000,   "ranger boots",  1),
    };

    /** Tier 3: rares + endgame. Godswords, bandos/armadyl, fury, fire cape. */
    private static final StockEntry[] CATALOG_RARE = new StockEntry[] {
        new StockEntry(11696, 1_800_000, "armadyl godsword", 1),
        new StockEntry(11694, 1_200_000, "bandos godsword", 1),
        new StockEntry(11698, 800_000,   "saradomin godsword", 1),
        new StockEntry(11700, 500_000,   "zamorak godsword", 1),
        new StockEntry(11724, 1_800_000, "bandos chestplate", 1),
        new StockEntry(11726, 1_200_000, "bandos tassets",    1),
        new StockEntry(11722, 3_000_000, "armadyl chestplate", 1),
        new StockEntry(11720, 2_500_000, "armadyl chainskirt", 1),
        new StockEntry(11718, 1_500_000, "armadyl helmet", 1),
        new StockEntry(11728, 600_000,   "bandos boots", 1),
        new StockEntry(6585,  400_000,   "amulet of fury", 1),
        new StockEntry(6737,  900_000,   "berserker ring", 1),
        new StockEntry(6735,  600_000,   "warrior ring", 1),
        new StockEntry(6733,  400_000,   "archers ring", 1),
        new StockEntry(6731,  500_000,   "seers ring", 1),
        new StockEntry(2572,  300_000,   "ring of wealth", 1),
        new StockEntry(15241, 500_000,   "hand cannon", 1),
        // Spirit shields
        new StockEntry(13738, 8_000_000, "arcane spirit shield", 1),
        new StockEntry(13740, 6_000_000, "divine spirit shield", 1),
        new StockEntry(13742, 5_000_000, "elysian spirit shield", 1),
        new StockEntry(13744, 4_000_000, "spectral spirit shield", 1),
        // Tradeable rares
        new StockEntry(20000, 300_000,   "fire cape", 1),
    };

    /** Mixed legacy catalog - flattened union of the three tiers, used by
     *  the back-compat SOCIALITE_GE_TRADER archetype only. New spawns should
     *  use the tier-specific archetypes. */
    private static final StockEntry[] CATALOG_MIXED = mergeCatalogs(
        CATALOG_SKILL, CATALOG_COMBAT, CATALOG_RARE);

    private static StockEntry[] mergeCatalogs(StockEntry[]... arrays) {
        int total = 0;
        for (StockEntry[] arr : arrays) total += arr.length;
        StockEntry[] out = new StockEntry[total];
        int i = 0;
        for (StockEntry[] arr : arrays)
            for (StockEntry e : arr) out[i++] = e;
        return out;
    }

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

    /** Periodic chatter so players see what the bot offers without trading.
     *  Each bot gets a random phase offset so a cluster of bots doesn't
     *  shout in unison. Line varies between calls for variety. */
    private static void maybeBroadcast(AIPlayer bot, boolean isGambler, boolean isTrader) {
        Object lastMs = bot.getTemporaryAttributtes().get("BotTradeBroadcastMs");
        long now = System.currentTimeMillis();
        // Phase offset (0-7s) so 10 bots at the same spawn don't sync.
        Long phase = (Long) bot.getTemporaryAttributtes().get("BotTradeBroadcastPhase");
        if (phase == null) {
            phase = (long) Utils.random(7000);
            bot.getTemporaryAttributtes().put("BotTradeBroadcastPhase", phase);
        }
        if (lastMs != null && now - ((Long) lastMs) < BROADCAST_INTERVAL_MS + phase) return;
        bot.getTemporaryAttributtes().put("BotTradeBroadcastMs", now);
        String line = null;
        if (isGambler) {
            DiceMode mode = pickDiceModeForBot(bot);
            String[] templates = new String[] {
                "Dice Game " + mode.name + " - trusted host",
                "Dicing here! " + mode.name + " up to 1m",
                mode.name + " active - msg to play",
                "Hot dice! " + mode.name + " - quick payouts",
                "Dicing " + mode.name + " - trade me to play",
                "Trusted host - " + mode.name + " no scams"
            };
            line = templates[Utils.random(templates.length)];
        } else if (isTrader) {
            StockEntry stock = (StockEntry) bot.getTemporaryAttributtes().get("BotTraderStock");
            if (stock == null) {
                stock = pickStockForBot(bot);
                if (stock != null) bot.getTemporaryAttributtes().put("BotTraderStock", stock);
            }
            if (stock != null) {
                String[] templates = new String[] {
                    "selling " + stock.name + " " + stock.priceGp + "gp each",
                    "wts " + stock.name + " " + stock.priceGp + "gp",
                    stock.name + " for sale - " + stock.priceGp + "gp ea",
                    "got " + stock.name + " - " + stock.priceGp + "gp",
                    "buy " + stock.name + " " + stock.priceGp + "gp"
                };
                line = templates[Utils.random(templates.length)];
            }
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

    /** Force-talk (overhead balloon) + public chat message (chat box) with
     *  per-bot stable color/animation effect. Public so CitizenBrain can use
     *  it for non-trade chatter (idle, panic) too - was previously using
     *  ForceTalk-only which doesn't reach chat boxes.
     *
     *  Effects format: (color << 8) | animation
     *    color  6=glow1, 7=glow2, 8=glow3, 9=flash1, 10=flash2, 11=flash3,
     *           1=red, 2=green, 3=cyan, 4=purple, 5=white, 0=yellow
     *    anim   0=none, 1=wave, 2=wave2, 3=shake, 4=scroll, 5=slide
     *
     *  Each bot rolls one effect at first chat (cached in TemporaryAttributtes)
     *  so the same bot consistently uses the same color/animation - mirrors
     *  RS hosts who have a "signature look". */
    public static void sayBoth(AIPlayer bot, String text) {
        // ForceTalk REMOVED. The user reported chat appearing with the
        // effect (color/anim) then flickering back to plain yellow - that
        // was ForceTalk's plain-text overhead bubble overwriting the
        // PublicChatMessage's effected one a frame later. PublicChatMessage
        // alone renders both the chat box entry AND the overhead bubble
        // with effects intact.
        try {
            int effects = chatEffectFor(bot);
            com.rs.game.player.PublicChatMessage msg =
                new com.rs.game.player.PublicChatMessage(text, effects);
            int botX = bot.getX(), botY = bot.getY(), botPlane = bot.getPlane();
            for (com.rs.game.player.Player real : com.rs.game.World.getPlayers()) {
                if (real == null) continue;
                if (real instanceof AIPlayer) continue;
                if (!real.hasStarted() || real.hasFinished()) continue;
                if (real.getPlane() != botPlane) continue;
                int dx = real.getX() - botX;
                int dy = real.getY() - botY;
                if (dx * dx + dy * dy > 196) continue; // ~14 tiles
                try { real.getPackets().sendPublicMessage(bot, msg); }
                catch (Throwable ignored2) {}
            }
        } catch (Throwable ignored) {}
    }

    /** Per-bot stable chat effect. Rolled once per bot, cached.
     *  Larger pool than before so adjacent bots don't all use the same color
     *  - user feedback was that GE bots all looked identical. */
    private static int chatEffectFor(AIPlayer bot) {
        Object cached = bot.getTemporaryAttributtes().get("BotChatEffect");
        if (cached != null) return (Integer) cached;
        // Effect format: (color << 8) | animation
        //   color  0=yellow, 1=red, 2=green, 3=cyan, 4=purple, 5=white,
        //          6=glow1 (red->yellow), 7=glow2 (red->purple),
        //          8=glow3 (white->green), 9=flash1, 10=flash2, 11=flash3
        //   anim   0=none, 1=wave, 2=wave2, 3=shake, 4=scroll, 5=slide
        int[] pool = new int[] {
            // Plain colors
            (1 << 8) | 0,    // red
            (2 << 8) | 0,    // green
            (3 << 8) | 0,    // cyan
            (4 << 8) | 0,    // purple
            (5 << 8) | 0,    // white
            // Glow effects
            (6 << 8) | 0,    // glow1
            (7 << 8) | 0,    // glow2
            (8 << 8) | 0,    // glow3
            // Flash effects
            (9 << 8) | 0,    // flash1
            (10 << 8) | 0,   // flash2
            (11 << 8) | 0,   // flash3
            // Color + animation combos
            (1 << 8) | 1,    // red + wave
            (1 << 8) | 4,    // red + scroll
            (2 << 8) | 1,    // green + wave
            (2 << 8) | 4,    // green + scroll
            (3 << 8) | 2,    // cyan + wave2
            (4 << 8) | 5,    // purple + slide
            (5 << 8) | 4,    // white + scroll
            (6 << 8) | 1,    // glow1 + wave
            (6 << 8) | 4,    // glow1 + scroll
            (7 << 8) | 2,    // glow2 + wave2
            (8 << 8) | 1,    // glow3 + wave
            (9 << 8) | 3,    // flash1 + shake
            (10 << 8) | 4,   // flash2 + scroll
            (11 << 8) | 5,   // flash3 + slide
        };
        int hash = bot.getDisplayName() == null ? 0 : bot.getDisplayName().hashCode();
        int eff = pool[Math.abs(hash) % pool.length];
        bot.getTemporaryAttributtes().put("BotChatEffect", eff);
        return eff;
    }
}
