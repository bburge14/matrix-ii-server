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

    /** Bumped whenever seedDefaults() changes. Old config files without
     *  this version (or with a lower one) get auto-replaced with the new
     *  defaults on load. Avoids requiring users to manually `rm` the file
     *  every time we ship new GE socialite anchor coords. */
    private static final int CURRENT_SCHEMA_VERSION = 4;

    public static final class Slot {
        public String archetype;     // AmbientArchetype enum name
        public int count;            // target alive count
        public int x, y, plane;      // primary anchor world tile
        public int scatter;          // radius around anchor
        public boolean autospawn;    // spawn on server start
        /** Optional additional anchors. Each spawn picks one of (primary)
         *  + extraAnchors at random, so a single slot can scatter bots
         *  across multiple skill spots / GE corners / wildy zones. Each
         *  entry is [x, y, plane]. Empty list = single-anchor behaviour. */
        public java.util.List<int[]> extraAnchors = new java.util.ArrayList<>();

        public Slot() {}

        public Slot(String archetype, int count, int x, int y, int plane,
                    int scatter, boolean autospawn) {
            this.archetype = archetype;
            this.count = count;
            this.x = x; this.y = y; this.plane = plane;
            this.scatter = scatter;
            this.autospawn = autospawn;
        }

        /** Random pick from the (primary + extraAnchors) pool. */
        public int[] pickAnchor() {
            int total = 1 + (extraAnchors == null ? 0 : extraAnchors.size());
            int idx = total <= 1 ? 0 : com.rs.utils.Utils.random(total);
            if (idx == 0) return new int[] { x, y, plane };
            return extraAnchors.get(idx - 1);
        }

        public String toJson() {
            StringBuilder extras = new StringBuilder();
            if (extraAnchors != null && !extraAnchors.isEmpty()) {
                extras.append(",\"extra_anchors\":[");
                for (int i = 0; i < extraAnchors.size(); i++) {
                    int[] a = extraAnchors.get(i);
                    if (i > 0) extras.append(',');
                    extras.append('[').append(a[0]).append(',').append(a[1])
                        .append(',').append(a[2]).append(']');
                }
                extras.append(']');
            }
            return "{\"archetype\":\"" + jsonEscape(archetype == null ? "" : archetype)
                + "\",\"count\":" + count
                + ",\"x\":" + x
                + ",\"y\":" + y
                + ",\"plane\":" + plane
                + ",\"scatter\":" + scatter
                + ",\"autospawn\":" + (autospawn ? "true" : "false")
                + extras.toString()
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
            String json = sb.toString();
            int diskVersion = parseSchemaVersion(json);
            if (diskVersion < CURRENT_SCHEMA_VERSION) {
                System.out.println("[CitizenBudget] config schema v" + diskVersion
                    + " < current v" + CURRENT_SCHEMA_VERSION
                    + " - reseeding defaults (old file backed up)");
                try {
                    java.nio.file.Files.copy(f.toPath(),
                        new File(CONFIG_PATH + ".bak.v" + diskVersion).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Throwable ignored) {}
                seedDefaults();
                save();
                return;
            }
            slots = parseSlots(json);
            System.out.println("[CitizenBudget] loaded " + slots.size() + " slots from " + CONFIG_PATH);
        } catch (Throwable t) {
            System.err.println("[CitizenBudget] load failed: " + t);
            seedDefaults();
        }
    }

    /** Pulls "schema":N from the JSON, returns 0 if missing. */
    private static int parseSchemaVersion(String json) {
        try {
            int i = json.indexOf("\"schema\":");
            if (i < 0) return 0;
            i += "\"schema\":".length();
            int end = i;
            while (end < json.length() && (json.charAt(end) == '-' || Character.isDigit(json.charAt(end)))) end++;
            if (end == i) return 0;
            return Integer.parseInt(json.substring(i, end));
        } catch (Throwable t) {
            return 0;
        }
    }

    public static synchronized void save() {
        File f = new File(CONFIG_PATH);
        File dir = f.getParentFile();
        if (dir != null) dir.mkdirs();
        try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
            w.print("{\"schema\":" + CURRENT_SCHEMA_VERSION + ",\"slots\":[");
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

    /** Wipe + reseed defaults. Backs up current config first. */
    public static synchronized int reseedDefaults() {
        File f = new File(CONFIG_PATH);
        if (f.isFile()) {
            try {
                java.nio.file.Files.copy(f.toPath(),
                    new File(CONFIG_PATH + ".bak.reseed-" + System.currentTimeMillis()).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Throwable ignored) {}
        }
        seedDefaults();
        loaded = true;
        save();
        return slots.size();
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
                // Each spawn picks a random anchor from the slot's pool
                // (primary + extraAnchors). Spreads bots across multiple
                // skill spots / GE corners / wildy zones from one slot
                // entry instead of stacking them all at one tile.
                int spawnedHere = 0;
                for (int i = 0; i < need; i++) {
                    int[] a = s.pickAnchor();
                    com.rs.game.WorldTile anchor = new com.rs.game.WorldTile(a[0], a[1], a[2]);
                    java.util.List<com.rs.bot.AIPlayer> one =
                        CitizenSpawner.spawnBatch(1, s.archetype, anchor, s.scatter);
                    spawnedHere += (one == null ? 0 : one.size());
                }
                spawned += spawnedHere;
                if (false) {
                // Legacy single-anchor path kept as inline doc reference.
                com.rs.game.WorldTile anchor = new com.rs.game.WorldTile(s.x, s.y, s.plane);
                // Pass the archetype name directly so spawner picks THAT exact
                // archetype (CITIZEN_GE_TRADER_RARE etc) instead of a random
                // socialite. randomFor() handles enum-name match before falling
                // back to category buckets.
                java.util.List<com.rs.bot.AIPlayer> batch =
                    CitizenSpawner.spawnBatch(need, s.archetype, anchor, s.scatter);
                // batch.size() is just the FIRST sync spawn; the rest are
                // queued. We count what we requested as "spawned planned".
                }
            } catch (Throwable t) {
                System.err.println("[CitizenBudget] applyBudget slot failed: " + s.toJson() + " -> " + t);
            }
        }
        return spawned;
    }

    // === Named budget profiles ===
    // Stored under data/citizens/budgets/<name>.json so admins can
    // save snapshots ("debug-pkers", "live-prod", "wildy-stress")
    // and swap between them without losing previous configs. Each
    // profile is a complete slot list - load REPLACES the active
    // budget atomically.

    private static final String PROFILES_DIR = "data/citizens/budgets";

    public static synchronized java.util.List<String> listProfiles() {
        java.util.List<String> out = new java.util.ArrayList<>();
        File dir = new File(PROFILES_DIR);
        if (!dir.isDirectory()) return out;
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            String n = f.getName();
            if (f.isFile() && n.endsWith(".json")) {
                out.add(n.substring(0, n.length() - 5));
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    /** Save the current active budget as a named profile. Overwrites
     *  if a profile with the same name already exists. */
    public static synchronized boolean saveProfile(String name) {
        if (!isSafeProfileName(name)) return false;
        load();
        File dir = new File(PROFILES_DIR);
        if (!dir.isDirectory()) dir.mkdirs();
        File out = new File(dir, name + ".json");
        try (PrintWriter w = new PrintWriter(out)) {
            w.println(currentJson());
            return true;
        } catch (Throwable t) {
            System.err.println("[CitizenBudget] saveProfile " + name + " failed: " + t);
            return false;
        }
    }

    /** Replace the active budget with the contents of a named profile.
     *  Returns the slot count loaded, or -1 on failure. */
    public static synchronized int loadProfile(String name) {
        if (!isSafeProfileName(name)) return -1;
        File f = new File(PROFILES_DIR, name + ".json");
        if (!f.isFile()) return -1;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            java.util.List<Slot> parsed = parseSlots(sb.toString());
            slots = parsed;
            loaded = true;
            save();
            return slots.size();
        } catch (Throwable t) {
            System.err.println("[CitizenBudget] loadProfile " + name + " failed: " + t);
            return -1;
        }
    }

    public static synchronized boolean deleteProfile(String name) {
        if (!isSafeProfileName(name)) return false;
        File f = new File(PROFILES_DIR, name + ".json");
        return f.isFile() && f.delete();
    }

    private static boolean isSafeProfileName(String name) {
        if (name == null || name.isEmpty()) return false;
        // Disallow path traversal / special chars - admin panel only.
        return name.matches("[A-Za-z0-9_\\-]{1,40}");
    }

    /** Build the on-disk JSON for the current slots without writing it,
     *  reused by saveProfile(). Uses the same shape as save()'s output. */
    private static String currentJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"schema\":").append(CURRENT_SCHEMA_VERSION).append(",\"slots\":[");
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(slots.get(i).toJson());
        }
        sb.append("]}");
        return sb.toString();
    }

    /** Default budget seed.
     *
     *  Population matches the user's tested baseline:
     *    Skillers     - 50 noob + 80 casual + 80 efficient = 210 across
     *                    ~10 multi-anchor skill spots so they spread
     *                    naturally across the world map.
     *    Combatants   - 100 pure + 50 tank + 100 hybrid = 250
     *                    spawned at training spots (rock crabs, sand
     *                    crabs, slayer tower, stronghold, dragons).
     *    PKers        - 50 split 30 LURE + 20 HUNTER. LURE multi-
     *                    anchored across 6 ditch-cluster tiles so the
     *                    wildy lvl 1-3 strip stays active. HUNTER
     *                    multi-anchored across 8 mid-deep tiles
     *                    spread from lvl 5 to lvl 50.
     *    Socialites   - 4 bankstand + 4 gambler at GE for crowd flavour.
     *
     *  All slots autospawn=true so the server boots with a populated
     *  world. Quick-spawn overrides + named profiles let admins layer
     *  scenarios on top.
     */
    private static void seedDefaults() {
        slots.clear();

        // ===== Skillers (210 total, multi-anchored across guilds + spots) =====
        Slot sNoob = new Slot("SKILLER_NOOB",      50, 3225, 3220, 0, 14, true);
        sNoob.extraAnchors.add(new int[] { 3088, 3232, 0 });   // Draynor willows
        sNoob.extraAnchors.add(new int[] { 3175, 3413, 0 });   // Varrock west park
        sNoob.extraAnchors.add(new int[] { 3253, 3266, 0 });   // Lumby cow field
        sNoob.extraAnchors.add(new int[] { 3046, 3322, 0 });   // Falador east oaks
        sNoob.extraAnchors.add(new int[] { 3214, 3247, 0 });   // Lumby general store area
        slots.add(sNoob);

        Slot sCasual = new Slot("SKILLER_CASUAL",  80, 2706, 3464, 0, 14, true);
        sCasual.extraAnchors.add(new int[] { 2841, 3434, 0 });  // Catherby fishing
        sCasual.extraAnchors.add(new int[] { 3047, 9762, 0 });  // Mining guild
        sCasual.extraAnchors.add(new int[] { 2599, 3422, 0 });  // Fishing guild
        sCasual.extraAnchors.add(new int[] { 3145, 3450, 0 });  // Cooks' guild
        sCasual.extraAnchors.add(new int[] { 2933, 3287, 0 });  // Crafting guild
        sCasual.extraAnchors.add(new int[] { 3127, 3404, 0 });  // Air altar
        slots.add(sCasual);

        Slot sEff = new Slot("SKILLER_EFFICIENT", 80, 3651, 5117, 0, 14, true);
        sEff.extraAnchors.add(new int[] { 2929, 4824, 0 });    // Living rock caverns
        sEff.extraAnchors.add(new int[] { 2156, 3863, 0 });    // Astral altar
        sEff.extraAnchors.add(new int[] { 2841, 3434, 0 });    // Catherby fishing
        sEff.extraAnchors.add(new int[] { 3651, 5117, 0 });    // Living rock
        slots.add(sEff);

        // ===== Combatants (250 total, multi-anchored across training spots) =====
        Slot cPure = new Slot("COMBATANT_PURE",  100, 2670, 3712, 0, 14, true);
        cPure.extraAnchors.add(new int[] { 1779, 3469, 0 });   // Sand crabs Hosidius
        cPure.extraAnchors.add(new int[] { 3081, 3421, 0 });   // Stronghold of security
        cPure.extraAnchors.add(new int[] { 3168, 2981, 0 });   // Bandit camp
        cPure.extraAnchors.add(new int[] { 3428, 3537, 0 });   // Slayer tower
        slots.add(cPure);

        Slot cTank = new Slot("COMBATANT_TANK",   50, 2670, 3712, 0, 14, true);
        cTank.extraAnchors.add(new int[] { 1779, 3469, 0 });
        cTank.extraAnchors.add(new int[] { 3081, 3421, 0 });
        cTank.extraAnchors.add(new int[] { 3060, 9572, 0 });   // Frost dragons
        slots.add(cTank);

        Slot cHybrid = new Slot("COMBATANT_HYBRID", 100, 2670, 3712, 0, 14, true);
        cHybrid.extraAnchors.add(new int[] { 1779, 3469, 0 });
        cHybrid.extraAnchors.add(new int[] { 3081, 3421, 0 });
        cHybrid.extraAnchors.add(new int[] { 3168, 2981, 0 });
        cHybrid.extraAnchors.add(new int[] { 3428, 3537, 0 });
        cHybrid.extraAnchors.add(new int[] { 3060, 9572, 0 });
        slots.add(cHybrid);

        // ===== PK bots (50 total, ditch + mid-deep wildy spread) =====
        // LURE - 30 across the lvl 1-3 wildy strip north of Edge ditch.
        // 6 anchors so a batch of 30 spawns ~5 per anchor instead of
        // stacking on one tile.
        Slot pkLure = new Slot("COMBATANT_PKER_LURE", 30, 3088, 3525, 0, 8, true);
        pkLure.extraAnchors.add(new int[] { 3082, 3528, 0 });
        pkLure.extraAnchors.add(new int[] { 3094, 3530, 0 });
        pkLure.extraAnchors.add(new int[] { 3076, 3534, 0 });
        pkLure.extraAnchors.add(new int[] { 3100, 3535, 0 });
        pkLure.extraAnchors.add(new int[] { 3072, 3540, 0 });
        slots.add(pkLure);

        // HUNTER - 20 across mid-deep wildy. 8 anchors covering lvl 5-50
        // so they patrol the entire wilderness ladder from Bandit camp
        // through Lava maze and on to deep wildy.
        Slot pkHunter = new Slot("COMBATANT_PKER_HUNTER", 20, 3076, 3580, 0, 18, true);
        pkHunter.extraAnchors.add(new int[] { 3036, 3713, 0 });   // Bandit camp lvl ~13
        pkHunter.extraAnchors.add(new int[] { 3076, 3856, 0 });   // Lava maze lvl ~36
        pkHunter.extraAnchors.add(new int[] { 3105, 3954, 0 });   // Mage arena lvl ~50
        pkHunter.extraAnchors.add(new int[] { 3294, 3886, 0 });   // Demonic ruins lvl ~46
        pkHunter.extraAnchors.add(new int[] { 3186, 3933, 0 });   // Resource area
        pkHunter.extraAnchors.add(new int[] { 3041, 3949, 0 });   // Wildy bandits lvl ~50
        pkHunter.extraAnchors.add(new int[] { 3120, 3680, 0 });   // mid-wildy lvl ~25
        slots.add(pkHunter);

        // ===== GE socialites (small crowd for visual flavour) =====
        // Bankstand auto-picks one of 4 GE counters per spawn (socialiteAnchor).
        slots.add(new Slot("SOCIALITE_BANKSTAND", 4, 3164, 3486, 0, 12, true));
        slots.add(new Slot("SOCIALITE_GAMBLER",   4, 3142, 3487, 0, 10, true));

        // Per-minigame lobbies. Anchor is the lobby tile - CitizenSpawner
        // pins minigame archetypes to AmbientArchetype.lobbyTile() at spawn
        // time so anchor here is mostly informational. Default off so admins
        // toggle them per-test.
        slots.add(new Slot("MINIGAMER_CASTLEWARS_RUSHER",        4,  2442, 3090, 0, 6, false));
        slots.add(new Slot("MINIGAMER_CASTLEWARS_DEFENDER",      4,  2442, 3090, 0, 6, false));
        slots.add(new Slot("MINIGAMER_SOULWARS_RUSHER",          4,  2210, 3056, 0, 6, false));
        slots.add(new Slot("MINIGAMER_SOULWARS_DEFENDER",        4,  2210, 3056, 0, 6, false));
        slots.add(new Slot("MINIGAMER_STEALINGCREATION_RUSHER",  4,  2860, 5567, 0, 6, false));
        slots.add(new Slot("MINIGAMER_STEALINGCREATION_DEFENDER",4,  2860, 5567, 0, 6, false));
        slots.add(new Slot("MINIGAMER_RUSHER",                   4,  2440, 3093, 0, 8, false));
        slots.add(new Slot("MINIGAMER_DEFENDER",                 4,  2440, 3093, 0, 8, false));
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
        s.extraAnchors = parseExtraAnchors(obj);
        return s.archetype == null ? null : s;
    }

    /** Parse "extra_anchors":[[x,y,p],[x,y,p],...] from a slot JSON. */
    public static java.util.List<int[]> parseExtraAnchors(String obj) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        if (obj == null) return out;
        int key = obj.indexOf("\"extra_anchors\"");
        if (key < 0) return out;
        int colon = obj.indexOf(':', key);
        if (colon < 0) return out;
        int arrStart = obj.indexOf('[', colon);
        if (arrStart < 0) return out;
        // Find matching close-bracket for the OUTER array
        int depth = 0;
        int arrEnd = -1;
        for (int i = arrStart; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) { arrEnd = i; break; }
            }
        }
        if (arrEnd < 0) return out;
        String body = obj.substring(arrStart + 1, arrEnd);
        // Split on inner [...]
        int p = 0;
        while (p < body.length()) {
            int open = body.indexOf('[', p);
            if (open < 0) break;
            int close = body.indexOf(']', open);
            if (close < 0) break;
            String triple = body.substring(open + 1, close).trim();
            String[] parts = triple.split(",");
            try {
                if (parts.length >= 2) {
                    int xv = Integer.parseInt(parts[0].trim());
                    int yv = Integer.parseInt(parts[1].trim());
                    int pv = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 0;
                    out.add(new int[] { xv, yv, pv });
                }
            } catch (Throwable ignored) {}
            p = close + 1;
        }
        return out;
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
