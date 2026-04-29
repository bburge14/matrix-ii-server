package com.rs.game.player.dialogues.lumbridge;

import com.rs.game.item.Item;
import com.rs.game.player.dialogues.Dialogue;

public class GhostFind extends Dialogue {

	int npcId;
	Item skull = new Item(553, 1);

	@Override
	public void start() {
		sendNPCDialogue(457, 9827, "How are you doing finiding my skull?" );
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (stage == -1) {
			if (!player.getInventory().contains(skull)) {
				sendPlayerDialogue(9827, "I'm still searching.");
				stage = 1;
			} else {
				sendPlayerDialogue(9827, "I've found your skull!");
				stage = 2;
			}
		} 
		else if (stage == 1) {
			sendNPCDialogue(457, 9827, "Okay, thanks for your help!" );
			stage = 5;
		}
		else if (stage == 2) {
			sendNPCDialogue(457, 9827, "Well, place it back in my coffin please!" );
			stage = 5;
		}
		else if (stage == 5) {
			end();
		}
	}

	public void finish() {
	
	}
}