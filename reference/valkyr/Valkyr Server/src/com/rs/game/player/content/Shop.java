package com.rs.game.player.content;

import java.util.concurrent.CopyOnWriteArrayList;

import com.rs.Settings;
import com.rs.cache.loaders.ClientScriptMap;
import com.rs.cache.loaders.ItemDefinitions;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.utils.ItemExamines;
import com.rs.utils.ItemSetsKeyGenerator;

public class Shop {
	
	public static int[][] DungPrices = { { 18349, 200000 }, { 18351, 200000 },
	{ 18353, 200000 }, { 18355, 200000 }, { 18357, 200000 },
	{ 18359, 200000 }, { 25991, 100000 }, { 25993, 100000 }, { 25995, 100000 },
	{ 27068, 20000 }, { 18337, 34000 }, { 19669, 50000 }, 
	{ 31449, 100000 }, { 19894, 85000 }, { 19888, 35000 },
	{ 18335, 100000}, { 18361, 200000}, { 18363, 200000 }, { 27996, 100000}, { 19675, 70000}, { 18340, 70000}  };
	
	public static int[][] PestPrices = { { 11663, 200 }, { 11664, 200 },
	{ 11665, 200 }, { 8842, 150 }, { 8840, 250 },
	{ 8839, 250 }, { 19782, 350 }, { 19785, 350 }, { 19712, 300 }, {31640, 350}  };

    private static final int MAIN_STOCK_ITEMS_KEY = ItemSetsKeyGenerator.generateKey();

    private static final int MAX_SHOP_ITEMS = 40;
    public static final int COINS = 995, TOKKUL = 6529;

    private String name;
    private Item[] mainStock;
    private int[] defaultQuantity;
    private Item[] generalStock;
    private int money;
    private CopyOnWriteArrayList<Player> viewingPlayers;

    public Shop(String name, int money, Item[] mainStock, boolean isGeneralStore) {
	viewingPlayers = new CopyOnWriteArrayList<Player>();
	this.name = name;
	this.money = money;
	this.mainStock = mainStock;
	defaultQuantity = new int[mainStock.length];
	for (int i = 0; i < defaultQuantity.length; i++)
	    defaultQuantity[i] = mainStock[i].getAmount();
	if (isGeneralStore && mainStock.length < MAX_SHOP_ITEMS)
	    generalStock = new Item[MAX_SHOP_ITEMS - mainStock.length];
    }

    public boolean isGeneralStore() {
	return generalStock != null;
    }

    public void addPlayer(final Player player) {
	viewingPlayers.add(player);
	player.getTemporaryAttributtes().put("Shop", this);
	player.setCloseInterfacesEvent(new Runnable() {
	    @Override
	    public void run() {
		viewingPlayers.remove(player);
		player.getTemporaryAttributtes().remove("Shop");
		player.getTemporaryAttributtes().remove("shop_transaction");
		player.getTemporaryAttributtes().remove("isShopBuying");
		player.getTemporaryAttributtes().remove("ShopSelectedSlot");
		player.getTemporaryAttributtes().remove("ShopSelectedInventory");
	    }
	});
	player.refreshVerboseShopDisplayMode();
	player.getVarsManager().sendVar(118, generalStock != null ? 139 : MAIN_STOCK_ITEMS_KEY); 
	player.getVarsManager().sendVar(1496, -1); // sample items container id (TODO: add support for it)
	player.getVarsManager().sendVar(532, money);
	resetSelected(player);
	sendStore(player);
	player.getInterfaceManager().sendInterface(1265); // opens shop
	resetTransaction(player);
	setBuying(player, true);
	if (generalStock != null)
	    player.getPackets().sendHideIComponent(1265, 19, false); // unlocks general store icon
	player.getPackets().sendIComponentSettings(1265, 20, 0, getStoreSize(), 1150); // unlocks stock slots
	sendInventory(player);
	player.getPackets().sendIComponentText(1265, 85, name);
    }

    public void resetTransaction(Player player) {
	setTransaction(player, 1);
    }

    public void increaseTransaction(Player player, int amount) {
	setTransaction(player, getTransaction(player) + amount);
    }

    public int getTransaction(Player player) {
	Integer transaction = (Integer) player.getTemporaryAttributtes().get("shop_transaction");
	return transaction == null ? 1 : transaction;
    }

