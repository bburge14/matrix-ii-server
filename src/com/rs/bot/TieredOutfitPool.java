package com.rs.bot;

/**
 * Structured outfit dataset for bot equipment, organised by combat
 * style (MELEE / RANGED / MAGIC / SKILLER / SOCIALITE) and wealth tier
 * (POOR / MIDDLE / RICH / WEALTHY). Captured from the user's Matrix III
 * 830-cache spec.
 *
 * BotEquipment.applyMelee / applyRanger / applyMage / applySkiller /
 * applySocialite read from here so a single edit updates the entire
 * loadout pool. tierForCb maps a bot's combat level to a tier; bots
 * with cb < 60 = POOR, < 90 = MIDDLE, < 110 = RICH, else WEALTHY.
 */
public final class TieredOutfitPool {

    private TieredOutfitPool() {}

    public enum Style { MELEE, RANGED, MAGIC, SKILLER, SOCIALITE }
    public enum Tier  { POOR, MIDDLE, RICH, WEALTHY }

    public static Tier tierForCb(int cb) {
        if (cb < 60)  return Tier.POOR;
        if (cb < 90)  return Tier.MIDDLE;
        if (cb < 110) return Tier.RICH;
        return Tier.WEALTHY;
    }

    /**
     * Slots: hat, body, legs, weapon, shield, cape.
     * -1 in any position = leave that slot empty (or pick from a
     * separate accessories pool). Each Set may have multiple options
     * per slot - BotEquipment picks one at random.
     */
    public static final class Set {
        public final int[] hats, bodies, legs, weapons, shields, capes, gloves, feet;
        public Set(int[] hats, int[] bodies, int[] legs,
                   int[] weapons, int[] shields, int[] capes,
                   int[] gloves, int[] feet) {
            this.hats = hats; this.bodies = bodies; this.legs = legs;
            this.weapons = weapons; this.shields = shields; this.capes = capes;
            this.gloves = gloves; this.feet = feet;
        }
        public Set(int[] hats, int[] bodies, int[] legs,
                   int[] weapons, int[] shields, int[] capes) {
            this(hats, bodies, legs, weapons, shields, capes, EMPTY, EMPTY);
        }
    }

    private static final int[] EMPTY = new int[0];

    // ============================================================
    //  MELEE
    // ============================================================

    public static final Set MELEE_POOR = new Set(
        new int[] { 1139, 1153, 1157, 1165 },                                 // bronze, iron, steel, black helms
        new int[] { 1117, 1115, 1119, 1125 },                                 // bronze, iron, steel, black bodies
        new int[] { 1075, 1067, 1069, 1077 },                                 // bronze, iron, steel, black legs
        new int[] { 1321, 1323, 1325, 1307, 1311 },                           // bronze/iron/steel scim, bronze/steel 2h
        new int[] { 1189, 1191, 1193 },                                       // bronze/iron/steel kite
        new int[] { 1052, 4315, 4317, 4319, 4321, 4323, 4325, 4327, 4329 }    // legends + team capes
    );

    public static final Set MELEE_MIDDLE = new Set(
        new int[] { 1159, 1161, 1163, 10828 },                                // mith, addy, rune, helm of neitiznot
        new int[] { 1121, 1123, 1127, 10551 },                                // mith, addy, rune body, fighter torso
        new int[] { 1071, 1073, 1079 },                                       // mith, addy, rune legs
        new int[] { 1329, 1331, 1333, 4587, 1319 },                           // mith/addy/rune scim, dragon scim, rune 2h
        new int[] { 1197, 1199, 1201, 1187 },                                 // mith/addy/rune kite, dragon sq
        new int[] { 6568, 6570 }                                              // obby cape, fire cape
    );

    public static final Set MELEE_RICH = new Set(
        new int[] { 4716, 4724, 4753, 11724 },                                // dharok/guthan/verac helms, bandos chest (head slot fallback)
        new int[] { 11724, 4720, 4728, 4755 },                                // bandos chestplate, dharok/guthan/verac bodies
        new int[] { 11726, 4722, 4730, 4759 },                                // bandos tassets, dharok/guthan/verac legs
        new int[] { 4151, 21371, 11694, 11696, 4718 },                        // whip, vine whip, ags, bgs, dharok axe
        new int[] { 11283 },                                                  // dragonfire shield
        new int[] { 6570, 23659, 9747, 9750, 9753, 9768, 9747+1, 9750+1 }     // fire cape, tokhaar, attk/str/def/hp untrim+trim
    );

