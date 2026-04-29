package com.rs.game.npc.familiar;

import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.game.player.actions.Summoning.Pouches;

public class KingBlackDragonling extends Familiar {

	private static final long serialVersionUID = 102480789844450847L;

	public KingBlackDragonling(Player owner, Pouches pouch, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea) {
		super(owner, pouch, tile, mapAreaNameHash, canBeAttackFromOutOfArea);
	}

	@Override
	public String getSpecialName() {
		return "Firebreath";
	}

	@Override
	public String getSpecialDescription() {
		return "Unleashes a firey blaze upon your enemy.";
	}

	@Override
	public int getBOBSize() {
		return 30;
	}

	@Override
	public int getSpecialAmount() {
		return 30;
	}
	
    @Override
    public boolean isAgressive() {
    	return true;
    }

	@Override
	public SpecialAttack getSpecialAttack() {
		return SpecialAttack.ENTITY;
	}

	@Override
	public boolean submitSpecial(Object context) {
		return true;
	}

}
