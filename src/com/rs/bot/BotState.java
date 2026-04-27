package com.rs.bot;

/**
 * Possible states for bot behavior state machine
 */
public enum BotState {
    IDLE,        // Standing around, deciding what to do
    PLANNING,    // Choosing next activity based on goals
    TRAVELING,   // Moving to activity location
    ACTIVITY,    // Performing activity (skilling, combat, etc.)
    BANKING,     // Banking operations
    SOCIAL       // Social interactions (chatting, trading, etc.)
}
