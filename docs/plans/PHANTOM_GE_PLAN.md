# Auto-GE Phantom Market — Plan

Goal: make the in-game Grand Exchange feel alive without spawning bot
players to place offers. When a player puts up a buy or sell offer, it
has a chance to be filled automatically at a simulated price, on a
realistic timeline.

---

## Approach: shadow matching, not phantom offers

Two ways we could fake GE activity:

| Approach | Pro | Con |
|---|---|---|
| **A) Phantom offers** — create offers in the OFFERS map under a "house" account | Visible in GE history, looks "real" | House account needs management, offers leak on crash, infinite-bag accounting |
| **B) Shadow matching** — when a player offer arrives, roll a fill chance and fulfil from a virtual counter-party | No state to leak, no fake account, offers just fill normally | Player can't see "house" offers in their GE history (they just see their offer fill) |

**Going with B** — it's simpler, has no persistent state to manage,
and from the player's POV it's indistinguishable from another player
filling their offer.

---

## Architecture

### New file: `src/com/rs/game/player/content/grandExchange/PhantomMarket.java`

Static class. Two entry points:

1. **`onOfferPlaced(Offer offer)`** — called from `GrandExchange.placeOffer()`
   right after a player creates a new offer. Schedules a delayed fill task
   on the world tick scheduler.

2. **`tickAgingOffers()`** — called every 30s from the world scheduler.
   Walks all open player offers and rolls the per-tick fill chance for
   anything older than `MIN_AGE_BEFORE_FILL`.

### Hook points

`GrandExchange.placeOffer()` — drop in `PhantomMarket.onOfferPlaced(offer)`
at the end. Already called once per new offer.

`GameLauncher` startup — schedule `PhantomMarket.tickAgingOffers()`
every 50 ticks (~30s).

### Fill mechanism

When the market decides to fill an offer:

```java
private static void fill(Offer offer, int quantity, int unitPrice) {
    // Mirror the path real player-vs-player matching takes:
    //   - reduce offer.amountLeft by quantity
    //   - credit offer.coinsCollected (for sell) or offer.itemsCollected (for buy)
    //   - update offer state (FINISHED if amountLeft == 0)
    //   - send the player a GE notification packet
    //   - persist via SerializableFilesManager.saveGEOffers()
}
```

Reuse whatever method already handles a successful match in `Offer.java`
(`tryMatch` / `completeOffer` / etc — need to find it).

---

## Pricing model

For each item we know about (every entry in the bot trader catalog +
anything with `ItemDefinitions.getValue() > 0`):

```
ref_price = GrandExchange.getPrice(itemId)   // current live GE price
spread    = uniform(-SPREAD%, +SPREAD%)
fill_price = ref_price * (1 + spread)
```

When filling a **player's BUY offer**, we sell to them at:
`min(fill_price, player_offer_price)` — players never pay more than they
asked.

When filling a **player's SELL offer**, we buy from them at:
`max(fill_price, player_offer_price)` — players never get less than they
asked.

If `player_offer_price` is way out of band (e.g. trying to buy a phat
for 1gp), the phantom market refuses — only realistic offers fill.

---

## Configurable knobs

Stored in `data/phantom_market.json`, editable via admin panel:

| Knob | Default | Purpose |
|---|---|---|
| `enabled` | true | Master kill-switch |
| `fillRateOnPlace` | 0.10 | Per-offer chance to fill within 5s of placing |
| `fillRatePerTick` | 0.02 | Per-30s chance to fill an aging offer |
| `priceSpread` | 0.05 | ±5% noise on fill prices |
| `acceptableSpread` | 0.30 | Reject offers >30% off ref price |
| `minAgeBeforeFill` | 30s | Offer must sit for X before any fill |
| `maxFillsPerPlayerPerHour` | 50 | Anti-farm cap |
| `maxFillsPerItemPerHour` | 20 | Per-item cap |
| `partialFillChance` | 0.4 | When filling, 40% chance to fill only some of the offer |
| `partialFillRange` | [0.2, 0.7] | If partial, fill 20–70% of remaining qty |

---

## Fill rate tuning per item tier

Override the per-tick fill rate by item tier so common items move fast
and rares move slowly:

```
TIER_BULK     (logs, ores, runes, fish, bones)   fillRatePerTick = 0.10
TIER_COMBAT   (dragon weapons, rune armor, etc)  fillRatePerTick = 0.04
TIER_RARE     (godswords, bandos, partyhats)     fillRatePerTick = 0.005
TIER_UNKNOWN  (anything else)                    fillRatePerTick = 0.02
```

So a player listing yew logs at GE price gets matched within a minute
or two; a player listing a partyhat at GE price waits 30+ minutes.

Tier inferred from `BotTradeHandler.catalogPriceFor(itemId)`:
- < 10k -> BULK
- < 1m -> COMBAT
- ≥ 1m -> RARE

---

## Anti-abuse

- **Per-player fill cap**: 50/hour, tracked in-memory + persisted.
  Resets on server restart. Stops anyone from sitting at the GE
  flipping for free with phantom counter-parties.
- **Per-item fill cap**: 20/hour total across all players. Stops
  someone from buying out the phantom's "supply" of e.g. partyhats.
- **Wash-trade detection**: same player listing buy + sell at the
  same price doesn't trigger phantom fills (would be free gold).
- **Offer-aging requirement**: 30s minimum before any phantom fill,
  so player has time to cancel a misclick.

---

## Admin panel integration

Add a "Phantom GE" section to the existing `GE Prices` tab:

- Master toggle (enabled / disabled)
- Per-tier fill rate sliders
- Live counter: phantom fills today / this hour
- Fill log (last 50 events): time, player, item, qty, price, side
- Per-player override (block specific players from phantom fills)

Endpoints:
- `GET /admin/phantom/config` — returns current knobs
- `POST /admin/phantom/config` — updates knobs (saves to disk)
- `GET /admin/phantom/log?n=50` — recent fill events
- `POST /admin/phantom/toggle` — flip master on/off

---

## Implementation order

1. Hook + scheduler skeleton (no fills yet, just logging)
2. Pricing function + fill mechanism
3. Per-tier fill rates + spread + acceptable-spread refusal
4. Anti-abuse caps
5. Persistence of knobs + fill counters
6. Admin panel UI

Roughly a 3–5 hour build for everything end-to-end.

---

## Open questions to confirm before I build

1. **Should phantom-fill events show up in `OfferHistory`?** RuneScape's
   GE history shows past offers including counter-party. With shadow
   matching, the counter-party would have to be "(market)" or similar.
   OK? Or want it to show a fake username?

2. **Does the player's offer price affect the chance, or just the price?**
   E.g. if I list a whip for 50% of GE price, should it fill faster
   (because it's a great deal for the buyer)? Or same chance, just
   filled at the player's listed price?

3. **Cap on how cheap a phantom will sell?** If a player lists a buy
   offer at 1gp for a phat, the phantom should obviously decline. Where
   should we draw the line — 50% of ref price? 70%?

4. **Should certain items be excluded entirely?** Untradeable items,
   PvP gear with charges, items that only have value through quests
   (e.g. dragon defender)?

Answer those and I'll start building.
