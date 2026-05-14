package com.rs.bot.ambient;

import com.rs.bot.AIPlayer;
import com.rs.game.World;
import com.rs.game.player.Player;
import com.rs.utils.Utils;

/**
 * Lightweight bot-to-bot conversations. Two-line threads only - one bot
 * starts, a nearby bot replies a few seconds later. Both lines come from
 * the SAME thread so it reads as a coherent exchange.
 *
 * Usage from CitizenBrain.tickIdle:
 *   BotConversations.tickConvo(bot);    // emit pending reply if due
 *   BotConversations.maybeStart(bot);   // chance to kick off a new thread
 *
 * Goals:
 *   - Cheap: no World.getPlayers() scan unless we're actually starting one
 *   - Plain text: convo lines use sayBoth(..., false) so no neon effects
 *     (those are reserved for traders/gamblers per user spec)
 *   - Coherent: each pair shares a topic so it doesn't read as gibberish
 */
public final class BotConversations {

    private BotConversations() {}

    /** Two-line conversation thread (call/response). */
    private static final String[][] THREADS = new String[][] {
        // === Combat / training ===
        {"anyone training combat lately?", "yeah chaeldar tasks all morning"},
        {"slayer task is gargoyles ugh", "always"},
        {"i need 99 attack so bad", "grind it out, only takes a week"},
        {"barrows worth doing still?", "for sure, dharok set is ez money"},
        {"any good drops today?", "got a fury, pretty hyped"},
        {"how do i get to god wars?", "trollheim teleport then north"},
        {"corp beast tonight?", "im in, need 4 more"},
        {"got the dharok set today", "lucky, took me months"},
        {"abyssal whip drop?", "from cerberus, way better than slayer tower"},
        {"chinning is too slow lately", "monkey madness 2 fixes it"},
        {"krystilia tasks pay well", "wildy slayer is op"},
        {"what's your slayer master", "duradel always"},
        {"i hate dust devils", "facemask is mandatory"},
        {"pure or main account?", "pure, all about pvp"},
        {"max combat last night", "grats! cape feels good"},
        {"i need turoth task", "blocked mine, hate em"},
        {"avas accumulator broke again", "use elysian assembler"},
        {"is veracs worth grinding", "for prayer slayer yes"},
        {"got d boots from cyclopes", "warriors guild i assume"},
        {"hitting 25s with whip", "scim spec saved me before"},
        // === Skilling ===
        {"99 mining incoming", "nice grats early"},
        {"anyone doing fishing?", "lobs at karamja"},
        {"runecrafting is so slow", "abyss is the way"},
        {"farming patches are ready", "running them rn"},
        {"agility tickets stack rn", "barbarian course best xp"},
        {"prayer is so expensive", "wait for ge dragon bones price drop"},
        {"woodcutting yew logs", "ge sells em for 800ea"},
        {"smithing is dead xp", "blast furnace fixes it"},
        {"thieving sceptre pickpocket", "ardy knights still meta"},
        {"hunter is so chill", "red chins for the gp"},
        {"firemaking 99 was free", "wintertodt is the play"},
        {"crafting is the worst skill", "dragon hides at least pay"},
        {"got 99 cooking via gauntlets", "rfd is the move"},
        {"runespan is too slow tbh", "skip it, do abyss"},
        {"divination annoying me", "best gp/hr cape though"},
        {"fletching 99 in a week", "magic shortbows the whole time?"},
        {"herblore is mandatory for prayer", "yeah, herb runs every 80m"},
        {"making summoning pouches", "yaks > everything"},
        {"living rock mining", "lrc still goated"},
        {"motherlode mine xp", "varrock 4 helps drop rate"},
        // === GE / economy ===
        {"did you see ge prices?", "sharks dropped again"},
        {"bots flooded the market", "always do"},
        {"phat prices skyrocketing", "rip my bank"},
        {"buying whip 800k anyone?", "go find a trader bot lol"},
        {"selling 100 yew logs ea", "pst"},
        {"fury cheap rn?", "around 400k yeah"},
        {"market dump after dxp", "predictable"},
        {"got a 3rd age drop", "wym, drop or ge"},
        {"merching pots today", "buy low sell high"},
        {"got scammed for 50m", "report and forget"},
        {"prices on raw lob", "around 250 each rn"},
        {"who buys nature runes", "everyone, perfect merch item"},
        {"selling 99 fishing", "gz on the cape"},
        {"buying torstols 4k", "sold mine yesterday"},
        {"who buys yew logs?", "fletchers do, ge always"},
        {"ge slot full of offers", "patience is key"},
        {"flipping ranger boots", "1m profit per flip"},
        {"i miss the old ge prices", "everyone does"},
        // === Social / fashion ===
        {"love the fashion here", "all about the phats"},
        {"nice cape", "thanks 99 cooking"},
        {"where you get those boots?", "fight pits drop"},
        {"that gear looks clean", "took forever to get"},
        {"clan recruiting btw", "what cb level?"},
        {"thinking of trying pvp", "edgeville is dead, try clan wars"},
        {"who else loves this game", "all of us lol"},
        {"max cape looks so good", "120 dung is the gate"},
        {"i miss 2007", "we all do friend"},
        {"got my comp cape today", "huge grats man"},
        {"trim comp grind is real", "all music tracks gets me"},
        {"that recolor whip though", "1b at least"},
        {"phat rainbow loadout", "absolutely cracked"},
        {"got a hween mask", "save it, dont alch"},
        {"my fc is dead lately", "make a new one"},
        {"bank tab aesthetic", "important detail"},
        // === Random / banter ===
        {"hot today huh", "im at the fountain cooling off"},
        {"anyone seen tutor near here?", "south of the bank"},
        {"how long u been playing?", "few years now, feels like forever"},
        {"need a quest helper", "what quest"},
        {"is the wildy worth it?", "for risk pkers, yeah"},
        {"any easy money methods?", "kingdom of miscellania"},
        {"diary rewards op", "easy diary first"},
        {"fc to join?", "look up world clue scrolls"},
        {"who has 200m in something", "saw a maxed acc earlier"},
        {"my router crashed mid raid", "feels bad bro"},
        {"first day playing", "welcome to the grind"},
        {"this server feels alive", "more bots than players probably"},
        {"got banned for macroing", "appeal it"},
        {"got my membership today", "p2p is a different game"},
        {"need help with a clue scroll", "step or coord?"},
        {"anyone else play rs3", "no thanks"},
        // === Minigames ===
        {"castle wars later?", "down for it"},
        {"soul wars zeal grind", "saving for capes"},
        {"pest control still active?", "yeah but slow tbh"},
        {"barbarian assault xp", "best xp/hr if you have a team"},
        {"stealing creation worth?", "tools are op for skilling"},
        {"fight pits earlier", "obby cape time"},
        {"mage training arena", "just for the cape"},
        {"clan wars at the gates", "fun fights for hours"},
        {"trouble brewing run", "pirate hat is a must"},
        {"trawler for angler set", "rng but worth"},
        {"warriors guild for d defender", "long grind, worth"},
        {"duel arena gambling", "lost everything once, never again"},
        {"agility pyramid xp", "underrated tbh"},
        {"tzhaar fight cave for cape", "jad is brutal first time"},
        // === Bosses / pvm ===
        {"corp tonight?", "im in if we get 4 more"},
        {"king black dragon farm", "easy money + dbones"},
        {"dag kings duo?", "supreme prime rex sure"},
        {"nex spliting?", "got the team yet?"},
        {"vorago tries", "p1 still kills me"},
        {"gwd grind", "sara is the best for solo"},
        {"slayer tower trip", "gargs still meta"},
        {"zulrah pretty new still", "rotations get easier"},
        {"got a sigil drop", "huge w man"},
        {"trying gwd for the first time", "kc requirement is annoying"},
        {"chaos elemental drops", "dragon 2h drops there i think"},
        {"who solos bandos", "tank gear and prayer flick"},
        {"dks setup", "supreme first, prime with mage"},
        {"jad attempts so far", "23 deaths, still grinding"},
        {"raids tonight?", "got 4 more, need a healer"},
        {"vetion in wildy", "skip it, deep wildy not worth"},
        // === Quests ===
        {"any easy 99 grind?", "fletching - flying"},
        {"recipe for disaster", "best cooking xp gauntlets"},
        {"while guthix sleeps", "max combat req brutal"},
        {"nomad reborn done?", "rip my prayer"},
        {"questing for cape", "few more to go"},
        {"dragon slayer 2 boss", "annoying but doable"},
        {"monkey madness 2 length", "took me 4 hours"},
        {"plague city this morning", "useful for ardy access"},
        {"shilo village quest", "fishing reqs gate it"},
        {"druidic ritual is fast", "5 min and herblore unlocks"},
        {"cooks assistant for prayer", "varrock teleports too"},
        {"the elemental workshop set", "frustrating puzzles"},
        // === Real talk / lore-ish ===
        {"jagex when update", "soon tm"},
        {"this server been up long?", "couple years yeah"},
        {"who's the strongest pker", "depends on the day"},
        {"any clan recruiting", "pst me your stats"},
        {"sat at edge for 3 hrs", "no one came"},
        {"dxp weekend incoming", "i can feel it"},
        {"lobby is dead", "always after dxp"},
        {"polypore eu server", "best of the privates imo"},
        {"new boss when", "always teasing never delivering"},
        {"economy is so weird here", "small server problems"},
        // === Gambling / hosting ===
        {"hot streak today", "let it ride"},
        {"never gambling again", "till next paycheck"},
        {"tilted off 100m", "rebuild grind starts"},
        {"saw a 500m drop", "lucky bastard"},
        {"hosting flowers at edge", "i bet 5m"},
        {"55 x2 hot or cold", "hot all day"},
        {"banned for staking botting", "deserved"},
        {"max hits stake earlier", "1 hit ruined me"},
        // === Skiller-flavored ===
        {"divination is mind numbing", "podcast time"},
        {"rune crafting cape", "3 weeks of abyss"},
        {"farming runs every 5 hours", "set an alarm"},
        {"summoning is so expensive", "war tortoise master"},
        {"99 hp without combat", "pure skiller mode"},
        {"agility ardy course", "graceful set first"},
        {"5 hours straight wcing", "burnt out"},
        {"infernal pickaxe drop", "from a chest somewhere?"},
        // === Casual / random ===
        {"who else loves agility", "lol no one"},
        {"my pet won't drop", "rng gonna rng"},
        {"got 99 cooking finally", "you been cooking lobs?"},
        {"buying angler outfit", "fishing trawler grind"},
        {"i love this game", "same been playing forever"},
        {"any wiki recommendations?", "wiki dot com obv"},
        {"got my first pet drop", "which one"},
        {"max acc goals", "level 200m everything"},
        {"watching osrs streams", "boring without playing"},
        {"4 monitors and 8 accs", "the multilog life"},
        {"i love rng", "till it doesnt go your way"},
        {"music in osrs slaps", "gnome dance my favorite"},
        // === PK chatter ===
        {"any pkers at edge?", "1 def pures roam there"},
        {"lvl 50s for fun fights", "im 60 you down?"},
        {"got skulled at 50 wild", "running for the ditch"},
        {"rune scimmy spec hits", "still relevant in 2024"},
        {"voided range pure", "ele bow is meta still"},
        {"clan war tonight?", "what tier"},
        {"tank brid setup", "max gear except no hp legs"},
        {"d claws spec ko", "old school combo"},
        {"my pure is 1 def 70 att", "hybrid material"},
        {"new pking spot?", "callisto on the way"},
        // === Boss-specific ===
        {"vorkath grind for the day", "350+ kc here"},
        {"draconic visage", "from king black dragon"},
        {"thermy slayer task", "best slayer gold"},
        {"hydra cape req", "95 slayer i think"},
        {"giant mole drop", "nothing but bones tbh"},
        // === Reverse social ===
        {"want to chat?", "sure, what's up"},
        {"new player here", "welcome to the chaos"},
        {"miss anyone", "old friends moved on"},
        {"clan dropped me", "find a new one"},
        // === More mining/fishing/wc combos ===
        {"runite at wildy worth?", "skull risk yeah"},
        {"adamant rocks at mining guild", "always crowded"},
        {"living minerals at chambers", "useful for crafting"},
        {"shooting star landed", "size 9 telegrabs the dust"},
        {"swamp lizards at desert", "hunter is bizarre"},
        {"black salamanders", "rune crafted by them"},
        // === Help / new player ===
        {"how do i get to varrock?", "lumbridge teleport then west"},
        {"where is the bank?", "every major city, look for an arrow"},
        {"how do i wear runes?", "right click equipment slot"},
        {"how do u level?", "kill stuff or skill, xp = level"},
        {"why am i poor", "play more and grind"},
        // === Tradeable banter ===
        {"selling my mil for 500 ea", "decent rate"},
        {"buying 50m from someone", "discord?"},
        {"got an item id", "ge sets price"},
        // === RNG/luck banter ===
        {"i bought a robin hood hat", "8m goal achieved"},
        {"got a dragon med helm drop", "from chaos dwarves?"},
        {"unique drop from mining", "still hyped from the moment"},
    };

