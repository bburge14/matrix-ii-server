# Matrix II — Working Session Summary + TODO

Living document tracking the Matrix II RSPS server bot AI + cache
migration work. Update as items complete or new work lands.

Last updated: 2026-05-01

---

## Where we are today

**Server:** Runs on cache 830 (fallback). Dual-cache architecture is in
place; will auto-promote to 876 whenever a properly-packed 876 cache
is dropped at `~/matrix/876_packed/`.

**Cache 876 status:** OpenRS2 flat-format extracted into
`~/matrix/876_packed/cache/`. Custom `pack` subcommand added to
OpenRS2's `cache-cli` at `tools/openrs2-pack/`. First pack attempt
hit `StoreFullException` at 8.7 GB (JS5 MAX_BLOCK ceiling). Second
attempt (skipping music indexes 14 + 40) is the current move.

**Client 876 status:** Gemini bumped `game/Client.java` build constant
`831 → 876` at lines 566, 7336, 7622, 7825. Rebuilt cleanly.

**Citizens:** AIPlayer-backed (not NPC), full player rendering, real
XP, real gear, real combat via the SAME `tryStart*` methods Legends
use. Consume `TrainingMethods` for location/skill data. Rate-limited
spawn (1 per 5 ticks) to avoid packet-buffer overflow.

**Legends:** Existing BotBrain-based AIPlayers. Full goal-driven AI
with LifetimeIdentity bias, ranked training methods, Plan A/B/C
fallback, crowd-balancing, stuck-detection.

---

## Common gotchas

- **Login launcher needs a TTY.** `Scanner.nextLine()` at
  `LoginLauncher.java:65` reads stdin. Run in the foreground, not
  backgrounded with `&`.
- **Port binding failures on restart.** Old server processes retain
  sockets on ports 8090 (admin) + 43594 (game) for ~30s. Always run
  `pkill -f LoginLauncher && pkill -f GameLauncher && sleep 3`
  before starting a new session.
- **`packedShops.s` cache.** Delete `data/items/packedShops.s` before
  restart if you've edited `unpackedShops.txt` and want the changes
  loaded. Server prefers packed if present.
- **Compile encoding.** Two source files have non-UTF-8 bytes
  (`Bank.java`, `Summoning.java`). Always compile with
  `-encoding Cp1252` — the wrapper scripts already do this.

---

## Standard test cycle

```bash
# 1. Kill any stale processes
pkill -f LoginLauncher; pkill -f GameLauncher; sleep 3

# 2. Pull + compile
cd ~/matrix/Server
git pull origin claude/resume-session-liWrt
find src -name "*.java" -type f > /tmp/srcs.txt
javac -encoding Cp1252 -d bin -cp "data/libs/*:src" \
      -source 1.8 -target 1.8 -Xlint:none -nowarn @/tmp/srcs.txt
rm -f /tmp/srcs.txt

# 3. Boot - TWO SEPARATE TERMINALS
# Terminal 1
./run_login.sh
# Terminal 2
./run_game.sh
```

---

## Bot architecture (Legends vs Citizens)

Both are **AIPlayer subclasses** — visually indistinguishable from real
players. Difference is purely in the brain:

| | Legend | Citizen |
|---|---|---|
| Class | `AIPlayer` | `AIPlayer` (same) |
| Brain | `BotBrain` (goals) | `CitizenBrain` (FSM + role) |
| Persisted to disk | Yes | No (ephemeral) |
| Tracked in | `BotPool.online` | `CitizenSpawner.liveCitizens` |
| Decision making | Goal-ranked, LifetimeIdentity bias, Plan A/B/C | Random pick from role's method pool |
| Per-tick decision cost | Heavy (~10x) | Light |
| Real Actions fired | Yes (Woodcutting, Combat, etc) | Yes (same code path) |
| XP / loot / damage / death | Yes | Yes (same as Legends) |
| Budget | ~50 marquee | 400+ decoration |

