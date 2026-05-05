package com.rs.bot;
import com.rs.utils.Utils;

import com.rs.game.player.Player;

/**
 * Player subclass that flags an entity as AI-driven.
 * Tick processing and packet sends are bypassed via WorldThread skips
 * and the no-op processEntity() override.
 */
public class AIPlayer extends Player {


    /** Bot identity: "melee", "ranged", "magic", "tank", "pure", "main", "hybrid", "skiller", "f2p", "maxed". */
    private String archetype = "random";

    /** Long-term north star identity that biases goal selection. Set once at bot creation. */
    private com.rs.bot.ai.LifetimeIdentity lifetimeIdentity;
    public com.rs.bot.ai.LifetimeIdentity getLifetimeIdentity() { return lifetimeIdentity; }
    public void setLifetimeIdentity(com.rs.bot.ai.LifetimeIdentity id) { this.lifetimeIdentity = id; }

    /** Combat mode: 0=Manual, 1=Revolution, 2=Momentum, 3=Legacy */
    private int botCombatMode = -1;  // -1 means random assignment
    
    /** Whether this bot spawns with weapons sheathed (true) or unsheathed (false). */
    private boolean weaponSheathe = true;  // default sheathed

    /** Bot behavioral AI brain */
    private transient BotBrain brain;


    public BotBrain getBrain() { return brain; }
    public void setBrain(BotBrain brain) { this.brain = brain; }

    public String getArchetype() { return archetype; }
    public void setArchetype(String a) { this.archetype = a; }

    public int getBotCombatMode() { return botCombatMode; }
    public void setBotCombatMode(int mode) { this.botCombatMode = mode; }
    
    public boolean getWeaponSheathe() { return weaponSheathe; }
    public void setWeaponSheathe(boolean sheathe) { this.weaponSheathe = sheathe; }

    private static final long serialVersionUID = 1L;

    public AIPlayer() {
        super();
    }

    @Override
    public void start() {
        // Skip Player.start entirely - it calls Logger.globalLog with session.getIP(),
        // loadMapRegions (which sends packets), and run() (which sends interfaces).
        // For bots we just mark started; the world loop will broadcast their position
        // to nearby real players via THEIR packet streams.
        setStarted(true);
    }

    @Override
    public void loadMapRegions() {
        // Mirror Entity.loadMapRegions WITHOUT the Player override that
        // sends scene packets to a non-existent client. We DO need to
        // populate mapRegionsIds and force-load the surrounding regions
        // so RouteFinder has clipping data and EnvironmentScanner finds
        // NPCs/objects. Without this, every walkTo after a region change
        // hit "blocked - region not loaded" and the bot froze on the
        // landing tile until a real player happened to load the area.
        try {
            int chunkX = getChunkX();
            int chunkY = getChunkY();
            int sceneChunksRadio = com.rs.Settings.MAP_SIZES[getMapSize()] / 16;
            int sceneBaseChunkX = Math.max(0, chunkX - sceneChunksRadio);
            int sceneBaseChunkY = Math.max(0, chunkY - sceneChunksRadio);
            int fromRegionX = sceneBaseChunkX / 8;
            int fromRegionY = sceneBaseChunkY / 8;
            int toRegionX = (chunkX + sceneChunksRadio) / 8;
            int toRegionY = (chunkY + sceneChunksRadio) / 8;
            getMapRegionsIds().clear();
            for (int regionX = fromRegionX; regionX <= toRegionX; regionX++) {
                for (int regionY = fromRegionY; regionY <= toRegionY; regionY++) {
                    int regionId = com.rs.game.map.MapUtils.encode(
                        com.rs.game.map.MapUtils.Structure.REGION,
                        regionX, regionY);
                    com.rs.game.World.getRegion(regionId, true);
                    getMapRegionsIds().add(regionId);
                }
            }
        } catch (Throwable t) {
            System.err.println("[AIPlayer] loadMapRegions failed for "
                + getDisplayName() + ": " + t);
        }
    }

