package com.rs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Bidirectional retro / replica item-id mapping.
 *
 * Loaded from data/items/retro_swaps.json:
 *   { "<baseId>": <retroOrReplicaId>, ... }
 *
 * The cache stores the retro / replica look as a separate item id from
 * the base (e.g. 11696 "Bandos godsword" -> 31240 "Retro bandos godsword").
 * When a player has oldItemsLook = true, render-time encoders swap
 * base -> retro on the way out, and swap retro -> base on incoming
 * actions (so the player can still drop / use / equip the item without
 * the rest of the engine caring about which look they're seeing).
 *
 * Auto-populated by /admin/items/retro/autopopulate which runs the
 * same name-pair scan as the oldlook-scan endpoint and writes the
 * resulting (baseId -> variantId) map.
 */
public final class RetroSwaps {

    private static final String FILE_PATH = "data/items/retro_swaps.json";

    /** baseId -> retroId (forward). */
    private static final Map<Integer, Integer> BASE_TO_RETRO = new HashMap<>();
    /** retroId -> baseId (reverse, built from forward at load time). */
    private static final Map<Integer, Integer> RETRO_TO_BASE = new HashMap<>();
    private static volatile boolean loaded = false;

    private RetroSwaps() {}

    /** Return the retro id for this base id, or itemId if no mapping. */
    public static int toOld(int itemId) {
        load();
        Integer r = BASE_TO_RETRO.get(itemId);
        return r == null ? itemId : r;
    }

    /** Return the base id for this retro id, or itemId if no mapping. */
    public static int toNew(int itemId) {
        load();
        Integer r = RETRO_TO_BASE.get(itemId);
        return r == null ? itemId : r;
    }

    /** Render-time helper: given the player's oldItemsLook flag and an
     *  item id, return the id the client should see. */
    public static int forDisplay(boolean oldLook, int itemId) {
        if (itemId <= 0) return itemId;
        return oldLook ? toOld(itemId) : itemId;
    }

    public static synchronized void put(int baseId, int retroId) {
        BASE_TO_RETRO.put(baseId, retroId);
        RETRO_TO_BASE.put(retroId, baseId);
    }

    public static synchronized void clearAll() {
        BASE_TO_RETRO.clear();
        RETRO_TO_BASE.clear();
    }

    public static synchronized int size() {
        load();
        return BASE_TO_RETRO.size();
    }

    public static synchronized Map<Integer, Integer> snapshotBaseToRetro() {
        load();
        return new HashMap<>(BASE_TO_RETRO);
    }

    public static synchronized void reload() {
        loaded = false;
        BASE_TO_RETRO.clear();
        RETRO_TO_BASE.clear();
        load();
    }

    public static synchronized void save() {
        try {
            File f = new File(FILE_PATH);
            File dir = f.getParentFile();
            if (dir != null) dir.mkdirs();
            try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
                w.print("{");
                boolean first = true;
                for (Map.Entry<Integer, Integer> e : BASE_TO_RETRO.entrySet()) {
                    if (!first) w.print(",");
                    first = false;
                    w.print("\"" + e.getKey() + "\":" + e.getValue());
                }
                w.println("}");
            }
        } catch (Throwable t) {
            System.err.println("[RetroSwaps] save failed: " + t);
        }
    }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        File f = new File(FILE_PATH);
        if (!f.isFile()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            parse(sb.toString());
        } catch (Throwable t) {
            System.err.println("[RetroSwaps] load failed: " + t);
        }
    }

    private static void parse(String json) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "\"?(\\d+)\"?\\s*:\\s*(\\d+)").matcher(json);
        while (m.find()) {
            try {
                int baseId  = Integer.parseInt(m.group(1));
                int retroId = Integer.parseInt(m.group(2));
                BASE_TO_RETRO.put(baseId, retroId);
                RETRO_TO_BASE.put(retroId, baseId);
            } catch (Throwable ignored) {}
        }
    }
}
