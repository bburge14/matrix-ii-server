package com.rs.game.npc.vorago;

import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.WorldTile;
import com.rs.game.Hit.HitLook;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

@SuppressWarnings("serial")
public class Vorago extends NPC {
	
	public Vorago(int id, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea, boolean spawned) {
		super(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, spawned);
		setForceAgressive(true);
		setForceMultiAttacked(true);
		setCapDamage(1000);
		setLureDelay(0);
	}	
	
	@Override
	public double getMagePrayerMultiplier() {
		return 0.8;
	}

	@Override
	public double getRangePrayerMultiplier() {
		return 0.8;
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return 0.8;
	}
	
	@Override
	public void processNPC() {
		if (getFreezeDelay() > 0)
			setFreezeDelay(0);
		super.processNPC();
	}
	
	@Override
	public void handleIngoingHit(Hit hit) {
		super.handleIngoingHit(hit);
	}
	
	
	@Override
	public void sendDeath(Entity source) {
		final NPCCombatDefinitions defs = getCombatDefinitions();
		resetWalkSteps();
		getCombat().removeTarget();
		setNextAnimation(null);
		WorldTasksManager.schedule(new WorldTask() {
			int loop;

			@Override
			public void run() {
				if (loop == 0) {
					setNextAnimation(new Animation(defs.getDeathEmote()));
				} else if (loop >= defs.getDeathDelay()) {
					drop();
					reset();
					setLocation(getRespawnTile());
					finish();
					setRespawnTask();
					stop();
				}
				loop++;
			}
		}, 0, 1);
	}
	
}