    public void pay(Player player) {
	Integer selectedSlot = (Integer) player.getTemporaryAttributtes().get("ShopSelectedSlot");
	Boolean inventory = (Boolean) player.getTemporaryAttributtes().get("ShopSelectedInventory");
	if (selectedSlot == null || inventory == null)
	    return;
	int amount = getTransaction(player);
	if (inventory)
	    sell(player, selectedSlot, amount);
	else
	    buy(player, selectedSlot, amount);
    }

    public int getSelectedMaxAmount(Player player) {
	Integer selectedSlot = (Integer) player.getTemporaryAttributtes().get("ShopSelectedSlot");
	Boolean inventory = (Boolean) player.getTemporaryAttributtes().get("ShopSelectedInventory");
	if (selectedSlot == null || inventory == null)
	    return 1;
	if (inventory) {
	    Item item = player.getInventory().getItem(selectedSlot);
	    if (item == null)
		return 1;
	    return player.getInventory().getAmountOf(item.getId());
	} else {
	    if (selectedSlot >= getStoreSize())
		return 1;
	    Item item = selectedSlot >= mainStock.length ? generalStock[selectedSlot - mainStock.length] : mainStock[selectedSlot];
	    if (item == null)
		return 1;
	    return item.getAmount();
	}
    }

    public void setTransaction(Player player, int amount) {
	int max = getSelectedMaxAmount(player);
	if (amount > max)
	    amount = max;
	else if (amount < 1)
	    amount = 1;
	player.getTemporaryAttributtes().put("shop_transaction", amount);
	player.getVarsManager().sendVar(2564, amount);
    }

    public static void setBuying(Player player, boolean buying) {
	player.getTemporaryAttributtes().put("isShopBuying", buying);
	player.getVarsManager().sendVar(2565, buying ? 0 : 1);
    }

    public static boolean isBuying(Player player) {
	Boolean isBuying = (Boolean) player.getTemporaryAttributtes().get("isShopBuying");
	return isBuying != null && isBuying;
    }

    public void sendInventory(Player player) {
	player.getInterfaceManager().sendInventoryInterface(1266);
	player.getPackets().sendItems(93, player.getInventory().getItems());
	player.getPackets().sendUnlockIComponentOptionSlots(1266, 0, 0, 27, 0, 1, 2, 3, 4, 5);
	player.getPackets().sendInterSetItemsOptionsScript(1266, 0, 93, 4, 7, "Value", "Sell 1", "Sell 5", "Sell 10", "Sell 50", "Examine");
    }

    public void buyAll(Player player, int slotId) {
	if (slotId >= getStoreSize())
	    return;
	Item item = slotId >= mainStock.length ? generalStock[slotId - mainStock.length] : mainStock[slotId];
	buy(player, slotId, item.getAmount());
    }

