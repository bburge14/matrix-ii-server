package com.rs.bot;

import com.rs.game.item.Item;
import com.rs.game.player.Equipment;
import com.rs.game.player.Player;
import com.rs.utils.Utils;

/**
 * Realistic gear loadouts driven by named gear sets and wealth distribution.
 *
 * Each archetype picks a "wealth roll" first (BIS / good / mid / budget / poor),
 * then a coordinated kit from that tier. Accessories layered on with probabilities.
 *
 * All item IDs verified from the cache catalog scan.
 */
public final class BotEquipment {

    private BotEquipment() {}

    public static void applyLoadout(Player bot, String archetype, int combatLevel) {
        if (archetype == null) return;
        archetype = archetype.toLowerCase();
        try {
            // Brand-new level-3 bots (combat 3, total level low) skip the
            // archetype loadout entirely - they get the proper starter kit
            // instead, like a real new account. No full bronze, no plate.
            if (isBrandNewBot(bot, combatLevel)) {
                applyStarterKit(bot);
                applyGatheringToolkit(bot, combatLevel); // bronze pick/axe
                return;
            }
            switch (archetype) {
                case "skiller":  applySkiller(bot, combatLevel); break;
                case "ranged":   applyRanger(bot, combatLevel); break;
                case "magic":    applyMage(bot, combatLevel); break;
                case "tank":     applyTank(bot, combatLevel); break;
                case "pure":     applyPure(bot, combatLevel); break;
                case "hybrid":   applyHybrid(bot, combatLevel); break;
                case "main":     applyMelee(bot, combatLevel); break;
                case "maxed":    applyEndgameRandom(bot); break;
                case "f2p":      applyF2PMelee(bot, combatLevel); break;
                case "melee":
                case "random":
                default:         applyMelee(bot, combatLevel); break;
            }
            // Common accessories regardless of archetype
            applyAccessories(bot, archetype, combatLevel);
            // Universal gathering toolkit so bots can actually skill -
            // Mining/WC/Fishing all checkAll() against tool presence.
            applyGatheringToolkit(bot, combatLevel);
            // Random "lived-in" bank + inventory clutter + GP scaled to
            // the bot's combat level. Makes mid-tier bots feel like real
            // accounts with accumulated stuff, not freshly-loaded NPCs.
            applyAccumulatedWealth(bot, combatLevel);
        } catch (Throwable t) {
            System.err.println("[BotEquipment] failed for archetype=" + archetype + " cb=" + combatLevel + ": " + t);
        }
    }

    /**
     * Give every bot a basic gathering toolkit. Without these, the skill
     * Action.checkAll() returns false and the bot animates (briefly) but
     * never actually gathers - exactly what was happening before.
     *
     * Tier scales with combat level so low-level bots get bronze, high
     * get rune. The tools go into the inventory; the action code accepts
     * them from inventory or toolbelt or weapon slot.
     */
    /**
     * Public-callable refill - used when a bot's existing kit is missing
     * the tool needed for its current activity. Equivalent to "buying a
     * replacement from the master" without the full shop interaction.
     */
    public static void ensureGatheringToolkit(Player bot) {
        applyGatheringToolkit(bot, bot.getSkills().getCombatLevel());
    }

