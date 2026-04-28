package com.rs.game.player.dialogues;

import com.rs.game.player.Player;
import com.rs.game.item.Item;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.game.Animation;

import com.rs.game.player.Skills;
import com.rs.game.player.dialogues.Dialogue;

public class SirenicMaking extends Dialogue {

	@Override
	public void start() {
		sendOptionsDialogue("Crafting an Sirenic Armour", "Sirenic Mask", "Sirenic Hauberk", "Sirenic Chaps");
		stage = -1;
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (stage == -1) {
			if (componentId == OPTION_1 && player.getInventory().containsItem(29863, 14) && (player.getSkills().getLevel(Skills.CRAFTING) >= 99)) {
				end();
				player.getInventory().deleteItem(29863, 14);
                player.getInventory().addItem(29854, 1);
                player.getSkills().addXp(Skills.CRAFTING, 60);
				player.getDialogueManager().startDialogue("SimpleMessage","You make a Sirenic Mask.");
			} else if (componentId == OPTION_1 && !player.getInventory().containsItem(29863, 14) || !(player.getSkills().getLevel(Skills.CRAFTING) >= 99)) {
				end();
				player.getDialogueManager().startDialogue("SimpleMessage","You need 14 Sirenic scales and 99 Crafting to make this.");
				return;
			} else if (componentId == OPTION_2 && player.getInventory().containsItem(29863, 42) && (player.getSkills().getLevel(Skills.CRAFTING) >= 99)) {
				end();
				player.getInventory().deleteItem(29863, 42);
                player.getInventory().addItem(29857, 1);
                player.getSkills().addXp(Skills.CRAFTING, 100);
				player.getDialogueManager().startDialogue("SimpleMessage","You make a Sirenic Hauberk.");
			} else if (componentId == OPTION_2 && !player.getInventory().containsItem(29863, 42) || !(player.getSkills().getLevel(Skills.CRAFTING) >= 99)) {
				end();
				player.getDialogueManager().startDialogue("SimpleMessage","You need 42 Sirenic scales and 99 Crafting to make this.");
				return;
			} else if (componentId == OPTION_3 && player.getInventory().containsItem(29863, 28) && (player.getSkills().getLevel(Skills.CRAFTING) >= 99)) {
				end();
				player.getInventory().deleteItem(29863, 28);
                player.getInventory().addItem(29860, 1);
                player.getSkills().addXp(Skills.CRAFTING, 80);
				player.getDialogueManager().startDialogue("SimpleMessage","You make a pair of Sirenic chaps.");
			} else if (componentId == OPTION_3 && !player.getInventory().containsItem(29863, 28) || !(player.getSkills().getLevel(Skills.CRAFTING) >= 99)) {
				end();
				player.getDialogueManager().startDialogue("SimpleMessage","You need 28 Sirenic scales and 99 Crafting to make this.");
				return;
			}
		}
		 
	}
	
	@Override
	public void finish() {

	}

}