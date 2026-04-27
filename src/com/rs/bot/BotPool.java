package com.rs.bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import com.rs.utils.SerializableFilesManager;
import com.rs.utils.SerializationUtilities;

/**
 * Phase 3a: persistent bot pool.
 *
 * Bots are saved to disk as serialized AIPlayer objects in MiniFS at virtual
 * path bots/<name>.bot - same filesystem as clans and GE offers.
 *
 * Two in-memory lists:
 *   offline: names of bots that exist on disk but aren't currently in the world
 *   online:  name -> AIPlayer of bots currently spawned and ticking
 *
 * Lifecycle:
 *   - Server start: scan disk via SerializableFilesManager.listBots(), populate offline
 *   - Generate N: create N fresh bots, save to disk, add names to offline
 *   - Spawn N: pull N names from offline, load AIPlayer from disk, hydrate, add to world, move to online
 *   - Despawn N: serialize state, save to disk, finish() bot, move from online to offline
 *   - Delete: remove from disk + memory entirely
 */
public final class BotPool {

    private static final List<String> offline = new ArrayList<String>();
    private static final Map<String, AIPlayer> online = new HashMap<String, AIPlayer>();
    private static boolean initialized = false;

    private BotPool() {}

    /**
     * Scan disk and populate the offline list. Called once at server startup
     * (and idempotently on admin re-init).
     */
    public static synchronized void initialize() {
        offline.clear();
        // online is left alone - bots already in world stay
        String[] saved = SerializableFilesManager.listBots();
        Set<String> onlineNames = new HashSet<String>(online.keySet());
        for (String name : saved) {
            if (!onlineNames.contains(name)) {
                offline.add(name);
            }
        }
        Collections.sort(offline);
        initialized = true;
        System.out.println("[BotPool] Initialized: " + offline.size() + " offline, " + online.size() + " online");
    }

    /**
     * Generate `count` fresh bots, save them to disk, add to offline list.
     * Returns the number actually created (some may fail name collisions etc).
     */
    public static synchronized int generate(int count) {
        return generate(count, "default", 3);
    }

    public static synchronized int generate(int count, String mode, int targetCombat) {
        return generate(count, mode, targetCombat, "random");
    }

    public static synchronized int generate(int count, String mode, int targetCombat, String archetype) {
        if (!initialized) initialize();
        int created = 0;
        // Resolve "random" once per bot so the skill profile and the equipment
        // loadout agree on what archetype the bot is. Resolving inside both
        // BotSkillProfile.build AND BotFactory.createOffline used to roll the
        // archetype twice independently - bots ended up with magic skills but
        // melee gear, ranged skills with mage robes, etc.
        String[] randomPool = {"melee", "ranged", "magic", "hybrid", "tank", "pure", "main"};
        for (int i = 0; i < count; i++) {
            String name = uniqueName();
            if (name == null) break;
            String resolvedArchetype = archetype;
            if ("random".equalsIgnoreCase(archetype) || archetype == null) {
                resolvedArchetype = randomPool[com.rs.utils.Utils.random(randomPool.length)];
            }
            int[] profile = BotSkillProfile.build(mode, targetCombat, resolvedArchetype);
            AIPlayer bot = BotFactory.createOffline(name, profile, resolvedArchetype);
            if (bot == null) continue;
            byte[] data = SerializationUtilities.tryStoreObject(bot);
            if (data == null || data.length == 0) {
                System.err.println("[BotPool] generate: serialize failed for " + name);
                continue;
            }
            if (!SerializableFilesManager.saveBotData(name, data)) {
                System.err.println("[BotPool] generate: disk save failed for " + name);
                continue;
            }
            offline.add(name);
            created++;
        }
        Collections.sort(offline);
        System.out.println("[BotPool] Generated " + created + " new bots. Total offline: " + offline.size());
        return created;
    }

    /**
     * Spawn `count` bots: pull names from offline, load from disk, hydrate, add to world.
     */
    public static synchronized int spawn(int count) {
        if (!initialized) initialize();
        int spawned = 0;
        for (int i = 0; i < count && !offline.isEmpty(); i++) {
            // pick random for visual variety
            int idx = (int)(Math.random() * offline.size());
            String name = offline.remove(idx);

            AIPlayer bot = loadFromDisk(name);
            if (bot == null) {
                System.err.println("[BotPool] spawn: load failed for " + name + " - dropping from pool");
                continue;
            }

            try {
                // Hydrate transient state and re-enter world. Pass name explicitly
                // because displayName is transient and gets wiped by serialization.
                bot.hydrate(name);   // this calls init() which calls World.addPlayer
                bot.start();
                bot.setBrain(new BotBrain(bot));
                online.put(name, bot);
                spawned++;
                System.out.println("[BotPool] Spawned " + name + " from disk");
            } catch (Throwable t) {
                System.err.println("[BotPool] spawn: hydrate failed for " + name + ": " + t);
                t.printStackTrace();
                offline.add(name); // give the name back
            }
        }
        Collections.sort(offline);
        return spawned;
    }

