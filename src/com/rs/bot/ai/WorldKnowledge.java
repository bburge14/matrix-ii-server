package com.rs.bot.ai;
import java.util.*;

public class WorldKnowledge {
    
    // ===== MAJOR TOWNS & CITIES =====
    public static final Map<String, int[]> TOWNS = new HashMap<String, int[]>() {{
        // Free-to-Play Towns
        put("LUMBRIDGE", new int[]{3222, 3218});
        put("DRAYNOR_VILLAGE", new int[]{3093, 3244});
        put("VARROCK", new int[]{3213, 3424});
        put("FALADOR", new int[]{2965, 3378});
        put("BARBARIAN_VILLAGE", new int[]{3081, 3421});
        put("EDGEVILLE", new int[]{3087, 3496});
        put("AL_KHARID", new int[]{3293, 3174});
        
        // Members Towns
        put("SEERS_VILLAGE", new int[]{2708, 3484});
        put("CATHERBY", new int[]{2804, 3434});
        put("ARDOUGNE_EAST", new int[]{2662, 3305});
        put("ARDOUGNE_WEST", new int[]{2529, 3305});
        put("YANILLE", new int[]{2606, 3093});
        put("BRIMHAVEN", new int[]{2758, 3178});
        put("SHILO_VILLAGE", new int[]{2852, 3033});
        put("CANIFIS", new int[]{3493, 3487});
        put("POLLNIVNEACH", new int[]{3359, 2975});
        put("MENAPHOS", new int[]{3200, 2700});
        put("RELLEKKA", new int[]{2670, 3631});
        put("FISHING_GUILD", new int[]{2611, 3393});
        put("GUILD_CITY", new int[]{2916, 3176}); // Legends Guild area
    }};
    
    // ===== BANKING LOCATIONS =====
    public static final Map<String, int[]> BANKS = new HashMap<String, int[]>() {{
        // F2P Banks
        put("LUMBRIDGE_BANK", new int[]{3094, 3493});
        put("DRAYNOR_BANK", new int[]{3092, 3243});
        put("VARROCK_EAST_BANK", new int[]{3253, 3420});
        put("VARROCK_WEST_BANK", new int[]{3185, 3436});
        put("FALADOR_EAST_BANK", new int[]{3013, 3355});
        put("FALADOR_WEST_BANK", new int[]{2946, 3368});
        put("EDGEVILLE_BANK", new int[]{3094, 3491});
        put("AL_KHARID_BANK", new int[]{3269, 3167});
        
        // P2P Banks
        put("SEERS_BANK", new int[]{2724, 3493});
        put("CATHERBY_BANK", new int[]{2808, 3441});
        put("ARDOUGNE_NORTH_BANK", new int[]{2618, 3332});
        put("ARDOUGNE_SOUTH_BANK", new int[]{2655, 3283});
        put("YANILLE_BANK", new int[]{2612, 3094});
        put("CANIFIS_BANK", new int[]{3512, 3480});
        put("SHILO_BANK", new int[]{2852, 3033});
        put("RELLEKKA_BANK", new int[]{2612, 3618});
        put("GRAND_EXCHANGE", new int[]{3164, 3487});
    }};
    
