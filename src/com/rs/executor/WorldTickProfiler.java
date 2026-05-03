package com.rs.executor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-phase timing for WorldThread. Profile-before-optimize: before
 * splitting tick work across threads, we need actual numbers showing
 * which phases are the hot ones.
 *
 * Usage from WorldThread:
 *   WorldTickProfiler.start("processEntity");
 *   ... work ...
 *   WorldTickProfiler.end("processEntity");
 *
 * Every 100 ticks (~60s at 600ms), prints accumulated stats: count, total
 * ms, avg ms, max ms per phase. After running with this enabled for a few
 * minutes under load (real players + bots + citizens), we know exactly
 * which phase to parallelize and how much it would buy us.
 *
 * Single-thread by design - WorldThread is the only writer. Stats are
 * AtomicLong only because the dump task may read them concurrently.
 */
public final class WorldTickProfiler {

    private WorldTickProfiler() {}

    /** Toggle. When false, all calls are no-ops. Set via ::profilestart command. */
    public static volatile boolean ENABLED = false;

    /** Tick counter (incremented at end of every WorldThread cycle). */
    private static int tickCount = 0;
    /** How often to dump stats (in ticks). 100 ticks ~ 60s. */
    private static final int DUMP_INTERVAL_TICKS = 100;

    private static class PhaseStats {
        final AtomicLong count = new AtomicLong();
        final AtomicLong totalNs = new AtomicLong();
        final AtomicLong maxNs = new AtomicLong();
        long lastStartNs = 0;
    }

    /** Phase name -> stats. java.util.concurrent map for lock-free reads. */
    private static final java.util.concurrent.ConcurrentHashMap<String, PhaseStats> PHASES =
        new java.util.concurrent.ConcurrentHashMap<>();

    public static void start(String phase) {
        if (!ENABLED) return;
        PHASES.computeIfAbsent(phase, k -> new PhaseStats()).lastStartNs = System.nanoTime();
    }

    public static void end(String phase) {
        if (!ENABLED) return;
        PhaseStats s = PHASES.get(phase);
        if (s == null || s.lastStartNs == 0) return;
        long ns = System.nanoTime() - s.lastStartNs;
        s.lastStartNs = 0;
        s.count.incrementAndGet();
        s.totalNs.addAndGet(ns);
        // Atomic max via CAS loop.
        long cur;
        do {
            cur = s.maxNs.get();
            if (ns <= cur) break;
        } while (!s.maxNs.compareAndSet(cur, ns));
    }

    /** Called once per tick. Triggers a stats dump every DUMP_INTERVAL_TICKS. */
    public static void onTickComplete() {
        if (!ENABLED) return;
        tickCount++;
        if (tickCount % DUMP_INTERVAL_TICKS == 0) dump();
    }

    /** Print accumulated stats. Resets counters after dump. */
    public static synchronized void dump() {
        StringBuilder sb = new StringBuilder("[WorldTickProfiler] ").append(tickCount).append(" ticks profiled\n");
        // Sort by total time desc - hottest phase first.
        java.util.List<java.util.Map.Entry<String, PhaseStats>> entries = new java.util.ArrayList<>(PHASES.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue().totalNs.get(), a.getValue().totalNs.get()));
        for (java.util.Map.Entry<String, PhaseStats> e : entries) {
            PhaseStats s = e.getValue();
            long count = s.count.get();
            if (count == 0) continue;
            long totalMs = s.totalNs.get() / 1_000_000;
            long avgUs = (s.totalNs.get() / count) / 1_000;
            long maxMs = s.maxNs.get() / 1_000_000;
            sb.append(String.format("  %-32s n=%6d  total=%6d ms  avg=%5d us  max=%4d ms%n",
                e.getKey(), count, totalMs, avgUs, maxMs));
        }
        System.out.print(sb.toString());
        // Reset for next interval so we see deltas, not cumulative.
        for (PhaseStats s : PHASES.values()) {
            s.count.set(0);
            s.totalNs.set(0);
            s.maxNs.set(0);
        }
    }

    public static void enable() {
        ENABLED = true;
        System.out.println("[WorldTickProfiler] ENABLED - stats every " + DUMP_INTERVAL_TICKS + " ticks");
    }

    public static void disable() {
        ENABLED = false;
        System.out.println("[WorldTickProfiler] DISABLED");
    }
}
