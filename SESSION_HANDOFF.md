# Matrix II RSPS — Session Handoff

Branch: `claude/review-resume-PQG3h`
Repo: `bburge14/matrix-ii-server`
Last commit at handoff: `9a728272`
Working dir: `/home/user/matrix-ii-server` (or `~/matrix/Server` on your box)
Cache: 830-revision

---

## TL;DR

We have been overhauling the bot AI, admin panel, and player-facing
systems on this server. Three high-level threads are live right now:

1. **Bot AI (Citizens + Legends)** — pathing, retaliation, combat,
   trading, equipment, archetypes. Mostly stable, two outstanding
   issues (monster retaliation, retro toggle).
2. **Admin Panel (`admin_panel.py`)** — Citizens, Items, Phantom GE,
   GE Prices, Gear Sets, dye/retro tooling. All working.
3. **Player-facing Oracle dialogue** — combat mode, xp rate, items
   look, PK opt-in. All working except the items-look render swap
   (waiting on user verification).

The cosmetic / kit system is the next chunk after the items-look
toggle is confirmed working.

---

## Build / Run

```bash
cd ~/matrix/Server                                         # or /home/user/matrix-ii-server
git pull origin claude/review-resume-PQG3h
javac -encoding ISO-8859-1 -cp "data/libs/*:src" -d bin $(find src -name "*.java")
# kill the running game and ./run_game.sh again
# admin panel: relaunch admin_panel.py (no Java needed for python-only changes)
```

`-encoding ISO-8859-1` is required — the codebase has non-UTF8
chars in `Bank.java` and `Summoning.java` that break with default
UTF-8.

---

## Currently OPEN issues (what to pick up first)

### 1. Retro / replica items toggle not visibly swapping
**State:** Mechanism is fully wired. User reports putting on
Bandos after toggling the Oracle option doesn't show the retro
look.

**Most likely cause:** `data/items/retro_swaps.json` is empty
or missing — the autopopulate endpoint hasn't been run yet.

**Verify:**
```bash
ls -la /home/user/matrix-ii-server/data/items/retro_swaps.json
head -c 500 /home/user/matrix-ii-server/data/items/retro_swaps.json
```
If empty/missing → user needs to click **Items tab → Auto-build
Swap Map** in the admin panel (or `POST /admin/items/retro/autopopulate`).
If file has the 38 mappings → there is a real render-cache bug
to investigate.

**User also asked:** they want the inventory icon to STAY new but
the worn appearance to swap to retro. Currently `WorldPacketsEncoder.sendItems`
+ `Appearence.java:237` BOTH swap. Quick tweak:
remove the swap from `sendItems` / `sendItemsContainer` and keep
only the Appearence one once the basic swap is verified working.

**Files:**
- `src/com/rs/utils/RetroSwaps.java` (new)
- `src/com/rs/net/encoders/WorldPacketsEncoder.java` (sendItems / sendItemsContainer)
- `src/com/rs/game/player/Appearence.java` (line 237)
- `src/com/rs/game/player/content/EconomyManager.java` (Oracle option 3)
- `src/com/rs/admin/AdminHttpServer.java` (RetroAutoPopulateHandler)
- `data/items/retro_swaps.json` (output; user-controlled or autopopulated)

Cache scan results from the user:
- 38 pairs total. 18× "Replica X" (mostly Replica dragon armor + sp/or
  variants + void), 20× "Retro X" (gloves, boots, armadyl/bandos sets,
  godswords).
- Dyes (separate system): 33294 Barrows dye, 33296 Shadow dye, 33298
  Third-age dye. ~50 weapons each have 3 (broken / shadow / 3a) variants.

### 2. Monsters not retaliating against players (server-wide)
**State:** Three retaliation hooks added; user reports many NPCs
still don't fight back. Diagnostic logging is in place at 1-in-20
sample rate but user has not pasted the log output yet.

**What to do next:**
- Have user attack a passive NPC for ~30 seconds.
- Pull `[NPC-RETAL-HIT]` and `[NPC-RETAL]` lines from the server's
  stdout (`./run_game.sh` terminal).
- If lines appear with `target=null` after `setTarget` → checkAll
  is removing the target; log will show why (mapArea / respawnDist
  / cantInteract / forceWalking).
- If NO lines appear → applyHit isn't firing. Different bug —
  PlayerCombatNew isn't actually delivering damage. Check
  combat-mode / weapon / EOC ability gates.