    // ===== WOODCUTTING LOCATIONS =====
    public static final Map<String, int[]> WOODCUTTING = new HashMap<String, int[]>() {{
        // Regular Trees
        put("LUMBRIDGE_TREES", new int[]{3096, 3468});
        put("DRAYNOR_TREES", new int[]{3101, 3263});
        put("VARROCK_TREES", new int[]{3280, 3424});
        put("FALADOR_TREES", new int[]{3034, 3341});
        
        // Willow Trees
        put("DRAYNOR_WILLOWS", new int[]{3087, 3234});
        put("LUMBRIDGE_WILLOWS", new int[]{3160, 3251});
        put("BARBARIAN_WILLOWS", new int[]{3109, 3436});
        put("RIMMINGTON_WILLOWS", new int[]{2987, 3229});
        put("PORT_SARIM_WILLOWS", new int[]{3058, 3251});
        
        // Oak Trees  
        put("VARROCK_OAKS", new int[]{3280, 3420});
        put("FALADOR_OAKS", new int[]{3034, 3341});
        put("DRAYNOR_OAKS", new int[]{3101, 3251});
        
        // Maple Trees
        put("SEERS_MAPLES", new int[]{2730, 3502});
        put("TREE_GNOME_MAPLES", new int[]{2540, 3170});
        
        // Yew Trees
        put("LUMBRIDGE_YEWS", new int[]{3087, 3468});
        put("FALADOR_YEWS", new int[]{2995, 3315});
        put("EDGEVILLE_YEWS", new int[]{3087, 3481});
        put("SEERS_YEWS", new int[]{2713, 3463});
        put("RIMMINGTON_YEWS", new int[]{2942, 3229});
        
        // Magic Trees
        put("TREE_GNOME_MAGIC", new int[]{2692, 3425});
        put("SORCERERS_TOWER_MAGIC", new int[]{2705, 3397});
    }};
    
    // ===== MINING LOCATIONS =====
    public static final Map<String, int[]> MINING = new HashMap<String, int[]>() {{
        // F2P Mines
        put("LUMBRIDGE_MINE", new int[]{3149, 3148});
        put("VARROCK_EAST_MINE", new int[]{3289, 3372});
        put("VARROCK_WEST_MINE", new int[]{3175, 3364});
        put("FALADOR_MINE", new int[]{3034, 9556}); // Underground
        put("DRAYNOR_CLAY", new int[]{3142, 3305});
        put("RIMMINGTON_MINE", new int[]{2978, 3239});
        put("AL_KHARID_MINE", new int[]{3299, 3315});
        put("BARBARIAN_VILLAGE_MINE", new int[]{3082, 3420});
        
        // P2P Mines  
        put("MINING_GUILD", new int[]{3046, 9756}); // Underground
        put("COAL_TRUCK_MINE", new int[]{2579, 3477});
        put("ARDOUGNE_MONASTERY_MINE", new int[]{2606, 3222});
        put("YANILLE_MINE", new int[]{2632, 3146});
        put("RELLEKKA_MINE", new int[]{2677, 3702});
        put("SHILO_MINE", new int[]{2824, 2996});
        
        // Special Ores
        put("HEROES_GUILD_MINE", new int[]{2916, 3506}); // Adamant
        put("WILDERNESS_RUNITE", new int[]{3058, 3884}); // Runite
    }};
    
    // ===== FISHING LOCATIONS =====
    public static final Map<String, int[]> FISHING = new HashMap<String, int[]>() {{
        // F2P Fishing
        put("LUMBRIDGE_RIVER", new int[]{3238, 3241}); // Shrimp/Anchovies
        put("DRAYNOR_RIVER", new int[]{3105, 3251}); // Shrimp/Anchovies  
        put("BARBARIAN_VILLAGE_RIVER", new int[]{3109, 3436}); // Trout/Salmon
        put("AL_KHARID_RIVER", new int[]{3269, 3146}); // Shrimp/Anchovies
        put("KARAMJA_DOCK", new int[]{2924, 3178}); // Lobster/Swordfish
        
        // P2P Fishing
        put("CATHERBY_BEACH", new int[]{2837, 3434}); // Lobster/Swordfish
        put("FISHING_GUILD", new int[]{2611, 3393}); // Lobster/Swordfish/Shark
        put("RELLEKKA_DOCK", new int[]{2632, 3693}); // Various
        put("SHILO_RIVER", new int[]{2859, 3018}); // Karambwan
        put("OTTO_BARBARIAN", new int[]{2501, 3508}); // Barbarian Fishing
    }};
    
