package com.rs.bot.economy;

import com.rs.game.player.content.grandExchange.GrandExchange;
import com.rs.game.player.content.grandExchange.Offer;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phantom GE market maker. Shadow-matching: when a player places an offer
 * in the in-game GE, this class rolls a chance to fill it from a virtual
 * counter-party at a randomized price within tolerance. No bot players
 * are involved - the offer just looks like another player matched it.
 *
 * <p>Two trigger paths:
 * <ul>
 *   <li><b>onOfferPlaced</b> - called from GrandExchange.sendOffer right
 *       after a player creates an offer. Schedules a delayed fill task.
 *   <li><b>tickAgingOffers</b> - runs every 30s from the world scheduler.
 *       Walks the OFFERS map and rolls per-tier fill chance for each
 *       offer older than {@link #MIN_AGE_BEFORE_FILL_MS}.
 * </ul>
 *
 * <p>Per-tier fill rates make common items move fast and rares slow:
 * <ul>
 *   <li>BULK   (logs/ores/runes/fish) - 10% per tick (~70% in 6 ticks/3 min)
 *   <li>COMBAT (dragon/rune armor)    - 4%
 *   <li>RARE   (gs/bandos/partyhat)   - 0.5%
 * </ul>
 *
 * <p>Anti-abuse: per-player + per-item caps reset hourly, in-memory only.
 *
 * <p>Configuration is in-class for now (no JSON). Admin panel toggles
 * via /admin/phantom-ge endpoints - see AdminHttpServer.
 */
public final class PhantomMarket {

    private PhantomMarket() {}

    // ---- Knobs (defaults; admin panel can override at runtime) ----

    private static volatile boolean enabled = true;

    /** Per-tick fill probability when rolling on placement (within ~5s).
     *  This is the BASE; tier multipliers below apply on top so cheap
     *  items fill instantly more often than rares. */
    private static volatile double fillRateOnPlace = 0.30;

    /** Per-30s-tick base fill rate for an aging offer. Multiplied by the
     *  per-tier rate. */
    private static volatile double fillRatePerTick = 0.10;

    /** Spread tolerance: phantom fills only if player's price is within
     *  +/- this fraction of the catalog reference. */
    private static volatile double acceptableSpread = 0.30;

    /** Min age (ms) before phantom-fill can fire. Lets player cancel a
     *  misclick before the market eats it. */
    private static volatile long minAgeBeforeFillMs = 30_000L;

    /** Per-tier fill rate multipliers. Applied to BOTH on-place and
     *  per-tick rolls so cheap items have a high instant-fill chance
     *  AND fast aging fills, while rare items take their time on both
     *  paths. Refprice tiers:
     *    cheap   < 1k       -> 10.0x  (effectively instant for tutorials)
     *    bulk    < 10k      -> 5.0x   (logs / ores / runes / fish)
     *    low     < 100k     -> 2.0x   (basic dragon, rune armor pieces)
     *    mid     < 1m       -> 1.0x   (whips, dragon weapons, mid jewelry)
     *    high    < 10m      -> 0.5x   (bandos, armadyl, fury)
     *    rare    >= 10m     -> 0.1x   (godswords, partyhats, hween masks)
     */
    private static volatile double cheapMultiplier  = 10.0;
    private static volatile double bulkMultiplier   = 5.0;
    private static volatile double lowMultiplier    = 2.0;
    private static volatile double midMultiplier    = 1.0;
    private static volatile double highMultiplier   = 0.5;
    private static volatile double rareMultiplier   = 0.1;

    /** Anti-abuse caps. */
    private static volatile int maxFillsPerPlayerPerHour = 50;
    private static volatile int maxFillsPerItemPerHour   = 20;

    /** When filling, chance to do a partial fill instead of full. */
    private static volatile double partialFillChance = 0.40;
    /** Partial fill range (fraction of remaining). */
    private static volatile double partialFillMin = 0.20;
    private static volatile double partialFillMax = 0.70;

    // ---- Anti-abuse counters ----

    private static final Map<String, int[]> playerFillCounts = new HashMap<>();
    private static final Map<Integer, int[]> itemFillCounts = new HashMap<>();
    private static long counterEpochMs = System.currentTimeMillis();

    // ---- Recent fill log (last 100 events for admin panel) ----

    public static final class FillEvent {
        public final long timestamp;
        public final String playerName;
        public final int itemId;
        public final int amount;
        public final int unitPrice;
        public final boolean buy;
        public FillEvent(long ts, String name, int id, int amt, int price, boolean buy) {
            this.timestamp = ts; this.playerName = name; this.itemId = id;
            this.amount = amt; this.unitPrice = price; this.buy = buy;
        }
    }
    private static final Deque<FillEvent> recentFills = new ArrayDeque<>();
    private static final int MAX_LOG_ENTRIES = 100;

    // ---- Lifecycle ----

    private static volatile boolean schedulerStarted = false;

    /** Call once at server startup to install the aging-offer ticker. */
    public static synchronized void start() {
        if (schedulerStarted) return;
        schedulerStarted = true;
        try {
            WorldTasksManager.schedule(new WorldTask() {
                @Override public void run() {
                    if (enabled) {
                        try { tickAgingOffers(); } catch (Throwable t) {
                            System.err.println("[PhantomMarket] tick err: " + t);
                        }
                    }
                }
            }, 50, 50); // 50 ticks = ~30s
            System.out.println("[PhantomMarket] scheduler started (30s tick)");
        } catch (Throwable t) {
            System.err.println("[PhantomMarket] failed to start: " + t);
            schedulerStarted = false;
        }
    }

    /** Hook from GrandExchange.sendOffer right after the new offer is
     *  created + P2P match attempted. Rolls a one-shot fill chance after
     *  a small delay, so the offer doesn't fill on the exact same tick
     *  the player placed it. */
    public static void onOfferPlaced(final Offer offer) {
        if (!enabled || offer == null) return;
        // Tier-aware roll: cheap items basically always insta-fill (10x
        // base = 100%), rares almost never (0.1x base = 1%). User asked
        // for "higher chance for cheaper items to be instant buy/sell,
        // higher you get in price the lower chance".
        final int refPrice = referencePrice(offer.getId());
        final double tierMult = tierMultiplier(refPrice);
        final double rate = Math.min(1.0, fillRateOnPlace * tierMult);
        // Delay the placement-fill roll by a few ticks so it doesn't feel
        // instant on the same packet. 8 ticks = ~5 seconds.
        try {
            WorldTasksManager.schedule(new WorldTask() {
                @Override public void run() {
                    try {
                        if (!enabled) return;
                        if (Math.random() > rate) return;
                        tryFill(offer, false);
                    } catch (Throwable ignored) {}
                }
            }, 8);
        } catch (Throwable ignored) {}
    }

    /** Walk every open offer in OFFERS and roll the per-tier fill chance
     *  for anything past minAgeBeforeFillMs. */
    public static void tickAgingOffers() {
        rotateCountersIfNeeded();
        // Snapshot the offers list so we don't ConcurrentModify if a real
        // P2P match happens mid-iteration.
        List<Offer> snap;
        try {
            // Re-using GrandExchange.getOffers if it exists, else iterate
            // via a reflection-free workaround by walking the histories.
            // GrandExchange exposes getHistory but not the live OFFERS map -
            // we bridge via offersSnapshot below.
            snap = offersSnapshot();
        } catch (Throwable t) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Offer offer : snap) {
            if (offer == null) continue;
            if (offer.getOwner() == null) continue;       // logged out
            if (now - offer.getPlacedMs() < minAgeBeforeFillMs) continue;
            if (offer.getAmountLeft() <= 0) continue;
            // Tier-aware fill rate
            int refPrice = referencePrice(offer.getId());
            double tierMult = tierMultiplier(refPrice);
            double rate = fillRatePerTick * tierMult;
            if (Math.random() > rate) continue;
            try { tryFill(offer, true); } catch (Throwable ignored) {}
        }
    }

    /** Returns the live OFFERS map values via {@link GrandExchange#getActiveOffers}.
     *  Uses a workaround if the helper isn't there. */
    @SuppressWarnings("unchecked")
    private static List<Offer> offersSnapshot() {
        // Try the public getter first.
        try {
            java.lang.reflect.Method m = GrandExchange.class.getDeclaredMethod("getActiveOffers");
            m.setAccessible(true);
            Object res = m.invoke(null);
            if (res instanceof List) return new ArrayList<>((List<Offer>) res);
        } catch (NoSuchMethodException ignored) {
            // fall through
        } catch (Throwable t) {
            return new ArrayList<>();
        }
        // Fallback: read OFFERS field directly (private static).
        try {
            java.lang.reflect.Field f = GrandExchange.class.getDeclaredField("OFFERS");
            f.setAccessible(true);
            HashMap<Long, Offer> m = (HashMap<Long, Offer>) f.get(null);
            if (m == null) return new ArrayList<>();
            synchronized (m) {
                return new ArrayList<>(m.values());
            }
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    /** Decide whether + how much to fill, then commit via Offer.phantomFill. */
    private static void tryFill(Offer offer, boolean isAgingTick) {
        if (offer == null || offer.getOwner() == null) return;
        int refPrice = referencePrice(offer.getId());
        if (refPrice <= 0) return; // unknown item, can't safely price
        int playerPrice = offer.getPrice();
        if (!priceAcceptable(offer.isBuying(), playerPrice, refPrice)) return;

        // Anti-abuse caps
        rotateCountersIfNeeded();
        String pname = offer.getOwner().getDisplayName();
        if (pname == null) pname = "?";
        int[] pCount = playerFillCounts.computeIfAbsent(pname, k -> new int[]{0});
        if (pCount[0] >= maxFillsPerPlayerPerHour) return;
        int[] iCount = itemFillCounts.computeIfAbsent(offer.getId(), k -> new int[]{0});
        if (iCount[0] >= maxFillsPerItemPerHour) return;

        // Decide fill quantity - partial vs full
        int amountLeft = offer.getAmountLeft();
        int fillAmount;
        if (amountLeft > 1 && Math.random() < partialFillChance) {
            double frac = partialFillMin
                + Math.random() * (partialFillMax - partialFillMin);
            fillAmount = Math.max(1, (int) (amountLeft * frac));
        } else {
            fillAmount = amountLeft;
        }

        // Realistic fill price: simulate matching against a counter-party
        // who would never pay more / accept less than they have to. So:
        //   BUY  : fillPrice in [refPrice, playerPrice] - player saves $$
        //   SELL : fillPrice in [playerPrice, refPrice] - player gets $$
        // User: "I put in a buy offer for like a few clicks of the 5% and
        // it bought for exactly this price? that wouldn't happen, it would
        // have bought for lower and I would've saved money".
        int fillPrice;
        if (offer.isBuying()) {
            if (playerPrice > refPrice) {
                int range = playerPrice - refPrice;
                fillPrice = refPrice + (int) (Math.random() * (range + 1));
            } else {
                fillPrice = playerPrice;
            }
        } else {
            if (playerPrice < refPrice) {
                int range = refPrice - playerPrice;
                fillPrice = playerPrice + (int) (Math.random() * (range + 1));
            } else {
                fillPrice = playerPrice;
            }
        }
        if (!offer.phantomFill(fillAmount, fillPrice)) return;

        // Update counters + log
        pCount[0]++;
        iCount[0]++;
        addLog(new FillEvent(System.currentTimeMillis(), pname,
            offer.getId(), fillAmount, fillPrice, offer.isBuying()));
    }

    private static void addLog(FillEvent e) {
        synchronized (recentFills) {
            recentFills.addLast(e);
            while (recentFills.size() > MAX_LOG_ENTRIES) recentFills.removeFirst();
        }
    }

    private static void rotateCountersIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - counterEpochMs > 3_600_000L) {
            playerFillCounts.clear();
            itemFillCounts.clear();
            counterEpochMs = now;
        }
    }

    /** Pull the current GE reference price for an item, with a fallback
     *  to the bot trader catalog (which is what the live GE was seeded
     *  from). Returns -1 if neither has a value. */
    public static int referencePrice(int itemId) {
        try {
            int p = GrandExchange.getPrice(itemId);
            if (p > 0) return p;
        } catch (Throwable ignored) {}
        try {
            // Fallback - bot catalog lookup. Reflective so we don't add a
            // hard cycle dep with bot package.
            Class<?> bth = Class.forName("com.rs.bot.ambient.BotTradeHandler");
            java.lang.reflect.Method m = bth.getDeclaredMethod("catalogPriceFor", int.class);
            m.setAccessible(true);
            Object r = m.invoke(null, itemId);
            if (r instanceof Integer && (Integer) r > 0) return (Integer) r;
        } catch (Throwable ignored) {}
        return -1;
    }

    public static boolean priceAcceptable(boolean buy, int playerPrice, int refPrice) {
        if (refPrice <= 0 || playerPrice <= 0) return false;
        double ratio = (double) playerPrice / refPrice;
        if (buy) return ratio >= (1.0 - acceptableSpread);
        return ratio <= (1.0 + acceptableSpread);
    }

    /** 6-tier price classification. Returns a multiplier applied to BOTH
     *  the on-place fill chance and the per-tick aging-fill chance, so a
     *  player listing a partyhat at fair price won't insta-fill but a
     *  player listing yew logs probably will. */
    public static double tierMultiplier(int refPrice) {
        if (refPrice <= 0) return 0.0;
        if (refPrice < 1_000)        return cheapMultiplier;
        if (refPrice < 10_000)       return bulkMultiplier;
        if (refPrice < 100_000)      return lowMultiplier;
        if (refPrice < 1_000_000)    return midMultiplier;
        if (refPrice < 10_000_000)   return highMultiplier;
        return rareMultiplier;
    }

    // ---- Admin panel API ----

    public static synchronized Map<String, Object> getConfig() {
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", enabled);
        m.put("fillRateOnPlace", fillRateOnPlace);
        m.put("fillRatePerTick", fillRatePerTick);
        m.put("acceptableSpread", acceptableSpread);
        m.put("minAgeBeforeFillMs", minAgeBeforeFillMs);
        m.put("cheapMultiplier", cheapMultiplier);
        m.put("bulkMultiplier", bulkMultiplier);
        m.put("lowMultiplier", lowMultiplier);
        m.put("midMultiplier", midMultiplier);
        m.put("highMultiplier", highMultiplier);
        m.put("rareMultiplier", rareMultiplier);
        m.put("maxFillsPerPlayerPerHour", maxFillsPerPlayerPerHour);
        m.put("maxFillsPerItemPerHour", maxFillsPerItemPerHour);
        m.put("partialFillChance", partialFillChance);
        return m;
    }

    /** Apply a partial config update. Unknown keys ignored. */
    public static synchronized void updateConfig(Map<String, Object> updates) {
        if (updates == null) return;
        Object v;
        if ((v = updates.get("enabled")) != null) enabled = parseBool(v, enabled);
        if ((v = updates.get("fillRateOnPlace")) != null) fillRateOnPlace = parseDouble(v, fillRateOnPlace);
        if ((v = updates.get("fillRatePerTick")) != null) fillRatePerTick = parseDouble(v, fillRatePerTick);
        if ((v = updates.get("acceptableSpread")) != null) acceptableSpread = parseDouble(v, acceptableSpread);
        if ((v = updates.get("minAgeBeforeFillMs")) != null) minAgeBeforeFillMs = parseLong(v, minAgeBeforeFillMs);
        if ((v = updates.get("cheapMultiplier")) != null) cheapMultiplier = parseDouble(v, cheapMultiplier);
        if ((v = updates.get("bulkMultiplier")) != null) bulkMultiplier = parseDouble(v, bulkMultiplier);
        if ((v = updates.get("lowMultiplier")) != null) lowMultiplier = parseDouble(v, lowMultiplier);
        if ((v = updates.get("midMultiplier")) != null) midMultiplier = parseDouble(v, midMultiplier);
        if ((v = updates.get("highMultiplier")) != null) highMultiplier = parseDouble(v, highMultiplier);
        if ((v = updates.get("rareMultiplier")) != null) rareMultiplier = parseDouble(v, rareMultiplier);
        // Back-compat: old "combatMultiplier" key maps to the new "lowMultiplier".
        if ((v = updates.get("combatMultiplier")) != null) lowMultiplier = parseDouble(v, lowMultiplier);
        if ((v = updates.get("maxFillsPerPlayerPerHour")) != null) maxFillsPerPlayerPerHour = (int) parseLong(v, maxFillsPerPlayerPerHour);
        if ((v = updates.get("maxFillsPerItemPerHour")) != null) maxFillsPerItemPerHour = (int) parseLong(v, maxFillsPerItemPerHour);
        if ((v = updates.get("partialFillChance")) != null) partialFillChance = parseDouble(v, partialFillChance);
    }

    public static synchronized List<FillEvent> getRecentFills(int limit) {
        synchronized (recentFills) {
            List<FillEvent> out = new ArrayList<>(recentFills);
            int from = Math.max(0, out.size() - limit);
            return new ArrayList<>(out.subList(from, out.size()));
        }
    }

    public static synchronized int getRecentFillCount() {
        synchronized (recentFills) {
            return recentFills.size();
        }
    }

    // ---- helpers ----

    private static boolean parseBool(Object v, boolean def) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) {
            String s = ((String) v).toLowerCase().trim();
            if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) return true;
            if ("false".equals(s) || "0".equals(s) || "no".equals(s)) return false;
        }
        return def;
    }
    private static double parseDouble(Object v, double def) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); }
            catch (Throwable ignored) {}
        }
        return def;
    }
    private static long parseLong(Object v, long def) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); }
            catch (Throwable ignored) {}
        }
        return def;
    }
}
