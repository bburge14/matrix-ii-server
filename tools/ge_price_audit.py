#!/usr/bin/env python3
"""
GE price audit for the Matrix II bot trader catalog.

What it does:
  1. Scrapes item ids + current bot prices from:
       src/com/rs/bot/ambient/BotTradeHandler.java   (StockEntry catalog)
       src/com/rs/bot/ambient/BotChatListener.java   (addAlias DB)
  2. Hits the RuneScape3 GE JSON API for each id (rate-limit friendly).
  3. Writes:
       data/ge_audit.csv          - one row per id with both prices + delta
       data/ge_audit.patch.txt    - human-reviewable list of suggested patches
  4. Optionally emits a generated Java file with updated catalog prices
     (--apply) - DOES NOT touch the original; you copy the diff yourself.

Why standalone (not auto-apply):
  - GE prices can be skewed by squeeze/dump bots; you want eyeballs on it.
  - Server economy may diverge intentionally (e.g. RSPS-tuned cheaper rares).
  - Diff is printable, easy to drop wholesale or pick & choose.

Endpoint:
  https://secure.runescape.com/m=itemdb_rs/api/catalogue/detail.json?item=ID

Rate limit: Jagex doesn't publish one. Conservative sleep between calls
keeps us well under any reasonable threshold (default 600ms).
"""

import argparse
import csv
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

GE_URL = "https://secure.runescape.com/m=itemdb_rs/api/catalogue/detail.json?item={id}"
USER_AGENT = "matrix-ii-price-audit/1.0 (+https://github.com/bburge14/matrix-ii-server)"

# ----- file paths (relative to project root) -----
CATALOG_FILE = "src/com/rs/bot/ambient/BotTradeHandler.java"
ALIAS_FILE = "src/com/rs/bot/ambient/BotChatListener.java"
OUT_DIR = "data"
OUT_CSV = "ge_audit.csv"
OUT_PATCH = "ge_audit.patch.txt"

# ----- regex matchers -----
# new StockEntry(4151, 150_000, "abyssal whip", 1)
# new StockEntry(1511, 50,      "logs",         100)
# new StockEntry(1511, 50,      "logs")
STOCK_ENTRY_RE = re.compile(
    r"""new\s+StockEntry\(\s*
        (?P<id>\d+)\s*,\s*
        (?P<price>[\d_]+)\s*,\s*
        "(?P<name>[^"]+)"
        (?:\s*,\s*(?P<bundle>\d+))?
    \s*\)""",
    re.VERBOSE,
)

# addAlias(4151, "whip", "abyssal whip");
ADD_ALIAS_RE = re.compile(
    r"""addAlias\(\s*
        (?P<id>\d+)\s*,\s*
        (?P<args>"[^;]+)
        \)""",
    re.VERBOSE,
)


def parse_price_int(s: str) -> int:
    """Strip Java _ separators, parse int. '1_500_000' -> 1500000"""
    return int(s.replace("_", ""))


def extract_catalog(path: Path):
    """Yield (item_id, server_price, name, bundle, line_no) for every
    StockEntry in BotTradeHandler.java."""
    text = path.read_text(encoding="utf-8")
    for m in STOCK_ENTRY_RE.finditer(text):
        yield (
            int(m.group("id")),
            parse_price_int(m.group("price")),
            m.group("name"),
            int(m.group("bundle")) if m.group("bundle") else 1,
            text[: m.start()].count("\n") + 1,
        )


def extract_aliases(path: Path):
    """Yield item_ids referenced by addAlias() in BotChatListener.java
    that aren't already in the catalog (we still want to audit
    nicknames-only items so the chat-driven trade prices are sane)."""
    text = path.read_text(encoding="utf-8")
    seen = set()
    for m in ADD_ALIAS_RE.finditer(text):
        iid = int(m.group("id"))
        if iid in seen:
            continue
        seen.add(iid)
        yield iid


def fetch_ge_price(item_id: int, timeout: float = 10.0):
    """Returns (price_int, name) or (None, error_string)."""
    url = GE_URL.format(id=item_id)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            data = json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None, "not in GE (untradeable / removed)"
        return None, f"HTTP {e.code}"
    except urllib.error.URLError as e:
        return None, f"network error: {e}"
    except Exception as e:
        return None, f"parse error: {e}"

    if not data or "item" not in data:
        return None, "no item field"
    item = data["item"]
    cur = item.get("current", {}).get("price")
    name = item.get("name", "?")
    if cur is None:
        return None, "no current price"
    return parse_ge_price_field(cur), name


