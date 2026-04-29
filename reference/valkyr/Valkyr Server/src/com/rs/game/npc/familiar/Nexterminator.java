package com.rs.game.npc.familiar;

import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.game.player.actions.Summoning.Pouches;

public class Nexterminator extends Familiar {

	public Nexterminator(Player owner, Pouches pouch, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea) {
		super(owner, pouch, tile, mapAreaNameHash, canBeAttackFromOutOfArea);
	}

	private static final long serialVersionUID = -9181393770444014076L;

	@Override
	public String getSpecialName() {
		return "Cuckold";
	}

	@Override
	public String getSpecialDescription() {
		return "Cuckold";
	}

	@Override
	public int getBOBSize() {
		return 0;
	}

	@Override
	public int getSpecialAmount() {
		return 0;
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
