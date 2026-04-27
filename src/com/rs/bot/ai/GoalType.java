package com.rs.bot.ai;

import java.util.*;

/**
 * GoalType - Comprehensive enumeration of all possible bot goals in Matrix RSPS
 * 
 * Organized into 6 main categories with 135 specific goals total.
 * Each goal type contains metadata about requirements, rewards, and logic.
 */
public enum GoalType {
    
    // ========== COMBAT GOALS (25) ==========
    
    // Stat Training Goals (8)
    TRAIN_ATTACK_99("Train Attack to 99", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000, 
                   "skill:attack", 99, "Get 99 Attack for maximum melee damage"),
    TRAIN_STRENGTH_99("Train Strength to 99", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                     "skill:strength", 99, "Get 99 Strength for maximum melee damage"),
    TRAIN_DEFENCE_99("Train Defence to 99", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                    "skill:defence", 99, "Get 99 Defence for maximum survivability"),
    TRAIN_MAGIC_99("Train Magic to 99", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                  "skill:magic", 99, "Get 99 Magic for maximum magic damage"),
    TRAIN_RANGED_99("Train Ranged to 99", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                   "skill:ranged", 99, "Get 99 Ranged for maximum ranged damage"),
    TRAIN_PRAYER_99("Train Prayer to 99", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 1800000,
                   "skill:prayer", 99, "Get 99 Prayer for all prayers and bonuses"),
    TRAIN_SUMMONING_99("Train Summoning to 99", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 1800000,
                      "skill:summoning", 99, "Get 99 Summoning for all familiars"),
    MAX_COMBAT_STATS("Max all combat stats", Goal.GoalCategory.COMBAT, Goal.Priority.URGENT, 7200000,
                    "combat:max", 138, "Achieve maximum combat level"),
    
    // Equipment Tier Goals (12)
    GET_RUNE_ARMOR("Get full Rune armor set", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 300000,
                  "equipment:rune", 40, "Get full rune armor for mid-level combat"),
    GET_DRAGON_ARMOR("Get full Dragon armor set", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 600000,
                    "equipment:dragon", 60, "Get full dragon armor for high-level combat"),
    GET_BARROWS_ARMOR("Get Barrows armor set", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 1200000,
                     "equipment:barrows", 70, "Get full barrows set for elite combat"),
    GET_BANDOS_ARMOR("Get Bandos armor set", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 2400000,
                    "equipment:bandos", 70, "Get full Bandos set - best melee armor"),
    GET_ARMADYL_ARMOR("Get Armadyl armor set", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 2400000,
                     "equipment:armadyl", 70, "Get full Armadyl set - best ranged armor"),
    GET_VOID_ARMOR("Get Void armor set", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 1800000,
                  "equipment:void", 42, "Get Void armor for damage bonuses"),
    GET_FIRE_CAPE("Get Fire Cape", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                 "equipment:firecape", 1, "Complete Fight Cave for Fire Cape"),
    
    // Weapon Goals (5)
    GET_ABYSSAL_WHIP("Get Abyssal Whip", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 1800000,
                    "weapon:whip", 70, "Get Abyssal Whip - best 1h melee weapon"),
    GET_GODSWORD("Get Godsword", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                "weapon:godsword", 75, "Get any Godsword for 2h melee combat"),
    GET_STAFF_OF_LIGHT("Get Staff of Light", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 2400000,
                      "weapon:sol", 75, "Get Staff of Light for magic combat"),
    GET_CHAOTIC_WEAPONS("Get Chaotic weapons", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 7200000,
                       "weapon:chaotic", 80, "Get Chaotic weapons from Dungeoneering"),
    GET_BEST_WEAPONS("Get best-in-slot weapons", Goal.GoalCategory.COMBAT, Goal.Priority.URGENT, 10800000,
                    "weapon:bis", 90, "Get best weapons for all combat styles"),
    
    // ========== SKILLING GOALS (35) ==========
    
