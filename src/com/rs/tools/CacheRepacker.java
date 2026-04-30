package com.rs.tools;

import com.alex.store.Index;
import com.alex.store.Store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Converts an OpenRS2 "loose-file" cache into the packed
 * main_file_cache.dat2 + main_file_cache.idx* format that the alex/store
 * FileStore library (and the running Matrix II server) requires.
 *
 * Input layout (what OpenRS2 produces):
 *   <inputDir>/<index_id>/<archive_id>.dat   (one .dat per archive blob)
 *   <inputDir>/255/<idx>.dat                  (master reference tables)
 *
 * Output layout (what the server reads):
 *   <outputDir>/main_file_cache.dat2
 *   <outputDir>/main_file_cache.idx0..idxN
 *   <outputDir>/main_file_cache.idx255
 *
 * Usage:
 *   java -cp bin:data/libs/* com.rs.tools.CacheRepacker \
 *        ~/matrix/876_cache/cache/  ~/matrix/876_packed/
 *
 * Notes:
 * - .dat files are RAW compressed archive blobs and go straight into dat2
 *   via MainFile.putArchiveData(archiveId, bytes). No decompression needed.
 * - We pre-create empty index slots for gaps in the numbering (e.g. user
 *   has 0,1,2,3 but no 4) so addIndex() lines up to the right index id.
 * - Index 255 (the master reference table) is handled like any other index:
 *   each <archive>.dat in 255/ is the reference table for index <archive>.
 */
