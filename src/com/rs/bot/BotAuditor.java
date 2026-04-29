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
        }
        AuditLog.log("audit complete: " + total + " methods scanned, " + broken + " broken");
    }

    /**
     * Snapshot every online bot's current state to audit.log. Runs in
     * concert with the method-coord audit so users tailing audit.log get
     * a continuous picture of what bots are doing right now.
     */
    public static void dumpOnlineBots() {
        int total = 0, stuck = 0, working = 0, idle = 0;
        AuditLog.log("--- bot state snapshot ---");
        try {
            for (com.rs.game.player.Player p : com.rs.game.World.getPlayers()) {
                if (p == null || !(p instanceof AIPlayer)) continue;
                AIPlayer bot = (AIPlayer) p;
                BotBrain brain = bot.getBrain();
                if (brain == null) continue;
                total++;
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
                AuditLog.log("bot " + bot.getDisplayName()
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
        AuditLog.log("--- end snapshot: " + total + " bots, " + working + " working, " + stuck + " stuck, " + idle + " idle ---");
    }

    /**
     * Schedule periodic audits. Method-coord verification + per-bot state
     * dump together. First pass at +60s, then every ~3 minutes so users
     * tailing the log see fresh data continuously.
     */
    public static void scheduleAutoAudit() {
        WorldTasksManager.schedule(new WorldTask() {
            int passCount = 0;
            @Override
            public void run() {
                try {
                    // Method-coord audit only every 5th pass (heavy-ish);
                    // bot state every pass (light, more useful when bots spawn).
                    if (passCount % 5 == 0) runAudit();
                    dumpOnlineBots();
                    passCount++;
                } catch (Throwable t) {
                    AuditLog.log("auto-audit threw: " + t);
                }
            }
        }, 100, 300); // 100 ticks (~60s) initial, then every 300 ticks (~3 min)
        AuditLog.log("auto-audit scheduled (initial 60s, then every 3min)");
    }
}
