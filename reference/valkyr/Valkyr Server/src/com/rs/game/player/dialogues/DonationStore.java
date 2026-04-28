package com.rs.game.player.dialogues;

import com.rs.game.Animation;
import com.rs.Settings;
import com.rs.game.ForceTalk;
import com.rs.game.WorldTile;
import com.rs.game.player.Skills;
import com.rs.game.player.content.Magic;
import com.rs.utils.ShopsHandler;
import com.rs.game.player.Player;
import com.rs.game.player.content.DonatorZone;

public class DonationStore extends Dialogue {

	@Override
	public void start() {
		sendOptionsDialogue("Select an option.",
				"Donator Zone", "Vip Zone", "Donation Page", "Close");
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (componentId == OPTION_1) {
		if (player.isDonator()) {
					DonatorZone.enterDonatorzone(player);
				}
			}
		if (componentId == OPTION_2) {
		if (player.isSupremeDonator()) {
                
          Magic.sendPegasusTeleportSpell(player, 0, 0, new WorldTile(1824, 5146, 2));
		 
			}
			}
		if (componentId == OPTION_3) {
		player.getPackets().sendOpenURL(Settings.DONATE_LINK);
			}		
			
			
	    if (componentId == OPTION_4) {
			end();
		}
		end();
		
		}

	

	@Override
	public void finish() {

	}

}