Shared plumbing (any fix benefits BOTH tiers):
- `TrainingMethods` — where trees/rocks/NPCs are, level gates, item reqs
- `EnvironmentScanner` — find nearest tree/rock/fish/NPC/named object
- `WorldKnowledge` — hardcoded region coords
- `BotEquipment` — tier-appropriate gear loadouts
- `BotSkillProfile` — stat generation per archetype
- `BotPathing` — pathfinding helpers
- `BotBrain.tryStart*()` — the actual "fire the real Action" code
  (protected; CitizenBrain inherits and dispatches to them)

---

## Directory / file map (important stuff only)

### Repo root
```
~/matrix/Server/
├── src/com/rs/
├── data/
│   ├── libs/                  # required JARs (FileStore, netty, mysql, minifs)
│   ├── items/unpackedShops.txt
│   ├── citizen_budget.json    # persistent citizen population config
│   ├── dump/830_to_876.txt    # cache inventory (30k items / 33k objs / 18k NPCs)
│   └── logs/audit.log         # streaming bot activity log
├── bin/                       # compiled .class files
├── tools/
│   ├── openrs2-pack/          # flat→packed cache converter (uses OpenRS2)
│   └── ...
├── run_login.sh
├── run_game.sh
├── run_cache_diff.sh
├── admin_panel.py             # Python admin tool (customtkinter)
└── Matrix_TODO.md             # THIS file
```

### Bot code
```
src/com/rs/bot/
├── AIPlayer.java              # subclass of Player - hydrate(), start(), etc.
├── BotBrain.java              # Legend AI: tick loop, tryStart*() methods
├── BotFactory.java            # create() / createOffline() - build AIPlayers
├── BotPool.java               # persistent Legend registry
├── BotSkillProfile.java       # stat generation per archetype
├── BotEquipment.java          # gear loadouts (tier-gated, verified IDs)
├── BotNames.java              # themed name generator
├── BotAuditor.java            # scheduled TrainingMethods coord audit
├── AuditLog.java              # streaming log writer
├── SuccessTracker.java        # per-method picked/success/stuck tally
├── ai/
│   ├── TrainingMethods.java   # SHARED: method table (location + kind + reqs)
│   ├── EnvironmentScanner.java # SHARED: find nearest tree/rock/npc
│   ├── WorldKnowledge.java    # SHARED: hardcoded region coords
│   ├── Goal.java              # Legend goal (isAchievable, addLifetimeBoost)
│   ├── GoalType.java          # goal enum
│   ├── GoalStack.java         # Legend goal management
│   ├── LifetimeIdentity.java  # 8 long-term identities
│   ├── ArchetypeGoalGenerator.java # generates goals per archetype
│   ├── BotTeleporter.java     # magic/jewelry teleports
│   ├── BotPathing.java        # walk helpers
│   ├── BotTrading.java        # (stub) bot-to-bot trade
│   └── BotCombat.java         # combat mode + prayer selection
└── ambient/
    ├── AmbientArchetype.java  # 16 role enums + lobby tiles
    ├── CitizenBrain.java      # Citizen FSM (IDLE/TRAVERSING/INTERACTING/PANICKING)
    ├── CitizenSpawner.java    # rate-limited spawn (1/5 ticks)
    └── CitizenBudget.java     # persistent population config
```

### Cache + admin
```
src/com/rs/
├── Settings.java              # CACHE_PATH_PRIMARY / _LEGACY / _DLC
├── cache/
│   ├── Cache.java             # STORE + STORE_DLC + smoke-test fallback chain
│   └── loaders/
│       ├── ItemDefinitions.java   # uses Cache.getFileWithDlcFallback
│       ├── ObjectDefinitions.java # same
│       └── NPCDefinitions.java    # same
├── admin/
│   └── AdminHttpServer.java   # HTTP endpoints: /admin/citizens/* /profiler/* /cache/*
├── executor/
│   ├── WorldThread.java       # instrumented per-phase timing
│   └── WorldTickProfiler.java # start/stop/dump per-phase stats
├── tools/
│   ├── CacheDiffUtility.java  # compare 2 caches (item/object/npc id shifts)
│   └── CacheRepacker.java     # ABANDONED - OpenRS2 CLI is better
└── game/player/content/commands/Commands.java # in-game ::commands
```

