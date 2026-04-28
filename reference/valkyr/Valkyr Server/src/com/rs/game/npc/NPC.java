package com.rs.game.npc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.rs.Settings;
import com.rs.cache.loaders.ItemDefinitions;
import com.rs.cache.loaders.NPCDefinitions;
import com.rs.cores.CoresManager;
import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.ForceTalk;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.cores.mysql.impl.DropLogger;
import com.rs.game.item.Item;
import com.rs.game.minigames.zombies.Zombies;
import com.rs.game.npc.combat.NPCCombat;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.npc.familiar.Familiar;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.actions.HerbCleaning;
import com.rs.game.player.actions.thieving.PickPocketableNPC;
import com.rs.game.player.content.Assassins;
import com.rs.game.player.content.Burying;
import com.rs.game.player.content.Deaths;
import com.rs.game.player.content.FriendChatsManager;
import com.rs.game.player.content.SpringCleaner;
import com.rs.game.player.controlers.FightCaves;
import com.rs.game.player.controlers.FightKiln;
import com.rs.game.player.controlers.WGuildControler;
import com.rs.game.player.controlers.Wilderness;
import com.rs.game.player.controlers.darkinvasion.DarkInvasion;
import com.rs.game.player.controlers.dung.RuneDungGame;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Logger;
import com.rs.utils.MapAreas;
import com.rs.utils.Misc;
import com.rs.utils.NPCBonuses;
import com.rs.utils.NPCCombatDefinitionsL;
import com.rs.utils.NPCDrops;
import com.rs.utils.Utils;
import com.rs.game.player.SlayerManager;
import com.rs.game.SecondaryBar;

public class NPC extends Entity implements Serializable {

	private static final long serialVersionUID = -4794678936277614443L;

	private int id;
	private WorldTile respawnTile;
	private int mapAreaNameHash;
	private boolean canBeAttackFromOutOfArea;
	private boolean randomwalk;
	private int[] bonuses; // 0 stab, 1 slash, 2 crush,3 mage, 4 range, 5 stab
	// def, blahblah till 9
	private boolean spawned;
	private transient NPCCombat combat;
	public WorldTile forceWalk;
	public int whatToSay = 0;
	private long lastAttackedByTarget;
	private boolean cantInteract;
	private int capDamage;
	private int lureDelay;
	private boolean cantFollowUnderCombat;
	private boolean forceAgressive;
	private int forceTargetDistance;
	private boolean forceFollowClose;
	private boolean forceMultiAttacked;
	private boolean noDistanceCheck;

	// npc masks
	private transient Transformation nextTransformation;
	private transient SecondaryBar nextSecondaryBar;
	//name changing masks
	private String name;
	private transient boolean changedName;
	private int combatLevel;
	private transient boolean changedCombatLevel;
	private transient boolean locked;
	
	public NPC(int id, WorldTile tile, int mapAreaNameHash,
			boolean canBeAttackFromOutOfArea) {
		this(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, false);
	}
	

	/*
	 * creates and adds npc
	 */
	public NPC(int id, WorldTile tile, int mapAreaNameHash,
			boolean canBeAttackFromOutOfArea, boolean spawned) {
		super(tile);
		this.id = id;
		this.respawnTile = new WorldTile(tile);
		this.mapAreaNameHash = mapAreaNameHash;
		this.canBeAttackFromOutOfArea = canBeAttackFromOutOfArea;
		this.setSpawned(spawned);
		combatLevel = -1;
		setHitpoints(getMaxHitpoints());
		setDirection(getRespawnDirection());
		for (int i : Settings.NON_WALKING_NPCS1) {
            if (i == id)
                setRandomWalk(false);
            else
                setRandomWalk((getDefinitions().walkMask & 0x2) != 0
                        || forceRandomWalk(id));
        }
		bonuses = NPCBonuses.getBonuses(id);
		combat = new NPCCombat(this);
		capDamage = -1;
		lureDelay = 12000;
		// npc is inited on creating instance
		initEntity();
		World.addNPC(this);
		World.updateEntityRegion(this);
		// npc is started on creating instance
		loadMapRegions();
		checkMultiArea();
	}

	@Override
	public boolean needMasksUpdate() {
		return super.needMasksUpdate() || nextSecondaryBar != null || nextTransformation != null || changedCombatLevel || changedName;
	}

	public void transformIntoNPC(int id) {
		setNPC(id);
		nextTransformation = new Transformation(id);
	}
	
	public void setNextNPCTransformation(int id) {
		setNPC(id);
		nextTransformation = new Transformation(id);
		if (getCustomCombatLevel() != -1)
		    changedCombatLevel = true;
		if (getCustomName() != null)
		    changedName = true;
	    }
	
	public static void moo() {
		WorldTasksManager.schedule(new WorldTask() {
			@Override
			public void run() {
				String[] mooing = { "Moo", "Moooo", "MOOOOOOOOO", "derp", "Mooooooooo", "Neigh" };
				int i = Misc.random(1, 5);
				for (NPC n : World.getNPCs()) {
					if (!n.getName().equalsIgnoreCase("Cow")) {
						continue;
					}
					n.setNextForceTalk(new ForceTalk(mooing[i]));
				}
			}
		}, 0, 5); //time in seconds
	}

	public void setNPC(int id) {
		this.id = id;
		bonuses = NPCBonuses.getBonuses(id);
	}

	@Override
	public void resetMasks() {
		super.resetMasks();
		nextTransformation = null;
		changedCombatLevel = false;
		changedName = false;
		nextSecondaryBar = null;
	}

	public int getMapAreaNameHash() {
		return mapAreaNameHash;
	}

	public void setCanBeAttackFromOutOfArea(boolean b) {
		canBeAttackFromOutOfArea = b;
	}
	
	public boolean canBeAttackFromOutOfArea() {
		return canBeAttackFromOutOfArea;
	}
	
	public void setBonuses(int[] bonuses) {
		this.bonuses = bonuses;
	}

	public NPCDefinitions getDefinitions() {
		return NPCDefinitions.getNPCDefinitions(id);
	}

	public NPCCombatDefinitions getCombatDefinitions() {
		return NPCCombatDefinitionsL.getNPCCombatDefinitions(id);
	}

	@Override
	public int getMaxHitpoints() {
		return getCombatDefinitions().getHitpoints();
	}

	public int getId() {
		return id;
	}

