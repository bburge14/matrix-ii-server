package com.rs.game.player.content;

import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.actions.Action;
import com.rs.game.player.content.SkillsDialogue;

public class AmuletAttaching {
    
    public void attatchAmulet(int itemUsed, int itemUsedWith, Player player) {
        if (itemUsed == 1759 && itemUsedWith == 1673) {
            player.getInventory().deleteItem(1759, 1);
            player.getInventory().deleteItem(1673, 1);
            player.getInventory().addItem(1692, 1);
            player.getSkills().addXp(Skills.CRAFTING, 5);
            player.sm("You string the gold amulet with a ball of wool...");
        } else if (itemUsed == 1759 && itemUsedWith == 1675) {
            player.getInventory().deleteItem(1759, 1);
            player.getInventory().deleteItem(1675, 1);
            player.getInventory().addItem(1694, 1);
            player.getSkills().addXp(Skills.CRAFTING, 10);
            player.sm("You string the sapphire amulet with a ball of wool...");
        } else if (itemUsed == 1759 && itemUsedWith == 1677) {
            player.getInventory().deleteItem(1759, 1);
            player.getInventory().deleteItem(1677, 1);
            player.getInventory().addItem(1696, 1);
            player.getSkills().addXp(Skills.CRAFTING, 15);
            player.sm("You string the emerald amulet with a ball of wool...");
        } else if (itemUsed == 1759 && itemUsedWith == 1679) {
            player.getInventory().deleteItem(1759, 1);
            player.getInventory().deleteItem(1679, 1);
            player.getInventory().addItem(1698, 1);
            player.getSkills().addXp(Skills.CRAFTING, 20);
            player.sm("You string the ruby amulet with a ball of wool...");
        } else if (itemUsed == 1759 && itemUsedWith == 1681) {
            player.getInventory().deleteItem(1759, 1);
            player.getInventory().deleteItem(1681, 1);
            player.getInventory().addItem(1700, 1);
            player.getSkills().addXp(Skills.CRAFTING, 25);
            player.sm("You string the diamond amulet with a ball of wool...");
        } else if (itemUsed == 1759 && itemUsedWith == 1679) {
            player.getInventory().deleteItem(1759, 1);
            player.getInventory().deleteItem(1683, 1);
            player.getInventory().addItem(1702, 1);
            player.getSkills().addXp(Skills.CRAFTING, 25);
            player.sm("You string the dragonstone amulet with a ball of wool...");
        }
    }
    
    public void finish() {
        
    }
    
    
    
}
