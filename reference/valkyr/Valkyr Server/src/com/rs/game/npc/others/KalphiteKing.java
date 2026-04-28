package com.rs.game.npc.others;

import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.player.Player;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Utils;

@SuppressWarnings("serial")
public class KalphiteKing extends NPC {
    
    private long lastSwitch;

	public KalphiteKing(int id, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea, boolean spawned) {
		super(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, spawned);
		setLureDelay(0);
        setCapDamage(1000);
		setForceAgressive(true);
        setForceMultiAttacked(true);
	}
    
    public int getCurrentPhase() {
		return getId() - 16697;
	}
    
    public boolean canSwitch() {
		return lastSwitch + 200 <= Utils.currentWorldCycle();
	}
	
	private void resetSwitch() {
		lastSwitch = Utils.currentWorldCycle();
	}
    
    @Override
	public void processNPC() {
		if (isDead())
			return;
		if(canSwitch()) {
			switchPhase();
        }
		super.processNPC();
    }  
    
    private void switchPhase() {
		int currentPhase = getCurrentPhase();
		int nextPhase = (currentPhase + Utils.random(2) + 1) % 3;
		setNextNPCTransformation(16697 + nextPhase);
		setNextGraphics(new Graphics(nextPhase == 0 ? 3750 : nextPhase == 1 ? 3749 : 3751));
		resetSwitch();
	}
    
    @Override
	public void handleIngoingHit(Hit hit) {
		if (Utils.getRandom(25) == 0)
            switchPhase();
		super.handleIngoingHit(hit);
	}
    
    public void dig(final Entity target) {
		setNextAnimation(new Animation(19453));
		setNextGraphics(new Graphics(3746));
		WorldTasksManager.schedule(new WorldTask() {

			boolean part1 = true;
			
			@Override
			public void run() {
				if(part1) {
					setFinished(true);
					part1 = false;
				}
				else {
					stop();

					//if instance spawned be sure that the player didnt leave or the boss would tp outside
					//if npc dont make it tp under to be sure it doesnt bug (shouldnt happen anyway)
					if (target instanceof Player) {
						WorldTile loc = new WorldTile(target.getX() - (getSize() / 2), target.getY() - (getSize() / 2), target.getPlane());
						if (World.isFloorFree(loc.getPlane(), loc.getX(), loc.getY(), getSize()))
							setLocation(loc);
					}
					setFinished(false);
					getOutsideEarth(target);
				}
			}
		}, 6, 5);
		
	}
	
	private void getOutsideEarth(Entity target) {
		setNextAnimation(new Animation(19451));
		setNextGraphics(new Graphics(3745));
		WorldTasksManager.schedule(new WorldTask() {

			@Override
			public void run() {
				if(target != null)
					setTarget(target);
				setNextAnimation(new Animation(-1));
				for(Entity target : getPossibleTargets()) {
					if(Utils.colides(KalphiteKing.this, target)) {
						target.applyHit(new Hit(KalphiteKing.this, Utils.random(400, 700), HitLook.REGULAR_DAMAGE));
						if(target instanceof Player) 
							target.setNextAnimation(new Animation(10070));
					}
				}
				
			}
			
		}, 5);
	}
}
