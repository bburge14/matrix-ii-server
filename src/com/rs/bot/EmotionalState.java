package com.rs.bot;

import com.rs.utils.Utils;

/**
 * Bot emotional state tracking - affects decision making and behavior
 */
public class EmotionalState {
    
    private int happiness;    // 0-100: affects chat positivity, risk taking
    private int frustration;  // 0-100: affects efficiency, aggression
    private int excitement;   // 0-100: affects activity switching, chat frequency
    private int boredom;     // 0-100: affects activity switching, afk behavior
    
    // Track recent events that affect emotions
    private long lastDeath;
    private long lastRareDrop;
    private long lastLevelUp;
    private int recentFailures; // Failed activities, deaths, etc.
    
    public EmotionalState() {
        // Start with neutral emotions
        this.happiness = 50;
        this.frustration = 20;
        this.excitement = 30;
        this.boredom = 10;
        this.recentFailures = 0;
    }
    
    /**
     * Update emotional state based on recent bot experiences
     */
    public void update(AIPlayer bot) {
        long now = System.currentTimeMillis();
        
        // Decay emotions over time (return to neutral)
        happiness = Math.max(30, happiness - 1);
        frustration = Math.max(0, frustration - 2);
        excitement = Math.max(10, excitement - 2);
        boredom = Math.min(80, boredom + 1);
        
        // Check for recent positive events
        if (now - lastLevelUp < 60000) { // 1 minute ago
            happiness = Math.min(100, happiness + 20);
            excitement = Math.min(100, excitement + 15);
            boredom = Math.max(0, boredom - 20);
        }
        
        if (now - lastRareDrop < 120000) { // 2 minutes ago  
            happiness = Math.min(100, happiness + 30);
            excitement = Math.min(100, excitement + 25);
        }
        
        // Check for recent negative events
        if (now - lastDeath < 300000) { // 5 minutes ago
            happiness = Math.max(0, happiness - 15);
            frustration = Math.min(100, frustration + 25);
        }
        
        // Frustration from repeated failures
        if (recentFailures > 3) {
            frustration = Math.min(100, frustration + 10);
            happiness = Math.max(0, happiness - 5);
        }
        
        // Random minor mood fluctuations (like real people)
        if (Utils.random(100) < 5) { // 5% chance per update
            happiness += Utils.random(21) - 10; // -10 to +10
            happiness = Math.max(0, Math.min(100, happiness));
        }
    }
    
    /**
     * Bot experienced a level up - positive emotion boost
     */
    public void onLevelUp() {
        lastLevelUp = System.currentTimeMillis();
        recentFailures = Math.max(0, recentFailures - 1);
    }
    
    /**
     * Bot got a rare drop - major positive boost  
     */
    public void onRareDrop() {
        lastRareDrop = System.currentTimeMillis();
        recentFailures = 0;
    }
    
    /**
     * Bot died - negative emotion impact
     */
    public void onDeath() {
        lastDeath = System.currentTimeMillis();
        recentFailures++;
    }
    
    /**
     * Bot failed at something - minor negative impact
     */
    public void onFailure() {
        recentFailures++;
        if (recentFailures > 10) recentFailures = 10; // Cap at 10
    }
    
    // Emotion influence on behavior
    public double getEfficiencyModifier() {
        // High frustration reduces efficiency, happiness improves it
        return 0.5 + (happiness - frustration) / 200.0; // 0.0 to 1.0 range
    }
    
    public double getChatMultiplier() {
        // Excitement and happiness increase chat, boredom decreases it
        return 0.5 + (happiness + excitement - boredom) / 200.0;
    }
    
    public boolean shouldSwitchActivity() {
        return boredom > 60 || (excitement < 20 && Utils.random(100) < 10);
    }
    
    // Getters
    public int getHappiness() { return happiness; }
    public int getFrustration() { return frustration; }
    public int getExcitement() { return excitement; }
    public int getBoredom() { return boredom; }
    
    @Override
    public String toString() {
        return String.format("Emotions[H:%d F:%d E:%d B:%d]", 
            happiness, frustration, excitement, boredom);
    }
}
