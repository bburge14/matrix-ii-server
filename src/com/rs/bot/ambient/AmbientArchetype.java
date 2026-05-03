package com.rs.bot.ambient;

import com.rs.utils.Utils;

/**
 * Behavior template for a Citizen-tier bot. Defines:
 *   - Which animations to play in INTERACTING state
 *   - Which chatter strings to randomly speak in IDLE state
 *
 * Archetypes here intentionally trade depth for breadth - we want 8+ visible
 * "personalities" wandering the world to make it look populated, not 2 bots
 * with full economic simulation. The complex AI lives in BotBrain (Legends).
 *
 * New archetypes are added by extending the enum + the per-archetype helper
 * methods. No need to add new switch branches anywhere else; the FSM treats
 * archetypes as opaque animation+chatter providers.
 */
public enum AmbientArchetype {

    // Animation IDs below are only the player emote-tab cosmetics
    // (yes=855, no=856, think=857, bow=858, angry=859, cry=860, laugh=861,
    //  cheer=862, wave=863, beckon=864, clap=865, dance=866). Skiller and
    // combatant archetypes use empty arrays - their REAL action (Mining,
    // Combat, etc) fires its own animation, so a fake overlay would either
    // double up or replace the real one with an inappropriate gesture.

    // === Skiller variants ===
    SKILLER_EFFICIENT("efficient skiller",
        new int[] {},
        new String[] {"99 mining incoming", "this xp/hr though", "let's gooo",
                      "almost 99", "another lvl"}),

    SKILLER_CASUAL("casual skiller",
        new int[] {},
        new String[] {"chill skill day", "just vibing", "no rush", "skilling > pking",
                      "anyone here for the same", "got my podcast going"}),

    SKILLER_NOOB("noob skiller",
        new int[] {},
        new String[] {"how do i level fast?", "wait this xp is slow",
                      "is this still a thing", "what should i train next?",
                      "lol noob here", "first time playing"}),

    // === Combatant variants ===
    COMBATANT_PURE("pure",
        new int[] {},
        new String[] {"any 1v1?", "no def pure here", "pking time",
                      "looking for fights", "wildy soon"}),

    COMBATANT_TANK("tank",
        new int[] {},
        new String[] {"high def life", "tank build", "barely takes damage",
                      "shield > 2h", "def pots stacked"}),

    COMBATANT_HYBRID("hybrid",
        new int[] {},
        new String[] {"hybrid clan", "looking for a tribrid team",
                      "switching styles", "ranger here", "mage build"}),

    // === Socialite variants ===
    SOCIALITE_GAMBLER("gambler",
        new int[] {862, 866, 865, 861},                   // cheer/dance/clap/laugh
        new String[] {"55x2 dicing", "host 65x2", "trusted host", "hot dice",
                      "dicing here", "flower poker"}),

    SOCIALITE_GE_TRADER("ge trader",
        new int[] {863, 864, 857, 855},                   // wave/beckon/think/yes
        new String[] {"buying logs 200ea", "selling sharks 950 each",
                      "pst me", "wts whip", "wtb dragon claws", "ge prices going up"}),

    // Tiered traders - same trade lifecycle as GE_TRADER but with curated
    // catalog + spawn anchor per tier. SKILL = bulk skilling supplies,
    // COMBAT = mid-tier weapons/armor, RARE = high-tier rares.
    SOCIALITE_GE_TRADER_SKILL("ge skill trader",
        new int[] {863, 864, 855},
        new String[] {"selling logs", "wts ores", "raw fish here",
                      "100k bone bundle", "bulk runes", "low low prices"}),

    SOCIALITE_GE_TRADER_COMBAT("ge combat trader",
        new int[] {863, 864, 857, 855},
        new String[] {"selling weapons", "wts armor", "barrows for sale",
                      "dragon weapons here", "rune sets cheap"}),

    SOCIALITE_GE_TRADER_RARE("ge rare trader",
        new int[] {864, 857, 855, 858},
        new String[] {"top tier gear", "endgame weapons", "rares for sale",
                      "godswords here", "premium prices, premium gear"}),

    SOCIALITE_BANKSTAND("bankstander",
        new int[] {863, 862, 866, 858},                   // wave/cheer/dance/bow
        new String[] {"hi", "hello there", "anyone need help?", "cool gear",
                      "where do you train?", "nice cape"}),

    // === Minigamer - Castle Wars (rusher = goes for flag, defender = guards base) ===
    MINIGAMER_CASTLEWARS_RUSHER("CW rusher",
        new int[] {862, 855, 865},
        new String[] {"saradomin team rusher", "going for the flag", "let's gooo",
                      "flag bearer here", "team push north"}),

    MINIGAMER_CASTLEWARS_DEFENDER("CW defender",
        new int[] {857, 856, 862},
        new String[] {"defending the flag", "zammy team here", "watch the gates",
                      "barricade up", "team push south"}),

    // === Minigamer - Soul Wars ===
    MINIGAMER_SOULWARS_RUSHER("SW rusher",
        new int[] {862, 866, 865},
        new String[] {"blue team go go go", "obelisk push", "graveyard rush",
                      "soul fragments incoming"}),

