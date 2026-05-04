package com.rs.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class AdminHttpServer {

    private AdminHttpServer() {}

    private static final int PORT = 8090;
    private static final String TOKEN_FILE = "/home/brad/matrix/admin_token.txt";
    private static final String LOG_FILE = "/home/brad/matrix/Server/logs/game.log";
    private static final String BACKUPS_REGULAR = "/home/brad/backups/regular";
    private static final String BACKUPS_DAILY = "/home/brad/backups/daily";
    private static final String BACKUP_SCRIPT = "/home/brad/matrix/backup-scheduler.sh";

    private static HttpServer server;
    private static String token;
    private static final long startedAt = System.currentTimeMillis();

    public static synchronized void start() {
        if (server != null) return;
        try {
            // Reuse existing token across restarts. Generate only on first launch.
            token = readTokenFile();
            if (token == null || token.isEmpty()) {
                token = generateToken();
                writeTokenFile(token);
                System.out.println("[AdminHttpServer] Generated NEW token (no existing file)");
            } else {
                System.out.println("[AdminHttpServer] Reusing token from " + TOKEN_FILE);
            }

            server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
            server.setExecutor(Executors.newFixedThreadPool(4));

            // Read-only endpoints
            server.createContext("/admin/ping",      auth(new PingHandler()));
            server.createContext("/admin/stats",     auth(new StatsHandler()));
            server.createContext("/admin/bots",      auth(new BotsHandler()));
            server.createContext("/admin/players",   auth(new PlayersHandler()));
            server.createContext("/admin/snapshots", auth(new SnapshotsHandler()));
            server.createContext("/admin/log/tail",  auth(new LogTailHandler()));

            // Action endpoints (POST)
            server.createContext("/admin/bots/spawn",      auth(postOnly(new BotSpawnHandler())));
            server.createContext("/admin/bots/despawn",    auth(postOnly(new BotDespawnHandler())));
            server.createContext("/admin/bots/generate",   auth(postOnly(new BotGenerateHandler())));
            server.createContext("/admin/bots/delete",     auth(postOnly(new BotDeleteHandler())));
            server.createContext("/admin/bots/inspect",    auth(new BotInspectHandler()));
            server.createContext("/admin/bots/status",     auth(new BotStatusHandler()));
            server.createContext("/admin/bots/diagnose",   auth(new BotDiagnoseHandler()));
            server.createContext("/admin/bots/scan",       auth(new BotScanHandler()));
            server.createContext("/admin/bots/force",      auth(postOnly(new BotForceHandler())));
            server.createContext("/admin/world/scan",      auth(new WorldScanHandler()));
            server.createContext("/admin/items/find",       auth(new ItemFindHandler()));
            server.createContext("/admin/items/scan",       auth(new ItemScanHandler()));
            server.createContext("/admin/players/inspect", auth(new PlayerInspectHandler()));
            server.createContext("/admin/players/heal",    auth(postOnly(new PlayerHealHandler())));
            server.createContext("/admin/players/teleport",auth(postOnly(new PlayerTeleportHandler())));
            server.createContext("/admin/players/give",    auth(postOnly(new PlayerGiveHandler())));
            server.createContext("/admin/players/rights",       auth(postOnly(new PlayerRightsHandler())));
            server.createContext("/admin/players/flags",        auth(postOnly(new PlayerFlagsHandler())));
            server.createContext("/admin/players/kick",         auth(postOnly(new PlayerKickHandler())));
            server.createContext("/admin/players/mute",         auth(postOnly(new PlayerMuteHandler())));
            server.createContext("/admin/players/maxstats",     auth(postOnly(new PlayerMaxStatsHandler())));
            server.createContext("/admin/players/resetstats",   auth(postOnly(new PlayerResetStatsHandler())));
            server.createContext("/admin/players/setstat",      auth(postOnly(new PlayerSetStatHandler())));
            server.createContext("/admin/players/teleporthere", auth(postOnly(new PlayerTeleportHereHandler())));
            server.createContext("/admin/server/save",     auth(postOnly(new ServerSaveHandler())));
            server.createContext("/admin/server/restart",  auth(postOnly(new ServerRestartHandler())));
            server.createContext("/admin/server/broadcast",auth(postOnly(new ServerBroadcastHandler())));
            server.createContext("/admin/server/reload",   auth(postOnly(new ServerReloadHandler())));
            server.createContext("/admin/snapshots/take",  auth(postOnly(new SnapshotsTakeHandler())));

            // Citizen (AmbientBot/FSM) management
            server.createContext("/admin/citizens",         auth(new CitizensListHandler()));
            server.createContext("/admin/citizens/spawn",   auth(postOnly(new CitizensSpawnHandler())));
            server.createContext("/admin/citizens/clear",   auth(postOnly(new CitizensClearHandler())));
            // Citizen population budget (persistent config)
            server.createContext("/admin/citizens/budget",       auth(new CitizensBudgetHandler()));
            server.createContext("/admin/citizens/budget/apply", auth(postOnly(new CitizensBudgetApplyHandler())));
            server.createContext("/admin/citizens/archetypes",   auth(new CitizensArchetypesHandler()));

            // Bulk GE price update - lets the audit script push live RS3
            // prices into the server's GrandExchange.PRICES map. Body:
            //   {"prices":{"4151":240000,"11696":2500000,...}}
            // Updates are applied via setPrice() and persisted via
            // savePrices() so they survive a restart.
            server.createContext("/admin/ge/prices/bulk", auth(postOnly(new GePricesBulkHandler())));
            // GET /admin/ge/prices?ids=1,2,3 - returns current prices for the
            // requested item ids (or every catalog item id if 'ids' is omitted
            // and 'catalog=1' is set). Reads through GrandExchange.getPrice()
            // which falls back to ItemDefinitions value when no price is set.
            server.createContext("/admin/ge/prices", auth(new GePricesGetHandler()));

            // World tick profiler (Phase 1.C step 1)
            server.createContext("/admin/profiler/start",   auth(postOnly(new ProfilerStartHandler())));
            server.createContext("/admin/profiler/stop",    auth(postOnly(new ProfilerStopHandler())));
            server.createContext("/admin/profiler/dump",    auth(postOnly(new ProfilerDumpHandler())));

            // Cache status - which store loaded, which path
            server.createContext("/admin/cache/status",     auth(new CacheStatusHandler()));

            server.start();
            System.out.println("[AdminHttpServer] Listening on :" + PORT + " - token written to " + TOKEN_FILE);
        } catch (Throwable t) {
            System.err.println("[AdminHttpServer] start failed: " + t);
            t.printStackTrace();
        }
    }

    public static synchronized void stop() {
        if (server == null) return;
        try { server.stop(0); System.out.println("[AdminHttpServer] Stopped"); }
        catch (Throwable ignored) {}
        server = null;
    }

    private static String generateToken() {
        SecureRandom rng = new SecureRandom();
        byte[] bytes = new byte[24];
        rng.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    private static void writeTokenFile(String token) {
        try (PrintWriter w = new PrintWriter(new File(TOKEN_FILE), "UTF-8")) {
            w.println(token);
        } catch (Throwable t) {
            System.err.println("[AdminHttpServer] Could not write token file: " + t);
        }
    }

    private static String readTokenFile() {
        File f = new File(TOKEN_FILE);
        if (!f.isFile()) return null;
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            String s = new String(bytes, StandardCharsets.UTF_8).trim();
            return s.isEmpty() ? null : s;
        } catch (Throwable t) {
            System.err.println("[AdminHttpServer] Could not read token file: " + t);
            return null;
        }
    }

    private static HttpHandler auth(final HttpHandler delegate) {
        return new HttpHandler() {
            @Override public void handle(HttpExchange ex) throws IOException {
                String hdr = ex.getRequestHeaders().getFirst("Authorization");
                if (hdr == null || !hdr.startsWith("Bearer ") || !hdr.substring(7).equals(token)) {
                    sendText(ex, 401, "Unauthorized");
                    return;
                }
                ex.getResponseHeaders().add("Content-Type", "application/json");
                try {
                    delegate.handle(ex);
                } catch (Throwable t) {
                    t.printStackTrace();
                    String err = "{\"ok\":false,\"error\":\"" + jsonEscape(String.valueOf(t.getMessage())) + "\"}";
                    sendText(ex, 500, err);
                }
            }
        };
    }

    private static HttpHandler postOnly(final HttpHandler delegate) {
        return new HttpHandler() {
            @Override public void handle(HttpExchange ex) throws IOException {
                if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                    sendText(ex, 405, "{\"ok\":false,\"error\":\"POST required\"}");
                    return;
                }
                delegate.handle(ex);
            }
        };
    }

    // ===== Read-only handlers =====

    private static class PingHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            sendText(ex, 200, "{\"ok\":true,\"server\":\"matrix\",\"time\":" + System.currentTimeMillis() + "}");
        }
    }

    private static class StatsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Runtime rt = Runtime.getRuntime();
            long uptimeMs = System.currentTimeMillis() - startedAt;
            int onlinePlayers = 0, onlineBots = 0;
            try {
                for (com.rs.game.player.Player p : com.rs.game.World.getPlayers()) {
                    if (p == null) continue;
                    if (p.isHeadless()) onlineBots++; else onlinePlayers++;
                }
            } catch (Throwable ignored) {}
            int offlineBots = 0;
            try { offlineBots = com.rs.bot.BotPool.getOfflineCount(); } catch (Throwable ignored) {}

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"ok\":true,");
            sb.append("\"uptime_ms\":").append(uptimeMs).append(",");
            sb.append("\"players_online\":").append(onlinePlayers).append(",");
            sb.append("\"bots_online\":").append(onlineBots).append(",");
            sb.append("\"bots_offline\":").append(offlineBots).append(",");
            sb.append("\"mem_used_mb\":").append((rt.totalMemory() - rt.freeMemory()) / 1048576).append(",");
            sb.append("\"mem_total_mb\":").append(rt.totalMemory() / 1048576).append(",");
            sb.append("\"mem_max_mb\":").append(rt.maxMemory() / 1048576);
            sb.append("}");
            sendText(ex, 200, sb.toString());
        }
    }

    private static class BotsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"bots\":[");
            boolean first = true;
            try {
                for (com.rs.game.player.Player p : com.rs.game.World.getPlayers()) {
                    if (p == null || !p.isHeadless()) continue;
                    if (!first) sb.append(",");
                    first = false;
                    String botArch = "";
                    if (p instanceof com.rs.bot.AIPlayer) {
                        try { botArch = ((com.rs.bot.AIPlayer) p).getArchetype(); } catch (Throwable ignored) {}
                    }
                    sb.append("{\"name\":\"").append(jsonEscape(p.getDisplayName()))
                      .append("\",\"online\":true,\"combat\":").append(p.getSkills().getCombatLevel())
                      .append(",\"total\":").append(p.getSkills().getTotalLevel())
                      .append(",\"archetype\":\"").append(jsonEscape(botArch == null ? "" : botArch))
                      .append("\",\"x\":").append(p.getX())
                      .append(",\"y\":").append(p.getY())
                      .append(",\"plane\":").append(p.getPlane()).append("}");
                }
            } catch (Throwable ignored) {}
            try {
                for (String name : com.rs.bot.BotPool.snapshotOfflineNames()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"name\":\"").append(jsonEscape(name)).append("\",\"online\":false}");
                }
            } catch (Throwable ignored) {}
            sb.append("]}");
            sendText(ex, 200, sb.toString());
        }
    }

    private static class PlayersHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"players\":[");
            boolean first = true;
            try {
                for (com.rs.game.player.Player p : com.rs.game.World.getPlayers()) {
                    if (p == null || p.isHeadless()) continue;
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"name\":\"").append(jsonEscape(p.getDisplayName()))
                      .append("\",\"username\":\"").append(jsonEscape(p.getUsername()))
                      .append("\",\"rights\":").append(p.getRights())
                      .append(",\"combat\":").append(p.getSkills().getCombatLevel())
                      .append(",\"total\":").append(p.getSkills().getTotalLevel())
                      .append(",\"donator\":").append(p.isDonator())
                      .append(",\"extreme\":").append(p.isExtremeDonator())
                      .append(",\"supporter\":").append(p.isSupporter())
                      .append(",\"invulnerable\":").append(p.isInvulnerable())
                      .append(",\"master\":").append(p.isMasterLogin())
                      .append(",\"muted\":").append(p.isMuted())
                      .append(",\"x\":").append(p.getX())
                      .append(",\"y\":").append(p.getY())
                      .append(",\"plane\":").append(p.getPlane()).append("}");
                }
            } catch (Throwable ignored) {}
            sb.append("]}");
            sendText(ex, 200, sb.toString());
        }
    }

    private static class SnapshotsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            List<File> all = new ArrayList<File>();
            collect(all, new File(BACKUPS_REGULAR));
            collect(all, new File(BACKUPS_DAILY));
            all.sort(new Comparator<File>() {
                @Override public int compare(File a, File b) { return Long.compare(b.lastModified(), a.lastModified()); }
            });
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"snapshots\":[");
            for (int i = 0; i < all.size(); i++) {
                File f = all.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"").append(jsonEscape(f.getName()))
                  .append("\",\"path\":\"").append(jsonEscape(f.getAbsolutePath()))
                  .append("\",\"size_bytes\":").append(f.length())
                  .append(",\"modified_ms\":").append(f.lastModified()).append("}");
            }
            sb.append("]}");
            sendText(ex, 200, sb.toString());
        }
        private void collect(List<File> out, File dir) {
            if (!dir.isDirectory()) return;
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File f : files) if (f.isFile()) out.add(f);
        }
    }

    private static class LogTailHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            int lines = 200;
            String query = ex.getRequestURI().getQuery();
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && pair.substring(0, eq).equals("lines")) {
                        try { lines = Integer.parseInt(pair.substring(eq + 1)); } catch (Throwable ignored) {}
                    }
                }
            }
            if (lines < 1) lines = 1;
            if (lines > 2000) lines = 2000;

            File f = new File(LOG_FILE);
            if (!f.isFile()) { sendText(ex, 200, "{\"ok\":true,\"lines\":[],\"note\":\"log file not found\"}"); return; }
            List<String> tailLines = readTail(f, lines);
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"lines\":[");
            for (int i = 0; i < tailLines.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(jsonEscape(tailLines.get(i))).append("\"");
            }
            sb.append("]}");
            sendText(ex, 200, sb.toString());
        }
        private List<String> readTail(File f, int maxLines) {
            List<String> lines = new ArrayList<String>();
            try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                long pos = raf.length() - 1;
                StringBuilder line = new StringBuilder();
                int found = 0;
                while (pos >= 0 && found < maxLines + 1) {
                    raf.seek(pos);
                    int ch = raf.read();
                    if (ch == '\n') {
                        if (line.length() > 0) {
                            lines.add(line.reverse().toString());
                            line.setLength(0);
                            found++;
                        }
                    } else if (ch != '\r') {
                        line.append((char) ch);
                    }
                    pos--;
                }
                if (line.length() > 0 && found < maxLines) lines.add(line.reverse().toString());
            } catch (Throwable t) {
                lines.add("<read error: " + t.getMessage() + ">");
            }
            Collections.reverse(lines);
            return lines;
        }
    }

    // ===== Action handlers =====

    private static class BotSpawnHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            int count = parseIntOr(body.get("count"), 0);
            if (name != null && !name.isEmpty()) {
                boolean ok = com.rs.bot.BotPool.spawnByName(name);
                sendText(ex, 200, "{\"ok\":" + ok + ",\"name\":\"" + jsonEscape(name) + "\"}");
            } else if (count > 0) {
                int n = com.rs.bot.BotPool.spawn(count);
                sendText(ex, 200, "{\"ok\":true,\"spawned\":" + n + "}");
            } else {
                sendText(ex, 400, "{\"ok\":false,\"error\":\"need name or count\"}");
            }
        }
    }

    private static class BotDespawnHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            if (name != null && !name.isEmpty()) {
                boolean ok = com.rs.bot.BotPool.despawnByName(name);
                sendText(ex, 200, "{\"ok\":" + ok + ",\"name\":\"" + jsonEscape(name) + "\"}");
            } else {
                int n = com.rs.bot.BotPool.despawnAll();
                sendText(ex, 200, "{\"ok\":true,\"despawned\":" + n + "}");
            }
        }
    }

    private static class BotGenerateHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            int count = parseIntOr(body.get("count"), 10);
            String mode = body.get("mode");
            if (mode == null) mode = "default";
            int level = parseIntOr(body.get("level"), 3);
            String archetype = body.get("archetype");
            if (archetype == null) archetype = "random";
            if (count < 1 || count > 500) { sendText(ex, 400, "{\"ok\":false,\"error\":\"count must be 1-500\"}"); return; }
            int n = com.rs.bot.BotPool.generate(count, mode, level, archetype);
            sendText(ex, 200, "{\"ok\":true,\"generated\":" + n + ",\"mode\":\"" + mode + "\",\"level\":" + level + ",\"archetype\":\"" + archetype + "\"}");
        }
    }

    private static class BotDeleteHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            if (name == null || name.isEmpty()) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name required\"}"); return; }
            boolean ok = com.rs.bot.BotPool.deleteBot(name);
            sendText(ex, 200, "{\"ok\":" + ok + ",\"name\":\"" + jsonEscape(name) + "\"}");
        }
    }

    private static class PlayerHealHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name required\"}"); return; }
            com.rs.game.player.Player p = com.rs.game.World.getPlayerByDisplayName(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            p.heal(p.getMaxHitpoints());
            sendText(ex, 200, "{\"ok\":true,\"name\":\"" + jsonEscape(name) + "\"}");
        }
    }

    private static class PlayerTeleportHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            int x = parseIntOr(body.get("x"), -1);
            int y = parseIntOr(body.get("y"), -1);
            int plane = parseIntOr(body.get("plane"), 0);
            if (name == null || x < 0 || y < 0) { sendText(ex, 400, "{\"ok\":false,\"error\":\"need name, x, y, plane\"}"); return; }
            com.rs.game.player.Player p = com.rs.game.World.getPlayerByDisplayName(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            p.setNextWorldTile(new com.rs.game.WorldTile(x, y, plane));
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static class PlayerGiveHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            int itemId = parseIntOr(body.get("itemId"), -1);
            int amount = parseIntOr(body.get("amount"), 1);
            if (name == null || itemId < 0 || amount < 1) { sendText(ex, 400, "{\"ok\":false,\"error\":\"need name, itemId, amount\"}"); return; }
            com.rs.game.player.Player p = com.rs.game.World.getPlayerByDisplayName(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            p.getInventory().addItem(itemId, amount);
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static class ServerSaveHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            try {
                int bots = com.rs.bot.BotPool.saveOnline();
                com.rs.utils.SerializableFilesManager.flush();
                sendText(ex, 200, "{\"ok\":true,\"bots_saved\":" + bots + "}");
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.getMessage()) + "\"}");
            }
        }
    }

    private static class ServerRestartHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            int delay = parseIntOr(body.get("delay"), 60);
            if (delay < 5) delay = 5;
            boolean ok = com.rs.GameLauncher.initDelayedShutdown(delay);
            sendText(ex, 200, "{\"ok\":" + ok + ",\"delay\":" + delay + "}");
        }
    }

    private static class ServerBroadcastHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String message = body.get("message");
            if (message == null || message.isEmpty()) { sendText(ex, 400, "{\"ok\":false,\"error\":\"message required\"}"); return; }
            com.rs.game.World.sendWorldMessage("<col=ffaa00>[Broadcast] " + message + "</col>", false);
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static class ServerReloadHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            try {
                com.rs.utils.ShopsHandler.forceReload();
                sendText(ex, 200, "{\"ok\":true,\"reloaded\":\"shops\"}");
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.getMessage()) + "\"}");
            }
        }
    }

    private static class SnapshotsTakeHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            try {
                new ProcessBuilder(BACKUP_SCRIPT, "do_backup").redirectErrorStream(true).start();
                sendText(ex, 200, "{\"ok\":true,\"note\":\"snapshot triggered\"}");
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.getMessage()) + "\"}");
            }
        }
    }

    private static class BotInspectHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String query = ex.getRequestURI().getQuery();
            String name = null;
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && pair.substring(0, eq).equals("name")) {
                        name = java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                    }
                }
            }
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name query required\"}"); return; }
            com.rs.game.player.Player p = null;
            for (com.rs.game.player.Player q : com.rs.game.World.getPlayers()) {
                if (q != null && name.equals(q.getDisplayName())) { p = q; break; }
            }
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"not online\"}"); return; }

            String arch = "";
            if (p instanceof com.rs.bot.AIPlayer) {
                try { arch = ((com.rs.bot.AIPlayer) p).getArchetype(); } catch (Throwable ignored) {}
            }

            StringBuilder sb = new StringBuilder("{\"ok\":true,\"name\":\"");
            sb.append(jsonEscape(p.getDisplayName())).append("\",");
            sb.append("\"combat\":").append(p.getSkills().getCombatLevel()).append(",");
            sb.append("\"total_level\":").append(p.getSkills().getTotalLevel()).append(",");
            sb.append("\"archetype\":\"").append(jsonEscape(arch == null ? "" : arch)).append("\",");
            sb.append("\"x\":").append(p.getX()).append(",\"y\":").append(p.getY()).append(",\"plane\":").append(p.getPlane()).append(",");
            sb.append("\"skills\":{");
            String[] names = com.rs.game.player.Skills.SKILL_NAME;
            for (int i = 0; i < names.length && i < 26; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(names[i]).append("\":").append(p.getSkills().getLevel(i));
            }
            sb.append("},\"equipment\":[");
            try {
                String[] slotNames = {"hat","cape","amulet","weapon","chest","shield","slot6","legs","slot8","hands","feet","slot11","ring","arrows","aura","pocket","slot16","wings"};
                boolean firstEq = true;
                for (int s = 0; s < slotNames.length; s++) {
                    com.rs.game.item.Item it = null;
                    try { it = p.getEquipment().getItem(s); } catch (Throwable ignored) {}
                    if (it == null) continue;
                    if (!firstEq) sb.append(",");
                    firstEq = false;
                    String itemName = "unknown";
                    try {
                        com.rs.cache.loaders.ItemDefinitions def = com.rs.cache.loaders.ItemDefinitions.getItemDefinitions(it.getId());
                        if (def != null && def.getName() != null) itemName = def.getName();
                    } catch (Throwable ignored) {}
                    sb.append("{\"slot\":\"").append(slotNames[s]).append("\",\"id\":").append(it.getId())
                      .append(",\"name\":\"").append(jsonEscape(itemName)).append("\"}");
                }
            } catch (Throwable ignored) {}
            sb.append("]}");
            sendText(ex, 200, sb.toString());
        }
    }

    // ===== FIXED BotStatusHandler with goal, personality, emotions =====
    private static class BotStatusHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"bots\":[");
            boolean first = true;
            try {
                for (com.rs.game.player.Player p : com.rs.game.World.getPlayers()) {
                    if (p == null || !p.isHeadless()) continue;
                    if (!first) sb.append(",");
                    first = false;

                    String name = p.getDisplayName();
                    int x = p.getX(), y = p.getY(), plane = p.getPlane();
                    String area = "UNKNOWN";
                    try { area = com.rs.bot.ai.WorldKnowledge.getCurrentArea(x, y); } catch (Throwable ignored) {}
                    String state = "UNKNOWN";
                    String goal = "None";
                    String activity = "";
                    String method = "";
                    String methodKind = "";
                    String diag = "";
                    String archetype = "";
                    int hp = 0, maxHp = 0, cb = 0, totalLvl = 0, freeInv = 0;
                    boolean locked = false, working = false;

                    if (p instanceof com.rs.bot.AIPlayer) {
                        com.rs.bot.AIPlayer bot = (com.rs.bot.AIPlayer) p;
                        try { archetype = bot.getArchetype(); } catch (Throwable ignored) {}
                        try {
                            com.rs.bot.BotBrain brain = bot.getBrain();
                            if (brain != null) {
                                if (brain.getCurrentState() != null) state = brain.getCurrentState().toString();
                                com.rs.bot.ai.Goal g = brain.getCurrentGoal();
                                if (g != null) goal = g.getDescription();
                                activity = brain.getCurrentActivity() == null ? "" : brain.getCurrentActivity();
                                com.rs.bot.ai.TrainingMethods.Method m = brain.getLastMethod();
                                if (m != null) {
                                    method = m.description;
                                    methodKind = String.valueOf(m.kind);
                                }
                                diag = brain.getLastDiagnostic() == null ? "" : brain.getLastDiagnostic();
                            }
                        } catch (Throwable ignored) {}
                        try { hp = bot.getHitpoints(); } catch (Throwable ignored) {}
                        try { maxHp = bot.getMaxHitpoints(); } catch (Throwable ignored) {}
                        try { cb = bot.getSkills().getCombatLevel(); } catch (Throwable ignored) {}
                        try { totalLvl = bot.getSkills().getTotalLevel(); } catch (Throwable ignored) {}
                        try { freeInv = bot.getInventory().getFreeSlots(); } catch (Throwable ignored) {}
                        try { locked = bot.isLocked(); } catch (Throwable ignored) {}
                        try { working = bot.getActionManager() != null && bot.getActionManager().hasSkillWorking(); } catch (Throwable ignored) {}
                    }

                    sb.append("{\"name\":\"").append(jsonEscape(name)).append("\"")
                      .append(",\"x\":").append(x).append(",\"y\":").append(y).append(",\"plane\":").append(plane)
                      .append(",\"area\":\"").append(jsonEscape(area)).append("\"")
                      .append(",\"state\":\"").append(jsonEscape(state)).append("\"")
                      .append(",\"goal\":\"").append(jsonEscape(goal)).append("\"")
                      .append(",\"activity\":\"").append(jsonEscape(activity)).append("\"")
                      .append(",\"method\":\"").append(jsonEscape(method)).append("\"")
                      .append(",\"method_kind\":\"").append(jsonEscape(methodKind)).append("\"")
                      .append(",\"diag\":\"").append(jsonEscape(diag)).append("\"")
                      .append(",\"archetype\":\"").append(jsonEscape(archetype)).append("\"")
                      .append(",\"lifetime\":\"");
                    if (p instanceof com.rs.bot.AIPlayer) {
                        com.rs.bot.ai.LifetimeIdentity id = ((com.rs.bot.AIPlayer) p).getLifetimeIdentity();
                        if (id != null) sb.append(jsonEscape(id.label));
                    }
                    sb.append("\"")
                      .append(",\"hp\":").append(hp).append(",\"max_hp\":").append(maxHp)
                      .append(",\"cb\":").append(cb).append(",\"total_lvl\":").append(totalLvl)
                      .append(",\"free_inv\":").append(freeInv)
                      .append(",\"locked\":").append(locked)
                      .append(",\"working\":").append(working)
                      .append("}");
                }
            } catch (Throwable t) {
                sb = new StringBuilder("{\"ok\":false,\"error\":\"").append(jsonEscape(t.getMessage())).append("\"}");
            }
            sb.append("]}");
            sendText(ex, 200, sb.toString());
        }
    }

    private static class PlayerRightsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            int level = parseIntOr(body.get("level"), -1);
            if (name == null || level < 0 || level > 2) { sendText(ex, 400, "{\"ok\":false,\"error\":\"need name and level 0-2\"}"); return; }
            com.rs.game.player.Player p = findPlayer(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            p.setRights(level);
            try { p.getPackets().sendGameMessage("Your rights level was set to " + level + " by an admin."); } catch (Throwable ignored) {}
            sendText(ex, 200, "{\"ok\":true,\"name\":\"" + jsonEscape(name) + "\",\"level\":" + level + "}");
        }
    }

    private static class PlayerFlagsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name required\"}"); return; }
            com.rs.game.player.Player p = findPlayer(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            String donator = body.get("donator");
            String extreme = body.get("extreme");
            String supporter = body.get("supporter");
            String invul = body.get("invulnerable");
            if (donator != null) p.setDonator("true".equalsIgnoreCase(donator));
            if (extreme != null) p.setExtremeDonator("true".equalsIgnoreCase(extreme));
            if (supporter != null) p.setSupporter("true".equalsIgnoreCase(supporter));
            if (invul != null) {
                boolean on = "true".equalsIgnoreCase(invul);
                p.setInvulnerable(on);
                if (on) {
                    p.setHitpoints(p.getMaxHitpoints());
                    try { p.setRunEnergy(100); } catch (Throwable ignore) {}
                }
            }
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static class PlayerKickHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name required\"}"); return; }
            com.rs.game.player.Player p = findPlayer(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            try { p.disconnect(true, false); }
            catch (Throwable t) { sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.getMessage()) + "\"}"); return; }
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static class PlayerMuteHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            String muted = body.get("muted");
            if (name == null || muted == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name and muted required\"}"); return; }
            com.rs.game.player.Player p = findPlayer(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            p.setMuted("true".equalsIgnoreCase(muted));
            sendText(ex, 200, "{\"ok\":true,\"muted\":" + p.isMuted() + "}");
        }
    }

    private static class PlayerMaxStatsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name required\"}"); return; }
            com.rs.game.player.Player p = findPlayer(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            try {
                for (int i = 0; i < 25; i++) {
                    p.getSkills().setXp(i, com.rs.game.player.Skills.getXPForLevel(99));
                    p.getSkills().set(i, 99);
                }
                p.getSkills().init();
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.getMessage()) + "\"}");
                return;
            }
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static class PlayerResetStatsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name required\"}"); return; }
            com.rs.game.player.Player p = findPlayer(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            try {
                for (int i = 0; i < 25; i++) {
                    p.getSkills().setXp(i, 0.0);
                    p.getSkills().set(i, 1);
                }
                p.getSkills().setXp(3, 1184.0);
                p.getSkills().set(3, 10);
                p.getSkills().setXp(15, 250.0);
                p.getSkills().set(15, 3);
                p.getSkills().init();
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.getMessage()) + "\"}");
                return;
            }
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static class PlayerSetStatHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            String skillName = body.get("skill");
            int level = parseIntOr(body.get("level"), -1);
            if (name == null || skillName == null || level < 1 || level > 99) {
                sendText(ex, 400, "{\"ok\":false,\"error\":\"need name, skill, level 1-99\"}");
                return;
            }
            com.rs.game.player.Player p = findPlayer(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"player offline\"}"); return; }
            int idx = -1;
            String[] names = com.rs.game.player.Skills.SKILL_NAME;
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(skillName)) { idx = i; break; }
            }
            if (idx < 0) { sendText(ex, 400, "{\"ok\":false,\"error\":\"unknown skill\"}"); return; }
            try {
                p.getSkills().setXp(idx, com.rs.game.player.Skills.getXPForLevel(level));
                p.getSkills().set(idx, level);
                p.getSkills().init();
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.getMessage()) + "\"}");
                return;
            }
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static class PlayerTeleportHereHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            String name = body.get("name");
            String target = body.get("target");
            if (name == null || target == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"need name and target\"}"); return; }
            com.rs.game.player.Player p = findPlayer(name);
            com.rs.game.player.Player t = findPlayer(target);
            if (p == null || t == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"one or both offline\"}"); return; }
            p.setNextWorldTile(new com.rs.game.WorldTile(t.getX(), t.getY(), t.getPlane()));
            sendText(ex, 200, "{\"ok\":true}");
        }
    }

    private static com.rs.game.player.Player findPlayer(String name) {
        if (name == null || name.isEmpty()) return null;
        com.rs.game.player.Player p = com.rs.game.World.getPlayerByDisplayName(name);
        if (p != null) return p;
        for (com.rs.game.player.Player q : com.rs.game.World.getPlayers()) {
            if (q != null && (name.equalsIgnoreCase(q.getDisplayName()) || name.equalsIgnoreCase(q.getUsername()))) return q;
        }
        return null;
    }

    private static class PlayerInspectHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String query = ex.getRequestURI().getQuery();
            String name = null;
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && pair.substring(0, eq).equals("name")) {
                        name = java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                    }
                }
            }
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name query required\"}"); return; }
            com.rs.game.player.Player p = findPlayer(name);
            if (p == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"not online\"}"); return; }
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"name\":\"");
            sb.append(jsonEscape(p.getDisplayName())).append("\",");
            sb.append("\"combat\":").append(p.getSkills().getCombatLevel()).append(",");
            sb.append("\"total_level\":").append(p.getSkills().getTotalLevel()).append(",");
            sb.append("\"x\":").append(p.getX()).append(",\"y\":").append(p.getY()).append(",\"plane\":").append(p.getPlane()).append(",");
            sb.append("\"skills\":{");
            String[] names = com.rs.game.player.Skills.SKILL_NAME;
            for (int i = 0; i < names.length && i < 26; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(names[i]).append("\":").append(p.getSkills().getLevel(i));
            }
            sb.append("}}");
            sendText(ex, 200, sb.toString());
        }
    }

    // ===== Helpers =====

    private static class ItemScanHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            // Walk the cache, capture every equippable item with all combat stats, write JSON to disk.
            // Also returns total count + path to the JSON file.
            String outPath = "/home/brad/matrix/items_catalog.json";
            int total = 0, equippable = 0;
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(outPath));
                pw.print("[");
                boolean first = true;
                for (int id = 0; id < 30000; id++) {
                    com.rs.cache.loaders.ItemDefinitions def;
                    try { def = com.rs.cache.loaders.ItemDefinitions.getItemDefinitions(id); }
                    catch (Throwable t) { continue; }
                    if (def == null) continue;
                    String name = null;
                    try { name = def.getName(); } catch (Throwable ignored) {}
                    if (name == null || name.equalsIgnoreCase("null")) continue;
                    total++;
                    boolean wear;
                    try { wear = def.isWearItem(); } catch (Throwable t) { wear = false; }
                    if (!wear) continue;
                    int slot;
                    try { slot = def.equipSlot; } catch (Throwable t) { continue; }
                    if (slot < 0) continue;
                    equippable++;
                    if (!first) pw.print(",");
                    first = false;
                    pw.print("{");
                    pw.print("\"id\":" + id);
                    pw.print(",\"name\":\"" + jsonEscape(name) + "\"");
                    pw.print(",\"slot\":" + slot);
                    pw.print(",\"stab_atk\":" + safe(def, "getStabAttack"));
                    pw.print(",\"slash_atk\":" + safe(def, "getSlashAttack"));
                    pw.print(",\"crush_atk\":" + safe(def, "getCrushAttack"));
                    pw.print(",\"magic_atk\":" + safe(def, "getMagicAttack"));
                    pw.print(",\"range_atk\":" + safe(def, "getRangeAttack"));
                    pw.print(",\"stab_def\":" + safe(def, "getStabDef"));
                    pw.print(",\"slash_def\":" + safe(def, "getSlashDef"));
                    pw.print(",\"crush_def\":" + safe(def, "getCrushDef"));
                    pw.print(",\"magic_def\":" + safe(def, "getMagicDef"));
                    pw.print(",\"range_def\":" + safe(def, "getRangeDef"));
                    pw.print(",\"summ_def\":" + safe(def, "getSummoningDef"));
                    pw.print(",\"str_bonus\":" + safe(def, "getStrengthBonus"));
                    pw.print(",\"ranged_str\":" + safe(def, "getRangedStrBonus"));
                    pw.print(",\"magic_dmg\":" + safe(def, "getMagicDamage"));
                    pw.print(",\"prayer\":" + safe(def, "getPrayerBonus"));
                    pw.print(",\"abs_melee\":" + safe(def, "getAbsorveMeleeBonus"));
                    pw.print(",\"abs_mage\":" + safe(def, "getAbsorveMageBonus"));
                    pw.print(",\"abs_range\":" + safe(def, "getAbsorveRangeBonus"));
                    pw.print(",\"armor\":" + safe(def, "getArmor"));
                    pw.print(",\"atk_speed\":" + safe(def, "getAttackSpeed"));
                    boolean isMeleeWep = false, isRangeWep = false, isMagicWep = false, isShield = false;
                    try { isMeleeWep = def.isMeleeTypeWeapon(); } catch (Throwable ignored) {}
                    try { isRangeWep = def.isRangeTypeWeapon(); } catch (Throwable ignored) {}
                    try { isMagicWep = def.isMagicTypeWeapon(); } catch (Throwable ignored) {}
                    try { isShield   = def.isShield(); }          catch (Throwable ignored) {}
                    pw.print(",\"is_melee_wep\":" + isMeleeWep);
                    pw.print(",\"is_range_wep\":" + isRangeWep);
                    pw.print(",\"is_magic_wep\":" + isMagicWep);
                    pw.print(",\"is_shield\":" + isShield);
                    pw.print("}");
                }
                pw.print("]");
                pw.flush();
                pw.close();
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.getMessage()) + "\"}");
                return;
            }
            sendText(ex, 200, "{\"ok\":true,\"path\":\"" + outPath + "\",\"total_items\":" + total + ",\"equippable\":" + equippable + "}");
        }

        private static int safe(com.rs.cache.loaders.ItemDefinitions def, String methodName) {
            try {
                java.lang.reflect.Method m = def.getClass().getMethod(methodName);
                Object r = m.invoke(def);
                if (r instanceof Integer) return (Integer) r;
                if (r instanceof Number) return ((Number) r).intValue();
            } catch (Throwable ignored) {}
            return 0;
        }
    }

    private static class ItemFindHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String query = ex.getRequestURI().getQuery();
            String search = null;
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && pair.substring(0, eq).equals("name")) {
                        search = java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8").toLowerCase();
                    }
                }
            }
            if (search == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name query required\"}"); return; }
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"matches\":[");
            int found = 0;
            int max = 50;
            for (int id = 0; id < 30000 && found < max; id++) {
                try {
                    com.rs.cache.loaders.ItemDefinitions def = com.rs.cache.loaders.ItemDefinitions.getItemDefinitions(id);
                    if (def == null) continue;
                    String n = def.getName();
                    if (n == null || n.equalsIgnoreCase("null")) continue;
                    if (n.toLowerCase().contains(search)) {
                        if (found > 0) sb.append(",");
                        sb.append("{\"id\":").append(id).append(",\"name\":\"").append(jsonEscape(n)).append("\"}");
                        found++;
                    }
                } catch (Throwable ignored) {}
            }
            sb.append("]}");
            sendText(ex, 200, sb.toString());
        }
    }

    public static void sendText(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /** Tiny flat JSON parser. Handles {"key":"val","k2":N} only. No nesting, no arrays. */
    private static Map<String,String> parseBody(HttpExchange ex) throws IOException {
        Map<String,String> map = new HashMap<String,String>();
        InputStream is = ex.getRequestBody();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
        String s = baos.toString("UTF-8").trim();
        if (s.isEmpty() || s.equals("{}")) return map;
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);

        // Split on commas at top level (no nested handling needed)
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        List<String> parts = new ArrayList<String>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') { inStr = !inStr; cur.append(c); }
            else if (c == ',' && !inStr) { parts.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        if (cur.length() > 0) parts.add(cur.toString());

        for (String part : parts) {
            int colon = -1;
            inStr = false;
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c == '"') inStr = !inStr;
                else if (c == ':' && !inStr) { colon = i; break; }
            }
            if (colon < 0) continue;
            String k = part.substring(0, colon).trim();
            String v = part.substring(colon + 1).trim();
            if (k.startsWith("\"") && k.endsWith("\"")) k = k.substring(1, k.length() - 1);
            if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
            map.put(k, v);
        }
        return map;
    }

    private static int parseIntOr(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (Throwable t) { return def; }
    }

    /** Bot diagnostic dump: goal, method, last diag, scan result, inventory free slots. */
    private static class BotDiagnoseHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String name = queryParam(ex, "name");
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name query required\"}"); return; }
            com.rs.bot.AIPlayer bot = findBot(name);
            if (bot == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"bot not online\"}"); return; }
            com.rs.bot.BotBrain brain = bot.getBrain();
            if (brain == null) { sendText(ex, 500, "{\"ok\":false,\"error\":\"bot has no brain\"}"); return; }
            StringBuilder sb = new StringBuilder("{\"ok\":true");
            sb.append(",\"name\":\"").append(jsonEscape(bot.getDisplayName())).append("\"");
            sb.append(",\"x\":").append(bot.getX()).append(",\"y\":").append(bot.getY()).append(",\"plane\":").append(bot.getPlane());
            sb.append(",\"state\":\"").append(jsonEscape(String.valueOf(brain.getCurrentState()))).append("\"");
            sb.append(",\"activity\":\"").append(jsonEscape(brain.getCurrentActivity())).append("\"");
            com.rs.bot.ai.Goal g = brain.getCurrentGoal();
            sb.append(",\"goal\":\"").append(jsonEscape(g == null ? "null" : g.getDescription())).append("\"");
            com.rs.bot.ai.TrainingMethods.Method m = brain.getLastMethod();
            sb.append(",\"method\":\"").append(jsonEscape(m == null ? "null" : m.description)).append("\"");
            sb.append(",\"method_kind\":\"").append(jsonEscape(m == null ? "null" : String.valueOf(m.kind))).append("\"");
            sb.append(",\"diag\":\"").append(jsonEscape(brain.getLastDiagnostic())).append("\"");
            try { sb.append(",\"free_inv\":").append(bot.getInventory().getFreeSlots()); } catch (Throwable ignored) {}
            try { sb.append(",\"hp\":").append(bot.getHitpoints()); } catch (Throwable ignored) {}
            try { sb.append(",\"locked\":").append(bot.isLocked()); } catch (Throwable ignored) {}
            try { sb.append(",\"working\":").append(bot.getActionManager() != null && bot.getActionManager().hasSkillWorking()); } catch (Throwable ignored) {}
            sb.append("}");
            sendText(ex, 200, sb.toString());
        }
    }

    /** Bot scanner dump: nearest tree/rock/fish/NPC at the bot's tile. */
    private static class BotScanHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String name = queryParam(ex, "name");
            if (name == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"name query required\"}"); return; }
            com.rs.bot.AIPlayer bot = findBot(name);
            if (bot == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"bot not online\"}"); return; }
            com.rs.bot.ai.EnvironmentScanner.TreeMatch tm = com.rs.bot.ai.EnvironmentScanner.findNearestTree(bot, 12);
            com.rs.bot.ai.EnvironmentScanner.RockMatch rm = com.rs.bot.ai.EnvironmentScanner.findNearestRock(bot, 12);
            com.rs.bot.ai.EnvironmentScanner.FishMatch fm = com.rs.bot.ai.EnvironmentScanner.findNearestFishingSpot(bot, 14);
            StringBuilder sb = new StringBuilder("{\"ok\":true");
            sb.append(",\"x\":").append(bot.getX()).append(",\"y\":").append(bot.getY()).append(",\"plane\":").append(bot.getPlane());
            if (tm != null) sb.append(",\"tree\":{\"def\":\"").append(jsonEscape(String.valueOf(tm.definition))).append("\",\"x\":").append(tm.object.getX()).append(",\"y\":").append(tm.object.getY()).append("}");
            else sb.append(",\"tree\":null");
            if (rm != null) sb.append(",\"rock\":{\"def\":\"").append(jsonEscape(String.valueOf(rm.definition))).append("\",\"x\":").append(rm.object.getX()).append(",\"y\":").append(rm.object.getY()).append("}");
            else sb.append(",\"rock\":null");
            if (fm != null) sb.append(",\"fish\":{\"def\":\"").append(jsonEscape(String.valueOf(fm.definition))).append("\",\"x\":").append(fm.npc.getX()).append(",\"y\":").append(fm.npc.getY()).append("}");
            else sb.append(",\"fish\":null");
            sb.append("}");
            sendText(ex, 200, sb.toString());
        }
    }

    /** POST {"name":"X","skill":"wc|mining|fishing|thieving|combat"} - manually drive a TrainingMethod. */
    private static class BotForceHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            java.util.Map<String, String> body = readJsonBody(ex);
            String name = body.get("name");
            String skill = body.get("skill");
            if (name == null || skill == null) { sendText(ex, 400, "{\"ok\":false,\"error\":\"need name+skill\"}"); return; }
            com.rs.bot.AIPlayer bot = findBot(name);
            if (bot == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"bot not online\"}"); return; }
            com.rs.bot.ai.TrainingMethods.Kind kind;
            switch (skill.toLowerCase()) {
                case "wc": case "woodcutting": kind = com.rs.bot.ai.TrainingMethods.Kind.WOODCUTTING; break;
                case "mining":   kind = com.rs.bot.ai.TrainingMethods.Kind.MINING; break;
                case "fishing":  kind = com.rs.bot.ai.TrainingMethods.Kind.FISHING; break;
                case "thieving": kind = com.rs.bot.ai.TrainingMethods.Kind.THIEVING; break;
                case "combat":   kind = com.rs.bot.ai.TrainingMethods.Kind.COMBAT; break;
                default: sendText(ex, 400, "{\"ok\":false,\"error\":\"unknown skill\"}"); return;
            }
            com.rs.bot.ai.TrainingMethods.Method m = com.rs.bot.ai.TrainingMethods.firstApplicable(bot, kind);
            if (m == null) { sendText(ex, 404, "{\"ok\":false,\"error\":\"no applicable method\"}"); return; }
            bot.getBrain().forceTrainingMethod(m);
            sendText(ex, 200, "{\"ok\":true,\"method\":\"" + jsonEscape(m.description) + "\"}");
        }
    }

    /**
     * GET /admin/world/scan?x=X&y=Y&plane=P&radius=R
     * Returns the nearest tree, rock, and fishing spot from any tile in
     * the world. Use this to verify whether a TrainingMethod's coord is
     * actually near the right resource. Defaults: plane=0, radius=24.
     */
    private static class WorldScanHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            int x = parseIntOr(queryParam(ex, "x"), -1);
            int y = parseIntOr(queryParam(ex, "y"), -1);
            int plane = parseIntOr(queryParam(ex, "plane"), 0);
            int radius = parseIntOr(queryParam(ex, "radius"), 24);
            if (x < 0 || y < 0) { sendText(ex, 400, "{\"ok\":false,\"error\":\"need x and y query\"}"); return; }
            com.rs.game.WorldTile from = new com.rs.game.WorldTile(x, y, plane);
            com.rs.bot.ai.EnvironmentScanner.TreeMatch tm = com.rs.bot.ai.EnvironmentScanner.findNearestTree(from, radius);
            com.rs.bot.ai.EnvironmentScanner.RockMatch rm = com.rs.bot.ai.EnvironmentScanner.findNearestRock(from, radius);
            com.rs.bot.ai.EnvironmentScanner.FishMatch fm = com.rs.bot.ai.EnvironmentScanner.findNearestFishingSpot(from, radius);
            StringBuilder sb = new StringBuilder("{\"ok\":true");
            sb.append(",\"x\":").append(x).append(",\"y\":").append(y).append(",\"plane\":").append(plane);
            sb.append(",\"radius\":").append(radius);
            if (tm != null) sb.append(",\"tree\":{\"def\":\"").append(jsonEscape(String.valueOf(tm.definition))).append("\",\"x\":").append(tm.object.getX()).append(",\"y\":").append(tm.object.getY()).append(",\"id\":").append(tm.object.getId()).append("}");
            else sb.append(",\"tree\":null");
            if (rm != null) sb.append(",\"rock\":{\"def\":\"").append(jsonEscape(String.valueOf(rm.definition))).append("\",\"x\":").append(rm.object.getX()).append(",\"y\":").append(rm.object.getY()).append(",\"id\":").append(rm.object.getId()).append("}");
            else sb.append(",\"rock\":null");
            if (fm != null) sb.append(",\"fish\":{\"def\":\"").append(jsonEscape(String.valueOf(fm.definition))).append("\",\"x\":").append(fm.npc.getX()).append(",\"y\":").append(fm.npc.getY()).append(",\"id\":").append(fm.npc.getId()).append("}");
            else sb.append(",\"fish\":null");
            sb.append("}");
            sendText(ex, 200, sb.toString());
        }
    }

    private static com.rs.bot.AIPlayer findBot(String name) {
        for (com.rs.game.player.Player p : com.rs.game.World.getPlayers()) {
            if (p == null || p.hasFinished()) continue;
            if (!(p instanceof com.rs.bot.AIPlayer)) continue;
            if (name.equalsIgnoreCase(p.getDisplayName())) return (com.rs.bot.AIPlayer) p;
        }
        return null;
    }

    private static String queryParam(HttpExchange ex, String key) {
        String q = ex.getRequestURI().getQuery();
        if (q == null) return null;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                try { return java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8"); }
                catch (Exception e) { return null; }
            }
        }
        return null;
    }

    private static java.util.Map<String, String> readJsonBody(HttpExchange ex) throws IOException {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        java.io.InputStream is = ex.getRequestBody();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
        String body = baos.toString("UTF-8").trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length() - 1);
        for (String pair : body.split(",")) {
            int colon = pair.indexOf(':');
            if (colon <= 0) continue;
            String k = pair.substring(0, colon).trim().replace("\"", "");
            String v = pair.substring(colon + 1).trim().replace("\"", "");
            out.put(k, v);
        }
        return out;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    // ===== Citizen handlers =====

    private static class CitizensListHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            try {
                int total = com.rs.bot.ambient.CitizenSpawner.liveCount();
                java.util.List<com.rs.bot.AIPlayer> live =
                    com.rs.bot.ambient.CitizenSpawner.getLive();
                // Per-archetype + per-state tally + per-bot detail array.
                java.util.Map<String, Integer> byArch = new java.util.TreeMap<>();
                java.util.Map<String, Integer> byState = new java.util.TreeMap<>();
                StringBuilder bots = new StringBuilder();
                bots.append("[");
                boolean firstBot = true;
                for (com.rs.bot.AIPlayer b : live) {
                    if (b == null) continue;
                    if (!(b.getBrain() instanceof com.rs.bot.ambient.CitizenBrain)) continue;
                    com.rs.bot.ambient.CitizenBrain cb = (com.rs.bot.ambient.CitizenBrain) b.getBrain();
                    String a = cb.getArchetype() == null ? "?" : cb.getArchetype().name();
                    byArch.merge(a, 1, Integer::sum);
                    String s = cb.getState() == null ? "?" : cb.getState().name();
                    byState.merge(s, 1, Integer::sum);
                    if (!firstBot) bots.append(",");
                    firstBot = false;
                    bots.append("{")
                        .append("\"name\":\"").append(jsonEscape(b.getDisplayName() == null ? "?" : b.getDisplayName())).append("\",")
                        .append("\"archetype\":\"").append(jsonEscape(a)).append("\",")
                        .append("\"state\":\"").append(jsonEscape(s)).append("\",")
                        .append("\"x\":").append(b.getX()).append(",")
                        .append("\"y\":").append(b.getY()).append(",")
                        .append("\"plane\":").append(b.getPlane()).append(",")
                        .append("\"cb\":");
                    int cbLvl = 3;
                    try { cbLvl = b.getSkills().getCombatLevel(); } catch (Throwable ignored) {}
                    bots.append(cbLvl);
                    bots.append("}");
                }
                bots.append("]");

                StringBuilder sb = new StringBuilder("{\"ok\":true,\"total\":").append(total);
                sb.append(",\"byArchetype\":{");
                boolean first = true;
                for (java.util.Map.Entry<String,Integer> e : byArch.entrySet()) {
                    if (!first) sb.append(","); first = false;
                    sb.append("\"").append(jsonEscape(e.getKey())).append("\":").append(e.getValue());
                }
                sb.append("},\"byState\":{");
                first = true;
                for (java.util.Map.Entry<String,Integer> e : byState.entrySet()) {
                    if (!first) sb.append(","); first = false;
                    sb.append("\"").append(jsonEscape(e.getKey())).append("\":").append(e.getValue());
                }
                sb.append("},\"bots\":").append(bots);
                sb.append("}");
                sendText(ex, 200, sb.toString());
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.toString()) + "\"}");
            }
        }
    }

    private static class CitizensSpawnHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            int count;
            try { count = Integer.parseInt(body.getOrDefault("count", "10")); }
            catch (NumberFormatException e) { sendText(ex, 400, "{\"ok\":false,\"error\":\"bad count\"}"); return; }
            count = Math.max(1, Math.min(count, 500)); // cap
            String category = body.get("category");
            int x, y, plane;
            try {
                x = Integer.parseInt(body.getOrDefault("x", "3222"));
                y = Integer.parseInt(body.getOrDefault("y", "3218"));
                plane = Integer.parseInt(body.getOrDefault("plane", "0"));
            } catch (NumberFormatException e) {
                sendText(ex, 400, "{\"ok\":false,\"error\":\"bad coords\"}"); return;
            }
            int scatter;
            try { scatter = Integer.parseInt(body.getOrDefault("scatter", "12")); }
            catch (NumberFormatException e) { scatter = 12; }
            try {
                com.rs.game.WorldTile anchor = new com.rs.game.WorldTile(x, y, plane);
                java.util.List<com.rs.bot.AIPlayer> spawned =
                    com.rs.bot.ambient.CitizenSpawner.spawnBatch(count, category, anchor, scatter);
                int total = com.rs.bot.ambient.CitizenSpawner.liveCount();
                sendText(ex, 200, "{\"ok\":true,\"spawned\":" + spawned.size()
                    + ",\"total\":" + total + "}");
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.toString()) + "\"}");
            }
        }
    }

    private static class CitizensClearHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            int removed = com.rs.bot.ambient.CitizenSpawner.clearAll();
            sendText(ex, 200, "{\"ok\":true,\"removed\":" + removed + "}");
        }
    }

    /**
     * GET  /admin/citizens/budget  - returns the slot list as JSON
     * POST /admin/citizens/budget  - body is {"slots":[...]} - replaces config + saves
     */
    private static class CitizensBudgetHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                java.util.List<com.rs.bot.ambient.CitizenBudget.Slot> slots = com.rs.bot.ambient.CitizenBudget.getSlots();
                StringBuilder sb = new StringBuilder("{\"ok\":true,\"slots\":[");
                for (int i = 0; i < slots.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(slots.get(i).toJson());
                }
                sb.append("]}");
                sendText(ex, 200, sb.toString());
                return;
            }
            if (!"POST".equalsIgnoreCase(method)) {
                sendText(ex, 405, "{\"ok\":false,\"error\":\"GET or POST only\"}");
                return;
            }
            // POST: replace slots with body content. Body format matches the GET output:
            //   {"slots":[{"archetype":"X","count":N,"x":...,"y":...,"plane":...,"scatter":...,"autospawn":bool}, ...]}
            String body = readBody(ex);
            try {
                java.util.List<com.rs.bot.ambient.CitizenBudget.Slot> parsed = parseBudgetSlots(body);
                com.rs.bot.ambient.CitizenBudget.setSlots(parsed);
                sendText(ex, 200, "{\"ok\":true,\"saved\":" + parsed.size() + "}");
            } catch (Throwable t) {
                sendText(ex, 400, "{\"ok\":false,\"error\":\"" + jsonEscape(t.toString()) + "\"}");
            }
        }
    }

    /** POST /admin/citizens/budget/apply - spawn enough to hit each slot's target.
     *  Optional body field "includeManual": true to also fill non-autospawn slots. */
    private static class CitizensBudgetApplyHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            Map<String,String> body = parseBody(ex);
            boolean includeManual = "true".equalsIgnoreCase(body.getOrDefault("includeManual", "true"));
            try {
                int spawned = com.rs.bot.ambient.CitizenBudget.applyBudget(includeManual);
                int total = com.rs.bot.ambient.CitizenSpawner.liveCount();
                sendText(ex, 200, "{\"ok\":true,\"spawned\":" + spawned + ",\"total\":" + total + "}");
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.toString()) + "\"}");
            }
        }
    }

    /** GET /admin/citizens/archetypes - returns the list of available archetype names
     *  + their categories so the panel can populate dropdowns. */
    private static class CitizensArchetypesHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            StringBuilder sb = new StringBuilder("{\"ok\":true,\"archetypes\":[");
            com.rs.bot.ambient.AmbientArchetype[] all = com.rs.bot.ambient.AmbientArchetype.values();
            for (int i = 0; i < all.length; i++) {
                if (i > 0) sb.append(",");
                com.rs.bot.ambient.AmbientArchetype a = all[i];
                String cat = a.isSkiller() ? "skiller"
                          : a.isCombatant() ? "combatant"
                          : a.isSocialite() ? "socialite"
                          : a.isMinigamer() ? "minigamer"
                          : "other";
                sb.append("{\"name\":\"").append(a.name())
                  .append("\",\"label\":\"").append(jsonEscape(a.label))
                  .append("\",\"category\":\"").append(cat).append("\"}");
            }
            sb.append("]}");
            sendText(ex, 200, sb.toString());
        }
    }

    /** GET /admin/ge/prices?ids=4151,11696,1038
     *      or  /admin/ge/prices?catalog=1
     *  Returns {"ok":true,"prices":{"4151":86700,...},"names":{"4151":"abyssal whip"}}
     *  When catalog=1, includes every id present in the bot StockEntry
     *  catalog so the admin panel can show one-row-per-item table.
     *  When ids=... is given, fetches just those. */
    private static class GePricesGetHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            try {
                java.util.Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
                java.util.List<Integer> ids = new java.util.ArrayList<>();
                String idsParam = q.get("ids");
                if (idsParam != null && !idsParam.isEmpty()) {
                    for (String s : idsParam.split(",")) {
                        try { ids.add(Integer.parseInt(s.trim())); }
                        catch (Throwable ignored) {}
                    }
                }
                if (ids.isEmpty() && "1".equals(q.get("catalog"))) {
                    // Every id from the bot trader catalog file.
                    java.util.Set<Integer> seen = new java.util.LinkedHashSet<>();
                    java.io.File f = new java.io.File("src/com/rs/bot/ambient/BotTradeHandler.java");
                    if (f.isFile()) {
                        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                            "new\\s+StockEntry\\(\\s*(\\d+)\\s*,");
                        java.util.regex.Matcher m = p.matcher(
                            new String(java.nio.file.Files.readAllBytes(f.toPath())));
                        while (m.find()) {
                            try { seen.add(Integer.parseInt(m.group(1))); }
                            catch (Throwable ignored) {}
                        }
                    }
                    ids.addAll(seen);
                }
                StringBuilder prices = new StringBuilder("{");
                StringBuilder names = new StringBuilder("{");
                boolean first = true;
                for (int id : ids) {
                    if (id <= 0) continue;
                    int price;
                    try {
                        price = com.rs.game.player.content.grandExchange
                            .GrandExchange.getPrice(id);
                    } catch (Throwable t) { price = -1; }
                    String name = "";
                    try {
                        com.rs.cache.loaders.ItemDefinitions def =
                            com.rs.cache.loaders.ItemDefinitions.getItemDefinitions(id);
                        if (def != null && def.getName() != null) name = def.getName();
                    } catch (Throwable ignored) {}
                    if (!first) { prices.append(","); names.append(","); }
                    first = false;
                    prices.append("\"").append(id).append("\":").append(price);
                    names.append("\"").append(id).append("\":\"")
                          .append(jsonEscape(name)).append("\"");
                }
                prices.append("}");
                names.append("}");
                sendText(ex, 200, "{\"ok\":true,\"prices\":" + prices
                    + ",\"names\":" + names + "}");
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\""
                    + jsonEscape(t.toString()) + "\"}");
            }
        }
    }

    private static java.util.Map<String, String> parseQuery(String q) {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        if (q == null || q.isEmpty()) return out;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            try {
                String k = java.net.URLDecoder.decode(
                    eq < 0 ? pair : pair.substring(0, eq), "UTF-8");
                String v = eq < 0 ? "" : java.net.URLDecoder.decode(
                    pair.substring(eq + 1), "UTF-8");
                out.put(k, v);
            } catch (Throwable ignored) {}
        }
        return out;
    }

    /** POST /admin/ge/prices/bulk - body {"prices":{"id":price,...}}.
     *  Updates GrandExchange.PRICES via setPrice() then saves to disk.
     *  Returns count of applied + skipped (id <= 0 or price <= 0). */
    private static class GePricesBulkHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            try {
                java.io.InputStream in = ex.getRequestBody();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                String body = new String(out.toByteArray(), "UTF-8");
                java.util.Map<Integer, Integer> pairs = parseGePricesBody(body);
                int applied = 0;
                int skipped = 0;
                for (java.util.Map.Entry<Integer, Integer> e : pairs.entrySet()) {
                    int id = e.getKey();
                    int price = e.getValue();
                    if (id <= 0 || price <= 0) { skipped++; continue; }
                    try {
                        com.rs.game.player.content.grandExchange.GrandExchange.setPrice(id, price);
                        applied++;
                    } catch (Throwable t) {
                        skipped++;
                    }
                }
                try {
                    com.rs.game.player.content.grandExchange.GrandExchange.savePrices();
                } catch (Throwable ignored) {}
                sendText(ex, 200, "{\"ok\":true,\"applied\":" + applied
                    + ",\"skipped\":" + skipped + "}");
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\""
                    + jsonEscape(t.toString()) + "\"}");
            }
        }
    }

    /** Hand-rolled parser for {"prices":{"4151":240000,"11696":2500000}}.
     *  Tolerant - whitespace, missing trailing commas, integer-only values.
     *  Returns empty map on any structural problem. */
    private static java.util.Map<Integer, Integer> parseGePricesBody(String json) {
        java.util.Map<Integer, Integer> out = new java.util.HashMap<>();
        if (json == null) return out;
        int pricesKey = json.indexOf("\"prices\"");
        if (pricesKey < 0) return out;
        int objStart = json.indexOf('{', pricesKey);
        if (objStart < 0) return out;
        // Walk balanced braces to find object end.
        int depth = 0, objEnd = -1;
        for (int i = objStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { objEnd = i; break; }
            }
        }
        if (objEnd < 0) return out;
        String inner = json.substring(objStart + 1, objEnd);
        // Match "<number>": <number>
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "\"(\\d+)\"\\s*:\\s*(\\d+)").matcher(inner);
        while (m.find()) {
            try {
                int id = Integer.parseInt(m.group(1));
                long p = Long.parseLong(m.group(2));
                if (p > Integer.MAX_VALUE) p = Integer.MAX_VALUE;
                out.put(id, (int) p);
            } catch (Throwable ignored) {}
        }
        return out;
    }

    /** Lightweight JSON parse for budget body: {"slots":[{...},{...}]}.
     *  Reuses the same opportunistic parser style as CitizenBudget. */
    private static java.util.List<com.rs.bot.ambient.CitizenBudget.Slot> parseBudgetSlots(String json) {
        java.util.List<com.rs.bot.ambient.CitizenBudget.Slot> out = new java.util.ArrayList<>();
        if (json == null) return out;
        int arrStart = json.indexOf('[');
        int arrEnd = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd < 0 || arrEnd <= arrStart) return out;
        String arr = json.substring(arrStart + 1, arrEnd);
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                cur.append(c);
                if (depth == 0) {
                    com.rs.bot.ambient.CitizenBudget.Slot s = parseOneSlot(cur.toString());
                    if (s != null) out.add(s);
                    cur.setLength(0);
                }
                continue;
            }
            if (depth == 0 && (c == ',' || Character.isWhitespace(c))) continue;
            cur.append(c);
        }
        return out;
    }

    private static com.rs.bot.ambient.CitizenBudget.Slot parseOneSlot(String obj) {
        com.rs.bot.ambient.CitizenBudget.Slot s = new com.rs.bot.ambient.CitizenBudget.Slot();
        s.archetype = bExtractStr(obj, "archetype");
        s.count   = bExtractInt(obj, "count", 0);
        s.x       = bExtractInt(obj, "x", 0);
        s.y       = bExtractInt(obj, "y", 0);
        s.plane   = bExtractInt(obj, "plane", 0);
        s.scatter = bExtractInt(obj, "scatter", 8);
        s.autospawn = bExtractBool(obj, "autospawn", false);
        return s.archetype == null ? null : s;
    }

    private static String bExtractStr(String obj, String key) {
        String marker = "\"" + key + "\":\"";
        int i = obj.indexOf(marker);
        if (i < 0) return null;
        i += marker.length();
        int end = obj.indexOf('"', i);
        return end < 0 ? null : obj.substring(i, end);
    }

    private static int bExtractInt(String obj, String key, int def) {
        String marker = "\"" + key + "\":";
        int i = obj.indexOf(marker);
        if (i < 0) return def;
        i += marker.length();
        int end = i;
        while (end < obj.length() && (obj.charAt(end) == '-' || Character.isDigit(obj.charAt(end)))) end++;
        if (end == i) return def;
        try { return Integer.parseInt(obj.substring(i, end)); }
        catch (Throwable t) { return def; }
    }

    private static boolean bExtractBool(String obj, String key, boolean def) {
        String marker = "\"" + key + "\":";
        int i = obj.indexOf(marker);
        if (i < 0) return def;
        i += marker.length();
        if (obj.startsWith("true", i)) return true;
        if (obj.startsWith("false", i)) return false;
        return def;
    }

    /** Read raw request body as a UTF-8 string. */
    private static String readBody(HttpExchange ex) throws IOException {
        java.io.InputStream is = ex.getRequestBody();
        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int n;
        while ((n = is.read(buf)) > 0) bout.write(buf, 0, n);
        return bout.toString("UTF-8");
    }

    // ===== Profiler handlers =====

    private static class ProfilerStartHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            com.rs.executor.WorldTickProfiler.enable();
            sendText(ex, 200, "{\"ok\":true,\"enabled\":true}");
        }
    }

    private static class ProfilerStopHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            com.rs.executor.WorldTickProfiler.disable();
            sendText(ex, 200, "{\"ok\":true,\"enabled\":false}");
        }
    }

    private static class ProfilerDumpHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            com.rs.executor.WorldTickProfiler.dump();
            sendText(ex, 200, "{\"ok\":true,\"dumped\":true}");
        }
    }

    // ===== Cache status handler =====

    private static class CacheStatusHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            try {
                int primaryIdx = (com.rs.cache.Cache.STORE != null && com.rs.cache.Cache.STORE.getIndexes() != null)
                    ? com.rs.cache.Cache.STORE.getIndexes().length : 0;
                int dlcIdx = (com.rs.cache.Cache.STORE_DLC != null && com.rs.cache.Cache.STORE_DLC.getIndexes() != null)
                    ? com.rs.cache.Cache.STORE_DLC.getIndexes().length : 0;
                StringBuilder sb = new StringBuilder("{\"ok\":true");
                sb.append(",\"primaryPath\":\"").append(jsonEscape(com.rs.Settings.CACHE_PATH_PRIMARY == null ? "" : com.rs.Settings.CACHE_PATH_PRIMARY)).append("\"");
                sb.append(",\"primaryLoaded\":").append(primaryIdx > 0);
                sb.append(",\"primaryIndexes\":").append(primaryIdx);
                sb.append(",\"legacyPath\":\"").append(jsonEscape(com.rs.Settings.CACHE_PATH_LEGACY == null ? "" : com.rs.Settings.CACHE_PATH_LEGACY)).append("\"");
                sb.append(",\"dlcPath\":\"").append(jsonEscape(com.rs.Settings.CACHE_PATH_DLC == null ? "" : com.rs.Settings.CACHE_PATH_DLC)).append("\"");
                sb.append(",\"dlcLoaded\":").append(dlcIdx > 0);
                sb.append(",\"dlcIndexes\":").append(dlcIdx);
                sb.append(",\"dlcEnabled\":").append(com.rs.Settings.DLC_FALLBACK_ENABLED);
                sb.append("}");
                sendText(ex, 200, sb.toString());
            } catch (Throwable t) {
                sendText(ex, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(t.toString()) + "\"}");
            }
        }
    }
}
