package com.rs.bot;
import com.rs.utils.Utils;
import com.rs.utils.Utils;

/**
 * Builds a single AIPlayer in fully-initialized state, ready to be added to the world.
 * Mirrors what real login does (Player.init with all defaults), without the network layer.
 */
public final class BotFactory {

    private BotFactory() {}

    /** Create a fully-initialized bot. Returns null on failure (e.g. name collision). */
    public static AIPlayer create(String name) {
        try {
            AIPlayer bot = new AIPlayer();
            // 20-arg init signature - same as a fresh real account would get on first login.
            bot.init(
                new NullSession(),
                false,
                name,
                name,
                "00:00:00:00:00:00",
                "ai@local",
                0, 0,
                false, false, false, false, false, false,
                0L, 0, 765, 503,
                null, null
            );
            // Build appearance data so the client can render the bot.
            try {
                bot.getAppearence().resetAppearence();
                com.rs.game.player.content.PlayerLook.randomizeLook(bot.getAppearence());
            } catch (Throwable t) {
                System.err.println("[BotFactory] appearance reset failed for " + name + ": " + t);
            }
            try {
                bot.getAppearence().generateAppearenceData();
            } catch (Throwable t) {
                System.err.println("[BotFactory] appearance gen failed for " + name);
                t.printStackTrace();
            }
            return bot;
        } catch (Throwable t) {
            System.err.println("[BotFactory] Failed to create bot '" + name + "':");
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Create a bot for OFFLINE storage only - does not call Player.init() so it
     * never enters the world. Used by BotPool.generate to create bots destined
     * for disk without flickering in-world.
     */
    public static AIPlayer createOffline(String name) {
        return createOffline(name, null, "random");
    }

    public static AIPlayer createOffline(String name, int[] skillLevels) {
        return createOffline(name, skillLevels, "random");
    }

    public static AIPlayer createOffline(String name, int[] skillLevels, String archetype) {
        try {
            AIPlayer bot = new AIPlayer();

            // Set the displayName via reflection so the bot has identity.
            try {
                java.lang.reflect.Field f = com.rs.game.player.Player.class.getDeclaredField("displayName");
                f.setAccessible(true);
                f.set(bot, name);
            } catch (Throwable ignored) {}

            // Generate appearance bytes so save-blob is complete
            try {
                bot.getAppearence().generateAppearenceData();
            } catch (Throwable ignored) {}

            // Apply skill profile if provided. We can't use Skills.set()/setXp() here
            // because they call refresh() which dereferences player.getPackets() -
            // and the bot has no player wired into Skills yet (no init() called).
            // Write to the underlying arrays directly via reflection.
            if (skillLevels != null) {
                try {
                    com.rs.game.player.Skills sk = bot.getSkills();
                    java.lang.reflect.Field levelField = com.rs.game.player.Skills.class.getDeclaredField("level");
                    java.lang.reflect.Field xpField = com.rs.game.player.Skills.class.getDeclaredField("xp");
                    levelField.setAccessible(true);
                    xpField.setAccessible(true);
                    short[] levelArr = (short[]) levelField.get(sk);
                    double[] xpArr = (double[]) xpField.get(sk);
                    for (int i = 0; i < skillLevels.length && i < 26 && i < levelArr.length; i++) {
                        levelArr[i] = (short) skillLevels[i];
                        xpArr[i] = com.rs.game.player.Skills.getXPForLevel(skillLevels[i]);
                    }
                } catch (Throwable t) {
                    System.err.println("[BotFactory] applying skill profile failed for " + name + ": " + t);
                    t.printStackTrace();
                }
            }

            // Stamp archetype on the bot (persistent identity for AI behavior + equipment)
            // Resolve "random" archetype before setting it on the bot
            String resolvedArchetype = archetype;
            if ("random".equals(archetype)) {
                String[] archetypes = {"melee", "ranged", "magic", "hybrid", "tank", "pure", "main"};
                resolvedArchetype = archetypes[Utils.random(archetypes.length)];
            }
            try { bot.setArchetype(resolvedArchetype); } catch (Throwable ignored) {}
            // Pick a lifetime identity (north-star) for this bot. Drives long-
            // term goal bias - aligned goals get score boost in rankedMethodsFor.
            try {
                bot.setLifetimeIdentity(com.rs.bot.ai.LifetimeIdentity.pickFor(resolvedArchetype));
            } catch (Throwable ignored) {}

            // Apply equipment loadout based on archetype + actual combat level.
            // IMPORTANT: must use resolvedArchetype, not the raw 'archetype'
            // arg - if caller passed "random" the latter is still "random"
            // and BotEquipment falls through to the default melee branch,
            // which means a mage-stat bot ends up in melee gear.
            try {
                int cb = bot.getSkills().getCombatLevel();
                BotEquipment.applyLoadout(bot, resolvedArchetype, cb);
                // Re-generate appearance bytes so the equipment shows up visually
                bot.getAppearence().generateAppearenceData();
            } catch (Throwable t) {
                System.err.println("[BotFactory] equipment apply failed for " + name + ": " + t);
                t.printStackTrace(System.err);
            }

            return bot;
        } catch (Throwable t) {
            System.err.println("[BotFactory] createOffline failed for " + name + ":");
            t.printStackTrace();
            return null;
        }
    }
}