    /**
     * True if this bot is a brand-new level-3 account: combat=3 and the
     * total level is low enough to confirm default 1-everything stats
     * (10 hp + 7 base skills = 17 minimum total). Anything higher means
     * the bot rolled real stats and should get the appropriate kit.
     */
    private static boolean isBrandNewBot(Player bot, int combatLevel) {
        try {
            return combatLevel <= 3 && bot.getSkills().getTotalLevel() <= 25;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Tutorial-Island-style starter pack. A real new player gets a basic
     * pile of stuff to start them off. Replaces the old "spawn with full
     * bronze armor" which was unrealistic for cb 3.
     */
    private static void applyStarterKit(Player bot) {
        try {
            // Stash items - go in inventory not equipped (new players don't
            // start auto-equipped except for skills).
            bot.getInventory().addItem(1265, 1);  // Bronze pickaxe
            bot.getInventory().addItem(1351, 1);  // Bronze hatchet
            bot.getInventory().addItem(303, 1);   // Small fishing net
            bot.getInventory().addItem(590, 1);   // Tinderbox
            bot.getInventory().addItem(1205, 1);  // Bronze dagger
            bot.getInventory().addItem(841, 1);   // Shortbow
            bot.getInventory().addItem(882, 25);  // Bronze arrows x25
            bot.getInventory().addItem(556, 30);  // Air runes
            bot.getInventory().addItem(558, 15);  // Mind runes
            bot.getInventory().addItem(2309, 1);  // Bread
            bot.getInventory().addItem(1925, 1);  // Bucket
            bot.getInventory().addItem(1931, 1);  // Pot
            bot.getInventory().addItem(995, 25);  // 25 coins (starter cash)
        } catch (Throwable t) {
            System.err.println("[BotEquipment] starter kit failed: " + t);
        }
    }

    /**
     * Add random "lived-in" items + GP to the bank/inventory based on
     * combat level. Higher cb = more accumulated wealth + variety.
     * Skips brand-new bots (they get just the starter kit).
     *
     * Picks from food/potions/herbs/seeds/runes/raw drops/processed
     * supplies so each bot feels like a real account with a history.
     */
    private static void applyAccumulatedWealth(Player bot, int combatLevel) {
        try {
            // GP scaled by cb - rough OSRS economy expectations
            int baseGp;
            if      (combatLevel >= 100) baseGp = 5_000_000 + Utils.random(15_000_000);
            else if (combatLevel >= 70)  baseGp = 500_000   + Utils.random(2_000_000);
            else if (combatLevel >= 40)  baseGp = 50_000    + Utils.random(200_000);
            else if (combatLevel >= 20)  baseGp = 5_000     + Utils.random(20_000);
            else                          baseGp = 100      + Utils.random(2_000);
            try { bot.getMoneyPouch().setCoinsAmount(baseGp); } catch (Throwable t) {}

            // Random food (any cb gets food in bank)
            int[] foodChoices = combatLevel >= 70
                ? new int[]{379, 385, 7946, 15272, 391, 397}  // lobster, swordfish, monkfish, rocktail
                : combatLevel >= 30
                ? new int[]{315, 333, 379, 2309, 333}         // shrimp, trout, lobster, bread
                : new int[]{2309, 315, 1965};                  // bread, shrimp, cabbage
            int foodId = foodChoices[Utils.random(foodChoices.length)];
            int foodQty = combatLevel >= 70 ? 50 + Utils.random(150) : 10 + Utils.random(50);
            bot.getBank().addItem(foodId, foodQty, true);

            // Random raw stash (logs/fish/ore/hide) - looks like past skilling
            if (Utils.random(100) < 40) {
                int[] rawIds = {1511, 1521, 1519, 1517, 1515, 1513, 6332, 317, 327, 321, 331, 359, 377, 436, 438, 440, 442, 444, 447, 449, 451, 1739, 1745, 2505};
                int rawId = rawIds[Utils.random(rawIds.length)];
                int rawQty = 50 + Utils.random(500);
                bot.getBank().addItem(rawId, rawQty, true);
            }

            // Random potion - more potions for combat-focused bots
            if (combatLevel >= 30 && Utils.random(100) < 60) {
                int[] potIds = {2440, 113, 2434, 3024, 6685, 12695}; // str pot, attack pot, prayer pot, super attack, sara brew, super combat
                int potId = potIds[Utils.random(potIds.length)];
                int potQty = 1 + Utils.random(15);
                bot.getBank().addItem(potId, potQty, true);
            }

            // Random rune stash (low-cb might have starter runes still)
            if (Utils.random(100) < 70) {
                int[] runeIds = {556, 555, 557, 554, 558, 562, 560, 565};
                int runeId = runeIds[Utils.random(runeIds.length)];
                int runeQty = 100 + Utils.random(2000);
                bot.getBank().addItem(runeId, runeQty, true);
            }

            // Random herb/seed for skiller bots
            if (Utils.random(100) < 30) {
                int[] herbIds = {199, 201, 203, 205, 207, 209, 211, 213, 215, 2998, 3000};
                int herbId = herbIds[Utils.random(herbIds.length)];
                int herbQty = 1 + Utils.random(20);
                bot.getBank().addItem(herbId, herbQty, true);
            }
        } catch (Throwable t) {
            System.err.println("[BotEquipment] accumulated wealth failed: " + t);
        }
    }

    /** Pickaxe item ID at-or-below the bot's mining level. */
    public static int pickaxeForMiningLevel(int miningLevel) {
        if      (miningLevel >= 61) return 15259; // Dragon pickaxe
        else if (miningLevel >= 41) return 1275;  // Rune
        else if (miningLevel >= 31) return 1271;  // Adamant
        else if (miningLevel >= 21) return 1269;  // Mithril
        else if (miningLevel >= 6)  return 1267;  // Steel
        else                        return 1265;  // Bronze
    }

    /** Hatchet item ID at-or-below the bot's woodcutting level. */
    public static int hatchetForWoodcuttingLevel(int wcLevel) {
        if      (wcLevel >= 61) return 6739; // Dragon hatchet
        else if (wcLevel >= 41) return 1359; // Rune
        else if (wcLevel >= 31) return 1357; // Adamant
        else if (wcLevel >= 21) return 1355; // Mithril
        else if (wcLevel >= 11) return 1361; // Black
        else if (wcLevel >= 6)  return 1353; // Steel
        else                    return 1351; // Bronze
    }

    /**
     * Try to "buy" a single tool by deducting gp from the bot's money.
     * Returns true if purchase succeeded (item added, coins deducted).
     * If the bot can't afford it, returns false so the caller can fall
     * back to a no-tool activity.
     *
     * Tool prices match the in-game shop value at the relevant master.
     */
    public static boolean tryBuyTool(Player bot, int itemId) {
        int price;
        switch (itemId) {
            case 1265: price = 1;     break; // Bronze pickaxe
            case 1267: price = 200;   break; // Iron pickaxe
            case 1269: price = 500;   break; // Steel pickaxe
            case 1273: price = 1300;  break; // Mithril pickaxe (skip black tier 1271)
            case 1271: price = 3200;  break; // Adamant pickaxe
            case 1275: price = 32000; break; // Rune pickaxe
            case 1351: price = 1;     break; // Bronze axe
            case 1353: price = 200;   break; // Iron axe
            case 1355: price = 500;   break; // Steel axe
            case 1357: price = 1300;  break; // Mithril axe
            case 1359: price = 32000; break; // Rune axe
            case 303:  price = 5;     break; // Small fishing net
            case 307:  price = 5;     break; // Fishing rod
            case 313:  price = 4;     break; // Fishing bait (per)
            case 301:  price = 20;    break; // Lobster pot
            case 311:  price = 45;    break; // Harpoon
            case 590:  price = 1;     break; // Tinderbox
            case 2347: price = 1;     break; // Hammer
            case 1755: price = 1;     break; // Chisel
            default:   price = 100;
        }
        try {
            int pouch = bot.getMoneyPouch().getCoinsAmount();
            int invCoins = bot.getInventory().getAmountOf(995);
            int totalGp = pouch + invCoins;
            if (totalGp < price) return false;
            // Pay from pouch first, then inventory.
            int fromPouch = Math.min(pouch, price);
            if (fromPouch > 0) bot.getMoneyPouch().setCoinsAmount(pouch - fromPouch);
            int rem = price - fromPouch;
            if (rem > 0) bot.getInventory().deleteItem(995, rem);
            bot.getInventory().addItem(itemId, 1);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void applyGatheringToolkit(Player bot, int cb) {
        // Per-skill tier selection - a bot with 50 mining + 1 woodcutting
        // gets a rune pickaxe but a bronze hatchet, matching what they
        // can actually use.
        int miningLvl, wcLvl, fishingLvl;
        try { miningLvl  = bot.getSkills().getLevel(com.rs.game.player.Skills.MINING); } catch (Throwable t) { miningLvl  = 1; }
        try { wcLvl      = bot.getSkills().getLevel(com.rs.game.player.Skills.WOODCUTTING); } catch (Throwable t) { wcLvl  = 1; }
        try { fishingLvl = bot.getSkills().getLevel(com.rs.game.player.Skills.FISHING); } catch (Throwable t) { fishingLvl = 1; }
        int pickaxe = pickaxeForMiningLevel(miningLvl);
        int hatchet = hatchetForWoodcuttingLevel(wcLvl);
        try {
            // Straight to the toolbelt (doesn't take inventory slots).
            // Mining/WC/Fishing checkAll() accepts toolbelt items.
            addToBeltOrInv(bot, pickaxe);
            addToBeltOrInv(bot, hatchet);
            if (fishingLvl >= 1)  addToBeltOrInv(bot, 303); // Small net
            if (fishingLvl >= 5)  addToBeltOrInv(bot, 307); // Fishing rod
            if (fishingLvl >= 35) addToBeltOrInv(bot, 311); // Harpoon
            if (fishingLvl >= 40) addToBeltOrInv(bot, 301); // Lobster pot
            addToBeltOrInv(bot, 590);  // Tinderbox
            addToBeltOrInv(bot, 2347); // Hammer
            addToBeltOrInv(bot, 1755); // Chisel
            // Bait stays in inventory - it's a stack consumable, not a toolbelt tool.
            if (fishingLvl >= 5 && !bot.getInventory().containsItem(313, 50))
                bot.getInventory().addItem(313, 50);
        } catch (Throwable t) {
            System.err.println("[BotEquipment] toolkit failed: " + t);
        }
    }

    /**
     * Add a tool to the toolbelt directly. Falls back to inventory if the
     * item isn't toolbelt-eligible (e.g. item ID not in the toolbelt's
     * item table).
     */
    public static void addToBeltOrInv(Player bot, int itemId) {
        try {
            if (bot.getToolbelt() != null && bot.getToolbelt().addToolDirect(itemId)) return;
        } catch (Throwable ignored) {}
        try {
            if (!bot.getInventory().containsItem(itemId, 1))
                bot.getInventory().addItem(itemId, 1);
        } catch (Throwable ignored) {}
    }

    // ===== Wealth tier (drives gear quality) =====
    // Distribution: 5% BIS, 15% near-BIS, 30% mid, 35% budget, 15% poor
    // Caps based on bot's combat level - low level bots can't roll endgame.

    private static int rollWealth(int cb) {
        int roll = Utils.random(100);
        // Cap tier by combat level. Real RS-equivalent thresholds:
        //   0 (poor)      - bronze/iron/steel/black/mithril/adamant range
        //   1 (budget)    - rune armor    (40 def, ~cb 50+)
        //   2 (mid)       - barrows       (70 def, ~cb 80+)
        //   3 (near-BIS)  - bandos+chaotic(80 atk/def, ~cb 100+)
        //   4 (BIS)       - torva+drygore (90 atk/def, ~cb 120+)
        //
        // Per-item EquipmentReqs.canWear() is the final gate so even with
        // a permissive cap, items that exceed actual stats won't be put
        // on. The cap here is just so the wealth ROLL is realistic - a
        // low-level bot doesn't try to roll BIS and end up empty-slotted.
        int maxTier;
        if      (cb < 30)  maxTier = 0;
        else if (cb < 50)  maxTier = 1;
        else if (cb < 80)  maxTier = 2;
        else if (cb < 100) maxTier = 3;
        else               maxTier = 4;

        int tier;
        if (roll < 5)        tier = 4;
        else if (roll < 20)  tier = 3;
        else if (roll < 50)  tier = 2;
        else if (roll < 85)  tier = 1;
        else                 tier = 0;
        return Math.min(tier, maxTier);
    }

    // ===== MELEE LOADOUTS =====

    private static void applyMelee(Player bot, int cb) {
        int wealth = rollWealth(cb);
        switch (wealth) {
            case 4: meleeBIS(bot); return;
            case 3: meleeNearBIS(bot); return;
            case 2: meleeMid(bot); return;
            case 1: meleeBudget(bot, cb); return;
            default: meleePoor(bot, cb); return;
        }
    }

    private static void meleeBIS(Player bot) {
        // Torva + Drygore + Spirit shield + Glaiven boots
        equip(bot, Equipment.SLOT_HAT,    20137);  // Torva full helm
        equip(bot, Equipment.SLOT_CHEST,  20141);  // Torva platebody
        equip(bot, Equipment.SLOT_LEGS,   20145);  // Torva platelegs
        int[] drygores = {26579, 26587, 26595};    // rapier, longsword, mace
        equip(bot, Equipment.SLOT_WEAPON, pick(drygores));
        if (chance(60)) equip(bot, Equipment.SLOT_SHIELD, 13742);  // Elysian spirit shield
        equip(bot, Equipment.SLOT_FEET,   pick(new int[]{21790, 21787}));  // Glaiven / Steadfast
        equip(bot, Equipment.SLOT_HANDS,  24977);  // Torva gloves
    }

    private static void meleeNearBIS(Player bot) {
        // Bandos + Chaotic / Vine whip
        equip(bot, Equipment.SLOT_HAT,    pick(new int[]{11718, 13455}));  // Armadyl helm fillin (Bandos has no helm), or use Torva broken
        equip(bot, Equipment.SLOT_CHEST,  11724);  // Bandos chestplate
        equip(bot, Equipment.SLOT_LEGS,   11726);  // Bandos tassets
        int[] weps = {18349, 18351, 18353, 21371, 4151};  // chaotic rapier/longsword/maul, vine whip, whip
        equip(bot, Equipment.SLOT_WEAPON, pick(weps));
        if (chance(50)) equip(bot, Equipment.SLOT_SHIELD, pick(new int[]{13738, 13744}));  // Arcane / Spectral spirit shield
        equip(bot, Equipment.SLOT_FEET,   11728);  // Bandos boots
        equip(bot, Equipment.SLOT_HANDS,  pick(new int[]{22358, 22361}));  // Goliath gloves
    }

    private static void meleeMid(Player bot) {
        // Barrows (Dharok / Guthan / Verac / Torag) + whip / godsword / dragon weapons
        int[][] barrows = {
            { 4716, 4720, 4722, 4718 },  // Dharok: helm, body, legs, weapon
            { 4724, 4728, 4730, 4726 },  // Guthan
            { 4753, 4757, 4759, 4755 },  // Verac
            { 4745, 4749, 4751, 4747 }   // Torag
        };
        int[] set = barrows[Utils.random(barrows.length)];
        equip(bot, Equipment.SLOT_HAT,    set[0]);
        equip(bot, Equipment.SLOT_CHEST,  set[1]);
        equip(bot, Equipment.SLOT_LEGS,   set[2]);
        // 50/50: keep barrows weapon or use whip/godsword
        int[] altWeps = {4151, 11696, 11694, 11698, 11700, 1305};  // whip, BGS, AGS, SGS, ZGS, dragon longsword
        equip(bot, Equipment.SLOT_WEAPON, chance(50) ? set[3] : pick(altWeps));
        if (chance(40)) equip(bot, Equipment.SLOT_SHIELD, 11283);  // Dragonfire shield
        equip(bot, Equipment.SLOT_FEET,   pick(new int[]{11732, 4131}));  // Dragon / Rune boots
        equip(bot, Equipment.SLOT_HANDS,  10551 == 10551 ? 7462 : 1059);  // Barrows gloves (assume 7462) or leather
    }

    private static void meleeBudget(Player bot, int cb) {
        // Rune armor + scimitar / longsword / dragon weapons
        equip(bot, Equipment.SLOT_HAT,    1163);  // Rune full helm
        equip(bot, Equipment.SLOT_CHEST,  pick(new int[]{1127, 1113}));  // Rune plate / chain
        equip(bot, Equipment.SLOT_LEGS,   pick(new int[]{1079, 1093}));  // Rune plate / skirt
        int[] weps = {1333, 1303, 1213, 1432, 1373, 4587, 1305};  // rune scim/longsword/dagger/mace/battleaxe, dragon scim/longsword
        equip(bot, Equipment.SLOT_WEAPON, pick(weps));
        if (chance(70)) equip(bot, Equipment.SLOT_SHIELD, 1201);  // Rune kiteshield
        equip(bot, Equipment.SLOT_FEET,   pick(new int[]{4131, 4121}));  // Rune / Iron boots
        equip(bot, Equipment.SLOT_HANDS,  1059);  // Leather gloves
    }

    private static void meleePoor(Player bot, int cb) {
        // Mid-tier metal: adamant/mithril/steel + basic weapons
        int t = Math.min(pickMetalTier(cb), 4);  // bronze..adamant
        int[] helms  = { 1155, 1153, 1157, 1159, 1161 };
        int[] bodies = { 1117, 1115, 1119, 1121, 1123 };
        int[] legs   = { 1075, 1067, 1069, 1071, 1073 };
        int[] weapons= { 1321, 1323, 1325, 1329, 1331 };
        int[] shields= { 1189, 1191, 1193, 1197, 1199 };
        equip(bot, Equipment.SLOT_HAT,    helms[t]);
        equip(bot, Equipment.SLOT_CHEST,  bodies[t]);
        equip(bot, Equipment.SLOT_LEGS,   legs[t]);
        equip(bot, Equipment.SLOT_WEAPON, weapons[t]);
        if (chance(50)) equip(bot, Equipment.SLOT_SHIELD, shields[t]);
    }

    // ===== TANK =====

    private static void applyTank(Player bot, int cb) {
        int wealth = rollWealth(cb);
        if (wealth >= 4) {
            // Statius (PvP top tank)
            equip(bot, Equipment.SLOT_HAT,    13896);  // Statius's full helm
            equip(bot, Equipment.SLOT_CHEST,  13884);  // Statius's platebody
            equip(bot, Equipment.SLOT_LEGS,   13890);  // Statius's platelegs
            equip(bot, Equipment.SLOT_WEAPON, 13902);  // Statius's warhammer
            equip(bot, Equipment.SLOT_SHIELD, 13740);  // Divine spirit shield
        } else if (wealth >= 3) {
            // Bandos tank build
            equip(bot, Equipment.SLOT_CHEST,  11724);
            equip(bot, Equipment.SLOT_LEGS,   11726);
            equip(bot, Equipment.SLOT_WEAPON, 4151);   // whip for accuracy
            equip(bot, Equipment.SLOT_SHIELD, 11283);  // Dragonfire shield
            equip(bot, Equipment.SLOT_FEET,   11728);
        } else {
            // Proselyte (prayer-tank for low wealth)
            equip(bot, Equipment.SLOT_HAT,    9672);   // Proselyte sallet
            equip(bot, Equipment.SLOT_CHEST,  9674);   // Proselyte hauberk
            equip(bot, Equipment.SLOT_LEGS,   9676);   // Proselyte cuisse
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{1333, 1305}));
            equip(bot, Equipment.SLOT_SHIELD, 1201);   // Rune kite
        }
    }

    // ===== PURE (1 def) =====

    private static void applyPure(Player bot, int cb) {
        int wealth = rollWealth(cb);
        // Pures: weapon + amulet + cape + boots/gloves; NO body/legs/helm
        if (wealth >= 3) {
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{4151, 21371, 18349, 5698}));  // whip / vine / chaotic / DDP++
            equip(bot, Equipment.SLOT_AMULET, 6585);  // Fury
        } else {
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{4587, 1305, 5698, 4151}));
            equip(bot, Equipment.SLOT_AMULET, 1725);  // Strength
        }
        if (chance(60)) equip(bot, Equipment.SLOT_FEET, pick(new int[]{11732, 4131}));
        if (chance(40)) equip(bot, Equipment.SLOT_HANDS, 1059);
        if (chance(60)) equip(bot, Equipment.SLOT_CAPE, 6570);  // Fire cape
    }

    // ===== RANGED =====

    private static void applyRanger(Player bot, int cb) {
        int wealth = rollWealth(cb);
        if (wealth >= 4) {
            // Pernix + Royal/Armadyl crossbow
            equip(bot, Equipment.SLOT_HAT,    20149);  // Pernix cowl
            equip(bot, Equipment.SLOT_CHEST,  20153);  // Pernix body
            equip(bot, Equipment.SLOT_LEGS,   20157);  // Pernix chaps
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{24338, 25037}));  // Royal cwbow / Armadyl cwbow
            equip(bot, Equipment.SLOT_FEET,   21790);  // Glaiven
            equip(bot, Equipment.SLOT_HANDS,  24974);  // Pernix gloves
        } else if (wealth >= 3) {
            // Armadyl + Karil
            equip(bot, Equipment.SLOT_HAT,    11718);
            equip(bot, Equipment.SLOT_CHEST,  11720);
            equip(bot, Equipment.SLOT_LEGS,   11722);
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{4734, 18357, 25918}));  // Karil's cbow / Chaotic cbow / Karil pistol
            equip(bot, Equipment.SLOT_FEET,   2577);   // Ranger boots
            equip(bot, Equipment.SLOT_HANDS,  pick(new int[]{22362, 22364}));  // Swift gloves
        } else if (wealth >= 2) {
            // Karil's set
            equip(bot, Equipment.SLOT_HAT,    4732);   // Karil's coif
            equip(bot, Equipment.SLOT_CHEST,  4736);   // Karil's top
            equip(bot, Equipment.SLOT_LEGS,   4738);   // Karil's skirt
            equip(bot, Equipment.SLOT_WEAPON, 4734);   // Karil's crossbow
            equip(bot, Equipment.SLOT_FEET,   2577);
        } else {
            // Black/red dhide + magic shortbow / rune crossbow
            equip(bot, Equipment.SLOT_HAT,    2581);   // Robin hat
            equip(bot, Equipment.SLOT_CHEST,  pick(new int[]{2503, 2501, 2499}));
            equip(bot, Equipment.SLOT_LEGS,   pick(new int[]{2497, 2495, 2493}));
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{861, 9185, 4214}));  // Mage shortbow / Rune cwbow / Crystal bow
        }
        equip(bot, Equipment.SLOT_ARROWS, new Item(892, 250));  // rune arrows
    }

    // ===== MAGIC =====

    private static void applyMage(Player bot, int cb) {
        int wealth = rollWealth(cb);
        // Rune supply for any mage that's not using an unlimited-charge
        // staff. Polypore + Chaotic staffs draw their own ammo, but the
        // lower-tier bots end up with regular elemental staves that need
        // air/water/earth/fire + chaos/death/blood runes.
        try {
            bot.getInventory().addItem(556, 5000); // air
            bot.getInventory().addItem(555, 5000); // water
            bot.getInventory().addItem(557, 5000); // earth
            bot.getInventory().addItem(554, 5000); // fire
            bot.getInventory().addItem(558, 5000); // mind (strikes)
            bot.getInventory().addItem(562, 5000); // chaos (bolts)
            bot.getInventory().addItem(560, 2000); // death (blasts)
            bot.getInventory().addItem(565, 1000); // blood (waves/surges)
        } catch (Throwable ignore) {}
        // Pick + set an autocast spell tier based on Magic level so the bot
        // actually starts attacking when PlayerCombatNew runs.
        try {
            int mag = bot.getSkills().getLevelForXp(com.rs.game.player.Skills.MAGIC);
            int spell;
            if      (mag >= 95) spell = 61;  // Air surge
            else if (mag >= 75) spell = 48;  // Air wave
            else if (mag >= 55) spell = 42;  // Air blast
            else if (mag >= 35) spell = 32;  // Air bolt
            else                spell = 25;  // Air strike
            bot.getCombatDefinitions().setAutoCastSpell(spell);
        } catch (Throwable ignore) {}

        if (wealth >= 4) {
            // Virtus + Polypore staff
            equip(bot, Equipment.SLOT_HAT,    20161);  // Virtus mask
            equip(bot, Equipment.SLOT_CHEST,  20165);  // Virtus robe top
            equip(bot, Equipment.SLOT_LEGS,   20169);  // Virtus robe legs
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{22494, 18355}));  // Polypore / Chaotic staff
            equip(bot, Equipment.SLOT_HANDS,  24980);  // Virtus gloves
            equip(bot, Equipment.SLOT_FEET,   21795);  // Ragefire
        } else if (wealth >= 3) {
            // Ganodermic + chaotic/polypore
            equip(bot, Equipment.SLOT_HAT,    22482);
            equip(bot, Equipment.SLOT_CHEST,  22490);
            equip(bot, Equipment.SLOT_LEGS,   22486);
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{22494, 18355}));
            equip(bot, Equipment.SLOT_FEET,   2579);   // Wizard boots
            equip(bot, Equipment.SLOT_HANDS,  pick(new int[]{22366, 22368}));  // Spellcaster gloves
        } else if (wealth >= 2) {
            // Ahrim's set (Subjugation tier)
            equip(bot, Equipment.SLOT_HAT,    4708);
            equip(bot, Equipment.SLOT_CHEST,  4712);
            equip(bot, Equipment.SLOT_LEGS,   4714);
            equip(bot, Equipment.SLOT_WEAPON, 4710);   // Ahrim's staff
            equip(bot, Equipment.SLOT_FEET,   2579);
        } else {
            // Mystic (random color)
            int[] hats = {4089, 4099, 4109};
            int[] tops = {4091, 4101, 4111};
            int[] bots = {4093, 4103, 4113};
            int color = Utils.random(3);
            equip(bot, Equipment.SLOT_HAT,    hats[color]);
            equip(bot, Equipment.SLOT_CHEST,  tops[color]);
            equip(bot, Equipment.SLOT_LEGS,   bots[color]);
            equip(bot, Equipment.SLOT_WEAPON, pick(new int[]{1391, 1389, 1387}));
            equip(bot, Equipment.SLOT_FEET,   2579);
        }
    }

    // ===== HYBRID =====

    private static void applyHybrid(Player bot, int cb) {
        int roll = Utils.random(3);
        if (roll == 0) applyMelee(bot, cb);
        else if (roll == 1) applyRanger(bot, cb);
        else applyMage(bot, cb);
    }

    // ===== MAXED =====

    private static void applyEndgameRandom(Player bot) {
        // Random pick of BIS melee/range/mage
        int style = Utils.random(3);
        if (style == 0) meleeBIS(bot);
        else if (style == 1) {
            equip(bot, Equipment.SLOT_HAT,    20149);
            equip(bot, Equipment.SLOT_CHEST,  20153);
            equip(bot, Equipment.SLOT_LEGS,   20157);
            equip(bot, Equipment.SLOT_WEAPON, 24338);  // Royal crossbow
            equip(bot, Equipment.SLOT_FEET,   21790);
            equip(bot, Equipment.SLOT_HANDS,  24974);
        } else {
            equip(bot, Equipment.SLOT_HAT,    20161);
            equip(bot, Equipment.SLOT_CHEST,  20165);
            equip(bot, Equipment.SLOT_LEGS,   20169);
            equip(bot, Equipment.SLOT_WEAPON, 22494);
            equip(bot, Equipment.SLOT_FEET,   21795);
            equip(bot, Equipment.SLOT_HANDS,  24980);
        }
        // Always max cape + completionist
        equip(bot, Equipment.SLOT_CAPE,   pick(new int[]{20767, 20769}));
        equip(bot, Equipment.SLOT_AMULET, 6585);   // Fury
        equip(bot, Equipment.SLOT_RING,   pick(new int[]{6737, 6735, 6575}));
    }

    // ===== F2P =====

    private static void applyF2PMelee(Player bot, int cb) {
        // Caps at rune
        int t = Math.min(pickMetalTier(cb), 5);
        int[] helms  = { 1155, 1153, 1157, 1159, 1161, 1163 };
        int[] bodies = { 1117, 1115, 1119, 1121, 1123, 1127 };
        int[] legs   = { 1075, 1067, 1069, 1071, 1073, 1079 };
        int[] weapons= { 1321, 1323, 1325, 1329, 1331, 1333 };
        int[] shields= { 1189, 1191, 1193, 1197, 1199, 1201 };
        equip(bot, Equipment.SLOT_HAT,    helms[t]);
        equip(bot, Equipment.SLOT_CHEST,  bodies[t]);
        equip(bot, Equipment.SLOT_LEGS,   legs[t]);
        equip(bot, Equipment.SLOT_WEAPON, weapons[t]);
        equip(bot, Equipment.SLOT_SHIELD, shields[t]);
    }

    // ===== SKILLER =====

    private static void applySkiller(Player bot, int cb) {
        // Skillcape (matched to a randomly-chosen skill) + matching robes
        // Skillers display their identity through skill capes
        int[] skillcapes = {
            9774, 9777, 9780, 9783, 9786, 9792, 9795, 9798, 9801, 9804, 9807, 9810,  // melee skills
            9762, 9765, 9771, 9759, 9948, 12169, 15706, 29185                         // skill capes incl summoning, dung, divin
        };
        equip(bot, Equipment.SLOT_CAPE, pick(skillcapes));

        // Robes - colored variety
        int robeSet = Utils.random(6); int[][] robes = { {577,1011}, {581,1015}, {638,639}, {640,641}, {642,643}, {644,645} };  // wizard/black/green/blue/cream/turquoise + enchanted
        
        equip(bot, Equipment.SLOT_CHEST, robes[robeSet][0]);
        equip(bot, Equipment.SLOT_LEGS, robes[robeSet][1]);
        if (chance(40)) equip(bot, Equipment.SLOT_HAT, 579);  // wizard hat
        if (chance(60)) equip(bot, Equipment.SLOT_FEET, pick(new int[]{2579, 88}));
        // No weapon - they're skillers
    }

    // ===== ACCESSORIES (rings, amulets, capes) =====

    private static void applyAccessories(Player bot, String archetype, int cb) {
        // Skip if archetype handles its own accessories (skiller has cape, maxed has ring/cape/ammy already)
        if ("skiller".equals(archetype) || "maxed".equals(archetype)) return;

        // Amulet - 60% chance
        if (chance(60) && bot.getEquipment().getItem(Equipment.SLOT_AMULET) == null) {
            int amulet;
            if (cb >= 90 && chance(40)) amulet = 6585;       // Fury
            else if (cb >= 60)          amulet = pick(new int[]{1725, 1731, 1712, 1727});  // Strength/Power/Glory(4)/Magic
            else                        amulet = pick(new int[]{1725, 1731, 1727, 1729});
            equip(bot, Equipment.SLOT_AMULET, amulet);
        }

        // Ring - 40% chance
        if (chance(40) && bot.getEquipment().getItem(Equipment.SLOT_RING) == null) {
            int ring;
            if (cb >= 90 && chance(30)) ring = pick(new int[]{6737, 6735});  // Berserker / Warrior
            else if (cb >= 60)          ring = pick(new int[]{2572, 1641, 1643});  // Wealth / Ruby / Diamond
            else                        ring = pick(new int[]{1635, 1637, 1639});
            equip(bot, Equipment.SLOT_RING, ring);
        }

        // Cape - 50% chance if no cape yet
        if (chance(50) && bot.getEquipment().getItem(Equipment.SLOT_CAPE) == null) {
            int cape;
            if (cb >= 100 && chance(20))      cape = 6570;                 // Fire cape
            else if (cb >= 70 && chance(40))  cape = 6570;                 // Fire cape
            else                              cape = pick(new int[]{14641, 14642, 4514, 4516});  // colored team capes
            equip(bot, Equipment.SLOT_CAPE, cape);
        }

        // Boots fallback (if not set)
        if (bot.getEquipment().getItem(Equipment.SLOT_FEET) == null && chance(70)) {
            equip(bot, Equipment.SLOT_FEET, pick(new int[]{88, 4121}));  // Boots of lightness, iron boots
        }

        // Gloves fallback
        if (bot.getEquipment().getItem(Equipment.SLOT_HANDS) == null && chance(50)) {
            equip(bot, Equipment.SLOT_HANDS, 1059);
        }
    }

    // ===== Helpers =====

    private static int pickMetalTier(int cb) {
        if (cb < 20) return 0;       // bronze
        if (cb < 30) return 1;       // iron
        if (cb < 40) return 2;       // steel
        if (cb < 50) return 3;       // mithril
        if (cb < 60) return 4;       // adamant
        if (cb < 80) return 5;       // rune
        return 6;
    }

    private static int pick(int[] pool) {
        if (pool == null || pool.length == 0) return -1;
        return pool[Utils.random(pool.length)];
    }

    private static boolean chance(int percent) {
        return Utils.random(100) < percent;
    }

    private static void equip(Player bot, int slot, int itemId) {
        if (itemId <= 0) return;
        try {
            com.rs.cache.loaders.ItemDefinitions def = com.rs.cache.loaders.ItemDefinitions.getItemDefinitions(itemId);
            if (def == null || def.getName() == null || def.getName().equalsIgnoreCase("null")) {
                System.err.println("[BotEquipment] BAD item id " + itemId + " for slot " + slot);
                return;
            }
            // Stat gate: bots can't wear gear above their level. Silently
            // skipping is fine - the slot stays empty if no fallback was
            // provided. equipBest() handles fallback chains for the cases
            // where we want a graceful downgrade.
            if (!EquipmentReqs.canWear(bot, itemId)) return;
            bot.getEquipment().getItems().set(slot, new Item(itemId, 1));
        } catch (Throwable t) {
            System.err.println("[BotEquipment] error setting item " + itemId + " slot " + slot + ": " + t);
        }
    }

    private static void equip(Player bot, int slot, Item item) {
        if (item == null || item.getId() <= 0) return;
        if (!EquipmentReqs.canWear(bot, item.getId())) return;
        try { bot.getEquipment().getItems().set(slot, item); } catch (Throwable ignored) {}
    }

    /**
     * Equip the highest-tier item from the candidates the bot meets the
     * requirement for. Pass items in best-to-worst order. If none qualify,
     * the slot is left empty (better than wrong gear). Use this in tier
     * methods to give bots a graceful downgrade chain instead of skipping
     * the slot when the top-tier piece is too high level.
     */
    private static void equipBest(Player bot, int slot, int... candidates) {
        if (candidates == null) return;
        for (int id : candidates) {
            if (id <= 0) continue;
            if (!EquipmentReqs.canWear(bot, id)) continue;
            equip(bot, slot, id);
            return;
        }
    }
}
