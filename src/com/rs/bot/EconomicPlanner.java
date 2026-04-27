package com.rs.bot;

import java.util.*;
import com.rs.utils.Utils;

public class EconomicPlanner {
    private AIPlayer bot;
    private Map<Integer, ItemMarketData> marketData;
    private Map<String, MoneyMakingMethod> moneyMakers;
    private long bankGoal;
    private EconomicStrategy strategy;
    
    public enum EconomicStrategy {
        CONSERVATIVE,    // Safe, slow wealth building
        AGGRESSIVE,      // High risk, high reward
        EFFICIENT,       // Optimal gp/hour methods
        SOCIAL,         // Trading with players
        MERCHANT,       // Buy low, sell high
        SKILLER,        // Resource gathering focused
        FLIPPER         // Grand Exchange flipping
    }
    
    public static class ItemMarketData {
        public int itemId;
        public String itemName;
        public long averagePrice;
        public long buyPrice;
        public long sellPrice;
        public int dailyVolume;
        public double volatility;
        
        public ItemMarketData(int itemId, String name) {
            this.itemId = itemId;
            this.itemName = name;
            this.averagePrice = 0;
            this.buyPrice = 0;
            this.sellPrice = 0;
            this.dailyVolume = 0;
            this.volatility = 0.0;
        }
        
        public long getMargin() {
            return sellPrice - buyPrice;
        }
        
        public boolean isProfitable() {
            return getMargin() > 0 && dailyVolume > 10;
        }
    }
    
    public static class MoneyMakingMethod {
        public String methodName;
        public String category;
        public long gpPerHour;
        public Map<String, Integer> requirements;
        public Map<Integer, Integer> itemRequirements;
        public boolean isRepeatable;
        
        public MoneyMakingMethod(String name, String category, long gph) {
            this.methodName = name;
            this.category = category;
            this.gpPerHour = gph;
            this.requirements = new HashMap<>();
            this.itemRequirements = new HashMap<>();
            this.isRepeatable = true;
        }
        
        public void addSkillRequirement(String skill, int level) {
            requirements.put("skill_" + skill.toLowerCase(), level);
        }
        
        public void addItemRequirement(int itemId, int quantity) {
            itemRequirements.put(itemId, quantity);
        }
        
        public boolean canPerform(AIPlayer bot) {
            // Check skill requirements
            for (Map.Entry<String, Integer> req : requirements.entrySet()) {
                if (req.getKey().startsWith("skill_")) {
                    String skill = req.getKey().substring(6);
                    int reqLevel = req.getValue();
                    if (getPlayerSkillLevel(bot, skill) < reqLevel) {
                        return false;
                    }
                }
            }
            
            // Check item requirements
            for (Map.Entry<Integer, Integer> item : itemRequirements.entrySet()) {
                int itemId = item.getKey();
                int quantity = item.getValue();
                if (bot.getInventory().getAmountOf(itemId) < quantity) {
                    return false;
                }
            }
            
            return true;
        }
        
        private int getPlayerSkillLevel(AIPlayer bot, String skill) {
            Map<String, Integer> skillMap = new HashMap<>();
            skillMap.put("woodcutting", 8);
            skillMap.put("mining", 14);
            skillMap.put("fishing", 10);
            skillMap.put("attack", 0);
            
            Integer skillId = skillMap.get(skill.toLowerCase());
            return skillId != null ? bot.getSkills().getLevel(skillId) : 1;
        }
    }
    
    public EconomicPlanner(AIPlayer bot) {
        this.bot = bot;
        this.marketData = new HashMap<>();
        this.moneyMakers = new HashMap<>();
        this.bankGoal = 100000; // Default 100k goal
        
        initializeMarketData();
        initializeMoneyMakingMethods();
        selectEconomicStrategy();
    }
    
    private void initializeMarketData() {
        // Initialize market data for common items
        addItemData(995, "Coins", 1, 1, 1);
        addItemData(1511, "Logs", 50, 45, 55);
        addItemData(1521, "Oak logs", 150, 140, 160);
        addItemData(1519, "Willow logs", 300, 280, 320);
        addItemData(440, "Iron ore", 100, 90, 110);
        addItemData(453, "Coal", 200, 180, 220);
        addItemData(317, "Shrimps", 15, 12, 18);
        addItemData(373, "Raw lobster", 180, 170, 190);
        addItemData(1333, "Rune scimitar", 25000, 24000, 26000);
        addItemData(1359, "Rune axe", 8000, 7800, 8200);
    }
    
    private void addItemData(int itemId, String name, long avg, long buy, long sell) {
        ItemMarketData data = new ItemMarketData(itemId, name);
        data.averagePrice = avg;
        data.buyPrice = buy;
        data.sellPrice = sell;
        data.dailyVolume = Utils.random(100, 10000);
        data.volatility = Utils.random(5, 25) / 100.0; // 5-25% volatility
        marketData.put(itemId, data);
    }
    
