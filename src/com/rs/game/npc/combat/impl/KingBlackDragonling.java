package com.rs.game.npc.combat.impl;

import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.World;
import com.rs.game.npc.NPC;
import com.rs.game.EffectsManager;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.npc.familiar.Familiar;
import com.rs.utils.Utils;

public class KingBlackDragonling extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 20544 };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int distanceX = target.getX() - npc.getX();
		int distanceY = target.getY() - npc.getY();
		boolean distant = false;
		int size = npc.getSize();
		Familiar familiar = (Familiar) npc;
		boolean usingSpecial = familiar.hasSpecialOn();
		int damage = 0;
		if (distanceX > size || distanceX < -1 || distanceY > size
				|| distanceY < -1)
			distant = true;
		if (usingSpecial) {// priority over regular attack
			npc.setNextAnimation(new Animation(-1));
			target.setNextGraphics(new Graphics(1449));
			if (distant) {// range hit
				delayHit(npc, 2, target, getMagicHit(npc, getMaxHit(npc, 250, NPCCombatDefinitions.MAGE, target)));
			}
		} else {
			if (distant) {
				int attackStage = Utils.getRandom(1);// 2
				switch (attackStage) {
				
					case 0:// magic
						damage = getMaxHit(npc, 255, NPCCombatDefinitions.MAGE, target);
						npc.setNextAnimation(new Animation(7694));
						World.sendProjectile(npc, target, 393, 34, 16, 30, 35, 16, 0);
						delayHit(npc, 2, target, getMagicHit(npc, damage));
					break;
					
					case 1: // poison
						EffectsManager.makePoisoned(target, 80);
						damage = getMaxHit(npc, 255, NPCCombatDefinitions.MAGE, target);
						World.sendProjectile(npc, target, 394, 34, 16, 30, 35, 16, 0);
						delayHit(npc, 2, target, getMagicHit(npc, damage));
					break;
					
				}
			} else {// melee
				damage = getMaxHit(npc, 250, NPCCombatDefinitions.MELEE, target);
				npc.setNextAnimation(new Animation(-1));
				delayHit(npc, 1, target, getMeleeHit(npc, damage));
			}
		}
		return npc.getAttackSpeed();
	}

}
