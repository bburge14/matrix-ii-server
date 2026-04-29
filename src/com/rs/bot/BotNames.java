package com.rs.bot;

import java.util.Random;

import com.rs.game.World;

/**
 * Generates randomized RSPS-style usernames using a mix of styles, so 50
 * bots online don't all look like cookie-cutter "WildKnight42". Picks one of
 * eight naming styles per bot:
 *   - AdjNoun + 1-3 digits           ("ShadowReaper47")
 *   - Fantasy 2-syllable name        ("Thalric", "Aedwyn")
 *   - "xX_Word_Xx" tryhard           ("xX_DeathDealer_Xx")
 *   - Real-ish first name + role     ("MikeTheKnight", "JenSlayer")
 *   - Animal + verb + number         ("WolfStriker22")
 *   - lowercase short + numbers      ("zerk69", "bow420")
 *   - Word + word camelCase          ("ironToast", "saltDestroyer")
 *   - Noun + Noun                    ("BloodFury", "IronHand")
 * Falls back to a unique nanoTime suffix if collision persists.
 */
public class BotNames {

    private static final Random RNG = new Random();

    private static final String[] ADJ = {
        "Wild","Dark","Silver","Lone","Old","Young","Cool","Epic","Grim",
        "Swift","Bold","Mad","Iron","Holy","Fierce","Quiet","Lucky","Shadow",
        "Crimson","Frozen","Burning","Ancient","Sacred","Cursed","Blessed",
        "Savage","Brutal","Stealthy","Drunken","Sleepy","Hungry","Greedy",
        "Noble","Rusty","Golden","Steel","Mystic","Phantom","Eternal","Void",
        "Toxic","Plague","Rabid","Feral","Royal","Ghostly","Bloody","Lazy"
    };

    private static final String[] NOUN = {
        "Knight","Mage","Ranger","Slayer","Hunter","Scout","Warlock","Cleric",
        "Monk","Rogue","Bard","Reaper","Wizard","Fighter","Archer","Priest",
        "Berserker","Crusader","Paladin","Sorcerer","Necromancer","Druid",
        "Warden","Templar","Vagabond","Bandit","Sentinel","Marauder","Outlaw",
        "Pyromancer","Cryomancer","Geomancer","Phoenix","Wyvern","Dragon",
        "Wolf","Falcon","Viper","Reaver","Cutthroat","Brigand","Specter"
    };

    private static final String[] FANTASY_PREFIX = {
        "Ael","Bal","Cael","Dor","Edr","Fae","Gal","Hal","Iru","Jor","Kael",
        "Lor","Mor","Nael","Olr","Pyr","Qor","Ral","Sar","Thal","Ulr","Vaer",
        "Wyl","Xal","Yor","Zar","Aedw","Bryn","Cyrr","Dunn","Eor"
    };

    private static final String[] FANTASY_SUFFIX = {
        "ric","yn","wen","dor","ion","las","mir","nor","gar","din","len",
        "ven","mar","tor","fel","gan","wyn","drid","loth","fas","mund","raen"
    };

    private static final String[] REAL_FIRST = {
        "Mike","Jen","Bob","Sam","Alex","Pat","Chris","Tony","Steve","Dave",
        "Linda","Karen","Jim","Brad","Greg","Marcus","Ricky","Vinny","Carl",
        "Sue","Hank","Earl","Jeff","Frank","Donny","Marv","Walt","Norm","Lou"
    };

    private static final String[] ROLE_TAG = {
        "Knight","Slayer","PKer","Boss","Pro","God","King","Queen","Lord",
        "Master","Hero","Champ","Sniper","Camper","Lover","Hater"
    };

    private static final String[] ANIMAL = {
        "Wolf","Bear","Hawk","Lion","Tiger","Shark","Raven","Falcon","Cobra",
        "Stag","Boar","Eagle","Panther","Mantis","Spider","Wasp","Snake",
        "Crab","Otter","Hyena","Owl","Bat","Drake"
    };

    private static final String[] VERB = {
        "Striker","Slayer","Hunter","Stalker","Crusher","Slasher","Smasher",
        "Bringer","Walker","Rider","Caller","Ender","Killer","Eater","Lover"
    };

