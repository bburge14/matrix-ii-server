package com.rs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps "use dye on item" -> resulting recolored/redesigned item id.
 *
 * Loaded from data/items/dye_recolors.json. Format:
 *   {
 *     "<dyeItemId>": {
 *       "<sourceItemId>": <resultItemId>,
 *       ...
 *     },
 *     ...
 *   }
 *
 * Hooked into InventoryOptionsHandler.handleInterfaceOnInterface so
 * dragging a registered dye onto a registered source consumes both
 * and produces the result.
 *
 * Add new entries by editing the JSON file - no recompile needed.
 * The file is auto-created on first server start with a couple of
 * commented-out example entries for the common cape recolor pattern
 * so admins can see the schema.
 */
public final class DyeRecolors {

    private static final String FILE_PATH = "data/items/dye_recolors.json";

    /** dyeId -> (sourceId -> resultId) */
    private static final Map<Integer, Map<Integer, Integer>> MAP = new HashMap<>();
    private static boolean loaded = false;

    private DyeRecolors() {}

    /** Returns the result item id for (dye, source) or -1 if not registered. */
    public static synchronized int getResult(int dyeId, int sourceId) {
        load();
        Map<Integer, Integer> sub = MAP.get(dyeId);
        if (sub == null) return -1;
        Integer r = sub.get(sourceId);
        return r == null ? -1 : r;
    }

    /** True if either argument is a registered dye id (in either direction). */
    public static synchronized boolean isInvolved(int itemA, int itemB) {
        load();
        return MAP.containsKey(itemA) || MAP.containsKey(itemB);
    }

    /** Try to recolor: handles either order (dye-on-item, item-on-dye).
     *  Returns the result id on success, -1 on no-match. */
    public static synchronized int tryRecolor(int itemA, int itemB) {
        load();
        int r = getResult(itemA, itemB);
        if (r != -1) return r;
        return getResult(itemB, itemA);
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        File f = new File(FILE_PATH);
        if (!f.isFile()) {
            seedExample(f);
            return;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            parse(sb.toString());
        } catch (Throwable t) {
            System.err.println("[DyeRecolors] load failed: " + t);
        }
    }

    private static void parse(String json) {
        // Very lightweight parser - top-level {...} keyed by dye id with
        // nested {...} of sourceId:resultId pairs. Tolerates whitespace
        // and either quoted or unquoted ids.
        java.util.regex.Pattern outerP = java.util.regex.Pattern.compile(
            "\"?(\\d+)\"?\\s*:\\s*\\{([^}]*)\\}");
        java.util.regex.Pattern innerP = java.util.regex.Pattern.compile(
            "\"?(\\d+)\"?\\s*:\\s*\"?(\\d+)\"?");
        java.util.regex.Matcher om = outerP.matcher(json);
        while (om.find()) {
            int dye = Integer.parseInt(om.group(1));
            Map<Integer, Integer> sub = MAP.computeIfAbsent(
                dye, k -> new LinkedHashMap<>());
            java.util.regex.Matcher im = innerP.matcher(om.group(2));
            while (im.find()) {
                sub.put(Integer.parseInt(im.group(1)),
                        Integer.parseInt(im.group(2)));
            }
        }
    }

    /** Write a stub file so admins can see the schema. Uses comment-style
     *  keys (negative ids) that won't actually match anything in-game. */
    private static void seedExample(File f) {
        try {
            File dir = f.getParentFile();
            if (dir != null) dir.mkdirs();
            try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
                w.println("{");
                w.println("  \"_comment\": \"Format: dyeItemId -> {sourceItemId: resultItemId}. Edit then restart server.\",");
                w.println("  \"_example\": {");
                w.println("    \"_doc\": \"using red dye (1763) on item X produces item Y\",");
                w.println("    \"-1\": -1");
                w.println("  },");
                w.println("  \"1763\": {},");
                w.println("  \"1765\": {},");
                w.println("  \"1767\": {},");
                w.println("  \"1769\": {},");
                w.println("  \"1771\": {},");
                w.println("  \"1773\": {}");
                w.println("}");
            }
            System.out.println("[DyeRecolors] seeded empty " + FILE_PATH
                + " - populate it to wire up dyes");
        } catch (Throwable t) {
            System.err.println("[DyeRecolors] seed failed: " + t);
        }
    }

    /** Reload from disk (admin endpoint can call this). */
    public static synchronized void reload() {
        loaded = false;
        MAP.clear();
        load();
    }

    public static synchronized int registeredDyeCount() {
        load();
        return MAP.size();
    }
}
