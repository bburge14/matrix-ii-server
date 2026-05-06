package com.rs.bot;

import com.rs.game.player.Player;
import com.rs.game.player.Skills;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Skill-level requirements for high-tier wearable items.
 *
 * The 830 cache doesn't expose item stat-requirements through the
 * standard ItemDefinitions API the way later RS3 caches do, so this
 * is a hand-curated table for the items the tiered outfit pool
 * actually picks. Items NOT in this table have no requirement.
 *
 * Built so {@link com.rs.bot.TieredOutfitPool} can filter each slot's
 * pool down to options the bot actually meets - prevents the
 * "level-1 ranger with a zaryte bow" embarrassment the user reported.
 *
 * Each entry maps item id -> array of (skill, level) pairs flattened
 * as alternating ints. e.g. { Skills.RANGE, 80, Skills.DEFENCE, 80 }.
 */
public final class ItemRequirements {

    private ItemRequirements() {}

    private static final Map<Integer, int[]> REQS = new HashMap<>();

    static {
        // === MELEE WEAPONS ===
        req(4151,  Skills.ATTACK, 70);   // Abyssal whip
        req(21371, Skills.ATTACK, 80);   // Abyssal vine whip
        req(11696, Skills.ATTACK, 75);   // Armadyl godsword
        req(11694, Skills.ATTACK, 75);   // Bandos godsword
        req(11698, Skills.ATTACK, 75);   // Saradomin godsword
        req(11700, Skills.ATTACK, 75);   // Zamorak godsword
        req(4587,  Skills.ATTACK, 60);   // Dragon scimitar
        req(1305,  Skills.ATTACK, 60);   // Dragon longsword
        req(7158,  Skills.ATTACK, 60);   // Dragon 2h
        req(1377,  Skills.ATTACK, 60);   // Dragon battleaxe
        req(1434,  Skills.ATTACK, 60);   // Dragon mace
        req(1215,  Skills.ATTACK, 60);   // Dragon dagger
        req(4718,  Skills.ATTACK, 70);   // Dharok's greataxe
        req(4726,  Skills.ATTACK, 70);   // Guthan's spear
        req(4747,  Skills.ATTACK, 70);   // Torag's hammers
        req(4755,  Skills.ATTACK, 70);   // Verac's flail
        req(26605, Skills.ATTACK, 90);   // Drygore mace
        req(26611, Skills.ATTACK, 90);   // Drygore rapier
        req(31725, Skills.ATTACK, 90, Skills.STRENGTH, 90);   // Noxious scythe
        req(33224, Skills.ATTACK, 90);   // Shadow drygore
        req(33302, Skills.ATTACK, 90, Skills.STRENGTH, 90);   // Shadow nox scythe
        req(18353, Skills.ATTACK, 80);   // Chaotic maul

        // === MELEE ARMOR ===
        req(11724, Skills.DEFENCE, 65);  // Bandos chestplate
        req(11726, Skills.DEFENCE, 65);  // Bandos tassets
        req(11728, Skills.DEFENCE, 65);  // Bandos boots
        req(4716,  Skills.DEFENCE, 70);  // Dharok helm
        req(4720,  Skills.DEFENCE, 70);  // Dharok platebody
        req(4722,  Skills.DEFENCE, 70);  // Dharok platelegs
        req(4724,  Skills.DEFENCE, 70);  // Guthan helm
        req(4728,  Skills.DEFENCE, 70);  // Guthan platebody
        req(4730,  Skills.DEFENCE, 70);  // Guthan chainskirt
        req(4753,  Skills.DEFENCE, 70);  // Verac helm
        req(4755,  Skills.DEFENCE, 70);  // Verac brassard
        req(4759,  Skills.DEFENCE, 70);  // Verac plateskirt
        req(11283, Skills.DEFENCE, 75);  // Dragonfire shield
        req(20137, Skills.DEFENCE, 80);  // Torva full helm
        req(20141, Skills.DEFENCE, 80);  // Torva platebody
        req(20145, Skills.DEFENCE, 80);  // Torva platelegs
        req(20135, Skills.DEFENCE, 80);  // Torva (alt id from spec)
        req(20139, Skills.DEFENCE, 80);
        req(20143, Skills.DEFENCE, 80);
        req(30005, Skills.DEFENCE, 90);  // Malevolent helm
        req(30008, Skills.DEFENCE, 90);  // Malevolent cuirass
        req(30011, Skills.DEFENCE, 90);  // Malevolent greaves
        req(33348, Skills.DEFENCE, 90);  // Shadow malevolent
        req(33336, Skills.DEFENCE, 90);  // Barrows malevolent
        req(1163,  Skills.DEFENCE, 40);  // Rune full helm
        req(1127,  Skills.DEFENCE, 40);  // Rune platebody
        req(1079,  Skills.DEFENCE, 40);  // Rune platelegs
        req(1201,  Skills.DEFENCE, 40);  // Rune kiteshield
        req(10828, Skills.DEFENCE, 55);  // Helm of Neitiznot
        req(10551, Skills.DEFENCE, 1, Skills.HITPOINTS, 1); // Fighter torso (no req)

        // === RANGED WEAPONS ===
        req(11235, Skills.RANGE, 60);   // Dark bow
        req(14619, Skills.RANGE, 70);   // Armadyl crossbow (alt id)
        req(13083, Skills.RANGE, 70);   // Armadyl crossbow
        req(20171, Skills.RANGE, 80);   // Zaryte bow
        req(31729, Skills.RANGE, 90);   // Noxious longbow
        req(28437, Skills.RANGE, 90);   // Ascension crossbow
        req(33396, Skills.RANGE, 90);   // Shadow nox bow
        req(4734,  Skills.RANGE, 70);   // Karil's crossbow
        req(9185,  Skills.RANGE, 50);   // Rune crossbow

        // === RANGED ARMOR ===
        req(1099,  Skills.RANGE, 40);   // Green dhide chaps
        req(1135,  Skills.RANGE, 40);   // Green dhide body
        req(2487,  Skills.RANGE, 40);   // Green dhide vambs
        req(2493,  Skills.RANGE, 50);   // Blue dhide chaps
        req(2499,  Skills.RANGE, 50);   // Blue dhide body
        req(10829, Skills.RANGE, 45);   // Archer helm
        req(11718, Skills.RANGE, 70);   // Armadyl helmet
        req(11720, Skills.RANGE, 70);   // Armadyl chainskirt
        req(11722, Skills.RANGE, 70);   // Armadyl chestplate
        req(4732,  Skills.RANGE, 70, Skills.DEFENCE, 70);   // Karil's coif
        req(4736,  Skills.RANGE, 70, Skills.DEFENCE, 70);   // Karil's leather top
        req(4738,  Skills.RANGE, 70, Skills.DEFENCE, 70);   // Karil's leather skirt
        req(20149, Skills.RANGE, 80, Skills.DEFENCE, 80);   // Pernix cowl
        req(20151, Skills.RANGE, 80, Skills.DEFENCE, 80);   // Pernix body
        req(20155, Skills.RANGE, 80, Skills.DEFENCE, 80);   // Pernix chaps
        req(29715, Skills.RANGE, 90, Skills.DEFENCE, 90);   // Sirenic hauberk
        req(29718, Skills.RANGE, 90, Skills.DEFENCE, 90);   // Sirenic chaps
        req(33414, Skills.RANGE, 90, Skills.DEFENCE, 90);   // Sirenic mask shadow
        req(2577,  Skills.RANGE, 40);   // Ranger boots

        // === MAGIC WEAPONS ===
        req(4675,  Skills.MAGIC, 50);    // Ancient staff
        req(15486, Skills.MAGIC, 75);    // Staff of light
        req(22494, Skills.MAGIC, 80);    // Polypore staff
        req(6914,  Skills.MAGIC, 60);    // Master wand
        req(31733, Skills.MAGIC, 90);    // Noxious staff
        req(28617, Skills.MAGIC, 90);    // Seismic wand

        // === MAGIC ARMOR ===
        req(577,   Skills.MAGIC, 1);     // Wizard robe top
        req(1011,  Skills.MAGIC, 1);     // Wizard robe legs
        req(4089,  Skills.MAGIC, 40, Skills.DEFENCE, 20);   // Mystic hat blue
        req(4091,  Skills.MAGIC, 40, Skills.DEFENCE, 20);   // Mystic top
        req(4093,  Skills.MAGIC, 40, Skills.DEFENCE, 20);   // Mystic legs
        req(4099,  Skills.MAGIC, 40, Skills.DEFENCE, 20);   // Mystic hat dark
        req(4101,  Skills.MAGIC, 40, Skills.DEFENCE, 20);
        req(4103,  Skills.MAGIC, 40, Skills.DEFENCE, 20);
        req(4109,  Skills.MAGIC, 40, Skills.DEFENCE, 20);
        req(4111,  Skills.MAGIC, 40, Skills.DEFENCE, 20);
        req(4113,  Skills.MAGIC, 40, Skills.DEFENCE, 20);
        req(4708,  Skills.MAGIC, 70, Skills.DEFENCE, 70);   // Ahrim's hood
        req(4712,  Skills.MAGIC, 70, Skills.DEFENCE, 70);   // Ahrim's robetop
        req(4714,  Skills.MAGIC, 70, Skills.DEFENCE, 70);   // Ahrim's robeskirt
        req(22486, Skills.MAGIC, 80, Skills.DEFENCE, 80);   // Ganodermic poncho
        req(22490, Skills.MAGIC, 80, Skills.DEFENCE, 80);   // Ganodermic legs
        req(26337, Skills.MAGIC, 78);    // Subjugation hood
        req(26339, Skills.MAGIC, 78);    // Subjugation robe top
        req(20163, Skills.MAGIC, 80, Skills.DEFENCE, 80);   // Virtus top
        req(20167, Skills.MAGIC, 80, Skills.DEFENCE, 80);   // Virtus legs
        req(28611, Skills.MAGIC, 90);    // Tectonic top
        req(28614, Skills.MAGIC, 90);    // Tectonic legs
        req(33405, Skills.MAGIC, 90);    // Tectonic mask shadow
        req(33468, Skills.MAGIC, 90);    // Blood tectonic top

        // === CAPES with prerequisites ===
        req(20767, Skills.HITPOINTS, 99); // Max cape (uses HP as proxy for 'maxed')
        req(20769, Skills.HITPOINTS, 99); // Comp
        req(20771, Skills.HITPOINTS, 99); // Trimmed comp
        req(31284, Skills.SLAYER, 120);   // 120 slayer cape
        req(19709, Skills.DUNGEONEERING, 120);
        req(31277, Skills.HERBLORE, 120);
        req(6570,  Skills.HITPOINTS, 1);  // Fire cape - completion based, treat as no req
        req(23659, Skills.HITPOINTS, 1);  // TokHaar-Kal
    }

    private static void req(int itemId, int... pairs) {
        REQS.put(itemId, pairs);
    }

    /** True if {@code player} meets every (skill, level) pair recorded
     *  for {@code itemId}. Items not in the table are always wearable. */
    public static boolean canEquip(Player player, int itemId) {
        int[] reqs = REQS.get(itemId);
        if (reqs == null) return true;
        try {
            for (int i = 0; i + 1 < reqs.length; i += 2) {
                int skill = reqs[i];
                int needed = reqs[i + 1];
                if (player.getSkills().getLevelForXp(skill) < needed) {
                    return false;
                }
            }
        } catch (Throwable t) {
            return false;
        }
        return true;
    }

    /** Filter a pool array down to ids the player can equip. Returns
     *  the original array if everything passes, a new shorter array
     *  otherwise, or null if NOTHING matches (caller should drop down
     *  to a lower tier). */
    public static int[] filter(Player player, int[] pool) {
        if (pool == null || pool.length == 0) return pool;
        int[] tmp = new int[pool.length];
        int n = 0;
        for (int id : pool) if (canEquip(player, id)) tmp[n++] = id;
        if (n == 0)         return null;
        if (n == pool.length) return pool;
        return Arrays.copyOf(tmp, n);
    }
}
