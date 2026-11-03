package com.rs.game.player.anticheat.botcracker;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.rs.bot.AIPlayer;
import com.rs.executor.WorldThread;
import com.rs.game.player.Player;
import com.rs.utils.Logger;
import com.rs.utils.Utils;

/**
 * BotCracker facade. This is the only class the rest of the server touches.
 * <p>
 * Integration points (all one-liners, see docs/BOTCRACKER.md):
 * <ul>
 * <li>{@link #onTickStart(long)} from the world thread at the top of each cycle.</li>
 * <li>{@link #onPacket(Player, int)} from the packet decoder for every
 * gameplay opcode, and {@link #onClick(Player, int, int, int)} for CLICK_PACKET.</li>
 * <li>{@link #onSkillAction(Player)} from repetitive skill actions when a
 * production cycle completes (Fletching, Crafting, Cooking...).</li>
 * <li>{@link #onXp(Player, int, double)} from Skills.addXp.</li>
 * <li>{@link #onLogin(Player)} / {@link #onLogout(Player)} from Player.</li>
 * <li>{@link #tick()} once per world cycle (drives evaluation and ban waves).</li>
 * </ul>
 * Everything is fail-safe: a bug in a detector is logged and never affects
 * gameplay.
 */
public final class BotCracker {

	private static final BotCracker INSTANCE = new BotCracker();

	public static BotCracker get() {
		return INSTANCE;
	}

	private final ConcurrentHashMap<String, PlayerTelemetry> online = new ConcurrentHashMap<String, PlayerTelemetry>();
	private final TelemetryLogger log = new TelemetryLogger();
	private final HeuristicEngine engine = new HeuristicEngine();
	private final ShadowProfileManager shadow = new ShadowProfileManager(log);
	private volatile long tickStartMs;
	private volatile boolean initialised;

	private BotCracker() {
		engine.register(new IATAnomalyDetector());
		engine.register(new FatigueInvarianceDetector());
		engine.register(new SpatialClusterDetector());
	}

	/** Call once from the launcher after executors exist. */
	public void init() {
		if (initialised)
			return;
		initialised = true;
		shadow.load();
		Logger.log("BotCracker", "Initialised with " + engine.getDetectors().size() + " detectors.");
	}

	// ------------------------------------------------------------------
	// Hooks
	// ------------------------------------------------------------------

	/** World thread, start of cycle. */
	public void onTickStart(long now) {
		tickStartMs = now;
	}

	public void onLogin(Player player) {
		if (!tracked(player))
			return;
		online.put(player.getUsername(), new PlayerTelemetry(player.getUsername(), Utils.currentTimeMillis()));
	}

	public void onLogout(Player player) {
		if (player == null || player.getUsername() == null)
			return;
		PlayerTelemetry t = online.remove(player.getUsername());
		if (t != null) {
			try {
				long now = Utils.currentTimeMillis();
				evaluate(player, t, now, true);
			} catch (Throwable e) {
				Logger.handle(e);
			}
		}
	}

	/** Any gameplay packet (Netty thread). Classified into an ActionKind by opcode group. */
	public void onPacket(Player player, ActionKind kind) {
		if (!BotCrackerConfig.ENABLED)
			return;
		PlayerTelemetry t = telemetry(player);
		if (t == null)
			return;
		long now = Utils.currentTimeMillis();
		t.recordAction(kind, now, tickStartMs);
		if (kind != ActionKind.INPUT_PACKET)
			t.recordAction(ActionKind.INPUT_PACKET, now, tickStartMs);
		log.raw(player.getUsername(), kind, now, tickStartMs > 0 ? now - tickStartMs : -1, -1, -1);
	}

	/** CLICK_PACKET with screen coordinates (Netty thread). */
	public void onClick(Player player, int x, int y, int button) {
		if (!BotCrackerConfig.ENABLED)
			return;
		PlayerTelemetry t = telemetry(player);
		if (t == null)
			return;
		long now = Utils.currentTimeMillis();
		t.recordClick(x, y, button, now);
		t.recordAction(ActionKind.MOUSE_CLICK, now, tickStartMs);
		log.raw(player.getUsername(), ActionKind.MOUSE_CLICK, now, tickStartMs > 0 ? now - tickStartMs : -1, x, y);
	}

