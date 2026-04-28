package com.rs.game.npc.combat.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.Colour;
import com.rs.game.Hit.HitLook;
import com.rs.game.ForceMovement;
import com.rs.game.Projectile;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.player.Player;
import com.rs.game.npc.others.KalphiteKing;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Utils;

public class KalphiteKingCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 16697, 16698, 16699 };
	}
	
	@Override
	public int attack(final NPC npc, final Entity target) {
		
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		
		KalphiteKing king = (KalphiteKing) npc;
				
		int attackStyle = Utils.random(5);
		
		boolean dig = Utils.random(15) == 1;
				
		if (dig) {
			king.dig(target);
			return defs.getAttackDelay();
		}
		
		switch(npc.getId()) {
				
			case 16697: //melee //DONE
				npc.setNextAnimation(new Animation(19449));
				if (Utils.random(15) == 0) {
					   WorldTasksManager.schedule(new WorldTask() {

							@Override
							public void run() {
								if(target instanceof Player) {
									((Player)target).lock(15);
									((Player)target).stopAll();
									((Player)target).setNextAnimation(new Animation(-1)); //to stop abilities emotes
									((Player)target).setNextColour(new Colour(1, 640, 70, 110, 90, 130));
									((Player)target).getPackets().sendGameMessage("<col=ff0000>The Kalphite King has imobilised you while preparing for a powerful attack. You are unable to move.</col>");
								}
								WorldTasksManager.schedule(new WorldTask() {
									@Override
									public void run() {
										target.applyHit(new Hit(npc, 3000, HitLook.REGULAR_DAMAGE));
									}
								}, 16);
						   };
					   }, 1);
						
				} else {
					delayHit(npc, 0, target, getMeleeHit(npc, getRandomMaxHit(npc, 450, NPCCombatDefinitions.MELEE, target)));
				}
			break;
			  
			case 16698: //mage //DONE
				if (attackStyle <= 3) {
					npc.setNextAnimation(new Animation(19448));
					npc.setNextGraphics(new Graphics(3757));
					for (Entity t : npc.getPossibleTargets()) {
						World.sendProjectile(npc, t, 3758, 100, 30, 80, 2, 16, 0);
						t.setNextGraphics(new Graphics(3759));
						delayHit(npc, 0, t, getMagicHit(npc, getRandomMaxHit(npc, 400, NPCCombatDefinitions.MAGE, t)));
					}
				}
				if (attackStyle >= 4) {
					boolean twoOrbs = Utils.random(2) == 1;
					
					npc.setNextAnimation(new Animation(19448));
					npc.setNextGraphics(new Graphics(3742));
					for (Entity t : npc.getPossibleTargets()) {
						final WorldTile tile = new WorldTile(t);
							
						WorldTasksManager.schedule(new WorldTask() {

							@Override
							public void run() {
								World.sendGraphics(npc, new Graphics(3743), tile); //TODO correct gfx here for ball in floor
								WorldTasksManager.schedule(new WorldTask() {
									@Override
									public void run() {
										World.sendGraphics(npc, new Graphics(3752), tile);
										for (Entity t : npc.getPossibleTargets()) {
											if (t.withinDistance(tile, 2)) {
												if(twoOrbs) {
													t.applyHit(new Hit(npc, Utils.random(250) + 1, HitLook.REGULAR_DAMAGE));
													t.applyHit(new Hit(npc, Utils.random(250) + 1, HitLook.REGULAR_DAMAGE));
												}else{
													if(Utils.random(2) == 1) 
														t.applyHit(new Hit(npc, Utils.random(250) + 1, HitLook.REGULAR_DAMAGE));
													else
														t.applyHit(new Hit(npc, Utils.random(250) + 1, HitLook.REGULAR_DAMAGE));
												}
											}
										}
									}
								}, 2);
							}
						}, 3);
					}
				}
			break;
			   
			case 16699: //range //DONE
				
				if (attackStyle <= 3) {
					
					npc.setNextAnimation(new Animation(19450));
					
					for (Entity t : npc.getPossibleTargets()) {
						World.sendProjectile(npc, t, 3747, 100, 30, 80, 2, 16, 0);
						delayHit(npc, 0, t, getRangeHit(npc, getRandomMaxHit(npc, 400, NPCCombatDefinitions.SPECIAL, t)));
					}
				}
				if (attackStyle >= 4) {
					List<Entity> list = king.getPossibleTargets();
					Collections.shuffle(list);
					int c = 0;
					for(Entity t : list) {
						if(c++ == 3)
							break;
                        t.applyHit(new Hit(npc, Utils.random(300, 450) + 50, HitLook.REGULAR_DAMAGE));
						t.setNextGraphics(new Graphics(3522));
					}
				}  
			break;
		}
		return defs.getAttackDelay();
	 }           
}