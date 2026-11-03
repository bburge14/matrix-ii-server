package com.rs.game.player.anticheat.botcracker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rs.executor.GameExecutorManager;
import com.rs.utils.Logger;

/**
 * Asynchronous, append-only telemetry writer.
 * <p>
 * Callers enqueue short text lines; a single drain task on the slow executor
 * writes them to {@code data/botcracker/<kind>/<username>.log}. Nothing on the
 * world or Netty threads ever touches disk.
 * <p>
 * Two streams exist:
 * <ul>
 * <li>{@code events}: one line per evaluation / flag / ban-wave decision.
 * Always on, low volume, this is the audit trail.</li>
 * <li>{@code raw}: one line per sampled action/click. Off unless
 * {@link BotCrackerConfig#RAW_TELEMETRY_LOG} is set, used to pull a
 * suspect's full input stream for offline analysis.</li>
 * </ul>
 */
public final class TelemetryLogger {

	private static final class Line {
		final String stream, username, text;

		Line(String stream, String username, String text) {
			this.stream = stream;
			this.username = username;
			this.text = text;
		}
	}

	private final ConcurrentLinkedQueue<Line> queue = new ConcurrentLinkedQueue<Line>();
	private final AtomicBoolean drainScheduled = new AtomicBoolean();
	private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public void event(String username, String text) {
		enqueue("events", username, text);
	}

	public void raw(String username, ActionKind kind, long now, long phase, int x, int y) {
		if (!BotCrackerConfig.RAW_TELEMETRY_LOG)
			return;
		enqueue("raw", username, kind + "\t" + now + "\t" + phase + "\t" + x + "\t" + y);
	}

	public void verdict(String username, HeuristicEngine.Verdict v) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("verdict score=%.3f sources=%d", v.score, v.sources));
		for (Finding f : v.findings)
			sb.append(" | ").append(f);
		event(username, sb.toString());
	}

	private void enqueue(String stream, String username, String text) {
		queue.add(new Line(stream, username, text));
		if (drainScheduled.compareAndSet(false, true)) {
			try {
				GameExecutorManager.slowExecutor.execute(new Runnable() {
					@Override
					public void run() {
						drain();
					}
				});
			} catch (Throwable e) {
				drainScheduled.set(false);
				Logger.handle(e);
			}
		}
	}

	/** Flush everything queued. Safe to call from any thread. */
	public void drain() {
		try {
			Line l;
			while ((l = queue.poll()) != null)
				write(l);
		} finally {
			drainScheduled.set(false);
		}
		if (!queue.isEmpty() && drainScheduled.compareAndSet(false, true))
			drain();
	}

	private void write(Line l) {
		File dir = new File(BotCrackerConfig.DATA_DIR, l.stream);
		if (!dir.exists())
			dir.mkdirs();
		File f = new File(dir, safe(l.username) + ".log");
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(f, true));
			synchronized (fmt) {
				w.write(fmt.format(new Date()));
			}
			w.write('\t');
			w.write(l.text);
			w.newLine();
		} catch (IOException e) {
			Logger.handle(e);
		} finally {
			if (w != null)
				try {
					w.close();
				} catch (IOException ignored) {
				}
		}
	}

	static String safe(String username) {
		return username.replaceAll("[^a-zA-Z0-9_\\- ]", "_");
	}

	/** Human readable multi-line report for the admin command. */
	public static String report(PlayerTelemetry t, long now) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("BotCracker report for %s: score=%.3f, online %.1fh, continuous %.1fh, breaks=%d, xp=%.0f%n",
				t.getUsername(), t.getScore(), (now - t.getLoginTime()) / 3600000.0, t.getContinuousSpanMs(now) / 3600000.0,
				t.getBreaksTaken(), t.getSessionXp()));
		for (ActionKind k : ActionKind.values()) {
			long n = t.getTotalActions(k);
			if (n > 0)
				sb.append(String.format("  %s: %d actions, IAT cv=%.3f%n", k, n, Stats.cv(t.iatSnapshot(k))));
		}
		List<Finding> fs = t.getFindings();
		if (fs.isEmpty())
			sb.append("  no findings\n");
		for (Finding f : fs)
			sb.append("  ").append(f).append('\n');
		return sb.toString();
	}
}
