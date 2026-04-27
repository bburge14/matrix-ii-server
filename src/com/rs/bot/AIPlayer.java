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
        // Do nothing. Real players need this to tell their client about the map.
        // Bots have no client. Region tracking happens via setPlace() in BotManager.
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
        // Phase 2a: tick the brain only. Do NOT call super.processEntity() -
        // the parent method touches packet/dialogue/prayer systems that NPE for headless bots.
        // Movement application happens in processEntityUpdate (next loop) -> processMovement().
        if (brain != null) {
            try {
                brain.tick(this);
            } catch (Throwable t) {
                System.err.println("[AIPlayer] brain tick error for " + getDisplayName() + ": " + t);
                t.printStackTrace();
            }
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
                null, null
            );
            // After init(): re-randomize appearance only if the stored data is broken,
            // otherwise leave the persisted Appearence as-is so the bot keeps its identity.
            if (getAppearence().getAppeareanceData() == null) {
                getAppearence().resetAppearence();
                com.rs.game.player.content.PlayerLook.randomizeLook(getAppearence());
                getAppearence().generateAppearenceData();

            // Assign random combat mode
            if (botCombatMode == -1) {
            // Randomize weapon sheathe (50/50 chance)
            weaponSheathe = Utils.random(5) == 0;

                botCombatMode = Utils.random(4);  // 0-3 random mode
            }
            getCombatDefinitions().setCombatMode(botCombatMode);

            if (!weaponSheathe) getCombatDefinitions().switchSheathe();
            
            // Apply weapon sheathe preference
            
            // Apply weapon sheathe preference
            }
        } catch (Throwable t) {
            System.err.println("[AIPlayer] hydrate failed: " + t);
            t.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "[AI]" + getDisplayName();
    }
}
