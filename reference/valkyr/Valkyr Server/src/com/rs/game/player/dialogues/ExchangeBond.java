package com.rs.game.player.dialogues;

import com.rs.game.player.Player;
import com.rs.game.player.Inventory;

public class ExchangeBond extends Dialogue {

	@Override
	public void start() {
		sendOptionsDialogue( "What would you like to do?", "Redeem for RuneCoins. (195 RuneCoins)", "Redeem for Donator Credit.", "Redeem for a Donator box." );
        stage = 1;
	}

	@Override
	public void run(int interfaceId, int componentId) {
	 if(stage == 1) {
		if(componentId == OPTION_1) {
            player.getInventory().deleteItem(29492, 1);
            player.setRuneCoins(player.getRuneCoins() + 195);
            player.sm("You've redeemed your bond for 195 runecoins. You now have: " + player.getRuneCoins() + " RuneCoins.");
            end();
		} else if(componentId == OPTION_2) {
            player.getInventory().deleteItem(29492, 1);
            player.setDonatorAmount(player.getDonatorAmount() + 5);
            end();
		} else if(componentId == OPTION_3) {
            player.getInventory().deleteItem(29492, 1);
            player.getInventory().addItem(6199, 1);
            player.sm("You've redeemed your bond for a Donator box!");
            end();
		}
	 }
		
	}

	@Override
	public void finish() {
		
	}
	
}