public final class CacheRepacker {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: java com.rs.tools.CacheRepacker <inputDir> <outputDir>");
            System.err.println("example: ~/matrix/876_cache/cache/  ~/matrix/876_packed/");
            System.exit(1);
        }
        File inputDir = new File(expandTilde(args[0]));
        File outputDir = new File(expandTilde(args[1]));

        if (!inputDir.isDirectory()) {
            System.err.println("[CacheRepacker] input dir missing or not a directory: " + inputDir);
            System.exit(1);
        }

        // Discover loose-format index ids. Each numeric subdir name = one cache index.
        // We exclude 255 from the regular-index list because that's the master
        // reference table, rebuilt by alex/store on rewriteTable() and accessed
        // via store.getIndex255() not store.getIndexes()[255]. Treating it as a
        // regular index would pad placeholder slots all the way to 255.
        TreeSet<Integer> indexIds = new TreeSet<>();
        File[] children = inputDir.listFiles();
        if (children != null) {
            for (File f : children) {
                if (!f.isDirectory()) continue;
                try {
                    int id = Integer.parseInt(f.getName());
                    if (id == 255) continue; // skip master ref table
                    indexIds.add(id);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (indexIds.isEmpty()) {
            System.err.println("[CacheRepacker] no numbered subdirs in " + inputDir);
            System.exit(1);
        }
        System.out.println("[CacheRepacker] found " + indexIds.size() + " loose indexes (excluding 255): " + indexIds);

        // Inventory: index -> sorted list of archive ids present
        TreeMap<Integer, List<Integer>> archivesPerIndex = new TreeMap<>();
        long totalArchives = 0;
        long totalBytes = 0;
        for (int idx : indexIds) {
            File idxDir = new File(inputDir, String.valueOf(idx));
            File[] datFiles = idxDir.listFiles((d, n) -> n.endsWith(".dat"));
            List<Integer> archives = new ArrayList<>();
            if (datFiles != null) {
                for (File df : datFiles) {
                    String name = df.getName();
                    int dot = name.indexOf('.');
                    if (dot <= 0) continue;
                    try {
                        archives.add(Integer.parseInt(name.substring(0, dot)));
                        totalBytes += df.length();
                    } catch (NumberFormatException ignored) {}
                }
            }
            Collections.sort(archives);
            archivesPerIndex.put(idx, archives);
            totalArchives += archives.size();
        }
        System.out.println("[CacheRepacker] " + totalArchives + " archives, " + (totalBytes / 1024 / 1024) + " MB total");

        // Wipe + recreate output dir to start fresh.
        outputDir.mkdirs();
        for (File f : outputDir.listFiles()) {
            if (f.getName().startsWith("main_file_cache.")) f.delete();
        }

        // Open a fresh Store. The Store(path) constructor creates the
        // dat2 file if it doesn't exist; addIndex() creates each .idx<N>.
        Store store = new Store(outputDir.getAbsolutePath() + File.separator);

        // Walk 0..maxId and add an index slot for each. Gaps get empty
        // placeholders so addIndex() returns the right index number for
        // real ones. Index 255 (master) is handled inside the same loop.
        int maxIdx = indexIds.last();
        System.out.println("[CacheRepacker] creating index slots 0.." + maxIdx);
        for (int i = 0; i <= maxIdx; i++) {
            int created = store.addIndex(false, false, 0);
            if (created != i) {
                System.err.println("[CacheRepacker] WARN: addIndex returned " + created + " for slot " + i);
            }
        }
        // The reference-table-master is at index 255, separate from sequential numbering.
        // alex/store's Store auto-creates idx255 inside the constructor (getIndex255()).

        // Now write each archive blob into its index's MainFile.
        // CRITICAL: alex/store's MainFile caches every archive blob in memory
        // when you putArchiveData() - that cache grows unbounded and OOMs after
        // ~270k archives on a 4GB heap. We call resetCachedArchives() every
        // 500 archives to keep RSS flat. Each batch is also wrapped in try/
        // finally so rewriteTable() runs even if a later index OOMs.
        long writtenArchives = 0;
        long startMs = System.currentTimeMillis();
        long lastReset = 0;
        final int RESET_INTERVAL = 500;

        for (int idx : indexIds) {
            File idxDir = new File(inputDir, String.valueOf(idx));
            List<Integer> archives = archivesPerIndex.get(idx);
            if (archives.isEmpty()) continue;

            if (idx >= store.getIndexes().length || store.getIndexes()[idx] == null) {
                System.err.println("[CacheRepacker] WARN: index slot " + idx + " missing, skipping " + archives.size() + " archives");
                continue;
            }
            Index targetIndex = store.getIndexes()[idx];
            int written = 0;

            try {
                for (int archiveId : archives) {
                    File datFile = new File(idxDir, archiveId + ".dat");
                    byte[] bytes = Files.readAllBytes(datFile.toPath());
                    if (bytes.length == 0) continue;
                    boolean ok = targetIndex.getMainFile().putArchiveData(archiveId, bytes);
                    if (!ok) {
                        System.err.println("[CacheRepacker] putArchiveData FAILED idx=" + idx + " archive=" + archiveId);
                    } else {
                        written++;
                        writtenArchives++;
                    }

                    // Periodically drop the in-memory archive cache. Without
                    // this we'd hold every blob ever written and OOM at ~270k.
                    if (writtenArchives - lastReset >= RESET_INTERVAL) {
                        try {
                            targetIndex.getMainFile().resetCachedArchives();
                        } catch (Throwable t) {
                            // Non-fatal - cache reset is opportunistic.
                        }
                        lastReset = writtenArchives;
                    }

                    if (writtenArchives % 1000 == 0) {
                        long ms = System.currentTimeMillis() - startMs;
                        long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
                        System.out.println("[CacheRepacker] " + writtenArchives + " archives (" + ms + " ms, " + usedMb + " MB used)");
                    }
                }
                System.out.println("[CacheRepacker] index " + idx + ": " + written + "/" + archives.size() + " archives");
            } finally {
                // Always flush the table so a partial repack at least has its
                // completed indexes readable.
                try {
                    targetIndex.rewriteTable();
                } catch (Throwable t) {
                    System.err.println("[CacheRepacker] rewriteTable threw on index " + idx + ": " + t);
                }
            }
        }

        long totalMs = System.currentTimeMillis() - startMs;
        System.out.println("[CacheRepacker] DONE. " + writtenArchives + " archives in " + totalMs + " ms");
        System.out.println("[CacheRepacker] output dir: " + outputDir.getAbsolutePath());

        // Sanity-list output
        File[] out = outputDir.listFiles();
        if (out != null) {
            int idxCount = 0;
            long datSize = 0;
            for (File f : out) {
                if (f.getName().equals("main_file_cache.dat2")) datSize = f.length();
                if (f.getName().startsWith("main_file_cache.idx")) idxCount++;
            }
            System.out.println("[CacheRepacker] dat2 size: " + (datSize / 1024 / 1024) + " MB, idx files: " + idxCount);
            if (idxCount < 5) {
                System.err.println("[CacheRepacker] WARN: only " + idxCount + " idx files written - something went wrong");
            }
        }
    }

    private static String expandTilde(String p) {
        if (p.startsWith("~")) {
            return System.getProperty("user.home") + p.substring(1);
        }
        return p;
    }

    private CacheRepacker() {}
}
