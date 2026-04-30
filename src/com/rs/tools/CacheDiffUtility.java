package com.rs.tools;

import com.alex.store.Index;
import com.alex.store.Store;
import com.rs.cache.Cache;
import com.rs.cache.loaders.ItemDefinitions;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.cache.loaders.ObjectDefinitions;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Cache delta dumper. Compares the 876 base cache against a 900+ "DLC" cache
 * and writes the difference to data/dump/cache_comparison.txt.
 *
 * Usage:
 *   java -cp bin:data/libs/* com.rs.tools.CacheDiffUtility \
 *        ~/matrix/876_cache/cache/ \
 *        ~/caches/
 *
 * Optional args (positional):
 *   args[0] = primary path (default ~/matrix/876_cache/cache/)
 *   args[1] = dlc path     (default ~/caches/)
 *   args[2] = output file  (default data/dump/cache_comparison.txt)
 *
 * What it produces:
 *   [Items / Objects / NPCs - New Assets]   (in DLC but null/empty in primary)
 *   [Items / Objects / NPCs - ID Shifts]    (same name, different ID)
 *   [Items / Objects / NPCs - Name Changes] (same ID, different name)
 *   [Verification Map]                       (War's Retreat objects, Master Cape items)
 *   [Incompatible Opcodes]                   (IDs that crashed during decode)
 *
 * Why we swap Cache.STORE: the production parsers (ItemDefinitions etc.)
 * read directly from the global Cache.STORE singleton, so we point STORE
 * at the primary store, decode every ID, snapshot the names, then swap to
 * the DLC store and repeat. clear*Definitions() between swaps wipes the
 * static cached defs. Single-threaded utility - thread safety not a concern.
 *
 * Each per-ID decode is wrapped in a Throwable catch so a single bad opcode
 * (often the case when 900+ adds a new opcode unknown to the 876-era parser)
 * gets logged as "Incompatible Opcode" instead of aborting the dump.
 */
public final class CacheDiffUtility {

    /** Cache index numbers as used in this codebase (NOT the alex/store library
     *  defaults - check ItemDefinitions/ObjectDefinitions/NPCDefinitions). */
    private static final int OBJECT_INDEX = 16;
    private static final int NPC_INDEX = 18;
    private static final int ITEM_INDEX = 19;

    /** Hard upper bound to stop runaway loops when the cache appears empty. */
    private static final int ITEM_HARD_MAX = 60000;
    private static final int OBJECT_HARD_MAX = 150000;
    private static final int NPC_HARD_MAX = 35000;

