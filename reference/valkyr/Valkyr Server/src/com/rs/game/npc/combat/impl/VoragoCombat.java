package com.rs.game.npc.combat.impl;

import com.rs.game.Entity;
import com.rs.game.ForceTalk;
import com.rs.game.Graphics;
import com.rs.game.WorldTile;
import com.rs.game.World;
import com.rs.game.Animation;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.utils.Utils;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;

public class VoragoCombat extends CombatScript {

	private WorldTile[] groundBreak = {new WorldTile(3552, 9502, 0), new WorldTile(3543, 9508, 0), 
			new WorldTile(3557, 9508, 0), new WorldTile(3558, 9494, 0), new WorldTile(3544, 9496, 0), 
			new WorldTile(3548, 9510, 0), new WorldTile(3552, 9510, 0), new WorldTile(3553, 9506, 0),
			new WorldTile(3560, 9511, 0), new WorldTile(3560, 9506, 0), new WorldTile(3560, 9498, 0),
			new WorldTile(3554, 9495, 0), new WorldTile(3549, 9494, 0), new WorldTile(3542, 9499, 0),
			new WorldTile(3547, 9500, 0), new WorldTile(3549, 9502, 0), new WorldTile(3549, 9502, 0),
			new WorldTile(3551, 9503, 0), new WorldTile(3554, 9497, 0), new WorldTile(3556, 9499, 0),
			new WorldTile(3555, 9503, 0),
	
	};

	@Override
	public Object[] getKeys() {
		return new Object[] { 17182 };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		int attackStyle = Utils.random(10);
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		final WorldTile center = new WorldTile(target);
		for (Player player : World.getPlayers()) {
			if (player == null || player.isDead() || player.hasFinished())
				continue;
			if (player.withinDistance(npc, 10)) {
				if (attackStyle == 0 || attackStyle == 1 || attackStyle == 2) {
					npc.setNextAnimation(new Animation(20356));
					//npc.setNextGraphics(new Graphics(4022));
					World.sendProjectile(npc, player, 4023, 100, 40, 50, 1, 1, 0);
					World.sendProjectile(npc, target, 4023, 100, 40, 50, 1, 1, 0);
					delayHit(npc, 2, target, new Hit(npc, Utils.random(400, 500), HitLook.MELEE_DAMAGE));
					delayHit(npc, 2, player, new Hit(npc, Utils.random(400, 500), HitLook.MELEE_DAMAGE));
					WorldTasksManager.schedule(new WorldTask() {
						@Override
						public void run() {
							player.setNextGraphics(new Graphics(4024));
							target.setNextGraphics(new Graphics(4024));
							stop();
						}

					}, 2, 0);
					return 10;
				}
				else if (attackStyle == 3 || attackStyle == 4 || attackStyle == 5 || attackStyle == 8) {
					npc.setNextAnimation(new Animation(20356));
					//npc.setNextGraphics(new Graphics(4015));
					World.sendProjectile(npc, player, 4016, 100, 40, 50, 1, 1, 0);
					World.sendProjectile(npc, target, 4016, 100, 40, 50, 1, 1, 0);
					delayHit(npc, 2, player, new Hit(npc, Utils.random(400, 500), HitLook.MAGIC_DAMAGE));
					delayHit(npc, 2, target, new Hit(npc, Utils.random(400, 500), HitLook.MAGIC_DAMAGE));
					WorldTasksManager.schedule(new WorldTask() {
						
						@Override
						public void run() {
							player.setNextGraphics(new Graphics(4017));
							//target.setNextGraphics(new Graphics(4017));
							stop();
						}

					}, 2, 0);
					return 10;
				}
				else if (attackStyle == 6) {
					npc.setNextAnimation(new Animation(20369));
					npc.setNextGraphics(new Graphics(4021));
					WorldTasksManager.schedule(new WorldTask() {		
						
						@Override
						public void run() {
							if (player.withinDistance(center, 2)) {
								World.sendGraphics(npc, new Graphics(4019), new WorldTile(center));
								World.sendGraphics(npc, new Graphics(3096), new WorldTile(center));
								delayHit(npc, 1, player, new Hit(npc, Utils.random(150, 500), HitLook.REGULAR_DAMAGE));
							}
							stop();
						}

					}, 3, 0);
					return 10;
				}
				else if (attackStyle == 7) {
					npc.setNextAnimation(new Animation(20363));
					npc.setNextGraphics(new Graphics(4021));
					delayHit(npc, 4, player, new Hit(npc, player.getHitpoints() - 1, HitLook.REGULAR_DAMAGE));
					delayHit(npc, 4, target, new Hit(npc, 1, HitLook.REGULAR_DAMAGE));
					for (WorldTile ultimate : groundBreak) {
						World.sendProjectile(npc, npc, ultimate, 4016, 100, 0, 40, 1, 1, 0);
						WorldTasksManager.schedule(new WorldTask() {		
							
							@Override
							public void run() {
								World.sendGraphics(npc, new Graphics(4019), ultimate);
								player.setNextGraphics(new Graphics(4017));
								//target.setNextGraphics(new Graphics(4017));
								stop();
							}

						}, 2, 0);
					}						
					return 10;
				}
				else if (attackStyle == 9 || attackStyle == 10) {
					npc.setNextAnimation(new Animation(20369));
					npc.setNextGraphics(new Graphics(4021));
					WorldTasksManager.schedule(new WorldTask() {		
						
						@Override
						public void run() {
							if (player.withinDistance(center, 2)) {
								World.sendGraphics(npc, new Graphics(4019), new WorldTile(center));
								World.sendGraphics(npc, new Graphics(3096), new WorldTile(center));
								delayHit(npc, 1, target, new Hit(npc, Utils.random(150, 500), HitLook.REGULAR_DAMAGE));
								delayHit(npc, 1, player, new Hit(npc, Utils.random(150, 500), HitLook.REGULAR_DAMAGE));
							}
							stop();
						}

					}, 3, 0);
					return 10;
				}
			}						
		}
		return defs.getAttackDelay();
	}

	protected void applyBleed(Player player) {
		WorldTasksManager.schedule(new WorldTask() {
			
			boolean bleeding;
			int ticks;
			
			@Override
			public void run() {
				if (bleeding)
					stop();
				else {
					if (ticks == 0) {
						bleeding = true;
						player.applyHit(new Hit(player, ticks * 50, HitLook.REGULAR_DAMAGE));
						player.setNextGraphics(new Graphics(3821));
					}
					if (ticks >= 1) {
						player.applyHit(new Hit(player, ticks * 50, HitLook.REGULAR_DAMAGE));
						player.setNextGraphics(new Graphics(3821));
					}
					if (ticks == 10) {
						bleeding = false;
						stop();
						return;
					}
					ticks++;
				}
			}
			
		}, 0, 1);
	}
}