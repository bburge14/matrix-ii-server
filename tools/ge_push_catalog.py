#!/usr/bin/env python3
"""
Push the bot catalog's StockEntry prices straight to the running server's
in-game GE PRICES map.

No RS3 GE fetch. No scaling. No catalog rewrite. Just reads what's already
in BotTradeHandler.java and POSTs it to /admin/ge/prices/bulk.

Use this when:
  - You hand-set prices in the catalog and want them reflected in the GE
  - The audit's --push-server skipped items (e.g. capped rares)
  - You just want the in-game GE to match whatever the bots are selling at

Usage:
    python3 tools/ge_push_catalog.py
    python3 tools/ge_push_catalog.py --server http://localhost:8080
    python3 tools/ge_push_catalog.py --token YOUR_TOKEN
    MATRIX_ADMIN_TOKEN=YOUR_TOKEN python3 tools/ge_push_catalog.py
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

CATALOG_FILE = "src/com/rs/bot/ambient/BotTradeHandler.java"

STOCK_ENTRY_RE = re.compile(
    r"""new\s+StockEntry\(\s*
        (?P<id>\d+)\s*,\s*
        (?P<price>[\d_]+)\s*,\s*
        "(?P<name>[^"]+)"
        (?:\s*,\s*(?P<bundle>\d+))?
    \s*\)""",
    re.VERBOSE,
)

DEFAULT_TOKEN = "2e272a922d80906a4429eb9a59ef80d16dd9cbfb47d40dad"


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--repo", default=".", help="project root (default cwd)")
    ap.add_argument("--server", default="http://localhost:8080",
                    help="admin HTTP endpoint base URL")
    ap.add_argument("--token", default=None,
                    help="admin token (env $MATRIX_ADMIN_TOKEN, then default)")
    ap.add_argument("--dry-run", action="store_true",
                    help="print what would be pushed, don't POST")
    args = ap.parse_args()

    root = Path(args.repo).resolve()
    catalog_path = root / CATALOG_FILE
    if not catalog_path.is_file():
        print(f"[!] not found: {catalog_path}", file=sys.stderr)
        sys.exit(1)

    text = catalog_path.read_text(encoding="utf-8")
    prices = {}
    seen_dupe = []
    for m in STOCK_ENTRY_RE.finditer(text):
        iid = int(m.group("id"))
        price = int(m.group("price").replace("_", ""))
        if iid in prices and prices[iid] != price:
            seen_dupe.append((iid, prices[iid], price))
        prices[iid] = price

    print(f"[*] read {len(prices)} StockEntry rows from "
          f"{catalog_path.relative_to(root)}")
    if seen_dupe:
        print(f"[!] {len(seen_dupe)} item(s) appeared more than once "
              f"with different prices - last value wins:")
        for iid, old, new in seen_dupe[:5]:
            print(f"    id={iid}  {old} != {new}")

    if args.dry_run:
        print("\n[dry-run] would push these prices:")
        for iid in sorted(prices):
            print(f"    {iid:<6} -> {prices[iid]:>15,}")
        return

    token = args.token or os.environ.get("MATRIX_ADMIN_TOKEN") or DEFAULT_TOKEN
    url = args.server.rstrip("/") + "/admin/ge/prices/bulk"
    body = json.dumps({"prices": {str(k): v for k, v in prices.items()}}).encode()

    print(f"[*] POSTing to {url}")
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            resp = r.read().decode()
            print(f"[+] server response: {resp}")
    except urllib.error.HTTPError as e:
        print(f"[!] HTTP {e.code}: {e.read().decode('utf-8', errors='ignore')}",
              file=sys.stderr)
        sys.exit(2)
    except urllib.error.URLError as e:
        print(f"[!] connection error: {e}", file=sys.stderr)
        print("    Is the server running and the admin endpoint reachable?",
              file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
