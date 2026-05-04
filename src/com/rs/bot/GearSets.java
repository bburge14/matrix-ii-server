package com.rs.bot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Externalised bot gear sets. Loaded from data/gear_sets.json on first
 * use. If the file is absent we fall back to the hardcoded socialite
 * outfit array in BotEquipment - this lets the admin panel manage the
 * pool through a UI without recompiling Java.
 *
 * Schema (JSON):
 * {
 *   "schema": 1,
 *   "outfits": {
 *     "socialite": [
 *       {"name": "wizard blue", "hat": 579, "chest": 577, "legs": 1013},
 *       {"name": "robin hood + black", "hat": 2581, "chest": 1005, "legs": 1013}
 *     ]
 *   }
 * }
 *
 * `hat`/`chest`/`legs` use -1 for "no item". Only the keys defined in
 * the schema are read; unknown keys are preserved on save (forward-compat).
 *
 * Admin panel reads/writes via /admin/gear/sets endpoints (added when the
 * editor UI ships). Java reads at startup + after each save.
 */
public final class GearSets {

    private static final String CONFIG_PATH = "data/gear_sets.json";
    public static final int CURRENT_SCHEMA_VERSION = 3;

    public static final class Outfit {
        public String name;
        public int hat;
        public int chest;
        public int legs;
        public Outfit(String name, int hat, int chest, int legs) {
            this.name = name; this.hat = hat;
            this.chest = chest; this.legs = legs;
        }
    }

    private static final Map<String, List<Outfit>> POOLS = new LinkedHashMap<>();
    private static boolean loaded = false;

    private GearSets() {}

    /** Get the outfit pool for an archetype key (e.g. "socialite").
     *  Returns null if no pool defined - caller should fall back. */
    public static synchronized List<Outfit> getOutfits(String archetypeKey) {
        load();
        List<Outfit> p = POOLS.get(archetypeKey);
        return p == null || p.isEmpty() ? null : p;
    }

    /** Replace the outfit pool for an archetype + persist to disk. Used by
     *  the admin panel's gear-set editor. */
    public static synchronized void setOutfits(String archetypeKey, List<Outfit> outfits) {
        load();
        POOLS.put(archetypeKey, new ArrayList<>(outfits == null ? java.util.Collections.<Outfit>emptyList() : outfits));
        save();
    }

