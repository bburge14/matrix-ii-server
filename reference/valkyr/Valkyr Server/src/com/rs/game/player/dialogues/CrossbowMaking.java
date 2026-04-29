package com.rs.game.player.dialogues;

import com.rs.game.player.Player;
import com.rs.game.item.Item;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.game.Animation;

import com.rs.game.player.Skills;
import com.rs.game.player.dialogues.Dialogue;

public class CrossbowMaking extends Dialogue {

	@Override
	public void start() {
		sendOptionsDialogue("Crafting an Ascension Crossbow", "Main-Hand Ascension Crossbow", "Off-hand Ascension Crossbow");
		stage = -1;
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (stage == -1) {
			if (componentId == OPTION_1 && player.getInventory().containsItem(25917, 1) && player.getInventory().containsItem(28457, 1) 
										&& player.getInventory().containsItem(28458, 1) && player.getInventory().containsItem(28459, 1)
										&& player.getInventory().containsItem(28460, 1) && player.getInventory().containsItem(28461, 1)
										&& player.getInventory().containsItem(28462, 1) && (player.getSkills().getLevel(Skills.SMITHING) >= 99)) {
				end();
				player.getInventory().deleteItem(25917, 1);
                player.getInventory().deleteItem(28457, 1);
                player.getInventory().deleteItem(28458, 1);
                player.getInventory().deleteItem(28459, 1);
                player.getInventory().deleteItem(28460, 1);
                player.getInventory().deleteItem(28461, 1);
                player.getInventory().deleteItem(28462, 1);
                player.getInventory().addItem(28437, 1);
                player.getSkills().addXp(Skills.SMITHING, 80);
				player.getDialogueManager().startDialogue("SimpleMessage","You make a Main-hand Ascension Crossbow.");
			} else if (componentId == OPTION_1 && !player.getInventory().containsItem(25917, 1) || !player.getInventory().containsItem(28457, 1) 
										|| !player.getInventory().containsItem(28458, 1) || !player.getInventory().containsItem(28459, 1)
										|| !player.getInventory().containsItem(28460, 1) || !player.getInventory().containsItem(28461, 1)
										|| !player.getInventory().containsItem(28462, 1) || !(player.getSkills().getLevel(Skills.SMITHING) >= 99)) {
				end();
				player.getDialogueManager().startDialogue("SimpleMessage","You need all 6 Ascension signets, a Dragon crossbow and 99 Smithing to make this.");
				return;
			} else if (componentId == OPTION_2 && player.getInventory().containsItem(25917, 1) && player.getInventory().containsItem(28457, 1) 
										&& player.getInventory().containsItem(28458, 1) && player.getInventory().containsItem(28459, 1)
										&& player.getInventory().containsItem(28460, 1) && player.getInventory().containsItem(28461, 1)
										&& player.getInventory().containsItem(28462, 1) && (player.getSkills().getLevel(Skills.SMITHING) >= 99)) {
				end();
				player.getInventory().deleteItem(25917, 1);
                player.getInventory().deleteItem(28457, 1);
                player.getInventory().deleteItem(28458, 1);
                player.getInventory().deleteItem(28459, 1);
                player.getInventory().deleteItem(28460, 1);
                player.getInventory().deleteItem(28461, 1);
                player.getInventory().deleteItem(28462, 1);
                player.getInventory().addItem(28441, 1);
                player.getSkills().addXp(Skills.SMITHING, 80);
				player.getDialogueManager().startDialogue("SimpleMessage","You make an Off-hand Ascension Crossbow.");
			} else if (componentId == OPTION_2 && !player.getInventory().containsItem(25917, 1) || !player.getInventory().containsItem(28457, 1) 
										|| !player.getInventory().containsItem(28458, 1) || !player.getInventory().containsItem(28459, 1)
										|| !player.getInventory().containsItem(28460, 1) || !player.getInventory().containsItem(28461, 1)
										|| !player.getInventory().containsItem(28462, 1) || !(player.getSkills().getLevel(Skills.SMITHING) >= 99)) {
				end();
				player.getDialogueManager().startDialogue("SimpleMessage","You need all 6 Ascension signets, a Dragon crossbow and 99 Smithing to make this.");
				return;
			}
		}
		 
	}
	
	@Override
	public void finish() {

	}

}