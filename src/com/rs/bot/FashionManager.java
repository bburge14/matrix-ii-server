package com.rs.bot;

import java.util.*;
import com.rs.utils.Utils;

public class FashionManager {
    private AIPlayer bot;
    private FashionStyle preferredStyle;
    private Map<String, OutfitSet> outfitCollections;
    private List<Integer> wishlistItems;
    private FashionBudget budget;
    private int fashionScore;
    private long lastOutfitChange;
    
    public enum FashionStyle {
        WEALTHY,        // Expensive items, rare gear, party hats
        COORDINATED,    // Color-matched outfits, themed sets
        FUNCTIONAL,     // Best stats, doesn't care about looks  
        TRENDY,         // Current popular items, follows trends
        UNIQUE,         // Rare combinations, stands out
        CASUAL,         // Simple, comfortable outfits
        SEASONAL,       // Holiday items, event gear
        HARDCORE        // Achievement-based gear only
    }
    
    public enum FashionBudget {
        UNLIMITED,      // Money no object
        HIGH,          // 10M+ for fashion items
        MEDIUM,        // 1-10M for fashion
        LOW,           // 100K-1M for fashion  
        MINIMAL        // Only free/cheap items
    }
    
    public static class OutfitSet {
        public String name;
        public String occasion;
        public Map<String, Integer> items; // slot -> itemId
        public long totalCost;
        public int prestigeLevel;
        public boolean isComplete;
        
        public OutfitSet(String name, String occasion) {
            this.name = name;
            this.occasion = occasion;
            this.items = new HashMap<>();
            this.totalCost = 0;
            this.prestigeLevel = 1;
            this.isComplete = false;
        }
        
        public void addItem(String slot, int itemId, long cost) {
            items.put(slot, itemId);
            totalCost += cost;
        }
        
        public boolean canAfford(long availableMoney) {
            return availableMoney >= totalCost;
        }
        
        public int getMissingItems(AIPlayer bot) {
            int missing = 0;
            for (int itemId : items.values()) {
                if (!bot.getInventory().containsItem(itemId, 1)) {
                    missing++;
                }
            }
            return missing;
        }
    }
    
    public FashionManager(AIPlayer bot) {
        this.bot = bot;
        this.outfitCollections = new HashMap<>();
        this.wishlistItems = new ArrayList<>();
        this.fashionScore = 0;
        this.lastOutfitChange = 0;
        
        determineFashionStyle();
        createOutfitCollections();
        generateWishlist();
    }
    
    private void determineFashionStyle() {
        String archetype = bot.getArchetype() != null ? bot.getArchetype() : "main";
        
        // Default based on archetype
        switch (archetype.toLowerCase()) {
            case "pure":
            case "hardcore":
                preferredStyle = FashionStyle.HARDCORE;
                budget = FashionBudget.LOW;
                break;
            case "skiller":
                preferredStyle = FashionStyle.CASUAL;
                budget = FashionBudget.LOW;
                break;
            case "main":
                preferredStyle = FashionStyle.COORDINATED;
                budget = FashionBudget.MEDIUM;
                break;
            default:
                preferredStyle = FashionStyle.FUNCTIONAL;
                budget = FashionBudget.MINIMAL;
        }
        
        System.out.println("[FashionManager] " + bot.getDisplayName() + " fashion style: " + preferredStyle + " (budget: " + budget + ")");
    }
    
    private void createOutfitCollections() {
        createWealthyOutfits();
        createCombatOutfits();
        createSkillingOutfits();
    }
    
    private void createWealthyOutfits() {
        // Party hat collections
        OutfitSet redPartyOutfit = new OutfitSet("Red Party Elegance", "showing_off");
        redPartyOutfit.addItem("head", 1038, 2100000000L); // Red partyhat
        redPartyOutfit.addItem("cape", 1007, 50000);       // Red cape
        redPartyOutfit.addItem("boots", 1061, 30000);      // Red boots
        redPartyOutfit.prestigeLevel = 10;
        outfitCollections.put("red_party", redPartyOutfit);
        
        // Santa hat outfit
        OutfitSet santaOutfit = new OutfitSet("Holiday Wealth", "seasonal");
        santaOutfit.addItem("head", 1050, 15000000);        // Santa hat
        santaOutfit.prestigeLevel = 7;
        outfitCollections.put("santa", santaOutfit);
    }
    
    private void createCombatOutfits() {
        // Dragon gear
        OutfitSet dragonSet = new OutfitSet("Dragon Warrior", "combat");
        dragonSet.addItem("body", 1149, 200000);      // Dragon chainbody
        dragonSet.addItem("legs", 4087, 150000);      // Dragon platelegs
        dragonSet.addItem("weapon", 4587, 100000);    // Dragon scimitar
        dragonSet.prestigeLevel = 4;
        outfitCollections.put("dragon", dragonSet);
        
        // Rune gear
        OutfitSet runeSet = new OutfitSet("Rune Warrior", "combat");
        runeSet.addItem("body", 1127, 50000);         // Rune platebody
        runeSet.addItem("legs", 1079, 40000);         // Rune platelegs
        runeSet.addItem("weapon", 1333, 25000);       // Rune scimitar
        runeSet.prestigeLevel = 3;
        outfitCollections.put("rune", runeSet);
    }
    
