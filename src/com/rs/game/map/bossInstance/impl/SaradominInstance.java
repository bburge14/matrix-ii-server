package com.rs.game.map.bossInstance.impl;

import com.rs.game.World;
import com.rs.game.map.bossInstance.BossInstance;
import com.rs.game.map.bossInstance.InstanceSettings;
import com.rs.game.player.Player;

/**
 * GWD Saradomin Encampment instance. Boss NPC 6247 (Commander
 * Zilyana) spawns at the canonical lair tile (2924, 5250).
 */
public class SaradominInstance extends BossInstance {

    public SaradominInstance(Player owner, InstanceSettings settings) {
        super(owner, settings);
    }

    @Override
    public int[] getMapPos() {
        return new int[] { 365, 656 };
    }

    @Override
    public int[] getMapSize() {
        return new int[] { 1, 1 };
    }

    @Override
    public void enterInstance(Player player, boolean login) {
        if (!login)
            player.getPackets().sendGameMessage("You teleport into Commander Zilyana's encampment.", true);
        super.enterInstance(player, login);
    }

    @Override
    public void loadMapInstance() {
        World.spawnNPC(6247, getTile(2924, 5250, 0), -1, true, false).setBossInstance(this);
    }
}