    // ===== COMBAT TRAINING =====
    public static final Map<String, int[]> COMBAT_AREAS = new HashMap<String, int[]>() {{
        // Low Level (1-20)
        put("LUMBRIDGE_COWS", new int[]{3257, 3266});
        put("LUMBRIDGE_GOBLINS", new int[]{3247, 3245});
        put("LUMBRIDGE_RATS", new int[]{3202, 3209});
        
        // Medium Level (20-60)  
        put("VARROCK_GUARDS", new int[]{3210, 3379});
        put("FALADOR_GUARDS", new int[]{2967, 3343});
        put("AL_KHARID_WARRIORS", new int[]{3295, 3189});
        put("BARBARIAN_VILLAGE", new int[]{3081, 3421});
        
        // High Level (60+)
        put("ROCK_CRABS", new int[]{2673, 3709});
        put("EXPERIMENTS", new int[]{3563, 9946});
        put("SLAYER_TOWER", new int[]{3428, 3537});
        put("BRIMHAVEN_DUNGEON", new int[]{2709, 9564});
        
        // Wilderness
        put("EDGEVILLE_DUNGEON", new int[]{3097, 9867});
        put("CHAOS_DRUIDS", new int[]{3252, 3401});
    }};
    
    // ===== SHOPS =====
    public static final Map<String, int[]> SHOPS = new HashMap<String, int[]>() {{
        // General Stores
        put("LUMBRIDGE_GENERAL", new int[]{3212, 3247});
        put("DRAYNOR_GENERAL", new int[]{3079, 3249});
        put("VARROCK_GENERAL", new int[]{3216, 3414});
        put("FALADOR_GENERAL", new int[]{2955, 3390});
        
        // Weapon Shops
        put("VARROCK_SWORD_SHOP", new int[]{3201, 3403});
        put("FALADOR_WEAPON_SHOP", new int[]{2973, 3387});
        put("AL_KHARID_SCIMITAR", new int[]{3283, 3192});
        
        // Magic Shops
        put("VARROCK_MAGIC_SHOP", new int[]{3253, 3401});
        put("DRAYNOR_MAGIC_SHOP", new int[]{3079, 3249});
        
        // Food Shops
        put("PORT_SARIM_FOOD", new int[]{3012, 3208});
        put("CATHERBY_FOOD", new int[]{2817, 3443});
    }};
    
    // ===== QUEST LOCATIONS =====
    public static final Map<String, int[]> QUEST_LOCATIONS = new HashMap<String, int[]>() {{
        put("COOKS_ASSISTANT", new int[]{3207, 3214}); // Lumbridge Castle
        put("SHEEP_SHEARER", new int[]{3190, 3272}); // Fred's Farm
        put("ROMEO_JULIET", new int[]{3211, 3424}); // Varrock Square
        put("DEMON_SLAYER", new int[]{3204, 3392}); // Gypsy Aris
        put("DRAGON_SLAYER", new int[]{2834, 3335}); // Oziach
        put("KNIGHTS_SWORD", new int[]{2977, 3344}); // Thurgo
        put("PIRATES_TREASURE", new int[]{3054, 3245}); // Redbeard Frank
        put("VAMPIRE_SLAYER", new int[]{3097, 3266}); // Morgan
    }};
    
    // ===== TELEPORT DESTINATIONS =====
    public static final Map<String, int[]> TELEPORT_SPOTS = new HashMap<String, int[]>() {{
        // Standard Spellbook
        put("LUMBRIDGE_TELEPORT", new int[]{3222, 3218});
        put("VARROCK_TELEPORT", new int[]{3213, 3424});
        put("FALADOR_TELEPORT", new int[]{2965, 3378});
        put("CAMELOT_TELEPORT", new int[]{2757, 3477});
        put("ARDOUGNE_TELEPORT", new int[]{2662, 3305});
        
        // Ancient Spellbook  
        put("PADDEWWA_TELEPORT", new int[]{3098, 9884});
        put("SENNTISTEN_TELEPORT", new int[]{3322, 3338});
        put("KHARYRLL_TELEPORT", new int[]{3492, 3471});
        put("LASSAR_TELEPORT", new int[]{3006, 3471});
        put("DAREEYAK_TELEPORT", new int[]{2966, 3696});
        put("CARRALLANGAR_TELEPORT", new int[]{3156, 3666});
        put("ANNAKARL_TELEPORT", new int[]{3288, 3886});
        put("GHORROCK_TELEPORT", new int[]{2977, 3873});
        
        // Lunar Spellbook
        put("MOONCLAN_TELEPORT", new int[]{2113, 3915});
        put("TELE_GROUP_MOONCLAN", new int[]{2113, 3915});
        put("OURANIA_TELEPORT", new int[]{2469, 3245});
        put("WATERBIRTH_TELEPORT", new int[]{2544, 3757});
        
        // Jewelry Teleports
        put("RING_OF_DUELING_PVP", new int[]{3315, 3235}); // Al Kharid Duel Arena
        put("RING_OF_DUELING_CASTLE_WARS", new int[]{2440, 3090});
        put("GAMES_NECKLACE_BURTHORPE", new int[]{2898, 3551});
        put("GAMES_NECKLACE_BARBARIAN", new int[]{2519, 3570});
        put("GAMES_NECKLACE_CORPOREAL", new int[]{2966, 4382});
        put("GLORY_EDGEVILLE", new int[]{3087, 3496});
        put("GLORY_KARAMJA", new int[]{2918, 3176});
        put("GLORY_DRAYNOR", new int[]{3105, 3251});
        put("GLORY_AL_KHARID", new int[]{3293, 3163});
    }};
    
