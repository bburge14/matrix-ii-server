package com.rs.game.npc.others;

import java.util.concurrent.TimeUnit;

import com.rs.cores.CoresManager;
import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Hit;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.Hit.HitLook;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

@SuppressWarnings("serial")
public class Glacor extends NPC {

	
	public Glacor(int id, WorldTile tile, int mapAreaNameHash,
			boolean canBeAttackFromOutOfArea, boolean spawned) {
		super(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, spawned);
		setLureDelay(0);
		setRun(true);
		setForceTargetDistance(64);
		setForceMultiAttacked(true);
	}
	
	@Override
	public void handleIngoingHit(final Hit hit) {
		super.handleIngoingHit(hit);
		if (hit.getSource() instanceof Player) {
			Player player = (Player) hit.getSource();
			if (hit.getLook() == HitLook.MELEE_DAMAGE || hit.getLook() == HitLook.RANGE_DAMAGE) {
				hit.setDamage(0);
				player.getPackets().sendGameMessage("Glacors can only be harmed by magic.");
			}
		}
	}
	
}