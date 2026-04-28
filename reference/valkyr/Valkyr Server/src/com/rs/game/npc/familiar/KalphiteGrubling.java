package com.rs.game.npc.familiar;

import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.game.player.actions.Summoning.Pouches;

public class KalphiteGrubling extends Familiar {

	private static final long serialVersionUID = 456877449456697395L;

	public KalphiteGrubling(Player owner, Pouches pouch, WorldTile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea) {
		super(owner, pouch, tile, mapAreaNameHash, canBeAttackFromOutOfArea);
		// TODO Auto-generated constructor stub
	}

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
