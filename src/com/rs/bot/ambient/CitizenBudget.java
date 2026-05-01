package com.rs.bot.ambient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistent population budget for Citizen-tier bots.
 *
 * Stored as JSON in data/citizen_budget.json. Each entry tells the spawner:
 *   - which archetype to spawn (e.g., SOCIALITE_BANKSTAND)
 *   - target count (how many should be alive at any time)
 *   - anchor (where to scatter them around)
 *   - scatter radius
 *   - autospawn flag (whether to spawn on server start)
 *
 * The admin panel reads/writes this file via /admin/citizens/budget. On
 * server boot, BotPool's startup hook (or a similar mechanism) calls
 * CitizenBudget.applyBudget() to fill any auto-spawn slots.
 *
 * Hand-rolled JSON read/write to avoid pulling in a dependency. Format
 * matches what Python json.dumps would emit for the same dict layout, so
 * the admin panel can use plain json without any custom parsing.
 */
public final class CitizenBudget {

    private static final String CONFIG_PATH = "data/citizen_budget.json";

    public static final class Slot {
        public String archetype;     // AmbientArchetype enum name
        public int count;            // target alive count
        public int x, y, plane;      // anchor world tile
        public int scatter;          // radius around anchor
        public boolean autospawn;    // spawn on server start

        public Slot() {}

        public Slot(String archetype, int count, int x, int y, int plane,
                    int scatter, boolean autospawn) {
            this.archetype = archetype;
            this.count = count;
            this.x = x; this.y = y; this.plane = plane;
            this.scatter = scatter;
            this.autospawn = autospawn;
        }

        public String toJson() {
            return "{\"archetype\":\"" + jsonEscape(archetype == null ? "" : archetype)
                + "\",\"count\":" + count
                + ",\"x\":" + x
                + ",\"y\":" + y
                + ",\"plane\":" + plane
                + ",\"scatter\":" + scatter
                + ",\"autospawn\":" + (autospawn ? "true" : "false")
                + "}";
        }
    }

    private static List<Slot> slots = new ArrayList<>();
    private static boolean loaded = false;

    private CitizenBudget() {}

