package com.rs.bot.ambient;

import com.rs.bot.AIPlayer;
import com.rs.game.World;
import com.rs.game.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to real-player public chat at GE / Edge bank and gives nearby
 * trader/gambler bots a chance to respond.
 *
 * Two distinct things:
 *
 *   1) WTB / WTS broadcast detection
 *      Player types "wtb whip 1m" / "selling logs 100ea" in public chat ->
 *      a nearby trader bot opens a trade and either offers the item (BUY
 *      intent = player buying = bot selling) or accepts the player's item
 *      for gold (SELL intent = player selling = bot buying).
 *
 *      Phrases parsed:
 *        BUY:  "wtb X price", "want to buy X price", "buying X price",
 *              "need X price", "lf X price"
 *        SELL: "wts X price", "want to sell X price", "selling X price",
 *              "got X price"
 *
 *      Price tolerance: bot only responds if the player's price is within
 *      +/- 25% of the bot's catalog price (so we don't lose 100M on a
 *      partyhat to a clever player typing "wtb red phat 1gp").
 *
 *   2) In-trade quantity narrowing
 *      Player is in trade with a trader bot offering a stack (e.g. 100
 *      logs). Player types "i want 25 logs" or just "25 logs" in chat.
 *      Bot updates its offer to 25 instead of 100. User explicitly asked
 *      for this: "the user can say they want 25logs. so the trader puts
 *      up 25logs".
 *
 * Hook point: WorldPacketsDecoder calls onPublicChat() right after
 * Player.sendPublicChatMessage(...) for every real-player chat.
 *
 * Item alias DB: small in-memory map from common nicknames to item ids.
 * Add aliases here as they come up - the parser does longest-match so
 * "blue partyhat" wins over "blue".
 */
public final class BotChatListener {

    private BotChatListener() {}

    public enum Intent { BUY, SELL }

    public static final class TradeIntent {
        public Intent intent;
        public int itemId;
        public String itemName;
        public long price;       // -1 if no price specified
        public int quantity = 1;
    }

    /** Seconds a bot must wait before re-responding to chat from a player.
     *  Without this, every chat line spawns a new trade attempt. */
    private static final long CHAT_RESPONSE_COOLDOWN_MS = 30_000;

    /** Max distance bot will respond to public chat from. ~14 tiles. */
    private static final int CHAT_RESPONSE_RADIUS_SQ = 196;

    /** Price tolerance window: +/- 25% of catalog price. */
    private static final double PRICE_TOLERANCE = 0.25;