    private void initializeMoneyMakingMethods() {
        // Woodcutting methods
        MoneyMakingMethod wcLogs = new MoneyMakingMethod("Cutting Regular Logs", "Woodcutting", 15000);
        wcLogs.addSkillRequirement("woodcutting", 1);
        wcLogs.addItemRequirement(1351, 1); // Bronze axe
        moneyMakers.put("wc_logs", wcLogs);
        
        MoneyMakingMethod wcOaks = new MoneyMakingMethod("Cutting Oak Logs", "Woodcutting", 35000);
        wcOaks.addSkillRequirement("woodcutting", 15);
        wcOaks.addItemRequirement(1349, 1); // Iron axe
        moneyMakers.put("wc_oaks", wcOaks);
        
        MoneyMakingMethod wcWillows = new MoneyMakingMethod("Cutting Willow Logs", "Woodcutting", 60000);
        wcWillows.addSkillRequirement("woodcutting", 30);
        wcWillows.addItemRequirement(1353, 1); // Steel axe
        moneyMakers.put("wc_willows", wcWillows);
        
        // Mining methods
        MoneyMakingMethod mineIron = new MoneyMakingMethod("Mining Iron Ore", "Mining", 45000);
        mineIron.addSkillRequirement("mining", 15);
        mineIron.addItemRequirement(1267, 1); // Iron pickaxe
        moneyMakers.put("mine_iron", mineIron);
        
        MoneyMakingMethod mineCoal = new MoneyMakingMethod("Mining Coal", "Mining", 80000);
        mineCoal.addSkillRequirement("mining", 30);
        mineCoal.addItemRequirement(1269, 1); // Steel pickaxe
        moneyMakers.put("mine_coal", mineCoal);
        
        // Fishing methods
        MoneyMakingMethod fishLobsters = new MoneyMakingMethod("Fishing Lobsters", "Fishing", 120000);
        fishLobsters.addSkillRequirement("fishing", 40);
        fishLobsters.addItemRequirement(301, 1); // Lobster pot
        moneyMakers.put("fish_lobsters", fishLobsters);
        
        // Combat methods
        MoneyMakingMethod killCows = new MoneyMakingMethod("Killing Cows", "Combat", 25000);
        killCows.addSkillRequirement("attack", 10);
        moneyMakers.put("kill_cows", killCows);
        
        // Trading methods
        MoneyMakingMethod flipItems = new MoneyMakingMethod("Item Flipping", "Trading", 200000);
        flipItems.addItemRequirement(995, 50000); // Need starting capital
        moneyMakers.put("flip_items", flipItems);
    }
    
    private void selectEconomicStrategy() {
        String archetype = bot.getArchetype() != null ? bot.getArchetype() : "main";
        
        // Default based on archetype
        switch (archetype.toLowerCase()) {
            case "skiller":
                strategy = EconomicStrategy.SKILLER;
                break;
            case "combat":
                strategy = EconomicStrategy.EFFICIENT;
                break;
            default:
                strategy = EconomicStrategy.CONSERVATIVE;
        }
        
        System.out.println("[EconomicPlanner] " + bot.getDisplayName() + " using " + strategy + " economic strategy");
    }
    
    public MoneyMakingMethod getBestMoneyMaker() {
        List<MoneyMakingMethod> viable = new ArrayList<>();
        
        // Find methods bot can perform
        for (MoneyMakingMethod method : moneyMakers.values()) {
            if (method.canPerform(bot)) {
                viable.add(method);
            }
        }
        
        if (viable.isEmpty()) {
            return moneyMakers.get("wc_logs"); // Fallback to basic method
        }
        
        // Select based on strategy
        switch (strategy) {
            case EFFICIENT:
                return viable.stream().max(Comparator.comparing(m -> m.gpPerHour)).orElse(viable.get(0));
            case CONSERVATIVE:
                return viable.stream().filter(m -> m.category.equals("Woodcutting") || m.category.equals("Mining")).max(Comparator.comparing(m -> m.gpPerHour)).orElse(viable.get(0));
            case SKILLER:
                return viable.stream().filter(m -> m.category.equals("Woodcutting") || m.category.equals("Mining") || m.category.equals("Fishing")).max(Comparator.comparing(m -> m.gpPerHour)).orElse(viable.get(0));
            default:
                return viable.get(Utils.random(viable.size()));
        }
    }
    
    public void updateMarketData() {
        // Simulate market fluctuations
        for (ItemMarketData item : marketData.values()) {
            double change = (Utils.random(-100, 100) / 100.0) * item.volatility;
            item.averagePrice = Math.max(1, (long) (item.averagePrice * (1 + change)));
            item.buyPrice = (long) (item.averagePrice * 0.95);
            item.sellPrice = (long) (item.averagePrice * 1.05);
        }
    }
    
    public long calculateOptimalBankGoal() {
        long currentWealth = calculateTotalWealth();
        if (currentWealth < 10000) return 10000;
        if (currentWealth < 100000) return 100000;
        if (currentWealth < 1000000) return 1000000;
        return currentWealth * 2; // Double current wealth
    }
    
    private long calculateTotalWealth() {
        long total = bot.getInventory().getAmountOf(995); // Coins
        
        // Add estimated item values (simplified)
        for (int i = 0; i < 28; i++) {
            if (bot.getInventory().getItem(i) != null) {
                total += 1000; // Simplified item value
            }
        }
        
        return total;
    }
    
    // Getters
    public EconomicStrategy getStrategy() { return strategy; }
    public long getBankGoal() { return bankGoal; }
    public ItemMarketData getItemData(int itemId) { return marketData.get(itemId); }
    public Map<String, MoneyMakingMethod> getMoneyMakers() { return new HashMap<>(moneyMakers); }
}