**Files:**
- `src/com/rs/game/npc/NPC.java` — `handleIngoingHit()` hook,
  `processNPC()` tick fallback, both 1-in-20 logged
- `src/com/rs/game/npc/combat/NPCCombat.java` — `process()`,
  `checkAll()` (dist / area / mapAreaName / forceWalk gates)
- `src/com/rs/game/player/actions/PlayerCombatNew.java` —
  `autoRelatie(player, target)` at line 2364

### 3. PK bots — wired, ready to test
**State:** Just shipped. Combatant citizens (`COMBATANT_PURE / TANK
/ HYBRID`) auto-set `pkOptIn = true` on spawn. `CitizenBrain.tickInteracting`
checks first for an opted-in player victim within 8 tiles before
falling through to NPC training. Combat-level bracket respected.

**To test:**
1. Oracle of Dawn → Account & Character management → **PK opt-in: ON**.
2. Walk into wildy near combatant bot spawns (default budget puts
   some at 3088, 3491).
3. Bots should engage within a tick or two.
4. Toggle PK off mid-fight → bot's next swing should fail
   `Wilderness.canAttack` and they disengage.

If you want denser PK bots in wildy, in the Citizens panel add
budget slots like `COMBATANT_PURE @ 3088, 3520, count=12, scatter=8`.

**Files:**
- `src/com/rs/bot/ambient/CitizenSpawner.java` — `setPkOptIn(true)` for combatants
- `src/com/rs/bot/ambient/CitizenBrain.java` — `tickInteracting` PK target branch + `findNearbyPkVictim`
- `src/com/rs/game/player/controllers/Wilderness.java` — symmetric `canAttack` guard
- `src/com/rs/game/player/Player.java` — `pkOptIn` persistent flag

---

## What's been done (chronological-ish, grouped by area)

### Bot AI core
- `AIPlayer.loadMapRegions` — was a no-op, regions weren't loading;
  inlined `Entity.loadMapRegions` with `World.getRegion(id, true)`.
  This was the root cause of bots freezing on lodestones / banks
  / cross-region walks.
- `BotBrain.lastGoalCheck = 0L` — first tick processes goals
  immediately instead of after 10s grace.
- `BotPathing.walkTo` — force-loads source + destination regions
  before RouteFinder. Falls through to `addWalkSteps` if A* fails.
- `BotTeleporter` — added 16 lodestones with decoded coords.
  Pre-loads dest region at cast.
- `BotPathing.tryOpenNearbyObstacle` — opens any door / gate /
  portcullis / trapdoor with an Open or Unlock option when
  RouteFinder fails.
- `BotPathing.tryUseNearbyLadder` — generic plane-shift via
  `useStairs` for ladders / staircases. 5-arg overload takes
  explicit target plane.
- `BotBrain.executeTrainingMethod` — after XY arrival, if
  `bot.getPlane() != method.location.plane`, invoke ladder climb
  (handles Slayer Tower / multi-floor buildings).
- `EnvironmentScanner.findNearestFishingSpot` — match by tool,
  not enum identity (CAGE vs CAGE2 vs HARPOON on same NPC bug).

### Citizens
- `CitizenBrain` extends BotBrain but tick is full override.
  Surfaced FSM state via `getCurrentState` / `getCurrentActivity`
  overrides + `getLastMethod` so botinfo doesn't lie.
- TRAVERSING walks now use `BotPathing.walkTo` (real A*) instead
  of raw straight-line `addWalkSteps`.
- 80/20 unsheathed at spawn (combat level shown over head).
- Idle fidget + face-direction so citizens don't look dead.
- PK target hunting (see "PK bots" above).

### Combat
- `tryStartCombat` calls `CombatDefinitions.setSheathe(false)` on
  engage. Force `target.setTarget(bot)` before delayed autoRelatie.
- `executeTrainingMethod` early return when `PlayerCombatNew` is
  the active action (no more tele-mid-combat).
- `NPC.handleIngoingHit` retaliation hook (always sets target if
  null and source is Player).
- `NPC.processNPC` tick fallback (catches hits where `applyHit`
  was skipped).
- Dropped `hasAttackOption()` filter (cows have "Milk" not
  "Attack" but should still retaliate).
- HP scaling with melee level (was leaving cb 95 bots at HP 45).

### Trading
- Trader stock locked for bot lifetime (no rotation).
- `BotChatListener` alias for `scythe → 1052` removed (was Cape of
  Legends in this cache); auto-register cache items by keyword
  (Noxious / Drygore / Sirenic / Tectonic / Malevolent / Torva /
  Pernix / Virtus / Chaotic).
