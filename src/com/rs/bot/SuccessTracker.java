package com.rs.bot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight aggregate counter for bot outcomes. Tracks how many times
 * each method has been picked, how many led to actual XP gain, and how
 * many got blacklisted as stuck. The counters are dumped periodically
 * to AuditLog so the user can see real success rates instead of just
 * guessing from snapshots.
 *
 * Counters are per-method-description so the report shows which
 * methods are actually paying off vs which keep failing.
 */
public final class SuccessTracker {

    private SuccessTracker() {}

    private static final ConcurrentHashMap<String, AtomicLong> picked = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> success = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> stuck = new ConcurrentHashMap<>();
    private static final AtomicLong totalPicked = new AtomicLong();
    private static final AtomicLong totalSuccess = new AtomicLong();
    private static final AtomicLong totalStuck = new AtomicLong();

    public static void onMethodPicked(String desc) {
        if (desc == null) return;
        picked.computeIfAbsent(desc, k -> new AtomicLong()).incrementAndGet();
        totalPicked.incrementAndGet();
    }

    public static void onMethodSuccess(String desc) {
        if (desc == null) return;
        success.computeIfAbsent(desc, k -> new AtomicLong()).incrementAndGet();
        totalSuccess.incrementAndGet();
    }

    public static void onMethodStuck(String desc) {
        if (desc == null) return;
        stuck.computeIfAbsent(desc, k -> new AtomicLong()).incrementAndGet();
        totalStuck.incrementAndGet();
    }

    /** Dump current counters to audit.log. Called from BotAuditor periodic task. */
    public static void dump() {
        long tp = totalPicked.get();
        long ts = totalSuccess.get();
        long tk = totalStuck.get();
        AuditLog.log("=== SUCCESS TALLY ===");
        AuditLog.log("totals: picked=" + tp + " success=" + ts + " stuck=" + tk
            + (tp > 0 ? "  (" + (ts * 100L / tp) + "% success, " + (tk * 100L / tp) + "% stuck)" : ""));
        // Per-method breakdown - only methods that have any activity
        java.util.TreeSet<String> keys = new java.util.TreeSet<>();
        keys.addAll(picked.keySet());
        keys.addAll(success.keySet());
        keys.addAll(stuck.keySet());
        for (String k : keys) {
            long p = picked.getOrDefault(k, new AtomicLong()).get();
            long s = success.getOrDefault(k, new AtomicLong()).get();
            long st = stuck.getOrDefault(k, new AtomicLong()).get();
            if (p == 0 && s == 0 && st == 0) continue;
            AuditLog.log("  " + k + ": picked=" + p + " success=" + s + " stuck=" + st);
        }
        AuditLog.log("=== END TALLY ===");
    }
}
