# BotCracker — server-side input-telemetry anti-cheat

Package: `com.rs.game.player.anticheat.botcracker`
Data: `data/botcracker/{events,raw,shadow,banned}/`
Admin: `::botcracker status|report <name>|eval <name>|queue|wave preview|wave run|clear <name>|whitelist <name>|raw on|off|set <FIELD> <value>`

BotCracker targets tools that are invisible to packet-rate throttles: hardware
or driver-level remappers, PRNG-timed macro generators, and "fatigue
state-machines" that intentionally vary their cadence. It never looks at the
client. It only measures what the server already sees — *when* each action
arrives relative to the 600 ms world cycle, *how* throughput evolves over
hours, and *where* clicks land on screen — and turns that into a suspicion
score. Flagged accounts are shadow-profiled, not banned, until a ban wave runs.

## Pipeline

```
Netty I/O thread                      World thread (600 ms)             Slow executor
-----------------                     ---------------------             -------------
WorldPacketsDecoder.processPackets    WorldThread.run                   TelemetryLogger.drain
  └─ classifyForBotCracker(opcode)      ├─ BotCracker.onTickStart(ms)      (append-only logs)
  └─ BotCracker.onPacket(kind) ─┐       ├─ ... entities ...               ShadowProfileManager
  └─ BotCracker.onClick(x,y)  ──┤       └─ BotCracker.tick()                .persist / .executeBanWave
                                │            └─ every N ticks per player:
Fletching.processWithDelay ─────┤               HeuristicEngine.evaluate
  └─ BotCracker.onSkillAction   │                 ├─ IATAnomalyDetector
Skills.addXp ───────────────────┤                 ├─ FatigueInvarianceDetector
  └─ BotCracker.onXp            │                 └─ SpatialClusterDetector
                                ▼                      │
                        PlayerTelemetry (per account)  ▼
                        ring buffers + buckets     Verdict(score, sources, findings)
                                                        │ score ≥ SHADOW_THRESHOLD
                                                        ▼
                                                  ShadowProfileManager.shadow()
                                                  (data/botcracker/shadow/<name>.bin)
                                                        │ ::botcracker wave run / timer
                                                        ▼
                                                  BanExecutor (LoginServerBanExecutor →
                                                  login-server offence + kick)
```

### Hook points (all one-liners, already wired)

| Hook | File | What it feeds |
|------|------|---------------|
| `onTickStart(ms)` | `executor/WorldThread.java` | Tick phase reference for alignment analysis |
| `tick()` | `executor/WorldThread.java` (end of cycle) | Staggered per-player evaluation, periodic ban wave |
| `onPacket(player, kind)` | `net/decoders/WorldPacketsDecoder.processPackets` | IAT + phase per `ActionKind` (WALK, ITEM_ON_ITEM, WORLD_INTERACT, INPUT_PACKET) |
| `onClick(player, x, y, button)` | `WorldPacketsDecoder` CLICK_PACKET branch | Screen-space click samples |
| `onSkillAction(player)` | `actions/Fletching.processWithDelay` (add to Crafting, Cooking, Smithing, etc.) | Production cycles for the efficiency ceiling |
| `onXp(player, skill, xp)` | `player/Skills.addXp` | XP per 10-minute bucket |
| `onLogin` / `onLogout` | `Player.start` / `Player.realFinish` | Telemetry lifecycle; logout runs a final evaluation |
| `init()` / `shutdown()` | `GameLauncher` | Loads/persists shadow profiles, flushes logs |
| `BotCrackerCommands.handle` | `content/commands/Commands.processCommand` | Admin surface |

Staff (`rights >= EXEMPT_RIGHTS`) and `AIPlayer` instances are never tracked.

## Detectors

All detectors read **snapshots** of the player's buffers so no lock is held
during the maths, emit `Finding`s with a confidence in [0,1], and are combined
by the engine with noisy-OR after per-source weighting. Noisy-OR rewards
agreement between *orthogonal* signals: two independent 0.6 findings become
0.84, which is exactly the "timing AND spatial AND endurance" corroboration a
moderator would want before acting.

### 1. `IATAnomalyDetector` — inter-arrival time & tick alignment

Per `ActionKind`, the last 2048 inter-arrival deltas and the arrival phase
inside the 600 ms cycle are kept.