    // Individual 99s (23 skills)
    SKILL_WOODCUTTING_99("99 Woodcutting", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 2400000,
                        "skill:woodcutting", 99, "Master the art of tree cutting"),
    SKILL_MINING_99("99 Mining", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 2400000,
                   "skill:mining", 99, "Master the art of ore extraction"),
    SKILL_SMITHING_99("99 Smithing", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 1800000,
                     "skill:smithing", 99, "Master the art of metalworking"),
    SKILL_FISHING_99("99 Fishing", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 2400000,
                    "skill:fishing", 99, "Master the art of fishing"),
    SKILL_COOKING_99("99 Cooking", Goal.GoalCategory.SKILL, Goal.Priority.LOW, 1200000,
                    "skill:cooking", 99, "Master the art of cooking"),
    SKILL_FIREMAKING_99("99 Firemaking", Goal.GoalCategory.SKILL, Goal.Priority.LOW, 900000,
                       "skill:firemaking", 99, "Master the art of firemaking"),
    SKILL_CRAFTING_99("99 Crafting", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 1800000,
                     "skill:crafting", 99, "Master the art of crafting"),
    SKILL_FLETCHING_99("99 Fletching", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 1200000,
                      "skill:fletching", 99, "Master the art of fletching"),
    SKILL_RUNECRAFTING_99("99 Runecrafting", Goal.GoalCategory.SKILL, Goal.Priority.HIGH, 3600000,
                         "skill:runecrafting", 99, "Master the art of rune creation"),
    SKILL_HERBLORE_99("99 Herblore", Goal.GoalCategory.SKILL, Goal.Priority.HIGH, 2400000,
                     "skill:herblore", 99, "Master the art of potion making"),
    SKILL_AGILITY_99("99 Agility", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 3600000,
                    "skill:agility", 99, "Master the art of agility"),
    SKILL_THIEVING_99("99 Thieving", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 1800000,
                     "skill:thieving", 99, "Master the art of thieving"),
    SKILL_SLAYER_99("99 Slayer", Goal.GoalCategory.SKILL, Goal.Priority.HIGH, 4800000,
                   "skill:slayer", 99, "Master the art of slaying monsters"),
    SKILL_FARMING_99("99 Farming", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 2400000,
                    "skill:farming", 99, "Master the art of farming"),
    SKILL_CONSTRUCTION_99("99 Construction", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 2400000,
                         "skill:construction", 99, "Master the art of construction"),
    SKILL_HUNTER_99("99 Hunter", Goal.GoalCategory.SKILL, Goal.Priority.MEDIUM, 2400000,
                   "skill:hunter", 99, "Master the art of hunting"),
    
    // Milestone Goals (8)
    MAX_TOTAL_LEVEL("Max total level (2496)", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.URGENT, 36000000,
                   "achievement:max", 2496, "Achieve maximum total level in all skills"),
    GET_SKILL_CAPE("Get skill cape", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.MEDIUM, 2400000,
                  "achievement:skillcape", 1, "Get any 99 skill cape"),
    GET_TEN_SKILL_CAPES("Get 10 skill capes", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.HIGH, 24000000,
                       "achievement:10capes", 10, "Get 10 different skill capes"),
    GET_ALL_SKILL_CAPES("Get all skill capes", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.URGENT, 48000000,
                       "achievement:allcapes", 23, "Get all possible skill capes"),
    GET_MAX_CAPE("Get Max Cape", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.URGENT, 48000000,
                "achievement:maxcape", 1, "Get the prestigious Max Cape"),
    COMPLETE_ACHIEVEMENT_DIARIES("Complete all Achievement Diaries", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.HIGH, 12000000,
                                "achievement:diaries", 1, "Complete all achievement diaries"),
    TOTAL_LEVEL_1000("1000 total level", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.LOW, 1200000,
                    "achievement:1000total", 1000, "Reach 1000 total level milestone"),
    TOTAL_LEVEL_2000("2000 total level", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.MEDIUM, 12000000,
                    "achievement:2000total", 2000, "Reach 2000 total level milestone"),
    
