package com.rs.game.cores.mysql.impl;

import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.rs.game.cores.mysql.Database;
import com.rs.game.item.Item;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.utils.Utils;

/**
 * @author Andy || ReverendDread Feb 19, 2017
 */
public class DropLogger implements Runnable {
	
	private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private Player player;
	
	private Item item;
	
	private NPC npc;
	
	public DropLogger(Player player, NPC npc, Item item) {
		this.player = player;
		this.npc = npc;
		this.item = item;
	}

	@Override
	public void run() {
		
		try {
			
			Date date = new Date();
			
			Database db = new Database("valius.org", "wwwvalpk_admin69", "nigger42069", "wwwvalpk_logs");
			
			String playerName = Utils.formatString(player.getUsername());
			
			String dropName = Utils.formatString(item.getName());
			
			String npcName = Utils.formatString(npc.getName());
			
			int dropAmount = item.getAmount();
			
			if (!db.init()) {
				System.err.println("[DropLogger]:Failed to update " + player.getUsername() + "'s drop. Database could not connect.");
				return;
			}
			
			if (player.getRights() >= 2) {
				System.out.println("[DropLogger]: Unable to save drop for player + " + player.getUsername());
				return;
			}
			
			PreparedStatement stmt1 = db.prepare(generateQuery());
			stmt1.setString(1, sdf.format(date));
			stmt1.setString(2, playerName);
			stmt1.setString(3, npcName);
			stmt1.setString(4, dropName);
			stmt1.setInt(5, dropAmount);

			stmt1.execute();
			
			db.destroyAll();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static String generateQuery() {
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO drop_logs (");
		sb.append("time, ");
		sb.append("playername, ");
		sb.append("npcname, ");
		sb.append("dropname, ");
		sb.append("dropamount) ");
		sb.append("VALUES (?, ?, ?, ?, ?)");
		return sb.toString();
	}
	
	
	
}
