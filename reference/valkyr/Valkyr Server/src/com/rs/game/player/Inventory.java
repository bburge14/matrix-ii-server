package com.rs.game.player;

import java.io.Serializable;
import java.util.List;

import com.rs.cache.loaders.ItemDefinitions;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.item.Item;
import com.rs.game.item.ItemsContainer;
import com.rs.utils.ItemExamines;
import com.rs.utils.Utils;

public final class Inventory implements Serializable {

	private static final long serialVersionUID = 8842800123753277093L;

	private ItemsContainer<Item> items;

	private transient Player player;

	public static final int INVENTORY_INTERFACE = 679;

	public static final int SIZE = 28;

	public Inventory() {
		items = new ItemsContainer<Item>(SIZE, false);
	}

	public void setPlayer(Player player) {
		this.player = player;
	}
	
    public boolean containsItemToolBelt(int id) {
	return containsOneItem(id) || player.getToolbelt().containsItem(id);
    }
    

    public boolean containsItemToolBelt(int id, int amount) {
	return containsItem(id, amount) || player.getToolbelt().containsItem(id);
    }

	public void init() {
		player.getPackets().sendItems(93, items);
	}

	public void unlockInventoryOptions() {
		player.getPackets().sendIComponentSettings(INVENTORY_INTERFACE, 0, 0,
				27, 4554126);
		player.getPackets().sendIComponentSettings(INVENTORY_INTERFACE, 0, 28,
				55, 2097152);
	}
	
	public boolean contains(Item... items) {
		for (Item item : items) {
			if (item != null && !containsItem(item.getId(), item.getAmount())) {
				return false;
			}
		}
		return true;
	}
	
    public boolean containsItems(List<Item> list) {
	for (Item item : list)
	    if (!items.contains(item))
		return false;
	return true;
    }


	public void reset() {
		items.reset();
		init(); // as all slots reseted better just send all again
	}


    public boolean removeItems(List<Item> list) {
	for (Item item : list) {
	    if (item == null)
		continue;
	    deleteItem(item);
	}
	return true;
    }

	public void refresh(int... slots) {
		player.getPackets().sendUpdateItems(93, items, slots);
	}

	public boolean addItem(int itemId, int amount) {
		if (itemId < 0 || amount < 0 || !Utils.itemExists(itemId) || !player.getControlerManager().canAddInventoryItem(itemId, amount))
		    return false;
		Item[] itemsBefore = items.getItemsCopy();
		if (!items.add(new Item(itemId, amount))) {
			items.add(new Item(itemId, items.getFreeSlots()));
			player.getPackets().sendGameMessage(
					"Not enough space in your inventory.");
			refreshItems(itemsBefore);
			return false;
		}
		refreshItems(itemsBefore);
		return true;
	}
	
	

	public boolean addItem(Item item) {
		if (item.getId() < 0 || item.getAmount() < 0 || !Utils.itemExists(item.getId()) || !player.getControlerManager().canAddInventoryItem(item.getId(), item.getAmount()))
		    return false;
		Item[] itemsBefore = items.getItemsCopy();
		if (!items.add(item)) {
			items.add(new Item(item.getId(), items.getFreeSlots()));
			player.getPackets().sendGameMessage(
					"Not enough space in your inventory.");
			refreshItems(itemsBefore);
			return false;
		}
		refreshItems(itemsBefore);
		return true;
	}

	public void deleteItem(int slot, Item item) {
		if (!player.getControlerManager().canDeleteInventoryItem(item.getId(),
				item.getAmount()))
			return;
		Item[] itemsBefore = items.getItemsCopy();
		items.remove(slot, item);
		refreshItems(itemsBefore);
	}

	public boolean removeItems(Item... list) {
		for (Item item : list)  {
			if(item == null)
				continue;
				deleteItem(item);
		}
		return true;
	}
	
	public void deleteItem(int itemId, int amount) {
		if (!player.getControlerManager()
				.canDeleteInventoryItem(itemId, amount))
			return;
		Item[] itemsBefore = items.getItemsCopy();
		items.remove(new Item(itemId, amount));
		refreshItems(itemsBefore);
	}

	public void deleteItem(Item item) {
		if (!player.getControlerManager().canDeleteInventoryItem(item.getId(),
				item.getAmount()))
			return;
		Item[] itemsBefore = items.getItemsCopy();
		items.remove(item);
		refreshItems(itemsBefore);
	}

	/*
	 * No refresh needed its client to who does it :p
	 */
	public void switchItem(int fromSlot, int toSlot) {
		Item[] itemsBefore = items.getItemsCopy();
		Item fromItem = items.get(fromSlot);
		Item toItem = items.get(toSlot);
		items.set(fromSlot, toItem);
		items.set(toSlot, fromItem);
		refreshItems(itemsBefore);
	}

	public void refreshItems(Item[] itemsBefore) {
		int[] changedSlots = new int[itemsBefore.length];
		int count = 0;
		for (int index = 0; index < itemsBefore.length; index++) {
			if (itemsBefore[index] != items.getItems()[index])
				changedSlots[count++] = index;
		}
	int[] finalChangedSlots = new int[count];
		System.arraycopy(changedSlots, 0, finalChangedSlots, 0, count);
		refresh(finalChangedSlots);
	}
	