    @Override
    public boolean isRunning() {
        // Bots have no socket, but for visibility purposes they ARE active in the world.
        // LocalPlayerUpdate.needsAdd() filters out !isRunning, so we have to return true.
        return true;
    }

    @Override
    public boolean clientHasLoadedMapRegion() {
        // Bots have no client, but LocalPlayerUpdate gates visibility on this.
        return true;
    }

    @Override
    public void setRunEnergy(int energy) {
        // Bots don't have run energy UI - skip the encoder call that NPEs.
        // We still update the field so other code reading it doesn't break.
        try {
            java.lang.reflect.Field f = com.rs.game.player.Player.class.getDeclaredField("runEnergy");
            f.setAccessible(true);
            f.setInt(this, energy);
        } catch (Throwable t) {
            // field name might differ; safe to ignore for bots
        }
    }

    @Override
    public boolean isHeadless() {
        return true;
    }

        @Override
    public void processEntity() {
        // Tick the brain to make decisions, then process the action manager
        // so skilling/combat actions the brain has set actually run (gain XP,
        // drop items, etc.). We skip the full super.processEntity() because
        // it touches subsystems with side effects we don't want for headless
        // bots (logic packets, dialogue, music). The encoder is wired up
        // in hydrate() so anything that eventually calls getPackets() inside
        // an action just discards bytes harmlessly via the MockChannel.
        if (brain != null) {
            try {
                brain.tick(this);
            } catch (Throwable t) {
                System.err.println("[AIPlayer] brain tick error for " + getDisplayName() + ": " + t);
                t.printStackTrace();
            }
        }
        try {
            if (getActionManager() != null) getActionManager().process();
        } catch (Throwable t) {
            System.err.println("[AIPlayer] action process error for " + getDisplayName() + ": " + t);
            t.printStackTrace();
        }
        try {
            if (getTimersManager() != null) getTimersManager().process();
        } catch (Throwable t) {
            // timers are best-effort - don't break the tick over them
        }
    }

    @Override
    public void processEntityUpdate() {
        // Diagnostic: prove processMovement actually runs on bots and observe
        // whether nextWorldTile / walkSteps are applied. Sampled at ~1% so it
        // doesn't flood the log.
        boolean log = com.rs.utils.Utils.random(100) < 1;
        int beforeX = getX(), beforeY = getY();
        com.rs.game.WorldTile pendingTele = getNextWorldTile();
        int pendingSteps = getWalkSteps() == null ? -1 : getWalkSteps().size();
        if (log) {
            System.out.println("[AI-MOVE-PRE] " + getDisplayName()
                + " pos=" + beforeX + "," + beforeY
                + " nextTile=" + (pendingTele == null ? "null" : (pendingTele.getX() + "," + pendingTele.getY()))
                + " queuedSteps=" + pendingSteps
                + " started=" + hasStarted() + " finished=" + hasFinished());
        }
        try {
            super.processEntityUpdate();
        } catch (Throwable t) {
            System.err.println("[AI-MOVE-ERR] " + getDisplayName() + ": " + t);
            t.printStackTrace();
        }
        if (log) {
            System.out.println("[AI-MOVE-POST] " + getDisplayName()
                + " pos=" + getX() + "," + getY()
                + " (deltaX=" + (getX() - beforeX) + " deltaY=" + (getY() - beforeY) + ")");
        }
    }

    @Override
    public void finish() {
        // Bot-safe shutdown - skip real-player cleanup that would NPE without a session.
        // Mirrors realFinish()'s essential bits: mark finished, broadcast removal via
        // updateEntityRegion, remove from World player list. Skips: session.close(),
        // PlayerHandlerThread.addLogout, Highscores, GE unlink, friend/clan detach,
        // controller/cutscene logout - none apply to a bot.
        if (hasFinished()) return;
        try {
            // Clean up the brain so it stops trying to walk a finished bot
            setBrain(null);
            setFinished(true);
            // Broadcast region removal so nearby clients drop the bot from their local view
            com.rs.game.World.updateEntityRegion(this);
            com.rs.game.World.removePlayer(this);
        } catch (Throwable t) {
            System.err.println("[AIPlayer] finish() error for " + getDisplayName() + ": " + t);
            t.printStackTrace();
        }
    }

