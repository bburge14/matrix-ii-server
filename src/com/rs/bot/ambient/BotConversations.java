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
        // Combat / training
        {"anyone training combat lately?", "yeah chaeldar tasks all morning"},
        {"slayer task is gargoyles ugh", "always"},
        {"i need 99 attack so bad", "grind it out, only takes a week"},
        {"barrows worth doing still?", "for sure, dharok set is ez money"},
        {"any good drops today?", "got a fury, pretty hyped"},
        {"how do i get to god wars?", "trollheim teleport then north"},
        {"corp beast tonight?", "im in, need 4 more"},
        // Skilling
        {"99 mining incoming", "nice grats early"},
        {"anyone doing fishing?", "lobs at karamja"},
        {"runecrafting is so slow", "abyss is the way"},
        {"farming patches are ready", "running them rn"},
        {"agility tickets stack rn", "barbarian course best xp"},
        {"prayer is so expensive", "wait for ge dragon bones price drop"},
        {"woodcutting yew logs", "ge sells em for 800ea"},
        // GE / economy
        {"did you see ge prices?", "sharks dropped again"},
        {"bots flooded the market", "always do"},
        {"phat prices skyrocketing", "rip my bank"},
        {"buying whip 800k anyone?", "go find a trader bot lol"},
        {"selling 100 yew logs ea", "pst"},
        {"fury cheap rn?", "around 400k yeah"},
        {"market dump after dxp", "predictable"},
        // Social / fashion
        {"love the fashion here", "all about the phats"},
        {"nice cape", "thanks 99 cooking"},
        {"where you get those boots?", "fight pits drop"},
        {"that gear looks clean", "took forever to get"},
        {"clan recruiting btw", "what cb level?"},
        {"thinking of trying pvp", "edgeville is dead, try clan wars"},
        {"who else loves this game", "all of us lol"},
        // Random / banter
        {"hot today huh", "im at the fountain cooling off"},
        {"anyone seen tutor near here?", "south of the bank"},
        {"how long u been playing?", "few years now, feels like forever"},
        {"need a quest helper", "what quest"},
        {"is the wildy worth it?", "for risk pkers, yeah"},
        {"any easy money methods?", "kingdom of miscellania"},
        {"diary rewards op", "easy diary first"},
        {"fc to join?", "look up world clue scrolls"},
        // Minigames
        {"castle wars later?", "down for it"},
        {"soul wars zeal grind", "saving for capes"},
        {"pest control still active?", "yeah but slow tbh"},
        {"barbarian assault xp", "best xp/hr if you have a team"},
        {"stealing creation worth?", "tools are op for skilling"},
        {"fight pits earlier", "obby cape time"},
        {"mage training arena", "just for the cape"},
        // Bosses / pvm
        {"corp tonight?", "im in if we get 4 more"},
        {"king black dragon farm", "easy money + dbones"},
        {"dag kings duo?", "supreme prime rex sure"},
        {"nex spliting?", "got the team yet?"},
        {"vorago tries", "p1 still kills me"},
        {"gwd grind", "sara is the best for solo"},
        {"slayer tower trip", "gargs still meta"},
        // Quests
        {"any easy 99 grind?", "fletching - flying"},
        {"recipe for disaster", "best cooking xp gauntlets"},
        {"while guthix sleeps", "max combat req brutal"},
        {"nomad reborn done?", "rip my prayer"},
        {"questing for cape", "few more to go"},
        // Real talk / lore-ish
        {"jagex when update", "soon tm"},
        {"this server been up long?", "couple years yeah"},
        {"who's the strongest pker", "depends on the day"},
        {"any clan recruiting", "pst me your stats"},
        {"sat at edge for 3 hrs", "no one came"},
        {"dxp weekend incoming", "i can feel it"},
        {"lobby is dead", "always after dxp"},
        // GE / market - more variety
        {"selling 99 fishing", "gz on the cape"},
        {"buying torstols 4k", "sold mine yesterday"},
        {"prices going crazy", "always before holidays"},
        {"got scammed for 50m", "report and forget"},
        {"who buys yew logs?", "fletchers do, ge always"},
        // Gambling / hosting (gambler-flavored)
        {"hot streak today", "let it ride"},
        {"never gambling again", "till next paycheck"},
        {"tilted off 100m", "rebuild grind starts"},
        {"saw a 500m drop", "lucky bastard"},
        // Skiller-flavored
        {"divination is mind numbing", "podcast time"},
        {"rune crafting cape", "3 weeks of abyss"},
        {"farming runs every 5 hours", "set an alarm"},
        {"summoning is so expensive", "war tortoise master"},
        // Casual / random
        {"who else loves agility", "lol no one"},
        {"my pet won't drop", "rng gonna rng"},
        {"got 99 cooking finally", "you been cooking lobs?"},
        {"buying angler outfit", "fishing trawler grind"},
        {"i love this game", "same been playing forever"},
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
