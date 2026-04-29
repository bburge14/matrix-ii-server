package com.rs.game.player.dialogues;

import com.rs.game.player.Player;
import com.rs.game.World;
import com.rs.Settings;

public class IronManSelect extends Dialogue {

	@Override
	public void start() {
		sendOptionsDialogue("What would you like to be?", "<col=e82626>Legend - x10</col>", 
				"<col=debb1c>Hero - x30</col>", "<col=16a6e0>Knight - x50</col>", "<col=30b11c>Squire - x100</col>", "<col=8f8686>Ironman - x5</col>");
        stage = 1;
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (stage == -1)
			stage = 1;
		if (stage == 1) {
			switch (componentId) {			
				case OPTION_1:
					player.setExpMode(10);
				break;
					
				case OPTION_2:
					player.setExpMode(30);
				break;
					
				case OPTION_3:
					player.setExpMode(50);
				break;
					
				case OPTION_4:
					player.setExpMode(100);
				break;
				
				case OPTION_5:
					player.setExpMode(5);
				break;
				
			default:
				player.setExpMode(100);		
			}
			sendWelcome();
			end();
		}
    }
	
	private void sendWelcome() {
		World.sendWorldMessage("<img=4><col=debb1c>" + player.getDisplayName() + " has joined Valkyr, they're playing in <col=1ee2e5>" + player.getExpMultiplyer(player.getExpMode()) + "<col=debb1c> mode!", false);
	}


	@Override
	public void finish() {

	}

}
