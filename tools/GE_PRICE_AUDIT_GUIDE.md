# GE Price Audit — How to run on Ubuntu

This script (`tools/ge_price_audit.py`) hits the live RuneScape GE JSON API
for every item id in the bot trader catalog and produces a diff so you can
review and selectively apply price updates.

## What you get

Two output files in `data/`:

- **`data/ge_audit.csv`** — full report. One row per id with: server name,
  GE name, server price, GE price, suggested price (= GE × scale), %
  delta, flagged Y/N, and the line number in `BotTradeHandler.java`.
- **`data/ge_audit.patch.txt`** — short, eyeball-friendly list of flagged
  items only. Each line is `id  oldPrice -> newPrice  (delta)  name`.

The script does NOT modify any source files. You apply changes by hand
after reading the patch.

---

## Prerequisites

Ubuntu 20.04+ with Python 3 (almost certainly already there). Quick check:

```bash
python3 --version    # expect 3.8+
```

Outbound HTTPS to `secure.runescape.com` (TCP 443). If your server is
behind a strict firewall, run from your laptop instead and copy the
`data/` outputs over.

No pip dependencies — the script uses only the standard library
(`urllib.request`, `json`, `csv`, `re`).

---

## Running it

From the project root (`/home/user/matrix-ii-server` or wherever you
cloned it):

### 1. Pull latest

```bash
cd /home/user/matrix-ii-server
git pull origin claude/review-resume-PQG3h
```

### 2. Default audit (recommended first run)

```bash
python3 tools/ge_price_audit.py
```

What this does:
- Reads all `StockEntry` lines from `BotTradeHandler.java` (~118 items
  in the current catalog).
- Hits the GE API at ~600 ms intervals (≈70 seconds total run time).
- Suggests `server_price = ge_price × 0.10` (RSPS economy ~10% of OSRS GE).
- Flags rows where the suggested price differs from the current price
  by more than 30%.

You'll see live progress like:

```
[  1/118]   id=1511   server=      50  ge=     400  sug=      40  logs
[  2/118] ! id=4151   server=    150k  ge=     2.4m sug=    240k  abyssal whip
```

A `!` in the second column means the row was flagged for review.

### 3. Review the patch

```bash
cat data/ge_audit.patch.txt
```

Example output:

```
# GE Price Audit - suggested edits
# Scale factor: 0.1 (server = ge * scale)
# Threshold:    30% delta
# Total flagged: 14 of 118

4151    150.00k ->   240.00k  (+60.0%)  abyssal whip
11696   1.80m   ->   2.50m    (+38.9%)  armadyl godsword
...
```

### 4. Apply changes (manual)

Open `src/com/rs/bot/ambient/BotTradeHandler.java`, find each flagged
`StockEntry`, and update the price column. The CSV's `java_line` column
tells you the exact line number to jump to:

```bash
# Quick way to grep one
grep -n "StockEntry(4151" src/com/rs/bot/ambient/BotTradeHandler.java
```

Don't apply changes you disagree with — RSPS economy is your call. The
audit is advisory.

---

## Tuning the run

### Server economy isn't 10% of OSRS

If your server runs higher prices, raise the scale:

```bash
# 25% of GE (server is closer to OSRS pricing)
python3 tools/ge_price_audit.py --scale 0.25
```

### You only want big delta items

```bash
# Only flag where suggested price differs by 50%+ from current
python3 tools/ge_price_audit.py --threshold 0.5
```

### Fast dry run (test the parser)

```bash
# Hit only the first 5 items, ~3 seconds total
python3 tools/ge_price_audit.py --limit 5
```

### Audit alias-only items too

Some chat aliases reference items that aren't in the catalog yet
(`addAlias` only). Add `--include-aliases` to fetch GE prices for those
so you have data when you decide whether to add them as stock entries:

```bash
python3 tools/ge_price_audit.py --include-aliases
```

### Different rate limit (be polite to Jagex)

Default is 600 ms between requests. If you want to be even slower:

```bash
python3 tools/ge_price_audit.py --sleep 1.5
```

### Run from a non-default repo path

```bash
python3 tools/ge_price_audit.py --repo /path/to/matrix-ii-server
```

---

## Background / nohup

If you want to run from SSH and disconnect:

```bash
nohup python3 tools/ge_price_audit.py > data/ge_audit.log 2>&1 &
echo $!   # remember the PID

# tail progress
tail -f data/ge_audit.log

# check when done
ls -la data/ge_audit.csv data/ge_audit.patch.txt
```

Or with `screen`:

```bash
screen -S geaudit
python3 tools/ge_price_audit.py
# Ctrl+A then D to detach. Reattach with:
screen -r geaudit
```

---

## Troubleshooting

**`HTTP 503` / `network error`**
Jagex throttling or maintenance. Bump `--sleep 2.0` and retry. Failed
items show `ge_error` in the CSV — re-run only those by hand.

**`not in GE (untradeable / removed)`**
Item is untradeable on RS3 (e.g. discontinued holiday rares, charged
gear). The audit will note it but won't suggest a price. Decide
manually if it should stay in the bot catalog at all.

**Catalog parser missed an entry**
The regex only catches `new StockEntry(id, price, "name")` and
`new StockEntry(id, price, "name", bundle)`. If you wrote a constructor
in a different style, it'll be skipped. Easiest fix: stick to the
existing format.

**Blocked outbound**
Your VPS may block raw outbound HTTPS. Run the audit from your laptop:

```bash
# on laptop
git clone <repo>
cd matrix-ii-server
python3 tools/ge_price_audit.py
# then scp data/ge_audit.* back to the server if you want
```

---

## What the script DOESN'T do (deliberately)

- It does NOT auto-edit the Java file. RSPS economies routinely diverge
  from OSRS (especially rares and untradeables); always review before
  applying.
- It does NOT cache results. Each run hits the GE API fresh — prices
  drift, so this is a feature.
- It does NOT validate that suggested prices fit Java `int` bounds.
  Anything below the 2.1B max int is fine; if you somehow suggest a
  price above that, edit by hand.
- It does NOT know about your server's existing economy (donor shop
  prices, alch values, etc). Cross-reference manually.

---

## Schedule it (optional)

Run weekly via cron:

```bash
crontab -e
```

Add:

```cron
# Sunday 04:00 - GE price audit (no auto-apply, just generate the diff)
0 4 * * 0 cd /home/user/matrix-ii-server && /usr/bin/python3 tools/ge_price_audit.py >> data/ge_audit.cron.log 2>&1
```

Then check `data/ge_audit.patch.txt` on Mondays before deciding which
prices to bump.