    MINIGAMER_SOULWARS_DEFENDER("SW defender",
        new int[] {857, 856, 862},
        new String[] {"red team defend", "obelisk lock", "guarding the avatar",
                      "watching the graveyard"}),

    // === Minigamer - Stealing Creation ===
    MINIGAMER_STEALINGCREATION_RUSHER("SC rusher",
        new int[] {862, 866, 865},
        new String[] {"red team SC", "harvesting clay", "team rush mid",
                      "scoring resources"}),

    MINIGAMER_STEALINGCREATION_DEFENDER("SC defender",
        new int[] {857, 856, 862},
        new String[] {"blue team SC", "guarding the bin", "depositing materials",
                      "watching the score"}),

    // === Generic minigamer fallback (back-compat for old budget configs) ===
    MINIGAMER_RUSHER("generic rusher",
        new int[] {862, 855, 865},
        new String[] {"any minigame going?", "looking for a team",
                      "in the mood for a minigame"}),

    MINIGAMER_DEFENDER("generic defender",
        new int[] {857, 856, 862},
        new String[] {"any minigame going?", "looking for a team",
                      "watching for some action"});

    public final String label;
    private final int[] interactAnimations;
    private final String[] chatterPool;

    AmbientArchetype(String label, int[] interactAnimations, String[] chatterPool) {
        this.label = label;
        this.interactAnimations = interactAnimations;
        this.chatterPool = chatterPool;
    }

    public int randomInteractAnimation() {
        if (interactAnimations.length == 0) return -1;
        return interactAnimations[Utils.random(interactAnimations.length)];
    }

    public String randomChatter() {
        if (chatterPool.length == 0) return null;
        return chatterPool[Utils.random(chatterPool.length)];
    }

    public boolean isCombatant() {
        return this == COMBATANT_PURE || this == COMBATANT_TANK || this == COMBATANT_HYBRID;
    }

    public boolean isSkiller() {
        return this == SKILLER_EFFICIENT || this == SKILLER_CASUAL || this == SKILLER_NOOB;
    }

    public boolean isSocialite() {
        return this == SOCIALITE_GAMBLER || this == SOCIALITE_GE_TRADER
            || this == SOCIALITE_GE_TRADER_SKILL || this == SOCIALITE_GE_TRADER_COMBAT
            || this == SOCIALITE_GE_TRADER_RARE  || this == SOCIALITE_BANKSTAND;
    }

    /** Any of the trader subtypes (legacy + tiered). */
    public boolean isTrader() {
        return this == SOCIALITE_GE_TRADER
            || this == SOCIALITE_GE_TRADER_SKILL
            || this == SOCIALITE_GE_TRADER_COMBAT
            || this == SOCIALITE_GE_TRADER_RARE;
    }

    /** Trader tier - drives catalog selection in BotTradeHandler.
     *  0 = skill/bulk, 1 = combat, 2 = rare/high-tier, -1 = mixed (legacy). */
    public int traderTier() {
        if (this == SOCIALITE_GE_TRADER_SKILL)  return 0;
        if (this == SOCIALITE_GE_TRADER_COMBAT) return 1;
        if (this == SOCIALITE_GE_TRADER_RARE)   return 2;
        return -1;
    }

    public boolean isMinigamer() {
        return this == MINIGAMER_RUSHER || this == MINIGAMER_DEFENDER
            || this == MINIGAMER_CASTLEWARS_RUSHER || this == MINIGAMER_CASTLEWARS_DEFENDER
            || this == MINIGAMER_SOULWARS_RUSHER || this == MINIGAMER_SOULWARS_DEFENDER
            || this == MINIGAMER_STEALINGCREATION_RUSHER || this == MINIGAMER_STEALINGCREATION_DEFENDER;
    }

    public boolean isCastleWars() {
        return this == MINIGAMER_CASTLEWARS_RUSHER || this == MINIGAMER_CASTLEWARS_DEFENDER;
    }
    public boolean isSoulWars() {
        return this == MINIGAMER_SOULWARS_RUSHER || this == MINIGAMER_SOULWARS_DEFENDER;
    }
    public boolean isStealingCreation() {
        return this == MINIGAMER_STEALINGCREATION_RUSHER || this == MINIGAMER_STEALINGCREATION_DEFENDER;
    }

    /**
     * Lobby tile this archetype should spawn at. Returns null for non-minigame
     * archetypes - those use category anchors set by the spawner caller.
     */
    public com.rs.game.WorldTile lobbyTile() {
        if (isCastleWars())        return new com.rs.game.WorldTile(2442, 3090, 0);
        if (isSoulWars())          return new com.rs.game.WorldTile(2210, 3056, 0);
        if (isStealingCreation())  return new com.rs.game.WorldTile(2860, 5567, 0);
        return null;
    }