def parse_ge_price_field(raw):
    """GE returns either an int or a string like '3.6m', '1.5k', '1,234'."""
    if isinstance(raw, int):
        return raw
    s = str(raw).strip().lower().replace(",", "")
    mult = 1
    if s.endswith("k"):
        mult = 1_000
        s = s[:-1]
    elif s.endswith("m"):
        mult = 1_000_000
        s = s[:-1]
    elif s.endswith("b"):
        mult = 1_000_000_000
        s = s[:-1]
    try:
        return int(round(float(s.strip()) * mult))
    except (TypeError, ValueError):
        return None


def fmt_money(n):
    if n is None:
        return "?"
    if n >= 1_000_000_000:
        return f"{n/1_000_000_000:.2f}b"
    if n >= 1_000_000:
        return f"{n/1_000_000:.2f}m"
    if n >= 1_000:
        return f"{n/1_000:.1f}k"
    return str(n)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--repo", default=".", help="project root (default: cwd)")
    ap.add_argument("--sleep", type=float, default=0.6,
                    help="delay between GE requests in seconds (default 0.6)")
    ap.add_argument("--limit", type=int, default=0,
                    help="cap items fetched (0 = all). Useful for dry runs.")
    ap.add_argument("--scale", type=float, default=0.60,
                    help="server-economy scale factor vs RS GE (default 0.60 "
                    "= server prices are ~60%% of live RS GE). Patch "
                    "suggestions are GE_price * scale.")
    ap.add_argument("--threshold", type=float, default=0.30,
                    help="only flag items where |delta| > this fraction of "
                    "the current server price (default 0.30 = 30%%)")
    ap.add_argument("--include-aliases", action="store_true",
                    help="also audit ids that only appear in BotChatListener "
                    "(no catalog entry yet)")
    ap.add_argument("--apply", action="store_true",
                    help="automatically rewrite BotTradeHandler.java with "
                    "the suggested prices for every flagged item. Makes a "
                    "timestamped .bak file before editing.")
    ap.add_argument("--apply-min-delta", type=float, default=0.0,
                    help="when --apply is set, only auto-update items with "
                    "|delta| > this fraction. 0.0 = update every flagged "
                    "row. Use 0.5 to skip small drifts.")
    ap.add_argument("--max-price", type=int, default=2_000_000_000,
                    help="cap suggested price at this value (default 2B - "
                    "Java int max is 2.147B). Items where the GE-derived "
                    "suggested price exceeds this get clamped + flagged "
                    "with 'CAPPED' in the patch and SKIPPED by --apply.")
    ap.add_argument("--max-raw-ge", type=int, default=10_000_000_000,
                    help="if RS3 GE returns a price above this (default 10B), "
                    "treat it as garbage (delisted item / max-cash report) "
                    "and don't suggest a change. Phat raw prices on RS3 GE "
                    "can be 60B+ which is meaningless.")
    ap.add_argument("--push-server", default=None, metavar="URL",
                    help="POST suggested prices to the running server's "
                    "/admin/ge/prices/bulk endpoint so the in-game GE shows "
                    "them too. Example: --push-server http://localhost:8080")
    ap.add_argument("--admin-token", default=None,
                    help="value sent as 'X-Admin-Token' header for "
                    "--push-server. Read from $MATRIX_ADMIN_TOKEN if unset.")
    args = ap.parse_args()

    root = Path(args.repo).resolve()
    catalog_path = root / CATALOG_FILE
    alias_path = root / ALIAS_FILE
    if not catalog_path.is_file():
        print(f"[!] catalog file not found: {catalog_path}", file=sys.stderr)
        sys.exit(1)

    print(f"[*] reading catalog: {catalog_path.relative_to(root)}")
    catalog = list(extract_catalog(catalog_path))
    print(f"    {len(catalog)} StockEntry rows")

    catalog_ids = {row[0] for row in catalog}
    extra_ids = []
    if args.include_aliases and alias_path.is_file():
        for iid in extract_aliases(alias_path):
            if iid not in catalog_ids:
                extra_ids.append(iid)
        print(f"    +{len(extra_ids)} ids from BotChatListener (no catalog entry)")

    work = list(catalog)
    for iid in extra_ids:
        work.append((iid, 0, "(alias only)", 1, 0))

    if args.limit > 0:
        work = work[:args.limit]
        print(f"[*] capped to {args.limit} items for this run")

    out_dir = root / OUT_DIR
    out_dir.mkdir(parents=True, exist_ok=True)
    csv_path = out_dir / OUT_CSV
    patch_path = out_dir / OUT_PATCH

    print(f"[*] hitting GE API ({len(work)} items, ~{args.sleep:.1f}s each, "
          f"~{int(len(work)*args.sleep)}s total)")

    rows = []
    for i, (iid, server_price, name, bundle, line_no) in enumerate(work, 1):
        ge_price, ge_name = fetch_ge_price(iid)
        suggested = None
        delta_pct = None
        flagged = False
        capped = False
        if isinstance(ge_price, int) and ge_price > 0:
            # If RS3 GE reports an absurd price (delisted phats, max cash
            # cap, etc), don't trust it. Skip the suggestion entirely.
            if ge_price > args.max_raw_ge:
                ge_name = (ge_name or "?") + " [GE garbage]"
            else:
                raw_suggested = max(1, int(round(ge_price * args.scale)))
                # Cap at Java int safe ceiling so --apply doesn't write a
                # value that overflows when stored as int in StockEntry.
                if raw_suggested > args.max_price:
                    suggested = args.max_price
                    capped = True
                else:
                    suggested = raw_suggested
                if server_price > 0:
                    delta_pct = (suggested - server_price) / server_price
                    flagged = abs(delta_pct) > args.threshold
                else:
                    # alias-only entry; no current server price to diff against
                    flagged = True
        rows.append({
            "id": iid,
            "server_name": name,
            "ge_name": ge_name if isinstance(ge_price, int) else (ge_name or ""),
            "server_price": server_price,
            "ge_price": ge_price if isinstance(ge_price, int) else "",
            "ge_error": ge_name if not isinstance(ge_price, int) else "",
            "suggested": suggested if suggested is not None else "",
            "suggested_int": suggested,           # numeric, for --apply
            "delta_pct_float": delta_pct,         # numeric, for --apply
            "delta_pct": (f"{delta_pct*100:+.1f}%" if delta_pct is not None else ""),
            "flagged": "Y" if flagged else "",
            "capped": "Y" if capped else "",      # auto-apply will skip these
            "java_line": line_no,
        })
        flag = "!" if flagged else " "
        if capped:
            flag = "C"  # capped marker
        print(f"  [{i:3}/{len(work)}] {flag} id={iid:<6} "
              f"server={fmt_money(server_price):>8}  "
              f"ge={fmt_money(ge_price) if isinstance(ge_price, int) else '-':>8}  "
              f"sug={fmt_money(suggested) if suggested else '-':>8}  "
              f"{name}{' [CAPPED]' if capped else ''}")
        if i < len(work):
            time.sleep(args.sleep)

    # Drop helper numeric fields from the CSV for readability.
    csv_rows = [{k: v for k, v in r.items()
                 if k not in ("suggested_int", "delta_pct_float")}
                for r in rows]
    with csv_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(csv_rows[0].keys()))
        w.writeheader()
        w.writerows(csv_rows)
    print(f"\n[+] wrote {csv_path.relative_to(root)} ({len(csv_rows)} rows)")

    # Patch suggestion file - only flagged rows, in a format you can scan
    flagged_rows = [r for r in rows if r["flagged"] == "Y"]
    with patch_path.open("w", encoding="utf-8") as f:
        f.write("# GE Price Audit - suggested edits\n")
        f.write(f"# Scale factor: {args.scale} (server = ge * scale)\n")
        f.write(f"# Threshold:    {args.threshold*100:.0f}% delta\n")
        f.write(f"# Total flagged: {len(flagged_rows)} of {len(rows)}\n\n")
        f.write("# Apply by editing src/com/rs/bot/ambient/BotTradeHandler.java\n")
        f.write("# Format: id  current -> suggested  (delta)   name\n")
        f.write("# " + "-"*70 + "\n")
        for r in flagged_rows:
            f.write(f"{r['id']:<6}  {fmt_money(r['server_price']):>9} -> "
                    f"{fmt_money(r['suggested']):>9}  ({r['delta_pct']:>7})  "
                    f"{r['server_name']}\n")
    print(f"[+] wrote {patch_path.relative_to(root)} ({len(flagged_rows)} flagged)")

    print(f"\n[*] done. {len(flagged_rows)}/{len(rows)} items flagged "
          f"(>={args.threshold*100:.0f}% delta from suggested).")
    print(f"    review: {patch_path.relative_to(root)}")
    print(f"    full:   {csv_path.relative_to(root)}")

    # ---- --apply: rewrite the catalog file in-place with new prices ----
    if args.apply:
        # Filter rows we'll actually rewrite: must be flagged, have a numeric
        # suggested price, and (if --apply-min-delta set) exceed that threshold.
        applicable = []
        for r in flagged_rows:
            sp = r.get("suggested_int")
            if not isinstance(sp, int) or sp <= 0:
                continue
            if r.get("server_price", 0) == sp:
                continue  # no change
            # Skip rows where the suggestion was capped at --max-price.
            # The audit caps so the patch is review-able, but auto-apply
            # would write a misleading flat-cap value (e.g. every phat at
            # exactly 2B). Better to leave the user's curated price alone.
            if r.get("capped") == "Y":
                continue
            d = r.get("delta_pct_float")
            if args.apply_min_delta > 0 and (d is None or abs(d) < args.apply_min_delta):
                continue
            applicable.append(r)

        if not applicable:
            print("\n[*] --apply: no rows to rewrite (all suggestions skipped)")
            return

        print(f"\n[*] --apply: rewriting {len(applicable)} prices in "
              f"{catalog_path.relative_to(root)}")
        # Backup first.
        ts = time.strftime("%Y%m%d-%H%M%S")
        bak = catalog_path.with_suffix(catalog_path.suffix + f".bak.{ts}")
        bak.write_bytes(catalog_path.read_bytes())
        print(f"    backed up to {bak.relative_to(root)}")

        text = catalog_path.read_text(encoding="utf-8")
        applied = 0
        skipped = []
        for r in applicable:
            iid = r["id"]
            old_price = r["server_price"]
            new_price = r["suggested_int"]
            new_lit = format_int_literal(new_price)
            # Match the StockEntry line for this id with its current price.
            # Anchored to id+old-price so we never bump the wrong row even
            # if two entries share an id (shouldn't happen but be safe).
            row_re = re.compile(
                r"(new\s+StockEntry\(\s*" + str(iid) + r"\s*,\s*)"
                + re.escape(re.sub(r"\B(?=(\d{3})+(?!\d))", "_", str(old_price)).rstrip("_"))
                + r"(\s*,)"
            )
            # The old_price in source might or might not have underscores.
            # Try with and without separators.
            old_lit_plain = str(old_price)
            old_lit_sep = format_int_literal(old_price)
            row_re2 = re.compile(
                r"(new\s+StockEntry\(\s*" + str(iid) + r"\s*,\s*)"
                + r"(?:" + re.escape(old_lit_plain) + r"|" + re.escape(old_lit_sep) + r")"
                + r"(\s*,)"
            )
            new_text, count = row_re2.subn(r"\g<1>" + new_lit + r"\g<2>", text, count=1)
            if count == 0:
                skipped.append((iid, r["server_name"], "no match"))
                continue
            text = new_text
            applied += 1
            print(f"    [{applied:>3}] id={iid:<6} {fmt_money(old_price):>9} "
                  f"-> {fmt_money(new_price):<9}  {r['server_name']}")

        catalog_path.write_text(text, encoding="utf-8")
        print(f"\n[+] applied {applied} price changes to "
              f"{catalog_path.relative_to(root)}")
        if skipped:
            print(f"[!] skipped {len(skipped)} (regex didn't match):")
            for iid, name, why in skipped[:10]:
                print(f"    id={iid:<6} {name}  ({why})")
        print(f"\n  Next:")
        print(f"    javac -encoding ISO-8859-1 -cp 'data/libs/*:src' -d bin "
              f"$(find src -name '*.java')")
        print(f"    # restart server")
        print(f"  Revert (if needed):")
        print(f"    mv {bak.relative_to(root)} {catalog_path.relative_to(root)}")

    # ---- --push-server: send prices to the running game server ----
    if args.push_server:
        push_to_server(args.push_server, args.admin_token, rows)


