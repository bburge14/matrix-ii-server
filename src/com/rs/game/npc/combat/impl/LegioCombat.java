package com.rs.game.npc.combat.impl;

import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.utils.Utils;
import com.rs.game.player.Player;
import com.rs.game.player.content.Magic;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

public class LegioCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] {"Legio Sextus", "Legio Quintus", "Legio Quartus", "Legio Tertius", "Legio Secundus", "Legio Primus"};
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		final int hp = npc.getHitpoints();
		int random = Utils.getRandom(5);
		int x = target.getX();
		int y = target.getY();
		int z = target.getPlane();
		int a = npc.getX();
		int b = npc.getY();
		int c = npc.getPlane();
		final WorldTile center = new WorldTile(target);
		if (hp >= 11250 && random >= 4) {
			npc.setNextAnimation(new Animation(20260));
			npc.setNextGraphics(new Graphics(3985));
			delayHit(npc, 0, target, new Hit(npc, Utils.random(450), HitLook.MAGIC_DAMAGE));
			World.sendProjectile(npc, target, 3984, 34, 16, 40, 35, 16, 0);
		}
		if (hp >= 11250 && random == 5) {
			npc.setNextAnimation(new Animation(20260));
			npc.setNextGraphics(new Graphics(3985));
			World.sendGraphics(npc, new Graphics(3974), center);
			WorldTasksManager.schedule(new WorldTask() {
				int count = 0;
				@Override
				public void run() {
					for(Player player : World.getPlayers()) {
						if(player == null || player.isDead() || player.hasFinished())
							continue;
						if(player.withinDistance(center, 2)) {
							delayHit(npc, 0, player, new Hit(npc, Utils.random(150), HitLook.REGULAR_DAMAGE));
						}
					}
					if(count++ == 5) {
						stop();
						return;
					}
				}
			}, 0, 0);
		}
		else if (hp >= 7501 && hp <= 11249 && random >= 4) {
			npc.setNextAnimation(new Animation(20260));
			npc.setNextGraphics(new Graphics(3985));
			delayHit(npc, 0, target, new Hit(npc, Utils.random(320), HitLook.MAGIC_DAMAGE));
			World.sendProjectile(npc, target, 3984, 34, 16, 40, 35, 16, 0);
		}
		else if (hp >= 3750 && hp <= 7500 && random >= 4) {
			npc.setNextAnimation(new Animation(20260));
			npc.setNextGraphics(new Graphics(3985));
			delayHit(npc, 0, target, new Hit(npc, Utils.random(320), HitLook.MAGIC_DAMAGE));
			World.sendProjectile(npc, target, 3984, 34, 16, 40, 35, 16, 0);
		}
		else if (hp >= 3749 && random >= 2){
			npc.setNextAnimation(new Animation(20260));
			npc.setNextGraphics(new Graphics(3985));
			delayHit(npc, 0, target, new Hit(npc, Utils.random(450), HitLook.MAGIC_DAMAGE));
			World.sendProjectile(npc, target, 3984, 34, 16, 40, 35, 16, 0);
		}

		return npc.getAttackSpeed();
	}
}