	public void processNPC() {
		if (isDead() || locked)
			return;
		if (!combat.process()) { // if not under combat
			if (!isForceWalking()) {// combat still processed for attack delay
				// go down
				// random walk
				if (!cantInteract) {
					if (!checkAgressivity()) {
						if (getFreezeDelay() < Utils.currentTimeMillis()) {
							if (((hasRandomWalk()) && World.getRotation(
									getPlane(), getX(), getY()) == 0) // temporary
									// fix
									&& Math.random() * 1000.0 < 100.0) {
								int moveX = (int) Math
										.round(Math.random() * 10.0 - 5.0);
								int moveY = (int) Math
										.round(Math.random() * 10.0 - 5.0);
								resetWalkSteps();
								if (getMapAreaNameHash() != -1) {
									if (!MapAreas.isAtArea(getMapAreaNameHash(), this)) {
										forceWalkRespawnTile();
										return;
									}
									addWalkSteps(getX() + moveX, getY() + moveY, 5);
								}else 
									addWalkSteps(respawnTile.getX() + moveX, respawnTile.getY() + moveY, 5);
							}
						}
					}
				}
			}
		}
		setRandomWalk(false);
		if (id == 17144)
			setRandomWalk(true);
		if (id == 17145)
			setRandomWalk(true);
		if (id == 4932)
			setRandomWalk(true);
		if (id == 17146)
			setRandomWalk(true);
		if (id == 17147)
			setRandomWalk(true);
		if (id == 1624)
			setRandomWalk(true);
		if (id == 5529)
			setRandomWalk(true);
		if (id == 4355)
			setRandomWalk(true);
		if (id == 83)
			setRandomWalk(true);
		if (id == 110)
			setRandomWalk(true);
		if (id == 1593)
			setRandomWalk(true);
		if (id == 5086)
			setRandomWalk(true);
		if (id == 5079)
			setRandomWalk(true);
		if (id == 1615)
			setRandomWalk(true);
		if (id == 1643)
			setRandomWalk(true);
		if (id == 1618)
			setRandomWalk(true);
		if (id == 1613)
			setRandomWalk(true);
		if (id == 5073)
			setRandomWalk(true);
		if (id == 1610)
			setRandomWalk(true);
		if (id == 1604)
			setRandomWalk(true);
		if (id == 4243)
            setRandomWalk(true);
		if (id == 5082)
			setRandomWalk(true);
        if (id == 5080)
            setRandomWalk(true);
		if (id == 5088)
			setRandomWalk(true);
		if (id == 9077)
			setRandomWalk(true);
		if (id == 5362)
			setRandomWalk(true);
		if (id == 1612)
			setRandomWalk(true);
		if (id == 1648)
			setRandomWalk(true);
		if (id == 1632)
			setRandomWalk(true);
		if (id == 1620)
			setRandomWalk(true);
		if (id == 1633)
			setRandomWalk(true);
		if (id == 1600)
			setRandomWalk(true);
		if (id == 1616)
			setRandomWalk(true);
		if (id == 1637)
			setRandomWalk(true);
		if (id == 1623)
			setRandomWalk(true);
		if (id == 1608)
			setRandomWalk(true);
		if (id == 9172)
			setRandomWalk(true);
        if (id == 19109)
            setRandomWalk(true);
		if (id == 519)
			setName("Tools");
        if (id == 2932)
            setName("Ports Manager");
		if (id == 3705)
			setName("Master Capes");
		if (id == 538)
			setName("Pure Shop");
		if (id == 520)
			setName("Fishing Supplies");
		if (id == 529)
			setName("Herblore Supplies");
		if (id == 1526)
			setName("Castle Wars Shop");
        if (id == 585)
            setName("General Store");	 	 
        if (id == 522)
			setName("Skillcape Shop");
		if (id == 10)
			setName("Hoe");
		if (id == 548)
			setName("Skiller Outfits");
		if (id == 1513)
			setName("Make-over Mage");
		if (id == 3709)
			setName("Monster Teleports");
		if (id == 551)
			setName("Armour Supplies");
		if (id == 11475)
			setName("Magic Supplies");
		if (id == 546)
			setName("Runecrafting & Construction Supplies");
		if (id == 550)
			setName("Range Supplies");
		if (id == 555)
			setName("Summoning Supplies");
		if (id == 6539)
			setName("Potion Shop");
		if (id == 789)
			setName("Food & Pots");
		if (id == 14244)
			setName("Zerker pk shop");
		if (id == 13927)
            setName("Donator pk shop");
		if (id == 14242)
            setName("Zerk Donashop");	  	 
        if (id == 556)
            setName("Trivia Store");
        if (id == 552)
            setName("Donator's Potions");
		if (id == 14765)
            setName("Donator's Points");	 
        if (id == 2253)
            setName("Prestige Master");
		if (id == 2620)
           setName("Pvm shop");		 
		if (id == 12195)
			setName("Vote Shop");		  
		if (id == 2187)
			setName("Cannon Trader");
		if (id == 2892)
			setCantFollowUnderCombat(false);
		if (isForceWalking()) {
			if (getFreezeDelay() < Utils.currentTimeMillis()) {
				if (getX() != forceWalk.getX() || getY() != forceWalk.getY()) {
					if (!hasWalkSteps())
						addWalkSteps(forceWalk.getX(), forceWalk.getY(),
								getSize(), true);
					if (!hasWalkSteps()) { // failing finding route
						setNextWorldTile(new WorldTile(forceWalk)); // force
						// tele
						// to
						// the
						// forcewalk
						// place
						forceWalk = null; // so ofc reached forcewalk place
					}
				} else
					// walked till forcewalk place
					forceWalk = null;
			}
		}
	}

	@Override
	public void processEntity() {
		super.processEntity();
		processNPC();
	}

	public int getRespawnDirection() {
		NPCDefinitions definitions = getDefinitions();
		if (definitions.anInt853 << 32 != 0 && definitions.respawnDirection > 0
				&& definitions.respawnDirection <= 8)
			return (4 + definitions.respawnDirection) << 11;
		return 0;
	}

	/*
	 * forces npc to random walk even if cache says no, used because of fake
	 * cache information
	 */
	private static boolean forceRandomWalk(int npcId) {
		switch (npcId) {
		case 11226:
			return true;
		case 3341:
		case 3342:
		case 3343:
			return true;
		default:
			return false;
			/*
			 * default: return NPCDefinitions.getNPCDefinitions(npcId).name
			 * .equals("Icy Bones");
			 */
		}
	}
	
	public void sendSoulSplit(final Hit hit, final Entity user) {
		final NPC target = this;
		if (hit.getDamage() > 0)
			World.sendProjectile(user, this, 2263, 11, 11, 20, 5, 0, 0);
		if (user instanceof Player) {
			final Player p2 = (Player) user;
			if (((Player) user).getEquipment().isWearingAmuletOfSouls() && Utils.getRandom(1) == 0) {
				int healing = Utils.random(2, 3);
				user.heal(hit.getDamage() / healing);
				((Player) user).sm("Amulet of souls proc'd healing you " + hit.getDamage() / healing + " total.");
			} else {
				user.heal(hit.getDamage() / 5);
			}
		} else {
			user.heal(hit.getDamage() / 5);
		}
		WorldTasksManager.schedule(new WorldTask() {
			@Override
			public void run() {
				setNextGraphics(new Graphics(2264));
				if (hit.getDamage() > 0)
					World.sendProjectile(target, user, 2263, 11, 11, 20, 5, 0,
							0);
			}
		}, 1);
	}

