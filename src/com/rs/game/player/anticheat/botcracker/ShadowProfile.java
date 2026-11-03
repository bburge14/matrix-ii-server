package com.rs.game.player.anticheat.botcracker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent record of a flagged account. Lives on disk under
 * {@code data/botcracker/shadow/} so it survives restarts and so a moderator
 * can inspect it before the ban wave acts.
 */
public final class ShadowProfile implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final class Snapshot implements Serializable {
		private static final long serialVersionUID = 1L;
		public final long time;
		public final double score;
		public final int sources;
		public final List<String> findings;

		Snapshot(long time, double score, int sources, List<Finding> fs) {
			this.time = time;
			this.score = score;
			this.sources = sources;
			this.findings = new ArrayList<String>(fs.size());
			for (Finding f : fs)
				this.findings.add(f.toString());
		}
	}

	private final String username;
	private final long firstFlagged;
	private String lastIp;
	private final List<Snapshot> history = new ArrayList<Snapshot>();
	private double peakScore;
	private int peakSources;
	private boolean whitelisted;
	private String moderatorNote;

	public ShadowProfile(String username, long now) {
		this.username = username;
		this.firstFlagged = now;
	}

	public void addSnapshot(long now, HeuristicEngine.Verdict v, String ip) {
		history.add(new Snapshot(now, v.score, v.sources, v.findings));
		if (history.size() > 200)
			history.remove(0);
		if (v.score > peakScore)
			peakScore = v.score;
		if (v.sources > peakSources)
			peakSources = v.sources;
		if (ip != null)
			lastIp = ip;
	}

	/**
	 * Score used by the ban wave: the mean of the three most recent snapshots,
	 * so a single spike is not enough and a bot cannot "age out" a flag by
	 * idling for a few minutes.
	 */
	public double getRecentScore() {
		if (history.isEmpty())
			return 0;
		int n = Math.min(3, history.size());
		double s = 0;
		for (int i = history.size() - n; i < history.size(); i++)
			s += history.get(i).score;
		return s / n;
	}

	public int getRecentSources() {
		if (history.isEmpty())
			return 0;
		int best = 0;
		for (int i = Math.max(0, history.size() - 3); i < history.size(); i++)
			best = Math.max(best, history.get(i).sources);
		return best;
	}

	public String getUsername() {
		return username;
	}

	public long getFirstFlagged() {
		return firstFlagged;
	}

	public String getLastIp() {
		return lastIp;
	}

	public List<Snapshot> getHistory() {
		return history;
	}

	public double getPeakScore() {
		return peakScore;
	}

	public int getPeakSources() {
		return peakSources;
	}

	public boolean isWhitelisted() {
		return whitelisted;
	}

	public void setWhitelisted(boolean whitelisted) {
		this.whitelisted = whitelisted;
	}

	public String getModeratorNote() {
		return moderatorNote;
	}

	public void setModeratorNote(String moderatorNote) {
		this.moderatorNote = moderatorNote;
	}

	@Override
	public String toString() {
		Snapshot last = history.isEmpty() ? null : history.get(history.size() - 1);
		return String.format("%s peak=%.2f/%d recent=%.2f/%d snapshots=%d%s%s", username, peakScore, peakSources,
				getRecentScore(), getRecentSources(), history.size(), whitelisted ? " WHITELISTED" : "",
				last == null ? "" : " last=" + last.findings);
	}
}
