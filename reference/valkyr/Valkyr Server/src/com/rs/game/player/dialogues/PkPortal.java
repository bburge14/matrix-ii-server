package com.rs.game.player.dialogues;

import com.rs.game.Animation;
import com.rs.game.ForceTalk;
import com.rs.game.WorldTile;
import com.rs.game.player.Skills;
import com.rs.game.player.content.Magic;
import com.rs.utils.ShopsHandler;

public class PkPortal extends Dialogue {

	@Override
	public void start() {
		sendOptionsDialogue("Select an option.",
				"Clan Wars", "Revs", "Magebank", "Edgeville",  "Never mind.");
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (componentId == OPTION_1) {
	    	Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2993, 9679, 0));
				end();
			}
		if (componentId == OPTION_2) {
	    	Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3123, 10124, 0));
				end();
			}
		if (componentId == OPTION_3) {
	    	Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(2538, 4715, 0));
				end();
			}
		if (componentId == OPTION_4) {
	    	Magic.sendNormalTeleportSpell(player, 0, 0, new WorldTile(3087, 3501, 0));
		end();
		} else if (componentId == OPTION_5) {
		}
		end();
		
		}

	

	@Override
	public void finish() {

	}

}