    // Resource Goals (4)
    MINE_100K_ORES("Mine 100k ores", Goal.GoalCategory.ECONOMIC, Goal.Priority.LOW, 1800000,
                  "resource:ores", 100000, "Mine 100,000 ores for profit"),
    CUT_100K_LOGS("Cut 100k logs", Goal.GoalCategory.ECONOMIC, Goal.Priority.LOW, 1800000,
                 "resource:logs", 100000, "Cut 100,000 logs for profit"),
    CATCH_100K_FISH("Catch 100k fish", Goal.GoalCategory.ECONOMIC, Goal.Priority.LOW, 1800000,
                   "resource:fish", 100000, "Catch 100,000 fish for profit"),
    HARVEST_10K_HERBS("Harvest 10k herbs", Goal.GoalCategory.ECONOMIC, Goal.Priority.MEDIUM, 900000,
                     "resource:herbs", 10000, "Harvest 10,000 herbs for profit"),
    
    // ========== ECONOMIC GOALS (20) ==========
    
    // Wealth Targets (8)
    BUILD_1M_BANK("Build 1M bank", Goal.GoalCategory.ECONOMIC, Goal.Priority.MEDIUM, 600000,
                 "wealth:1m", 1000000, "Accumulate 1 million coins"),
    BUILD_10M_BANK("Build 10M bank", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 1800000,
                  "wealth:10m", 10000000, "Accumulate 10 million coins"),
    BUILD_100M_BANK("Build 100M bank", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 7200000,
                   "wealth:100m", 100000000, "Accumulate 100 million coins"),
    BUILD_1B_BANK("Build 1B bank", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 21600000,
                 "wealth:1b", 1000000000, "Accumulate 1 billion coins"),
    BUILD_10B_BANK("Build 10B bank", Goal.GoalCategory.ECONOMIC, Goal.Priority.URGENT, 72000000,
                  "wealth:10b", 1000000000, "Accumulate 10 billion coins"),
    BUILD_MAX_CASH("Build max cash stack", Goal.GoalCategory.ECONOMIC, Goal.Priority.URGENT, 144000000,
                  "wealth:maxcash", Integer.MAX_VALUE, "Reach maximum cash stack"),
    OWN_FULL_GEAR("Own full high-end gear", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 14400000,
                 "wealth:fullgear", 1, "Own full Bandos + Armadyl + all Barrows"),
    OWN_ALL_RARES("Own every rare item", Goal.GoalCategory.ECONOMIC, Goal.Priority.URGENT, 72000000,
                 "wealth:rares", 1, "Own every rare and discontinued item"),
    
    // Money Making Methods (12)
    MASTER_GE_FLIPPING("Master GE flipping", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 1800000,
                      "moneymaking:geflip", 1000000, "Make 1M+ profit per day from flipping"),
    BOSS_FARMING_PROFIT("Boss farming for drops", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 3600000,
                       "moneymaking:bosses", 10000000, "Farm bosses for consistent profit"),
    HIGH_SLAYER_PROFIT("High-level Slayer profit", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 3600000,
                      "moneymaking:slayer", 5000000, "Use high Slayer for profit"),
    BARROWS_RUNS_PROFIT("Barrows runs for profit", Goal.GoalCategory.ECONOMIC, Goal.Priority.MEDIUM, 1800000,
                       "moneymaking:barrows", 2000000, "Farm Barrows for gear and profit"),
    GWD_FARMING_PROFIT("Godwars farming", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 3600000,
                      "moneymaking:gwd", 10000000, "Farm GWD bosses for rare drops"),
    DRAGON_KILLING_PROFIT("Dragon killing profit", Goal.GoalCategory.ECONOMIC, Goal.Priority.MEDIUM, 1800000,
                         "moneymaking:dragons", 1000000, "Kill dragons for bones and hides"),
    RUNECRAFTING_PROFIT("Runecrafting profit", Goal.GoalCategory.ECONOMIC, Goal.Priority.MEDIUM, 2400000,
                       "moneymaking:rc", 2000000, "Use Runecrafting for profit"),
    HIGH_ALCHEMY_PROFIT("High Alchemy profit", Goal.GoalCategory.ECONOMIC, Goal.Priority.LOW, 900000,
                       "moneymaking:alch", 500000, "Use High Alchemy for profit"),
    PVP_LOOT_PROFIT("PvP for loot", Goal.GoalCategory.ECONOMIC, Goal.Priority.MEDIUM, 1800000,
                   "moneymaking:pvp", 2000000, "PK players for loot and statuettes"),
    CLUE_SCROLL_FARMING("Clue scroll farming", Goal.GoalCategory.ECONOMIC, Goal.Priority.LOW, 1200000,
                       "moneymaking:clues", 1000000, "Farm clue scrolls for rewards"),
    RESOURCE_GATHERING("Resource gathering empire", Goal.GoalCategory.ECONOMIC, Goal.Priority.MEDIUM, 2400000,
                      "moneymaking:resources", 5000000, "Build resource gathering operation"),
    MERCHANT_FLIPPING("Merchant flipping", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 1800000,
                     "moneymaking:merchant", 5000000, "Advanced merchant flipping operations"),
    
