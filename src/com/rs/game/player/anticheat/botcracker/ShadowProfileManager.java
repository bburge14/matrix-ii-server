package com.rs.game.player.anticheat.botcracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.rs.executor.GameExecutorManager;
import com.rs.utils.Logger;

/**
 * Shadow profile queue and delayed ban wave.
 * <p>
 * Flagged accounts are never actioned immediately. Instead a
 * {@link ShadowProfile} is created (or updated) and persisted. The account
 * keeps playing with no visible change, which stops bot authors from
 * A/B-testing which behaviour tripped detection. A ban wave, run manually or
 * on a timer, walks the queue and hands every profile that still exceeds the
 * ban threshold to the configured {@link BanExecutor}.
 */
public final class ShadowProfileManager {

	private final ConcurrentHashMap<String, ShadowProfile> profiles = new ConcurrentHashMap<String, ShadowProfile>();
	private final TelemetryLogger log;
	private volatile BanExecutor executor = new LoginServerBanExecutor();
	private volatile long lastWave;

	public ShadowProfileManager(TelemetryLogger log) {
		this.log = log;
	}

	public void setBanExecutor(BanExecutor executor) {
		this.executor = executor;
	}

	// ------------------------------------------------------------------
	// Queue maintenance
	// ------------------------------------------------------------------

	/** Record a verdict that crossed the shadow threshold. */
	public void shadow(String username, String ip, HeuristicEngine.Verdict verdict, long now) {
		ShadowProfile p = profiles.get(username);
		boolean fresh = p == null;
		if (fresh) {
			p = new ShadowProfile(username, now);
			ShadowProfile prev = profiles.putIfAbsent(username, p);
			if (prev != null)
				p = prev;
		}
		synchronized (p) {
			p.addSnapshot(now, verdict, ip);
		}
		log.event(username, (fresh ? "SHADOWED " : "shadow-update ") + p);
		persistAsync(p);
	}

	public ShadowProfile get(String username) {
		return profiles.get(username);
	}

	public Collection<ShadowProfile> all() {
		return profiles.values();
	}

	public boolean remove(String username) {
		ShadowProfile p = profiles.remove(username);
		if (p == null)
			return false;
		new File(dir(), TelemetryLogger.safe(username) + ".bin").delete();
		log.event(username, "shadow profile cleared by moderator");
		return true;
	}

	public boolean whitelist(String username, String note) {
		ShadowProfile p = profiles.get(username);
		if (p == null) {
			p = new ShadowProfile(username, System.currentTimeMillis());
			profiles.put(username, p);
		}
		synchronized (p) {
			p.setWhitelisted(true);
			p.setModeratorNote(note);
		}
		log.event(username, "whitelisted: " + note);
		persistAsync(p);
		return true;
	}

	public boolean isWhitelisted(String username) {
		ShadowProfile p = profiles.get(username);
		return p != null && p.isWhitelisted();
	}

	// ------------------------------------------------------------------
	// Ban wave
	// ------------------------------------------------------------------

	/**
	 * Execute a ban wave now.
	 *
	 * @param dryRun when true nothing is actioned, the candidates are only
	 *            listed (what ::botcracker wave preview does)
	 * @return the usernames that were (or would be) actioned
	 */
	public List<String> executeBanWave(boolean dryRun) {
		List<String> actioned = new ArrayList<String>();
		for (ShadowProfile p : profiles.values()) {
			double score;
			int sources;
			boolean white;
			synchronized (p) {
				score = p.getRecentScore();
				sources = p.getRecentSources();
				white = p.isWhitelisted();
			}
			if (white || score < BotCrackerConfig.BAN_THRESHOLD || sources < BotCrackerConfig.BAN_MIN_SOURCES)
				continue;
			String reason = String.format("BotCracker wave: score %.2f, %d detectors agree, first flagged %tF", score,
					sources, p.getFirstFlagged());
			if (dryRun) {
				actioned.add(p.getUsername());
				continue;
			}
			boolean done;
			try {
				done = executor.execute(p, reason);
			} catch (Throwable e) {
				Logger.handle(e);
				done = false;
			}
			log.event(p.getUsername(), (done ? "BANNED " : "ban-executor declined ") + reason);
			if (done) {
				actioned.add(p.getUsername());
				profiles.remove(p.getUsername());
				File f = new File(dir(), TelemetryLogger.safe(p.getUsername()) + ".bin");
				File archive = new File(new File(BotCrackerConfig.DATA_DIR, "banned"), f.getName());
				archive.getParentFile().mkdirs();
				if (!f.renameTo(archive))
					persist(p, archive);
			}
		}
		if (!dryRun)
			lastWave = System.currentTimeMillis();
		return actioned;
	}

	/** Called every tick by {@link BotCracker}; fires the periodic wave if enabled. */
	void tick(long now) {
		long period = BotCrackerConfig.BAN_WAVE_PERIOD_MS;
		if (period <= 0 || now - lastWave < period)
			return;
		lastWave = now;
		GameExecutorManager.slowExecutor.execute(new Runnable() {
			@Override
			public void run() {
				List<String> hit = executeBanWave(false);
				Logger.log("BotCracker", "Scheduled ban wave actioned " + hit.size() + " account(s): " + hit);
			}
		});
	}

	// ------------------------------------------------------------------
	// Persistence
	// ------------------------------------------------------------------

	private File dir() {
		File d = new File(BotCrackerConfig.DATA_DIR, "shadow");
		if (!d.exists())
			d.mkdirs();
		return d;
	}

	private void persistAsync(final ShadowProfile p) {
		GameExecutorManager.slowExecutor.execute(new Runnable() {
			@Override
			public void run() {
				persist(p, new File(dir(), TelemetryLogger.safe(p.getUsername()) + ".bin"));
			}
		});
	}

	private void persist(ShadowProfile p, File f) {
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(f));
			synchronized (p) {
				out.writeObject(p);
			}
		} catch (Throwable e) {
			Logger.handle(e);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (Throwable ignored) {
				}
		}
	}

	/** Load persisted profiles at startup. */
	public void load() {
		File[] files = dir().listFiles();
		if (files == null)
			return;
		int n = 0;
		for (File f : files) {
			if (!f.getName().endsWith(".bin"))
				continue;
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(f));
				ShadowProfile p = (ShadowProfile) in.readObject();
				profiles.put(p.getUsername(), p);
				n++;
			} catch (Throwable e) {
				Logger.handle(e);
			} finally {
				if (in != null)
					try {
						in.close();
					} catch (Throwable ignored) {
					}
			}
		}
		Logger.log("BotCracker", "Loaded " + n + " shadow profile(s).");
	}

	public void saveAll() {
		for (ShadowProfile p : profiles.values())
			persist(p, new File(dir(), TelemetryLogger.safe(p.getUsername()) + ".bin"));
	}
}
