package com.rs.bot;

import java.util.Random;

import com.rs.game.World;

/**
 * Generates usernames like "WildKnight42". Checks collision against online
 * players. Falls back to appending random digits if a collision persists.
 */
public class BotNames {

    private static final Random RNG = new Random();

    private static final String[] ADJ = {
        "Wild","Dark","Silver","Lone","Old","Young","Cool","Epic","Grim",
        "Swift","Bold","Mad","Iron","Holy","Fierce","Quiet","Lucky","Shadow"
    };

    private static final String[] NOUN = {
        "Knight","Mage","Ranger","Slayer","Hunter","Scout","Warlock","Cleric",
        "Monk","Rogue","Bard","Reaper","Wizard","Fighter","Archer","Priest"
    };

    public static String generate() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String name = ADJ[RNG.nextInt(ADJ.length)]
                        + NOUN[RNG.nextInt(NOUN.length)]
                        + (10 + RNG.nextInt(90));
            if (World.getPlayerByDisplayName(name) == null) {
                return name;
            }
        }
        // fallback: definitely-unique numeric suffix
        return "AI" + System.nanoTime();
    }
}