    /**
     * Phase 3a: rebuild transient state after deserializing from disk.
     * Mirrors what Player.init() does for managers, but skips network/session
     * stuff and skips World.addPlayer (caller does that explicitly).
     *
     * Designed to be called on a freshly-loaded AIPlayer that has its persisted
     * fields (Appearence, Inventory, Skills, position) intact but all transient
     * managers are null.
     */
    public void hydrate() {
        hydrate(null);
    }

    public void hydrate(String overrideName) {
        try {
            // Re-attach session
            com.rs.bot.NullSession session = new com.rs.bot.NullSession();
            // displayName is transient - it does NOT survive Java serialization.
            // BotPool tracks the bot's name in its offline list; pass it in here so we can restore identity.
            String name = overrideName != null && !overrideName.isEmpty() ? overrideName : getDisplayName();
            if (name == null || name.isEmpty()) name = "AI" + System.nanoTime();
            init(
                session,
                false,
                name, name,
                "00:00:00:00:00:00",
                "ai@local",
                0, 0,
                false, false, false, false, false, false,
                0L, 0, 765, 503,
                null,
                // Real IsaacKeyPair instead of null - packet construction
                // (OutputStream.writePacket) dereferences this to encrypt
                // opcodes, so passing null NPE'd every time the engine
                // tried to send any packet to a bot. The keys are fixed
                // dummy seeds; the encrypted bytes go straight to the
                // MockChannel and get discarded.
                new com.rs.utils.IsaacKeyPair(new int[] { 0, 0, 0, 0 })
            );
            // Wire up the world packet encoder. session.write() is a no-op for
            // bots (MockChannel.isConnected() == false), so packets construct
            // and discard harmlessly. This makes player.getPackets() non-null
            // so existing skilling/combat/inventory code doesn't NPE on bots.
            session.setEncoder(2, this);
            System.out.println("[BOT-SPAWN] " + name
                + " at " + getX() + "," + getY() + ",plane=" + getPlane()
                + " regionId=" + getRegionId()
                + " index=" + getIndex());
            // After init(): the cached appearance bytes are transient so they're
            // null after deserialization - regenerate. Each step is wrapped in
            // its own try/catch so one failure (e.g. setCombatMode looking up
            // a HUD packet that NPEs) doesn't leave the bot without an
            // appearance, which would make it invisible to nearby clients.
            if (getAppearence().getAppeareanceData() == null) {
                try {
                    getAppearence().resetAppearence();
                    com.rs.game.player.content.PlayerLook.randomizeLook(getAppearence());
                    getAppearence().generateAppearenceData();
                } catch (Throwable t) {
                    System.out.println("[AIPlayer] " + name + " appearance regen failed: " + t);
                    t.printStackTrace(System.out);
                }

                if (botCombatMode == -1) {
                    weaponSheathe = Utils.random(5) == 0;
                    botCombatMode = Utils.random(4);
                }
                try {
                    getCombatDefinitions().setCombatMode(botCombatMode);
                } catch (Throwable t) {
                    System.out.println("[AIPlayer] " + name + " setCombatMode failed: " + t);
                    t.printStackTrace(System.out);
                }
                try {
                    if (!weaponSheathe) getCombatDefinitions().switchSheathe();
                } catch (Throwable t) {
                    System.out.println("[AIPlayer] " + name + " switchSheathe failed: " + t);
                    t.printStackTrace(System.out);
                }
            }
        } catch (Throwable t) {
            System.out.println("[AIPlayer] hydrate failed: " + t);
            t.printStackTrace(System.out);
            System.err.println("[AIPlayer] hydrate failed: " + t);
            t.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "[AI]" + getDisplayName();
    }
}