    /**
     * GE-area anchor tiles per socialite role. Returns null for non-socialite
     * archetypes. Multi-anchor roles (gambler, bankstand) randomly pick one
     * per call so a batch spawns at varied spots, not stacked on one tile.
     *
     * Coords (per user spec):
     *   Gambler:        3142,3487 (north of rare traders) OR 3163,3489 (fountain)
     *   Bankstander:    one of the 4 GE bank counters
     *   SKILL trader:   3157,3477 (SW counter quadrant)
     *   COMBAT trader:  3157,3477 (same quadrant as skill)
     *   RARE trader:    3147,3472 (NW corner near tree)
     *   Legacy trader:  3164,3486 (default GE center)
     */
    public com.rs.game.WorldTile socialiteAnchor() {
        if (this == SOCIALITE_GAMBLER) {
            return Utils.random(2) == 0
                ? new com.rs.game.WorldTile(3142, 3487, 0)
                : new com.rs.game.WorldTile(3163, 3489, 0);
        }
        if (this == SOCIALITE_BANKSTAND) {
            // 4 GE bank counter clusters - covers all sides of the GE.
            com.rs.game.WorldTile[] counters = new com.rs.game.WorldTile[] {
                new com.rs.game.WorldTile(3165, 3490, 0),  // N counter
                new com.rs.game.WorldTile(3168, 3486, 0),  // E counter
                new com.rs.game.WorldTile(3164, 3482, 0),  // S counter
                new com.rs.game.WorldTile(3160, 3486, 0)   // W counter
            };
            return counters[Utils.random(counters.length)];
        }
        if (this == SOCIALITE_GE_TRADER_SKILL || this == SOCIALITE_GE_TRADER_COMBAT) {
            return new com.rs.game.WorldTile(3157, 3477, 0);
        }
        if (this == SOCIALITE_GE_TRADER_RARE) {
            return new com.rs.game.WorldTile(3147, 3472, 0);
        }
        if (this == SOCIALITE_GE_TRADER) {
            return new com.rs.game.WorldTile(3164, 3486, 0);
        }
        return null;
    }

    public static AmbientArchetype randomFor(String category) {
        if (category == null) return values()[Utils.random(values().length)];
        // Exact enum name (e.g. "SOCIALITE_GE_TRADER_RARE") wins over fuzzy
        // category match - lets CitizenBudget pin a slot to a specific tier.
        try {
            return AmbientArchetype.valueOf(category);
        } catch (IllegalArgumentException ignored) { /* fall through */ }
        switch (category.toLowerCase()) {
            case "skiller":   return new AmbientArchetype[] {SKILLER_EFFICIENT, SKILLER_CASUAL, SKILLER_NOOB}[Utils.random(3)];
            case "combatant": return new AmbientArchetype[] {COMBATANT_PURE, COMBATANT_TANK, COMBATANT_HYBRID}[Utils.random(3)];
            // Mixed socialite category: weighted toward bankstanders + tiered
            // traders (the visible "real" GE crowd) over gamblers/legacy.
            case "socialite": return new AmbientArchetype[] {
                    SOCIALITE_BANKSTAND, SOCIALITE_BANKSTAND,
                    SOCIALITE_GE_TRADER_SKILL, SOCIALITE_GE_TRADER_SKILL,
                    SOCIALITE_GE_TRADER_COMBAT, SOCIALITE_GE_TRADER_COMBAT,
                    SOCIALITE_GE_TRADER_RARE,
                    SOCIALITE_GAMBLER
                }[Utils.random(8)];
            case "trader_skill":  return SOCIALITE_GE_TRADER_SKILL;
            case "trader_combat": return SOCIALITE_GE_TRADER_COMBAT;
            case "trader_rare":   return SOCIALITE_GE_TRADER_RARE;
            case "gambler":       return SOCIALITE_GAMBLER;
            case "bankstand":     return SOCIALITE_BANKSTAND;
            // "minigamer" = legacy generic; pick from generic + per-minigame
            // variants so old budget configs still spawn a mix.
            case "minigamer": return new AmbientArchetype[] {
                    MINIGAMER_RUSHER, MINIGAMER_DEFENDER,
                    MINIGAMER_CASTLEWARS_RUSHER, MINIGAMER_CASTLEWARS_DEFENDER,
                    MINIGAMER_SOULWARS_RUSHER, MINIGAMER_SOULWARS_DEFENDER,
                    MINIGAMER_STEALINGCREATION_RUSHER, MINIGAMER_STEALINGCREATION_DEFENDER
                }[Utils.random(8)];
            // Per-minigame categories so admin panel can spawn specific groups.
            case "castlewars":        return new AmbientArchetype[] {MINIGAMER_CASTLEWARS_RUSHER, MINIGAMER_CASTLEWARS_DEFENDER}[Utils.random(2)];
            case "soulwars":          return new AmbientArchetype[] {MINIGAMER_SOULWARS_RUSHER, MINIGAMER_SOULWARS_DEFENDER}[Utils.random(2)];
            case "stealingcreation":  return new AmbientArchetype[] {MINIGAMER_STEALINGCREATION_RUSHER, MINIGAMER_STEALINGCREATION_DEFENDER}[Utils.random(2)];
            default:          return values()[Utils.random(values().length)];
        }
    }
}