    public void buy(Player player, int slotId, int quantity) {
	if (slotId >= getStoreSize())
	    return;
	Item item = slotId >= mainStock.length ? generalStock[slotId - mainStock.length] : mainStock[slotId];
	if (item == null)
	    return;
	if (item.getAmount() == 0) {
	    player.getPackets().sendGameMessage("There is no stock of that item at the moment.");
	    return;
	}
	int dq = slotId >= mainStock.length ? 0 : defaultQuantity[slotId];
	//int price = getBuyPrice(player, item, dq);
	int price = getBuyPrice(item, dq);
	if (price <= 0)
		return;
	int amountCoins = money == COINS ? player.getInventory().getCoinsAmount() : player.getInventory().getItems().getNumberOf(money);
	int maxQuantity = amountCoins / price;
	int buyQ = item.getAmount() > quantity ? quantity : item.getAmount();

	boolean enoughCoins = maxQuantity >= buyQ;
	if (money != 995) {
		for (int i11 = 0; i11 < DungPrices.length; i11++) {
			DungShop = 85;
			if (item.getId() == DungPrices[i11][0]) {
				if (player.getDungTokens() < DungPrices[i11][1] * quantity) {
					player.getPackets().sendGameMessage("You need " + DungPrices[i11][1] + " Dungeoneering Tokens to buy this!");
						return;
				} else
					if (player.getDungTokens() < 0) {
						return;
					}
					DungShop = 85;
					player.getPackets().sendGameMessage("You have bought a " + item.getDefinitions().getName() + " from the Dungeoneering Token Shop.");
					player.getInventory().addItem(DungPrices[i11][0], 1);
					player.setDungTokens(player.getDungTokens() - DungPrices[i11][1]);
					return;	
				}
		}
	}
	if (money != 995) {
		for (int i11 = 0; i11 < PestPrices.length; i11++) {
			PestShop = 154;
			if (item.getId() == PestPrices[i11][0]) {
				if (player.getPestPoints() < PestPrices[i11][1] * quantity) {
					player.getPackets().sendGameMessage("You need " + PestPrices[i11][1] + " Pest Points to buy this!");
						return;
				} else
					if (player.getPestPoints() < 0) {
						return;
					}
					PestShop = 154;
					player.getPackets().sendGameMessage("You have bought a " + item.getDefinitions().getName() + " from the Pest Point Shop.");
					player.getInventory().addItem(PestPrices[i11][0], 1);
					player.setDungTokens(player.getPestPoints() - PestPrices[i11][1]);
					return;	
				}
		}
	}
	if (!enoughCoins) {
	    player.getPackets().sendGameMessage("You don't have enough " + ItemDefinitions.getItemDefinitions(money).getName().toLowerCase() + ".");
	    buyQ = maxQuantity;
	} else if (quantity > buyQ)
	    player.getPackets().sendGameMessage("The shop has run out of stock.");
	if (item.getDefinitions().isStackable()) {
	    if (player.getInventory().getFreeSlots() < 1) {
		player.getPackets().sendGameMessage("Not enough space in your inventory.");
		return;
	    }
	} else {
	    int freeSlots = player.getInventory().getFreeSlots();
	    if (buyQ > freeSlots) {
		buyQ = freeSlots;
		player.getPackets().sendGameMessage("Not enough space in your inventory.");
	    }
	}
	if (buyQ != 0) {
	    int totalPrice = price * buyQ;
	    if (player.getInventory().removeItemMoneyPouch(new Item(money, totalPrice))) {
	    	player.shopLog(player, item.getId(), item.getAmount(), false);
		player.getInventory().addItem(item.getId(), buyQ);
		item.setAmount(item.getAmount() - buyQ);
		if (item.getAmount() <= 0 && slotId >= mainStock.length)
		    generalStock[slotId - mainStock.length] = null;
		refreshShop();
		resetSelected(player);
	    }
	}
    }

    public void restoreItems() {
	boolean needRefresh = false;
	for (int i = 0; i < mainStock.length; i++) {
	    if (mainStock[i].getAmount() < defaultQuantity[i]) {
		mainStock[i].setAmount(mainStock[i].getAmount() + 1);
		needRefresh = true;
	    } else if (mainStock[i].getAmount() > defaultQuantity[i]) {
		mainStock[i].setAmount(mainStock[i].getAmount() + -1);
		needRefresh = true;
	    }
	}
	if (generalStock != null) {
	    for (int i = 0; i < generalStock.length; i++) {
		Item item = generalStock[i];
		if (item == null)
		    continue;
		item.setAmount(item.getAmount() - 1);
		if (item.getAmount() <= 0)
		    generalStock[i] = null;
		needRefresh = true;
	    }
	}
	if (needRefresh)
	    refreshShop();
    }

    private boolean addItem(int itemId, int quantity) {
	for (Item item : mainStock) {
	    if (item.getId() == itemId) {
		item.setAmount(item.getAmount() + quantity);
		refreshShop();
		return true;
	    }
	}
	if (generalStock != null) {
	    for (Item item : generalStock) {
		if (item == null)
		    continue;
		if (item.getId() == itemId) {
		    item.setAmount(item.getAmount() + quantity);
		    refreshShop();
		    return true;
		}
	    }
	    for (int i = 0; i < generalStock.length; i++) {
		if (generalStock[i] == null) {
		    generalStock[i] = new Item(itemId, quantity);
		    refreshShop();
		    return true;
		}
	    }
	}
	return false;
    }

    public void sell(Player player, int slotId, int quantity) {
	if (player.getInventory().getItemsContainerSize() < slotId)
	    return;
	Item item = player.getInventory().getItem(slotId);
	if (item == null)
	    return;
	int originalId = item.getId();
	if (item.getDefinitions().isNoted() && item.getDefinitions().getCertId() != -1)
	    item = new Item(item.getDefinitions().getCertId(), item.getAmount());
	if (!ItemConstants.isTradeable(item) || item.getId() == money) {
	    player.getPackets().sendGameMessage("You can't sell this item.");
	    return;
	}
	int dq = getDefaultQuantity(item.getId());
	if (dq == -1 && generalStock == null) {
	    player.getPackets().sendGameMessage("You can't sell this item to this shop.");
	    return;
	}
	int price = getSellPrice(item, dq);
	if (price <= 0)
		return;
	int numberOff = player.getInventory().getItems().getNumberOf(originalId);
	if (quantity > numberOff)
	    quantity = numberOff;
	if (!addItem(item.getId(), quantity) && !isGeneralStore()) {
		player.getPackets().sendGameMessage("Shop is currently full.");
		return;
	}
	player.shopLog(player, item.getId(), item.getAmount(), true);
	player.getInventory().deleteItem(originalId, quantity);
	refreshShop();
	resetSelected(player);
	if (price == 0)
	    return;
	player.getInventory().addItemMoneyPouch(new Item(995, price * quantity));
    }
	
