package com.rs.game.npc.others;

import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;

@SuppressWarnings("serial")
public class Airut extends NPC {
    

	public Airut(int id, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea, boolean spawned) {
		super(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, spawned);
		setLureDelay(0);
        setCapDamage(1000);
		setForceAgressive(true);
	}
	
	@Override
	public double getMeleePrayerMultiplier() {
		return 0.25;
	}
}