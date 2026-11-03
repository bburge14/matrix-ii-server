package com.rs.game.player.anticheat.botcracker;

import java.util.ArrayList;
import java.util.List;

import com.rs.game.World;
import com.rs.game.player.Player;
import com.rs.utils.Utils;

/**
 * {@code ::botcracker <sub-command>} handling for admins.
 *
 * <pre>
 * ::botcracker status                 queue size, config, online tracked
 * ::botcracker report <name>          live telemetry + findings for an online player
 * ::botcracker eval <name>            force an evaluation now
 * ::botcracker queue                  list shadow profiles
 * ::botcracker wave preview           list who the next wave would action
 * ::botcracker wave run               execute the ban wave now
 * ::botcracker clear <name>           drop a shadow profile
 * ::botcracker whitelist <name>       exempt an account from waves
 * ::botcracker set <key> <value>      tweak a BotCrackerConfig field (ENABLED, BAN_THRESHOLD, ...)
 * ::botcracker raw on|off             toggle raw telemetry logging
 * </pre>
 */
public final class BotCrackerCommands {

	private BotCrackerCommands() {
	}

	/** @return true if the command was consumed. */
	public static boolean handle(Player player, String[] cmd) {
		if (cmd.length == 0 || !cmd[0].equalsIgnoreCase("botcracker"))
			return false;
		if (player.getRights() < 2) {
			player.getPackets().sendGameMessage("Admin+ only!");
			return true;
		}
		BotCracker bc = BotCracker.get();
		String sub = cmd.length > 1 ? cmd[1].toLowerCase() : "status";
		String arg = join(cmd, 2);
		switch (sub) {
		case "status": {
			int queued = bc.getShadowProfiles().all().size();
			msg(player, String.format("BotCracker %s | tracked=%d shadowed=%d | shadow>=%.2f ban>=%.2f (%d sources) | wave period=%dms raw=%s",
					BotCrackerConfig.ENABLED ? "ON" : "OFF", bc.getOnlineTelemetry().size(), queued,
					BotCrackerConfig.SHADOW_THRESHOLD, BotCrackerConfig.BAN_THRESHOLD, BotCrackerConfig.BAN_MIN_SOURCES,
					BotCrackerConfig.BAN_WAVE_PERIOD_MS, BotCrackerConfig.RAW_TELEMETRY_LOG));
			return true;
		}
		case "report":
		case "eval": {
			Player target = World.getPlayerByDisplayName(arg);
			if (target == null) {
				msg(player, "Player not online: " + arg);
				return true;
			}
			if (sub.equals("eval"))
				bc.evaluateNow(target);
			PlayerTelemetry t = bc.getTelemetry(target.getUsername());
			if (t == null) {
				msg(player, target.getDisplayName() + " is not tracked (staff or AI).");
				return true;
			}
			for (String line : TelemetryLogger.report(t, Utils.currentTimeMillis()).split("\n"))
				msg(player, line);
			ShadowProfile sp = bc.getShadowProfiles().get(target.getUsername());
			if (sp != null)
				msg(player, "Shadow: " + sp);
			return true;
		}
		case "queue": {
			List<ShadowProfile> all = new ArrayList<ShadowProfile>(bc.getShadowProfiles().all());
			msg(player, all.size() + " shadow profile(s):");
			for (ShadowProfile p : all)
				msg(player, "  " + p);
			return true;
		}
		case "wave": {
			boolean run = arg.equalsIgnoreCase("run");
			List<String> hit = bc.getShadowProfiles().executeBanWave(!run);
			msg(player, (run ? "Ban wave actioned " : "Ban wave would action ") + hit.size() + ": " + hit);
			return true;
		}
		case "clear": {
			msg(player, bc.getShadowProfiles().remove(Utils.formatPlayerNameForProtocol(arg)) ? "Cleared." : "No profile for " + arg);
			return true;
		}
		case "whitelist": {
			bc.getShadowProfiles().whitelist(Utils.formatPlayerNameForProtocol(arg), "by " + player.getUsername());
			msg(player, "Whitelisted " + arg);
			return true;
		}
		case "raw": {
			BotCrackerConfig.RAW_TELEMETRY_LOG = arg.equalsIgnoreCase("on");
			msg(player, "Raw telemetry logging " + (BotCrackerConfig.RAW_TELEMETRY_LOG ? "on" : "off"));
			return true;
		}
		case "set": {
			if (cmd.length < 4) {
				msg(player, "Usage: ::botcracker set <FIELD> <value>");
				return true;
			}
			msg(player, set(cmd[2], cmd[3]));
			return true;
		}
		default:
			msg(player, "Sub-commands: status, report, eval, queue, wave [preview|run], clear, whitelist, raw, set");
			return true;
		}
	}

	private static String set(String field, String value) {
		try {
			java.lang.reflect.Field f = BotCrackerConfig.class.getField(field.toUpperCase());
			Class<?> type = f.getType();
			if (type == boolean.class)
				f.setBoolean(null, Boolean.parseBoolean(value));
			else if (type == int.class)
				f.setInt(null, Integer.parseInt(value));
			else if (type == long.class)
				f.setLong(null, Long.parseLong(value));
			else if (type == double.class)
				f.setDouble(null, Double.parseDouble(value));
			else if (type == String.class)
				f.set(null, value);
			else
				return "Unsupported type for " + field;
			return field.toUpperCase() + " = " + f.get(null);
		} catch (NoSuchFieldException e) {
			return "No such config field: " + field;
		} catch (Exception e) {
			return "Could not set " + field + ": " + e.getMessage();
		}
	}

	private static String join(String[] cmd, int from) {
		StringBuilder sb = new StringBuilder();
		for (int i = from; i < cmd.length; i++)
			sb.append(i > from ? " " : "").append(cmd[i]);
		return sb.toString();
	}

	private static void msg(Player p, String s) {
		p.getPackets().sendGameMessage(s);
	}
}