    public static final Set MELEE_WEALTHY = new Set(
        new int[] { 30005, 20135 },                                           // malevolent, torva
        new int[] { 30008, 20139, 33348, 33336 },                             // malevolent, torva, shadow malevolent, barrows malevolent
        new int[] { 30011, 20143 },                                           // malevolent, torva legs
        new int[] { 26605, 26611, 31725, 33224, 33302 },                      // drygore mace/rapier/longsword, nox scythe, shadow drygore, shadow nox
        new int[] { 11283 },                                                  // dfs as shield (no spirit shields here per spec)
        new int[] { 20769, 20771, 20767, 31284 }                              // comp, trimmed comp, max, 120 slayer cape
    );

    // ============================================================
    //  RANGED
    // ============================================================

    public static final Set RANGED_POOR = new Set(
        new int[] { 1169 },                                                   // coif
        new int[] { 1129, 1131 },                                             // leather body, hard leather
        new int[] { 1095 },                                                   // leather chaps fallback
        new int[] { 841, 843 },                                               // shortbow, oak bow
        EMPTY,
        new int[] { 1019 }                                                    // basic cape
    );

    public static final Set RANGED_MIDDLE = new Set(
        new int[] { 10829 },                                                  // archer helm
        new int[] { 1135, 2499 },                                             // green / blue d'hide bodies
        new int[] { 1099, 2493 },                                             // green / blue d'hide chaps
        new int[] { 9185 },                                                   // rune crossbow
        new int[] { 2487 },                                                   // green d'hide vambraces (shield-slot fallback)
        new int[] { 10499 }                                                   // ava's accumulator
    );

    public static final Set RANGED_RICH = new Set(
        new int[] { 11722, 4732 },                                            // armadyl helm, karil's coif
        new int[] { 11718, 4736 },                                            // armadyl chest, karil's leather top
        new int[] { 11720, 4738 },                                            // armadyl chainskirt, karil's leather skirt
        new int[] { 11235, 14619 },                                           // dark bow, armadyl crossbow
        EMPTY,                                                                // ranged 2h, no shield
        new int[] { 21453 }                                                   // ava's alerter
    );

    public static final Set RANGED_WEALTHY = new Set(
        new int[] { 33414 },                                                  // sirenic mask shadow / pernix cowl handled in body slot pick
        new int[] { 29715, 20151, 33396 },                                    // sirenic hauberk, pernix body, sirenic shadow
        new int[] { 29718, 20155 },                                           // sirenic chaps, pernix chaps
        new int[] { 31729, 28437 },                                           // noxious longbow, ascension crossbow
        EMPTY,
        new int[] { 20769, 20771, 20767 }                                     // comp / trimmed / max
    );

    // ============================================================
    //  MAGIC
    // ============================================================

    public static final Set MAGIC_POOR = new Set(
        new int[] { 579, 1033 },                                              // wizard hat, zamorak hat
        new int[] { 577, 1035 },                                              // blue wizard robe top, zamorak top
        new int[] { 1011 },                                                   // wizard robe legs
        new int[] { 1381 },                                                   // staff of air
        EMPTY,
        new int[] { 1019 }
    );

    public static final Set MAGIC_MIDDLE = new Set(
        new int[] { 4089, 4099, 4109 },                                       // mystic hat blue / dark / light
        new int[] { 4091, 4101, 4111, 22424 },                                // mystic top blue/dark/light, batwing
        new int[] { 4093, 4103, 4113, 22432 },                                // mystic legs blue/dark/light, batwing legs
        new int[] { 4675 },                                                   // ancient staff
        EMPTY,
        new int[] { 6568, 6570 }
    );

    public static final Set MAGIC_RICH = new Set(
        new int[] { 4708, 26337 },                                            // ahrim's hood, subjugation hood
        new int[] { 4712, 22486 },                                            // ahrim's robetop, ganodermic poncho
        new int[] { 4714, 22490 },                                            // ahrim's robeskirt, ganodermic legs
        new int[] { 22494, 15486 },                                           // polypore staff, staff of light
        EMPTY,
        new int[] { 6570, 9762, 9763 }                                        // fire cape, magic cape untrim+trim
    );

    public static final Set MAGIC_WEALTHY = new Set(
        new int[] { 33405 },                                                  // tectonic mask shadow placeholder
        new int[] { 28611, 20163, 33468 },                                    // tectonic top, virtus top, blood tectonic top
        new int[] { 28614, 20167 },                                           // tectonic legs, virtus legs
        new int[] { 31733, 28617 },                                           // noxious staff, seismic wand
        EMPTY,
        new int[] { 20769, 20771, 20767 }
    );

    // ============================================================
    //  SKILLER  (tier matches activity tier; capes match progression)
    // ============================================================

    public static final Set SKILLER_POOR = new Set(
        new int[] { 1949 },                                                   // chef's hat
        new int[] { 1757 },                                                   // brown apron
        new int[] {},                                                         // default player legs
        new int[] {},                                                         // no weapon
        EMPTY,
        new int[] { 1019 }
    );

