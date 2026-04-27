package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.cache.loaders.ItemDefinitions;
import com.rs.game.item.Item;
import com.rs.game.player.Inventory;
import com.rs.game.player.content.Shop;
import com.rs.game.player.content.grandExchange.GrandExchange;
import com.rs.game.player.content.grandExchange.Offer;
import com.rs.utils.ShopsHandler;

/**
 * Bot-side helpers for trading on the Grand Exchange and at NPC shops.
 *
 * Both APIs are headless-bot-safe: GrandExchange.sendOffer / Shop.buy /
 * Shop.sell don't gate on UI dialogues - they read inventory + bank +
 * money pouch directly and update them in place. Packet sends like
 * sendGameMessage drop into the MockChannel and disappear.
 *
 * Why this matters: bots produce items (logs, ore, fish) faster than
 * they can use them. Without a way to sell, the bank fills with raw
 * material and wealth goals never complete. With this layer:
 *   - Banking trip places GE sell offers for tradeable resources before
 *     depositing leftovers (so coin actually flows into the bank).
 *   - Periodic collection sweeps completed offers back into inventory.
 *   - Equipment goals can later route through Shop.buy to actually
 *     acquire armor/weapons rather than perpetually grinding.
 */
public final class BotTrading {

    private BotTrading() {}

    /**
     * Resource items bots produce in bulk and that should be sold rather
     * than dumped into the bank. Item IDs cribbed from
     * Woodcutting.TreeDefinitions, Mining.RockDefinitions, and Fishing.Fish.
     * Add more as we wire more skilling methods.
     */
    public static final int[] TRADEABLE_RESOURCES = {
        // Logs
        1511, 1521, 1519, 1517, 1515, 1513,
        // Ores
        434, 436, 438, 440, 442, 447, 449, 451, 444,
        // Raw fish
        317, 321, 331, 359, 377, 371, 383, 7944, 7946, 7948,
    };

    private static boolean isTradeableResource(int itemId) {
        for (int id : TRADEABLE_RESOURCES) if (id == itemId) return true;
        return false;
    }

    // ===== Grand Exchange =====

    /**
     * Place GE sell offers for tradeable resource items in the bot's
     * inventory. Uses GrandExchange.getPrice() for the listing price - so
     * the bot's listing prices reflect the actual market.
     *
     * Returns the number of offers placed. Caller can then deposit any
     * remaining inventory (non-tradeable items, leftovers when GE slots
     * are full).
     */
    public static int sellInventoryOnGE(AIPlayer bot) {
        Inventory inv = bot.getInventory();
        if (inv == null) return 0;
        int placed = 0;
        // 6 GE slots in standard 718.
        for (int slot = 0; slot < 6 && placed < 6; slot++) {
            // Find a tradeable inventory item we haven't already listed.
            int invSlot = findTradeableInventorySlot(inv);
            if (invSlot < 0) break;
            Item it = inv.getItem(invSlot);
            if (it == null) break;
            // Only proceed if THIS GE slot is empty.
            Offer existing = GrandExchange.getOffer(bot, slot);
            if (existing != null) continue;
            int price = Math.max(1, GrandExchange.getPrice(it.getId()));
            try {
                GrandExchange.sendOffer(bot, slot, it.getId(), it.getAmount(), price, false);
                placed++;
            } catch (Throwable t) {
                // Defensive - GE backends sometimes have UI hooks we don't expect.
                System.err.println("[BotTrading] sellInventoryOnGE failed for "
                    + bot.getDisplayName() + " item=" + it.getId() + ": " + t);
                break;
            }
        }
        return placed;
    }

    private static int findTradeableInventorySlot(Inventory inv) {
        for (int s = 0; s < 28; s++) {
            Item it = inv.getItem(s);
            if (it == null) continue;
            if (isTradeableResource(it.getId())) return s;
        }
        return -1;
    }

