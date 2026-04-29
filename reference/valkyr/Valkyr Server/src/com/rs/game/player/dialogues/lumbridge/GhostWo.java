package com.rs.game.player.dialogues.lumbridge;

import com.rs.game.player.dialogues.Dialogue;

public class GhostWo extends Dialogue {

	int npcId;

	@Override
	public void start() {
		sendPlayerDialogue( 9827, "Hello, ghost, how are you?");
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (stage == -1) {
			sendNPCDialogue(457, 9827, "Woooo! Wooo!! Woooooo!!!" );
			stage = 1;
		} else if (stage == 1) {
			sendPlayerDialogue( 9827, "I'm sorry i dont understand?");
			stage = 2;
		} else if (stage == 2) {
			sendNPCDialogue(457, 9827, "Woooo! Wooo!! Woooooo!!!" );
			end();		
		}	
	}

	public void finish() {
			
	}
	
}