### Client (separate repo)
```
~/matrix/Matrix RS3 Client/    # separate codebase, NOT in server repo
├── src/game/Client.java       # build constant 876 at lines 566, 7336, 7622, 7825
├── src/game/RS3Applet.java    # main entry
├── clientlibs.jar
└── run_client.sh
```

### Launcher (client selection)
```
~/matrix/Server/Brad's Playground.bat  # prompts for cache/client at startup
                                        # expects client_830/ or client_876/ subdirs
                                        # + shared jre/ for Java runtime
```

---

## Cache 876 migration path

### What worked
- `Settings.CACHE_PATH_PRIMARY` = `~/matrix/876_packed/`
- `Cache.init()` fallback chain: PRIMARY → LEGACY (830) → raw Settings.CACHE_PATH
- `canReadCriticalArchives()` smoke test (reads Huffman index 10 before accepting)
- OpenRS2 clone + custom `pack` subcommand at `tools/openrs2-pack/`
- Rebuild + drop-in via `./install_and_pack.sh`

### What's outstanding
- **Music indexes 14, 40 skipped** in the packer (hits 8.7 GB dat2 ceiling).
  Player-side effect: no sound/music. Server functionally unaffected.
- If music is needed later: extend `FlatDiskPackCommand` to open a
  separate `main_file_cache.dat2m` for archives 14 + 40 (OpenRS2's
  standard split). ~30 more lines of Kotlin.

### To flip to 876 after successful pack
```bash
rm -rf ~/matrix/876_packed/{main_file_cache.*,cache,cache.zip,flat-file}
mv ~/matrix/876_repacked/main_file_cache.* ~/matrix/876_packed/
# restart server - Cache.init smoke-test auto-promotes
```

---

## In-game commands (all admin-only)

### Citizen management
- `::spawncitizen <count> [category]` — spawn N citizens (category = skiller / combatant / socialite / minigamer / mixed). Rate-limited to 1 per 3 seconds.
- `::clearcitizens` — despawn all
- `::citizencount` — live count
- `::citizeninfo [N]` — dump N citizen states + aggregate by archetype/state/method

### Auditing
- `::auditstart` / `::auditstop` — stream bot events to `data/logs/audit.log`
- `::auditmethods` — one-shot TrainingMethods coord audit
- `::botinfo <name>` — per-bot state + last diagnostic
- `::botscan <name>` — EnvironmentScanner dump at bot's tile
- `::botforce <name> <skill>` — manually trigger a method

### Profiling
- `::profilestart` — WorldThread per-phase timing (every 100 ticks to stdout)
- `::profilestop` / `::profiledump`

### World scanning
- `::npchere` — list NPCs near player

---

## OUTLINE / CHECKLIST

### DONE (recent → oldest)
- [x] OpenRS2 cache pack tool (`tools/openrs2-pack/`) — FlatDiskPackCommand + installer
- [x] Citizens fire REAL actions (shared `tryStart*` with Legends) — commit `1f4852aa`
- [x] Citizens consume `TrainingMethods` + `::citizeninfo` command
- [x] Citizens use `BotSkillProfile` + `createOffline` pipeline (matches Legends)
- [x] Citizens use `EnvironmentScanner` + Legend-style names
- [x] Citizens: `BotSkillProfile.build("set", ...)` for real combat levels (was `"default"` which returned null)
- [x] Phase 5: Admin panel Citizens tab + budget editor + autospawn-on-boot
- [x] Phase 4: Legend vs Citizen split (natural from type system)
- [x] Phase 3: 16 archetypes (11 originals + castle wars / soul wars / stealing creation minigame variants)
- [x] Phase 2: `AmbientBot` → `CitizenBrain` FSM (IDLE/TRAVERSING/INTERACTING/PANICKING)
- [x] Phase 1.C step 1: `WorldTickProfiler` per-phase timing
- [x] Phase 1.B: Dual-cache FileStore (PRIMARY / LEGACY / DLC) + smoke-test fallback
- [x] Phase 1.A: Understand Cache/FileStore/Settings architecture
- [x] `CacheDiffUtility` for 830 vs 876 item/object/NPC ID delta reports
- [x] 830 cache inventory dump (30k items / 33k objects / 18k NPCs) at `data/dump/830_to_876.txt`
- [x] Combat depth pass 1: Legacy<60, Revolution>=60, prayer activation, food@50%, prayer pots
- [x] Archetype skill diversity: combat archs train non-combat skills too, skillers stay non-combat
- [x] Client: build constant `831 → 876` at 4 sites (via Gemini)
- [x] Success/failure tally (`SuccessTracker.dump()`)
- [x] Audit + auto-snapshot + audit-log streaming
- [x] Tier 0: Goal hierarchy (Lifetime → Active → Step → Activity)
- [x] Tier 1: Activity layer (real-action wiring for WC/Mining/Fishing/Combat/Thieving/Firemaking/Cooking/Smelting/Crafting/Prayer)