    private void createSkillingOutfits() {
        // Basic skilling outfit
        OutfitSet basicSkiller = new OutfitSet("Basic Skiller", "skilling");
        basicSkiller.addItem("weapon", 1359, 8000);   // Rune axe
        basicSkiller.addItem("tool2", 1275, 18000);   // Rune pickaxe
        basicSkiller.prestigeLevel = 2;
        outfitCollections.put("basic_skiller", basicSkiller);
    }
    
    private void generateWishlist() {
        // Add items based on fashion style
        switch (preferredStyle) {
            case WEALTHY:
                wishlistItems.addAll(Arrays.asList(1038, 1042, 1044, 1050)); // Party hats, santa
                break;
            case COORDINATED:
                wishlistItems.addAll(Arrays.asList(6107, 6108, 6109, 6110)); // Elegant clothes
                break;
            case FUNCTIONAL:
                wishlistItems.addAll(Arrays.asList(4151, 6570)); // Whip, fire cape
                break;
            case HARDCORE:
                wishlistItems.addAll(Arrays.asList(9747, 9813, 6570)); // Achievement capes
                break;
            default:
                wishlistItems.addAll(Arrays.asList(1333, 1359, 1275)); // Basic gear
        }
    }
    
    public OutfitSet selectBestOutfit(String occasion) {
        List<OutfitSet> suitable = new ArrayList<>();
        
        // Find outfits for the occasion
        for (OutfitSet outfit : outfitCollections.values()) {
            if (outfit.occasion.equals(occasion) || outfit.occasion.equals("general")) {
                suitable.add(outfit);
            }
        }
        
        if (suitable.isEmpty()) {
            suitable.addAll(outfitCollections.values());
        }
        
        // Filter by budget
        long availableMoney = getAvailableFashionBudget();
        suitable.removeIf(outfit -> !outfit.canAfford(availableMoney));
        
        if (suitable.isEmpty()) {
            return null;
        }
        
        // Select based on style preference
        switch (preferredStyle) {
            case WEALTHY:
                return suitable.stream().max(Comparator.comparing(o -> o.totalCost)).orElse(suitable.get(0));
            case FUNCTIONAL:
                return suitable.stream().min(Comparator.comparing(o -> o.totalCost)).orElse(suitable.get(0));
            default:
                return suitable.get(Utils.random(suitable.size()));
        }
    }
    
    public boolean shouldChangeOutfit() {
        long timeSinceChange = System.currentTimeMillis() - lastOutfitChange;
        
        // Don't change too frequently
        if (timeSinceChange < 300000) { // 5 minutes
            return false;
        }
        
        // Style-based change frequency
        switch (preferredStyle) {
            case TRENDY:
                return Utils.random(100) < 15; // 15% chance
            case WEALTHY:
                return Utils.random(100) < 8;  // 8% chance (show off different items)
            case FUNCTIONAL:
                return Utils.random(100) < 2;  // 2% chance (rarely change)
            default:
                return Utils.random(100) < 5;  // 5% chance
        }
    }
    
    public void updateFashionScore() {
        fashionScore = 0;
        
        // Calculate fashion score based on equipped items
        if (bot.getEquipment() != null) {
            for (int i = 0; i < 14; i++) {
                if (bot.getEquipment().getItem(i) != null) {
                    int itemId = bot.getEquipment().getItem(i).getId();
                    fashionScore += getItemFashionValue(itemId);
                }
            }
        }
    }
    
    private int getItemFashionValue(int itemId) {
        Map<Integer, Integer> fashionValues = new HashMap<>();
        
        // Party hats (ultimate fashion)
        fashionValues.put(1038, 1000); // Red phat
        fashionValues.put(1042, 900);  // Blue phat
        fashionValues.put(1050, 500);  // Santa hat
        fashionValues.put(4151, 250);  // Abyssal whip
        fashionValues.put(6570, 300);  // Fire cape
        fashionValues.put(1333, 50);   // Rune scimitar
        
        return fashionValues.getOrDefault(itemId, 10); // Default 10 points
    }
    
    private long getAvailableFashionBudget() {
        long totalWealth = bot.getInventory().getAmountOf(995); // Simplified
        
        switch (budget) {
            case UNLIMITED:
                return totalWealth;
            case HIGH:
                return Math.min(totalWealth / 2, 50000000);
            case MEDIUM:
                return Math.min(totalWealth / 4, 10000000);
            case LOW:
                return Math.min(totalWealth / 8, 1000000);
            case MINIMAL:
                return Math.min(totalWealth / 20, 100000);
            default:
                return 100000;
        }
    }
    
    public void performFashionActivity() {
        // Change outfit periodically
        if (shouldChangeOutfit()) {
            changeOutfit("general");
        }
        
        // Update fashion score
        updateFashionScore();
    }
    
    private void changeOutfit(String occasion) {
        OutfitSet newOutfit = selectBestOutfit(occasion);
        if (newOutfit != null) {
            lastOutfitChange = System.currentTimeMillis();
            System.out.println("[FashionManager] " + bot.getDisplayName() + " changed to: " + newOutfit.name);
        }
    }
    
    // Getters
    public FashionStyle getPreferredStyle() { return preferredStyle; }
    public int getFashionScore() { return fashionScore; }
    public List<Integer> getWishlistItems() { return new ArrayList<>(wishlistItems); }
    public Map<String, OutfitSet> getOutfitCollections() { return new HashMap<>(outfitCollections); }
}
