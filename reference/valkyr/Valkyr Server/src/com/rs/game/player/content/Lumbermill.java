package com.rs.game.player.content;

import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.net.decoders.WorldPacketsDecoder;

/**
 * 
 * @author Andy || ReverendDread Dec 9, 2016
 *
 */

public class Lumbermill {

	private static final int interfaceId = 403;
	
	private static final int regularLogsPrice = 100;
	private static final int oakLogsPrice = 250;
	private static final int teakLogsPrice = 500;
	private static final int mahoganyLogsPrice = 1500;
	
	
	
	public static void openInterface(Player player) {
		player.getInterfaceManager().sendInterface(interfaceId);
	}
	
	public static void handleButtons(Player player, int buttonId, int packetId) {
		switch (buttonId) {
			case 12:
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON1_PACKET) {
					player.sm("Button 1");
					if (player.getInventory().contains(new Item(1511, 1))) {
						if (player.getInventory().getCoinsAmount() >= regularLogsPrice) {
							player.getInventory().deleteItem(new Item(1511));
							player.getMoneyPouch().sendDynamicInteraction(regularLogsPrice, true);
							player.getInventory().addItem(new Item(960));
							return;
						}
						player.sm("You need to have at least " + regularLogsPrice + " coins to do this.");
						return;
					}
					player.sm("You need at least one log to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON2_PACKET) {
					if (player.getInventory().contains(new Item(1511, 5))) {
						if (player.getInventory().getCoinsAmount() > oakLogsPrice * 5) {
							player.getInventory().deleteItem(new Item(1511, 5));
							player.getMoneyPouch().sendDynamicInteraction(oakLogsPrice * 5, true);
							player.getInventory().addItem(new Item(960, 5));
							return;
						}
						player.sm("You need to have at least " + regularLogsPrice * 5 + " coins to do this.");
						return;
					}
					player.sm("You need at least five logs to do this.");
				}		
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON3_PACKET) {
					if (player.getInventory().contains(new Item(1511, 10))) {
						if (player.getInventory().getCoinsAmount() > regularLogsPrice * 10) {
							player.getInventory().deleteItem(new Item(1511, 10));
							player.getMoneyPouch().sendDynamicInteraction(regularLogsPrice * 10, true);
							player.getInventory().addItem(new Item(960, 10));
							return;
						}
						player.sm("You need to have at least " + regularLogsPrice * 10 +  " coins to do this.");
						return;
					}
					player.sm("You need at least ten logs to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON5_PACKET) {					
					int logAmount = player.getInventory().getAmountOf(1511);
					if (logAmount == 0) {
						player.sm("You need to have at least one log to do this.");
						return;
					}
					if (player.getInventory().getCoinsAmount() > regularLogsPrice * logAmount) {
						player.getInventory().deleteItem(new Item(1511, logAmount));
						player.getInventory().addItem(new Item(960, logAmount));
						player.getMoneyPouch().sendDynamicInteraction(regularLogsPrice * logAmount, true);
						return;
					}
					player.sm("You need to have at least " + regularLogsPrice * logAmount +  " coins to do this.");
					return;
				}
				break;
				
			case 13:			
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON1_PACKET) {
					player.sm("Button 1");
					if (player.getInventory().contains(new Item(1521, 1))) {
						if (player.getInventory().getCoinsAmount() > oakLogsPrice) {
							player.getInventory().deleteItem(new Item(1521));
							player.getMoneyPouch().sendDynamicInteraction(oakLogsPrice, true);
							player.getInventory().addItem(new Item(8778));
							return;
						}
						player.sm("You need to have at least " + oakLogsPrice + " coins to do this.");
						return;
					}
					player.sm("You need at least one oak log to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON2_PACKET) {
					if (player.getInventory().contains(new Item(1521, 5))) {
						if (player.getInventory().getCoinsAmount() > oakLogsPrice * 5) {
							player.getInventory().deleteItem(new Item(1521, 5));
							player.getMoneyPouch().sendDynamicInteraction(oakLogsPrice * 5, true);
							player.getInventory().addItem(new Item(8778, 5));
							return;
						}
						player.sm("You need to have at least " + oakLogsPrice * 5 + " coins to do this.");
						return;
					}
					player.sm("You need at least five oak logs to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON3_PACKET) {
					if (player.getInventory().contains(new Item(1521, 10))) {
						if (player.getInventory().getCoinsAmount() > oakLogsPrice * 10) {
							player.getInventory().deleteItem(new Item(1521, 10));
							player.getMoneyPouch().sendDynamicInteraction(oakLogsPrice * 10, true);
							player.getInventory().addItem(new Item(8778, 10));
							return;
						}
						player.sm("You need to have at least " + oakLogsPrice * 10 +  " coins to do this.");
						return;
					}
					player.sm("You need at least ten oak logs to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON5_PACKET) {					
					int logAmount = player.getInventory().getAmountOf(1521);
					if (logAmount == 0) {
						player.sm("You need to have at least one log to do this.");
						return;
					}
					if (player.getInventory().getCoinsAmount() > oakLogsPrice * logAmount) {
						player.getInventory().deleteItem(new Item(1521, logAmount));
						player.getInventory().addItem(new Item(8778, logAmount));
						player.getMoneyPouch().sendDynamicInteraction(oakLogsPrice * logAmount, true);
						return;
					}
					player.sm("You need to have at least " + oakLogsPrice * logAmount +  " coins to do this.");
					return;
				}
				break;
				
			case 14:
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON1_PACKET) {
					if (player.getInventory().contains(new Item(6333, 1))) {
						if (player.getInventory().getCoinsAmount() > teakLogsPrice) {
							player.getInventory().deleteItem(new Item(6333));
							player.getMoneyPouch().sendDynamicInteraction(teakLogsPrice, true);
							player.getInventory().addItem(new Item(8780));
							return;
						}					
						player.sm("You need to have at least " + teakLogsPrice  +  " coins to do this.");
						return;
					}
					player.sm("You need at least one teak log to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON2_PACKET) {
					if (player.getInventory().contains(new Item(6333, 5))) {
						if (player.getInventory().getCoinsAmount() > teakLogsPrice * 5) {
							player.getInventory().deleteItem(new Item(6333, 5));
							player.getMoneyPouch().sendDynamicInteraction(teakLogsPrice * 5, true);
							player.getInventory().addItem(new Item(8780, 5));
							return;
						}
						player.sm("You need to have at least " + teakLogsPrice * 5 +  " coins to do this.");
						return;
					}
					player.sm("You need at least five teak logs to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON3_PACKET) {
					if (player.getInventory().contains(new Item(6333, 10))) {
						if (player.getInventory().getCoinsAmount() > teakLogsPrice * 10) {
							player.getInventory().deleteItem(new Item(6333, 10));
							player.getMoneyPouch().sendDynamicInteraction(teakLogsPrice * 10, true);
							player.getInventory().addItem(new Item(8780, 10));
							return;
						}
						player.sm("You need to have at least " + teakLogsPrice * 10 +  " coins to do this.");
						return;
					}
					player.sm("You need at least ten teak logs to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON5_PACKET) {					
					int logAmount = player.getInventory().getAmountOf(6333);
					if (logAmount == 0) {
						player.sm("You need to have at least one log to do this.");
						return;
					}
					if (player.getInventory().getCoinsAmount() > oakLogsPrice * logAmount) {
						player.getInventory().deleteItem(new Item(6333, logAmount));
						player.getInventory().addItem(new Item(8780, logAmount));
						player.getMoneyPouch().sendDynamicInteraction(oakLogsPrice * logAmount, true);
						return;
					}
					player.sm("You need to have at least " + oakLogsPrice * logAmount +  " coins to do this.");
					return;
				}
				break;
				
			case 15:
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON1_PACKET) {
					if (player.getInventory().contains(new Item(6332, 1))) {
						if (player.getInventory().getCoinsAmount() > mahoganyLogsPrice) {
							player.getInventory().deleteItem(new Item(6332));
							player.getMoneyPouch().sendDynamicInteraction(mahoganyLogsPrice, true);
							player.getInventory().addItem(new Item(8782));
							return;
						}
						player.sm("You need to have at least " + mahoganyLogsPrice +  " coins to do this.");
						return;
					}
					player.sm("You need at least one mahogany log to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON2_PACKET) {
					if (player.getInventory().contains(new Item(6332, 5))) {
						if (player.getInventory().getCoinsAmount() > mahoganyLogsPrice * 5) {
							player.getInventory().deleteItem(new Item(6332, 5));
							player.getMoneyPouch().sendDynamicInteraction(mahoganyLogsPrice * 5, true);
							player.getInventory().addItem(new Item(8782, 5));
							return;
						}
						player.sm("You need to have at least " + mahoganyLogsPrice * 5 +  " coins to do this.");
						return;
					}
					player.sm("You need at least five mahogany logs to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON3_PACKET) {
					if (player.getInventory().contains(new Item(6332, 10))) {
						if (player.getInventory().getCoinsAmount() > mahoganyLogsPrice * 10) {
							player.getInventory().deleteItem(new Item(6332, 10));
							player.getMoneyPouch().sendDynamicInteraction(mahoganyLogsPrice * 10, true);
							player.getInventory().addItem(new Item(8782, 10));
							return;
						}
						player.sm("You need to have at least " + mahoganyLogsPrice * 10 +  " coins to do this.");
						return;
					}
					player.sm("You need at least ten mahogany logs to do this.");
				}
				if (packetId == WorldPacketsDecoder.ACTION_BUTTON5_PACKET) {					
					int logAmount = player.getInventory().getAmountOf(6332);
					if (logAmount == 0) {
						player.sm("You need to have at least one log to do this.");
						return;
					}
					if (player.getInventory().getCoinsAmount() > mahoganyLogsPrice * logAmount) {
						player.getInventory().deleteItem(new Item(6332, logAmount));
						player.getInventory().addItem(new Item(8782, logAmount));
						player.getMoneyPouch().sendDynamicInteraction(mahoganyLogsPrice * logAmount, true);
						return;
					}
					player.sm("You need to have at least " + mahoganyLogsPrice * logAmount +  " coins to do this.");
					return;
				}
				break;
			
			default:
				player.sm("This button is a null.");
		}
	}
	
}
