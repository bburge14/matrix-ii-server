package com.rs.game.npc.combat.impl;

import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.ForceTalk;
import com.rs.game.Graphics;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.utils.Utils;

@SuppressWarnings("serial")
public class AirutCombat extends CombatScript {

    @Override
    public Object[] getKeys() {
        return new Object[] { "Airut" };
    }
	
    private static final Animation MELEE = new Animation(22175);
	 
	@Override 
    public int attack(final NPC npc, final Entity target) {
        
        final NPCCombatDefinitions defs = npc.getCombatDefinitions();

        switch (Utils.getRandom(2)) {
	        case 0:
	            npc.setNextAnimation(MELEE);
                delayHit(npc, 1, target, getMeleeHit(npc, getMaxHit(npc, 400, NPCCombatDefinitions.MELEE, target)));
	        break;
	     
		    case 1:
                npc.setNextAnimation(new Animation(22169)); 
                delayHit(npc, 1, target, getMeleeHit(npc, getMaxHit(npc, 400, NPCCombatDefinitions.MELEE, target)));
                npc.setNextForceTalk(new ForceTalk("Rarrgh!!!"));
		    break;
	     
	        case 2:
                npc.setNextAnimation(new Animation(22170));
                delayHit(npc, 1, target, getMeleeHit(npc, getMaxHit(npc, 200, NPCCombatDefinitions.MELEE, target)));
                npc.setNextForceTalk(new ForceTalk("Enough!"));
            break;
	     }
	     return npc.getAttackSpeed();
	 
    }
}
	 