	public static int DungShop = 0;
	public static int PestShop = 0;

    public void sendValue(Player player, int slotId) {
		if (player.getInventory().getItemsContainerSize() < slotId)
			return;
		Item item = player.getInventory().getItem(slotId);
		if (item == null)
			return;
		if (item.getDefinitions().isNoted())
			item = new Item(item.getDefinitions().getCertId(), item.getAmount());
		if (!ItemConstants.isTradeable(item) || item.getId() == money) {
			player.getPackets().sendGameMessage("You can't sell this item.");
			return;
		}
		int dq = getDefaultQuantity(item.getId());
		if (dq == -1 && generalStock == null) {
			player.getPackets().sendGameMessage("You can't sell this item to this shop.");
			return;
		}
		int price = getSellPrice(item, dq);
		if (price <= 0)
			return;
		player.getPackets().sendGameMessage(item.getDefinitions().getName() + ": shop will buy for: " + price + " " + ItemDefinitions.getItemDefinitions(money).getName().toLowerCase() + ". Right-click the item to sell.");
    }

    public int getDefaultQuantity(int itemId) {
		for (int i = 0; i < mainStock.length; i++)
			if (mainStock[i].getId() == itemId)
			return defaultQuantity[i];
		return -1;
    }

    public void resetSelected(Player player) {
		player.getTemporaryAttributtes().remove("ShopSelectedSlot");
		player.getVarsManager().sendVar(2563, -1);
    }

    public void sendInfo(Player player, int slotId, boolean inventory) {
		if (!inventory && slotId >= getStoreSize())
			return;
		Item item = inventory ? player.getInventory().getItem(slotId) : slotId >= mainStock.length ? generalStock[slotId - mainStock.length] : mainStock[slotId];
		if (item == null)
			return;
		if (item.getDefinitions().isNoted())
			item = new Item(item.getDefinitions().getCertId(), item.getAmount());
		if (inventory && (!ItemConstants.isTradeable(item) || item.getId() == money)) {
			player.getPackets().sendGameMessage("You can't sell this item.");
			resetSelected(player);
			return;
		}
		for (int i = 0; i < DungPrices.length; i++) {
			if (item.getId() == DungPrices[i][0]) {
				player.getPackets().sendGameMessage("" + item.getDefinitions().getName() + " costs " + DungPrices[i][1] + " Dung points.");
				player.getPackets().sendConfig(2564, DungPrices[i][1]);
				return;
			}
		}
		for (int i = 0; i < PestPrices.length; i++) {
			if (item.getId() == PestPrices[i][0]) {
				player.getPackets().sendGameMessage("" + item.getDefinitions().getName() + " costs " + PestPrices[i][1] + " Pest points.");
				player.getPackets().sendConfig(2564, PestPrices[i][1]);
				return;
			}
		}
		resetTransaction(player);
		player.getTemporaryAttributtes().put("ShopSelectedSlot", slotId);
		player.getTemporaryAttributtes().put("ShopSelectedInventory", inventory);
		player.getVarsManager().sendVar(2561, inventory ? 93 : generalStock != null ? 139 : MAIN_STOCK_ITEMS_KEY); // inv key
		player.getVarsManager().sendVar(2562, item.getId());
		player.getVarsManager().sendVar(2563, slotId);
		player.getPackets().sendGlobalString(362, ItemExamines.getExamine(item));
		player.getPackets().sendGlobalConfig(1876, item.getDefinitions().isWearItem() ? 0 : -1); // TODO item  pos or usage if has one, setting 0 to allow see stats
		int price = inventory ? getSellPrice(item, getDefaultQuantity(item.getId())) : getBuyPrice(item, slotId >= mainStock.length ? 0 : defaultQuantity[slotId]);
		player.getPackets().sendGameMessage(item.getDefinitions().getName() + ": shop will " + (inventory ? "buy" : "sell") + " for: " + price + " " + ItemDefinitions.getItemDefinitions(money).getName().toLowerCase());
    }
	