    // ========== PVP GOALS (15) ==========
    
    // PvP Builds (8)
    BUILD_PURE_ACCOUNT("Build pure account", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 3600000,
                      "pvpbuild:pure", 1, "Build 1 def pure for PKing"),
    BUILD_RANGE_TANK("Build range tank", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 7200000,
                    "pvpbuild:rtank", 45, "Build range tank (high def, 99 range)"),
    BUILD_MAGIC_TANK("Build magic tank", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 7200000,
                    "pvpbuild:mtank", 45, "Build magic tank (high def, 99 magic)"),
    BUILD_DHAROKS_PURE("Build dharok's pure", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 7200000,
                      "pvpbuild:dharok", 70, "Build dharok's pure for low HP PKing"),
    BUILD_BARROWS_PURE("Build barrows pure", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 7200000,
                      "pvpbuild:barrows", 45, "Build 45 def barrows pure"),
    BUILD_MAIN_ACCOUNT("Build main account", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 14400000,
                      "pvpbuild:main", 126, "Build maxed main for all PvP"),
    BUILD_HYBRID_ACCOUNT("Build hybrid account", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 10800000,
                        "pvpbuild:hybrid", 110, "Build balanced hybrid for all styles"),
    BUILD_DEFENCE_PURE("Build defence pure", Goal.GoalCategory.COMBAT, Goal.Priority.LOW, 3600000,
                      "pvpbuild:defpure", 99, "Build high def, low offence tank"),
    
    // PvP Achievement Goals (7)
    GET_100_KILLS("Get 100 player kills", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 1800000,
                 "pvp:100kills", 100, "Kill 100 players in PvP"),
    GET_1000_KILLS("Get 1000 player kills", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 7200000,
                  "pvp:1000kills", 1000, "Kill 1000 players in PvP"),
    KILL_HIGH_VALUE_PLAYER("Kill high-value player", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 1800000,
                          "pvp:bigkill", 100000000, "Kill player worth 100M+ loot"),
    WIN_10_CONSECUTIVE("Win 10 consecutive fights", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 1200000,
                      "pvp:streak", 10, "Win 10 PvP fights in a row"),
    MASTER_COMBO_EATING("Master combo eating", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 600000,
                       "pvp:combo", 1, "Learn perfect combo eating technique"),
    LEARN_PERFECT_SWITCHING("Learn perfect switching", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 900000,
                           "pvp:switch", 1, "Master gear switching in PvP"),
    GET_PVP_STATUETTES("Get PvP statuettes", Goal.GoalCategory.ECONOMIC, Goal.Priority.HIGH, 3600000,
                      "pvp:statuettes", 1000000000, "Get statuettes worth 1B+"),
    
    // ========== BOSS/PVM GOALS (18) ==========
    