    /** Load from disk. Idempotent - safe to call multiple times. */
    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        File f = new File(CONFIG_PATH);
        if (!f.isFile()) {
            seedDefaults();
            save();
            return;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            slots = parseSlots(sb.toString());
            System.out.println("[CitizenBudget] loaded " + slots.size() + " slots from " + CONFIG_PATH);
        } catch (Throwable t) {
            System.err.println("[CitizenBudget] load failed: " + t);
            seedDefaults();
        }
    }

    public static synchronized void save() {
        File f = new File(CONFIG_PATH);
        File dir = f.getParentFile();
        if (dir != null) dir.mkdirs();
        try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
            w.print("{\"slots\":[");
            for (int i = 0; i < slots.size(); i++) {
                if (i > 0) w.print(",");
                w.print(slots.get(i).toJson());
            }
            w.println("]}");
            System.out.println("[CitizenBudget] saved " + slots.size() + " slots to " + CONFIG_PATH);
        } catch (Throwable t) {
            System.err.println("[CitizenBudget] save failed: " + t);
        }
    }

    public static synchronized List<Slot> getSlots() {
        load();
        return new ArrayList<>(slots);
    }

    public static synchronized void setSlots(List<Slot> newSlots) {
        slots = new ArrayList<>(newSlots == null ? Collections.<Slot>emptyList() : newSlots);
        loaded = true;
        save();
    }

    /**
     * Spawn enough Citizens to bring each slot's live count up to target.
     * Counts are by archetype only - if you have 80 SOCIALITE_BANKSTAND
     * and the slot says 100, this spawns 20 more at the slot's anchor.
     *
     * Skips slots flagged autospawn=false unless includeManual is true.
     */
    public static synchronized int applyBudget(boolean includeManual) {
        load();
        int spawned = 0;
        java.util.Map<String, Integer> liveByArch = new java.util.HashMap<>();
        for (com.rs.bot.AIPlayer bot : CitizenSpawner.getLive()) {
            if (!(bot.getBrain() instanceof CitizenBrain)) continue;
            CitizenBrain cb = (CitizenBrain) bot.getBrain();
            if (cb.getArchetype() == null) continue;
            liveByArch.merge(cb.getArchetype().name(), 1, Integer::sum);
        }
        for (Slot s : slots) {
            if (!s.autospawn && !includeManual) continue;
            int alive = liveByArch.getOrDefault(s.archetype, 0);
            int need = Math.max(0, s.count - alive);
            if (need == 0) continue;
            try {
                AmbientArchetype arch = AmbientArchetype.valueOf(s.archetype);
                com.rs.game.WorldTile anchor = new com.rs.game.WorldTile(s.x, s.y, s.plane);
                String category = arch.isSkiller() ? "skiller"
                                : arch.isCombatant() ? "combatant"
                                : arch.isSocialite() ? "socialite"
                                : arch.isMinigamer() ? "minigamer"
                                : null;
                java.util.List<com.rs.bot.AIPlayer> batch =
                    CitizenSpawner.spawnBatch(need, category, anchor, s.scatter);
                // batch.size() is just the FIRST sync spawn; the rest are
                // queued. We count what we requested as "spawned planned".
                spawned += need;
            } catch (Throwable t) {
                System.err.println("[CitizenBudget] applyBudget slot failed: " + s.toJson() + " -> " + t);
            }
        }
        return spawned;
    }

    /** Default budget seed: a small mixed-population spawn at GE so first-time
     *  users see SOMETHING rather than an empty world. */
    private static void seedDefaults() {
        slots.clear();
        // Grand Exchange anchor (Varrock GE): ~3164, 3486
        slots.add(new Slot("SOCIALITE_BANKSTAND",   30, 3164, 3486, 0, 8, false));
        slots.add(new Slot("SOCIALITE_GE_TRADER",   25, 3164, 3486, 0, 8, false));
        slots.add(new Slot("SOCIALITE_GAMBLER",     10, 3164, 3486, 0, 6, false));
        // Lumbridge skillers
        slots.add(new Slot("SKILLER_NOOB",          15, 3222, 3218, 0, 10, false));
        slots.add(new Slot("SKILLER_CASUAL",        15, 3222, 3218, 0, 10, false));
        // Edgeville combat / wilderness edge
        slots.add(new Slot("COMBATANT_PURE",        10, 3088, 3491, 0, 8, false));
        slots.add(new Slot("COMBATANT_TANK",        10, 3088, 3491, 0, 8, false));
        slots.add(new Slot("COMBATANT_HYBRID",      8,  3088, 3491, 0, 8, false));
        // Minigame areas
        slots.add(new Slot("MINIGAMER_RUSHER",      8,  2440, 3093, 0, 8, false));
        slots.add(new Slot("MINIGAMER_DEFENDER",    8,  2440, 3093, 0, 8, false));
    }

    // === Minimal JSON parsing (slot list only) ===

    private static List<Slot> parseSlots(String json) {
        List<Slot> out = new ArrayList<>();
        // Find each "{...}" inside "slots":[...]
        int slotsKey = json.indexOf("\"slots\"");
        if (slotsKey < 0) return out;
        int arrStart = json.indexOf('[', slotsKey);
        int arrEnd = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd < 0 || arrEnd <= arrStart) return out;
        String arr = json.substring(arrStart + 1, arrEnd);
        // Split on '},{' boundaries while balancing braces.
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                cur.append(c);
                if (depth == 0) {
                    Slot s = parseOneSlot(cur.toString());
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

    private static Slot parseOneSlot(String obj) {
        if (obj == null || obj.isEmpty()) return null;
        Slot s = new Slot();
        s.archetype = extractStr(obj, "archetype");
        s.count   = extractInt(obj, "count", 0);
        s.x       = extractInt(obj, "x", 0);
        s.y       = extractInt(obj, "y", 0);
        s.plane   = extractInt(obj, "plane", 0);
        s.scatter = extractInt(obj, "scatter", 8);
        s.autospawn = extractBool(obj, "autospawn", false);
        return s.archetype == null ? null : s;
    }

    private static String extractStr(String obj, String key) {
        String marker = "\"" + key + "\":\"";
        int i = obj.indexOf(marker);
        if (i < 0) return null;
        i += marker.length();
        int end = obj.indexOf('"', i);
        return end < 0 ? null : obj.substring(i, end);
    }

    private static int extractInt(String obj, String key, int def) {
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

    private static boolean extractBool(String obj, String key, boolean def) {
        String marker = "\"" + key + "\":";
        int i = obj.indexOf(marker);
        if (i < 0) return def;
        i += marker.length();
        if (obj.startsWith("true", i)) return true;
        if (obj.startsWith("false", i)) return false;
        return def;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
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
}