    /** How often a casual idle tick rolls into a conversation start.
     *  0.4% per tick = roughly one new thread per 4 minutes per bot,
     *  scaled by population density. */
    private static final double START_PROBABILITY = 0.004;
    /** Cooldown before the same bot can start another thread (ms). */
    private static final long START_COOLDOWN_MS = 90_000;

    /**
     * Roll a chance to start a new conversation thread with a nearby
     * citizen. Caller (CitizenBrain.tickIdle) gates the outer chatter
     * probability so this is already in a "chatty moment".
     */
    public static void maybeStart(AIPlayer bot) {
        if (Math.random() > START_PROBABILITY) return;
        // Cooldown check so a single bot doesn't start back-to-back convos.
        Long lastStart = (Long) bot.getTemporaryAttributtes().get("ConvoLastStartMs");
        long now = System.currentTimeMillis();
        if (lastStart != null && now - lastStart < START_COOLDOWN_MS) return;
        // Don't start if we already have a pending reply queued.
        if (bot.getTemporaryAttributtes().get("ConvoReply") != null) return;

        AIPlayer partner = findNearbyBot(bot);
        if (partner == null) return;
        // Don't queue on a partner that already has a pending reply.
        if (partner.getTemporaryAttributtes().get("ConvoReply") != null) return;

        String[] thread = THREADS[Utils.random(THREADS.length)];
        bot.getTemporaryAttributtes().put("ConvoLastStartMs", now);
        // Starter bot speaks line 0 immediately, plain (no effect).
        BotTradeHandler.sayBoth(bot, thread[0], false);
        // Partner queued to speak line 1 in 2-4 seconds.
        if (thread.length > 1) {
            partner.getTemporaryAttributtes().put("ConvoReply", thread[1]);
            partner.getTemporaryAttributtes().put("ConvoReplyMs",
                now + 2000 + (long)(Math.random() * 2000));
        }
    }

