package com.rs.bot;

import com.rs.bot.ai.EnvironmentScanner;
import com.rs.bot.ai.TrainingMethods;
import com.rs.game.WorldObject;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

import java.util.Arrays;
import java.util.List;

/**
 * Iterates every TrainingMethod and verifies its location actually has
 * the expected resource (tree/rock/fish/npc/range/furnace/altar) within
 * scan range. Writes results to AuditLog. Runs both on demand
 * (::auditmethods) and on a schedule at server startup.
 */
public final class BotAuditor {

    private BotAuditor() {}

    /**
     * Run a single audit pass. Logs each broken method to AuditLog and
     * a summary at the end.
     */
    public static void runAudit() {
        List<TrainingMethods.Method> all = TrainingMethods.getAll();
        int total = 0, broken = 0;
        AuditLog.log("starting audit pass: " + all.size() + " methods");
        for (TrainingMethods.Method m : all) {
            if (m.location == null) continue;
            total++;
            boolean ok = false;
            String reason = "";
            WorldTile from = m.location;
            int radius = 24;
            try {
            // Force-load the method's region (and its 8 neighbours) before
            // scanning. NPCs from spawn files are only realised into
            // World.getNPCs() when their region loads via a player tick.
            // The audit runs from coords with no nearby player, so without
            // this preload every NPC check returns "no NPC [...]" and every
            // object scan returns null - all 85 of the FAIL entries from
            // the prior run were from this, not from bad coords.
            try {
                int rx = (from.getRegionId() >> 8) & 0xff;
                int ry = from.getRegionId() & 0xff;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nrx = rx + dx;
                        int nry = ry + dy;
                        if (nrx < 0 || nry < 0) continue;
                        com.rs.game.World.getRegion((nrx << 8) | nry, true);
                    }
                }
            } catch (Throwable ignored) {}
            switch (m.kind) {
                case WOODCUTTING: {
                    EnvironmentScanner.TreeMatch tm =
                        EnvironmentScanner.findNearestTree(from, radius, m.treeDef);
                    ok = tm != null;
                    if (!ok) reason = "no " + m.treeDef + " in " + radius;
                    break;
                }
                case MINING: {
                    EnvironmentScanner.RockMatch rm =
                        EnvironmentScanner.findNearestRock(from, radius, m.rockDef);
                    ok = rm != null;
                    if (!ok) reason = "no " + m.rockDef + " in " + radius;
                    break;
                }
                case FISHING: {
                    EnvironmentScanner.FishMatch fm =
                        EnvironmentScanner.findNearestFishingSpot(from, radius, m.fishDef);
                    ok = fm != null;
                    if (!ok) reason = "no " + m.fishDef + " in " + radius;
                    break;
                }
                case COMBAT:
                case THIEVING: {
                    if (m.npcIds == null || m.npcIds.length == 0) {
                        ok = false;
                        reason = "no npcIds set";
                        break;
                    }
                    NPC n = EnvironmentScanner.findNearestNPC(from, radius, m.npcIds);
                    ok = n != null;
                    if (!ok) reason = "no NPC " + Arrays.toString(m.npcIds) + " in " + radius;
                    break;
                }
                case COOKING: {
                    WorldObject o = EnvironmentScanner.findNearestObjectByName(
                        from, radius, "range", "stove", "fire", "firepit");
                    ok = o != null;
                    if (!ok) reason = "no range/stove/fire in " + radius;
                    break;
                }
                case SMELTING: {
                    WorldObject o = EnvironmentScanner.findNearestObjectByName(from, radius, "furnace");
                    ok = o != null;
                    if (!ok) reason = "no furnace in " + radius;
                    break;
                }
                case PRAYER: {
                    WorldObject o = EnvironmentScanner.findNearestObjectByName(from, radius, "altar");
                    ok = o != null;
                    if (!ok) reason = "no altar in " + radius;
                    break;
                }
                default:
                    ok = true; // FIREMAKING / CRAFTING don't need a world object
            }
            if (!ok) {
                broken++;
                AuditLog.log("FAIL: " + m.description + " @ " + from.getX() + "," + from.getY() + " -> " + reason);
            }
            } catch (Throwable methodEx) {
                broken++;
                AuditLog.log("ERROR scanning " + m.description + " @ " + from.getX() + "," + from.getY() + " -> " + methodEx);
            }
        }
        AuditLog.log("audit complete: " + total + " methods scanned, " + broken + " broken");
    }

    /**
     * Snapshot every online bot's current state to audit.log. Runs in
     * concert with the method-coord audit so users tailing audit.log get
     * a continuous picture of what bots are doing right now.
     */
    public static void dumpOnlineBots() {
        int totalLegend = 0, totalCitizen = 0, stuck = 0, working = 0, idle = 0;
        AuditLog.log("--- bot state snapshot ---");
        try {
            for (com.rs.game.player.Player p : com.rs.game.World.getPlayers()) {
                if (p == null || !(p instanceof AIPlayer)) continue;
                AIPlayer bot = (AIPlayer) p;
                BotBrain brain = bot.getBrain();
                if (brain == null) continue;

                // Citizens (CitizenBrain) and Legends (BotBrain) split here -
                // they share the AIPlayer infrastructure but have very
                // different state to dump.
                if (brain instanceof com.rs.bot.ambient.CitizenBrain) {
                    com.rs.bot.ambient.CitizenBrain cb = (com.rs.bot.ambient.CitizenBrain) brain;
                    totalCitizen++;
                    com.rs.bot.ai.TrainingMethods.Method m = cb.getCurrentMethod();
                    String mDesc = m == null ? "none"
                        : m.description + " @" + m.location.getX() + "," + m.location.getY();
                    AuditLog.log("citizen " + bot.getDisplayName()
                        + " cb=" + bot.getSkills().getCombatLevel()
                        + " @" + bot.getX() + "," + bot.getY() + "," + bot.getPlane()
                        + " arch=" + (cb.getArchetype() == null ? "?" : cb.getArchetype().name())
                        + " state=" + cb.getState()
                        + " method=" + mDesc);
                    continue;
                }

                // Legend bot dump - the original behaviour, unchanged.
                totalLegend++;
                String diag = brain.getLastDiagnostic() == null ? "" : brain.getLastDiagnostic();
                String method = brain.getLastMethod() == null ? "none" : brain.getLastMethod().description;
                com.rs.bot.ai.Goal g = brain.getCurrentGoal();
                String goal = g == null ? "none" : g.getDescription();
                boolean isWorking = false;
                try { isWorking = bot.getActionManager() != null && bot.getActionManager().hasSkillWorking(); }
                catch (Throwable ignored) {}
                if (isWorking) working++;
                if (diag.contains("stuck") || diag.contains("no ") || diag.contains("broke")) stuck++;
                if (method.equals("none") || diag.isEmpty()) idle++;
                AuditLog.log("legend " + bot.getDisplayName()
                    + " cb=" + bot.getSkills().getCombatLevel()
                    + " @" + bot.getX() + "," + bot.getY() + "," + bot.getPlane()
                    + " goal=" + goal
                    + " method=" + method
                    + " diag=" + diag
                    + " working=" + isWorking);
            }
        } catch (Throwable t) {
            AuditLog.log("dumpOnlineBots threw: " + t);
        }
        AuditLog.log("--- end snapshot: legends=" + totalLegend + " (working=" + working
            + " stuck=" + stuck + " idle=" + idle + "), citizens=" + totalCitizen + " ---");
    }

    /**
     * Schedule periodic audits. Method-coord verification + per-bot state
     * dump together. First pass at +30s, then every ~60s so users tailing
     * the log see fresh data continuously while debugging.
     */
    public static void scheduleAutoAudit() {
        WorldTasksManager.schedule(new WorldTask() {
            int passCount = 0;
            @Override
            public void run() {
                try {
                    // Method-coord audit only every 10th pass (heavy-ish, 10min);
                    // bot state + success tally every pass (light, useful for live debugging).
                    if (passCount % 10 == 0) runAudit();
                    dumpOnlineBots();
                    SuccessTracker.dump();
                    passCount++;
                } catch (Throwable t) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    t.printStackTrace(new java.io.PrintWriter(sw));
                    AuditLog.log("auto-audit threw: " + t + "\n" + sw.toString());
                }
            }
        }, 50, 100); // 50 ticks (~30s) initial, then every 100 ticks (~60s)
        AuditLog.log("auto-audit scheduled (initial 30s, then every 60s)");
    }
}