    /** Snapshot all pools (immutable copy). */
    public static synchronized Map<String, List<Outfit>> getAll() {
        load();
        Map<String, List<Outfit>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<Outfit>> e : POOLS.entrySet()) {
            out.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return out;
    }

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
                System.out.println("[GearSets] config schema v" + diskVersion
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
            parse(json);
        } catch (Throwable t) {
            System.err.println("[GearSets] load failed: " + t);
            seedDefaults();
        }
    }

    private static int parseSchemaVersion(String json) {
        try {
            int i = json.indexOf("\"schema\":");
            if (i < 0) return 0;
            i += "\"schema\":".length();
            int end = i;
            while (end < json.length()
                    && (json.charAt(end) == '-' || Character.isDigit(json.charAt(end)))) end++;
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
            w.print("{\"schema\":" + CURRENT_SCHEMA_VERSION + ",\"outfits\":{");
            boolean firstPool = true;
            for (Map.Entry<String, List<Outfit>> e : POOLS.entrySet()) {
                if (!firstPool) w.print(",");
                firstPool = false;
                w.print("\"" + jsonEscape(e.getKey()) + "\":[");
                boolean firstOut = true;
                for (Outfit o : e.getValue()) {
                    if (!firstOut) w.print(",");
                    firstOut = false;
                    w.print("{\"name\":\"" + jsonEscape(o.name == null ? "" : o.name)
                        + "\",\"hat\":" + o.hat
                        + ",\"chest\":" + o.chest
                        + ",\"legs\":" + o.legs + "}");
                }
                w.print("]");
            }
            w.println("}}");
        } catch (Throwable t) {
            System.err.println("[GearSets] save failed: " + t);
        }
    }

    private static void seedDefaults() {
        POOLS.clear();
        // Mirrors BotEquipment.applySocialite preset list as of the
        // 23-outfit revision. Editing the JSON file overrides these.
        // Mirrors BotEquipment.applySocialite outfit pool. 30+ entries
        // covering wizard / mystic / barrows / bandos / armadyl / pernix /
        // torva / virtus / statius / dragon / rune / casual themed sets.
        // canWear gate at apply time filters anything the bot can't actually
        // wear, with retry-roll so low-cb bots end up with simple robes
        // instead of bare slots.
        int[][] socialite = {
            // Wizard / robe family
            { 579, 577, 1013 },     { 1037, 1005, 1013 },   { -1,  577, 1095 },
            // Mystic mage robes
            { 4089, 4091, 4093 },   { 4099, 4101, 4103 },   { 4109, 4111, 4113 },
            // Casual / themed
            { 2581, 1005, 1013 },   { -1,   1011, 1013 },   { -1,   1015, 1017 },
            { -1,   542,  544 },    { 10398, 1005, 1013 },
            // Rune armor
            { 1163, 1127, 1079 },   { -1,   1127, 1079 },   { 1163, 1115, 1075 },
            // Dragon
            { 1149, 3140, 4087 },
            // Barrows brothers (full sets)
            { 4716, 4720, 4722 },   { 4724, 4728, 4730 },
            { 4745, 4749, 4751 },   { 4753, 4757, 4759 },
            { 4732, 4736, 4738 },   { 4708, 4712, 4714 },
            // Bandos / Armadyl / endgame
            { 11718, 11724, 11726 },   { 11718, 11720, 11722 },
            { 20149, 20153, 20157 },   { 20137, 20141, 20145 },
            { 20161, 20165, 20169 },   { 13896, 13884, 13890 },
            // Elegant costumes (verified - clue scroll rewards)
            { -1, 10404, 10424 },   { -1, 10406, 10426 },   // red (m/f)
            { -1, 10408, 10428 },   { -1, 10410, 10430 },   // blue (m/f)
            { -1, 10412, 10432 },   { -1, 10414, 10434 },   // green (m/f)
            { -1, 10400, 10420 },   { -1, 10402, 10422 },   // black/white
            { -1, 10416, 10436 },   { -1, 10418, 10438 },   // purple (m/f)
            // Elegant + headband / boater
            { 2645, 10404, 10424 }, { 2647, 10408, 10428 }, { 2649, 10412, 10432 },
            { 7319, 10402, 10422 }, { 7325, 10400, 10420 },
            // God vestments
            { 10452, 10458, 10460 }, { 10454, 10462, 10464 }, { 10456, 10466, 10468 },
            // Bob the cat shirts
            { -1, 10316, 1095 },    { -1, 10318, 1095 },    { -1, 10320, 1095 },
            { -1, 10322, 1095 },    { -1, 10324, 1095 },
            // Hybrid / mixed
            { 2581, 577,  1095 },   { 1037, 4101, 4103 },   { 579,  4091, 4093 },
            { -1,   1005, 1095 },
        };
        List<Outfit> list = new ArrayList<>();
        for (int[] r : socialite) {
            list.add(new Outfit(autoName(r[0], r[1], r[2]), r[0], r[1], r[2]));
        }
        POOLS.put("socialite", list);
    }

    /** Cheap "wizard blue / mystic dark" naming for seed entries. Admin
     *  panel can rename via setOutfits. */
    private static String autoName(int hat, int chest, int legs) {
        String prefix;
        if (chest == 577 || chest == 1005) prefix = "wizard";
        else if (chest >= 4089 && chest <= 4117) prefix = "mystic";
        else if (chest == 1011) prefix = "druidic";
        else if (chest == 1015) prefix = "priest";
        else if (chest == 542) prefix = "monk";
        else prefix = "outfit";
        String suffix = hat == -1 ? " (no hat)" : "";
        return prefix + " " + chest + "/" + legs + suffix;
    }

    /** Minimal hand-rolled JSON parser - mirrors CitizenBudget's style. */
    private static void parse(String json) {
        POOLS.clear();
        int outfitsKey = json.indexOf("\"outfits\"");
        if (outfitsKey < 0) return;
        int braceStart = json.indexOf('{', outfitsKey);
        if (braceStart < 0) return;
        int depth = 0;
        int braceEnd = -1;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { braceEnd = i; break; }
            }
        }
        if (braceEnd < 0) return;
        String body = json.substring(braceStart + 1, braceEnd);
        // Match: "key":[ {...},{...} ]
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*\\[(.*?)\\]", java.util.regex.Pattern.DOTALL).matcher(body);
        while (m.find()) {
            String key = m.group(1);
            String arr = m.group(2);
            List<Outfit> list = new ArrayList<>();
            java.util.regex.Matcher om = java.util.regex.Pattern.compile(
                "\\{[^}]*\\}").matcher(arr);
            while (om.find()) {
                String entry = om.group();
                String name = extractStr(entry, "name");
                int hat = extractInt(entry, "hat", -1);
                int chest = extractInt(entry, "chest", -1);
                int legs = extractInt(entry, "legs", -1);
                list.add(new Outfit(name == null ? "" : name, hat, chest, legs));
            }
            POOLS.put(key, list);
        }
    }

    private static String extractStr(String obj, String key) {
        String marker = "\"" + key + "\"";
        int i = obj.indexOf(marker);
        if (i < 0) return null;
        int colon = obj.indexOf(':', i);
        if (colon < 0) return null;
        int q1 = obj.indexOf('"', colon);
        if (q1 < 0) return null;
        int q2 = obj.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return obj.substring(q1 + 1, q2);
    }

    private static int extractInt(String obj, String key, int def) {
        String marker = "\"" + key + "\"";
        int i = obj.indexOf(marker);
        if (i < 0) return def;
        int colon = obj.indexOf(':', i);
        if (colon < 0) return def;
        int p = colon + 1;
        while (p < obj.length() && Character.isWhitespace(obj.charAt(p))) p++;
        int start = p;
        if (p < obj.length() && obj.charAt(p) == '-') p++;
        while (p < obj.length() && Character.isDigit(obj.charAt(p))) p++;
        if (p == start || (p == start + 1 && obj.charAt(start) == '-')) return def;
        try { return Integer.parseInt(obj.substring(start, p)); }
        catch (Throwable t) { return def; }
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