	public ItemsContainer<Item> getItems() {
		return items;
	}

	public boolean hasFreeSlots() {
		return items.getFreeSlot() != -1;
	}

	public int getFreeSlots() {
		return items.getFreeSlots();
	}
	
	public int getNumerOf(int itemId) {
		return items.getNumberOf(itemId);
	}

	public Item getItem(int slot) {
		return items.get(slot);
	}

	public int getItemsContainerSize() {
		return items.getSize();
	}

	public boolean containsItems(Item... item) {
		for (int i = 0; i < item.length; i++)
			if (!items.contains(item[i]))
				return false;
		return true;
	}

	public boolean containsItems(int[] itemIds, int[] ammounts) {
		int size = itemIds.length > ammounts.length ? ammounts.length
				: itemIds.length;
		for (int i = 0; i < size; i++)
			if (!items.contains(new Item(itemIds[i], ammounts[i])))
				return false;
		return true;
	}

	public boolean containsItem(int itemId, int ammount) {
		if (itemId == 995) {
			if (getCoinsAmount() >= ammount)
				return true;
			else
				return false;
		}
		return items.contains(new Item(itemId, ammount));
	}

	public boolean containsOneItem(int... itemIds) {
		for (int itemId : itemIds) {
			if (items.containsOne(new Item(itemId, 1)))
				return true;
		}
		return false;
	}

	public void sendExamine(int slotId) {
		if (slotId >= getItemsContainerSize())
			return;
		Item item = items.get(slotId);
		if (item == null)
			return;
		player.getPackets().sendInventoryMessage(0, slotId, ItemExamines.getExamine(item));
	}
	
	public void refresh() {
		player.getPackets().sendItems(93, items);
	}

	/**
	 * 
	 * Used For Pouch
	 */
	public boolean addCoins(int amount) {
		if (amount < 0)
			return false;
		Item[] itemsBefore = items.getItemsCopy();
		if (!items.add(new Item(995, amount))) {
			items.add(new Item(995, items.getFreeSlots()));
			player.getPackets().sendGameMessage(
					"Not enough space in your inventory.");
			refreshItems(itemsBefore);
			return false;
		}
		refreshItems(itemsBefore);
		return true;
	}
	
    public boolean addItemDrop(int itemId, int amount) {
    	return addItemDrop(itemId, amount, new WorldTile(player));
    }
	
    public boolean addItemDrop(int itemId, int amount, WorldTile tile) {
		if (itemId < 0 || amount < 0 || !Utils.itemExists(itemId) || !player.getControlerManager().canAddInventoryItem(itemId, amount))
		    return false;
		if (itemId == 995)
		    return player.getMoneyPouch().sendDynamicInteraction(amount, false);
		Item[] itemsBefore = items.getItemsCopy();
		if (!items.add(new Item(itemId, amount)))
		    World.addGroundItem(new Item(itemId, amount), tile, player, true, 180);
		else
		    refreshItems(itemsBefore);
		return true;
    }
    
    public int getAmountOf(int itemId) {
    	return items.getNumberOf(itemId);
    }
    
    public void replaceItem(int id, int amount, int slot) {
		Item item = items.get(slot);
		if (item == null)
		    return;
		item.setId(id);
		item.setAmount(amount);
		refresh(slot);
    }
    
    public boolean addItemMoneyPouch(Item item) {
		if (item.getId() == 995)
		    return player.getMoneyPouch().sendDynamicInteraction(item.getAmount(), false);
		return addItem(item);
    }

    public boolean removeItemMoneyPouch(Item item) {
		if (item.getId() == 995)
		    return player.getMoneyPouch().sendDynamicInteraction(item.getAmount(), true);
		return removeItems(item);
    }
    
    public boolean removeItemMoneyPouch(int id, int amount) {
		if (id == 995)
		    return player.getMoneyPouch().sendDynamicInteraction(amount, true);
		return false;
    }
    
    public boolean addItemMoneyPouch(int id, int amount) {
		if (id == 995)
		    return player.getMoneyPouch().sendDynamicInteraction(amount, false);
		return false;
    }
    
    public int getCoinsAmount() {
    	int coins = items.getNumberOf(995) + player.getMoneyPouch().getCoinsAmount();
    	return coins < 0 ? Integer.MAX_VALUE : coins;
    }
    
	public static void replaceItems(Player player, int[] items, int[] newItems) {
		if (items.length != newItems.length)
			return;
		for (int idx = 0; idx < items.length; idx++) {
			int orgId = items[idx], newId = newItems[idx];
			int amt = player.getInventory().getAmountOf(orgId);
			if (amt > 0) {
				player.getInventory().deleteItem(orgId, amt);
				player.getInventory().addItem(new Item(newItems[idx], amt));
			}
		}
	}
	
	/**
	 * Adds a list of {@link Item}'s
	 * @param list the items
	 * @return 
	 */
	public boolean addItems(Item... list) {
		for (Item item : list)  {
			if(item == null)
				continue;
			addItem(item);
		}
		return true;
	}


}
