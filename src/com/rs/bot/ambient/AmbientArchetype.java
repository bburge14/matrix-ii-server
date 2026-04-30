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

    // === Skiller variants ===
    SKILLER_EFFICIENT("efficient skiller",
        new int[] {867, 875, 6702, 11067, 832},          // wc/mining/fish/cook/fletch anims
        new String[] {"99 mining incoming", "this xp/hr though", "let's gooo",
                      "almost 99", "another lvl"}),

    SKILLER_CASUAL("casual skiller",
        new int[] {867, 875, 6702, 832},
        new String[] {"chill skill day", "just vibing", "no rush", "skilling > pking",
                      "anyone here for the same", "got my podcast going"}),

    SKILLER_NOOB("noob skiller",
        new int[] {867, 875, 832},
        new String[] {"how do i level fast?", "wait this xp is slow",
                      "is this still a thing", "what should i train next?",
                      "lol noob here", "first time playing"}),

    // === Combatant variants ===
    COMBATANT_PURE("pure",
        new int[] {422, 423, 451, 7045, 7041},           // basic + ability anims
        new String[] {"any 1v1?", "no def pure here", "pking time",
                      "looking for fights", "wildy soon"}),

    COMBATANT_TANK("tank",
        new int[] {422, 423, 14393, 14418},               // melee + defensive
        new String[] {"high def life", "tank build", "barely takes damage",
                      "shield > 2h", "def pots stacked"}),

    COMBATANT_HYBRID("hybrid",
        new int[] {422, 423, 18177, 14393},               // melee + range + magic
        new String[] {"hybrid clan", "looking for a tribrid team",
                      "switching styles", "ranger here", "mage build"}),

    // === Socialite variants ===
    SOCIALITE_GAMBLER("gambler",
        new int[] {862, 9810},                            // throw + idle
        new String[] {"55x2 dicing", "host 65x2", "trusted host", "hot dice",
                      "dicing here", "flower poker"}),

    SOCIALITE_GE_TRADER("ge trader",
        new int[] {832, 6702},                            // talking-ish
        new String[] {"buying logs 200ea", "selling sharks 950 each",
                      "pst me", "wts whip", "wtb dragon claws", "ge prices going up"}),

    SOCIALITE_BANKSTAND("bankstander",
        new int[] {862, 9810, 1832},                      // wave/cheer/dance
        new String[] {"hi", "hello there", "anyone need help?", "cool gear",
                      "where do you train?", "nice cape"}),

    // === Minigamer variants ===
    MINIGAMER_RUSHER("rusher",
        new int[] {422, 7045},
        new String[] {"castle wars rusher here", "let's flag", "saradomin team",
                      "soul wars next", "in the mood for a minigame"}),

    MINIGAMER_DEFENDER("defender",
        new int[] {422, 14393},
        new String[] {"defending", "watch the flag", "zammy team",
                      "anyone with food?", "pots running low"});

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
        return this == SOCIALITE_GAMBLER || this == SOCIALITE_GE_TRADER || this == SOCIALITE_BANKSTAND;
    }

    public boolean isMinigamer() {
        return this == MINIGAMER_RUSHER || this == MINIGAMER_DEFENDER;
    }

    public static AmbientArchetype randomFor(String category) {
        if (category == null) return values()[Utils.random(values().length)];
        switch (category.toLowerCase()) {
            case "skiller":   return new AmbientArchetype[] {SKILLER_EFFICIENT, SKILLER_CASUAL, SKILLER_NOOB}[Utils.random(3)];
            case "combatant": return new AmbientArchetype[] {COMBATANT_PURE, COMBATANT_TANK, COMBATANT_HYBRID}[Utils.random(3)];
            case "socialite": return new AmbientArchetype[] {SOCIALITE_GAMBLER, SOCIALITE_GE_TRADER, SOCIALITE_BANKSTAND}[Utils.random(3)];
            case "minigamer": return new AmbientArchetype[] {MINIGAMER_RUSHER, MINIGAMER_DEFENDER}[Utils.random(2)];
            default:          return values()[Utils.random(values().length)];
        }
    }
}
