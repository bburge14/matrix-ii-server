package com.rs.game.npc.others;

import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;

@SuppressWarnings("serial")
public class Legio extends NPC {

	public Legio(int id, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea, boolean spawned) {
		super(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, spawned);
		setLureDelay(0);
		setCapDamage(1000);
		setForceAgressive(true);
	}
	
	@Override
	public double getMagePrayerMultiplier() {
		return 0.0;
	}

	@Override
	public double getRangePrayerMultiplier() {
		return 0.0;
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return 0.0;
	}
}