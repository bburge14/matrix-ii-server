package com.rs.game.map.bossInstance.impl;

import com.rs.game.World;
import com.rs.game.map.bossInstance.BossInstance;
import com.rs.game.map.bossInstance.InstanceSettings;
import com.rs.game.player.Player;

/**
 * GWD Bandos throne room instance. Boss NPC 6260 spawns at the
 * canonical lair tile (2870, 5369). Minions (Sergeant Steelwill /
 * Strongstack / Grimspike) aren't wired here yet - they'd need NPC
 * IDs the server has spawned. Boss-only solo is the starting point.
 */
public class BandosInstance extends BossInstance {

    public BandosInstance(Player owner, InstanceSettings settings) {
        super(owner, settings);
    }

    @Override
    public int[] getMapPos() {
        // Chunk SW corner that contains the spawn (2870, 5369): chunk (358, 671)
        return new int[] { 358, 671 };
    }

    @Override
    public int[] getMapSize() {
        // 1 region (64x64 tiles) - throne room fits comfortably
        return new int[] { 1, 1 };
    }

    @Override
    public void enterInstance(Player player, boolean login) {
        if (!login)
            player.getPackets().sendGameMessage("You teleport into General Graardor's throne room.", true);
        super.enterInstance(player, login);
    }

    @Override
    public void loadMapInstance() {
        World.spawnNPC(6260, getTile(2870, 5369, 0), -1, true, false).setBossInstance(this);
    }
}