    /** Common item aliases. Many phrases map to one item id. Lowercase keys
     *  only - we lowercase the player's chat before looking up. */
    private static final Map<String, Integer> ALIASES = new HashMap<>();
    static {
        // === Combat weapons ===
        addAlias(4151, "abyssal whip", "abby whip", "whip");
        addAlias(11696, "armadyl godsword", "ags");
        addAlias(11694, "bandos godsword", "bgs");
        addAlias(11698, "saradomin godsword", "sgs", "sara gs");
        addAlias(11700, "zamorak godsword", "zgs", "zammy gs");
        addAlias(4587, "dragon scimitar", "d scim", "dscim");
        addAlias(1305, "dragon longsword", "d long");
        addAlias(7158, "dragon 2h sword", "dragon 2h", "d 2h");
        addAlias(1377, "dragon battleaxe", "d baxe");
        addAlias(1434, "dragon mace", "d mace");
        addAlias(1215, "dragon dagger", "d dagger", "ddag");
        addAlias(11283, "dragonfire shield", "dfs");
        addAlias(861, "magic shortbow", "msb");
        addAlias(859, "magic longbow", "mlb");
        addAlias(15241, "hand cannon", "hc");
        addAlias(4734, "karil's crossbow", "karils cbow", "karil cbow");
        // === Combat armor ===
        addAlias(11724, "bandos chestplate", "bcp");
        addAlias(11726, "bandos tassets", "tassets", "btassets");
        addAlias(11722, "armadyl chestplate", "acp");
        addAlias(11720, "armadyl chainskirt", "achainskirt", "achain");
        addAlias(11718, "armadyl helmet", "ahelm");
        addAlias(11728, "bandos boots", "bb");
        addAlias(11732, "dragon boots", "dboots");
        addAlias(2577, "ranger boots", "rangers");
        addAlias(1163, "rune full helm", "rune fh");
        addAlias(1127, "rune platebody", "rune pb");
        addAlias(1079, "rune platelegs", "rune pl");
        addAlias(1201, "rune kiteshield", "rune kite");
        addAlias(1149, "dragon med helm", "d med");
        addAlias(1187, "dragon sq shield", "dsq");
        // === Spirit shields ===
        addAlias(13738, "arcane spirit shield", "arcane");
        addAlias(13740, "divine spirit shield", "divine");
        addAlias(13742, "elysian spirit shield", "elysian", "ely");
        addAlias(13744, "spectral spirit shield", "spectral");
        // === Mage ===
        addAlias(1389, "mystic staff");
        addAlias(1391, "staff of air");
        addAlias(6914, "master wand", "mwand");
        // Mystic IDs verified against ItemSets.java (all 3 colors)
        addAlias(4091, "mystic robe top blue", "mystic top blue", "mystic top");
        addAlias(4093, "mystic robe legs blue", "mystic legs blue", "mystic legs");
        addAlias(4101, "mystic robe top dark", "mystic top dark");
        addAlias(4103, "mystic robe legs dark", "mystic legs dark");
        addAlias(4111, "mystic robe top light", "mystic top light");
        addAlias(4113, "mystic robe legs light", "mystic legs light");
        // === Jewelry ===
        addAlias(6585, "amulet of fury", "fury");
        addAlias(1712, "amulet of glory", "glory");
        addAlias(1725, "amulet of strength", "str ammy", "amulet of str");
        addAlias(1731, "amulet of power", "power ammy");
        addAlias(11128, "berserker necklace", "berserker neck", "bneck");
        addAlias(11105, "skills necklace");
        addAlias(6737, "berserker ring", "b ring", "bring");
        addAlias(6735, "warrior ring", "w ring", "wring");
        addAlias(6733, "archers ring", "a ring", "aring");
        addAlias(6731, "seers ring", "s ring", "sring");
        addAlias(2572, "ring of wealth", "row");
        addAlias(6570, "fire cape", "fc", "firecape");
        // === Holiday rares ===
        addAlias(1038, "red partyhat", "red phat", "rphat");
        addAlias(1040, "yellow partyhat", "yellow phat", "yphat");
        addAlias(1042, "blue partyhat", "blue phat", "bphat");
        addAlias(1044, "green partyhat", "green phat", "gphat");
        addAlias(1046, "purple partyhat", "purple phat", "pphat");
        addAlias(1048, "white partyhat", "white phat", "wphat");
        addAlias(1050, "santa hat", "santa");
        addAlias(1053, "red h'ween mask", "red mask", "red hween", "red halloween");
        addAlias(1055, "blue h'ween mask", "blue mask", "blue hween", "blue halloween");
        addAlias(1057, "green h'ween mask", "green mask", "green hween", "green halloween");
        addAlias(962, "christmas cracker", "cracker");
        addAlias(1959, "pumpkin");
        addAlias(7927, "easter ring");
        addAlias(7771, "disk of returning", "disk");
        addAlias(4566, "rubber chicken", "chicken");
        addAlias(1052, "scythe");
        addAlias(4084, "yo-yo", "yoyo");
        // === Bulk skilling supplies ===
        addAlias(1511, "logs", "regular logs", "reg logs");
        addAlias(1521, "oak logs", "oak");
        addAlias(1519, "willow logs", "willow", "wills");
        addAlias(1517, "maple logs", "maple", "maps");
        addAlias(1515, "yew logs", "yew", "yews");
        addAlias(1513, "magic logs", "magic log", "mage logs", "magics");
        addAlias(436, "copper ore", "copper");
        addAlias(438, "tin ore", "tin");
        addAlias(440, "iron ore", "iron");
        addAlias(453, "coal");
        addAlias(447, "mithril ore", "mith ore", "mith");
        addAlias(449, "adamantite ore", "addy ore", "addy", "adam ore");
        addAlias(451, "runite ore", "rune ore", "rune ores");
        addAlias(444, "gold ore");
        addAlias(317, "raw shrimps", "shrimps", "shrimp");
        addAlias(331, "raw salmon");
        addAlias(335, "raw trout");
        addAlias(371, "raw swordfish");
        addAlias(377, "raw lobster");
        addAlias(383, "raw shark");
        addAlias(379, "lobster", "lobs", "lobsters");
        addAlias(385, "shark", "sharks");
        addAlias(554, "fire runes", "fire rune", "fires");
        addAlias(555, "water runes", "water rune", "waters");
        addAlias(556, "air runes", "air rune", "airs");
        addAlias(557, "earth runes", "earth rune", "earths");
        addAlias(560, "death runes", "death rune", "deaths");
        addAlias(562, "chaos runes", "chaos rune", "chaos");
        addAlias(565, "blood runes", "blood rune", "bloods");
        addAlias(526, "bones");
        addAlias(532, "big bones");
        addAlias(536, "dragon bones", "d bones", "dbones");
        addAlias(207, "grimy ranarr");
        addAlias(219, "grimy torstol");
        addAlias(1623, "uncut sapphire");
        addAlias(1619, "uncut ruby");
        addAlias(1617, "uncut diamond");
        addAlias(11212, "dragon arrows", "d arrows");
        addAlias(892, "rune arrows");
    }

