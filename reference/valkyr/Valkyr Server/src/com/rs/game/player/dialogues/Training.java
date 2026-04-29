package com.rs.game.player.dialogues;


import com.rs.utils.ShopsHandler;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.game.player.content.Magic;
import com.rs.game.WorldTile;

public class Training extends Dialogue {

	@Override
	public void start() {
		sendOptionsDialogue("Choose a Teleport", "Yaks", "Experiments", "Brutal Green Dragons", "Rock Crabs");
		stage = 2;
	}

	@SuppressWarnings("unused")
	public void run(int interfaceId, int componentId) {
		int option;
		if (stage == 1) {
			sendOptionsDialogue("Choose a Teleport", "Yaks", "Experiments", "Brutal Green Dragons", "Rock Crabs");
			stage = 2;
		} else if (stage == 2) {
			if (componentId == OPTION_1) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2324, 3793, 0));
				end();
			}
			if (componentId == OPTION_2) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3555, 9947, 0));
				end();
			}
			if (componentId == OPTION_3) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(1767, 5337, 0));
				end();
			}
			if (componentId == OPTION_4) {
				Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2706, 3714, 0));
				end();
			}
		}
	}

	@Override
	public void finish() {

	}

}