package com.rs.game.player.dialogues.impl;

import com.rs.game.npc.NPC;
import com.rs.game.player.dialogues.Dialogue;
import com.rs.utils.Utils;

/**
 * Default fallback dialogue for any NPC without a dedicated handler.
 * Replaces the "Nothing interesting happens." message with a friendly
 * one-liner based on the NPC's name. Players still get the feel of
 * "talking to" the NPC without us having to write 5000 dialogue files.
 */
public class GenericTalk extends Dialogue {

	private NPC npc;

	@Override
	public void start() {
		npc = (NPC) parameters[0];
		String name = "";
		try {
			name = npc.getDefinitions().name == null ? "" : npc.getDefinitions().name.toLowerCase();
		} catch (Throwable ignore) {}

		String line = pickLine(name);
		int emote = pickEmote(name);
		sendNPCDialogue(npc.getId(), emote, line);
		stage = 99;
	}

	@Override
	public void run(int interfaceId, int componentId) {
		end();
	}

	@Override
	public void finish() {
	}

	private static String pickLine(String name) {
		if (name.contains("banker") || name.contains("clerk"))
			return "Click on the bank booth to open your account.";
		if (name.contains("smith") || name.contains("forge"))
			return "I forge weapons and armour. Talk to a shopkeeper to buy.";
		if (name.contains("shopkeep") || name.contains("merchant") || name.contains("store"))
			return "Welcome. Browse my wares using the shop interface.";
		if (name.contains("guard") || name.contains("soldier"))
			return "Move along, citizen. Nothing to see here.";
		if (name.contains("wizard") || name.contains("mage"))
			return "I'm studying magic. Please don't disturb me.";
		if (name.contains("knight") || name.contains("paladin"))
			return "Honour and glory to all who fight evil!";
		if (name.contains("monk") || name.contains("priest"))
			return "May the gods guide your path, traveller.";
		if (name.contains("dwarf"))
			return "Aye, what brings ye to me corner o' the realm?";
		if (name.contains("gnome"))
			return "I am a noble gnome. State your business briefly.";
		if (name.contains("elf"))
			return "Greetings, surface dweller.";
		if (name.contains("farmer") || name.contains("gardener"))
			return "Crops are coming in nicely this season.";
		if (name.contains("fisher") || name.contains("sailor"))
			return "The sea has been good to us this week.";
		if (name.contains("hunter"))
			return "Tracking is an art most have forgotten.";
		if (name.contains("slayer"))
			return "Got a slayer task? Keep at it.";
		if (name.contains("cook") || name.contains("chef"))
			return "Mind the stove! I won't be responsible if you burn yourself.";
		if (name.contains("tanner") || name.contains("tailor"))
			return "Best tanned leather in the kingdom, friend.";
		if (name.contains("miner"))
			return "There's good ore in these hills - if you know where to look.";
		if (name.contains("woodcutter") || name.contains("lumberjack"))
			return "Sharp axe makes light work of any tree.";
		if (name.contains("man") || name.contains("woman") || name.contains("villager")
				|| name.contains("citizen") || name.contains("peasant")) {
			String[] generic = {
				"Hello there.",
				"Nice weather we're having.",
				"Have you been to the Grand Exchange lately?",
				"I heard there's good adventuring up north.",
				"My back hurts from working the fields all day.",
				"Have you seen the lodestones? Marvels of magic.",
				"Watch out for thieves around here.",
				"My daughter wants to be an adventurer. Crazy kid.",
				"They say a new boss has been sighted in the wilderness."
			};
			return generic[Utils.random(generic.length)];
		}
		// Catch-all for any NPC we didn't pattern-match
		String[] genericFallback = {
			"Hello, traveller.",
			"Greetings.",
			"Move along now.",
			"Need something?",
			"Busy day today.",
			"Watch where you're going!",
			"What brings you here?"
		};
		return genericFallback[Utils.random(genericFallback.length)];
	}

	private static int pickEmote(String name) {
		if (name.contains("guard") || name.contains("soldier") || name.contains("knight"))
			return 9836; // stern
		if (name.contains("monk") || name.contains("priest"))
			return 9851; // peaceful
		if (name.contains("wizard") || name.contains("mage"))
			return 9810; // quizzical
		return 9827; // default happy
	}
}