    // Individual Boss Goals (12)
    KILL_KBD_100("Kill KBD 100 times", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 1800000,
                "boss:kbd", 100, "Kill King Black Dragon 100 times"),
    KILL_DKS_50("Kill DKS 50 times each", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
               "boss:dks", 150, "Kill each Dagannoth King 50 times"),
    COMPLETE_100_BARROWS("Complete 100 Barrows runs", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 1800000,
                        "boss:barrows", 100, "Complete 100 Barrows runs"),
    KILL_GRAARDOR_100("Kill Graardor 100 times", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                     "boss:bandos", 100, "Kill General Graardor 100 times"),
    KILL_KREEARRA_100("Kill Kree'arra 100 times", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                     "boss:armadyl", 100, "Kill Kree'arra 100 times"),
    KILL_ZILYANA_100("Kill Zilyana 100 times", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                    "boss:sara", 100, "Kill Commander Zilyana 100 times"),
    KILL_KRIL_100("Kill K'ril 100 times", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                 "boss:zammy", 100, "Kill K'ril Tsutsaroth 100 times"),
    KILL_CORP_50("Kill Corp 50 times", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 7200000,
                "boss:corp", 50, "Kill Corporeal Beast 50 times"),
    KILL_KQ_100("Kill KQ 100 times", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 2400000,
               "boss:kq", 100, "Kill Kalphite Queen 100 times"),
    KILL_CHAOS_ELE_100("Kill Chaos Ele 100 times", Goal.GoalCategory.COMBAT, Goal.Priority.MEDIUM, 1800000,
                      "boss:chaosele", 100, "Kill Chaos Elemental 100 times"),
    COMPLETE_FIGHT_CAVE("Complete Fight Cave", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                       "boss:jad", 1, "Complete TzHaar Fight Cave"),
    KILL_JAD_FIRE_CAPE("Kill Jad for Fire Cape", Goal.GoalCategory.COMBAT, Goal.Priority.HIGH, 3600000,
                      "boss:firecape", 1, "Kill Jad for Fire Cape"),
    
    // Boss Collection Goals (6)
    GET_ALL_GWD_ITEMS("Get all Godwars items", Goal.GoalCategory.COLLECTION, Goal.Priority.HIGH, 14400000,
                     "collection:gwd", 1, "Collect all GWD unique items"),
    GET_ALL_BARROWS_ITEMS("Get all Barrows items", Goal.GoalCategory.COLLECTION, Goal.Priority.HIGH, 7200000,
                         "collection:barrows", 24, "Collect all Barrows items"),
    GET_ALL_DRAGON_ITEMS("Get all dragon items", Goal.GoalCategory.COLLECTION, Goal.Priority.MEDIUM, 3600000,
                        "collection:dragon", 1, "Collect all dragon equipment"),
    GET_ALL_BOSS_PETS("Get all boss pets", Goal.GoalCategory.COLLECTION, Goal.Priority.URGENT, 36000000,
                     "collection:pets", 1, "Collect all boss pets"),
    COMPLETE_BOSS_LOG("Complete boss log", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.URGENT, 72000000,
                     "achievement:bosslog", 1, "Complete entire boss log book"),
    GET_RARE_FROM_ALL_BOSSES("Get rare from every boss", Goal.GoalCategory.COLLECTION, Goal.Priority.HIGH, 21600000,
                            "collection:bossrares", 1, "Get rare drop from every boss"),
    
    // ========== QUEST/ACHIEVEMENT GOALS (12) ==========
    
    COMPLETE_ALL_F2P_QUESTS("Complete all F2P quests", Goal.GoalCategory.QUEST, Goal.Priority.MEDIUM, 1800000,
                           "quest:f2p", 1, "Complete all free-to-play quests"),
    COMPLETE_ALL_P2P_QUESTS("Complete all P2P quests", Goal.GoalCategory.QUEST, Goal.Priority.HIGH, 7200000,
                           "quest:p2p", 1, "Complete all member quests"),
    GET_QUEST_CAPE("Get Quest Cape", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.HIGH, 7200000,
                  "achievement:qcape", 1, "Complete all quests for Quest Cape"),
    COMPLETE_RECIPE_DISASTER("Complete Recipe for Disaster", Goal.GoalCategory.QUEST, Goal.Priority.HIGH, 2400000,
                            "quest:rfd", 1, "Complete Recipe for Disaster quest"),
    COMPLETE_DESERT_TREASURE("Complete Desert Treasure", Goal.GoalCategory.QUEST, Goal.Priority.HIGH, 1800000,
                            "quest:dt", 1, "Complete Desert Treasure for Ancient Magicks"),
    COMPLETE_LUNAR_DIPLOMACY("Complete Lunar Diplomacy", Goal.GoalCategory.QUEST, Goal.Priority.MEDIUM, 1200000,
                            "quest:lunar", 1, "Complete Lunar Diplomacy for Lunar Magicks"),
    GET_BARROWS_GLOVES("Get Barrows gloves", Goal.GoalCategory.QUEST, Goal.Priority.HIGH, 2400000,
                      "quest:bgloves", 1, "Get Barrows gloves from RFD"),
    UNLOCK_ANCIENT_MAGICKS("Unlock Ancient Magicks", Goal.GoalCategory.QUEST, Goal.Priority.HIGH, 1800000,
                          "quest:ancients", 1, "Unlock Ancient Magick spellbook"),
    UNLOCK_LUNAR_MAGICKS("Unlock Lunar Magicks", Goal.GoalCategory.QUEST, Goal.Priority.MEDIUM, 1200000,
                        "quest:lunars", 1, "Unlock Lunar Magick spellbook"),
    COMPLETE_MONKEY_MADNESS("Complete Monkey Madness", Goal.GoalCategory.QUEST, Goal.Priority.MEDIUM, 900000,
                           "quest:mm", 1, "Complete Monkey Madness quest"),
    COMPLETE_ALL_DIARIES("Complete all diaries", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.HIGH, 14400000,
                        "achievement:alldiaries", 1, "Complete all Achievement Diaries"),
    GET_COMPLETIONIST_CAPE("Get Completionist Cape", Goal.GoalCategory.ACHIEVEMENT, Goal.Priority.URGENT, 72000000,
                          "achievement:compcape", 1, "Get Completionist Cape"),
    