    private static final String[] LOWER_SHORT = {
        "zerk","bow","tank","range","mage","melee","prod","grim","trim","dh",
        "afk","grind","rs","loot","pk","clue","gp","exp","xp","tick","crash",
        "scoob","shrek","yoinks","bork","oof","yolo","kek","poggers","sus"
    };

    private static final String[] CAMEL_LEFT = {
        "iron","steel","gold","void","obs","drag","rune","mith","spite","dharok",
        "guthan","verac","ahrim","karil","torag","rapier","blow","boom","loot",
        "pray","flick","tick","crab","camel","tofu","bread","salt","yew","oak"
    };

    private static final String[] CAMEL_RIGHT = {
        "Toast","Destroyer","Lord","King","Vibes","Andy","Bob","Steve","Mike",
        "Slayer","Grinder","Spammer","Camper","Banger","Smith","Dealer","Lover",
        "Scout","Brain","Soul","Fury","Storm","Flame","Edge","Strike"
    };

    private static final String[] DOUBLE_NOUN = {
        "Blood","Iron","Steel","Bone","Soul","Fire","Frost","Storm","Shadow",
        "Light","Dark","Death","Life","Sun","Moon","Star","Wolf","Hawk"
    };

    private static final String[] DOUBLE_NOUN_2 = {
        "Fury","Hand","Edge","Heart","Fang","Claw","Soul","Scream","Cleaver",
        "Striker","Bane","Kin","Forge","Tongue","Gaze","Crown","Ward","Veil"
    };

    public static String generate() {
        for (int attempt = 0; attempt < 30; attempt++) {
            String name = oneOfEightStyles();
            if (name.length() < 2 || name.length() > 12) continue;
            if (World.getPlayerByDisplayName(name) == null) return name;
        }
        // fallback: definitely-unique numeric suffix
        return "AI" + (System.nanoTime() % 10000);
    }

    private static String oneOfEightStyles() {
        int style = RNG.nextInt(8);
        switch (style) {
            case 0: // Classic AdjNoun + digits
                return ADJ[RNG.nextInt(ADJ.length)]
                     + NOUN[RNG.nextInt(NOUN.length)]
                     + smallNumber();
            case 1: // Fantasy 2-syllable
                return FANTASY_PREFIX[RNG.nextInt(FANTASY_PREFIX.length)]
                     + FANTASY_SUFFIX[RNG.nextInt(FANTASY_SUFFIX.length)];
            case 2: // xX_Word_Xx tryhard (capped to 12 chars)
                String inside = NOUN[RNG.nextInt(NOUN.length)];
                if (inside.length() > 6) inside = inside.substring(0, 6);
                return "xX_" + inside + "_Xx";
            case 3: // Real first + role  ("MikeKnight")
                return REAL_FIRST[RNG.nextInt(REAL_FIRST.length)]
                     + ROLE_TAG[RNG.nextInt(ROLE_TAG.length)];
            case 4: // Animal + verb + number ("WolfStriker22")
                return ANIMAL[RNG.nextInt(ANIMAL.length)]
                     + VERB[RNG.nextInt(VERB.length)]
                     + smallNumber();
            case 5: // lowercase + digits ("zerk69")
                return LOWER_SHORT[RNG.nextInt(LOWER_SHORT.length)]
                     + smallNumber();
            case 6: // camelCase ("ironToast")
                return CAMEL_LEFT[RNG.nextInt(CAMEL_LEFT.length)]
                     + CAMEL_RIGHT[RNG.nextInt(CAMEL_RIGHT.length)];
            case 7: // DoubleNoun ("BloodFury")
                return DOUBLE_NOUN[RNG.nextInt(DOUBLE_NOUN.length)]
                     + DOUBLE_NOUN_2[RNG.nextInt(DOUBLE_NOUN_2.length)];
        }
        return "Bot" + smallNumber();
    }

    private static String smallNumber() {
        // ~50% chance no digits, otherwise 1-3 digits.
        int kind = RNG.nextInt(4);
        switch (kind) {
            case 0: return "";
            case 1: return String.valueOf(RNG.nextInt(10));
            case 2: return String.valueOf(10 + RNG.nextInt(90));
            default: return String.valueOf(100 + RNG.nextInt(900));
        }
    }
}