    public static void main(String[] args) throws Exception {
        String home = System.getProperty("user.home");
        // Default paths match the user's actual VM layout:
        //   ~/matrix/Server/    = repo root
        //   ~/matrix/876_cache/ = primary (no /cache/ subdir)
        //   ~/830_cache/        = older base, useful as the "primary" if you
        //                         want to dump everything that arrived in 876
        //   ~/caches/           = 900+ DLC cache (when available)
        // Pass paths as args to override.
        String primaryPath = args.length > 0 ? args[0] : home + "/matrix/876_cache/";
        String dlcPath     = args.length > 1 ? args[1] : home + "/caches/";
        String outPath     = args.length > 2 ? args[2] : "data/dump/cache_comparison.txt";

        System.out.println("[CacheDiff] primary = " + primaryPath);
        System.out.println("[CacheDiff] dlc     = " + dlcPath);
        System.out.println("[CacheDiff] output  = " + outPath);

        primaryPath = resolveCachePath(primaryPath, "primary");
        dlcPath     = resolveCachePath(dlcPath, "dlc");

        Store primary = new Store(primaryPath);
        Store dlc     = new Store(dlcPath);

        // Sanity-check the Store actually loaded indexes. If the .idx files
        // weren't where we pointed, getIndexes() comes back length 0 and the
        // first index access blows up with ArrayIndexOutOfBounds. Catch it here.
        if (primary.getIndexes() == null || primary.getIndexes().length == 0) {
            System.err.println("[CacheDiff] primary store loaded 0 indexes from " + primaryPath);
            System.err.println("  Path must contain main_file_cache.dat2 + main_file_cache.idx* files directly.");
            System.exit(1);
        }
        if (dlc.getIndexes() == null || dlc.getIndexes().length == 0) {
            System.err.println("[CacheDiff] dlc store loaded 0 indexes from " + dlcPath);
            System.err.println("  Path must contain main_file_cache.dat2 + main_file_cache.idx* files directly.");
            System.exit(1);
        }
        System.out.println("[CacheDiff] primary indexes loaded: " + primary.getIndexes().length);
        System.out.println("[CacheDiff] dlc     indexes loaded: " + dlc.getIndexes().length);

        // Build per-type ID lists by walking each store's index and collecting
        // every (archive, file) pair. This avoids guessing max IDs and is
        // robust against sparse archives.
        Set<Integer> itemIdsP   = collectItemIds(primary);
        Set<Integer> itemIdsD   = collectItemIds(dlc);
        Set<Integer> objectIdsP = collectObjectIds(primary);
        Set<Integer> objectIdsD = collectObjectIds(dlc);
        Set<Integer> npcIdsP    = collectNpcIds(primary);
        Set<Integer> npcIdsD    = collectNpcIds(dlc);

        System.out.println("[CacheDiff] items:   primary=" + itemIdsP.size() + " dlc=" + itemIdsD.size());
        System.out.println("[CacheDiff] objects: primary=" + objectIdsP.size() + " dlc=" + objectIdsD.size());
        System.out.println("[CacheDiff] npcs:    primary=" + npcIdsP.size() + " dlc=" + npcIdsD.size());

        // Decode names for every ID in both stores. Returns id->name (or
        // sentinel "<incompatible>" / null for missing/broken).
        List<String> incompat = new ArrayList<>();
        Map<Integer, String> primaryItemNames = decodeAllItemNames(primary, itemIdsP, "primary", incompat);
        Map<Integer, String> dlcItemNames     = decodeAllItemNames(dlc, itemIdsD, "dlc", incompat);
        Map<Integer, String> primaryObjNames  = decodeAllObjectNames(primary, objectIdsP, "primary", incompat);
        Map<Integer, String> dlcObjNames      = decodeAllObjectNames(dlc, objectIdsD, "dlc", incompat);
        Map<Integer, String> primaryNpcNames  = decodeAllNpcNames(primary, npcIdsP, "primary", incompat);
        Map<Integer, String> dlcNpcNames      = decodeAllNpcNames(dlc, npcIdsD, "dlc", incompat);

        File out = new File(outPath);
        if (out.getParentFile() != null) out.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(out)) {
            pw.println("# Cache Comparison Report");
            pw.println("# primary = " + primaryPath);
            pw.println("# dlc     = " + dlcPath);
            pw.println("# generated " + new java.util.Date());
            pw.println();
            pw.println("# Counts");
            pw.println("items:   primary=" + primaryItemNames.size() + "  dlc=" + dlcItemNames.size());
            pw.println("objects: primary=" + primaryObjNames.size()  + "  dlc=" + dlcObjNames.size());
            pw.println("npcs:    primary=" + primaryNpcNames.size()  + "  dlc=" + dlcNpcNames.size());
            pw.println();

            writeNewAssets(pw, "Items",   primaryItemNames, dlcItemNames);
            writeNewAssets(pw, "Objects", primaryObjNames,  dlcObjNames);
            writeNewAssets(pw, "NPCs",    primaryNpcNames,  dlcNpcNames);

            writeIdShifts(pw, "Items",   primaryItemNames, dlcItemNames);
            writeIdShifts(pw, "Objects", primaryObjNames,  dlcObjNames);
            writeIdShifts(pw, "NPCs",    primaryNpcNames,  dlcNpcNames);

            writeNameChanges(pw, "Items",   primaryItemNames, dlcItemNames);
            writeNameChanges(pw, "Objects", primaryObjNames,  dlcObjNames);
            writeNameChanges(pw, "NPCs",    primaryNpcNames,  dlcNpcNames);

            // Verification map: explicit search for the user's reference assets.
            pw.println("[Verification Map]");
            pw.println("# These assets establish the DLC offset baseline.");
            pw.println();
            pw.println("## War's Retreat objects");
            dumpMatching(pw, "primary", primaryObjNames, "war's retreat", "wars retreat", "war retreat");
            dumpMatching(pw, "dlc    ", dlcObjNames,     "war's retreat", "wars retreat", "war retreat");
            pw.println();
            pw.println("## Master Cape items");
            dumpMatching(pw, "primary", primaryItemNames, "master cape", "master skillcape");
            dumpMatching(pw, "dlc    ", dlcItemNames,     "master cape", "master skillcape");
            pw.println();
            pw.println("## Other useful 'master' items (sanity check tier shift)");
            dumpMatching(pw, "primary", primaryItemNames, "max cape", "completionist");
            dumpMatching(pw, "dlc    ", dlcItemNames,     "max cape", "completionist");
            pw.println();

            pw.println("[Incompatible Opcodes]");
            pw.println("# IDs that threw during decode - usually means the 900+ cache uses");
            pw.println("# an opcode our 876-era parser doesn't know. Treat as 'needs parser update'.");
            for (String row : incompat) pw.println(row);
            pw.println();

            pw.println("# end of report");
        }

