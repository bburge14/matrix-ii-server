package com.rs.game.npc.corp;

import com.rs.game.Entity;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.player.Player;
import com.rs.cache.loaders.ItemDefinitions;

@SuppressWarnings("serial")
public class CorporealBeast extends NPC {

	private DarkEnergyCore core;

	public CorporealBeast(int id, WorldTile tile, int mapAreaNameHash,
			boolean canBeAttackFromOutOfArea, boolean spawned) {
		super(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, spawned);
		setCapDamage(1000);
		setLureDelay(3000);
		setForceTargetDistance(64);
		setForceFollowClose(false);
	}

	public void spawnDarkEnergyCore() {
		if (core != null)
			return;
		core = new DarkEnergyCore(this);
	}

	public void removeDarkEnergyCore() {
		if (core == null)
			return;
		core.finish();
		core = null;
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (isDead())
			return;
		int maxhp = getMaxHitpoints();
		if (maxhp > getHitpoints() && getPossibleTargets().isEmpty())
			setHitpoints(maxhp);
	}
	
	@Override
	public void handleIngoingHit(final Hit hit) {
		reduceHit(hit);
		super.handleIngoingHit(hit);
	}
	
	public void reduceHit(Hit hit) {
		if (!(hit.getSource() instanceof Player) || (hit.getLook() != HitLook.MELEE_DAMAGE && hit.getLook() != HitLook.RANGE_DAMAGE && hit.getLook() != HitLook.MAGIC_DAMAGE))
			return;
		Player from = (Player) hit.getSource();
		int weaponId = from.getEquipment().getWeaponId();
		String name = weaponId == -1 ? "null" : ItemDefinitions.getItemDefinitions(weaponId).getName().toLowerCase();
		if(hit.getLook() != HitLook.MELEE_DAMAGE || !name.contains("spear"))
			hit.setDamage(hit.getDamage() / 100);
		
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		if (core != null)
			core.sendDeath(source);
	}

	@Override
	public double getMagePrayerMultiplier() {
		return 0.6;
	}

}