	@Override
	public void handleIngoingHit(final Hit hit) {
		if (capDamage != -1 && hit.getDamage() > capDamage)
			hit.setDamage(capDamage);
		if (hit.getLook() != HitLook.MELEE_DAMAGE
				&& hit.getLook() != HitLook.RANGE_DAMAGE
				&& hit.getLook() != HitLook.MAGIC_DAMAGE)
			return;
		Entity source = hit.getSource();
		if (source == null)
			return;
		if (source instanceof Player) {
			final Player p2 = (Player) source;
            if (hit.getDamage() > p2.getMaxHit()) {
                p2.setMaxHit(hit.getDamage());
                p2.getPackets().sendGameMessage("Your new max hit is now: " + p2.getMaxHit());
                return;
            }
			if (p2.getPrayer().hasPrayersOn()) {
				if (p2.getPrayer().usingPrayer(1, 18)) 
					sendSoulSplit(hit, p2);
				if (hit.getDamage() == 0)
					return;
				if (!p2.getPrayer().isBoostedLeech()) {
					if (hit.getLook() == HitLook.MELEE_DAMAGE) {
							if (Utils.getRandom(4) == 0) {
								Player p = (Player) source;
									p2.prayer.increaseTurmoilBonus(p);
									p2.prayer.setBoostedLeech(true);
									return;
							}
						if (p2.getPrayer().usingPrayer(1, 19)) {
							p2.getPrayer().setBoostedLeech(true);
							return;
						} else if (p2.getPrayer().usingPrayer(1, 1)) { // sap
							// att
							if (Utils.getRandom(4) == 0) {
								if (p2.getPrayer().reachedMax(0)) {
									p2.getPackets()
									.sendGameMessage(
											"Your opponent has been weakened so much that your sap curse has no effect.",
											true);
								} else {
									p2.getPrayer().increaseLeechBonus(0);
									p2.getPackets()
									.sendGameMessage(
											"Your curse drains Attack from the enemy, boosting your Attack.",
											true);
								}
								p2.setNextAnimation(new Animation(12569));
								p2.setNextGraphics(new Graphics(2214));
								p2.getPrayer().setBoostedLeech(true);
								World.sendProjectile(p2, this, 2215, 35, 35,
										20, 5, 0, 0);
								WorldTasksManager.schedule(new WorldTask() {
									@Override
									public void run() {
										setNextGraphics(new Graphics(2216));
									}
								}, 1);
								return;
							}
						} else {
							if (p2.getPrayer().usingPrayer(1, 10)) {
								if (Utils.getRandom(7) == 0) {
									if (p2.getPrayer().reachedMax(3)) {
										p2.getPackets()
										.sendGameMessage(
												"Your opponent has been weakened so much that your leech curse has no effect.",
												true);
									} else {
										p2.getPrayer().increaseLeechBonus(3);
										p2.getPackets()
										.sendGameMessage(
												"Your curse drains Attack from the enemy, boosting your Attack.",
												true);
									}
									p2.setNextAnimation(new Animation(12575));
									p2.getPrayer().setBoostedLeech(true);
									World.sendProjectile(p2, this, 2231, 35,
											35, 20, 5, 0, 0);
									WorldTasksManager.schedule(new WorldTask() {
										@Override
										public void run() {
											setNextGraphics(new Graphics(2232));
										}
									}, 1);
									return;
								}
							}
							if (p2.getPrayer().usingPrayer(1, 14)) {
								if (Utils.getRandom(7) == 0) {
									if (p2.getPrayer().reachedMax(7)) {
										p2.getPackets()
										.sendGameMessage(
												"Your opponent has been weakened so much that your leech curse has no effect.",
												true);
									} else {
										p2.getPrayer().increaseLeechBonus(7);
										p2.getPackets()
										.sendGameMessage(
												"Your curse drains Strength from the enemy, boosting your Strength.",
												true);
									}
									p2.setNextAnimation(new Animation(12575));
									p2.getPrayer().setBoostedLeech(true);
									World.sendProjectile(p2, this, 2248, 35,
											35, 20, 5, 0, 0);
									WorldTasksManager.schedule(new WorldTask() {
										@Override
										public void run() {
											setNextGraphics(new Graphics(2250));
										}
									}, 1);
									return;
								}
							}

						}
					}
					if (hit.getLook() == HitLook.RANGE_DAMAGE) {
						if (p2.getPrayer().usingPrayer(1, 2)) { // sap range
							if (Utils.getRandom(4) == 0) {
								if (p2.getPrayer().reachedMax(1)) {
									p2.getPackets()
									.sendGameMessage(
											"Your opponent has been weakened so much that your sap curse has no effect.",
											true);
								} else {
									p2.getPrayer().increaseLeechBonus(1);
									p2.getPackets()
									.sendGameMessage(
											"Your curse drains Range from the enemy, boosting your Range.",
											true);
								}
								p2.setNextAnimation(new Animation(12569));
								p2.setNextGraphics(new Graphics(2217));
								p2.getPrayer().setBoostedLeech(true);
								World.sendProjectile(p2, this, 2218, 35, 35,
										20, 5, 0, 0);
								WorldTasksManager.schedule(new WorldTask() {
									@Override
									public void run() {
										setNextGraphics(new Graphics(2219));
									}
								}, 1);
								return;
							}
						} else if (p2.getPrayer().usingPrayer(1, 11)) {
							if (Utils.getRandom(7) == 0) {
								if (p2.getPrayer().reachedMax(4)) {
									p2.getPackets()
									.sendGameMessage(
											"Your opponent has been weakened so much that your leech curse has no effect.",
											true);
								} else {
									p2.getPrayer().increaseLeechBonus(4);
									p2.getPackets()
									.sendGameMessage(
											"Your curse drains Range from the enemy, boosting your Range.",
											true);
								}
								p2.setNextAnimation(new Animation(12575));
								p2.getPrayer().setBoostedLeech(true);
								World.sendProjectile(p2, this, 2236, 35, 35,
										20, 5, 0, 0);
								WorldTasksManager.schedule(new WorldTask() {
									@Override
									public void run() {
										setNextGraphics(new Graphics(2238));
									}
								});
								return;
							}
						}
					}
					if (hit.getLook() == HitLook.MAGIC_DAMAGE) {
						if (p2.getPrayer().usingPrayer(1, 3)) { // sap mage
							if (Utils.getRandom(4) == 0) {
								if (p2.getPrayer().reachedMax(2)) {
									p2.getPackets()
									.sendGameMessage(
											"Your opponent has been weakened so much that your sap curse has no effect.",
											true);
								} else {
									p2.getPrayer().increaseLeechBonus(2);
									p2.getPackets()
									.sendGameMessage(
											"Your curse drains Magic from the enemy, boosting your Magic.",
											true);
								}
								p2.setNextAnimation(new Animation(12569));
								p2.setNextGraphics(new Graphics(2220));
								p2.getPrayer().setBoostedLeech(true);
								World.sendProjectile(p2, this, 2221, 35, 35,
										20, 5, 0, 0);
								WorldTasksManager.schedule(new WorldTask() {
									@Override
									public void run() {
										setNextGraphics(new Graphics(2222));
									}
								}, 1);
								return;
							}
						} else if (p2.getPrayer().usingPrayer(1, 12)) {
							if (Utils.getRandom(7) == 0) {
								if (p2.getPrayer().reachedMax(5)) {
									p2.getPackets()
									.sendGameMessage(
											"Your opponent has been weakened so much that your leech curse has no effect.",
											true);
								} else {
									p2.getPrayer().increaseLeechBonus(5);
									p2.getPackets()
									.sendGameMessage(
											"Your curse drains Magic from the enemy, boosting your Magic.",
											true);
								}
								p2.setNextAnimation(new Animation(12575));
								p2.getPrayer().setBoostedLeech(true);
								World.sendProjectile(p2, this, 2240, 35, 35,
										20, 5, 0, 0);
								WorldTasksManager.schedule(new WorldTask() {
									@Override
									public void run() {
										setNextGraphics(new Graphics(2242));
									}
								}, 1);
								return;
							}
						}
					}

					// overall

					if (p2.getPrayer().usingPrayer(1, 13)) { // leech defence
						if (Utils.getRandom(10) == 0) {
							if (p2.getPrayer().reachedMax(6)) {
								p2.getPackets()
								.sendGameMessage(
										"Your opponent has been weakened so much that your leech curse has no effect.",
										true);
							} else {
								p2.getPrayer().increaseLeechBonus(6);
								p2.getPackets()
								.sendGameMessage(
										"Your curse drains Defence from the enemy, boosting your Defence.",
										true);
							}
							p2.setNextAnimation(new Animation(12575));
							p2.getPrayer().setBoostedLeech(true);
							World.sendProjectile(p2, this, 2244, 35, 35, 20, 5,
									0, 0);
							WorldTasksManager.schedule(new WorldTask() {
								@Override
								public void run() {
									setNextGraphics(new Graphics(2246));
								}
							}, 1);
							return;
						}
					}
				}
			}
		}

	}

