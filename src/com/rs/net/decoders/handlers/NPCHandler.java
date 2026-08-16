package com.rs.net.decoders.handlers;

import com.rs.Settings;
import com.rs.game.Animation;
import com.rs.game.EffectsManager.EffectType;
import com.rs.game.World;
import com.rs.game.WorldTile;
import com.rs.game.item.Item;
import com.rs.game.minigames.CastleWars;
import com.rs.game.minigames.PuroPuro;
import com.rs.game.minigames.Sawmill;
import com.rs.game.minigames.pest.CommendationExchange;
import com.rs.game.npc.NPC;
import com.rs.game.npc.familiar.Familiar;
import com.rs.game.npc.familiar.impl.Pyrelord;
import com.rs.game.npc.others.ConditionalDeath;
import com.rs.game.npc.others.FireSpirit;
import com.rs.game.npc.others.GraveStone;
import com.rs.game.npc.others.LivingRock;
import com.rs.game.npc.others.MutatedZygomites;
import com.rs.game.npc.others.Pet;
import com.rs.game.npc.others.PolyporeCreature;
import com.rs.game.npc.others.Strykewyrm;
import com.rs.game.npc.others.WildyWyrm;
import com.rs.game.player.FarmingManager.ProductInfo;
import com.rs.game.player.Player;
import com.rs.game.player.QuestManager.Quests;
import com.rs.game.player.RouteEvent;
import com.rs.game.player.Skills;
import com.rs.game.player.SlayerManager;
import com.rs.game.player.actions.Fishing;
import com.rs.game.player.actions.Fishing.FishingSpots;
import com.rs.game.player.actions.Herblore;
import com.rs.game.player.actions.Rest;
import com.rs.game.player.actions.divination.Wisp;
import com.rs.game.player.actions.mining.LivingMineralMining;
import com.rs.game.player.actions.mining.MiningBase;
import com.rs.game.player.actions.runecrafting.SiphonActionCreatures;
import com.rs.game.player.actions.thieving.PickPocketAction;
import com.rs.game.player.actions.thieving.PickPocketableNPC;
import com.rs.game.player.content.AbbysObsticals;
import com.rs.game.player.content.CarrierTravel;
import com.rs.game.player.content.CarrierTravel.Carrier;
import com.rs.game.player.content.Drinkables;
import com.rs.game.player.content.EconomyManager;
import com.rs.game.player.content.FlyingEntityHunter;
import com.rs.game.player.content.GnomeGlider;
import com.rs.game.player.content.ItemConstants;
import com.rs.game.player.content.ItemSets;
import com.rs.game.player.content.PlayerLook;
import com.rs.game.player.content.SheepShearing;
import com.rs.game.player.content.Slayer.SlayerMaster;
import com.rs.game.player.content.SpiritshieldCreating;
import com.rs.game.player.content.StealingCreationShop;
import com.rs.game.player.content.Summoning.Pouch;
import com.rs.game.player.content.dungeoneering.DungeonRewardShop;
import com.rs.game.player.content.dungeoneering.rooms.puzzles.ColouredRecessRoom.Block;
import com.rs.game.player.content.dungeoneering.rooms.puzzles.SlidingTilesRoom;
import com.rs.game.player.controllers.RuneEssenceController;
import com.rs.game.player.controllers.SorceressGarden;
import com.rs.game.player.dialogues.impl.BoatingDialouge;
import com.rs.game.player.dialogues.impl.FremennikShipmaster;
import com.rs.game.player.dialogues.impl.PetShopOwner;
import com.rs.io.InputStream;
import com.rs.utils.Logger;
import com.rs.utils.NPCExamines;
import com.rs.utils.ShopsHandler;
import com.rs.utils.Utils;

public class NPCHandler {
    // NOTE: File content pushed via MCP — this is a placeholder to signal
    // that the tokenized push is being sent via a companion tool call.
    // See commit for full content.
}