| Signal | Statistic | Human | Bot |
|--------|-----------|-------|-----|
| Rigidity | coefficient of variation of IAT | 0.25–0.6 on spam tasks | < 0.08 even with ±60 ms jitter |
| Tick lock | normalised entropy of 12-bin phase histogram, peak share | entropy ≈ 0.9+ | entropy → 0, one bin holds > 60 % |
| Periodicity | lag-1 autocorrelation | ≈ 0 | sawtooth / alternating generators give \|r₁\| > 0.7 |
| Quantisation | unique-delta ratio | high | coarse PRNG steps or fixed clocks give < 5 % unique |

Minimum 400 samples per kind before anything is emitted, so a handful of fast
clicks never flags anyone.

### 2. `FatigueInvarianceDetector` — macro-behavioural entropy

Throughput is accumulated in 10-minute buckets (6 h retained). A gap longer
than `BREAK_GAP_MS` (3 min) counts as a break and restarts the continuous run.
Heuristics only apply once a run exceeds `FATIGUE_MIN_SPAN_MS` (2 h):

* **Efficiency ceiling** — share of buckets at ≥ 92 % of the theoretical max.
  The ceiling is adaptive: it starts from `ActionKind.SKILL_ACTION`'s static
  max and tightens to the player's own 5th-percentile cycle time, so a 4-tick
  fletch is judged against 1500/h, not 6000/h.
* **Invariance** — CV of per-bucket throughput. Scripted "fatigue" curves are
  far smoother than emergent human variance.
* **No drop-off** — last-quarter vs first-quarter throughput; weighted by run
  length because a 2 h flat line is weak evidence and a 5 h one is strong.

### 3. `SpatialClusterDetector` — click clustering & synthetic uniformity

Clicks are greedily clustered within a 24 px radius. For dense clusters:

* **Clustering** — weighted std-dev of offsets from the centre (robotic < 1.5 px)
  and the ratio of *consecutive* pixel-identical clicks, which only counts when
  the scatter is also tight (a human hammering one inventory slot repeats
  pixels but with 4–5 px scatter).
* **Uniformity** — radial kurtosis of the offsets. Human aim is peaked
  (≈ gaussian, kurtosis ≈ 2); jitter drawn from a uniform box is flat
  (≈ 1.33). Also a coarse-grid share test for integer-stride jitter.

## Shadow profiling & ban waves

`ShadowProfileManager` keeps a `ConcurrentHashMap<username, ShadowProfile>`
persisted as Java serialisation under `data/botcracker/shadow/`. Every
evaluation at or above `SHADOW_THRESHOLD` (0.70) appends a snapshot (score,
source count, finding strings). The account sees no change in gameplay.

A wave (`::botcracker wave run`, or automatically every `BAN_WAVE_PERIOD_MS`
when > 0) actions a profile only if:

* mean of the three most recent snapshot scores ≥ `BAN_THRESHOLD` (0.85), and
* at least `BAN_MIN_SOURCES` (2) distinct detectors scored ≥ 0.5, and
* the profile is not whitelisted.

`BanExecutor` is a strategy interface. The default `LoginServerBanExecutor`
files an `OFFENCE_ADDTYPE_BAN` with the login server (same path as `::punish`)
under moderator name `BotCracker` and kicks the account; swap in a temp-ban,
an IP-ban, or a webhook that posts to a review channel. Actioned profiles move
to `data/botcracker/banned/` for the record.

`::botcracker wave preview` lists the candidates without acting.

## Tuning

Every threshold is a public static on `BotCrackerConfig` and can be changed
live with `::botcracker set FIELD value`. Turn on `RAW_TELEMETRY_LOG` for a
suspect to capture every sample (`data/botcracker/raw/<name>.log`, tab
separated: kind, ms, tick phase, x, y) for offline analysis.

`BotCrackerSelfTest` (main class) runs a synthetic human and three synthetic
bots through the engine; humans should print `score=0.000`, bots `≈1.000`
with ≥ 3 sources. Run it after changing any threshold.

## Extending

* Add `BotCracker.get().onSkillAction(player)` to any repetitive `Action`
  (Crafting, Cooking, Smithing, Herblore, Runecrafting) where a production
  cycle completes.
* Implement `Detector` and register it in the `BotCracker` constructor; add a
  `Finding.Source` and a weight in `HeuristicEngine`.
* For world-tile heuristics (camping one exact tile, identical bank-to-spot
  paths) add a tile ring to `PlayerTelemetry` and a detector — the click
  pipeline already shows the pattern.
