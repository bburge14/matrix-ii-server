package com.rs.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.rs.game.World;
import com.rs.game.WorldTile;

public final class NPCSpawns {

	public static final void init() {
		loadSpawnsList("data/npcs/spawnsList.txt");
		loadSpawnsList("data/npcs/customSpawnsList.txt");
	}
	
	private static class NPCSpawn {
		
		private NPCSpawn(int npcId, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea) {
			this.npcId = npcId;
			this.tile = tile;
			this.mapAreaNameHash = mapAreaNameHash;
			this.canBeAttackFromOutOfArea = canBeAttackFromOutOfArea;
		}
		
		private int npcId;
		private WorldTile tile; 
		private int mapAreaNameHash;
		private boolean canBeAttackFromOutOfArea;
	}
	
	private static final List<NPCSpawn>[][] spawns = new ArrayList[256][256];

	// Skill-shop NPCs that should still wander (override the static-trader rule)
	private static final java.util.Set<Integer> WALKING_TRADERS = new java.util.HashSet<Integer>(
		java.util.Arrays.asList(15085, 4906) // Nails (pickpocket target) + Wilfred (woods wanderer)
	);

	// NPC IDs that should walk regardless of cache walkMask (for NPCs whose
	// cache definition has walkMask=0 but should still wander - typically
	// pickpocket targets and skill-area background NPCs)
	private static final java.util.Set<Integer> FORCE_WALK_NPCS = new java.util.HashSet<Integer>(
		java.util.Arrays.asList(1, 7, 18, 187, 296, 23, 1905, 20, 21, 2109, 4906)
	);

	private static final void loadSpawnsList(String path) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(path));
			int count = 0;
			while (true) {
				count++;
				String line = in.readLine();
				if (line == null)
					break;
				if (line.startsWith("//") || line.startsWith("RSBOT"))
					continue;
				String[] splitedLine = line.split(" - ", 2);
				if (splitedLine.length != 2) {
					in.close();
					throw new RuntimeException("Invalid NPC Spawn line: " + line + " , line number: " + count);
				}
				int npcId = Integer.parseInt(splitedLine[0]);
				if (npcId >= Utils.getNPCDefinitionsSize())
					continue;
				String[] splitedLine2 = splitedLine[1].split(" ", 5);
				if (splitedLine2.length != 3 && splitedLine2.length != 5) {
					in.close();
					throw new RuntimeException("Invalid NPC Spawn line: " + line + " , line number: " + count);
				}
				WorldTile tile = new WorldTile(Integer.parseInt(splitedLine2[0]), Integer.parseInt(splitedLine2[1]), Integer.parseInt(splitedLine2[2]));
				int mapAreaNameHash = -1;
				boolean canBeAttackFromOutOfArea = true;
				if (splitedLine2.length == 5) {
					mapAreaNameHash = Utils.getNameHash(splitedLine2[3]);
					canBeAttackFromOutOfArea = Boolean.parseBoolean(splitedLine2[4]);
				}
				addNPCSpawn(npcId, tile, mapAreaNameHash, canBeAttackFromOutOfArea);
			}
			in.close();
		} catch (Throwable e) {
			Logger.handle(e);
		}
	}

	public static final void loadNPCSpawns(int regionId) {
		int x = (regionId >> 8) ;
		int y = (regionId & 0xff);
		if(spawns[x][y] == null)
			return;
		for(NPCSpawn spawn : spawns[x][y]) {
			com.rs.game.npc.NPC npc = World.spawnNPC(spawn.npcId, spawn.tile, spawn.mapAreaNameHash, spawn.canBeAttackFromOutOfArea);
			if (npc == null) continue;
			// Skill-shop trader NPCs stay put - no random walk - so players
			// can find them at fixed coords. WALKING_TRADERS is the opt-out
			// for NPCs that should keep wandering (e.g. Nails - pickpocket
			// gameplay needs him to move).
			if (com.rs.net.decoders.handlers.NPCHandler.skillShopForNpc(spawn.npcId) != -1
					&& !WALKING_TRADERS.contains(spawn.npcId))
				npc.setRandomWalk(0);
			else if (FORCE_WALK_NPCS.contains(spawn.npcId))
				npc.setRandomWalk(com.rs.game.npc.NPC.NORMAL_WALK);
		}
		spawns[x][y] = null;
	}

	private static final void addNPCSpawn(int npcId, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea) {
		//World.spawnNPC(npcId, tile, mapAreaNameHash, canBeAttackFromOutOfArea);
		
		int x = tile.getRegionX();
		int y = tile.getRegionY();
		if(spawns[x][y] == null)
			spawns[x][y] = new ArrayList<NPCSpawn>();
		spawns[x][y].add(new NPCSpawn( npcId, tile, mapAreaNameHash, canBeAttackFromOutOfArea));
	}

	private NPCSpawns() {
	}
}