    /**
     * Emit a pending conversation reply if one is due. Called every tick
     * from CitizenBrain.tickIdle so replies fire promptly without needing
     * the bot to be "chatty" itself.
     */
    public static void tickConvo(AIPlayer bot) {
        Long nextMs = (Long) bot.getTemporaryAttributtes().get("ConvoReplyMs");
        if (nextMs == null) return;
        if (System.currentTimeMillis() < nextMs) return;
        String line = (String) bot.getTemporaryAttributtes().get("ConvoReply");
        bot.getTemporaryAttributtes().remove("ConvoReply");
        bot.getTemporaryAttributtes().remove("ConvoReplyMs");
        if (line != null) {
            try { BotTradeHandler.sayBoth(bot, line, false); } catch (Throwable ignored) {}
        }
    }

    /** Find a citizen bot within ~10 tiles to converse with. */
    private static AIPlayer findNearbyBot(AIPlayer bot) {
        AIPlayer best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Player p : World.getPlayers()) {
            if (!(p instanceof AIPlayer)) continue;
            AIPlayer other = (AIPlayer) p;
            if (other == bot) continue;
            if (other.hasFinished()) continue;
            if (other.getPlane() != bot.getPlane()) continue;
            // Citizens only - we don't want Legend bots replying to chat.
            if (!(other.getBrain() instanceof CitizenBrain)) continue;
            int dx = other.getX() - bot.getX();
            int dy = other.getY() - bot.getY();
            int sq = dx*dx + dy*dy;
            if (sq > 100) continue; // 10 tiles
            if (sq < bestDist) {
                bestDist = sq;
                best = other;
            }
        }
        return best;
    }
}