    // ========== COLLECTION/RARE GOALS (10) ==========
    
    COLLECT_HOLIDAY_ITEMS("Collect all holiday items", Goal.GoalCategory.COLLECTION, Goal.Priority.MEDIUM, 3600000,
                         "collection:holiday", 1, "Collect all holiday event items"),
    COLLECT_DISCONTINUED_ITEMS("Collect discontinued items", Goal.GoalCategory.COLLECTION, Goal.Priority.HIGH, 14400000,
                              "collection:discontinued", 1, "Collect all discontinued items"),
    OWN_EVERY_WEAPON("Own every weapon type", Goal.GoalCategory.COLLECTION, Goal.Priority.MEDIUM, 7200000,
                    "collection:weapons", 1, "Own every type of weapon"),
    OWN_EVERY_ARMOR("Own every armor type", Goal.GoalCategory.COLLECTION, Goal.Priority.MEDIUM, 7200000,
                   "collection:armor", 1, "Own every type of armor"),
    GET_ALL_SKILLCAPES("Get all skillcapes", Goal.GoalCategory.COLLECTION, Goal.Priority.HIGH, 48000000,
                      "collection:skillcapes", 23, "Get all 23 skill capes"),
    COLLECT_ALL_GOD_BOOKS("Collect all god books", Goal.GoalCategory.COLLECTION, Goal.Priority.LOW, 1800000,
                         "collection:godbooks", 1, "Collect all god books"),
    GET_EVERY_RING("Get every ring type", Goal.GoalCategory.COLLECTION, Goal.Priority.LOW, 3600000,
                  "collection:rings", 1, "Collect every type of ring"),
    OWN_ALL_TELEPORTS("Own all teleport methods", Goal.GoalCategory.COLLECTION, Goal.Priority.LOW, 1800000,
                     "collection:teleports", 1, "Own all teleportation methods"),
    COLLECT_ALL_PETS("Collect all pets", Goal.GoalCategory.COLLECTION, Goal.Priority.URGENT, 72000000,
                    "collection:allpets", 1, "Collect every obtainable pet"),
    BUILD_FULL_POH("Build full POH", Goal.GoalCategory.COLLECTION, Goal.Priority.MEDIUM, 7200000,
                  "collection:poh", 1, "Build POH with all rooms and features");
    
    // ===== Goal Properties =====
    
    private final String description;
    private final Goal.GoalCategory category;
    private final Goal.Priority defaultPriority;
    private final long estimatedTime; // milliseconds
    private final String requirementKey; // e.g. "skill:attack", "wealth:10m"
    private final int requirementValue; // target value
    private final String detailedDescription;
    
