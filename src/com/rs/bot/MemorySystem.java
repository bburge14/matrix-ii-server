package com.rs.bot;

import java.util.*;

/**
 * Bot memory for players, locations, events, and experiences
 */
public class MemorySystem {
    
    // Player interactions and relationships
    private Map<String, PlayerMemory> playerMemories;
    
    // Location familiarity and preferences  
    private Map<String, LocationMemory> locationMemories;
    
    // Recent events and experiences
    private List<EventMemory> recentEvents;
    private static final int MAX_EVENTS = 100;
    
    public MemorySystem() {
        this.playerMemories = new HashMap<>();
        this.locationMemories = new HashMap<>();
        this.recentEvents = new ArrayList<>();
    }
    
    /**
     * Remember an interaction with a player
     */
    public void rememberPlayer(String playerName, InteractionType type, boolean positive) {
        PlayerMemory memory = playerMemories.getOrDefault(playerName, new PlayerMemory(playerName));
        memory.addInteraction(type, positive);
        playerMemories.put(playerName, memory);
    }
    
    /**
     * Remember a location experience
     */
    public void rememberLocation(String locationName, String activity, boolean successful) {
        LocationMemory memory = locationMemories.getOrDefault(locationName, new LocationMemory(locationName));
        memory.addExperience(activity, successful);
        locationMemories.put(locationName, memory);
    }
    
    /**
     * Remember an event
     */
    public void rememberEvent(String description, EventType type) {
        EventMemory event = new EventMemory(description, type, System.currentTimeMillis());
        recentEvents.add(0, event); // Add to front
        
        // Limit memory size
        if (recentEvents.size() > MAX_EVENTS) {
            recentEvents.remove(recentEvents.size() - 1);
        }
    }
    
    /**
     * Get trust level with a player (0.0 to 1.0)
     */
    public double getPlayerTrust(String playerName) {
        PlayerMemory memory = playerMemories.get(playerName);
        return memory != null ? memory.getTrustLevel() : 0.5; // Neutral for unknown
    }
    
    /**
     * Get location preference (0.0 to 1.0) 
     */
    public double getLocationPreference(String locationName) {
        LocationMemory memory = locationMemories.get(locationName);
        return memory != null ? memory.getPreference() : 0.5; // Neutral for unknown
    }
    
    /**
     * Check if we've seen this player recently
     */
    public boolean hasSeenPlayerRecently(String playerName) {
        PlayerMemory memory = playerMemories.get(playerName);
        if (memory == null) return false;
        
        long timeSince = System.currentTimeMillis() - memory.getLastSeen();
        return timeSince < 3600000; // 1 hour
    }
    
    /**
     * Get recent events of a specific type
     */
    public List<EventMemory> getRecentEvents(EventType type, int maxAge) {
        List<EventMemory> filtered = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - maxAge;
        
        for (EventMemory event : recentEvents) {
            if (event.getType() == type && event.getTimestamp() > cutoff) {
                filtered.add(event);
            }
        }
        
        return filtered;
    }
    
    // Getters
    public Set<String> getKnownPlayers() { return new HashSet<>(playerMemories.keySet()); }
    public Set<String> getKnownLocations() { return new HashSet<>(locationMemories.keySet()); }
}

/**
 * Memory of interactions with a specific player
 */
class PlayerMemory {
    private String name;
    private int positiveInteractions;
    private int negativeInteractions;
    private long lastSeen;
    private InteractionType lastInteractionType;
    
    public PlayerMemory(String name) {
        this.name = name;
        this.positiveInteractions = 0;
        this.negativeInteractions = 0;
        this.lastSeen = System.currentTimeMillis();
    }
    
    public void addInteraction(InteractionType type, boolean positive) {
        if (positive) positiveInteractions++;
        else negativeInteractions++;
        
        this.lastInteractionType = type;
        this.lastSeen = System.currentTimeMillis();
    }
    
    public double getTrustLevel() {
        int total = positiveInteractions + negativeInteractions;
        if (total == 0) return 0.5; // Neutral
        
        return (double) positiveInteractions / total;
    }
    
    public long getLastSeen() { return lastSeen; }
}

/**
 * Memory of experiences at a location
 */
class LocationMemory {
    private String name;
    private Map<String, Integer> successfulActivities;
    private Map<String, Integer> failedActivities;
    private long lastVisited;
    
    public LocationMemory(String name) {
        this.name = name;
        this.successfulActivities = new HashMap<>();
        this.failedActivities = new HashMap<>();
        this.lastVisited = System.currentTimeMillis();
    }
    
    public void addExperience(String activity, boolean successful) {
        Map<String, Integer> map = successful ? successfulActivities : failedActivities;
        map.put(activity, map.getOrDefault(activity, 0) + 1);
        this.lastVisited = System.currentTimeMillis();
    }
    
    public double getPreference() {
        int totalSuccesses = successfulActivities.values().stream().mapToInt(Integer::intValue).sum();
        int totalFailures = failedActivities.values().stream().mapToInt(Integer::intValue).sum();
        int total = totalSuccesses + totalFailures;
        
        if (total == 0) return 0.5; // Neutral
        return (double) totalSuccesses / total;
    }
}

/**
 * Memory of a specific event
 */
class EventMemory {
    private String description;
    private EventType type;
    private long timestamp;
    
    public EventMemory(String description, EventType type, long timestamp) {
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
    }
    
    public EventType getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
}

enum InteractionType {
    CHAT, TRADE, COMBAT, HELP, GRIEF
}

enum EventType {
    LEVEL_UP, RARE_DROP, DEATH, ACHIEVEMENT, SOCIAL, ECONOMIC
}
