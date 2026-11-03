package com.rs.game.player.anticheat.botcracker;

/**
 * Tunables for BotCracker. Everything is a plain mutable static so operators
 * can adjust thresholds at runtime (via ::botcracker set) without a restart.
 * <p>
 * Thresholds were chosen conservatively: BotCracker never bans directly, it
 * only queues accounts for a human-reviewed ban wave, so a false positive
 * costs a moderator a minute rather than a player their account.
 */
public final class BotCrackerConfig {

	private BotCrackerConfig() {
	}

	/** Master switch. */
	public static volatile boolean ENABLED = true;

	/** Skip staff accounts (rights >= this) and AI players. */
	public static volatile int EXEMPT_RIGHTS = 2;

	/** Length of the OSRS/RS game tick in ms. */
	public static final int TICK_MS = 600;

	// ------------------------------------------------------------------
	// IAT / tick alignment
	// ------------------------------------------------------------------

	/** Ring buffer size for inter-arrival samples per action kind. */
	public static volatile int IAT_WINDOW = 2048;
	/** Minimum samples before the IAT detector will emit a finding. */
	public static volatile int IAT_MIN_SAMPLES = 400;
	/**
	 * Coefficient of variation (stddev / mean) below which human timing is
	 * essentially impossible over hundreds of actions. Humans on a spam-click
	 * task typically land around 0.25 - 0.6.
	 */
	public static volatile double IAT_CV_RIGID = 0.08;
	/** Number of phase bins across one tick for the alignment histogram. */
	public static final int PHASE_BINS = 12;
	/**
	 * Normalised Shannon entropy (0..1) of the tick-phase histogram below which
	 * we consider arrivals "locked" to a tick phase. Humans are ~0.9+.
	 */
	public static volatile double PHASE_ENTROPY_LOCKED = 0.55;
	/** Share of samples in the single most popular phase bin that is suspicious. */
	public static volatile double PHASE_PEAK_SHARE = 0.60;
	/** |lag-1 autocorrelation| above this means a deterministic/periodic generator. */
	public static volatile double IAT_AUTOCORR_SUSPICIOUS = 0.70;
	/** Fraction of unique IAT values (ms granularity) below this is "quantised". */
	public static volatile double IAT_UNIQUE_RATIO_LOW = 0.05;

	// ------------------------------------------------------------------
	// Fatigue / efficiency
	// ------------------------------------------------------------------

	/** Bucket size for throughput history. */
	public static final long FATIGUE_BUCKET_MS = 10L * 60L * 1000L; // 10 minutes
	/** Number of buckets retained (36 * 10 min = 6 hours). */
	public static final int FATIGUE_BUCKETS = 36;
	/** Minimum continuous span before fatigue heuristics apply (2 hours). */
	public static volatile long FATIGUE_MIN_SPAN_MS = 2L * 60L * 60L * 1000L;
	/** A pause longer than this counts as a "break" and resets continuity. */
	public static volatile long BREAK_GAP_MS = 3L * 60L * 1000L;
	/** Sustained efficiency (observed / theoretical max) considered superhuman. */
	public static volatile double EFFICIENCY_CEILING = 0.92;
	/** Share of buckets that must exceed the ceiling before flagging. */
	public static volatile double EFFICIENCY_BUCKET_SHARE = 0.85;
	/** CV of per-bucket throughput below which there is "no fatigue curve". */
	public static volatile double FATIGUE_CV_INVARIANT = 0.05;
	/**
	 * Humans slow down: the last quarter of a long session usually has lower
	 * throughput than the first. If last/first >= this and the session is
	 * long, that's invariance.
	 */
	public static volatile double FATIGUE_LATE_EARLY_RATIO = 0.98;

	// ------------------------------------------------------------------
	// Spatial
	// ------------------------------------------------------------------

	/** Click samples retained per player. */
	public static volatile int CLICK_WINDOW = 1024;
	/** Minimum clicks before spatial heuristics run. */
	public static volatile int CLICK_MIN_SAMPLES = 300;
	/** Fraction of clicks that are pixel-identical to a previous click. */
	public static volatile double CLICK_DUPLICATE_RATIO = 0.35;
	/** Std-dev (pixels) of clicks around their cluster centre below which it's robotic. */
	public static volatile double CLICK_CLUSTER_STDDEV_PX = 1.5;
	/**
	 * Clustering radius in px. Clicks within this radius of an existing
	 * cluster centre are merged into it.
	 */
	public static volatile int CLICK_CLUSTER_RADIUS_PX = 24;
	/**
	 * Synthetic uniformity: bots that add jitter usually draw from a uniform
	 * box. The ratio of nearest-neighbour distance spread to a uniform-random
	 * expectation close to 1.0 with near-zero kurtosis is unnatural. We use the
	 * cheaper "grid alignment" test instead: share of clicks whose x and y are
	 * both multiples of a small stride.
	 */
	public static volatile double CLICK_GRID_SHARE = 0.50;

	// ------------------------------------------------------------------
	// Scoring / queue
	// ------------------------------------------------------------------

	/** Aggregate score at which an account enters the shadow profile queue. */
	public static volatile double SHADOW_THRESHOLD = 0.70;
	/** Aggregate score at which the ban wave will act on a shadowed account. */
	public static volatile double BAN_THRESHOLD = 0.85;
	/** Minimum distinct detector sources that must agree before a ban wave acts. */
	public static volatile int BAN_MIN_SOURCES = 2;
	/** Ticks between heuristic evaluations per player (50 ticks = 30s). */
	public static volatile int EVALUATE_EVERY_TICKS = 50;
	/** Ban-wave period. 0 disables automatic waves (manual ::botcracker wave). */
	public static volatile long BAN_WAVE_PERIOD_MS = 0;
	/** Directory for telemetry logs and shadow profiles. */
	public static volatile String DATA_DIR = "data/botcracker";
	/** Write raw telemetry (one line per sample) to disk. Expensive; off by default. */
	public static volatile boolean RAW_TELEMETRY_LOG = false;
}
