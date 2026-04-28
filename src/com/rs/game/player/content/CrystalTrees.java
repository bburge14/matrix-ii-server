package com.rs.game.player.content;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.rs.game.WorldObject;
import com.rs.game.WorldTile;
import com.rs.game.player.Player;

public class CrystalTrees {
	
	private static final int CRYSTAL_TREE = 87533;
	
	private WorldTile location;
	
	public static enum TreeLocations {
		
		//Placeholder tiles
		SEERS_VILLAGE(new WorldTile(2748, 3458, 0)),
		FALADOR(new WorldTile(1, 1, 0)),
		BRIMHAVEN(new WorldTile(1, 1, 0)),
		YANILLE(new WorldTile(1, 1, 0)),
		OBSERVATORY(new WorldTile(1, 1, 0)),
		TREE_GNOME_STRONGHOLD(new WorldTile(1, 1, 0)),
		LIGHT_HOUSE(new WorldTile(1, 1, 0)),
		PRIFDDINAS(new WorldTile(1, 1, 0));
		
		public WorldTile location;
		
		private TreeLocations(WorldTile location) {
			this.location = location;
		}		
		
		public String getLocation() {
			return location.toString();
		}
	}
	
}
