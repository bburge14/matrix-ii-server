package com.rs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Runtime tradeable-flag overrides. ItemConstants.isTradeable normally
 * pulls from Settings.TRADEABLE_EXCEPTION (a hardcoded array). This
 * class layers a mutable Set on top so admins can flip an item to
 * tradeable / untradeable from the admin panel without recompiling
 * Settings.java.
 *
 * Persists to data/items/tradeable_overrides.json:
 *   {"tradeable":[id,id,...], "untradeable":[id,id,...]}
 *
 * tradeable    = forced TRUE (overrides destroy/lended/charges/etc.)
 * untradeable  = forced FALSE
 * not in either set = ItemConstants falls through to its default rules
 */
public final class TradeableOverrides {

    private static final String FILE_PATH = "data/items/tradeable_overrides.json";

    private static final Set<Integer> tradeable   = new HashSet<>();
    private static final Set<Integer> untradeable = new HashSet<>();
    private static boolean loaded = false;

    private TradeableOverrides() {}

    public static synchronized boolean isForcedTradeable(int id) {
        load();
        return tradeable.contains(id);
    }

    public static synchronized boolean isForcedUntradeable(int id) {
        load();
        return untradeable.contains(id);
    }

    public static synchronized void setTradeable(int id, boolean t) {
        load();
        if (t) {
            tradeable.add(id);
            untradeable.remove(id);
        } else {
            untradeable.add(id);
            tradeable.remove(id);
        }
        save();
    }

    public static synchronized void clear(int id) {
        load();
        tradeable.remove(id);
        untradeable.remove(id);
        save();
    }

    public static synchronized Set<Integer> getTradeable() {
        load();
        return Collections.unmodifiableSet(new TreeSet<>(tradeable));
    }

    public static synchronized Set<Integer> getUntradeable() {
        load();
        return Collections.unmodifiableSet(new TreeSet<>(untradeable));
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        File f = new File(FILE_PATH);
        if (!f.isFile()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            String json = sb.toString();
            parseInto(json, "tradeable",   tradeable);
            parseInto(json, "untradeable", untradeable);
        } catch (Throwable t) {
            System.err.println("[TradeableOverrides] load failed: " + t);
        }
    }

    private static void parseInto(String json, String key, Set<Integer> dest) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return;
        int lb = json.indexOf('[', i);
        int rb = json.indexOf(']', lb < 0 ? 0 : lb);
        if (lb < 0 || rb < 0) return;
        for (String part : json.substring(lb + 1, rb).split(",")) {
            try { dest.add(Integer.parseInt(part.trim())); }
            catch (Throwable ignored) {}
        }
    }

    private static void save() {
        try {
            File f = new File(FILE_PATH);
            File dir = f.getParentFile();
            if (dir != null) dir.mkdirs();
            try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
                w.print("{\"tradeable\":[");
                writeIntList(w, tradeable);
                w.print("],\"untradeable\":[");
                writeIntList(w, untradeable);
                w.println("]}");
            }
        } catch (Throwable t) {
            System.err.println("[TradeableOverrides] save failed: " + t);
        }
    }

    private static void writeIntList(PrintWriter w, Set<Integer> set) {
        boolean first = true;
        for (int id : new TreeSet<>(set)) {
            if (!first) w.print(",");
            first = false;
            w.print(id);
        }
    }
}
