package com.rs.bot;

import com.rs.utils.Utils;

/**
 * Bot personality traits that influence decision making
 */
public class PersonalityProfile {
    
    // Efficiency personality (how optimal/sweaty the bot is)
    private EfficiencyType efficiency;
    
    // Social personality (how much the bot interacts)
    private SocialType social;
    
    // Risk personality (how risky decisions are)
    private RiskType risk;
    
    // Economic personality (spending/saving behavior)
    private EconomicType economic;
    
    public PersonalityProfile() {
        // Randomize personality on creation
        this.efficiency = EfficiencyType.values()[Utils.random(EfficiencyType.values().length)];
        this.social = SocialType.values()[Utils.random(SocialType.values().length)];
        this.risk = RiskType.values()[Utils.random(RiskType.values().length)];
        this.economic = EconomicType.values()[Utils.random(EconomicType.values().length)];
    }
    
    // Getters
    public EfficiencyType getEfficiency() { return efficiency; }
    public SocialType getSocial() { return social; }
    public RiskType getRisk() { return risk; }
    public EconomicType getEconomic() { return economic; }
    
    // Personality influence methods
    public double getEfficiencyMultiplier() {
        switch (efficiency) {
            case SWEATY: return 1.0; // 100% optimal
            case CASUAL: return 0.7; // 70% efficiency
            case CHAOTIC: return 0.3 + (Utils.random(70) / 100.0); // 30-100% random
            default: return 0.8;
        }
    }
    
    public int getChatFrequency() {
        switch (social) {
            case CHATTY: return 5; // Very frequent
            case HELPER: return 3; // Moderate, helpful
            case SHOWOFF: return 4; // Frequent bragging
            case QUIET: return 1; // Rare
            default: return 2;
        }
    }
    
    public double getRiskTolerance() {
        switch (risk) {
            case CONSERVATIVE: return 0.2; // Very safe
            case BALANCED: return 0.5; // Moderate
            case AGGRESSIVE: return 0.8; // High risk
            case DEGEN: return 1.0; // YOLO everything
            default: return 0.5;
        }
    }
    
    public double getSpendingRate() {
        switch (economic) {
            case SAVER: return 0.2; // Keep most money
            case BALANCED: return 0.5; // Moderate spending
            case SPENDER: return 0.8; // Spend freely
            case TRADER: return 0.6; // Spend on investments
            case GAMBLER: return 0.9; // Spend on bets
            default: return 0.5;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Personality[%s/%s/%s/%s]", 
            efficiency, social, risk, economic);
    }
}

enum EfficiencyType {
    SWEATY,   // Optimal, efficient, meta strategies
    CASUAL,   // Relaxed, suboptimal but chill
    CHAOTIC   // Random, unpredictable methods
}

enum SocialType {
    CHATTY,   // Talks a lot, social
    QUIET,    // Rarely speaks
    HELPER,   // Helpful to others
    SHOWOFF   // Brags about achievements
}

enum RiskType {
    CONSERVATIVE, // Plays it very safe
    BALANCED,     // Moderate risk taking
    AGGRESSIVE,   // High risk, high reward
    DEGEN         // YOLO, terrible decisions
}

enum EconomicType {
    SAVER,    // Hoards money
    BALANCED, // Moderate spending
    SPENDER,  // Spends freely
    TRADER,   // Focuses on making money
    GAMBLER   // Loses money gambling
}