    /**
     * Despawn all online bots: save state to disk, finish, return to offline list.
     */
    public static synchronized int despawnAll() {
        int despawned = 0;
        for (Map.Entry<String, AIPlayer> e : new ArrayList<Map.Entry<String, AIPlayer>>(online.entrySet())) {
            String name = e.getKey();
            AIPlayer bot = e.getValue();
            try {
                // Save current state BEFORE finishing - so brain delay, position, etc are preserved
                byte[] data = SerializationUtilities.tryStoreObject(bot);
                if (data != null && data.length > 0) {
                    SerializableFilesManager.saveBotData(name, data);
                }
                bot.finish();
                offline.add(name);
                despawned++;
            } catch (Throwable t) {
                System.err.println("[BotPool] despawn: failed for " + name + ": " + t);
                t.printStackTrace();
            }
        }
        online.clear();
        Collections.sort(offline);
        System.out.println("[BotPool] Despawned " + despawned + " bots. Now " + offline.size() + " offline.");
        return despawned;
    }

    /** Save online bots' current state to disk without despawning. */
    public static synchronized int saveOnline() {
        int saved = 0;
        for (Map.Entry<String, AIPlayer> e : online.entrySet()) {
            byte[] data = SerializationUtilities.tryStoreObject(e.getValue());
            if (data != null && data.length > 0
                && SerializableFilesManager.saveBotData(e.getKey(), data)) {
                saved++;
            }
        }
        System.out.println("[BotPool] Saved " + saved + "/" + online.size() + " online bots to disk");
        return saved;
    }

    /** Permanently delete a single bot from disk and memory. Returns true on success. */
    public static synchronized boolean deleteBot(String name) {
        AIPlayer onlineBot = online.remove(name);
        if (onlineBot != null) {
            try { onlineBot.finish(); } catch (Throwable ignored) {}
        }
        offline.remove(name);
        return SerializableFilesManager.deleteBot(name);
    }

    /** Permanently delete ALL bots from disk and memory. Returns count deleted. */
    public static synchronized int deleteAll() {
        // First despawn anything online (without saving)
        for (AIPlayer bot : new ArrayList<AIPlayer>(online.values())) {
            try { bot.finish(); } catch (Throwable ignored) {}
        }
        online.clear();

        String[] all = SerializableFilesManager.listBots();
        int deleted = 0;
        for (String name : all) {
            if (SerializableFilesManager.deleteBot(name)) deleted++;
        }
        offline.clear();
        System.out.println("[BotPool] Deleted ALL bots: " + deleted);
        return deleted;
    }

    private static AIPlayer loadFromDisk(String name) {
        byte[] data = SerializableFilesManager.loadBotData(name);
        if (data == null) return null;
        Object obj = SerializationUtilities.tryLoadObject(data);
        if (!(obj instanceof AIPlayer)) {
            System.err.println("[BotPool] loaded object for " + name + " is not AIPlayer: " + (obj == null ? "null" : obj.getClass().getName()));
            return null;
        }
        return (AIPlayer) obj;
    }

    private static String uniqueName() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = BotNames.generate();
            if (!SerializableFilesManager.containsBot(candidate)
                && !offline.contains(candidate)
                && !online.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    // ===== Accessors =====
    public static synchronized boolean isInitialized() { return initialized; }
    public static synchronized int getOnlineCount() { return online.size(); }
    public static synchronized int getOfflineCount() { return offline.size(); }
    public static synchronized int getTotalCount() { return online.size() + offline.size(); }

    public static synchronized List<AIPlayer> snapshotOnline() {
        return new ArrayList<AIPlayer>(online.values());
    }

    public static synchronized List<String> snapshotOnlineNames() {
        List<String> names = new ArrayList<String>(online.keySet());
        Collections.sort(names);
        return names;
    }

    public static synchronized List<String> snapshotOfflineNames() {
        return new ArrayList<String>(offline);
    }

	public static synchronized List<String> snapshotAllNames() {
		java.util.List<String> all = new java.util.ArrayList<String>();
		all.addAll(online.keySet());
		all.addAll(offline);
		java.util.Collections.sort(all);
		return all;
	}

	public static synchronized boolean isOnline(String name) {
		return online.containsKey(name);
	}

	public static synchronized boolean spawnByName(String name) {
		if (!initialized) initialize();
		if (online.containsKey(name)) return false;
		if (!offline.contains(name)) return false;
		AIPlayer bot = loadFromDisk(name);
		if (bot == null) {
			System.err.println("[BotPool] spawnByName: load failed for " + name);
			return false;
		}
		try {
			bot.hydrate(name);
			bot.start();
			bot.setBrain(new BotBrain(bot));
			offline.remove(name);
			online.put(name, bot);
			System.out.println("[BotPool] Spawned " + name + " by name");
			return true;
		} catch (Throwable t) {
			System.err.println("[BotPool] spawnByName: hydrate failed for " + name + ": " + t);
			t.printStackTrace();
			offline.add(name);
			return false;
		}
	}

	public static synchronized boolean despawnByName(String name) {
		com.rs.bot.AIPlayer ai = online.remove(name);
		if (ai == null) return false;
		try {
			byte[] data = com.rs.utils.SerializationUtilities.tryStoreObject(ai);
			if (data != null && data.length > 0) {
				com.rs.utils.SerializableFilesManager.saveBotData(name, data);
			}
		} catch (Throwable t) { t.printStackTrace(); }
		offline.add(name);
		try { ai.finish(); } catch (Throwable t) { t.printStackTrace(); }
		return true;
	}
}