    public static final Set SKILLER_MIDDLE = new Set(
        new int[] { 6654, 10836, 15259 },                                     // camo top (head), bearhead, mining helmet
        new int[] { 6654, 6185 },                                             // camo top, lederhosen top
        new int[] { 6655 },                                                   // camo bottom
        new int[] {},
        EMPTY,
        new int[] { 6568 }
    );

    public static final Set SKILLER_RICH = new Set(
        // Lumberjack (10939/41/40), Prospector (25185/86/87), Angler (25195/96/97),
        // Pyromancer-style (20789/91/90), Wicked robes (22332/34/36)
        new int[] { 10939, 25185, 25195, 20789, 22332 },
        new int[] { 10941, 25186, 25196, 20791, 22334 },
        new int[] { 10940, 25187, 25197, 20790, 22336 },
        new int[] {},
        EMPTY,
        new int[] { 9747, 9759, 9762, 9777, 9780, 9786, 9792, 9798, 9801, 9804, 9807 }  // assorted skillcape untrims
    );

    public static final Set SKILLER_WEALTHY = new Set(
        // Master fishing (32151+), master runecrafter (25190+), 120 cape variants
        new int[] { 32151, 25190 },
        new int[] { 32152, 25191 },
        new int[] { 32153, 25192 },
        new int[] {},
        EMPTY,
        new int[] { 19709, 31277, 9747+1, 9762+1, 9786+1 }                    // 120 dung, 120 herb, trimmed cape variants
    );

    // ============================================================
    //  SOCIALITE  (no combat tier - just dress code)
    // ============================================================

    public static final Set SOCIALITE_MIDDLE = new Set(
        // Suit jackets / hats / leg variants
        new int[] { 7332, 2643, 2631 },                                       // boater, cavalier, highwayman mask
        new int[] { 10408, 10420, 10424 },                                    // blue, green, purple elegant tops
        new int[] { 10410, 10422, 10426 },                                    // blue, green, purple elegant legs
        new int[] {},
        EMPTY,
        new int[] { 6568 }
    );

    public static final Set SOCIALITE_RICH = new Set(
        new int[] { 13101 },                                                  // top hat (head)
        new int[] { 10400, 10436, 10412, 25843, 25859 },                      // black, white, gold elegant + tuxedo + tropical
        new int[] { 10402, 10438, 10414, 25845, 25861 },                      // matching legs + tux + grass skirt
        new int[] { 10507, 4566 },                                            // bouquet, rubber chicken (held)
        EMPTY,
        new int[] { 6570, 6568 }
    );

    public static final Set SOCIALITE_WEALTHY = new Set(
        // Partyhats + rare masks + 3rd age + wings
        new int[] { 1038, 1040, 1042, 1044, 1046, 1048, 1050, 1053, 1055, 1057, 29854 },
        new int[] { 10348, 10338, 19302 },                                    // 3a melee/mage/druidic top
        new int[] { 10346, 10340, 19305 },                                    // 3a legs
        new int[] { 20964, 19308 },                                           // golden cane, 3a druidic staff
        EMPTY,
        new int[] { 25881, 25883, 25885, 20771 }                              // zammy / icyenic / skeletal wings, trimmed comp
    );

    /** Dispatch helper: pick the right Set for a (style, tier) combo. */
    public static Set pick(Style style, Tier tier) {
        switch (style) {
            case MELEE:
                switch (tier) {
                    case POOR: return MELEE_POOR;
                    case MIDDLE: return MELEE_MIDDLE;
                    case RICH: return MELEE_RICH;
                    case WEALTHY: return MELEE_WEALTHY;
                }
                break;
            case RANGED:
                switch (tier) {
                    case POOR: return RANGED_POOR;
                    case MIDDLE: return RANGED_MIDDLE;
                    case RICH: return RANGED_RICH;
                    case WEALTHY: return RANGED_WEALTHY;
                }
                break;
            case MAGIC:
                switch (tier) {
                    case POOR: return MAGIC_POOR;
                    case MIDDLE: return MAGIC_MIDDLE;
                    case RICH: return MAGIC_RICH;
                    case WEALTHY: return MAGIC_WEALTHY;
                }
                break;
            case SKILLER:
                switch (tier) {
                    case POOR: return SKILLER_POOR;
                    case MIDDLE: return SKILLER_MIDDLE;
                    case RICH: return SKILLER_RICH;
                    case WEALTHY: return SKILLER_WEALTHY;
                }
                break;
            case SOCIALITE:
                switch (tier) {
                    case POOR:    // socialites don't have a "poor" tier per spec
                    case MIDDLE:  return SOCIALITE_MIDDLE;
                    case RICH:    return SOCIALITE_RICH;
                    case WEALTHY: return SOCIALITE_WEALTHY;
                }
                break;
        }
        return MELEE_MIDDLE; // safe default
    }
}