- Catalogs broadened: SKILL ~90, COMBAT 80+, RARE 32 entries.
- Trader chatter pools stripped of fake "selling X" lines.
- DZ Skilling Master shop (id 204) stocks all 25 trim caps + hoods.
- Burthorpe trader shops (201/202/207/208) got their missing skillcapes.

### Phantom GE
- Default fill rates bumped 2-3x. Cap raised to 200/100.
- New default behaviour: bulk = 100% on place; mid (whip) = 75%
  + ~80s avg aging; high (bandos) = 50% + ~2 min avg; rare
  (partyhat) = 15% + ~7 min avg.
- Admin panel "Phantom GE" tab gained an explainer block at the
  top with formulas + worked examples.

### Admin panel
- Citizens budget — saves to `data/admin/citizen_budget.json`
  locally (server-side endpoint still exists, panel is source of
  truth). Apply Budget Now pushes local → server then triggers
  apply.
- Items tab — paged browser of every cache item with filters
  (search, equipable only, tradeable only, has override). Bulk
  Make Tradeable / Make Untradeable / Clear Override / Set GE Price.
- Items tab → Dyes row — Scan Cache / Auto-populate Mappings /
  Reload JSON.
- Items tab → Retro Look row — Scan Cache / Auto-build Swap Map /
  Reload Map.
- Phantom GE tab — 14 knobs + live fill log + How-it-works
  callout.
- Gear Sets tab — view/add/edit/delete outfit pools.
- GE Prices tab — bulk push.
- Popup helper `_show_text_window` keeps strong references so
  windows don't immediately close (was a GC bug).

### Oracle of Dawn dialogue
- Refactored from interface 1312 (cs2 fortune-cookie corruption
  bleed-through) to chatbox `sendOptionsDialogue` (interface 1188).
- Pagination: 4 options + "Next ▶" when > 5 entries; page indicator
  in title.
- "Account & Character management" sub-page now has:
  - Combat mode (Legacy / Standard) sub-page
  - XP rate (current: x?) sub-page
  - PK opt-in: ON/OFF toggle
  - Switch to retro items look (WIP — render swap)

### Items / cosmetic infrastructure
- `ItemDefinitions` parser captures opcodes 242-251 (old-look
  fields) — were being read into local vars and discarded.
- `hasOldLook()` helper.
- `TradeableOverrides` — runtime override set, persisted to
  `data/items/tradeable_overrides.json`.
- `DyeRecolors` — bidirectional map persisted to
  `data/items/dye_recolors.json`. Hooked into
  `InventoryOptionsHandler.handleInterfaceOnInterface`.
- `RetroSwaps` — bidirectional map persisted to
  `data/items/retro_swaps.json`. Hooked into `sendItems` +
  `sendItemsContainer` + `Appearence`.

### Slayer / locations / spawns
- Slayer Tower portcullis + entrance gate handler (generic
  fallback in `ObjectHandler.java` for door/gate/portcullis/
  trapdoor names + Open / Unlock option).
- Burthorpe pickpocket / combat method NPC IDs corrected:
  - imps (414, 7878) → (708, 709)
  - goblins (4481-4493) → (100, 101, 102)
  - trolls (941-945) → (1095-1098)
- Custom spawns added at Burthorpe coords for those NPCs +
  master farmers (2234/2235) so the Burthorpe pickpocket method
  has a target.

### PK system
- `Player.pkOptIn` persistent flag, default false.
- `Wilderness.process` only flips canPvp on if pkOptIn is true.
- `Wilderness.canAttack` symmetric guard — both attacker and
  target must have pkOptIn for the swing to succeed.
- Oracle "PK opt-in: ON/OFF" toggle (color-coded).
- PK bots (combatant citizens) auto opt in + hunt opted-in
  players in wildy (8-tile radius, cb-bracket respected).

---

## Roadmap (in priority order)

### Now
1. Verify retro toggle works after autopopulate.
2. Get monster-retaliation log output and pinpoint the failure.
3. User tests PK bots in wildy.

