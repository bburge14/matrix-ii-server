package com.rs.game.npc.combat.impl;

import java.util.ArrayList;

import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.npc.corp.CorporealBeast;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Utils;

public class GlacorCombat extends CombatScript{

	public Object[] getKeys() {
		return (new Object[]{Integer.valueOf(14301)});
	}

	public NPC Glacor;
	
	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
        int distanceX = target.getX() - npc.getX();
        int distanceY = target.getY() - npc.getY();
		Player player = (Player) target;
			switch (Utils.getRandom(3)) {
			case 0:
				if (distanceX < 15 || distanceY < 15){
					npc.setNextAnimation(new Animation(9968));
					npc.setNextGraphics(new Graphics(902));
					delayHit(npc, 0, player, new Hit(npc, Utils.random(450), HitLook.MAGIC_DAMAGE));
					World.sendProjectile(npc, target, 963, 34, 16, 40, 35, 16, 0);
				}
				break;
			case 1:
				if (distanceX < 15 || distanceY < 15){
					npc.setNextAnimation(new Animation(9968));
					npc.setNextGraphics(new Graphics(902));
					delayHit(npc, 0, player, new Hit(npc, Utils.random(320), HitLook.RANGE_DAMAGE));
					World.sendProjectile(npc, target, 962, 34, 16, 40, 35, 16, 0);
				}
				break;
			case 2:
				if (distanceX < 15 || distanceY < 15){
					npc.setNextAnimation(new Animation(9968));
					npc.setNextGraphics(new Graphics(902));
					delayHit(npc, 0, player, new Hit(npc, Utils.random(320), HitLook.MAGIC_DAMAGE));
					World.sendProjectile(npc, target, 963, 34, 16, 40, 35, 16, 0);
				}
				break;
			case 3:
				if (distanceX < 15 || distanceY < 15){
					npc.setNextAnimation(new Animation(9968));
					npc.setNextGraphics(new Graphics(902));
					delayHit(npc, 0, player, new Hit(npc, Utils.random(450), HitLook.RANGE_DAMAGE));
					World.sendProjectile(npc, target, 962, 34, 16, 40, 35, 16, 0);
				}
				break;
			}
		return defs.getAttackDelay();
	}
}