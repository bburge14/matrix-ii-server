package com.rs.game.map.bossInstance.impl;

import com.rs.game.World;
import com.rs.game.map.bossInstance.BossInstance;
import com.rs.game.map.bossInstance.InstanceSettings;
import com.rs.game.player.Player;

/**
 * GWD Armadyl Eyrie instance. Boss NPC 6222 (Kree'arra) spawns at
 * the canonical lair tile (2832, 5302).
 */
public class ArmadylInstance extends BossInstance {

    public ArmadylInstance(Player owner, InstanceSettings settings) {
        super(owner, settings);
    }

    @Override
    public int[] getMapPos() {
        // Chunk SW corner that contains (2832, 5302)
        return new int[] { 354, 662 };
    }

    @Override
    public int[] getMapSize() {
        return new int[] { 1, 1 };
    }

    @Override
    public void enterInstance(Player player, boolean login) {
        if (!login)
            player.getPackets().sendGameMessage("You teleport into the Eyrie of Kree'arra.", true);
        super.enterInstance(player, login);
    }

    @Override
    public void loadMapInstance() {
        World.spawnNPC(6222, getTile(2832, 5302, 0), -1, true, false).setBossInstance(this);
    }
}