    private static void addAlias(int id, String... aliases) {
        for (String a : aliases) ALIASES.put(a.toLowerCase().trim(), id);
    }

    /** Patterns for buy/sell intent. The body match continues into an
     *  optional price token at the end. */
    private static final Pattern BUY_RE = Pattern.compile(
        "^(?:wtb|want to buy|buying|need|lf|looking for)\\s+(.+?)(?:\\s+for\\s+|\\s+at\\s+|\\s+@\\s*|\\s+)?([\\d.]+\\s*[kmb]?)?$"
    );
    private static final Pattern SELL_RE = Pattern.compile(
        "^(?:wts|want to sell|selling|got|sell|s)\\s+(.+?)(?:\\s+for\\s+|\\s+at\\s+|\\s+@\\s*|\\s+)?([\\d.]+\\s*[kmb]?)?$"
    );

    /** In-trade narrow: "i want 25 logs", "give me 25", "25 logs", "25 of yew". */
    private static final Pattern QTY_NARROW_RE = Pattern.compile(
        "(?:i want |give me |only |just )?(\\d+)\\s*(?:of\\s+)?(.+)?"
    );

    /**
     * Hook called from WorldPacketsDecoder for every real-player public chat.
     * Two paths:
     *   1. Player is in trade with a bot - parse for qty narrow.
     *   2. Player is NOT in trade - parse for WTB/WTS broadcast.
     */
    public static void onPublicChat(Player player, String message) {
        if (player == null || message == null) return;
        if (player instanceof AIPlayer) return; // ignore bot chat

        String lower = message.toLowerCase().trim();
        if (lower.length() < 3) return;

        // Path 1: in-trade with a bot
        try {
            if (player.getTrade() != null && player.getTrade().isTrading()) {
                Player tradeTarget = player.getTrade().getTarget();
                if (tradeTarget instanceof AIPlayer) {
                    AIPlayer bot = (AIPlayer) tradeTarget;
                    if (bot.getBrain() instanceof CitizenBrain) {
                        if (handleInTradeChat(bot, player, lower)) return;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Path 2: WTB/WTS broadcast
        try {
            TradeIntent intent = parseIntent(lower);
            if (intent == null) return;
            AIPlayer responder = findResponderBot(player, intent);
            if (responder == null) return;
            scheduleBotResponse(responder, player, intent);
        } catch (Throwable ignored) {}
    }

    /** Returns true if the chat was consumed as an in-trade adjustment. */
    private static boolean handleInTradeChat(AIPlayer bot, Player player, String lower) {
        // Special "all" or "max" means use full stock.
        if (lower.equals("all") || lower.equals("max") || lower.equals("everything")) {
            bot.getTemporaryAttributtes().put("BotTraderRequestedQty", Integer.MAX_VALUE);
            return true;
        }
        Matcher m = QTY_NARROW_RE.matcher(lower);
        if (!m.matches()) return false;
        try {
            int qty = Integer.parseInt(m.group(1));
            if (qty <= 0) return false;
            // If a body is present, optional sanity-check it matches the
            // bot's stock (so "25 fish" doesn't narrow a "logs" trader).
            // We don't enforce it - just stash the qty.
            bot.getTemporaryAttributtes().put("BotTraderRequestedQty", qty);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static TradeIntent parseIntent(String lower) {
        Matcher m = BUY_RE.matcher(lower);
        Intent dir;
        if (m.matches()) {
            dir = Intent.BUY;
        } else {
            m = SELL_RE.matcher(lower);
            if (!m.matches()) return null;
            dir = Intent.SELL;
        }
        String body = m.group(1).trim();
        String priceStr = m.group(2);

        // Drop a leading qty digit ("100 logs 50k" -> body "100 logs", qty 100).
        int qty = 1;
        Matcher qtyM = Pattern.compile("^(\\d+)\\s+(.+)").matcher(body);
        if (qtyM.matches()) {
            try {
                qty = Math.max(1, Integer.parseInt(qtyM.group(1)));
                body = qtyM.group(2).trim();
            } catch (Throwable ignored) {}
        }

        // Longest-match alias lookup so "blue partyhat" beats "blue".
        int matchedId = -1;
        String matchedAlias = null;
        for (Map.Entry<String, Integer> e : ALIASES.entrySet()) {
            String key = e.getKey();
            if (body.contains(key)) {
                if (matchedAlias == null || key.length() > matchedAlias.length()) {
                    matchedAlias = key;
                    matchedId = e.getValue();
                }
            }
        }
        if (matchedId == -1) return null;

        long price = parsePrice(priceStr);

        TradeIntent ti = new TradeIntent();
        ti.intent = dir;
        ti.itemId = matchedId;
        ti.itemName = matchedAlias;
        ti.price = price;
        ti.quantity = qty;
        return ti;
    }

    /** Parse "1m" / "100k" / "1.5m" / "1500" / "1b". Returns -1 if unparseable. */
    private static long parsePrice(String s) {
        if (s == null) return -1;
        s = s.toLowerCase().trim();
        if (s.isEmpty()) return -1;
        long mult = 1;
        if (s.endsWith("k"))      { mult = 1_000L;        s = s.substring(0, s.length()-1); }
        else if (s.endsWith("m")) { mult = 1_000_000L;    s = s.substring(0, s.length()-1); }
        else if (s.endsWith("b")) { mult = 1_000_000_000L; s = s.substring(0, s.length()-1); }
        try {
            double d = Double.parseDouble(s.trim());
            return (long) (d * mult);
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Find the closest available trader bot near the player. */
    private static AIPlayer findResponderBot(Player player, TradeIntent intent) {
        AIPlayer best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Player p : World.getPlayers()) {
            if (!(p instanceof AIPlayer)) continue;
            AIPlayer bot = (AIPlayer) p;
            if (bot.hasFinished()) continue;
            if (bot.getPlane() != player.getPlane()) continue;
            int dx = bot.getX() - player.getX();
            int dy = bot.getY() - player.getY();
            int sq = dx*dx + dy*dy;
            if (sq > CHAT_RESPONSE_RADIUS_SQ) continue;

            if (!(bot.getBrain() instanceof CitizenBrain)) continue;
            CitizenBrain cb = (CitizenBrain) bot.getBrain();
            if (cb.getArchetype() == null) continue;
            // Only traders respond to WTB/WTS. Bankstanders + gamblers don't.
            if (!cb.getArchetype().isTrader()) continue;
            // Already in trade? skip.
            try {
                if (bot.getTrade() != null && bot.getTrade().isTrading()) continue;
            } catch (Throwable ignored) {}
            // Cooldown: bot must not have responded recently.
            Long lastResp = (Long) bot.getTemporaryAttributtes().get("ChatRespondLastMs");
            if (lastResp != null && System.currentTimeMillis() - lastResp < CHAT_RESPONSE_COOLDOWN_MS) continue;

            if (sq < bestDist) {
                bestDist = sq;
                best = bot;
            }
        }
        return best;
    }

    /** Stash the intent on the bot for the trade handler tick to pick up. */
    private static void scheduleBotResponse(AIPlayer bot, Player player, TradeIntent intent) {
        bot.getTemporaryAttributtes().put("ChatRespondLastMs", System.currentTimeMillis());
        bot.getTemporaryAttributtes().put("ChatRespondPlayer", player);
        bot.getTemporaryAttributtes().put("ChatRespondIntent", intent);
        // Tiny pre-action delay so the bot doesn't INSTANTLY pop a trade
        // window the same tick the player chats - feels too robotic.
        bot.getTemporaryAttributtes().put("ChatRespondAfterMs",
            System.currentTimeMillis() + 1500 + (long)(Math.random() * 1500));
    }

    /** Whether the price is within tolerance of the catalog reference price.
     *  intent BUY = player wants to buy = bot sells. Player must offer
     *    >= (1 - PRICE_TOLERANCE) * catalogPrice.
     *  intent SELL = player wants to sell = bot buys. Player must ask
     *    <= (1 + PRICE_TOLERANCE) * catalogPrice. */
    public static boolean priceAcceptable(Intent intent, long playerPrice, int catalogPrice) {
        if (catalogPrice <= 0) return false;
        if (playerPrice <= 0) return true; // no price = use catalog
        double ratio = (double) playerPrice / catalogPrice;
        if (intent == Intent.BUY) return ratio >= (1.0 - PRICE_TOLERANCE);
        return ratio <= (1.0 + PRICE_TOLERANCE);
    }
}