	/** A production cycle of a repetitive skill completed (world thread). */
	public void onSkillAction(Player player) {
		if (!BotCrackerConfig.ENABLED)
			return;
		PlayerTelemetry t = telemetry(player);
		if (t != null)
			t.recordAction(ActionKind.SKILL_ACTION, Utils.currentTimeMillis(), tickStartMs);
	}

	public void onXp(Player player, int skill, double xp) {
		if (!BotCrackerConfig.ENABLED || xp <= 0)
			return;
		PlayerTelemetry t = telemetry(player);
		if (t != null)
			t.recordXp(xp, Utils.currentTimeMillis());
	}

	/** World thread, once per cycle. */
	public void tick() {
		if (!BotCrackerConfig.ENABLED || !initialised)
			return;
		long now = Utils.currentTimeMillis();
		long cycle = WorldThread.WORLD_CYCLE;
		int every = Math.max(1, BotCrackerConfig.EVALUATE_EVERY_TICKS);
		// stagger evaluations: each player is evaluated on a different tick
		for (PlayerTelemetry t : online.values()) {
			if ((cycle + (t.getUsername().hashCode() & 0x7fffffff)) % every != 0)
				continue;
			Player p = com.rs.game.World.getPlayer(t.getUsername());
			if (p == null)
				continue;
			try {
				evaluate(p, t, now, false);
			} catch (Throwable e) {
				Logger.handle(e);
			}
		}
		shadow.tick(now);
	}

	// ------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------

	private void evaluate(Player player, PlayerTelemetry t, long now, boolean finalPass) {
		HeuristicEngine.Verdict v = engine.evaluate(t, now);
		t.setEvaluation(v.score, v.findings, WorldThread.WORLD_CYCLE);
		if (v.findings.isEmpty() && !finalPass)
			return;
		log.verdict(player.getUsername(), v);
		if (v.score >= BotCrackerConfig.SHADOW_THRESHOLD && !shadow.isWhitelisted(player.getUsername())) {
			String ip = player.getSession() != null ? player.getSession().getIP() : player.getLastGameIp();
			shadow.shadow(player.getUsername(), ip, v, now);
		}
	}

	private boolean tracked(Player player) {
		if (player == null || player.getUsername() == null)
			return false;
		if (player instanceof AIPlayer)
			return false;
		return player.getRights() < BotCrackerConfig.EXEMPT_RIGHTS;
	}

	private PlayerTelemetry telemetry(Player player) {
		if (player == null || player.getUsername() == null)
			return null;
		PlayerTelemetry t = online.get(player.getUsername());
		if (t == null && tracked(player) && player.hasStarted() && !player.hasFinished()) {
			t = new PlayerTelemetry(player.getUsername(), Utils.currentTimeMillis());
			PlayerTelemetry prev = online.putIfAbsent(player.getUsername(), t);
			if (prev != null)
				t = prev;
		}
		return t;
	}

	// ------------------------------------------------------------------
	// Admin / introspection
	// ------------------------------------------------------------------

	public PlayerTelemetry getTelemetry(String username) {
		return online.get(username);
	}

	public Collection<PlayerTelemetry> getOnlineTelemetry() {
		return online.values();
	}

	public ShadowProfileManager getShadowProfiles() {
		return shadow;
	}

	public HeuristicEngine getEngine() {
		return engine;
	}

	public TelemetryLogger getLog() {
		return log;
	}

	/** Force an immediate evaluation of one player (::botcracker eval name). */
	public HeuristicEngine.Verdict evaluateNow(Player player) {
		PlayerTelemetry t = telemetry(player);
		if (t == null)
			return null;
		long now = Utils.currentTimeMillis();
		HeuristicEngine.Verdict v = engine.evaluate(t, now);
		t.setEvaluation(v.score, v.findings, WorldThread.WORLD_CYCLE);
		return v;
	}

	/** Flush logs and shadow profiles (call on shutdown). */
	public void shutdown() {
		shadow.saveAll();
		log.drain();
	}
}