### Next (cosmetics)
The user's stated next phase. Same play as retro:
1. Identify cache cosmetic kits (look for "kit" in item names,
   ornament kits, full kits like "Trickster outfit", "Sea singer
   robes").
2. New `CosmeticKits.java` similar to `RetroSwaps` — map
   kit → equipment slots → cosmetic id mapping.
3. Add to admin Items tab — "Auto-build Cosmetic Map" button.
4. Wire through `Equipment.getCosmeticItems()` (already exists)
   or render-time swap in `Appearence`.

### Later
- Verify and fix any remaining Burthorpe / location bot issues.
- Add more PK bot variants (different combat styles, gear sets
  for testing).
- Cosmetic skin admin panel UI (drag-drop kit builder).
- Possible: server-configurable login-screen background (option
  3 from earlier discussion). Requires client patching.

---

## Important paths and files

### Server source layout
- `src/com/rs/Settings.java` — global server config
- `src/com/rs/game/player/Player.java` — player main + Oracle
  fields + pkOptIn + oldItemsLook
- `src/com/rs/game/player/Appearence.java` — worn appearance
  + retro swap (line 237)
- `src/com/rs/game/player/CombatDefinitions.java` — sheathe,
  combat mode, autoRelatie
- `src/com/rs/game/player/controllers/Wilderness.java` — wildy
  controller, canAttack symmetric PK guard
- `src/com/rs/game/player/content/EconomyManager.java` — Oracle
  of Dawn dialogue (chatbox-paginated)
- `src/com/rs/game/player/content/ItemConstants.java` — tradeable
  with override layer
- `src/com/rs/game/player/actions/PlayerCombatNew.java` — combat
  action (autoRelatie at line 2364)
- `src/com/rs/game/npc/NPC.java` — NPC main, retaliation hooks,
  diagnostic logging
- `src/com/rs/game/npc/combat/NPCCombat.java` — combat process,
  checkAll, target lifecycle
- `src/com/rs/cache/loaders/ItemDefinitions.java` — opcode
  parser, oldLook fields
- `src/com/rs/net/encoders/WorldPacketsEncoder.java` — sendItems
  / sendItemsContainer with retro swap
- `src/com/rs/net/decoders/handlers/ObjectHandler.java` — door
  / portcullis / gate fallbacks
- `src/com/rs/net/decoders/handlers/InventoryOptionsHandler.java`
  — item-on-item including dye recolor
- `src/com/rs/admin/AdminHttpServer.java` — all `/admin/*`
  endpoints (very large file)
- `src/com/rs/utils/RetroSwaps.java` — retro/replica swap map
- `src/com/rs/utils/DyeRecolors.java` — dye recolor map
- `src/com/rs/utils/TradeableOverrides.java` — runtime tradeable
  overrides
- `src/com/rs/bot/AIPlayer.java` — bot Player subclass
- `src/com/rs/bot/BotBrain.java` — Legend bot AI (huge)
- `src/com/rs/bot/ambient/CitizenBrain.java` — Citizen FSM
- `src/com/rs/bot/ambient/CitizenSpawner.java` — citizen spawn
  logic
- `src/com/rs/bot/ambient/CitizenBudget.java` — default citizen
  populations
- `src/com/rs/bot/ambient/AmbientArchetype.java` — archetype
  enum
- `src/com/rs/bot/ambient/BotTradeHandler.java` — trader /
  gambler bot logic + catalogs
- `src/com/rs/bot/ai/BotPathing.java` — A* + door + ladder
- `src/com/rs/bot/ai/BotTeleporter.java` — bot teleports +
  lodestones
- `src/com/rs/bot/ai/EnvironmentScanner.java` — find NPCs /
  objects / fishing spots
- `src/com/rs/bot/ai/TrainingMethods.java` — training method
  catalog
- `src/com/rs/bot/ambient/BotChatListener.java` — WTB / WTS
  parser

### Data files
- `data/admin/citizen_budget.json` — local citizen budget
  (panel-saved)
- `data/items/dye_recolors.json` — dye → (source → result) map
- `data/items/retro_swaps.json` — base → retro/replica id map
- `data/items/tradeable_overrides.json` — forced tradeable /
  untradeable
- `data/items/unpackedShops.txt` — shop catalogs
- `data/items/unpackedExamines.txt` — examine text (can search
  by name here without runtime cache)
- `data/npcs/customSpawnsList.txt` — NPC custom spawns
- `data/npcs/unpackedSpawnsListN.txt` — default NPC spawns
- `data/gear_sets.json` — bot outfit pool

### Admin panel
- `admin_panel.py` — single-file customtkinter panel
- API class `MatrixAPI` at top of file with all endpoint
  helpers

### Admin HTTP endpoints
- `GET /admin/stats` — server stats
- `POST /admin/citizens/budget` — save (whitespace-tolerant
  JSON parser)
- `POST /admin/citizens/budget/apply` — spawn from budget
- `GET /admin/items/all` — paged item list (filters: q, slot,
  wearable, tradeable, override)
- `POST /admin/items/tradeable` — set runtime tradeable override
- `GET /admin/items/dyes/scan` — list dye + tinted items
- `POST /admin/items/dyes/autopopulate` — build dye_recolors.json
- `POST /admin/items/dyes/reload`
- `GET /admin/items/oldlook-scan` — list retro/replica items
- `POST /admin/items/retro/autopopulate` — build retro_swaps.json
- `POST /admin/items/retro/reload`
- `GET /admin/ge/prices?catalog=1` — GE catalog prices
- `POST /admin/ge/prices/bulk` — bulk price set
- `GET /admin/phantom-ge` / `POST` — fill rate config
- (auth via `Authorization: Bearer <token>` on all routes)

---

## Known gotchas / pitfalls

1. **Compile encoding:** always use `-encoding ISO-8859-1`.
   `Bank.java` and `Summoning.java` have non-UTF8 chars.
2. **JVM holds old classes:** `javac` compiles to disk but the
   running game process still holds the old classes. ALWAYS
   restart the game after recompiling. The admin panel is a
   separate process — restart that for Python changes.
3. **Interface 1312 has cs2 onLoad fortune-cookie text** that
   bleeds into menu options. Don't use 1312 for menus — use
   1188 (chatbox sendOptionsDialogue).
4. **AIPlayer is a Player** (extends Player). `instanceof Player`
   is true for bots; that affects aggro / canAttack / scene
   queries. Some queries filter `clientHasLoadedMapRegion`,
   AIPlayer overrides that to true.
5. **Citizens use CitizenBrain (not BotBrain).** Their tick is
   a full override, parent goal-driven AI doesn't run.
6. **`sendItemsLook()` opcode 159 crashes 830 client.** Don't
   re-enable. Render swap via `RetroSwaps.toOld(item.getId())`
   in the encoders is the working path.
7. **Default `oldItemsLook` is false, default `pkOptIn` is false.**
   New players don't get either by default.
8. **Custom spawns file format:** `<npcId> - <x> <y> <plane>`.
   Lines starting with `//` are comments. After editing,
   restart server (custom spawns load at boot).
9. **Bots inherit player attributes** — `setPkOptIn(true)` on a
   bot works because AIPlayer is a Player. Combatant bots
   auto-opt-in at spawn.
10. **Phantom GE** filling depends on item being in
    `BotTradeHandler.catalogPriceFor(itemId)` — items not in
    the catalog never get a reference price and never fill.
    Add via the GE Prices tab or the catalog source if a player
    reports an item never trades.

---

## Quick test recipes

```
# Restart everything
killall -9 java && cd ~/matrix/Server && \
  javac -encoding ISO-8859-1 -cp "data/libs/*:src" -d bin $(find src -name "*.java") && \
  ./run_game.sh &
```

```
# Build retro swap map (REQUIRED before retro toggle works)
curl -X POST -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8090/admin/items/retro/autopopulate
# Should report ~38 added.
```

```
# Verify retro_swaps.json is populated
head -c 500 ~/matrix/Server/data/items/retro_swaps.json
```

```
# In-game retro toggle:
# Talk to Oracle of Dawn -> Account & Character management
#   -> Switch to retro items look
# Equipment + inventory icons should swap to retro skin immediately.
```

```
# In-game PK opt-in:
# Talk to Oracle of Dawn -> Account & Character management
#   -> PK opt-in: ON
# Walk into wildy. Combatant bots within 8 tiles + cb bracket should engage.
```

```
# Monster retaliation diagnostics:
# Watch the server stdout while attacking a passive NPC.
# Look for [NPC-RETAL-HIT] and [NPC-RETAL] tag lines.
```

---

## Open questions for the next thread

1. **Retro toggle:** does the user have `data/items/retro_swaps.json`
   populated? If yes and toggle still doesn't work, dig into
   render-cache. If no, run autopopulate.
2. **Monster retaliation:** what do the `[NPC-RETAL-HIT]` and
   `[NPC-RETAL]` log lines say after the user attacks a passive
   NPC?
3. **Cosmetics:** how is the user planning to add cosmetics — are
   they custom items they'll add, or existing kits in cache?
4. **Login screen background:** user said "not important for now".
   Park until they bring it back up.

---

End of handoff.