    /**
     * Sweep completed offers - any settled GE trade gets pulled back into
     * the bot's inventory (or bank if inventory is full). Cheap to call
     * frequently; no-op if no offer is completed.
     *
     * Returns count of offers collected.
     */
    public static int collectCompletedOffers(AIPlayer bot) {
        int collected = 0;
        for (int slot = 0; slot < 6; slot++) {
            try {
                Offer offer = GrandExchange.getOffer(bot, slot);
                if (offer == null) continue;
                // hasItemsWaiting is the public-API flag for "stuff to
                // collect" - includes both fully-completed offers and
                // partial fills with received coins/items.
                if (!offer.hasItemsWaiting()) continue;
                GrandExchange.collectItems(bot, slot, 0, 0);
                GrandExchange.collectItems(bot, slot, 1, 0);
                collected++;
            } catch (Throwable t) {
                // skip slot, keep trying others
            }
        }
        return collected;
    }

    /**
     * Place a buy offer for an item the bot wants. Uses GE price * 1.05
     * to encourage instant matching against existing sellers. Returns
     * true if the offer placed (slot was free + bot had coins).
     */
    public static boolean placeBuyOffer(AIPlayer bot, int itemId, int qty) {
        if (qty <= 0) return false;
        for (int slot = 0; slot < 6; slot++) {
            Offer existing = GrandExchange.getOffer(bot, slot);
            if (existing != null) continue;
            int price = (int) Math.ceil(Math.max(1, GrandExchange.getPrice(itemId)) * 1.05);
            try {
                GrandExchange.sendOffer(bot, slot, itemId, qty, price, true);
                return true;
            } catch (Throwable t) {
                System.err.println("[BotTrading] placeBuyOffer failed for "
                    + bot.getDisplayName() + " item=" + itemId + ": " + t);
                return false;
            }
        }
        return false;
    }

    // ===== Shop interaction =====

    /**
     * Direct shop buy. Looks up the shop, finds the slot of the requested
     * item, and calls Shop.buy() - no UI/dialogue needed. Returns true if
     * the buy went through.
     */
    public static boolean buyFromShop(AIPlayer bot, int shopKey, int itemId, int qty) {
        try {
            Shop shop = ShopsHandler.getShop(shopKey);
            if (shop == null) return false;
            int slot = findShopSlot(shop, itemId);
            if (slot < 0) return false;
            shop.buy(bot, slot, qty);
            return true;
        } catch (Throwable t) {
            System.err.println("[BotTrading] buyFromShop failed for "
                + bot.getDisplayName() + " shop=" + shopKey + " item=" + itemId + ": " + t);
            return false;
        }
    }

    /**
     * Direct shop sell. Sells inventory slot to the given shop. Useful
     * when GE doesn't have buyers for an item but a general store will.
     */
    public static boolean sellToShop(AIPlayer bot, int shopKey, int invSlot, int qty) {
        try {
            Shop shop = ShopsHandler.getShop(shopKey);
            if (shop == null) return false;
            shop.sell(bot, invSlot, qty);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Locate an item's slot in a shop's stock. The shop API only takes
     * slot indices; without an accessor we have to iterate via reflection
     * once. The result could be cached but stocks are small (< 40 items).
     */
    private static int findShopSlot(Shop shop, int itemId) {
        try {
            java.lang.reflect.Field f = Shop.class.getDeclaredField("mainStock");
            f.setAccessible(true);
            Item[] stock = (Item[]) f.get(shop);
            if (stock != null) {
                for (int i = 0; i < stock.length; i++) {
                    if (stock[i] != null && stock[i].getId() == itemId) return i;
                }
            }
        } catch (Throwable ignore) {}
        return -1;
    }

    // ===== Misc =====

    /** Estimated coin value of an item - GE price first, else item def value. */
    public static int estimateValue(int itemId) {
        int gePrice = GrandExchange.getPrice(itemId);
        if (gePrice > 0) return gePrice;
        try {
            ItemDefinitions def = ItemDefinitions.getItemDefinitions(itemId);
            if (def != null) return Math.max(1, def.getValue());
        } catch (Throwable ignore) {}
        return 1;
    }
}