        System.out.println("[CacheDiff] wrote " + out.getAbsolutePath());
        System.out.println("[CacheDiff] incompatible IDs: " + incompat.size());
    }

    /**
     * Find the directory that actually contains the .dat2/.idx files.
     * Accepts: "/foo/" (flat) or "/foo" (with /cache/ subdir or /cache subdir).
     * Returns the path Store should be given, or exits if nothing matches.
     */
    private static String resolveCachePath(String path, String label) {
        File root = new File(path);
        if (!root.exists()) {
            System.err.println("[CacheDiff] " + label + " path does not exist: " + path);
            System.exit(1);
        }
        // Already pointing at the cache files directly?
        if (new File(root, "main_file_cache.dat2").exists()) return appendSlash(path);
        // Common nested layout: <path>/cache/main_file_cache.dat2
        File nested = new File(root, "cache");
        if (new File(nested, "main_file_cache.dat2").exists()) {
            String resolved = appendSlash(nested.getAbsolutePath());
            System.out.println("[CacheDiff] " + label + " resolved to nested cache dir: " + resolved);
            return resolved;
        }
        System.err.println("[CacheDiff] " + label + " path has no main_file_cache.dat2: " + path);
        System.err.println("  Looked for: " + path + "main_file_cache.dat2");
        System.err.println("        and:  " + path + "cache/main_file_cache.dat2");
        System.exit(1);
        return null;
    }

    private static String appendSlash(String p) {
        return p.endsWith("/") ? p : p + "/";
    }

    // === ID enumeration ===

    private static Set<Integer> collectItemIds(Store store) {
        Set<Integer> ids = new HashSet<>();
        Index ix = store.getIndexes()[ITEM_INDEX];
        if (ix == null) return ids;
        int lastArch = safeLastArchive(ix);
        for (int a = 0; a <= lastArch; a++) {
            if (!safeArchiveExists(ix, a)) continue;
            int lastFile = safeLastFile(ix, a);
            for (int f = 0; f <= lastFile && f < 256; f++) {
                if (!safeFileExists(ix, a, f)) continue;
                int id = (a << 8) | (f & 0xff);
                if (id < ITEM_HARD_MAX) ids.add(id);
            }
        }
        return ids;
    }

    private static Set<Integer> collectObjectIds(Store store) {
        Set<Integer> ids = new HashSet<>();
        Index ix = store.getIndexes()[OBJECT_INDEX];
        if (ix == null) return ids;
        int lastArch = safeLastArchive(ix);
        for (int a = 0; a <= lastArch; a++) {
            if (!safeArchiveExists(ix, a)) continue;
            int lastFile = safeLastFile(ix, a);
            for (int f = 0; f <= lastFile && f < 256; f++) {
                if (!safeFileExists(ix, a, f)) continue;
                int id = (a << 8) | (f & 0xff);
                if (id < OBJECT_HARD_MAX) ids.add(id);
            }
        }
        return ids;
    }

    private static Set<Integer> collectNpcIds(Store store) {
        Set<Integer> ids = new HashSet<>();
        Index ix = store.getIndexes()[NPC_INDEX];
        if (ix == null) return ids;
        int lastArch = safeLastArchive(ix);
        for (int a = 0; a <= lastArch; a++) {
            if (!safeArchiveExists(ix, a)) continue;
            int lastFile = safeLastFile(ix, a);
            // NPCs use 7-bit file id (0x7f mask) per NPCDefinitions:96
            for (int f = 0; f <= lastFile && f < 128; f++) {
                if (!safeFileExists(ix, a, f)) continue;
                int id = (a << 7) | (f & 0x7f);
                if (id < NPC_HARD_MAX) ids.add(id);
            }
        }
        return ids;
    }

    // === Name decoding ===

    private static Map<Integer, String> decodeAllItemNames(Store store, Set<Integer> ids,
                                                            String label, List<String> incompat) {
        Map<Integer, String> out = new HashMap<>();
        Cache.STORE = store;
        ItemDefinitions.clearItemsDefinitions();
        int decoded = 0, errors = 0;
        for (int id : ids) {
            try {
                ItemDefinitions def = ItemDefinitions.getItemDefinitions(id);
                if (def != null && def.name != null && !def.name.isEmpty() && !def.name.equals("null")) {
                    out.put(id, def.name);
                    decoded++;
                }
            } catch (Throwable t) {
                errors++;
                incompat.add("[" + label + "/item/" + id + "] " + t.getClass().getSimpleName()
                    + ": " + safeMsg(t));
            }
        }
        ItemDefinitions.clearItemsDefinitions();
        System.out.println("[CacheDiff] " + label + " items decoded=" + decoded + " errors=" + errors);
        return out;
    }

    private static Map<Integer, String> decodeAllObjectNames(Store store, Set<Integer> ids,
                                                              String label, List<String> incompat) {
        Map<Integer, String> out = new HashMap<>();
        Cache.STORE = store;
        ObjectDefinitions.clearObjectDefinitions();
        int decoded = 0, errors = 0;
        for (int id : ids) {
            try {
                ObjectDefinitions def = ObjectDefinitions.getObjectDefinitions(id);
                if (def != null && def.name != null && !def.name.isEmpty() && !def.name.equals("null")) {
                    out.put(id, def.name);
                    decoded++;
                }
            } catch (Throwable t) {
                errors++;
                incompat.add("[" + label + "/object/" + id + "] " + t.getClass().getSimpleName()
                    + ": " + safeMsg(t));
            }
        }
        ObjectDefinitions.clearObjectDefinitions();
        System.out.println("[CacheDiff] " + label + " objects decoded=" + decoded + " errors=" + errors);
        return out;
    }

    private static Map<Integer, String> decodeAllNpcNames(Store store, Set<Integer> ids,
                                                           String label, List<String> incompat) {
        Map<Integer, String> out = new HashMap<>();
        Cache.STORE = store;
        NPCDefinitions.clearNPCDefinitions();
        int decoded = 0, errors = 0;
        for (int id : ids) {
            try {
                NPCDefinitions def = NPCDefinitions.getNPCDefinitions(id);
                if (def != null && def.name != null && !def.name.isEmpty() && !def.name.equals("null")) {
                    out.put(id, def.name);
                    decoded++;
                }
            } catch (Throwable t) {
                errors++;
                incompat.add("[" + label + "/npc/" + id + "] " + t.getClass().getSimpleName()
                    + ": " + safeMsg(t));
            }
        }
        NPCDefinitions.clearNPCDefinitions();
        System.out.println("[CacheDiff] " + label + " npcs decoded=" + decoded + " errors=" + errors);
        return out;
    }

    // === Report sections ===

    private static void writeNewAssets(PrintWriter pw, String label,
                                        Map<Integer, String> primary, Map<Integer, String> dlc) {
        pw.println("[" + label + " - New Assets]");
        pw.println("# IDs that exist in DLC but are missing/empty in primary.");
        TreeMap<Integer, String> sorted = new TreeMap<>();
        for (Map.Entry<Integer, String> e : dlc.entrySet()) {
            if (!primary.containsKey(e.getKey())) sorted.put(e.getKey(), e.getValue());
        }
        for (Map.Entry<Integer, String> e : sorted.entrySet()) {
            pw.println(e.getKey() + " = " + e.getValue());
        }
        pw.println("# total: " + sorted.size());
        pw.println();
    }

    private static void writeIdShifts(PrintWriter pw, String label,
                                       Map<Integer, String> primary, Map<Integer, String> dlc) {
        pw.println("[" + label + " - ID Shifts]");
        pw.println("# Same name appears in both stores with a DIFFERENT id.");
        pw.println("# Format: 'name' | primary_ids -> dlc_ids");

        // Build name -> [ids] for each side
        Map<String, List<Integer>> byNameP = invert(primary);
        Map<String, List<Integer>> byNameD = invert(dlc);

        TreeMap<String, String> shifts = new TreeMap<>();
        for (Map.Entry<String, List<Integer>> e : byNameP.entrySet()) {
            String name = e.getKey();
            List<Integer> pIds = e.getValue();
            List<Integer> dIds = byNameD.get(name);
            if (dIds == null || dIds.isEmpty()) continue;
            // Identify shifts: if no ID is shared between the two sides we treat
            // as a full shift. If the sets only overlap partially we still flag.
            Set<Integer> overlap = new HashSet<>(pIds);
            overlap.retainAll(dIds);
            if (overlap.size() == pIds.size() && overlap.size() == dIds.size()) continue; // identical
            shifts.put(name, sortedJoin(pIds) + " -> " + sortedJoin(dIds));
        }
        for (Map.Entry<String, String> e : shifts.entrySet()) {
            pw.println("'" + e.getKey() + "' | " + e.getValue());
        }
        pw.println("# total: " + shifts.size());
        pw.println();
    }

    private static void writeNameChanges(PrintWriter pw, String label,
                                          Map<Integer, String> primary, Map<Integer, String> dlc) {
        pw.println("[" + label + " - Name Changes]");
        pw.println("# Same id, name differs between stores (rare but flags renames in place).");
        TreeMap<Integer, String> changes = new TreeMap<>();
        for (Map.Entry<Integer, String> e : primary.entrySet()) {
            String d = dlc.get(e.getKey());
            if (d != null && !d.equalsIgnoreCase(e.getValue())) {
                changes.put(e.getKey(), e.getValue() + " -> " + d);
            }
        }
        for (Map.Entry<Integer, String> e : changes.entrySet()) {
            pw.println(e.getKey() + ": " + e.getValue());
        }
        pw.println("# total: " + changes.size());
        pw.println();
    }

    private static void dumpMatching(PrintWriter pw, String label, Map<Integer, String> map,
                                      String... needleAny) {
        TreeMap<Integer, String> hits = new TreeMap<>();
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            String lower = e.getValue().toLowerCase();
            for (String n : needleAny) {
                if (lower.contains(n)) { hits.put(e.getKey(), e.getValue()); break; }
            }
        }
        if (hits.isEmpty()) {
            pw.println(label + ": (no matches for " + Arrays.toString(needleAny) + ")");
            return;
        }
        for (Map.Entry<Integer, String> e : hits.entrySet()) {
            pw.println(label + ": " + e.getKey() + " = " + e.getValue());
        }
    }

    // === Helpers ===

    private static Map<String, List<Integer>> invert(Map<Integer, String> m) {
        Map<String, List<Integer>> out = new HashMap<>();
        for (Map.Entry<Integer, String> e : m.entrySet()) {
            String key = e.getValue().toLowerCase();
            out.computeIfAbsent(key, k -> new ArrayList<>()).add(e.getKey());
        }
        for (List<Integer> l : out.values()) l.sort(Comparator.naturalOrder());
        return out;
    }

    private static String sortedJoin(List<Integer> ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        return sb.append("]").toString();
    }

    private static int safeLastArchive(Index ix) {
        try { return ix.getLastArchiveId(); } catch (Throwable t) { return -1; }
    }

    private static int safeLastFile(Index ix, int archive) {
        try { return ix.getLastFileId(archive); } catch (Throwable t) { return -1; }
    }

    private static boolean safeArchiveExists(Index ix, int archive) {
        try { return ix.archiveExists(archive); } catch (Throwable t) { return false; }
    }

    private static boolean safeFileExists(Index ix, int archive, int file) {
        try { return ix.fileExists(archive, file); } catch (Throwable t) { return false; }
    }

    private static String safeMsg(Throwable t) {
        String m = t.getMessage();
        return m == null ? "(no message)" : m;
    }

    private CacheDiffUtility() {}
}
