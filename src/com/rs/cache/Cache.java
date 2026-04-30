package com.rs.cache;

import java.io.IOException;

import com.alex.io.OutputStream;
import com.alex.store.ReferenceTable;
import com.alex.store.Store;
import com.alex.util.whirlpool.Whirlpool;
import com.rs.Settings;
import com.rs.utils.Utils;

public final class Cache {

	/** Primary store - the OS cache. Everything loads from here first. */
	public static Store STORE;

	/**
	 * Secondary "DLC" store - read-only parts-bin loaded if
	 * Settings.CACHE_PATH_DLC is set. ItemDefinitions/ObjectDefinitions/
	 * NPCDefinitions fall through to this when an ID is missing in primary.
	 * Stays null when DLC isn't configured.
	 */
	public static Store STORE_DLC;

	private Cache() {

	}

	public static void init() throws IOException {
		ReferenceTable.NEW_PROTOCOL = true;

		// Try PRIMARY (876) first. If it doesn't exist, has no idx files,
		// or the alex/store reader chokes on it, fall back to LEGACY (830)
		// so the server still boots. This makes the migration risk-free:
		// invalid 876 = no downtime, just a log line and we're back on 830.
		STORE = tryLoadStore(Settings.CACHE_PATH_PRIMARY, "primary");
		if (STORE == null && Settings.CACHE_PATH_LEGACY != null) {
			System.err.println("[Cache] primary store unavailable, falling back to legacy "
				+ Settings.CACHE_PATH_LEGACY);
			STORE = tryLoadStore(Settings.CACHE_PATH_LEGACY, "legacy");
		}
		if (STORE == null) {
			// Last-resort: original Settings.CACHE_PATH so existing setups
			// without the new keys still work. If THAT fails the next access
			// will NPE, which is the previous behaviour.
			STORE = new Store(Settings.CACHE_PATH);
			System.err.println("[Cache] both primary and legacy paths missing - opened raw "
				+ Settings.CACHE_PATH);
		}

		// DLC is optional. Only loaded if a path is configured. Failures
		// here are non-fatal - we continue with primary-only.
		if (Settings.CACHE_PATH_DLC != null && !Settings.CACHE_PATH_DLC.isEmpty()) {
			STORE_DLC = tryLoadStore(Settings.CACHE_PATH_DLC, "dlc");
		}
	}

	/**
	 * Attempt to open a Store at the given path. Returns null if the path
	 * doesn't exist, has zero idx files, or the reader throws. Used by
	 * init() to make the primary -> legacy fallback chain risk-free.
	 */
	private static Store tryLoadStore(String path, String label) {
		if (path == null || path.isEmpty()) return null;
		java.io.File dir = new java.io.File(path);
		if (!dir.isDirectory()) {
			System.err.println("[Cache] " + label + " path missing: " + path);
			return null;
		}
		// Quick sanity: there must be at least one main_file_cache.idx*
		// next to a main_file_cache.dat2. Otherwise alex/store will load
		// a Store with 0 indexes and the next access blows up.
		java.io.File[] files = dir.listFiles((d, n) -> n.startsWith("main_file_cache.idx"));
		java.io.File dat2 = new java.io.File(dir, "main_file_cache.dat2");
		if (!dat2.isFile() || files == null || files.length == 0) {
			System.err.println("[Cache] " + label + " path has no packed cache files: " + path);
			return null;
		}
		try {
			Store s = new Store(path);
			int idxCount = s.getIndexes() == null ? 0 : s.getIndexes().length;
			if (idxCount == 0) {
				System.err.println("[Cache] " + label + " loaded 0 indexes from " + path);
				return null;
			}
			System.out.println("[Cache] " + label + " store loaded from " + path
				+ " (" + idxCount + " indexes)");
			return s;
		} catch (Throwable t) {
			System.err.println("[Cache] " + label + " store load threw (" + path + "): " + t);
			return null;
		}
	}

	/**
	 * Read a file from the primary store, falling through to the DLC store
	 * if the primary returns null AND the fallback is enabled.
	 *
	 * Returns null if both stores miss the file. Throws are propagated from
	 * the primary's getFile() but caught silently for the DLC fallback so a
	 * broken DLC index doesn't crash the primary code path.
	 *
	 * Used by ItemDefinitions/ObjectDefinitions/NPCDefinitions instead of
	 * the bare Cache.STORE.getIndexes()[N].getFile(...) pattern.
	 */
	public static byte[] getFileWithDlcFallback(int indexId, int archiveId, int fileId) {
		byte[] data = null;
		try {
			if (STORE != null && indexId >= 0 && indexId < STORE.getIndexes().length
				&& STORE.getIndexes()[indexId] != null) {
				data = STORE.getIndexes()[indexId].getFile(archiveId, fileId);
			}
		} catch (Throwable t) {
			// Primary read errors propagate as null - caller handles missing data.
		}
		if (data != null) return data;
		if (!Settings.DLC_FALLBACK_ENABLED || STORE_DLC == null) return null;
		try {
			if (indexId < 0 || indexId >= STORE_DLC.getIndexes().length) return null;
			if (STORE_DLC.getIndexes()[indexId] == null) return null;
			return STORE_DLC.getIndexes()[indexId].getFile(archiveId, fileId);
		} catch (Throwable t) {
			return null;
		}
	}


	public static final byte[] generateUkeysFile() {
		OutputStream stream = new OutputStream();
		stream.writeByte(STORE.getIndexes().length);
		for (int index = 0; index < STORE.getIndexes().length; index++) {
			if (STORE.getIndexes()[index] == null) {
				stream.writeInt(0);
				stream.writeInt(0);
				stream.writeInt(0);
				stream.writeInt(0);
				stream.writeBytes(new byte[64]);
				continue;
			}
			stream.writeInt(STORE.getIndexes()[index].getCRC());
			stream.writeInt(STORE.getIndexes()[index].getTable().getRevision());
			stream.writeInt(0);
			stream.writeInt(0);
			stream.writeBytes(STORE.getIndexes()[index].getWhirlpool());
		}
		byte[] archive = new byte[stream.getOffset()];
		stream.setOffset(0);
		stream.getBytes(archive, 0, archive.length);
		OutputStream hashStream = new OutputStream(65);
		hashStream.writeByte(0);
		hashStream.writeBytes(Whirlpool.getHash(archive, 0, archive.length));
		byte[] hash = new byte[hashStream.getOffset()];
		hashStream.setOffset(0);
		hashStream.getBytes(hash, 0, hash.length);
		hash = Utils.cryptRSA(hash, Settings.GRAB_SERVER_PRIVATE_EXPONENT,
				Settings.GRAB_SERVER_MODULUS);
		stream.writeBytes(hash);
		archive = new byte[stream.getOffset()];
		stream.setOffset(0);
		stream.getBytes(archive, 0, archive.length);
		return archive;
	}

}