	/**
	 * @param
	 * gets buy prices manuely
	 *
	 */
	
	public static int getBuyPrice(Item item, int dq) {
		String name = item.getName().toLowerCase();
		if (name.contains("master cape")) {
			item.getDefinitions().setValue(120000);
		}
		if (name.contains("grimy")) {
			item.getDefinitions().setValue(10000);
		}
        switch (item.getId()) {
			//Baby Dragon Bones
			case 535:
			    item.getDefinitions().setValue(50000);
			break;
			//Baby Dragon Bones
			case 534:
				item.getDefinitions().setValue(50000);
			break;
			//Seers' Ring
			case 6731:
				item.getDefinitions().setValue(5000000);
			break;
			//Berserkers' Ring
			case 6737:
				item.getDefinitions().setValue(5000000);
			break;
			//Warrior's Ring
			case 6735:
				item.getDefinitions().setValue(5000000);
			break;
			//Archers' Ring
			case 6733:
				item.getDefinitions().setValue(5000000);
			break;
			//Pouch
			case 12155:
				item.getDefinitions().setValue(1);
			break;
			//Spirit Shards
			case 12183:
				item.getDefinitions().setValue(25);
			break;
			//Wolf Bones
			case 2859:
				item.getDefinitions().setValue(1000);
			break;
			//Master Wand
			case 6914:
				item.getDefinitions().setValue(500000);
			break;
			//Mage's Book
			case 6889:
				item.getDefinitions().setValue(500000);
			break;
			//Abyssal Whip
			case 4151:
				item.getDefinitions().setValue(17500000);
			break;
			 //Abyssal Wand
			case 30825:
				item.getDefinitions().setValue(17500000);
			break;
			//Abyssal Orb
			case 30828:
				item.getDefinitions().setValue(17500000);
			break;
			//Berserkers' ring (i)
			case 15220:
				item.getDefinitions().setValue(50000000);
			break;
			//Seers' ring (i)
			case 15018:
				item.getDefinitions().setValue(50000000);
			break;
			//Archers' ring (i)
			case 15019:
				item.getDefinitions().setValue(50000000);
			break;
			//Warriors' ring (i)
			case 15020:
				item.getDefinitions().setValue(50000000);
			break;
			//Baby Troll
			case 23030:
				item.getDefinitions().setValue(25000000);
			break;
			//Amulet of fury (or)
			case 19335:
				item.getDefinitions().setValue(75000000);
			break;
			//Scythe
			case 1419:
				item.getDefinitions().setValue(50000000);
			break;
			//Overload Flask (6)
			case 23531:
				item.getDefinitions().setValue(25000);
			break;
			//Prayer Renewal Flask (6)
			case 23609:
				item.getDefinitions().setValue(20000);
			break;
			//Super Antifire Flask (6)
			case 23489:
				item.getDefinitions().setValue(20000);
			break;
			//Saradomin Brew Flask (6)
			case 23352:
				item.getDefinitions().setValue(20000);
			break;
			//Super Restore Flask (6)
			case 23400:
				item.getDefinitions().setValue(20000);
			break;
            //Beacon Ring
            case 11014:
                item.getDefinitions().setValue(100000);
            break;
		}
		return item.getDefinitions().getValue(); // TODO formula
	}

    public int getSellPrice(Item item, int dq) {
		String name = item.getName().toLowerCase();
		return item.getDefinitions().getValue() / 2;
    }

    public void sendExamine(Player player, int slotId) {
	if (slotId >= getStoreSize())
	    return;
	Item item = slotId >= mainStock.length ? generalStock[slotId - mainStock.length] : mainStock[slotId];
	if (item == null)
	    return;
	player.getPackets().sendGameMessage(ItemExamines.getExamine(item));
    }

    public void refreshShop() {
	for (Player player : viewingPlayers) {
	    sendStore(player);
	    player.getPackets().sendIComponentSettings(620, 25, 0, getStoreSize() * 6, 1150);
	}
    }

    public int getStoreSize() {
	return mainStock.length + (generalStock != null ? generalStock.length : 0);
    }

    public void sendStore(Player player) {
	Item[] stock = new Item[mainStock.length + (generalStock != null ? generalStock.length : 0)];
	System.arraycopy(mainStock, 0, stock, 0, mainStock.length);
	if (generalStock != null)
	    System.arraycopy(generalStock, 0, stock, mainStock.length, generalStock.length);
	player.getPackets().sendItems(generalStock != null ? 139 : MAIN_STOCK_ITEMS_KEY, stock);
    }

}