package com.rs.bot;

import com.rs.utils.Utils;

public class MovementActivity {
    private AIPlayer bot;
    private int ticksSinceLastMove = 0;
    
    public MovementActivity(AIPlayer bot) {
        this.bot = bot;
    }
    
    public void execute() {
        ticksSinceLastMove++;
        
        // Move every 5-15 ticks (random human-like timing)
        if (ticksSinceLastMove >= Utils.random(5, 15)) {
            int currentX = bot.getX();
            int currentY = bot.getY();
            
            // Random walk within 10 tiles
            int newX = currentX + Utils.random(-5, 5);
            int newY = currentY + Utils.random(-5, 5);
            
            bot.addWalkSteps(newX, newY, -1, false);
            ticksSinceLastMove = 0;
        }
    }
}
