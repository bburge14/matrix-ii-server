# Bot System — Plan & Status

Living document. Updated as we ship features. Architecture and roadmap
for the Matrix II bot population (Legends + Citizens).

---

## Architecture (one-page)

Two tiers of bots:

```
                         AIPlayer (extends Player)
                              │
                ┌─────────────┴─────────────┐
                │                           │
            BotBrain                   CitizenBrain
        (Legend bots)              (Citizen bots, FSM)
        goal-driven AI              IDLE/TRAVERSE/INTERACT
        full TrainingMethods        + per-archetype hooks
        memory + identity           ephemeral (no save)
        saved JSON per bot          population from
                                    citizen_budget.json
```

Both are real `Player` subclasses with a `NullSession`. They render
fully (other players see them as players), can walk, animate, talk,
trade, accept/reject UI events. The difference is purely in what they
*decide* to do.

### Subsystems

- **BotPool / BotFactory** — spawn pipeline, hydrate, equipment
- **BotEquipment** — per-archetype gear loadouts with slot validation
- **BotSkillProfile** — skill stat baking ("set" mode = build to target cb)
- **BotTeleporter** — jewelry + spellbook teleport for cross-world travel
- **TrainingMethods** — shared catalog of skill activities (locations,
  required items, stat gates) used by both BotBrain and CitizenBrain
- **EnvironmentScanner** — tile/object/NPC lookup for activities
- **CitizenBrain FSM** — IDLE / TRAVERSING / INTERACTING (PANICKING removed)
- **CitizenBudget** — persistent population config (data/citizen_budget.json)
- **BotTradeHandler** — socialite trade lifecycle (gambler dice, trader sales)
- **BotChatListener** — public-chat WTB/WTS detection + in-trade qty narrow
- **BotConversations** — bot-to-bot two-line dialog threads

---

## Status — completed

### Bot trading (✅ shipped)

