package com.rs.game.player.dialogues.lumbridge;

import com.rs.game.player.QuestManager.Quests;
import com.rs.game.player.dialogues.Dialogue;

public class FatherAereck1 extends Dialogue {

	int npcId;

	@Override
	public void start() {		
		sendNPCDialogue(456, 9827, "Welcome to the church of holy Saradomin." );
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (stage == -1) {
			sendPlayerDialogue(9827, "I'm looking for a quest." );
			stage = 1;
		} else if (stage == 1) {
			sendNPCDialogue(456, 9827, "That's lucky, I need someone to do a quest for me." );
			stage = 2;
		} else if (stage == 2) {
			sendPlayerDialogue(9827, "Sure!");
			stage = 3;
		} else if (stage == 3) {
			sendNPCDialogue(456, 9827, "Thank you. the problem is there's a ghost in the graveyard crypt",
					"of this church. I would like you to get rid of it." );
			stage = 4;
		} else if (stage == 4) {
			sendNPCDialogue(456, 9827, "You'll need the help of my freind, Father Urhney,", "who is a bit of a nutjob...");
			stage = 5;
		} else if (stage == 5) {
			sendNPCDialogue(456, 9827, "He's currently living in a little shack in the south of Lumbridge", "Swamps, neat the coast.");
			player.getQuestManager().setQuestStageAndRefresh(Quests.THE_RESTLESS_GHOST, 1);
			player.getPackets().sendConfig(107, 1);
			stage = 7;
		} else if (stage == 7) {
			sendPlayerDialogue( 9827, "Ok ill go find him now,");
			end();
		}
	}

	public void finish() {
		
	}
}