    GoalType(String description, Goal.GoalCategory category, Goal.Priority defaultPriority,
            long estimatedTime, String requirementKey, int requirementValue,
            String detailedDescription) {
        this.description = description;
        this.category = category;
        this.defaultPriority = defaultPriority;
        this.estimatedTime = estimatedTime;
        this.requirementKey = requirementKey;
        this.requirementValue = requirementValue;
        this.detailedDescription = detailedDescription;
    }
    
    // ===== Static Goal Categories =====
    
    public static List<GoalType> getCombatGoals() {
        List<GoalType> goals = new ArrayList<>();
        for (GoalType goal : values()) {
            if (goal.category == Goal.GoalCategory.COMBAT) {
                goals.add(goal);
            }
        }
        return goals;
    }
    
    public static List<GoalType> getSkillGoals() {
        List<GoalType> goals = new ArrayList<>();
        for (GoalType goal : values()) {
            if (goal.category == Goal.GoalCategory.SKILL) {
                goals.add(goal);
            }
        }
        return goals;
    }
    
    public static List<GoalType> getEconomicGoals() {
        List<GoalType> goals = new ArrayList<>();
        for (GoalType goal : values()) {
            if (goal.category == Goal.GoalCategory.ECONOMIC) {
                goals.add(goal);
            }
        }
        return goals;
    }
    
    public static List<GoalType> getAchievementGoals() {
        List<GoalType> goals = new ArrayList<>();
        for (GoalType goal : values()) {
            if (goal.category == Goal.GoalCategory.ACHIEVEMENT) {
                goals.add(goal);
            }
        }
        return goals;
    }
    
    public static List<GoalType> getQuestGoals() {
        List<GoalType> goals = new ArrayList<>();
        for (GoalType goal : values()) {
            if (goal.category == Goal.GoalCategory.QUEST) {
                goals.add(goal);
            }
        }
        return goals;
    }
    
    public static List<GoalType> getCollectionGoals() {
        List<GoalType> goals = new ArrayList<>();
        for (GoalType goal : values()) {
            if (goal.category == Goal.GoalCategory.COLLECTION) {
                goals.add(goal);
            }
        }
        return goals;
    }
    
    /**
     * Get goals suitable for a specific bot archetype
     */
    public static List<GoalType> getArchetypeGoals(String archetype) {
        List<GoalType> goals = new ArrayList<>();
        
        switch (archetype.toLowerCase()) {
            case "skiller":
                goals.addAll(getSkillGoals());
                goals.addAll(getEconomicGoals());
                break;
            case "combatant":
                goals.addAll(getCombatGoals());
                goals.addAll(getEconomicGoals());
                break;
            case "pker":
                goals.addAll(Arrays.asList(
                    BUILD_PURE_ACCOUNT, BUILD_MAIN_ACCOUNT, GET_100_KILLS, 
                    GET_1000_KILLS, MASTER_COMBO_EATING, BUILD_10M_BANK
                ));
                break;
            case "bosser":
                goals.addAll(Arrays.asList(
                    KILL_GRAARDOR_100, KILL_KREEARRA_100, KILL_ZILYANA_100,
                    KILL_KRIL_100, COMPLETE_100_BARROWS, GET_ALL_GWD_ITEMS
                ));
                break;
            case "quester":
                goals.addAll(getQuestGoals());
                goals.addAll(getAchievementGoals());
                break;
            case "collector":
                goals.addAll(getCollectionGoals());
                goals.addAll(getEconomicGoals());
                break;
            default: // "balanced" or unknown
                // Add a mix of different goal types
                goals.addAll(Arrays.asList(
                    TRAIN_ATTACK_99, SKILL_WOODCUTTING_99, BUILD_10M_BANK,
                    GET_BARROWS_ARMOR, COMPLETE_ALL_F2P_QUESTS
                ));
        }
        
        return goals;
    }
    
    // ===== Getters =====
    
    public String getDescription() { return description; }
    public Goal.GoalCategory getCategory() { return category; }
    public Goal.Priority getDefaultPriority() { return defaultPriority; }
    public long getEstimatedTime() { return estimatedTime; }
    public String getRequirementKey() { return requirementKey; }
    public int getRequirementValue() { return requirementValue; }
    public String getDetailedDescription() { return detailedDescription; }
    
    @Override
    public String toString() {
        return description;
    }
}