	@Override
	public void reset() {
		super.reset();
		setDirection(getRespawnDirection());
		combat.reset();
		bonuses = NPCBonuses.getBonuses(id); // back to real bonuses
		forceWalk = null;
	}

	@Override
	public void finish() {
		if (hasFinished())
			return;
		setFinished(true);
		World.updateEntityRegion(this);
		World.removeNPC(this);
		//setRespawnTask();
	}

	public void setRespawnTask() {
		if (!hasFinished()) {
			reset();
			setLocation(respawnTile);
			finish();
		}
		CoresManager.slowExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					spawn();
				} catch (Throwable e) {
					Logger.handle(e);
				}
			}
		}, getCombatDefinitions().getRespawnDelay() * 600,
		TimeUnit.MILLISECONDS);
	}
	
	public void setLongRespawnTask() {
		if (!hasFinished()) {
			reset();
			setLocation(respawnTile);
			finish();
		}
		CoresManager.slowExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					spawn();
				} catch (Throwable e) {
					Logger.handle(e);
				}
			}
		}, getCombatDefinitions().getRespawnDelay() * 2000,
		TimeUnit.MILLISECONDS);
	}
	public void removespring() {
		if (!hasFinished()) {
			World.removeNPC(null);
			finish();
		}
		CoresManager.slowExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					//spawn();
				} catch (Throwable e) {
					Logger.handle(e);
				}
			}
		}, getCombatDefinitions().getRespawnDelay() * 2000,
		TimeUnit.MILLISECONDS);
	}
	public void setLongerRespawnTask() {
		if (!hasFinished()) {
			reset();
			setLocation(respawnTile);
			finish();
		}
		CoresManager.slowExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					spawn();
				} catch (Throwable e) {
					Logger.handle(e);
				}
			}
		}, getCombatDefinitions().getRespawnDelay() * 5000,
		TimeUnit.MILLISECONDS);
	}
	public void setremovenpcTask() {
		if (!hasFinished()) {
			reset();
			setLocation(respawnTile);
			finish();
		}
		CoresManager.slowExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					spawn();
				} catch (Throwable e) {
					Logger.handle(e);
				}
			}
		}, getCombatDefinitions().getRespawnDelay() * -1,
		TimeUnit.MILLISECONDS);
	}
	public void deserialize() {
		if (combat == null)
			combat = new NPCCombat(this);
		spawn();
	}

	public void spawn() {
		setFinished(false);
		World.addNPC(this);
		setLastRegionId(0);
		World.updateEntityRegion(this);
		loadMapRegions();
		checkMultiArea();
	}

	public NPCCombat getCombat() {
		return combat;
	}

	@Override
	public void sendDeath(Entity source) {
		final NPCCombatDefinitions defs = getCombatDefinitions();
		resetWalkSteps();
		combat.removeTarget();
		setNextAnimation(null);
		WorldTasksManager.schedule(new WorldTask() {
			int loop;

			@Override
			public void run() {
				if (loop == 0) {
					setNextAnimation(new Animation(defs.getDeathEmote()));
				} else if (loop >= defs.getDeathDelay()) {
					drop();
					reset();
					setLocation(respawnTile);
					finish();
					if (!isSpawned())
						setRespawnTask();
					stop();
				}
				loop++;
			}
		}, 0, 1);
	}

	public void drop() {
		try {
			
			Player killer1 = getMostDamageReceivedSourcePlayer();
			Drop[] drops = NPCDrops.getDrops(id);
			if (id == 2677)
				return;
			int size = getSize();
			if (this.getCombatLevel() < 70) {
				if (Misc.random(300) == 1) {
					World.addGroundItem(new Item(2700, 1), new WorldTile(this.getCoordFaceX(size), getCoordFaceY(size), getPlane()), killer1, true, 180);
					//World.sendWorldMessage("<col=ff8c38><img=7>News: "+ killer1.getDisplayName() + " has just recieved an easy clue drop!"+ "</col> ", false);					
				}
			} else if (this.getCombatLevel() >= 70 && this.getCombatLevel() < 130) {
				if (Misc.random(200) == 1) {
					World.addGroundItem(new Item(13080, 1), new WorldTile(this.getCoordFaceX(size), getCoordFaceY(size), getPlane()), killer1, true, 180);
					//World.sendWorldMessage("<col=ff8c38><img=7>News: "+ killer1.getDisplayName() + " has just recieved a medium clue drop!"+ "</col> ", false);
				}
			} else if (this.getCombatLevel() >= 130 && this.getCombatLevel() < 500) {
				if (Misc.random(150) == 1) {
					World.addGroundItem(new Item(13010, 1), new WorldTile(this.getCoordFaceX(size), getCoordFaceY(size), getPlane()), killer1, true, 180);
					//World.sendWorldMessage("<col=ff8c38><img=7>News: "+ killer1.getDisplayName() + " has just recieved a hard clue drop!"+ "</col> ", false);
				}
			} else if (this.getCombatLevel() >= 500) {
				if (Misc.random(100) == 1) {
					World.addGroundItem(new Item(19064, 1), new WorldTile(this.getCoordFaceX(size), getCoordFaceY(size), getPlane()), killer1, true, 180);
					//World.sendWorldMessage("<col=ff8c38><img=7>News: "+ killer1.getDisplayName() + " has just recieved an elite clue drop!"+ "</col> ", false);
				}
			}
			final int[] charms = { 12158, 12159, 12160, 12163 };
			final int randomCharm = charms[Utils.random(charms.length)];
			int effigy = Misc.random(750);
			if (effigy == 1)
				World.addGroundItem(new Item(18778, 1), new WorldTile(this.getCoordFaceX(size), getCoordFaceY(size), getPlane()), killer1, true, 180);
			int charmamount = 4;
			if (World.quadcharms == true) {
				charmamount = 1;
			}
			if (this.getId() == 9356) {
				killer1.VS = 4;
			}			
			Player killer = getMostDamageReceivedSourcePlayer();
			if (killer == null)
				return;
			Player otherPlayer = killer.getSlayerManager().getSocialPlayer();
			SlayerManager manager = killer.getSlayerManager();
			if (manager.isValidTask(getName())) {
			    manager.checkCompletedTask(getDamageReceived(killer), otherPlayer != null ? getDamageReceived(otherPlayer) : 0);
			}
			Assassins manager2 = killer.getAssassinsManager();
			if (killer.getInventory().containsItem(27996, 1)) {
                if (killer.getInventory().getFreeSlots() >= 1) {
                    killer.getInventory().addItem(randomCharm, Utils.random(3));
                    //player.sm("Your charming imp collects some charms for you.");
                } else {
                    killer.getBank().addItem(randomCharm, Utils.random(3), false);
                }
            } else {
			    World.addGroundItem(new Item(randomCharm, new Item(randomCharm).getDefinitions().isStackable() ? Utils.random(charmamount) : 5), new WorldTile(this.getCoordFaceX(size), getCoordFaceY(size), getPlane()), killer, true, 180);
            }
			if (manager2.getTask() != null) {
			if (getId() == manager2.getNpcId()) {
				if (manager2.getGameMode() == 3) {
					if (manager2.getSpeed() <= 0) {
						manager2.resetTask();
						killer.sm("You have run out of time and can no longer complete your task.");
					} else {
						manager2.completeTask();
					}
				} else if (manager2.getGameMode() == 2) {
					if (killer.getEquipment().getWeaponId() == manager2.getWeapon()) {
						manager2.completeTask();
					} else {
						killer.sm("You must be using a "+manager2.getWeaponName()+" to kill this monster.");
					}
				} else {
						manager2.completeTask();
				}
			}
			}
			
			Deaths manager3 = killer.getDeathsManager();
			if (manager3.getTask() != null) {
			if (getId() == manager3.getNpcId()) {

						manager3.completeTask();
				}
			}
			//Boss kill count
			if(this.getId() == 6222){
				killer1.KCarmadyl++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCarmadyl+" KreeArra kills!</col>");
			}
			if(this.getId()== 6260){
				killer1.KCbandos++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCbandos+" General Graador kills!</col>");
			}
			if(this.getId() == 6203){
				killer1.KCzammy++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCzammy+" Kril Tsutsaroth kills!</col>");
			}
			if(this.getId() == 6247){
				killer1.KCsaradomin++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCsaradomin+" Commander Zilyana kills!</col>");
			}
			if(this.getId() == 8133){
				killer1.KCcorp++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCcorp+" Corporeal Beast kills!</col>");
			}
			if(this.getId() == 15222){
				killer1.KCsunfreet++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCsunfreet+" Sunfreet kills!</col>");
			}
			if(this.getId() == 3334){
				killer1.KCwild++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCwild+" Wildy Wyrm kills!</col>");
			}
			if(this.getId()== 12878){
				killer1.KCblink++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCblink+" Blink kills!</col>");
			}
			if(this.getId()==11872){
				killer1.KCThunder++;
				killer1.sendMessage("<col=15FF00>You now have "+killer1.KCThunder+" Yk'Lagor kills!</col>");
			}
			
			
			if(this.getId() == 5044 || this.getId() == 5045){
				killer1.Nstage1++;
				killer1.sendMessage("<col=00F5FF>You now have "+killer1.Nstage1+"/10 of the required kills to continue.</col>");
				if(killer1.Nstage1 >= 10){
					killer1.setNextWorldTile(new WorldTile(2649, 9393, 0));
					killer1.sendMessage("You have advanced to the next stage! Good luck!");
					killer1.Nstage1 = 0;
				}
			}
			if(this.getId() == 5218 || this.getId() == 5219){
				killer1.Nstage2++;
				killer1.sendMessage("<col=00F5FF>You now have "+killer1.Nstage2+"/10 of the required kills to continue.</col>");
				if(killer1.Nstage2 >= 10){
					//killer1.setNextWorldTile(new WorldTile(3174, 9766, 0));
					killer1.getControlerManager().startControler("DreadnautControler");
					killer1.Nstage2 = 0;
				}
			}
			if(this.getId() == 12862){
				killer1.Nstage3 = 1;
				killer1.sendMessage("<col=00F5FF>You have killed the Dreadnaut! Talk to Cassie at home for your reward!</col>");
				killer1.setNextWorldTile(new WorldTile(2966, 3397, 0));
			}
			if (drops == null)
				return;

			// SlayerTask task = killer.getSlayerTask();
			if (killer.slayerTask.getTaskMonstersLeft() > 0) {
				for (String m : killer.slayerTask.getTask().slayable) {
					if (getDefinitions().name.equals(m)) {
						killer.slayerTask.onMonsterDeath(killer, this);
						break;
					}
				}
			}
			Drop[] possibleDrops = new Drop[drops.length];
			int possibleDropsCount = 0;
			for (Drop drop : drops) {
				if (drop.getRate() == 100)
					sendDrop(killer, drop);
				else {
					if ((Utils.getRandomDouble(99) + 1) <= drop.getRate() * 1.0)
						possibleDrops[possibleDropsCount++] = drop;
				}
			}
			if (possibleDropsCount > 0)
				sendDrop(killer,
						possibleDrops[Utils.getRandom(possibleDropsCount - 1)]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendDrop(final Player player, Drop drop) {
		final int size = getSize();
		int bonus = 1;
		int random = Utils.random(1, 100);
		if (World.doubledrops == true) {
			bonus = 2;
		}
		String dropName = ItemDefinitions.getItemDefinitions(drop.getItemId())
				.getName().toLowerCase();
		Item item = ItemDefinitions.getItemDefinitions(drop.getItemId())
				.isStackable() ? new Item(drop.getItemId(),
				(drop.getMinAmount() * Settings.DROP_RATE*bonus)
						+ Utils.getRandom(drop.getExtraAmount()
								* Settings.DROP_RATE*bonus)) : new Item(
				drop.getItemId(), drop.getMinAmount()
						+ Utils.getRandom(drop.getExtraAmount()));
		if (item != null) {
			//new Thread(new DropLogger(player, this, item)).start();
		}
		if (item.getId() == 995) {
			player.getInventory().addItemMoneyPouch(item);
			return;
		}
		if (player.getEquipment().isWearingRingOfWealth() && this.getCombatLevel() >= 50) {
			int tableChance = Utils.getRandom(100);
			Item rdtd = RareDropTable.getRandomDrop(RareDropTable.RareTable.randomDrop());
			if (tableChance >= 96 && player.hasPerk("raining rares")) {
				World.addGroundItem(rdtd, new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
				World.addGroundItem(new Item(995, 50), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
				player.sm("<col=ff7000>Your ring of wealth shines more brightly!");
				if (player.debug) 
					player.sm("[Debug RDT]: <col=FFFF00>" + rdtd.getAmount() + "x</col> Drop Name: <col=FFFF00>" + rdtd.getName() + "</col>");
			} else if (tableChance >= 98 && !player.hasPerk("raining rares")) {
				World.addGroundItem(rdtd, new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
				World.addGroundItem(new Item(995, 50), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
				player.sm("<col=ff7000>Your ring of wealth shines more brightly!");
				if (player.debug) 
					player.sm("[Debug RDT]: <col=FFFF00>" + rdtd.getAmount() + "x</col> Drop Name: <col=FFFF00>" + rdtd.getName() + "</col>");
			}
		}
		if (player.getInventory().containsItem(31612, 1)) {
			if (player.cleanerSprings > 0 && Utils.getRandom(2) == 1) {
				if (item.getName().contains("leather") || item.getName().contains("d'hide") || item.getName().contains("vambraces") && player.springLeatherArmor) {
					SpringCleaner.processItem(player, item, 1);
					return;
				}	
				if (item.getName().contains(" bar") && player.springMetalBars) {
					SpringCleaner.processItem(player, item, 2);
					return;
				}
				if (item.getName().contains("platebody") || item.getName().contains("platelegs") || item.getName().contains("plateskirt") 
						|| item.getName().contains("full helm") || item.getName().contains("chainbody") || item.getName().contains("kiteshield") 
							|| item.getName().contains(" sq shield") && !item.getName().contains("dragon") && player.springMetalArmor) {		
					SpringCleaner.processItem(player, item, 3);
					return;
				}
				if (item.getName().contains(" longsword") || item.getName().contains("mace") || item.getName().contains("scimitar") 
						|| item.getName().contains("dagger") || item.getName().contains("halbard") || item.getName().contains("warhammer") 
							|| item.getName().contains(" sword") || item.getName().contains("battleaxe") || item.getName().contains("pickaxe") 
								|| item.getName().contains("spear") || item.getName().contains(" hatchet") && !item.getName().contains("dragon")  && player.springMetalWeaponsAndArrows) {
					SpringCleaner.processItem(player, item, 4);
					return;
				}
				if (item.getName().contains(" longbow") || item.getName().contains(" bow") && !item.getName().contains("dark") && player.springBows) { 
					SpringCleaner.processItem(player, item, 5);
					return;
				}
				if (item.getName().contains(" crossbow") && !item.getName().contains("dragon") && player.springCrossbows) {
					SpringCleaner.processItem(player, item, 6);
					return;
				}
				if (item.getName().contains(" amulet") && !item.getName().contains("onyx") && player.springJewllery) {
					SpringCleaner.processItem(player, item, 7);
					return;
				}
				if (item.getName().contains("water battlestaff") || item.getName().contains("air battlestaff") 
						|| item.getName().contains("earth battlestaff") || item.getName().contains("fire battlestaff") && player.springBattlestaves) {
					SpringCleaner.processItem(player, item, 8);
					return;
				}
			} else {
				player.sm("<col=feaf00>The spring cleaner gears crunch and it rejects an item.</col>");
				player.sm("<col=ff0000>The spring cleaner failed to break down an item and it has been left on the floor. No springs were used</col>");
			}
		}
		if (player.getInventory().containsItem(18337, 1)// Bonecrusher
				&& item.getDefinitions().getName().toLowerCase()
						.contains("bones")) {
			player.getSkills().addXp(Skills.PRAYER,
					Burying.Bone.forId(drop.getItemId()).getExperience());
			return;
		}
		if (player.getInventory().containsItem(19675, 1)// Herbicide
				&& item.getDefinitions().getName().toLowerCase()
						.contains("grimy")) {
			if (player.getSkills().getLevelForXp(Skills.HERBLORE) >= HerbCleaning.getHerb(item.getId()).getLevel()) {
				player.getSkills().addXp(
						Skills.HERBLORE,
						HerbCleaning.getHerb(drop.getItemId())
								.getExperience() * 2);
				return;
			}
		}
		if (random >= 95 && random <= 100) {
			if (this.getId() == 116
				||this.getId() == 4292
				||this.getId() == 4291
				||this.getId() == 6078
				||this.getId() == 6079
				||this.getId() == 6080
				&& drop.getItemId() == 8844) {
				WGuildControler.dropDefender(player, this);
				return;
			}
		}
        if (this.getId() == 6247) { //Saradomin
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33833, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved an Auburn lock drop!"+ "</col> ", false);
          }
        }
        if (this.getId() == 6222) { //Armadyl
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33830, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Giant feather drop!"+ "</col> ", false);
          }
        }
        if (this.getId() == 6260) { //Bandos
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33832, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Decaying tooth drop!"+ "</col> ", false);
          }
        }
        if (this.getId() == 6203) { //Kril
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33831, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Severed hoof drop!"+ "</col> ", false);
          }
        }
        if (this.getId() == 13450) { //Nex
            if (Utils.getRandom(1000) == 1) {
                World.addGroundItem(new Item(33834, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Blood-soaked feather drop!"+ "</col> ", false);
          }
        }
        if (this.getId() == 3340) { //Giant Mole
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33838, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Rotten fang drop!"+ "</col> ", false);
          }
        }
        if (this.getId() == 50) { //KBD
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33842, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a King Black Dragon scale drop!"+ "</col> ", false);
          }
        }
        if (this.getId() == 1158) { //Kalphite Queen
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33841, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Kalphite egg drop!"+ "</col> ", false);
          }
        }
        if (this.getId() == 8133) { //Corporeal Beast
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33837, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			    WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
				    }
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
					}
					if (loop == 4) {
						 stop();
					}
					 loop++;
				}
				  
			  }, 0, 1);
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			  World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Corporeal bone drop!"+ "</col> ", false);
           }
        }
        if (this.getId() == 8133) { //Chaos Elemental
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33836, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			    WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
				    }
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
					}
					if (loop == 4) {
						 stop();
					}
					 loop++;
				}
				  
			  }, 0, 1);
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			  World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Ribs of chaos drop!"+ "</col> ", false);
           }
        }
        if (this.getId() == 17182) { //Vorago
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(33716, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
				    }
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
					}
					if (loop == 4) {
						 stop();
					}
					 loop++;
				}
				  
			  }, 0, 1);
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			  World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved an Ancient Artefact drop!"+ "</col> ", false);
           }
        }
        if (this.getId() == 17182) { //Vorago
            if (Utils.getRandom(2500) == 1) {
                World.addGroundItem(new Item(28626, 1), new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), player, true, 180);
                World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
				    }
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
					}
					if (loop == 4) {
						 stop();
					}
					 loop++;
				}
				  
			  }, 0, 1);
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			  World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a Ancient summoning stone drop!"+ "</col> ", false);
           }
        }
		if (this.getId() == 6230 || this.getId() == 6231 || this.getId() == 6229
				|| this.getId() == 6232 || this.getId() == 6240 || this.getId() == 6241
				|| this.getId() == 6242 || this.getId() ==  6233 || this.getId() == 6234
				|| this.getId() == 6243 || this.getId() == 6244 || this.getId() == 6245
				|| this.getId() == 6246 || this.getId() == 6238 || this.getId() == 6239
				|| this.getId() == 6227 || this.getId() == 6625 || this.getId() == 6223
				|| this.getId() == 6222) {
			player.armadyl++;
	        player.getPackets().sendIComponentText(601, 8,  ""+ player.armadyl +"");
		}
		if (this.getId() == 6278 || this.getId() == 6277 || this.getId() == 6276
				|| this.getId() == 6283 || this.getId() == 6282 || this.getId() == 6280
				|| this.getId() == 6281 || this.getId() == 6279 || this.getId() == 6275
				|| this.getId() == 6271 || this.getId() == 6272 || this.getId() == 6273
				|| this.getId() == 6274 || this.getId() == 6269 || this.getId() ==  6270
				|| this.getId() == 6268 || this.getId() == 6265 || this.getId() == 6263
				|| this.getId() == 6261 || this.getId() == 6260) {
			player.bandos++;
			player.getPackets().sendIComponentText(601, 9,  ""+ player.bandos +"");
		}
		if (this.getId() == 6257 || this.getId() == 6255 || this.getId() == 6256
				|| this.getId() == 6258 || this.getId() == 6259 || this.getId() == 6254
				|| this.getId() == 6252 || this.getId() == 6250 || this.getId() == 6248
				|| this.getId() == 6247) {
			player.saradomin++;
			 player.getPackets().sendIComponentText(601, 10,  ""+ player.saradomin +"");
		}
		if (this.getId() == 6221 || this.getId() == 6219 || this.getId() == 6220
				|| this.getId() == 6217 || this.getId() == 6216 || this.getId() == 6215
				|| this.getId() == 6214 || this.getId() == 6213 || this.getId() == 6212
				|| this.getId() == 6211 || this.getId() == 6218 || this.getId() == 6208
				|| this.getId() == 6206 || this.getId() == 6204 || this.getId() == 6203) {
			player.zamorak++;
			player.getPackets().sendIComponentText(601, 11,  ""+ player.zamorak +"");
		}
		/*LootShare/CoinShare*/
		FriendChatsManager fc = player.getCurrentFriendChat();
		if(player.lootshareEnabled()) {
			if(fc != null) {
				CopyOnWriteArrayList<Player> players = fc.getPlayers();
				CopyOnWriteArrayList<Player> playersWithLs = new CopyOnWriteArrayList<Player>();
				for(Player p : players) {
					if(p.lootshareEnabled() && p.getRegionId() == player.getRegionId()) //If players in FC have LS enabled and are also in the same map region.
						playersWithLs.add(p);
				}
				if (item.getDefinitions().getTipitPrice() >= 1000000) {
				int playeramount = playersWithLs.size();
				int dividedamount = (item.getDefinitions().getTipitPrice() / playeramount);
				for(Player p : playersWithLs) {
					p.getInventory().addItemMoneyPouch(new Item(995, dividedamount));
					p.sendMessage(String.format("<col=115b0d>You received: %sx coins from a split of the item %s.</col>", dividedamount, dropName));
					return;
				}
				} else {
				Player luckyPlayer = playersWithLs.get((int)(Math.random()*playersWithLs.size())); //Choose a random player to get the drop.
				World.addGroundItem(item, new WorldTile(getCoordFaceX(size), getCoordFaceY(size), getPlane()), luckyPlayer, true, 180);
				luckyPlayer.sendMessage(String.format("<col=115b0d>You received: %sx %s.</col>", item.getAmount(), dropName));
				for(Player p : playersWithLs) {
					if(!p.equals(luckyPlayer))
					p.sendMessage(String.format("%s received: %sx %s.", luckyPlayer.getDisplayName(), item.getAmount(), dropName));
				}
				}
				return;
			}
		} 
		/*End of LootShare/CoinShare*/
		player.npcLog(player, item.getId(), item.getAmount(), item.getName(), this.getName(), this.getId());
		if (!player.isPker) {
			World.addGroundItem(item, new WorldTile(getCoordFaceX(size),
					getCoordFaceY(size), getPlane()), player, true, 180);
		} else {
			World.addGroundItem(item, new WorldTile(getCoordFaceX(size),
					getCoordFaceY(size), getPlane()), player, true, 1800000000);
		}
		if (dropName.contains("pernix") 
				|| dropName.contains("torva")
				|| dropName.contains("virtus") 
				|| dropName.contains("bandos")
				|| dropName.contains("hilt")
				|| dropName.contains("hati") 
				|| dropName.contains("korasi")
				|| dropName.contains("dragon claw")
				|| dropName.contains("tetsu")
				|| dropName.contains("seasinger")
				|| dropName.contains("deathlotus")
				|| dropName.contains("divine")
				|| (dropName.contains("saradomin")  && !dropName.contains("brew"))
				|| dropName.contains("visage")
				|| dropName.contains("zamorakian")
				|| dropName.contains("spectral")
				|| dropName.contains("elysian")
				|| dropName.contains("steadfast")
				|| dropName.contains("armadyl chest")
				|| dropName.contains("armadyl battlestaff")
				|| dropName.contains("armadyl plate")
				|| dropName.contains("armadyl boots")
				|| dropName.contains("armadyl helmet")
				|| dropName.contains("armadyl gloves")
				|| dropName.contains("armadyl_chest")
				|| dropName.contains("armadyl_plate")
				|| dropName.contains("armadyl_boots")
				|| dropName.contains("armadyl_helmet")
				|| dropName.contains("armadyl_gloves")
				|| dropName.contains("armadyl_chainskirt")
				|| dropName.contains("armadyl chainskirt")
				|| dropName.contains("buckler")
				|| dropName.contains("glaiven")
				|| dropName.contains("ragefire")
				|| dropName.contains("spirit shield")
				|| dropName.contains("spirit_shield")
				|| dropName.contains("elixer")
				|| dropName.contains("fury")
				|| dropName.contains("arcane")
				|| dropName.contains("subjugation")
				|| dropName.contains("goliath")
				|| dropName.contains("swift")
				|| dropName.contains("spellcaster")
				|| dropName.contains("gorgonite")
				|| dropName.contains("promethium")
				|| dropName.contains("primal")
				|| dropName.contains("polypore_stick")
				|| dropName.contains("polypore stick")
				|| dropName.contains("ganodermic gloves")
				|| dropName.contains("ganodermic_gloves")
				|| dropName.contains("ganodermic boots")
				|| dropName.contains("ganodermic_boots")
				|| dropName.contains("vesta")
				|| dropName.contains("statius")
				|| dropName.contains("zuriel")
				|| dropName.contains("morrigan")
				|| dropName.contains("clue")
				|| dropName.contains("clue_scroll_(elite)")
				|| dropName.contains("clue_scroll_(easy)")
				|| dropName.contains("clue_scroll_(hard)")
				|| dropName.contains("clue_scroll_(medium)")
				|| dropName.contains("deathtouched")
				|| dropName.contains("dragon_chain")
				|| dropName.contains("dragon crossbow")
				|| dropName.contains("dragon chain")
				|| dropName.contains("dragon_full")
				|| dropName.contains("dragon full")
				|| dropName.contains("dragon_kite")
				|| dropName.contains("dragon kite")
				|| dropName.contains("dragon_rider")
				|| dropName.contains("dragon rider")
				|| dropName.contains("holy_elixir")
				|| dropName.contains("inferno")
				|| dropName.contains("gilded")
				|| dropName.contains("mask")
				|| dropName.contains("chaotic")
				|| dropName.contains("tectonic robe top")
				|| dropName.contains("tectonic robe bottom")
				|| dropName.contains("tectonic mask")
				|| dropName.contains("ascension grips")
				|| dropName.contains("ascension signet")
				|| dropName.contains("seismic_wand")
				|| dropName.contains("seismic_singularity")
				|| dropName.contains("abyssal_whip")
				|| dropName.contains("abyssal wand")
				|| dropName.contains("abyssal orb")
				|| dropName.contains("razorback")
				|| dropName.contains("celestial handwraps")
				|| dropName.contains("dragon pickaxe")
				|| dropName.contains("drygore")
				|| dropName.contains("drygore_mace")
				|| dropName.contains("drygore_rapier")
				|| dropName.contains("drygore_longsword")
				|| dropName.contains("Off-hand_drygore_rapier")
				|| dropName.contains("Off-hand_drygore_mace")
				|| dropName.contains("Off-hand_drygore_longsword")){
			  World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
			  
			  player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			World.sendWorldMessage("<col=ff8c38><img=7>News: "+ player.getDisplayName() + " has just recieved a " + dropName + " drop!"+ "</col> ", false);
		}
		if (dropName.contains("ascension keystone")) {
			World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane()));
			player.sm("<col=ff8c38>A golden beam shines over one of your items.");
			  
			  WorldTasksManager.schedule(new WorldTask() {

				@Override
				public void run() {
					int loop = 0;
					if (loop == 1) {
					 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
								getCoordFaceY(size), getPlane()));
					}
					if (loop == 2) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 3) {
						 World.sendGraphics(player, new Graphics(4422), new WorldTile(getCoordFaceX(size),
									getCoordFaceY(size), getPlane()));
						}
					if (loop == 4) {
						 stop();
						}
					 loop++;
				}
				  
			  }, 0, 1);
		}
	}

	@Override
	public int getSize() {
		return getDefinitions().size;
	}

	public int getMaxHit() {
		return getCombatDefinitions().getMaxHit();
	}

	public int[] getBonuses() {
		return bonuses;
	}

	@Override
	public double getMagePrayerMultiplier() {
		return 0;
	}

	@Override
	public double getRangePrayerMultiplier() {
		return 0;
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return 0;
	}

	public WorldTile getRespawnTile() {
		return respawnTile;
	}

	public boolean isUnderCombat() {
		return combat.underCombat();
	}

	@Override
	public void setAttackedBy(Entity target) {
		super.setAttackedBy(target);
		if (target == combat.getTarget()
				&& !(combat.getTarget() instanceof Familiar))
			lastAttackedByTarget = Utils.currentTimeMillis();
	}

	public boolean canBeAttackedByAutoRelatie() {
		return Utils.currentTimeMillis() - lastAttackedByTarget > lureDelay;
	}

	public boolean isForceWalking() {
		return forceWalk != null;
	}

	public void setTarget(Entity entity) {
		if (isForceWalking()) // if force walk not gonna get target
			return;
		combat.setTarget(entity);
		lastAttackedByTarget = Utils.currentTimeMillis();
	}

	public void removeTarget() {
		if (combat.getTarget() == null)
			return;
		combat.removeTarget();
	}

	public void forceWalkRespawnTile() {
		setForceWalk(respawnTile);
	}

	public void setForceWalk(WorldTile tile) {
		resetWalkSteps();
		forceWalk = tile;
	}

	public boolean hasForceWalk() {
		return forceWalk != null;
	}

	public ArrayList<Entity> getPossibleTargets() {
		ArrayList<Entity> possibleTarget = new ArrayList<Entity>();
		for (int regionId : getMapRegionsIds()) {
			List<Integer> playerIndexes = World.getRegion(regionId)
					.getPlayerIndexes();
			if (playerIndexes != null) {
				for (int playerIndex : playerIndexes) {
					Player player = World.getPlayers().get(playerIndex);
					if (player == null
							|| player.isDead()
							|| player.hasFinished()
							|| !player.isRunning()
							|| !player
							.withinDistance(
									this,
									forceTargetDistance > 0 ? forceTargetDistance
											: (getCombatDefinitions()
													.getAttackStyle() == NPCCombatDefinitions.MELEE ? 4
															: getCombatDefinitions()
															.getAttackStyle() == NPCCombatDefinitions.SPECIAL ? 64
																	: 8))
																	|| (!forceMultiAttacked
																			&& (!isAtMultiArea() || !player
																					.isAtMultiArea())
																					&& player.getAttackedBy() != this && (player
																							.getAttackedByDelay() > Utils.
																							currentTimeMillis() || player
																							.getFindTargetDelay() > Utils
																							.currentTimeMillis()))
																							|| !clipedProjectile(player, false)
																							|| (!forceAgressive && !Wilderness.isAtWild(this) && immunity(player)))
						continue;
					possibleTarget.add(player);
				}
			}
		}
		return possibleTarget;
	}
	
	public boolean immunity(Player player) {
		if (player.stealth) {
		if (player.getSkills().getAssassinLevel(Skills.STEALTH_MOVES) <= 10) {
			if (player.getSkills().getCombatLevelWithSummoning() >= getCombatLevel() * 2)
				return true;
			else
				return false;
		} else if (player.getSkills().getAssassinLevel(Skills.STEALTH_MOVES) >= 11 && player.getSkills().getAssassinLevel(Skills.STEALTH_MOVES) <= 98) {
			int level = player.getSkills().getCombatLevelWithSummoning() + (player.getSkills().getAssassinLevel(Skills.STEALTH_MOVES)*2);
			if (level >= getCombatLevel() * 2)
				return true;
			else
				return false;
		} else {
			return true;
		}
		} else {
			if (player.getSkills().getCombatLevelWithSummoning() >= getCombatLevel() * 2)
				return true;
			else
				return false;
		}
	}

	public boolean checkAgressivity() {
		if (!forceAgressive) {
			NPCCombatDefinitions defs = getCombatDefinitions();
			if (defs.getAgressivenessType() == NPCCombatDefinitions.PASSIVE)
				return false;
		}
		ArrayList<Entity> possibleTarget = getPossibleTargets();
		if (!possibleTarget.isEmpty()) {
			Entity target = possibleTarget.get(Utils.random(possibleTarget.size()));
			setTarget(target);
			target.setAttackedBy(target);
			target.setFindTargetDelay(Utils.currentTimeMillis() + 10000);
			return true;
		}
		return false;
	}

	public boolean isCantInteract() {
		return cantInteract;
	}

	public void setCantInteract(boolean cantInteract) {
		this.cantInteract = cantInteract;
		if (cantInteract)
			combat.reset();
	}

	public int getCapDamage() {
		return capDamage;
	}

	public void setCapDamage(int capDamage) {
		this.capDamage = capDamage;
	}

	public int getLureDelay() {
		return lureDelay;
	}

	public void setLureDelay(int lureDelay) {
		this.lureDelay = lureDelay;
	}

	public boolean isCantFollowUnderCombat() {
		return cantFollowUnderCombat;
	}

	public void setCantFollowUnderCombat(boolean canFollowUnderCombat) {
		this.cantFollowUnderCombat = canFollowUnderCombat;
	}

	public Transformation getNextTransformation() {
		return nextTransformation;
	}

	@Override
	public String toString() {
		return getDefinitions().name + " - " + id + " - " + getX() + " "
				+ getY() + " " + getPlane();
	}

	public boolean isForceAgressive() {
		return forceAgressive;
	}

	public void setForceAgressive(boolean forceAgressive) {
		this.forceAgressive = forceAgressive;
	}

	public int getForceTargetDistance() {
		return forceTargetDistance;
	}

	public void setForceTargetDistance(int forceTargetDistance) {
		this.forceTargetDistance = forceTargetDistance;
	}

	public boolean isForceFollowClose() {
		return forceFollowClose;
	}

	public void setForceFollowClose(boolean forceFollowClose) {
		this.forceFollowClose = forceFollowClose;
	}

	public boolean isForceMultiAttacked() {
		return forceMultiAttacked;
	}

	public void setForceMultiAttacked(boolean forceMultiAttacked) {
		this.forceMultiAttacked = forceMultiAttacked;
	}

	public boolean hasRandomWalk() {
		return randomwalk;
	}

	public void setRandomWalk(boolean forceRandomWalk) {
		this.randomwalk = forceRandomWalk;
	}

	public String getCustomName() {
		return name;
	}

	public void setName(String string) {
		this.name = getDefinitions().name.equals(string) ? null : string;
		changedName = true;
	}

	public int getCustomCombatLevel() {
		return combatLevel;
	}

	public int getCombatLevel() {
		return combatLevel >= 0 ? combatLevel : getDefinitions().combatLevel;
	}

	public String getName() {
		return name != null ? name : getDefinitions().name;
	}
	
	public String getName2() {
		return getDefinitions().name != null ? getDefinitions().name : name;
	}

	public void setCombatLevel(int level) {
		combatLevel  = getDefinitions().combatLevel == level ? -1 : level;
		changedCombatLevel = true;
	}

	public boolean hasChangedName() {
		return changedName;
	}

	public boolean hasChangedCombatLevel() {
		return changedCombatLevel;
	}

	public WorldTile getMiddleWorldTile() {
		int size = getSize();
		return new WorldTile(getCoordFaceX(size),getCoordFaceY(size), getPlane());
	}

	public boolean isSpawned() {
		return spawned;
	}

	public void setSpawned(boolean spawned) {
		this.spawned = spawned;
	}

	public boolean isNoDistanceCheck() {
		return noDistanceCheck;
	}

	public void setNoDistanceCheck(boolean noDistanceCheck) {
		this.noDistanceCheck = noDistanceCheck;
	}
	
	public boolean withinDistance(Player tile, int distance) {
		return super.withinDistance(tile, distance);
	}

	/**
	 * Gets the locked.
	 * @return The locked.
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * Sets the locked.
	 * @param locked The locked to set.
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
    public SecondaryBar getNextSecondaryBar() {
		return nextSecondaryBar;
    }

    public void setNextSecondaryBar(SecondaryBar secondaryBar) {
		this.nextSecondaryBar = secondaryBar;
    }
    
	public boolean isBossPet(int npcId) {
		switch (npcId) {
			case 20534:
			case 20530:
			case 20533:
			case 20532:
			case 20531:
			case 20539:
			case 20540:
			case 20544:
			case 20551:
			case 20543:
			case 33789:
			case 20538:
			case 20537:
			case 20545:
			case 20546:
			case 20547:
			case 20548:
			case 20549:
			case 20550:
			case 20553:
			case 20552:
			case 20554:
			case 20461:
			case 17186:
			case 17397:
			case 20536:
			case 20535:
			case 19475:
			case 19476:
			case 19477:
			case 19478:
			case 20541:
			case 19479:
			case 19480:
				return true;
		}
		return false;
	}
    
}
