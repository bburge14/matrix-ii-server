package com.rs.game.player.dialogues;

public class AllAltars extends Dialogue {

	@Override
	public void start() {
		sendOptionsDialogue("Select an Option", "Lunar Spells", "Normal Spells", "Ancient Spells", "Curse Prayers", "Normal Prayers");
	}

	@Override
	public void run(int interfaceId, int componentId) {
		if (componentId == OPTION_1) {
			player.getPackets().sendGameMessage("Your mind clears and you switch to the Lunar spell book.");
			player.getCombatDefinitions().setSpellBook(2);
		}
		if (componentId == OPTION_2) {
			player.getPackets().sendGameMessage("Your mind clears and you switch back to Normal spells.");
			player.getCombatDefinitions().setSpellBook(0);
		}
		if (componentId == OPTION_3) {
			player.getPackets().sendGameMessage("Your mind clears and you switch to the Ancient spells.");
			player.getCombatDefinitions().setSpellBook(1);
		}
		if (componentId == OPTION_4) {
			player.getPrayer().setPrayerBook(true);
			player.getPackets().sendGameMessage("The altar fills your head with dark thoughts purging the prayers from your memory and leaving only curses in their place.");
		}
		if (componentId == OPTION_5) {
			player.getPrayer().setPrayerBook(false);
			player.getPackets().sendGameMessage("The altar eases its grip on your mid. The curses slip from your memory and you recall the prayers you used to know.");
		}
		end();
	}

	@Override
	public void finish() {

	}

}