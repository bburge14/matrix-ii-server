package com.rs.game.map.bossInstance.impl;

import com.rs.game.World;
import com.rs.game.map.bossInstance.BossInstance;
import com.rs.game.map.bossInstance.InstanceSettings;
import com.rs.game.player.Player;

/**
 * GWD Zamorak Fortress instance. Boss NPC 6203 (K'ril Tsutsaroth)
 * spawns at the canonical lair tile (2926, 5324).
 */
public class ZamorakInstance extends BossInstance {

    public ZamorakInstance(Player owner, InstanceSettings settings) {
        super(owner, settings);
    }

    @Override
    public int[] getMapPos() {
        return new int[] { 365, 665 };
    }

    @Override
    public int[] getMapSize() {
        return new int[] { 1, 1 };
    }

    @Override
    public void enterInstance(Player player, boolean login) {
        if (!login)
            player.getPackets().sendGameMessage("You teleport into K'ril Tsutsaroth's fortress.", true);
        super.enterInstance(player, login);
    }

    @Override
    public void loadMapInstance() {
        World.spawnNPC(6203, getTile(2926, 5324, 0), -1, true, false).setBossInstance(this);
    }
}