    // ===== UTILITY METHODS =====
    
    public static String getCurrentArea(int x, int y) {
        String closestArea = "UNKNOWN";
        double minDistance = Double.MAX_VALUE;
        
        // Check all towns
        for (Map.Entry<String, int[]> town : TOWNS.entrySet()) {
            int[] coords = town.getValue();
            double distance = Math.sqrt(Math.pow(x - coords[0], 2) + Math.pow(y - coords[1], 2));
            if (distance < minDistance && distance < 100) { // Within 100 tiles
                minDistance = distance;
                closestArea = town.getKey();
            }
        }
        
        // Special areas
        if (x >= 3072 && x <= 3328 && y >= 3648 && y <= 3968) return "WILDERNESS";
        if (x >= 2816 && x <= 2944 && y >= 3136 && y <= 3200) return "KARAMJA";
        if (x >= 2304 && x <= 2752 && y >= 4416 && y <= 4864) return "PEST_CONTROL";
        
        return closestArea;
    }
    
    public static int[] findNearestLocation(Map<String, int[]> locations, int currentX, int currentY) {
        int[] closest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (int[] coords : locations.values()) {
            double distance = Math.sqrt(Math.pow(currentX - coords[0], 2) + Math.pow(currentY - coords[1], 2));
            if (distance < minDistance) {
                minDistance = distance;
                closest = coords;
            }
        }
        return closest;
    }
    
    public static boolean isWalkingDistance(int fromX, int fromY, int toX, int toY) {
        double distance = Math.sqrt(Math.pow(fromX - toX, 2) + Math.pow(fromY - toY, 2));
        return distance < 100; // Less than 100 tiles = walking distance
    }
    
    public static int[] getBestLocationForGoal(Goal goal, int currentX, int currentY) {
        String desc = goal.getDescription().toLowerCase();
        
        if (desc.contains("woodcutting") || desc.contains("logs")) {
            if (desc.contains("willow")) return findNearestLocation(WOODCUTTING, currentX, currentY);
            if (desc.contains("yew")) return WOODCUTTING.get("LUMBRIDGE_YEWS");
            return findNearestLocation(WOODCUTTING, currentX, currentY);
        }
        
        if (desc.contains("mining") || desc.contains("ore")) {
            return findNearestLocation(MINING, currentX, currentY);
        }
        
        if (desc.contains("fishing") || desc.contains("fish")) {
            return findNearestLocation(FISHING, currentX, currentY);
        }
        
        if (desc.contains("combat") || desc.contains("train") || desc.contains("attack") || desc.contains("strength")) {
            return findNearestLocation(COMBAT_AREAS, currentX, currentY);
        }
        
        if (desc.contains("bank") || desc.contains("money")) {
            return findNearestLocation(BANKS, currentX, currentY);
        }
        
        if (desc.contains("armor") || desc.contains("weapon") || desc.contains("equipment")) {
            return findNearestLocation(SHOPS, currentX, currentY);
        }
        
        return null;
    }
}
