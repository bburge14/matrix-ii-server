package com.rs.game.player.content.preset;

import java.io.Serializable;
import java.util.HashMap;

import com.rs.cache.loaders.ItemDefinitions;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.utils.Logger;
import com.rs.game.player.Inventory;

/**
 * @author Andy || ReverendDread Feb 13, 2017
 * 
 * This Class handles all interaction for Bank Presets, similar to rs3. 
 * 
 */
public class PresetManager implements Serializable {


	private static final long serialVersionUID = -3797732710233164287L;

	/**
	 * {@link Player} the player
	 */
	private Player player;
	
	/**
	 * Map containing all equipment presets.
	 */
	private HashMap<Integer, Item[]> equipmentPresets;
	
	/**
	 * Map containing all inventory presets.
	 */
	private HashMap<Integer, Item[]> inventoryPresets;
	
	/**
	 * Constructs a new object.
	 */
	public PresetManager(Player player) {
		this.player = player;
		if (equipmentPresets == null)
			equipmentPresets = new HashMap<Integer, Item[]>();
		if (inventoryPresets == null) {
			inventoryPresets = new HashMap<Integer, Item[]>();
		}
	}

	/**
	 * The max number of preset types allowed per type.
	 */
	public static final int MAX_SIZE = 5;
	
	/**
	 * Saves a preset to specified slot.
	 * @param id preset id
	 */
	public void savePreset(int id) {
		
		if (getInventoryPresets().size() >= MAX_SIZE) {
			sendMessage("You can't have more than " + MAX_SIZE + " inventory presets.");
			return;
		}
		// the id the player uses has to be between 1 and 5.
		if (id < 1 || id > MAX_SIZE) {
			sendMessage("Incorrect preset value saved, please contact an Administrator.");
			return;
		}
		
		getEquipmentPresets().put(id, player.getEquipment().getItems().getItemsCopy());
		getInventoryPresets().put(id, player.getInventory().getItems().getItemsCopy());
		
		sendMessage("Saved items to preset " + id + ".");
	}
	
	/**
	 * Loads a preset from specified slot.
	 * @param id preset id
	 */
	public void loadPreset(int id) {
		
		if (getEquipmentPresets().isEmpty() || getInventoryPresets().isEmpty()) {
			sendMessage("Failed to load preset. No items were saved into slot " + id + ".");
			return;
		}
		
		if (!getEquipmentPresets().containsKey(id) || !getInventoryPresets().containsKey(id)) {
			sendMessage("Failed to load preset. You first must have a preset saved in slot " + id + ".");
			return;
		}
		// the id the player uses has to be between 1 and 5.
		if (id < 1 || id > MAX_SIZE) {
			sendMessage("Incorrect preset value loaded, please contact an Administrator.");
			return;
		}
		
		//Deposits all of inventory & equipment into bank.
		player.getBank().depositAllInventory(true);
		player.getBank().depositAllEquipment(true);
		
		//if player has items in their inventory, dont continue
		if (player.getInventory().getFreeSlots() < Inventory.SIZE) {
			sendMessage("Please deposit your inventory before loading a preset.");
			return;
		}
		
		loadInventoryPreset(id);
		loadEquipmentPreset(id);
		
		sendMessage("Loaded items from preset " + id + ".");
	}
	
	private void loadInventoryPreset(int id) {	
		//sets the inventory items to the preset items
		Item[] container = getInventoryPresets().get(id);
		//purely for showing how many items where loaded
		int inventoryCount = 0;
		//loops through items
		for (Item item : container) {
			if (item == null)
				continue;
			//Checks if player has item in their bank.
			if (!player.getBank().containsItem(item.getId(), item.getAmount())) { //if not skip this loop.
				sendMessage("Problem loading " + item.getName() + "...");
				continue;
			}
			player.getInventory().addItem(item);
			player.getBank().removeItem(item);
			inventoryCount++;
		}
		player.getInventory().refresh();
		
		sendMessage("Loaded " + inventoryCount + " items from inventory preset.");
	}
	
	private void loadEquipmentPreset(int id) {
		//sets the equipment items to the preset items
		Item[] equipment = getEquipmentPresets().get(id);
		//purely used for showing how many items were loaded.
		int equipmentCount = 0;
		//loops through item array from preset map
		for (Item item : equipment) {
			if (item == null)
				continue;
			//gets equip slot for item
			int equipSlot = ItemDefinitions.getItemDefinitions(item.getId()).getEquipSlot(); 
			//checks if player has item in bank.
			if (!player.getBank().containsItem(item.getId(), item.getAmount())) { //if not skip this loop.
				sendMessage("Problem loading item " + item.getName() + "...");
				continue;
			}
			player.getEquipment().getItems().set(equipSlot, item); //sets slot to item.
			player.getBank().removeItem(item); //removes item from bank.
			player.getEquipment().refresh(equipSlot); //refreshes equipment slot.
			equipmentCount++;
		}
		//refreshes appearence after equipment is loaded into slots.
		player.getAppearence().generateAppearenceData();
		
		sendMessage("Loaded " + equipmentCount + " items from equipment preset.");
	}
	
	/**
	 * Removes a preset from specified slot.
	 * @param id
	 */
	public void removePreset(int id) {
		// if the player has no saved inventory presets, there's nothing to remove.
		if (getEquipmentPresets().isEmpty() || getInventoryPresets().isEmpty()) {
			sendMessage("You have no presets currently saved.");
			return;
		}
		// the id the player uses has to be between 1 and MAX_SIZE
		if (id < 1 || id > MAX_SIZE) {
			sendMessage("Incorrect preset value removed, please contact an Administrator.");
			return;
		}
		// if the player doesnt have a preset saved for the id, don't do anything.
		if (!getEquipmentPresets().containsKey(id)) {
			sendMessage("No equipment preset saved for slot " + id);
			return;
		}
		// if the player doesnt have a preset saved for the id, don't do anything.
		if (!getInventoryPresets().containsKey(id)) {
			sendMessage("No inventory preset saved for slot " + id);
			return;
		}
		//remove entrys from map
		getEquipmentPresets().remove(id);
		getInventoryPresets().remove(id);
		
		sendMessage("Preset removed from slot " + id);		
	}
	
	/**
	 * Sends preset class messages with predefined tag.
	 * @param message
	 */
	private void sendMessage(String message) {
		player.sendMessage("<col=ff0000>[Presets]: " + message);
	}
		
	/**
	 * Gets the player.
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * Sets the player.
	 * @param player the player
	 */
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	/**
	 * Gets the Equipment presets.
	 * @return equipment presets map.
	 */
	private HashMap<Integer, Item[]> getEquipmentPresets() {
		return equipmentPresets;
	}
	
	/**
	 * Gets the Inventory presets map.
	 * @return inventory presets map.
	 */
	private HashMap<Integer, Item[]> getInventoryPresets() {
		return inventoryPresets;
	}
	
	
}
