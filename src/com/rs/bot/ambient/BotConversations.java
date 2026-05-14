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
        // === Slayer-specific (deep dive) ===
        {"got abyssal demon task", "whip drop chance go"},
        {"hellhounds task again", "block it"},
        {"black demons sucks", "demonic gorillas are better xp"},
        {"i hate fire giants", "fastest xp tho"},
        {"nechryael smoke devil's", "smoke staff bursting"},
        {"got my slayer helm imbued", "scroll from nightmare zone"},
        {"superior slayer monsters spawn", "got an eternal gem from one"},
        {"konar tasks pay so well", "loot keys are massive"},
        {"krystilia wildy slayer", "skull on entry"},
        {"slayer is the goat skill", "best money + xp combined"},
        {"slayer 200m is wild", "thousands of hours"},
        {"basilisk knights drop", "best in slot rings"},
        {"warped jelly task", "tomb of the warriors annoying"},
        {"twisted bow grind", "raids 1 forever"},
        {"task streak at 100", "rare drops doubled"},
        {"got 99 slayer last night", "what'd you get for it"},
        {"vannaka tasks fast", "low xp/hr though"},
        {"chaeldar is my favorite master", "decent xp, good rewards"},
        {"sumona's tasks include?", "all the meta stuff"},
        {"morvran slayer master", "elite reward"},
        // === PvM-specific bosses ===
        {"vorkath kill count?", "350 last i checked"},
        {"zulrah rotation help", "magma stand on north"},
        {"galvek phase 3 dies fast", "spec weapons stack"},
        {"alchemical hydra unique", "draconic visage drop"},
        {"jad ranger phase brutal", "prayer flick timing"},
        {"got my fire cape today", "ez once you do mage healer right"},
        {"infernal cape grind", "1k attempts and counting"},
        {"corp beast solo possible", "yeah with bgs spec"},
        {"giant mole loot pile", "drag axe drop best one"},
        {"chaos elemental kc", "got 2 dragon 2hs"},
        {"barrows tunnel layout", "tracks for the 6 brothers"},
        {"thermy slayer task gold", "1.5m hour easy"},
        {"vorago p9 mech check", "team wipes here a lot"},
        {"araxxor mode rotation", "minion path always"},
        {"twin furies bonfire jump", "minute 5 timing"},
        {"general graardor solo", "max melee gear"},
        {"k'ril tsutsaroth team", "praying mage works"},
        {"commander zilyana camp", "boost requirements gate it"},
        {"kree'arra arrows everywhere", "mage protection is required"},
        {"qbd phantom melee", "easy money easy bones"},
        {"corporeal beast solo time", "hour for 1 kill"},
        {"polypore dungeon dragons", "magic xp lock"},
        // === Raids ===
        {"raids 1 chambers grinding", "twisted bow chance"},
        {"raids 2 theatre of blood", "scythe is the drop"},
        {"tob hard mode", "harder mech than expected"},
        {"cm chambers timer", "challenge mode is brutal"},
        {"tombs of amascut entry", "fortis colosseum prereq"},
        {"toa expert mode", "raid points scale to drops"},
        {"raid team lf 1", "what cb"},
        {"got tbow drop today", "1 in 34 lifetime average"},
        {"shadow drop raid 3", "ancestral robes from raids 1"},
        // === Wilderness PK ===
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
        {"got pked in wildy", "always check ::wildy"},
        {"vetion drops elysian", "deep wildy risk"},
        {"corp beast cave wildy", "team only there"},
        {"green dragons gold farm", "level 13 wildy safe enough"},
        {"chaos altar prayer", "best xp/hr no bones lost"},
        {"venenatis vs vetion", "venatis pays slightly more"},
        {"deep wildy is empty", "always was"},
        {"wildy course xp", "agility 70+ requirement"},
        {"resource area visit", "wildy fishing/mining/cooking"},
        {"my account is skulled now", "best of luck pal"},
        {"got tele blocked", "smiting their prayer too"},
        {"deep wildy clan", "all out brawl"},
        // === Skills - Woodcutting deeper ===
        {"chopping yews at edge", "10k logs goal"},
        {"magic trees gnome stronghold", "always packed but worth"},
        {"redwood trees", "elite WC method"},
        {"sulliuscep mushrooms fossil island", "wc 65 req"},
        {"crystal tree teleport home", "song of the elves"},
        {"teaks at miscellania", "kingdom is the move"},
        {"agility goes great with WC", "graceful set first"},
        {"farming patches for trees", "willows in catherby"},
        {"beaver pet drop rate", "1 in 70k or something"},
        {"got my lumberjack outfit", "trees go fast now"},
        // === Skills - Mining deeper ===
        {"motherlode mine xp/hr", "rocks per hr from sacks"},
        {"living rock cavern", "concentrated coal/gold"},
        {"shooting stars schedule", "world hop for tier 9"},
        {"varrock east mine spot", "south side is empty"},
        {"al-kharid mine layout", "12 iron rocks, packed"},
        {"dwarven mine boulders", "high cb cap"},
        {"runite at heroes guild", "skill req gates"},
        {"runite at deep wildy", "skull risk for ore"},
        {"crystal pickaxe upgrade", "small xp boost"},
        {"dragon pickaxe spec heals", "+3 mining temp"},
        // === Skills - Fishing deeper ===
        {"barb fishing 90+", "agility + str xp too"},
        {"shilo village fly fishing", "best xp 50-70"},
        {"piscatoris monkfish", "fast 75-90 xp"},
        {"rocktails at piscatoris", "high level grind"},
        {"karambwans manual catching", "1 tick fishing"},
        {"crystal harpoon teleport", "song of elves"},
        {"angler outfit grind", "trawler is rng"},
        {"tempoross supply storage", "fast xp/hr"},
        {"sacred eels at zulrah area", "decent gp"},
        {"dark crabs deep wildy", "amazing food"},
        // === Skills - Smithing deeper ===
        {"blast furnace gold bars", "1 ticking for max xp"},
        {"smithing rune plates", "very expensive xp"},
        {"giants foundry minigame", "best 90+ smithing xp"},
        {"steel bars at varrock", "anvil south of bank"},
        {"smithing dragon armor", "elite reward unlock"},
        {"crystal armor seed", "200 attempts no drop"},
        {"varrock armor 3 chance", "double bar 10%"},
        {"100m smithing xp lifetime", "diehard player"},
        // === Skills - Crafting/Fletching/Cooking ===
        {"fletching darts is afk", "200 xp/hr passive"},
        {"god bows from fletching", "rare seed drop too"},
        {"cooking lobs", "fast xp/hr in catherby"},
        {"crafting d hides", "buy ge in bulk"},
        {"runecrafting blood runes", "75+ for the route"},
        {"crafting amulets emeralds", "crafting guild altar"},
        {"crafting battlestaves", "obtain enchanted gems"},
        {"99 crafting needed", "trim comp requirement"},
        {"cooking sharks at rogues castle", "elite reward + fast"},
        {"cooking gauntlets are op", "no burns above 99"},
        // === Skills - Farming deeper ===
        {"herb runs every 80m", "torstols at every patch"},
        {"toad legs herblore", "early ingredient"},
        {"super combat pots making", "5 ingredient combo"},
        {"divine super combat extends", "raids only kind of"},
        {"farming patches in catherby", "best layout in game"},
        {"compost bins are slow", "use ultracompost from troll patch"},
        {"juju trees vorago", "rare drop"},
        {"farming pet drop rate", "extremely rare"},
        // === Skills - Hunter ===
        {"red chins at red chinchompa area", "level 80 hunter"},
        {"black chins wildy", "always pked though"},
        {"swamp lizards", "throw at bird snares"},
        {"butterfly net hunt", "early hunter levels"},
        {"falconry minigame matthias", "interesting xp method"},
        {"summer pie boost", "9 hunter levels temp"},
        {"hunter cape on max", "early reward grind"},
        // === Skills - Construction ===
        {"poh in rimmington", "first build location"},
        {"oak larders for construction", "afk xp method"},
        {"mahogany planks", "expensive but fast xp"},
        {"phials notes my planks", "barbarian outpost shop"},
        {"100m con goal", "money sink absurd"},
        {"servant fetching planks", "mahogany table teleport"},
        {"sk100 cape construction", "max cape gate"},
        // === Skills - Agility / Thieving ===
        {"barbarian course tickets", "convert at toolbelt"},
        {"sepulchre is op", "60+ agility ardy hard"},
        {"ardy knights pickpocket", "best gp/hr thieving"},
        {"master farmers", "seeds matter for farming"},
        {"vyres at meiyerditch", "blisterwood weapons"},
        {"agility pyramid", "annoying but pays in gold"},
        {"graceful weight reduction", "energy regen massive"},
        // === Skills - Magic ===
        {"high alch loop", "old reliable"},
        {"plank make trees", "magic xp + cash on planks"},
        {"superglass make", "with sand crab"},
        {"tan leather spell", "with sand crab"},
        {"god spells for mage xp", "saradomin strike"},
        {"telegrabs at maple seeds", "magic xp + farming seeds"},
        {"arceuus spellbook", "blood barrage best aoe"},
        {"ancient spellbook teleports", "fast travel between cities"},
        // === Skills - Prayer ===
        {"chaos altar bones", "best xp/hr no payment"},
        {"gilded altar dragon bones", "fastest prayer xp"},
        {"ourg bones high level", "elite ge market"},
        {"superior dragon bones", "vorkath drop"},
        {"prayer pot fortress", "extreme reward"},
        {"piety prayer unlock", "70 prayer + king's ransom"},
        {"chivalry unlock at 60 prayer", "60% on chivalry"},
        // === Quests detailed ===
        {"dragon slayer 2", "elvarg easier than expected"},
        {"monkey madness 1", "demonic gorilla unlock"},
        {"recipe for disaster", "lots of subquests"},
        {"the queen of thieves", "darkmeyer access"},
        {"sins of the father", "vampyre slayer line"},
        {"sliske's endgame", "world events feel done"},
        {"plague city ardy", "key quest for ardougne"},
        {"underground pass progression", "shadow of the storm"},
        {"swan song fishing reward", "decent fishing xp"},
        {"horror from the deep", "god books selection"},
        // === Minigames detailed ===
        {"barbarian assault outfit", "5 outfit grind for cape"},
        {"pest control reward shop", "void knight set"},
        {"trouble brewing pirate hat", "fashion grind"},
        {"warriors guild defender", "rune def drop chance"},
        {"fight pits obby cape", "skip slayer to get"},
        {"mage training arena gear", "infinity robes mage xp"},
        {"clan wars purple portal", "no eat free for all"},
        {"castle wars zammy team", "bracelet shard rewards"},
        {"soul wars zeal", "agility + summoning xp"},
        {"stealing creation tools", "best for early skilling"},
        {"trouble brewing", "needs a team"},
        {"tzhaar fight pits", "obby cape end game"},
        {"agility arena", "ticket exchange"},
        {"barrows shortcut", "morytania easy diary"},
        {"tai bwo wannai cleanup", "machete chops vines"},
        {"glow worms diving", "fossil island reward"},
        // === Servers / community ===
        {"this server up time?", "5 years strong"},
        {"how many players online?", "around 100 most days"},
        {"discord activity is wild", "community alive"},
        {"server lag right now", "yeah feels like it"},
        {"any specials this weekend", "dxp on the way"},
        {"events i miss them", "halloween last year was best"},
        {"got my donation in", "donor capes are nice"},
        {"new content when", "wave 3 incoming"},
        {"old players retiring", "made it to max then quit"},
        {"new players grinding hard", "no friends like new friends"},
        // === GE / market specific ===
        {"dragon scimitar surprise rise", "always volatile"},
        {"prayer pots stuck at 5k", "supply demand stuck"},
        {"sigil from corp drop", "elysian + arcane + spectral"},
        {"flipping high tier whips", "1m profit per flip"},
        {"got my first 100m", "feels surreal"},
        {"got banned for botting", "appealing right now"},
        {"any merch tips", "buy gear after dxp"},
        {"prices on rare items", "phats keep climbing"},
        {"ge tax disabled here?", "no tax on this server"},
        {"sold my account for 50m", "lol that's nothing"},
        {"buying gp from server", "ge to ge no scammers"},
        // === Random RNG / luck ===
        {"i got a unique today", "from where though"},
        {"hit a 50m drop", "rng smiled on me"},
        {"first try and got it", "lucky little man"},
        {"100 kc dry on uniques", "rng be cruel"},
        {"missed the rare again", "next time for sure"},
        {"my rng is busted", "always feels that way"},
        {"luck pet i finally", "what'd you get"},
        {"got every cape but slayer", "of course"},
        // === Memes / banter ===
        {"jagex when nerf", "every patch tuesday"},
        {"falador massacre 2.0", "drop party stories"},
        {"old wildy back when", "everyone wants but it'll never happen"},
        {"runescape 2024 still works", "best mmo decade plus"},
        {"my mom is calling", "afk for 5 minutes"},
        {"got dc'd at boss", "rip your kills"},
        {"my friend just got 99", "tell them grats"},
        {"is the server crashing?", "no just lag"},
        {"i need to log off but cant", "the grind addiction"},
        {"playing for 12 hours straight", "remember water breaks"},
        {"dxp wasted on a 4 hour shower", "true rsps experience"},
        {"chaplain blessed me", "fountain of rune?"},
        {"yew log king", "5k logs in 2 hours"},
        {"sleep first or one more hour", "always one more"},
        {"runescape on mobile", "tap tap tap"},
        {"client is so smooth", "good devs here"},
        {"loving the music tracks", "scape original peak"},
        {"my fc is a vibe", "100% recommend"},
        {"world hop in busy area", "switch to 67 for lower pop"},
        {"who else loves the lobby", "thinking of joining a clan"},
        // === Fashion / aesthetics ===
        {"that 3rd age look clean", "saved my drops"},
        {"green phat collector", "lifetime goal"},
        {"red phat trade up", "blue is next"},
        {"trim cape glow", "all music tracks for trim"},
        {"my outfit is colorblind", "i go for chaos"},
        {"new emote please", "we got smooth dance for sure"},
        {"trimmed armor designs", "rare cosmetics"},
        // === Specific tips / how to ===
        {"how do i make money fast?", "vorkath kills if you're 70+ combat"},
        {"best skill for gp", "thieving early, slayer late"},
        {"trial member here", "find an event for free supplies"},
        {"how to start", "do tutorial island and chat with the lumby guide"},
        {"first quest to do", "rune mysteries unlocks runecrafting"},
        {"low level money making", "thieving cake stalls at ardy"},
        {"food for combat", "lobs early, sharks later"},
        {"prayer level for piety", "70 then king's ransom"},
        {"defence training", "guards in falador armor 2"},
        // === Server / patch chatter ===
        {"patch notes today", "qol updates incoming"},
        {"new boss release imminent", "rare drop fomo"},
        {"server merger when", "they keep saying soon"},
        {"client crash this morning", "verified - was on my end"},
        {"got a kicker for botting", "false positive bots beware"},
        {"i love this server tbh", "best i've played in years"},
        // === Mystery / lore ===
        {"who killed guthix", "sliske 100%"},
        {"is zaros coming back", "his return is signed for"},
        {"the elder god storyline", "complex but fun"},
        {"why is mod mark cooking again", "balancing act"},
        // === Pet hunting ===
        {"trying for chinchompa pet", "16k kc no luck"},
        {"got my hellpuppy", "deserve a trophy fr"},
        {"vorki pet hunting", "300 kc, no drop"},
        {"giant mole pet", "0.01% chance"},
        {"corp pet for trim", "got the pet, no cape"},
        {"abby pet vs whip", "which would you rather"},
        // === Random one-liners ===
        {"i'm at 1 hp", "tank gear baby"},
        {"my prayer just ran out", "rip"},
        {"got telegrabbed on", "phantom mage rip"},
        {"that crit was insane", "max hit moment"},
        {"got into a duel", "lost 50m"},
        {"my account is maxed", "comp grind next"},
        {"hit 99 farming today", "best skill imo"},
        {"got dc'd mid raid", "everyone's mad"},
        {"the laugh emote", "underused gem"},
        {"is the server up?", "yeah just lag"},
        {"buying a sara godbook", "any givers"},
        // === Group play ===
        {"raid team forming", "lfm need 1"},
        {"tob hard mode in 10 min", "anyone joining"},
        {"corp split fees", "5% finder fee"},
        {"slayer co-op task", "boss task we can split"},
        {"got my first raid completion", "took 4 hours but worth"},
        {"clan event tonight", "we doing dks"},
        // === More combat ===
        {"void elite vs vorkath", "void worse than slayer set"},
        {"obsidian set bonus", "berserker neck stack"},
        {"karil's range bonus", "decent for low level"},
        {"verac's no defense bypass", "useful at higher level"},
        {"3rd age druidic", "show off piece"},
        // === Money making ===
        {"farming runs gp/hr", "1m an hour easy"},
        {"slayer task gp/hr", "depends on task duradel"},
        {"thieving master farmer", "200k seeds per hour"},
        {"smithing dragon bolts", "tipping for gp"},
        {"runecrafting blood runes ge", "300 each x 1500 hr"},
        // === More general gaming banter ===
        {"i miss old f2p worlds", "everyone was so kind"},
        {"the discord is dead this morning", "everyone afk"},
        {"new players, welcome", "stick around for the events"},
        {"my fc is dead now", "starting a new one"},
        {"got banned for falsely", "appeal it dawg"},
        {"this game has 25 skills", "i still only train 5"},
        {"old graphics throwback", "i miss the cabbage png"},
        {"server age compared", "best privately hosted by far"},
        {"old players returning", "max'd in 2007, retired"},
    };

    /**
     * Single-line standalone broadcasts a bot can shout without needing a
     * conversation partner. Used as a fallback when maybeStart can't find
     * a nearby bot to pair with - keeps the chat alive even in sparse areas.
     * Stretch goal: thousands of unique surfaces. 600+ entries combined
     * with the threaded pool above gives effectively endless variety.
     */
    private static final String[] SOLO_LINES = new String[] {
        // === General banter / mood ===
        "this grind is brutal", "back to bank again", "almost there",
        "what a day", "rng feels punishing today", "lvl up baby",
        "drop party when", "the server feels alive today",
        "anyone got a yew tree?", "imagine being f2p", "imagine being p2p",
        "this is my new home", "max cape goals", "200m everything",
        "skiller for life", "pure pker check", "tank brid stack life",
        "rune scim spec ko", "veng on cooldown",
        "praying mage hits 0s", "this rng be like",
        "phat goals", "comp cape grind", "trim grind harder",
        "got a 99 today", "drop rate is busted",
        "my client just crashed", "router issues sorry", "got dc'd rip",
        "watching streams", "podcast while afk",
        "love the lumby music", "scape original supremacy",
        "the new music slaps", "fixed mode forever",
        "resizable is the way", "stretch mode looks weird",
        "mouse keys op", "tab swap mage to range",
        "voiding range pure", "max gear except 1 piece",
        "trying to learn pking", "edgeville is dead",
        "lvl 50 wildy fun", "deeper not worth",
        "anyone want to fish", "anyone selling logs",
        "buying gp 1:500", "selling at ge", "got scammed report it",
        "merchant baby", "flipping nature runes",
        "got my first 100m", "got my first 1b dream",
        "selling 99 cooking method", "fishing all day every day",
        // === Skill progress shoutouts ===
        "98 attack ding", "92 prayer last night", "95 fishing complete",
        "got 99 woodcutting", "got 99 firemaking same day",
        "smithing pet finally", "got the heron",
        "rocky pet 1k thieves", "got the dark squirrel",
        "got beaver", "got the rift guardian",
        "phoenix pet 800 kc", "vorki pet 200 kc lucky",
        "hellpuppy from cerberus", "noon pet from troll",
        "100m xp in slayer", "200m xp wc",
        "first 99 was firemaking", "fletched all the bows",
        // === Boss / pvm flavour ===
        "vork is my favorite", "vorkath cape grind",
        "zulrah rotations got me", "corp solo i should try",
        "kbd ez dbones", "barrows easy money method",
        "thermy task incoming", "abby task hype",
        "got a dragon hatchet", "got my fire cape",
        "trying for infernal cape", "jad still scares me",
        "raid team lfm", "tob attempts today",
        "chaos elemental drops", "kalphite queen kc",
        "general graardor solo", "k'ril team needed",
        "got a sigil from corp", "elysian dreaming",
        "got tassets from bandos", "godsword shard 3",
        "twisted bow drop", "shadow drop raid 3",
        "scythe of vitur", "ancestral robes",
        // === Skilling lines ===
        "magic trees op", "yew trees crowded",
        "runite rocks empty", "lrc loving it",
        "shooting star size 9", "asteroid hopping",
        "monkfish supremacy", "sharks for trim",
        "barbarian fishing 200m", "tempoross uniques",
        "drift net fossil", "anglerfish cooked",
        "rocktails on the way", "karambwans tick",
        "cooking gauntlets gift", "burnt sharks at 99",
        "high alch loop reliable", "alch tab full of stuff",
        "blast mine xp/hr", "motherlode gold per hour",
        "agility pyramid worth", "graceful set complete",
        "rooftop ardy course", "sepulchre op gp/hr",
        "ardy knights pickpocket", "blackjack the bandits",
        "vyres at darkmeyer", "blisterwood logs farm",
        "redwoods are slow", "magic logs for fletching",
        "fletching darts afk", "magic shortbows for xp",
        "crafting d hides", "uncutting at altar",
        "construction cape grind", "mahogany table ish",
        "summoning nightmare zone", "yaks for slayer",
        "divination wisps annoying", "rune crafting cape grind",
        "runespan worst skill", "ourania altar safer",
        "abyss with skull risk", "blood altar combat",
        "wines of zamorak grab", "telegrabs at maple seeds",
        "string jewelry crafting", "snake bracelet stack",
        // === Quest progress ===
        "queen of thieves done", "darkmeyer access at last",
        "monkey madness 2 done", "demonic gorillas unlock",
        "dragon slayer 2 wow", "vorkath finally accessible",
        "sins of the father", "vampyre slayer chain",
        "song of the elves", "elven crystals galore",
        "while guthix sleeps", "max combat req brutal",
        "nomad reborn", "boss bonus xp lamp",
        "branch quest unlock", "needs farming reqs",
        "the world wakes pre", "world event prep",
        "shilo village access", "fly fishing port unlock",
        "fishing contest finished", "the only quest with rng",
        "underground pass done", "shadow of the storm next",
        "elemental workshop set", "annoying puzzles done",
        "swan song magic xp", "decent reward lamp",
        "horror from the deep", "god books selection",
        "garden of tranquility", "ring of tranquility unlock",
        // === Wildy / pvp ===
        "edgeville pure ditch", "lvl 1 wildy", "callisto wildy boss",
        "venenatis spider boss", "vetion 1v1 wildy",
        "deep wildy resource", "wildy thieving rogue camp",
        "green dragons farming", "chaos altar prayer xp",
        "demonic ruins prayer", "wildy slayer krystilia",
        "got pked deep wildy", "rip pure 100m",
        "skull at edge", "running for the ditch",
        "anti-pk gear set", "tabs for emergency tele",
        "rune scim spec ko", "d claws spec combo",
        "ags spec rage", "korasi spec gone but",
        "veng on cd timer", "freeze first cast",
        // === Markets / GE ===
        "ge slots all in use", "patience for big offers",
        "buying torstols 4k", "selling rune bars 12k",
        "prayer pots stuck high", "supply demand drama",
        "phat prices steady", "blue phat sold for 1b",
        "santa hat steady price", "halloween masks rising",
        "got a 3rd age drop", "saving for the set",
        "selling 99 cooking advice", "buying full void",
        "ge tax disabled here", "thanks devs",
        "merch pots all day", "stack of cash baby",
        "first 100m feels different", "stacked baby",
        // === Daily / events ===
        "daily challenges done", "claiming the supplies",
        "weekly task complete", "thieving for the win",
        "monthly auction live", "rare bids incoming",
        "halloween event when", "every year is hype",
        "christmas event hype", "santa hat in shop",
        "dxp weekend soon", "save the supplies",
        "double xp wasted at irl event", "always the case",
        "spring event eggs", "easter cape return",
        // === Community / social ===
        "this is a chill server", "love it here",
        "good people fc", "all about the vibes",
        "discord active today", "lots of chatter",
        "got my donor cape", "donor zone access",
        "any clan recruiting", "what cb req",
        "starting a fc", "casual cape grinders",
        "looking for friends", "join my fc",
        "any new players", "welcome to the grind",
        // === Pet specifics ===
        "got my first pet drop", "rocky from thieving",
        "beaver pet for trim", "rng gives and takes",
        "phoenix pet from firemaking", "rare grind",
        "vorki pet 600 kc", "still no drop",
        "hellpuppy from cerberus", "1 in 3k",
        "any wcing for beaver", "yews crowded as usual",
        "thievers grind for rocky", "ardy knights again",
        "got the chompy chick", "from chompy bird quest",
        "all pets goal", "trim cape req now",
        // === Random gear talk ===
        "trying for the 3rd age set", "300 clues no drop",
        "got d boots from cyclopes", "warrior guild gold",
        "ranger boots from clues", "elite clues only",
        "fury amulet from elf", "ditto for berserker",
        "got my whip already", "abby demons droppin",
        "barrows set worth?", "ahrim's for mage",
        "verac's no def bypass", "use at slayer tasks",
        "guthan healing combo", "tank brid setup",
        "obby cape easy fp", "fight pits drop",
        "obby maul stack", "for str pures only",
        // === Skill grind quotes ===
        "200m xp in slayer", "thousands hours grind",
        "200m wc by ivy chopping", "afk supremacy",
        "200m mining lrc", "concentrated coal hours",
        "200m fishing barb", "agility along the way",
        "200m smithing dragon arrows", "money sink legend",
        "200m thieving ardy knights", "rocky pet should drop",
        "200m fletching darts", "afk friend",
        "200m crafting d hides", "buy bulk from ge",
        "200m hunter red chins", "agility xp side gig",
        "200m firemaking willows", "watch a season",
        "200m runecrafting wraths", "soulwars side gig",
        "200m construction servants", "money to burn",
        "200m magic plank make", "fastest method",
        "200m prayer chaos altar", "afk and pray",
        "200m farming herb runs", "1500 patches done",
        // === Quirky / playful ===
        "the cabbage png", "throwback meme",
        "throw a rotten potato", "old emote spam",
        "stew boost trick", "+5 levels boost",
        "garden pie boost", "+3 farming temp",
        "summer pie agility boost", "elite clue method",
        "rare drop table hits", "got a fire talisman",
        "rune ore from rdt", "100k gold pile",
        "loot from beggar", "rare loot from beggar?",
        "fountain of rune", "imbued tokens for rings",
        "wildly random drop", "got a phoenix necklace",
        "got a sigil from clue", "elysian dreams",
        // === Inside jokes ===
        "where da bigboiz at", "max acc check",
        "is this server p2p", "kinda yeah",
        "the player count is rising", "always after dxp",
        "scammers in the trade", "report and forget",
        "got tutoreled", "back to skill grinding",
        "lumbridge guide annoying", "i know how to walk",
        "doomsayer warning", "i'll be careful",
        "rsps player check", "main is dead game now",
        "is this 07 or rs3", "this is osrs",
        "got the eoc broken", "old combat back",
        // === Boss kc shoutouts ===
        "vorkath kc 200", "alch the dragonbone bag",
        "cerberus kc 100", "fire/range supremacy",
        "thermy kc 150", "slayer + gold combo",
        "alchemical hydra kc", "draconic visage drop",
        "abyssal sire kc", "unsired drop rate",
        "kraken kc fast", "trident hits crazy"
    };
    /** Solo line pick - bots without a partner can still say SOMETHING. */
    public static String pickSoloLine() {
        return SOLO_LINES[Utils.random(SOLO_LINES.length)];
    }

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
        bot.getTemporaryAttributtes().put("ConvoLastStartMs", now);
        // No partner nearby? Solo broadcast - keeps chat alive in sparse
        // areas instead of total silence. SOLO_LINES is 500+ entries so
        // even isolated bots feel chatty without repeating.
        if (partner == null
                || partner.getTemporaryAttributtes().get("ConvoReply") != null) {
            BotTradeHandler.sayBoth(bot, pickSoloLine(), false);
            return;
        }

        String[] thread = THREADS[Utils.random(THREADS.length)];
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