def push_to_server(base_url: str, admin_token, rows):
    """POST {"prices":{"id":price,...}} to /admin/ge/prices/bulk so the
    in-game GE PRICES map is updated alongside the bot catalog. Skips
    rows without a numeric suggestion or where the GE returned garbage."""
    import os
    if not admin_token:
        admin_token = os.environ.get("MATRIX_ADMIN_TOKEN")
    payload_prices = {}
    for r in rows:
        sp = r.get("suggested_int")
        if not isinstance(sp, int) or sp <= 0:
            continue
        if r.get("capped") == "Y":
            # Don't push the flat-cap value; let the user curate by hand.
            continue
        payload_prices[str(r["id"])] = sp
    if not payload_prices:
        print("\n[*] --push-server: nothing to push (no valid suggestions)")
        return
    body = '{"prices":{' + ",".join(
        f'"{k}":{v}' for k, v in payload_prices.items()) + "}}"
    url = base_url.rstrip("/") + "/admin/ge/prices/bulk"
    print(f"\n[*] --push-server: POSTing {len(payload_prices)} prices to {url}")
    req = urllib.request.Request(url, data=body.encode("utf-8"), method="POST")
    req.add_header("Content-Type", "application/json")
    if admin_token:
        req.add_header("X-Admin-Token", admin_token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            resp = r.read().decode("utf-8")
            print(f"    server response: {resp}")
    except urllib.error.HTTPError as e:
        print(f"[!] HTTP {e.code}: {e.read().decode('utf-8', errors='ignore')}")
    except Exception as e:
        print(f"[!] push failed: {e}")


def format_int_literal(n: int) -> str:
    """Render an int as a Java numeric literal with _ separators every 3
    digits: 1500000 -> '1_500_000'."""
    s = str(n)
    if len(s) <= 3:
        return s
    out = []
    for i, c in enumerate(reversed(s)):
        if i and i % 3 == 0:
            out.append("_")
        out.append(c)
    return "".join(reversed(out))


if __name__ == "__main__":
    main()