### IN PROGRESS
- [ ] OpenRS2 cache pack: verify final packed output works with server (last commit: skip music indexes 14, 40)

### NEXT UP (priority order)
- [ ] Resume Legend tasks/locations debugging via `audit.log` — same fixes benefit Citizens now
- [ ] Phase 1.C step 2: Run profiler under load (real players + 100 citizens), identify hot phase
- [ ] Phase 1.C step 3: Parallelize hottest WorldThread phase (3 of 4 vCPUs)
- [ ] Audit + fix shop IDs against 830 inventory
- [ ] Skill trader shop tier rework (bronze→rune+ per skill master)
- [ ] Combat depth pass 2: EoC ability bar setup + protection prayer per enemy attack type
- [ ] Personality-weighted ranking (Tier 3.A): efficiency/social/risk dimensions
- [ ] Remaining process skills (1.H): Herblore / Agility / Runecrafting / Hunter / Summoning / Farming / Construction
- [ ] Lunar / Ancient teleport spells (2.H)
- [ ] Legend bots: real trade with real players (offer windows) + social interaction (chat replies, follow, party-up)

### FUTURE (roadmap, not yet started)
- [ ] Phase E1: GE auto buy/sell — working order matching engine
- [ ] Phase E2: Realistic economy — prices fluctuate on buy/sell volume; Citizens/Legends drive market
- [ ] Bot-to-bot chat + bot-to-bot trading
- [ ] Tier 3 polish: money makers beyond gathering, social variety, death recovery, memory/adaptive learning, fluff (bankstand fashionscape, mini-games)
- [ ] Music dat2m secondary file for cache 876 (if music becomes needed)

---

## Recent commits (branch `claude/resume-session-liWrt`)

- `723c178e` OpenRS2 pack: skip music indexes (14, 40) to fit under 8.7GB dat2 limit
- `f460a174` OpenRS2 pack: explicit DiskStore.create for output, wipe stale output
- `6769645c` OpenRS2 pack: drop `org.openrs2.buffer.use`, use try/finally + release()
- `7dbdba20` OpenRS2 flat→disk pack: tooling + installer at `tools/openrs2-pack/`
- `1f4852aa` Citizens fire REAL Actions — same execution path as Legends
- `598717d8` Citizens: `BotSkillProfile` mode `"default"` returns null - switch to `"set"`
- `e1655428` Citizens consume `TrainingMethods` + per-tier audit + `::citizeninfo`
- `3ea6d154` Citizens: unify stat+gear pipeline with Legends (`BotSkillProfile` + `createOffline`)
- `808a37f1` Citizens: real interaction targets + Legend-style names
- `aab58239` Citizens: fix double-init crash - use `createOffline` + `hydrate` (matches BotPool)
- `ddff21d8` Citizens: AIPlayer-based again, 1-spawn-per-5-ticks pacing (real player look)
- `3a6a71d6` Cache: smoke-test reads before accepting primary, auto-detect new 876
- `327cf7ac` Phase 1.B: Dual-cache architecture (PRIMARY 876 / LEGACY 830 / DLC 900+)
- `dc63a77a` Phase 1.C step 1: `WorldTickProfiler`
- `ac60e8ba` Phase 2+3+4: `AmbientBot` FSM + 11 archetypes + Citizen layer (original NPC-based, later replaced)
- `974ee8e4` CacheDiffUtility: 876 vs 900+ delta dumper
