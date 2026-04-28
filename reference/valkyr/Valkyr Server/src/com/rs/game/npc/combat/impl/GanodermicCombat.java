package com.rs.game.npc.combat.impl;

import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.ForceTalk;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.World;
import com.rs.game.npc.NPC;
import com.rs.game.npc.slayer.GanodermicBeast;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Utils;



@SuppressWarnings("unused")
public class GanodermicCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 14696, 14697 };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int attackStyle = Utils.getRandom(4);
		if (isDistant(npc, target))
			if(Utils.random(2) == 0)
				Jump(npc, target);
		if (attackStyle == 0 || attackStyle == 1) {
			if (isDistant(npc, target))
				npc.setNextAnimation(new Animation(15470));
			npc.setNextGraphics(new Graphics(2034));
			npc.setNextGraphics(new Graphics(2038));
			delayHit(npc, 1,target, getMagicHit(npc,getRandomMaxHit(npc, 650,NPCCombatDefinitions.MAGE, target)));
			World.sendProjectile(npc, target, 2035, 70, 32, 30, 50, 0, 0);
		}        //if they're far away, the beast will mage them
			else {
				npc.setNextAnimation(new Animation(15470));
				npc.setNextGraphics(new Graphics(2034));
				npc.setNextGraphics(new Graphics(2038));
				delayHit(npc, 1,target, getMagicHit(npc,getRandomMaxHit(npc, 650,NPCCombatDefinitions.MAGE, target)));
				World.sendProjectile(npc, target, 2035, 70, 32, 30, 50, 0, 0);
				return defs.getAttackDelay();
			}
		if (attackStyle == 2) {//fungal attack
			if (!isDistant(npc, target))
			specialAttack(npc, target);
		} else if (attackStyle == 3) {
			npc.setNextAnimation(new Animation(15466));
			delayHit(npc, 1, target, getMeleeHit(npc, getRandomMaxHit(npc, 550,NPCCombatDefinitions.MELEE, target)));
		}
		return defs.getAttackDelay();
	}
	
{
	
}
public void Jump(final NPC npc, final Entity target) {
	talk(npc, "Krrr!");
	npc.faceEntity(target);
	npc.addWalkSteps(target.getX(), target.getY() + 1);
	WorldTasksManager.schedule(new WorldTask() {
		int loop;

		@Override
		public void run() {
			if (loop == 2) 
			if (!isDistant(npc, target)) {
				specialAttack(npc, target);
			}
			loop++;
		}
	}, 0, 1);
}

public void specialAttack(final NPC npc, final Entity target) {
	move(npc, 15468);
	talk(npc, "Krrr!");
	((Player) target).getPackets().sendGameMessage("The creature infests you with its toxic fungus");
	WorldTasksManager.schedule(new WorldTask() {
		int loop;

		@Override
		public void run() {
			if (loop == 1) {
				target.applyHit(
					new Hit(target, Utils.random(20, 150), HitLook.POISON_DAMAGE));
				
			}
			if (isDistant(npc, target)) {
				stop();
			}
			loop++;
		}
	}, 0, 1);
}

public boolean isDistant(NPC npc, Entity target) {
	int size = npc.getSize();
	int dX = target.getX() - npc.getX();
	int dY = target.getY() - npc.getY();
if (dX > size || dX < -1 || dY > size
		|| dY < -1) {
	return true;
	
}
return false;
}
public void talk(NPC npc, String text) {
	npc.setNextForceTalk(new ForceTalk(text));
}
public void move(NPC npc, int animation) {
	npc.setNextAnimation(new Animation(animation));
}
public void gfx(NPC npc, int gfx) {
	npc.setNextGraphics(new Graphics(15491));
}

}