- Gambler 2-trade payout flow with paced 4-phase announce
- Single dice mode (55x2) — no advert spread across population
- Real dice item (15098) on spawn + animation 11900 during roll
- Gambler bankroll 250m, MAX_BET 100m, refuses if over (no silent capping)
- Trader puts FULL stack on trade open (was 1 unit)
- Tiered catalog: SKILL / COMBAT / RARE archetypes with separate spawn anchors
- 30-min stock rotation per bot (StockSalt forces different pick)
- Stock depletion for rares (≥1m items don't auto-restock — bot rotates)
- Multi-unit cheap items: <10k offers 10, <100k offers 5, ≥100k offers 1
- Per-unit pricing + chat narrow ("i want 25 logs") with exact-gp validation
- Underpay protection: trader rejects accept if `playerGp < sellUnits * price`
- WTB/WTS chat listener with item alias DB (~140 items) + price tolerance ±25%
- BUY (player buys = bot sells) + SELL (player sells = bot buys) flows
- 50% response chance on WTB/WTS — bots don't acknowledge every shout
- 30s per-bot cooldown so spam doesn't trigger same bot repeatedly
- Trade.java null-safe for offline bot session (was the trade-cancel bug)
- Mystic ID mismatch fixed (4097/4099 mislabelled across 3 files)
- Catalog catalog → live GE bridge: `/admin/ge/prices/bulk` + push tools
- Holiday rares at 100m+ floor (partyhats / hween / santa / cracker)

### Citizen behavior (✅ shipped)

- PANICKING state removed (no more fleeing)
- Per-bot FSM phase offset (no sync'd dance)
- Spawn scatter ≥12 tiles + tile-occupied rejection
- Walkable-tile validation on spawn + wander
- GE socialite anchors per role (gambler / trader-tier / bankstand)
- Edge bank socialites (3+2 traders, 4 bankstand, 4 gambler)
- 98% IDLE-stay for socialites
- Schema-versioned citizen_budget.json with auto-migrate

### Equipment (✅ shipped)

- Slot-mismatch validator in `equip()`
- 23 coordinated socialite outfit presets (was 13)
- `applyAccessories` skipped for socialites (was overlaying team capes)
- Comp/max capes blocked from socialites; rare on maxed
- Holiday rare hat (5%) overrides outfit hat

### Chat (✅ shipped)

- Effects only for traders/gamblers (plain for everyone else)
- ForceTalk dropped — no flicker back to yellow
- Bot-to-bot conversations (80+ two-line threads)
- "...!" debug chat replaced (panic state itself removed)
- Trader/gambler advert frequency 30s → 10s with phase offset
- fmtGp shorthand: 800m / 50k / 1.5b in all chat (not 800000000gp)
- Profanity filter forced off server-wide

### Tooling (✅ shipped)

- `tools/ge_price_audit.py` — fetch RS3 GE, --apply rewrites catalog,
  --push-server pushes to live GE
- `tools/ge_push_catalog.py` — one-shot push catalog to live GE
- Admin panel "GE Prices" tab — view/edit/save with shorthand
- Admin panel per-archetype quick-spawn buttons
- Admin panel live citizens detail table with filter

---

## Status — pending / next up

### Tier 1 (bigger features, this branch)

1. **Phantom GE market maker** — auto-fill player GE offers without
   bots. See `PHANTOM_GE_PLAN.md`. ~3–5 hour build.

2. **Multi-item bank sales** — trader puts a mix of items in trade,
   player chats "i want logs and ores" to filter. Single-item qty
   narrow already works; mixed-bag mode is the gap.

3. **Auto-restock from "warehouse"** — when a trader rotates after
   selling a rare, the next pick should also be rotated for the
   bot's neighbours so a cluster doesn't all advertise the same
   newly-picked item.

### Tier 2 (smaller polish, this branch)

4. **More outfits** — 23 → 50+ presets. Sailor / fancy hats / colored
   robes / armor combos for skiller-sociallites.

5. **More chat threads** — 80 → 150+. Especially gambler-flavored
   ("rolled 99 last night", "new host in town?").

6. **Conversation chains** — currently 2 lines. Some threads should
   be 3-4 lines, with a chance to continue.

7. **In-trade chat narrow expansion** — "all", "max", "half",
   "everything" already work. Add "less" / "more" for incremental.

8. **Real flower poker** — gambler subtype that uses Flower-poker
   items (2460-2476) and simulates the game flow.

### Tier 3 (architectural / future)

9. **Stub-skill action wiring** — currently auto-XP for Agility,
   RC, Hunter, etc. Wire real Action classes so XP rates match
   gameplay.

10. **Castle Wars / Soul Wars / SC actual gameplay** — currently
    bots stand at lobby. Wire bots to enter the minigame, score
    flag captures / soul fragments / clay deposits.

11. **PvP / wilderness combatant bots** — combatant archetype
    currently only does PvE. Wilderness pkers would make Edge cool.

12. **Bot economy simulation** — trader bots actually buy stock
    from skiller bots, skiller bots farm and sell. Closes the
    economy loop without infinite spawning items.

---

## Status — deferred / blocked

- **876 cache repack** — running on 830 fallback. Was a pre-bot priority,
  shelved to focus on bot AI. Revisit when bot system is stable.
- **Slayer Tower force-load** — partial; some monsters not spawning on
  upper floors. Needs a region-realisation pass.
- **Combat AI for bot fights** — citizens that fight each other or NPCs.
  Crashes were happening; CombatDefinitions init was the issue. Gear
  audit + slot validator may have fixed it; needs re-test.

---

## Known issues

- **Bot fight NPE** (CombatDefinitions.stats null) — might be fixed by
  the eager init we did, needs verification under load.
- **Trade interface stuck open** if player closes their game window
  during a bot trade. Trade.closeTrade is null-safe now but the close
  may not fire if the player session disconnects ungracefully.
- **Citizen drift past wander radius** — rare, happens when an
  INTERACTING target is out of range and the bot walks toward it
  without rechecking the home anchor. Low priority.

---

## Testing checklist (manual)

When testing changes to the bot system, hit these in order:

1. Spawn 30 socialites at GE — outfits look coordinated, no two stacked
2. Spawn 5 gamblers — each carries a dice, advertises 55x2, dice anim
   plays during roll
3. Bet 1m on a gambler — 4-phase paced announce, payout trade opens
   with 2m, deal closes
4. Bet 200m on a gambler — bot refuses ("max bet is 100m")
5. Buy a partyhat from a rare-tier trader — bot puts 1 phat in trade,
   prices announced in shorthand, deal closes for catalog price
6. Try to underpay (500m on a 750m phat) — bot rejects, prompts for
   more gp, doesn't accept
7. Buy second partyhat from same bot — bot says it's out, rotates to
   different rare
8. Type "wtb whip 100k" near a bot — 50% of the time a bot responds,
   opens a trade with the whip
9. Type "wtb whip 1gp" — bot ignores (price out of tolerance)
10. Type "i want 25 logs" while in trade with a logs trader — bot
    adjusts qty, announces 25x logs = Ngp
11. Sit at GE for 5 min — bots converse with each other in 2-line
    threads (no neon effects on casual chat)
12. Watch admin panel "Live Citizens" tab — count stays at budget
    targets, no PANICKING state, no stuck bots

---

## How to extend the system

Adding a new archetype:

1. New entry in `AmbientArchetype.java` with chatter pool + animations
2. Register in `randomFor()` + `isXxx()` predicates
3. Add `socialiteAnchor()` / `lobbyTile()` if the spawn location is
   role-specific
4. Add a slot in `CitizenBudget.seedDefaults()` (bump `CURRENT_SCHEMA_VERSION`)
5. Wire `BotTradeHandler.tick()` if it needs trade behavior
6. Add to admin panel quick-spawn dropdown if user-spawnable

Adding a new training method:

1. New entry in `TrainingMethods.java` with kind + location + reqs
2. Wire `BotBrain.tryStart<Kind>()` for Legend bots
3. Wire `CitizenBrain.tickInteracting` switch case for Citizens
4. Test via auditor that the location's resources are loadable
