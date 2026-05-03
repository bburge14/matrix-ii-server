package com.rs.bot.ai;

import com.rs.bot.AIPlayer;
import com.rs.game.item.Item;
import com.rs.game.player.Skills;
import com.rs.game.player.actions.Fletching;
import com.rs.game.player.actions.HerbCleaning;
import com.rs.utils.Utils;

/**
 * Shared skill-action helpers that work for both Legend (BotBrain) and
 * Citizen (CitizenBrain) bots. Each method:
 *   - Self-stocks materials if the bot doesn't have them (no bank trip
 *     simulation yet - we just materialize a small starter batch)
 *   - Fires the real Action class, so XP gain + animations + drops are
 *     identical to a player doing the same skill
 *
 * "Real action" here means we call the same code path the
 * NPCHandler/ObjectHandler routes a player click through. No fake XP, no
 * simulated drops.
 */
public final class BotSkillActions {

    private BotSkillActions() {}

    // === Herblore: clean grimy herbs ===

    /** Clean the highest-tier grimy herb the bot can. Self-stocks 14 grimy
     *  herbs of the appropriate tier if inventory is empty. Returns true if
     *  an action was started, false on no-op. */
    public static boolean cleanHerbs(AIPlayer bot) {
        try {
            int hLvl = bot.getSkills().getLevel(Skills.HERBLORE);
            // Pick the highest-tier grimy the bot can clean (subject to inv).
            HerbCleaning.Herbs picked = pickHerbForLevel(hLvl);
            if (picked == null) return false;

            // Self-stock: give bot 14 of the herb if they have none.
            if (bot.getInventory().getAmountOf(picked.getHerbId()) <= 0) {
                int free = 28 - bot.getInventory().getItems().getUsedSlots();
                int give = Math.min(14, Math.max(1, free));
                if (give <= 0) return false;
                bot.getInventory().addItem(picked.getHerbId(), give);
            }

            // Find the slot of the first matching grimy herb + clean it.
            for (int i = 0; i < bot.getInventory().getItemsContainerSize(); i++) {
                Item it = bot.getInventory().getItem(i);
                if (it == null || it.getId() != picked.getHerbId()) continue;
                HerbCleaning.clean(bot, it, i);
                return true;
            }
        } catch (Throwable t) {
            // Fall through: no-op, caller logs.
        }
        return false;
    }

    private static HerbCleaning.Herbs pickHerbForLevel(int hLvl) {
        HerbCleaning.Herbs best = null;
        for (HerbCleaning.Herbs h : HerbCleaning.Herbs.values()) {
            if (h.getLevel() > hLvl) continue;
            if (best == null || h.getLevel() > best.getLevel()) best = h;
        }
        return best;
    }

    // === Fletching: cut tier-appropriate shortbow from logs ===

    /** Cut a shortbow from the highest-tier log the bot has Fletching for.
     *  Self-stocks 14 logs + a knife if needed. */
    public static boolean fletchBow(AIPlayer bot) {
        try {
            int fLvl = bot.getSkills().getLevel(Skills.FLETCHING);
            FletchTier tier = pickFletchTierForLevel(fLvl);
            if (tier == null) return false;

            // Self-stock knife (946) and 14 logs.
            if (bot.getInventory().getAmountOf(946) <= 0) {
                bot.getInventory().addItem(946, 1);
            }
            if (bot.getInventory().getAmountOf(tier.logId) <= 0) {
                int free = 28 - bot.getInventory().getItems().getUsedSlots();
                int give = Math.min(14, Math.max(1, free));
                if (give <= 0) return false;
                bot.getInventory().addItem(tier.logId, give);
            }

            int qty = bot.getInventory().getAmountOf(tier.logId);
            // option 0 in each Fletch enum is the first variant in its array
            // - shortbow (u) for the bow tiers. setAction starts the timed
            // cutting loop (real action, real XP, real animations).
            bot.getActionManager().setAction(new Fletching(tier.fletch, 0, qty));
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    /** Tiered fletch progression. Picks the highest one the bot's level
     *  permits and we have a known Fletch enum entry for. */
    private static FletchTier pickFletchTierForLevel(int fLvl) {
        FletchTier[] tiers = new FletchTier[] {
            new FletchTier(1, 1511, Fletching.Fletch.REGULAR_BOW),    // normal logs
            new FletchTier(20, 1521, Fletching.Fletch.OAK_BOW),       // oak logs
        };
        // Try willow/maple/yew/magic if those Fletch enum entries exist by
        // name. Reflection-safe to handle missing variants on this server.
        FletchTier[] later = tryHigherTiers();
        FletchTier[] all = new FletchTier[tiers.length + later.length];
        System.arraycopy(tiers, 0, all, 0, tiers.length);
        System.arraycopy(later, 0, all, tiers.length, later.length);

        FletchTier best = null;
        for (FletchTier t : all) {
            if (t == null) continue;
            if (fLvl < t.minLevel) continue;
            if (best == null || t.minLevel > best.minLevel) best = t;
        }
        return best;
    }

    private static FletchTier[] tryHigherTiers() {
        java.util.List<FletchTier> out = new java.util.ArrayList<>();
        addIfPresent(out, "WILLOW_BOW",  35, 1519);
        addIfPresent(out, "MAPLE_BOW",   45, 1517);
        addIfPresent(out, "YEW_BOW",     55, 1515);
        addIfPresent(out, "MAGIC_BOW",   75, 1513);
        return out.toArray(new FletchTier[0]);
    }

    private static void addIfPresent(java.util.List<FletchTier> out, String enumName, int lvl, int logId) {
        try {
            Fletching.Fletch f = Fletching.Fletch.valueOf(enumName);
            out.add(new FletchTier(lvl, logId, f));
        } catch (Throwable ignored) {}
    }

    private static final class FletchTier {
        final int minLevel, logId;
        final Fletching.Fletch fletch;
        FletchTier(int minLevel, int logId, Fletching.Fletch fletch) {
            this.minLevel = minLevel; this.logId = logId; this.fletch = fletch;
        }
    }

    // === Auto-XP for fully-stub skills (Construction / Dungeoneering) ===

    /** Add a small chunk of XP to represent the bot doing the activity
     *  off-screen. Used for skills we deliberately don't action-wire because
     *  the activity (POH building / dungeon runs) needs real-player coupling.
     *  Caller throttles - typically once per few ticks at the target tile. */
    public static void autoXp(AIPlayer bot, int skill, double xp) {
        try {
            // Avoid hammering: small jitter so multiple bots don't tick in
            // lockstep.
            if (Utils.random(5) != 0) return;
            bot.getSkills().addXp(skill, xp);
        } catch (Throwable ignored) {}
    }
}
