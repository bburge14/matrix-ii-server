import json
import os
import threading
import time
import tkinter as tk
from datetime import datetime
from pathlib import Path

import customtkinter as ctk
import requests
from tkinter import messagebox, simpledialog, ttk

# ----- Config -----

APP_NAME = "matrix-admin"

def config_path():
    base = os.environ.get("APPDATA") or str(Path.home())
    d = Path(base) / APP_NAME
    d.mkdir(parents=True, exist_ok=True)
    return d / "config.json"

def load_config():
    p = config_path()
    if not p.exists():
        return {"host": "107.202.173.6", "port": 8090, "token": ""}
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        return {"host": "107.202.173.6", "port": 8090, "token": ""}

def save_config(cfg):
    config_path().write_text(json.dumps(cfg, indent=2), encoding="utf-8")


# ----- API client -----

class MatrixAPI:
    def __init__(self, cfg):
        self.cfg = cfg
        self._lock = threading.Lock()

    @property
    def base(self): return f"http://{self.cfg['host']}:{self.cfg['port']}"

    @property
    def headers(self):
        return {"Authorization": f"Bearer {self.cfg.get('token','')}",
                "Content-Type": "application/json"}

    def update(self, host, port, token):
        with self._lock:
            self.cfg["host"] = host
            self.cfg["port"] = int(port)
            self.cfg["token"] = token
            save_config(self.cfg)

    def _get(self, path, timeout=5):
        r = requests.get(f"{self.base}{path}", headers=self.headers, timeout=timeout)
        r.raise_for_status()
        return r.json()

    def _post(self, path, body=None, timeout=10):
        r = requests.post(f"{self.base}{path}", headers=self.headers,
                          data=json.dumps(body or {}), timeout=timeout)
        if not r.ok:
            # Surface the server's JSON error message instead of letting
            # raise_for_status drop a generic "400 Client Error" / None.
            try:
                msg = r.json().get("error") or r.text
            except Exception:
                msg = r.text or f"HTTP {r.status_code}"
            raise RuntimeError(f"HTTP {r.status_code}: {msg}")
        return r.json()

    def ping(self):       return self._get("/admin/ping", timeout=3)
    def stats(self):      return self._get("/admin/stats", timeout=3)
    def bots(self):       return self._get("/admin/bots")
    def bot_status(self): return self._get("/admin/bots/status")
    def players(self):    return self._get("/admin/players")
    def snapshots(self):  return self._get("/admin/snapshots")
    def log_tail(self, n=200): return self._get(f"/admin/log/tail?lines={n}")
    def bot_inspect(self, name):    return self._get(f"/admin/bots/inspect?name={requests.utils.quote(name)}")
    def bot_diagnose(self, name):   return self._get(f"/admin/bots/diagnose?name={requests.utils.quote(name)}")
    def bot_scan(self, name):       return self._get(f"/admin/bots/scan?name={requests.utils.quote(name)}")
    def bot_force(self, name, skill):
        return self._post("/admin/bots/force", {"name": name, "skill": skill})
    def player_inspect(self, name): return self._get(f"/admin/players/inspect?name={requests.utils.quote(name)}")

    def bots_generate(self, count, mode="default", level=3, archetype="random"):
        return self._post("/admin/bots/generate",
                          {"count": count, "mode": mode, "level": level, "archetype": archetype})
    def bots_spawn(self, name=None, count=None):
        body = {}
        if name: body["name"] = name
        if count: body["count"] = count
        return self._post("/admin/bots/spawn", body)
    def bots_despawn(self, name=None):
        body = {"name": name} if name else {}
        return self._post("/admin/bots/despawn", body)
    def bots_delete(self, name):    return self._post("/admin/bots/delete", {"name": name})

    def player_heal(self, name):    return self._post("/admin/players/heal", {"name": name})
    def player_teleport(self, name, x, y, plane):
        return self._post("/admin/players/teleport", {"name": name, "x": x, "y": y, "plane": plane})
    def player_give(self, name, item_id, amount):
        return self._post("/admin/players/give", {"name": name, "itemId": item_id, "amount": amount})
    def player_rights(self, name, level):
        return self._post("/admin/players/rights", {"name": name, "level": level})
    def player_flag(self, name, flag, value):
        return self._post("/admin/players/flags", {"name": name, flag: "true" if value else "false"})
    def player_kick(self, name):    return self._post("/admin/players/kick", {"name": name})
    def player_mute(self, name, muted):
        return self._post("/admin/players/mute", {"name": name, "muted": "true" if muted else "false"})
    def player_maxstats(self, name):  return self._post("/admin/players/maxstats", {"name": name})
    def player_resetstats(self, name): return self._post("/admin/players/resetstats", {"name": name})
    def player_setstat(self, name, skill, level):
        return self._post("/admin/players/setstat", {"name": name, "skill": skill, "level": level})

    def server_save(self):           return self._post("/admin/server/save")
    def server_restart(self, delay): return self._post("/admin/server/restart", {"delay": delay})
    def server_broadcast(self, msg): return self._post("/admin/server/broadcast", {"message": msg})
    def server_reload(self):         return self._post("/admin/server/reload")
    def snapshot_take(self):         return self._post("/admin/snapshots/take")

    # Citizens (AmbientBot/FSM lightweight bots)
    def citizens(self):              return self._get("/admin/citizens")
    def citizens_spawn(self, count, category=None, x=3222, y=3218, plane=0, scatter=12):
        body = {"count": count, "x": x, "y": y, "plane": plane, "scatter": scatter}
        if category and category != "mixed":
            body["category"] = category
        return self._post("/admin/citizens/spawn", body)
    def citizens_clear(self):        return self._post("/admin/citizens/clear")
    def citizens_archetypes(self):   return self._get("/admin/citizens/archetypes")
    def citizens_budget_get(self):   return self._get("/admin/citizens/budget")
    def citizens_budget_set(self, slots):
        return self._post("/admin/citizens/budget", {"slots": slots})
    def citizens_budget_apply(self, include_manual=True):
        return self._post("/admin/citizens/budget/apply",
                          {"includeManual": "true" if include_manual else "false"})
    def citizens_budget_reseed(self):
        return self._post("/admin/citizens/budget/reseed")

    # Phantom GE
    def phantom_ge_get(self):    return self._get("/admin/phantom-ge")
    def phantom_ge_set(self, updates):
        return self._post("/admin/phantom-ge", updates)

    # Gear sets
    def gear_sets_get(self):         return self._get("/admin/gear/sets")
    def gear_sets_save(self, key, outfits):
        # outfits = [{"name":..,"hat":..,"chest":..,"legs":..}, ...]
        return self._post("/admin/gear/sets", {"key": key, "outfits": outfits})

    # GE prices
    def ge_prices_catalog(self):     return self._get("/admin/ge/prices?catalog=1")
    def ge_prices_for(self, ids):
        if not ids: return {"prices": {}, "names": {}}
        return self._get("/admin/ge/prices?ids=" + ",".join(str(i) for i in ids))
    def ge_prices_bulk_set(self, prices):
        # prices is {int_id: int_price}; server expects {"id_str": int_price}
        return self._post("/admin/ge/prices/bulk",
                          {"prices": {str(k): int(v) for k, v in prices.items()}})

    # All-items browser
    def items_all(self, q=None, slot=None, wearable=False, tradeable=False,
                  override=False, page=0, page_size=200):
        from urllib.parse import quote
        parts = [f"page={page}", f"pageSize={page_size}"]
        if q:        parts.append("q=" + quote(q))
        if slot is not None: parts.append(f"slot={int(slot)}")
        if wearable: parts.append("wearable=1")
        if tradeable: parts.append("tradeable=1")
        if override: parts.append("override=1")
        return self._get("/admin/items/all?" + "&".join(parts))

    def items_set_tradeable(self, ids, tradeable):
        body = {"ids": [int(i) for i in ids], "tradeable": bool(tradeable)}
        return self._post("/admin/items/tradeable", body)

    def items_clear_override(self, ids):
        body = {"ids": [int(i) for i in ids], "clear": True}
        return self._post("/admin/items/tradeable", body)

    # Dye recolor framework
    def dyes_scan(self):
        return self._get("/admin/items/dyes/scan")
    def dyes_autopopulate(self):
        return self._post("/admin/items/dyes/autopopulate")
    def dyes_reload(self):
        return self._post("/admin/items/dyes/reload")

    # Old/retro look scan (every item with an alternate "old" model
    # variant in the cache; the items-look toggle target list).
    def items_oldlook_scan(self, limit=500):
        return self._get(f"/admin/items/oldlook-scan?limit={int(limit)}")
    def retro_autopopulate(self):
        return self._post("/admin/items/retro/autopopulate")
    def retro_reload(self):
        return self._post("/admin/items/retro/reload")

    # World tick profiler
    def profiler_start(self):        return self._post("/admin/profiler/start")
    def profiler_stop(self):         return self._post("/admin/profiler/stop")
    def profiler_dump(self):         return self._post("/admin/profiler/dump")

    # Cache status (PRIMARY/LEGACY/DLC)
    def cache_status(self):          return self._get("/admin/cache/status")


# ----- UI helpers -----

def fmt_uptime(ms):
    s = ms // 1000
    h, s = divmod(s, 3600)
    m, s = divmod(s, 60)
    return f"{h}h {m}m {s}s"

def fmt_size(n):
    for unit in ("B","KB","MB","GB"):
        if n < 1024: return f"{n:.1f} {unit}"
        n /= 1024
    return f"{n:.1f} TB"

def fmt_time(ms):
    return datetime.fromtimestamp(ms/1000).strftime("%Y-%m-%d %H:%M:%S")

def rights_label(level):
    return {0: "Player", 1: "Mod", 2: "Admin"}.get(level, f"Lv{level}")

SKILLS = [
    "Attack", "Defence", "Strength", "Constitution", "Ranged", "Prayer", "Magic",
    "Cooking", "Woodcutting", "Fletching", "Fishing", "Firemaking",
    "Crafting", "Smithing", "Mining", "Herblore", "Agility", "Thieving",
    "Slayer", "Farming", "Runecrafting", "Hunter", "Construction", "Summoning",
    "Dungeoneering",
]


# ----- Main App -----

ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("blue")

class App(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.title("Matrix Admin Panel")
        self.geometry("1200x720")
        self.cfg = load_config()
        self.api = MatrixAPI(self.cfg)

        self.grid_columnconfigure(1, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.sidebar = ctk.CTkFrame(self, width=180, corner_radius=0)
        self.sidebar.grid(row=0, column=0, sticky="nsew")
        self.sidebar.grid_propagate(False)
        ctk.CTkLabel(self.sidebar, text="Matrix Admin",
                     font=ctk.CTkFont(size=18, weight="bold")).pack(pady=(20, 2))
        # Build marker so you can confirm you're running the latest panel.
        # If "GE Prices" tab isn't visible below, you're on an old build -
        # `git pull` on this machine and re-run admin_panel.py.
        ctk.CTkLabel(self.sidebar, text="build 2026-05",
                     font=ctk.CTkFont(size=9), text_color="#666").pack(pady=(0, 14))

        self.tabs = {}
        for name in ("Dashboard", "Bots", "Bot AI", "Citizens", "Gear Sets", "GE Prices", "Items", "Phantom GE", "Players", "Server", "Backups", "Log", "Settings"):
            btn = ctk.CTkButton(self.sidebar, text=name, height=36,
                                command=lambda n=name: self.show(n))
            btn.pack(fill="x", padx=10, pady=4)
            self.tabs[name] = {"button": btn, "frame": None}

        self.status_bar = ctk.CTkLabel(self.sidebar, text="Connecting...",
                                       font=ctk.CTkFont(size=11))
        self.status_bar.pack(side="bottom", pady=10)

        self.content = ctk.CTkFrame(self, corner_radius=0)
        self.content.grid(row=0, column=1, sticky="nsew")
        self.content.grid_columnconfigure(0, weight=1)
        self.content.grid_rowconfigure(0, weight=1)

        self.tabs["Dashboard"]["frame"] = DashboardFrame(self.content, self.api)
        self.tabs["Bots"]["frame"]      = BotsFrame(self.content, self.api)
        self.tabs["Bot AI"]["frame"]    = BotAIFrame(self.content, self.api)
        self.tabs["Citizens"]["frame"]  = CitizensFrame(self.content, self.api)
        self.tabs["Gear Sets"]["frame"] = GearSetsFrame(self.content, self.api)
        self.tabs["GE Prices"]["frame"] = GePricesFrame(self.content, self.api)
        self.tabs["Items"]["frame"]     = ItemsFrame(self.content, self.api)
        self.tabs["Phantom GE"]["frame"] = PhantomGEFrame(self.content, self.api)
        self.tabs["Players"]["frame"]   = PlayersFrame(self.content, self.api)
        self.tabs["Server"]["frame"]    = ServerFrame(self.content, self.api)
        self.tabs["Backups"]["frame"]   = BackupsFrame(self.content, self.api)
        self.tabs["Log"]["frame"]       = LogFrame(self.content, self.api)
        self.tabs["Settings"]["frame"]  = SettingsFrame(self.content, self.api, self.on_config_changed)

        self.after(500, self.poll_connection)

        if not self.cfg.get("token"):
            self.show("Settings")
        else:
            self.show("Dashboard")

    def show(self, name):
        for n, t in self.tabs.items():
            if t["frame"]: t["frame"].grid_forget()
        f = self.tabs[name]["frame"]
        f.grid(row=0, column=0, sticky="nsew")
        if hasattr(f, "on_show"): f.on_show()

    def poll_connection(self):
        def check():
            try: self.api.ping(); ok = True
            except Exception: ok = False
            self.after(0, lambda: self.status_bar.configure(
                text=("● Connected" if ok else "● Disconnected"),
                text_color=("#3fbf3f" if ok else "#cc3030")))
        threading.Thread(target=check, daemon=True).start()
        self.after(5000, self.poll_connection)

    def on_config_changed(self):
        self.cfg = load_config()
        self.api.cfg = self.cfg


# ----- Dashboard -----

class DashboardFrame(ctk.CTkFrame):
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        ctk.CTkLabel(self, text="Dashboard", font=ctk.CTkFont(size=22, weight="bold")).pack(anchor="w", padx=20, pady=(20, 10))

        self.stat_grid = ctk.CTkFrame(self)
        self.stat_grid.pack(fill="x", padx=20, pady=10)
        self.cards = {}
        for i, key in enumerate(("Players Online", "Bots Online", "Bots Offline", "Memory Used", "Memory Max", "Uptime")):
            card = ctk.CTkFrame(self.stat_grid)
            card.grid(row=i//3, column=i%3, padx=8, pady=8, sticky="nsew")
            self.stat_grid.grid_columnconfigure(i%3, weight=1)
            ctk.CTkLabel(card, text=key, font=ctk.CTkFont(size=12)).pack(pady=(10, 0))
            val = ctk.CTkLabel(card, text="—", font=ctk.CTkFont(size=24, weight="bold"))
            val.pack(pady=(0, 10))
            self.cards[key] = val
        ctk.CTkButton(self, text="Refresh", command=self.refresh).pack(pady=10)
        self._poll_id = None

    def on_show(self):
        self.refresh()
        if self._poll_id is None: self._auto_refresh()

    def _auto_refresh(self):
        self.refresh()
        self._poll_id = self.after(5000, self._auto_refresh)

    def refresh(self):
        def do():
            try: 
                s = self.api.stats()
                self.after(0, lambda stats=s: self._update(stats))
            except Exception as e: 
                error_msg = str(e)
                self.after(0, lambda: self._update_err(error_msg))
        threading.Thread(target=do, daemon=True).start()

    def _update(self, s):
        self.cards["Players Online"].configure(text=str(s.get("players_online", 0)))
        self.cards["Bots Online"].configure(text=str(s.get("bots_online", 0)))
        self.cards["Bots Offline"].configure(text=str(s.get("bots_offline", 0)))
        self.cards["Memory Used"].configure(text=f"{s.get('mem_used_mb',0)} MB")
        self.cards["Memory Max"].configure(text=f"{s.get('mem_max_mb',0)} MB")
        self.cards["Uptime"].configure(text=fmt_uptime(s.get("uptime_ms", 0)))

    def _update_err(self, error_msg):
        for v in self.cards.values(): v.configure(text="?")


# ----- Bot AI Monitor -----

class BotAIFrame(ctk.CTkFrame):
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        
        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 5))
        ctk.CTkLabel(top, text="Bot AI Monitor", font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        self.count_label = ctk.CTkLabel(top, text="—", font=ctk.CTkFont(size=12))
        self.count_label.pack(side="left", padx=20)
        
        actions = ctk.CTkFrame(self)
        actions.pack(fill="x", padx=20, pady=4)
        ctk.CTkButton(actions, text="Refresh", width=80, command=self.refresh).pack(side="left", padx=4)
        self.auto_var = tk.BooleanVar(value=True)
        ctk.CTkCheckBox(actions, text="Auto-refresh (3s)", variable=self.auto_var, command=self._toggle_auto).pack(side="left", padx=10)

        # Filter bar
        filt = ctk.CTkFrame(self, fg_color="transparent")
        filt.pack(fill="x", padx=20, pady=(0, 4))
        ctk.CTkLabel(filt, text="Filter:").pack(side="left", padx=(4, 6))
        self.filter_var = tk.StringVar(value="")
        ctk.CTkEntry(filt, textvariable=self.filter_var, width=200,
                     placeholder_text="name / area / goal / diag substring").pack(side="left")
        self.filter_var.trace_add("write", lambda *a: self._update(self.bot_data))
        self.problems_only_var = tk.BooleanVar(value=False)
        ctk.CTkCheckBox(filt, text="Problems only (has [Debug])",
                        variable=self.problems_only_var,
                        command=lambda: self._update(self.bot_data)).pack(side="left", padx=10)

        tree_frame = ctk.CTkFrame(self)
        tree_frame.pack(fill="both", expand=True, padx=20, pady=10)

        cols = ("name", "cb", "area", "coords", "state", "goal", "method", "diag", "hp", "inv", "working")
        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings", selectmode="browse")
        widths = (140, 40, 120, 110, 90, 200, 200, 320, 60, 50, 60)
        for c, w in zip(cols, widths):
            self.tree.heading(c, text=c.upper(), anchor="w")
            self.tree.column(c, width=w, anchor="w", stretch=False)

        # Both scrollbars - need a wrapper grid so vertical+horizontal coexist.
        ysb = ttk.Scrollbar(tree_frame, orient="vertical", command=self.tree.yview)
        xsb = ttk.Scrollbar(tree_frame, orient="horizontal", command=self.tree.xview)
        self.tree.configure(yscrollcommand=ysb.set, xscrollcommand=xsb.set)
        self.tree.grid(row=0, column=0, sticky="nsew")
        ysb.grid(row=0, column=1, sticky="ns")
        xsb.grid(row=1, column=0, sticky="ew")
        tree_frame.grid_rowconfigure(0, weight=1)
        tree_frame.grid_columnconfigure(0, weight=1)

        # Click-to-detail row
        self.tree.bind("<<TreeviewSelect>>", self._on_select)
        self.detail = ctk.CTkLabel(self, text="(select a bot to see full diagnostic)",
                                   anchor="w", justify="left", wraplength=900)
        self.detail.pack(fill="x", padx=20, pady=(0, 10))

        self._poll_id = None
        self.bot_data = []

    def on_show(self):
        self.refresh()
        if self.auto_var.get() and self._poll_id is None:
            self._auto_refresh()

    def _toggle_auto(self):
        if self.auto_var.get() and self._poll_id is None:
            self._auto_refresh()

    def _auto_refresh(self):
        if not self.auto_var.get():
            self._poll_id = None
            return
        self.refresh()
        self._poll_id = self.after(3000, self._auto_refresh)

    def refresh(self):
        def do():
            try:
                r = self.api.bot_status()
                bots = r.get("bots", [])
                self.bot_data = bots
                self.after(0, lambda: self._update(bots))
                self.after(0, lambda: self.count_label.configure(text=f"{len(bots)} bots monitored"))
            except Exception as e:
                error_msg = str(e)
                self.after(0, lambda: self._update_error(error_msg))
        threading.Thread(target=do, daemon=True).start()

    def _matches_filter(self, bot):
        q = (self.filter_var.get() or "").strip().lower()
        if q:
            haystack = (
                str(bot.get("name", "")) + " " +
                str(bot.get("area", "")) + " " +
                str(bot.get("goal", "")) + " " +
                str(bot.get("method", "")) + " " +
                str(bot.get("diag", ""))
            ).lower()
            if q not in haystack:
                return False
        if self.problems_only_var.get():
            d = str(bot.get("diag", "")).lower()
            if not d:
                return False
            if not any(k in d for k in ("stuck", "no ", "broke", "fail", "level", "lvl ", "missing")):
                return False
        return True

    def _update(self, bots):
        self.tree.delete(*self.tree.get_children())
        for bot in bots:
            if not self._matches_filter(bot):
                continue
            x = bot.get("x", 0)
            y = bot.get("y", 0)
            plane = bot.get("plane", 0)
            coords = f"{x},{y},{plane}"
            hp = f"{bot.get('hp', 0)}/{bot.get('max_hp', 0)}"
            inv = bot.get("free_inv", "?")
            working = "yes" if bot.get("working") else "no"
            self.tree.insert("", "end", values=(
                bot.get("name", ""),
                bot.get("cb", "?"),
                bot.get("area", ""),
                coords,
                bot.get("state", ""),
                bot.get("goal", ""),
                bot.get("method", ""),
                bot.get("diag", ""),
                hp,
                inv,
                working,
            ))

    def _on_select(self, _evt=None):
        sel = self.tree.selection()
        if not sel:
            return
        idx = self.tree.index(sel[0])
        # idx counts only filtered bots; pick from the filtered subset
        filtered = [b for b in self.bot_data if self._matches_filter(b)]
        if idx >= len(filtered):
            return
        b = filtered[idx]
        text = (
            f"{b.get('name','?')}  cb {b.get('cb','?')}  ({b.get('archetype','')})\n"
            f"  area    : {b.get('area','')}    coords {b.get('x',0)},{b.get('y',0)},{b.get('plane',0)}\n"
            f"  state   : {b.get('state','')}\n"
            f"  goal    : {b.get('goal','')}\n"
            f"  method  : {b.get('method','')}  ({b.get('method_kind','')})\n"
            f"  diag    : {b.get('diag','')}\n"
            f"  hp      : {b.get('hp',0)}/{b.get('max_hp',0)}    inv free {b.get('free_inv','?')}    working {b.get('working')}\n"
            f"  totalLvl: {b.get('total_lvl','?')}    locked {b.get('locked')}"
        )
        self.detail.configure(text=text)

    def _update_error(self, error_msg):
        self.tree.delete(*self.tree.get_children())
        self.count_label.configure(text=f"Error: {error_msg}")


# ----- Bots tab -----

class BotsFrame(ctk.CTkFrame):
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api

        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 5))
        ctk.CTkLabel(top, text="Bots", font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        self.count_label = ctk.CTkLabel(top, text="—", font=ctk.CTkFont(size=12))
        self.count_label.pack(side="left", padx=20)

        actions1 = ctk.CTkFrame(self)
        actions1.pack(fill="x", padx=20, pady=4)
        ctk.CTkButton(actions1, text="Refresh", width=80, command=self.refresh).pack(side="left", padx=4)
        ctk.CTkLabel(actions1, text="Count:").pack(side="left", padx=(20, 4))
        self.count_var = tk.StringVar(value="10")
        ctk.CTkEntry(actions1, textvariable=self.count_var, width=70).pack(side="left", padx=4)
        ctk.CTkButton(actions1, text="Generate New...", width=140, command=self._open_generate_dialog).pack(side="left", padx=4)
        ctk.CTkButton(actions1, text="Spawn N from Pool", width=140, command=self._spawn_n).pack(side="left", padx=4)
        ctk.CTkButton(actions1, text="Spawn All Offline", width=140, fg_color="#1b6e3a", command=self._spawn_all).pack(side="left", padx=4)

        actions2 = ctk.CTkFrame(self)
        actions2.pack(fill="x", padx=20, pady=4)
        self.selection_label = ctk.CTkLabel(actions2, text="0 selected", font=ctk.CTkFont(size=12, weight="bold"))
        self.selection_label.pack(side="left", padx=4)
        ctk.CTkButton(actions2, text="Spawn Selected", width=130, fg_color="#1b6e3a", command=lambda: self._bulk_action("spawn")).pack(side="left", padx=4)
        ctk.CTkButton(actions2, text="Despawn Selected", width=140, command=lambda: self._bulk_action("despawn")).pack(side="left", padx=4)
        ctk.CTkButton(actions2, text="Delete Selected", width=130, fg_color="#aa3030", command=lambda: self._bulk_action("delete")).pack(side="left", padx=4)
        ctk.CTkLabel(actions2, text=" │ ").pack(side="left", padx=2)
        ctk.CTkButton(actions2, text="Despawn All", width=110, command=self._despawn_all).pack(side="left", padx=4)
        ctk.CTkButton(actions2, text="Delete All Offline", width=140, fg_color="#aa3030", command=self._delete_all_offline).pack(side="left", padx=4)

        search_frame = ctk.CTkFrame(self, fg_color="transparent")
        search_frame.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(search_frame, text="Filter:").pack(side="left", padx=4)
        self.search_var = tk.StringVar()
        self.search_var.trace_add("write", lambda *a: self._apply_filter())
        ctk.CTkEntry(search_frame, textvariable=self.search_var, width=200, placeholder_text="filter by name...").pack(side="left", padx=4)
        self.online_filter = tk.StringVar(value="all")
        for label, val in (("All", "all"), ("Online only", "online"), ("Offline only", "offline")):
            ctk.CTkRadioButton(search_frame, text=label, variable=self.online_filter, value=val, command=self._apply_filter).pack(side="left", padx=8)

        # Archetype filter dropdown - populated dynamically from the
        # bots that come back from the API so it covers whatever is
        # actually in the pool (skiller, melee, ranged, mage, ...).
        ctk.CTkLabel(search_frame, text="Archetype:").pack(side="left", padx=(20, 4))
        self.archetype_filter = tk.StringVar(value="all")
        self.archetype_menu = ctk.CTkOptionMenu(search_frame, values=["all"],
                                                variable=self.archetype_filter,
                                                command=lambda _: self._apply_filter(),
                                                width=120)
        self.archetype_menu.pack(side="left", padx=4)

        tree_frame = ctk.CTkFrame(self)
        tree_frame.pack(fill="both", expand=True, padx=20, pady=10)

        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Treeview", background="#2b2b2b", foreground="white", fieldbackground="#2b2b2b", borderwidth=0, rowheight=24)
        style.configure("Treeview.Heading", background="#1f6aa5", foreground="white", borderwidth=0)
        style.map("Treeview", background=[("selected", "#1f6aa5")])

        cols = ("name", "online", "archetype", "combat", "total", "x", "y", "plane")
        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings", selectmode="extended")
        for c, w in zip(cols, (200, 70, 90, 70, 70, 70, 70, 50)):
            self.tree.heading(c, text=c.title(), anchor="w", command=lambda col=c: self._sort_by(col))
            self.tree.column(c, width=w, anchor="w")
        self.tree.pack(side="left", fill="both", expand=True)
        self.tree.bind("<<TreeviewSelect>>", lambda e: self._update_sel_count())
        self.tree.bind("<Double-1>", lambda e: self._view_stats())

        sb = ttk.Scrollbar(tree_frame, orient="vertical", command=self.tree.yview)
        sb.pack(side="right", fill="y")
        self.tree.configure(yscrollcommand=sb.set)

        self.menu = tk.Menu(self, tearoff=0)
        self.menu.add_command(label="View Stats", command=self._view_stats)
        self.menu.add_separator()
        self.menu.add_command(label="Spawn",   command=lambda: self._row_action("spawn"))
        self.menu.add_command(label="Despawn", command=lambda: self._row_action("despawn"))
        self.menu.add_command(label="Respawn (despawn + spawn)", command=lambda: self._row_action("respawn"))
        self.menu.add_separator()
        self.menu.add_command(label="Delete (PERMANENT)", command=lambda: self._row_action("delete"))
        self.tree.bind("<Button-3>", self._on_right_click)

        self.all_bots = []
        self.sort_col = "name"
        self.sort_rev = False

    def on_show(self): self.refresh()

    def _selected_names(self):
        return [self.tree.item(s, "values")[0] for s in self.tree.selection()]

    def _update_sel_count(self):
        n = len(self.tree.selection())
        self.selection_label.configure(text=f"{n} selected")

    def refresh(self):
        def do():
            try:
                r = self.api.bots()
                self.all_bots = r.get("bots", [])
                self.after(0, self._apply_filter)
                online_count = sum(1 for b in self.all_bots if b.get('online'))
                total_count = len(self.all_bots)
                self.after(0, lambda: self.count_label.configure(text=f"{online_count} online / {total_count} total"))
            except Exception as e:
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()

    def _apply_filter(self):
        q = self.search_var.get().lower()
        f = self.online_filter.get()
        a = self.archetype_filter.get() if hasattr(self, "archetype_filter") else "all"
        # Refresh archetype dropdown with whatever archetypes appeared in
        # the latest bot fetch. Keep "all" at the top.
        self._refresh_archetype_menu()
        rows = []
        for b in self.all_bots:
            if q and q not in b.get("name", "").lower(): continue
            if f == "online" and not b.get("online"): continue
            if f == "offline" and b.get("online"): continue
            if a != "all" and (b.get("archetype", "") or "") != a: continue
            rows.append(b)
        rows.sort(key=lambda r: str(r.get(self.sort_col, "")), reverse=self.sort_rev)
        self.tree.delete(*self.tree.get_children())
        for b in rows:
            arch = b.get("archetype", "") or ""
            self.tree.insert("", "end", values=(
                b.get("name",""),
                "Online" if b.get("online") else "Offline",
                arch if b.get("online") else "-",
                b.get("combat","-") if b.get("online") else "-",
                b.get("total","-") if b.get("online") else "-",
                b.get("x",""), b.get("y",""), b.get("plane","")))
        self._update_sel_count()

    def _sort_by(self, col):
        if self.sort_col == col: self.sort_rev = not self.sort_rev
        else: self.sort_col = col; self.sort_rev = False
        self._apply_filter()

    def _refresh_archetype_menu(self):
        """Rebuild the archetype dropdown from the current bot list."""
        if not hasattr(self, "archetype_menu"): return
        seen = set()
        for b in self.all_bots:
            arch = (b.get("archetype") or "").strip()
            if arch: seen.add(arch)
        values = ["all"] + sorted(seen)
        try:
            self.archetype_menu.configure(values=values)
        except Exception:
            pass
        # Reset to "all" if the previously-selected archetype no longer
        # exists in the pool.
        if self.archetype_filter.get() not in values:
            self.archetype_filter.set("all")

    def _read_count(self, default=10):
        try:
            n = int(self.count_var.get())
            return max(1, min(500, n))
        except ValueError: return default

    def _open_generate_dialog(self):
        dlg = GenerateDialog(self, self._read_count())
        self.wait_window(dlg)
        if dlg.result:
            count, mode, level, archetype = dlg.result
            if not messagebox.askyesno("Generate bots",
                                       f"Generate {count} bots\nMode: {mode}\nArchetype: {archetype}\nLevel: {level}"):
                return
            threading.Thread(target=lambda: self._call_then_refresh(
                lambda: self.api.bots_generate(count, mode, level, archetype)), daemon=True).start()

    def _spawn_n(self):
        n = self._read_count()
        threading.Thread(target=lambda: self._call_then_refresh(lambda: self.api.bots_spawn(count=n)), daemon=True).start()

    def _spawn_all(self):
        offline = [b for b in self.all_bots if not b.get("online")]
        if not offline: return messagebox.showinfo("Spawn all", "No offline bots to spawn.")
        if not messagebox.askyesno("Spawn all offline", f"Spawn ALL {len(offline)} offline bots?"): return
        threading.Thread(target=lambda: self._call_then_refresh(lambda: self.api.bots_spawn(count=len(offline))), daemon=True).start()

    def _despawn_all(self):
        online = [b for b in self.all_bots if b.get("online")]
        if not online: return messagebox.showinfo("Despawn all", "No online bots.")
        if not messagebox.askyesno("Despawn all", f"Despawn all {len(online)} online bots?"): return
        threading.Thread(target=lambda: self._call_then_refresh(lambda: self.api.bots_despawn()), daemon=True).start()

    def _delete_all_offline(self):
        offline = [b["name"] for b in self.all_bots if not b.get("online")]
        if not offline: return messagebox.showinfo("Delete offline", "No offline bots.")
        if not messagebox.askyesno("Delete ALL offline", f"PERMANENTLY DELETE all {len(offline)} offline bots?"): return
        if not messagebox.askyesno("Confirm again", f"Really delete {len(offline)} bots? Final."): return
        # Use the unified bulk runner with the delete API call.
        delete_fn = self.BULK_ACTIONS["delete"][2]
        threading.Thread(target=lambda: self._bulk_run("Delete", offline, delete_fn), daemon=True).start()

    # ===== Bulk actions =====
    # Three actions (spawn / despawn / delete) used to have identical
    # loop-and-count code. Collapsed into _bulk_run which takes a label,
    # the names, and a per-name callable that returns the API response.
    BULK_ACTIONS = {
        "spawn":   ("Spawn",   None,
                    lambda api, n: api.bots_spawn(name=n)),
        "despawn": ("Despawn", None,
                    lambda api, n: api.bots_despawn(name=n)),
        "delete":  ("Delete",  "PERMANENTLY DELETE {n} bot(s)?",
                    lambda api, n: api.bots_delete(n)),
        "respawn": ("Respawn", None,
                    None),  # special - handled inline (despawn then spawn)
    }

    def _bulk_action(self, action):
        names = self._selected_names()
        if not names:
            return messagebox.showinfo("No selection", "Select one or more bots first.")
        meta = self.BULK_ACTIONS.get(action)
        if meta is None: return
        label, confirm_template, fn = meta
        if confirm_template:
            if not messagebox.askyesno(label + " selected",
                                       confirm_template.format(n=len(names))):
                return
        if action == "respawn":
            threading.Thread(target=lambda: self._bulk_respawn(names), daemon=True).start()
        else:
            threading.Thread(target=lambda: self._bulk_run(label, names, fn), daemon=True).start()

    def _bulk_run(self, label, names, fn):
        ok = fail = 0
        for n in names:
            try:
                resp = fn(self.api, n)
                if resp and resp.get("ok"):
                    ok += 1
                else:
                    fail += 1
            except Exception:
                fail += 1
        self.after(0, lambda: self._show_bulk(label, ok, fail))
        self.after(0, self.refresh)

    def _bulk_respawn(self, names):
        ok = fail = 0
        for n in names:
            try:
                self.api.bots_despawn(name=n)
                resp = self.api.bots_spawn(name=n)
                if resp and resp.get("ok"):
                    ok += 1
                else:
                    fail += 1
            except Exception:
                fail += 1
        self.after(0, lambda: self._show_bulk("Respawn", ok, fail))
        self.after(0, self.refresh)

    def _show_bulk(self, label, ok, fail):
        if fail: messagebox.showwarning(label, f"{label}: {ok} succeeded, {fail} failed.")
        else: messagebox.showinfo(label, f"{label}: {ok} succeeded.")

    def _call_then_refresh(self, fn):
        try:
            fn()
            self.after(0, self.refresh)
        except Exception as e:
            error_msg = str(e)
            self.after(0, lambda: messagebox.showerror("Error", error_msg))

    def _on_right_click(self, e):
        row = self.tree.identify_row(e.y)
        if row and row not in self.tree.selection():
            self.tree.selection_set(row)
        if self.tree.selection():
            self.menu.post(e.x_root, e.y_root)

    def _row_action(self, action):
        names = self._selected_names()
        if not names: return
        if len(names) > 1: self._bulk_action(action); return
        name = names[0]
        if action == "delete":
            if not messagebox.askyesno("Delete bot", f"PERMANENTLY delete '{name}'?"): return
        def do():
            try:
                if action == "spawn":     self.api.bots_spawn(name=name)
                elif action == "despawn": self.api.bots_despawn(name=name)
                elif action == "delete":  self.api.bots_delete(name)
                elif action == "respawn":
                    # despawn first (no-op if already offline), then spawn.
                    try: self.api.bots_despawn(name=name)
                    except Exception: pass
                    self.api.bots_spawn(name=name)
                self.after(0, self.refresh)
            except Exception as e:
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()

    def _view_stats(self):
        names = self._selected_names()
        if not names: return
        name = names[0]
        bot = next((b for b in self.all_bots if b.get("name") == name), None)
        if not bot or not bot.get("online"):
            messagebox.showinfo("View stats", "Bot must be online to view stats.")
            return
        def do():
            try:
                r = self.api.bot_inspect(name)
                self.after(0, lambda: StatsWindow(self, r))
            except Exception as e:
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()


# ----- Citizens tab -----

class CitizensFrame(ctk.CTkFrame):
    """Population control for Citizen-tier (FSM-driven) bots.

    Layout:
      - Top row: Refresh / Apply Now / Spawn Quick / Clear All / live count
      - Editable Treeview: archetype, count, x, y, plane, scatter, autospawn
      - Per-row Edit (selects + opens editor) / Add Row / Delete Row
      - Save Budget writes to data/citizen_budget.json on the server.
    """
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        self.archetypes = []   # list of {"name","label","category"}
        self.slots = []        # list of dicts (mirror of server slot format)
        self._dirty = False

        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 5))
        ctk.CTkLabel(top, text="Citizens", font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        self.live_label = ctk.CTkLabel(top, text="—", font=ctk.CTkFont(size=12))
        self.live_label.pack(side="left", padx=20)
        self.dirty_label = ctk.CTkLabel(top, text="", font=ctk.CTkFont(size=11), text_color="#e0a93f")
        self.dirty_label.pack(side="left", padx=10)

        actions = ctk.CTkFrame(self)
        actions.pack(fill="x", padx=20, pady=4)
        ctk.CTkButton(actions, text="Refresh", width=80, command=self.refresh).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Apply Budget Now", width=140, fg_color="#1b6e3a",
                      command=self._apply_budget).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Save Budget", width=110,
                      command=self._save_budget).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Load Budget", width=110,
                      command=self._load_budget).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Clear All Live", width=120, fg_color="#aa3030",
                      command=self._clear_all).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Reseed Defaults", width=130, fg_color="#a05522",
                      command=self._reseed_defaults).pack(side="left", padx=4)
        ctk.CTkLabel(actions, text=" │ ").pack(side="left", padx=2)
        ctk.CTkButton(actions, text="+ Add Row", width=90,
                      command=self._add_slot).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="− Delete Row", width=110,
                      command=self._delete_selected).pack(side="left", padx=4)

        # Quick-spawn (one-off, doesn't touch the saved budget)
        quick = ctk.CTkFrame(self)
        quick.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(quick, text="Quick Spawn:").pack(side="left", padx=4)
        self.q_count = tk.StringVar(value="20")
        ctk.CTkEntry(quick, textvariable=self.q_count, width=60).pack(side="left", padx=2)
        self.q_category = tk.StringVar(value="mixed")
        # All categories + each archetype subtype (server's randomFor() maps
        # exact enum names to the matching archetype now). User wants to
        # spawn each citizen subtype + sub cat separately.
        category_choices = [
            "mixed", "skiller", "combatant", "socialite", "minigamer",
            "castlewars", "soulwars", "stealingcreation",
            "trader_skill", "trader_combat", "trader_rare",
            "gambler", "bankstand",
            # exact archetype names
            "SKILLER_EFFICIENT", "SKILLER_CASUAL", "SKILLER_NOOB",
            "COMBATANT_PURE", "COMBATANT_TANK", "COMBATANT_HYBRID",
            "SOCIALITE_GAMBLER", "SOCIALITE_GE_TRADER",
            "SOCIALITE_GE_TRADER_SKILL", "SOCIALITE_GE_TRADER_COMBAT",
            "SOCIALITE_GE_TRADER_RARE", "SOCIALITE_BANKSTAND",
            "MINIGAMER_CASTLEWARS_RUSHER", "MINIGAMER_CASTLEWARS_DEFENDER",
            "MINIGAMER_SOULWARS_RUSHER", "MINIGAMER_SOULWARS_DEFENDER",
            "MINIGAMER_STEALINGCREATION_RUSHER", "MINIGAMER_STEALINGCREATION_DEFENDER",
            "MINIGAMER_RUSHER", "MINIGAMER_DEFENDER",
        ]
        ctk.CTkOptionMenu(quick, values=category_choices,
                          variable=self.q_category, width=240).pack(side="left", padx=2)
        ctk.CTkLabel(quick, text=" at X").pack(side="left", padx=(8,2))
        self.q_x = tk.StringVar(value="3164")
        ctk.CTkEntry(quick, textvariable=self.q_x, width=70).pack(side="left", padx=2)
        ctk.CTkLabel(quick, text="Y").pack(side="left", padx=(8,2))
        self.q_y = tk.StringVar(value="3486")
        ctk.CTkEntry(quick, textvariable=self.q_y, width=70).pack(side="left", padx=2)
        ctk.CTkLabel(quick, text="P").pack(side="left", padx=(8,2))
        self.q_plane = tk.StringVar(value="0")
        ctk.CTkEntry(quick, textvariable=self.q_plane, width=40).pack(side="left", padx=2)
        ctk.CTkButton(quick, text="Spawn", width=80, fg_color="#1b6e3a",
                      command=self._quick_spawn).pack(side="left", padx=8)
        # Per-minigame + per-socialite-tier quick rows
        mg_quick = ctk.CTkFrame(self)
        mg_quick.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(mg_quick, text="Quick: minigame ").pack(side="left", padx=4)
        self.mg_count = tk.StringVar(value="10")
        ctk.CTkEntry(mg_quick, textvariable=self.mg_count, width=50).pack(side="left", padx=2)
        for label, cat in [("Castle Wars","castlewars"), ("Soul Wars","soulwars"),
                           ("Stealing Creation","stealingcreation")]:
            ctk.CTkButton(mg_quick, text=label, width=130, fg_color="#1b6e3a",
                          command=lambda c=cat: self._minigame_spawn(c)).pack(side="left", padx=2)
        # Socialite tier shortcut row
        soc_quick = ctk.CTkFrame(self)
        soc_quick.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(soc_quick, text="Quick: socialite ").pack(side="left", padx=4)
        self.soc_count = tk.StringVar(value="10")
        ctk.CTkEntry(soc_quick, textvariable=self.soc_count, width=50).pack(side="left", padx=2)
        for label, arch in [("Skill Trader","SOCIALITE_GE_TRADER_SKILL"),
                            ("Combat Trader","SOCIALITE_GE_TRADER_COMBAT"),
                            ("Rare Trader","SOCIALITE_GE_TRADER_RARE"),
                            ("Bankstander","SOCIALITE_BANKSTAND"),
                            ("Gambler","SOCIALITE_GAMBLER")]:
            ctk.CTkButton(soc_quick, text=label, width=110, fg_color="#1b6e3a",
                          command=lambda a=arch: self._socialite_spawn(a)).pack(side="left", padx=2)

        # Combatant + PK quick-spawn row
        com_quick = ctk.CTkFrame(self)
        com_quick.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(com_quick, text="Quick: combatant ").pack(side="left", padx=4)
        self.com_count = tk.StringVar(value="10")
        ctk.CTkEntry(com_quick, textvariable=self.com_count, width=50).pack(side="left", padx=2)
        for label, arch in [("Pure","COMBATANT_PURE"),
                            ("Tank","COMBATANT_TANK"),
                            ("Hybrid","COMBATANT_HYBRID"),
                            ("PK Bot","COMBATANT_PKER")]:
            ctk.CTkButton(com_quick, text=label, width=110,
                          fg_color="#aa3030" if arch == "COMBATANT_PKER" else "#3a5588",
                          command=lambda a=arch: self._combatant_spawn(a)).pack(side="left", padx=2)

        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Treeview", background="#2b2b2b", foreground="white",
                        fieldbackground="#2b2b2b", borderwidth=0, rowheight=26)
        style.configure("Treeview.Heading", background="#1f6aa5", foreground="white", borderwidth=0)
        style.map("Treeview", background=[("selected", "#1f6aa5")])

        # Pane split: budget table on top, live bot detail list on bottom.
        # Budget table = configure auto-spawn slots. Live list = see who is
        # actually online + their location/state (like the AI tab does).
        ctk.CTkLabel(self, text="Budget slots", font=ctk.CTkFont(size=13, weight="bold")).pack(anchor="w", padx=20, pady=(10,2))
        tree_frame = ctk.CTkFrame(self)
        tree_frame.pack(fill="both", expand=True, padx=20, pady=(0,5))
        cols = ("archetype", "count", "x", "y", "plane", "scatter", "autospawn", "live")
        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings", selectmode="browse", height=8)
        widths = {"archetype":210, "count":60, "x":80, "y":80, "plane":50,
                  "scatter":70, "autospawn":80, "live":60}
        for c in cols:
            self.tree.heading(c, text=c)
            self.tree.column(c, width=widths.get(c, 80), anchor="w")
        vsb = ttk.Scrollbar(tree_frame, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscroll=vsb.set)
        vsb.pack(side="right", fill="y")
        self.tree.pack(fill="both", expand=True)
        self.tree.bind("<Double-1>", self._edit_selected)

        # Live bots detail list
        live_header = ctk.CTkFrame(self, fg_color="transparent")
        live_header.pack(fill="x", padx=20, pady=(10,2))
        ctk.CTkLabel(live_header, text="Live citizens", font=ctk.CTkFont(size=13, weight="bold")).pack(side="left")
        self.live_filter = tk.StringVar(value="")
        ctk.CTkLabel(live_header, text="  filter:").pack(side="left", padx=(20,2))
        ctk.CTkEntry(live_header, textvariable=self.live_filter, width=160,
                     placeholder_text="archetype/state/name").pack(side="left")
        ctk.CTkButton(live_header, text="Apply", width=60,
                      command=self._refresh_live_table).pack(side="left", padx=4)

        live_frame = ctk.CTkFrame(self)
        live_frame.pack(fill="both", expand=True, padx=20, pady=(0,10))
        live_cols = ("name", "archetype", "state", "x", "y", "plane", "cb")
        self.live_tree = ttk.Treeview(live_frame, columns=live_cols, show="headings", selectmode="browse", height=10)
        live_widths = {"name":140, "archetype":230, "state":110, "x":60, "y":60, "plane":50, "cb":50}
        for c in live_cols:
            self.live_tree.heading(c, text=c)
            self.live_tree.column(c, width=live_widths.get(c, 80), anchor="w")
        live_vsb = ttk.Scrollbar(live_frame, orient="vertical", command=self.live_tree.yview)
        self.live_tree.configure(yscroll=live_vsb.set)
        live_vsb.pack(side="right", fill="y")
        self.live_tree.pack(fill="both", expand=True)
        # Cache of last refresh (for filter apply without re-querying)
        self._last_bots = []

    def on_show(self):
        self.refresh()

    def refresh(self):
        def do():
            try:
                arch = self.api.citizens_archetypes()
                budget = self.api.citizens_budget_get()
                live = self.api.citizens()
                self.after(0, lambda: self._apply_data(arch, budget, live))
            except Exception as e:
                self.after(0, lambda: self.live_label.configure(
                    text=f"refresh failed: {e}", text_color="#cc3030"))
        threading.Thread(target=do, daemon=True).start()

    def _apply_data(self, arch_resp, budget_resp, live_resp):
        self.archetypes = arch_resp.get("archetypes", []) if arch_resp else []
        # Prefer the user's local saved budget over whatever the server
        # has - the local file is the source of truth now. Falls back to
        # the server's slots if no local file yet (first run).
        local_slots = self._read_local_budget()
        if local_slots is not None:
            self.slots = local_slots
        else:
            self.slots = budget_resp.get("slots", []) if budget_resp else []
        live_total = live_resp.get("total", 0) if live_resp else 0
        live_by_arch = (live_resp or {}).get("byArchetype", {})
        self._last_bots = (live_resp or {}).get("bots", [])
        self.live_label.configure(text=f"Live: {live_total}", text_color="#3fbf3f")
        self._render_table(live_by_arch)
        self._refresh_live_table()
        self._dirty = False

    def _read_local_budget(self):
        """Return slots list from the local budget file, or None if absent."""
        try:
            import os, json as _json
            path = self._budget_file()
            if not os.path.exists(path):
                return None
            with open(path, "r") as f:
                data = _json.load(f)
            slots = data.get("slots")
            return list(slots) if isinstance(slots, list) else None
        except Exception:
            return None
        self.dirty_label.configure(text="")

    def _refresh_live_table(self):
        """Re-render the live-bot detail table from cached _last_bots,
        applying the current filter string (case-insensitive substring
        match against archetype/state/name)."""
        for iid in self.live_tree.get_children():
            self.live_tree.delete(iid)
        flt = (self.live_filter.get() if hasattr(self, "live_filter") else "").lower().strip()
        for i, bot in enumerate(self._last_bots):
            name = bot.get("name", "?")
            arch = bot.get("archetype", "?")
            state = bot.get("state", "?")
            if flt and flt not in name.lower() and flt not in arch.lower() and flt not in state.lower():
                continue
            self.live_tree.insert("", "end", iid=str(i), values=(
                name, arch, state,
                bot.get("x", 0), bot.get("y", 0), bot.get("plane", 0),
                bot.get("cb", 0),
            ))

    def _render_table(self, live_by_arch):
        for iid in self.tree.get_children():
            self.tree.delete(iid)
        for i, s in enumerate(self.slots):
            name = s.get("archetype", "?")
            self.tree.insert("", "end", iid=str(i), values=(
                name,
                s.get("count", 0),
                s.get("x", 0),
                s.get("y", 0),
                s.get("plane", 0),
                s.get("scatter", 8),
                "yes" if s.get("autospawn", False) else "no",
                live_by_arch.get(name, 0)
            ))

    def _mark_dirty(self):
        self._dirty = True
        self.dirty_label.configure(text="● unsaved changes")

    def _add_slot(self):
        # Default to a generic socialite at GE; immediately open the editor
        # so the user fills in the real values rather than ending up with
        # multiple identical default rows (the "copies" complaint).
        new_slot = {
            "archetype": (self.archetypes[0]["name"] if self.archetypes else "SOCIALITE_BANKSTAND"),
            "count": 10, "x": 3164, "y": 3486, "plane": 0,
            "scatter": 8, "autospawn": False
        }
        self.slots.append(new_slot)
        self._mark_dirty()
        self._render_table({})
        # Auto-edit the new row.
        idx = len(self.slots) - 1
        self.tree.selection_set(str(idx))
        SlotEditor(self, self.slots[idx], self.archetypes, self._on_slot_edited).load(idx)

    def _reseed_defaults(self):
        if not messagebox.askyesno("Reseed defaults",
                "Wipe the current budget and restore server defaults? "
                "Your edits will be backed up server-side as "
                "data/citizen_budget.json.bak.reseed-<timestamp>."):
            return
        def do():
            try:
                resp = self.api.citizens_budget_reseed()
                self.after(0, lambda: messagebox.showinfo("Reseeded",
                    f"Server reseeded {resp.get('slots', '?')} default slots"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Reseed failed", str(e)))
        threading.Thread(target=do, daemon=True).start()

    def _delete_selected(self):
        sel = self.tree.selection()
        if not sel: return
        idx = int(sel[0])
        if 0 <= idx < len(self.slots):
            del self.slots[idx]
            self._mark_dirty()
            self._render_table({})

    def _edit_selected(self, _evt=None):
        sel = self.tree.selection()
        if not sel: return
        idx = int(sel[0])
        if not (0 <= idx < len(self.slots)): return
        SlotEditor(self, self.slots[idx], self.archetypes, self._on_slot_edited).load(idx)

    def _on_slot_edited(self, idx, updated):
        self.slots[idx] = updated
        self._mark_dirty()
        self._render_table({})

    def _save_budget(self):
        # Local-file save. The server-side save kept hitting parser /
        # whitespace / "0 slots" issues and the user lost edits over and
        # over. From now on the source of truth is a JSON file on the
        # user's machine; the server only sees it when "Apply Budget Now"
        # pushes it. Hitting Save here cannot fail because of the server.
        path = self._budget_file()
        try:
            import os, json as _json
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w") as f:
                _json.dump({"slots": self.slots}, f, indent=2)
            self._dirty = False
            self.after(0, lambda: self.dirty_label.configure(text=""))
            messagebox.showinfo("Saved",
                f"Budget saved locally ({len(self.slots)} slots)\n{path}")
        except Exception as e:
            messagebox.showerror("Save failed", f"{type(e).__name__}: {e}")

    def _load_budget(self):
        path = self._budget_file()
        try:
            import os, json as _json
            if not os.path.exists(path):
                messagebox.showinfo("Load Budget",
                    f"No saved budget at:\n{path}\n\n"
                    "Save the current budget first to create one.")
                return
            with open(path, "r") as f:
                data = _json.load(f)
            slots = data.get("slots") or []
            self.slots = list(slots)
            self._dirty = True
            self._render_table({})
            self.after(0, lambda: self.dirty_label.configure(
                text="loaded from disk - hit Apply Budget Now to push to server"))
            messagebox.showinfo("Loaded",
                f"Loaded {len(self.slots)} slots from\n{path}")
        except Exception as e:
            messagebox.showerror("Load failed", f"{type(e).__name__}: {e}")

    @staticmethod
    def _budget_file():
        # Lives next to admin_panel.py so it travels with the project,
        # rather than in $HOME where it could get separated from the
        # codebase.  data/admin/citizen_budget.json keeps the data dir
        # convention used by the rest of the project.
        import os
        here = os.path.dirname(os.path.abspath(__file__))
        return os.path.join(here, "data", "admin", "citizen_budget.json")

    def _apply_budget(self):
        # Push the local slots to the server first, then trigger apply.
        # Server-side budget is now slaved to the local file - the user's
        # source of truth is on their machine, the server's copy is just
        # what gets used to autospawn between restarts.
        def do():
            try:
                if self.slots:
                    self.api.citizens_budget_set(self.slots)
                resp = self.api.citizens_budget_apply(include_manual=True)
                spawned = resp.get("spawned", 0)
                total = resp.get("total", 0)
                self.after(0, lambda: messagebox.showinfo("Applied",
                    f"Spawned {spawned} citizens (live total: {total})"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Apply failed", str(e)))
        threading.Thread(target=do, daemon=True).start()

    def _clear_all(self):
        if not messagebox.askyesno("Clear", "Despawn ALL live citizens?"): return
        def do():
            try:
                self.api.citizens_clear()
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Clear failed", str(e)))
        threading.Thread(target=do, daemon=True).start()

    def _quick_spawn(self):
        try:
            n = int(self.q_count.get())
            x = int(self.q_x.get()); y = int(self.q_y.get()); p = int(self.q_plane.get())
        except ValueError:
            messagebox.showerror("Bad input", "count/x/y/plane must be numbers")
            return
        cat = self.q_category.get()
        if cat == "mixed": cat = None
        def do():
            try:
                resp = self.api.citizens_spawn(n, category=cat, x=x, y=y, plane=p, scatter=12)
                self.after(0, lambda: messagebox.showinfo("Spawned",
                    f"Spawned {resp.get('spawned',0)} (live total: {resp.get('total',0)})"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Spawn failed", str(e)))
        threading.Thread(target=do, daemon=True).start()

    def _combatant_spawn(self, archetype_name):
        """Quick-spawn N combatant / PK bots. PKers spawn in low wildy
        (3093, 3525) by default; pure / tank / hybrid spawn at the
        Edgeville / wildy edge (3088, 3491)."""
        try:
            n = int(self.com_count.get())
        except ValueError:
            messagebox.showerror("Bad input", "count must be a number")
            return
        if archetype_name == "COMBATANT_PKER":
            x, y, p = 3093, 3525, 0
        else:
            x, y, p = 3088, 3491, 0
        def do():
            try:
                resp = self.api.citizens_spawn(n, category=archetype_name,
                    x=x, y=y, plane=p, scatter=10)
                self.after(0, lambda: messagebox.showinfo("Combatant spawn",
                    f"Spawned {resp.get('spawned',0)} {archetype_name} "
                    f"(live total: {resp.get('total',0)})"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Spawn failed", str(e)))
        threading.Thread(target=do, daemon=True).start()

    def _socialite_spawn(self, archetype_name):
        """Quick-spawn N citizens of an exact socialite archetype.
        Server's randomFor() recognises full enum names so we pass the
        archetype name as the category. Anchor coords pulled from the
        archetype's socialiteAnchor() server-side, so x/y here are dummies."""
        try:
            n = int(self.soc_count.get())
        except ValueError:
            messagebox.showerror("Bad input", "count must be a number")
            return
        # Anchor inferred server-side from archetype.socialiteAnchor() but
        # we still pass GE coords as a sane default.
        x, y, p = 3164, 3486, 0
        def do():
            try:
                resp = self.api.citizens_spawn(n, category=archetype_name,
                    x=x, y=y, plane=p, scatter=12)
                self.after(0, lambda: messagebox.showinfo("Socialite spawn",
                    f"Spawned {resp.get('spawned',0)} {archetype_name} "
                    f"(live total: {resp.get('total',0)})"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Spawn failed", str(e)))
        threading.Thread(target=do, daemon=True).start()

    def _minigame_spawn(self, category):
        """Spawn N citizens at a specific minigame's lobby tile.

        Server-side, AmbientArchetype.lobbyTile() pins the spawn location
        for any minigame archetype regardless of the X/Y the request sends.
        We pass dummy coords; the spawner ignores them for these categories.
        """
        try:
            n = int(self.mg_count.get())
        except ValueError:
            messagebox.showerror("Bad input", "count must be a number")
            return
        # Dummy anchor - server pins to lobbyTile() per archetype.
        lobby = {"castlewars": (2442,3090,0),
                 "soulwars":   (2210,3056,0),
                 "stealingcreation": (2860,5567,0)}.get(category, (3164,3486,0))
        def do():
            try:
                resp = self.api.citizens_spawn(n, category=category,
                    x=lobby[0], y=lobby[1], plane=lobby[2], scatter=6)
                self.after(0, lambda: messagebox.showinfo("Minigame spawn",
                    f"Spawned {resp.get('spawned',0)} {category} citizens "
                    f"(live total: {resp.get('total',0)})"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Spawn failed", str(e)))
        threading.Thread(target=do, daemon=True).start()


class SlotEditor(ctk.CTkToplevel):
    """Edit a single budget slot's fields."""
    def __init__(self, parent, slot, archetypes, on_save):
        super().__init__(parent)
        self.title("Edit Citizen Slot")
        self.geometry("440x340")
        self.on_save = on_save
        self.slot = dict(slot)  # local copy

        # archetype dropdown
        ctk.CTkLabel(self, text="Archetype:").pack(anchor="w", padx=20, pady=(20,2))
        self.arch_var = tk.StringVar(value=slot.get("archetype", ""))
        names = [a["name"] for a in archetypes] if archetypes else [slot.get("archetype", "")]
        ctk.CTkOptionMenu(self, variable=self.arch_var, values=names, width=380).pack(padx=20)

        # count / scatter
        row1 = ctk.CTkFrame(self, fg_color="transparent")
        row1.pack(fill="x", padx=20, pady=8)
        ctk.CTkLabel(row1, text="Target count:").pack(side="left")
        self.count_var = tk.StringVar(value=str(slot.get("count", 10)))
        ctk.CTkEntry(row1, textvariable=self.count_var, width=80).pack(side="left", padx=8)
        ctk.CTkLabel(row1, text="Scatter:").pack(side="left", padx=(20,4))
        self.scatter_var = tk.StringVar(value=str(slot.get("scatter", 8)))
        ctk.CTkEntry(row1, textvariable=self.scatter_var, width=60).pack(side="left", padx=4)

        # anchor x/y/plane
        row2 = ctk.CTkFrame(self, fg_color="transparent")
        row2.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(row2, text="Anchor X:").pack(side="left")
        self.x_var = tk.StringVar(value=str(slot.get("x", 3164)))
        ctk.CTkEntry(row2, textvariable=self.x_var, width=80).pack(side="left", padx=4)
        ctk.CTkLabel(row2, text="Y:").pack(side="left", padx=(20,4))
        self.y_var = tk.StringVar(value=str(slot.get("y", 3486)))
        ctk.CTkEntry(row2, textvariable=self.y_var, width=80).pack(side="left", padx=4)
        ctk.CTkLabel(row2, text="Plane:").pack(side="left", padx=(20,4))
        self.plane_var = tk.StringVar(value=str(slot.get("plane", 0)))
        ctk.CTkEntry(row2, textvariable=self.plane_var, width=40).pack(side="left", padx=4)

        # autospawn
        self.autospawn_var = tk.BooleanVar(value=bool(slot.get("autospawn", False)))
        ctk.CTkCheckBox(self, text="Auto-spawn on server start",
                        variable=self.autospawn_var).pack(anchor="w", padx=20, pady=10)

        # buttons
        btns = ctk.CTkFrame(self, fg_color="transparent")
        btns.pack(side="bottom", fill="x", padx=20, pady=20)
        ctk.CTkButton(btns, text="Cancel", width=80, command=self.destroy).pack(side="right", padx=4)
        ctk.CTkButton(btns, text="Save", width=80, fg_color="#1b6e3a",
                      command=self._save).pack(side="right", padx=4)

        self.idx = -1

    def load(self, idx):
        self.idx = idx
        self.lift()
        self.grab_set()

    def _save(self):
        try:
            updated = {
                "archetype": self.arch_var.get(),
                "count":   int(self.count_var.get()),
                "x":       int(self.x_var.get()),
                "y":       int(self.y_var.get()),
                "plane":   int(self.plane_var.get()),
                "scatter": int(self.scatter_var.get()),
                "autospawn": bool(self.autospawn_var.get()),
            }
        except ValueError as e:
            messagebox.showerror("Invalid", f"Numeric field error: {e}")
            return
        if self.on_save:
            self.on_save(self.idx, updated)
        self.destroy()


# ----- Gear Sets tab -----

class GearSetsFrame(ctk.CTkFrame):
    """View / add / edit / delete bot outfit presets per archetype.

    Reads server-side via /admin/gear/sets, writes back the full pool
    on Save. Each row is one outfit (hat/chest/legs ids + a name). The
    Java side reads data/gear_sets.json on startup + on every save.
    """
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        self.pools = {}      # key -> [outfit dicts]
        self.current_key = "socialite"
        self.dirty = False

        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 5))
        ctk.CTkLabel(top, text="Gear Sets",
                     font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        self.dirty_label = ctk.CTkLabel(top, text="",
            font=ctk.CTkFont(size=11), text_color="#e0a93f")
        self.dirty_label.pack(side="left", padx=20)

        actions = ctk.CTkFrame(self)
        actions.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(actions, text="Pool:").pack(side="left", padx=4)
        self.pool_var = tk.StringVar(value=self.current_key)
        self.pool_menu = ctk.CTkOptionMenu(actions, values=[self.current_key],
            variable=self.pool_var, width=140,
            command=self._switch_pool)
        self.pool_menu.pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Refresh", width=90,
                      command=self.refresh).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Save", width=90, fg_color="#1b6e3a",
                      command=self._save).pack(side="left", padx=4)
        ctk.CTkLabel(actions, text=" │ ").pack(side="left", padx=2)
        ctk.CTkButton(actions, text="+ Add Outfit", width=110,
                      command=self._add).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="− Delete Selected", width=140,
                      fg_color="#aa3030",
                      command=self._delete_selected).pack(side="left", padx=4)

        ctk.CTkLabel(self,
            text="Each outfit = name + 3 item ids (hat/chest/legs). Use -1 for "
                 "no hat. IDs verified against the cache; double-click a row "
                 "to edit. Save to push the whole pool back to the server.",
            font=ctk.CTkFont(size=11), justify="left", text_color="#888"
            ).pack(fill="x", padx=20, pady=(0, 4))

        tree_frame = ctk.CTkFrame(self)
        tree_frame.pack(fill="both", expand=True, padx=20, pady=10)
        cols = ("idx", "name", "hat", "chest", "legs")
        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings",
                                  selectmode="browse")
        widths = {"idx": 50, "name": 240, "hat": 80, "chest": 80, "legs": 80}
        for c in cols:
            self.tree.heading(c, text=c)
            self.tree.column(c, width=widths.get(c, 80), anchor="w")
        vsb = ttk.Scrollbar(tree_frame, orient="vertical",
                             command=self.tree.yview)
        self.tree.configure(yscroll=vsb.set)
        vsb.pack(side="right", fill="y")
        self.tree.pack(fill="both", expand=True)
        self.tree.bind("<Double-1>", self._edit_selected)

    def on_show(self):
        self.refresh()

    def refresh(self):
        def do():
            try:
                resp = self.api.gear_sets_get()
                self.after(0, lambda: self._apply(resp))
            except Exception as e:
                self.after(0, lambda: self.dirty_label.configure(
                    text=f"refresh failed: {e}", text_color="#cc3030"))
        threading.Thread(target=do, daemon=True).start()

    def _apply(self, resp):
        self.pools = (resp or {}).get("pools", {}) or {}
        keys = list(self.pools.keys()) or ["socialite"]
        if self.current_key not in self.pools:
            self.current_key = keys[0]
        self.pool_var.set(self.current_key)
        self.pool_menu.configure(values=keys)
        self.dirty = False
        self.dirty_label.configure(text="")
        self._render()

    def _switch_pool(self, key):
        if self.dirty:
            if not messagebox.askyesno("Unsaved changes",
                "Discard unsaved changes and switch?"):
                self.pool_var.set(self.current_key)
                return
        self.current_key = key
        self.dirty = False
        self.dirty_label.configure(text="")
        self._render()

    def _render(self):
        for iid in self.tree.get_children():
            self.tree.delete(iid)
        outfits = self.pools.get(self.current_key, [])
        for i, o in enumerate(outfits):
            self.tree.insert("", "end", iid=str(i), values=(
                i, o.get("name", "?"), o.get("hat", -1),
                o.get("chest", -1), o.get("legs", -1)))

    def _mark_dirty(self):
        self.dirty = True
        self.dirty_label.configure(text="● unsaved", text_color="#e0a93f")

    def _add(self):
        name = simpledialog.askstring("New outfit", "Name:",
            initialvalue=f"new outfit {len(self.pools.get(self.current_key, []))}")
        if name is None: return
        new_o = {"name": name, "hat": -1, "chest": 1005, "legs": 1013}
        self.pools.setdefault(self.current_key, []).append(new_o)
        self._mark_dirty()
        self._render()
        # Open editor for the new entry
        last = len(self.pools[self.current_key]) - 1
        self.tree.selection_set(str(last))
        self._edit_selected()

    def _delete_selected(self):
        sel = self.tree.selection()
        if not sel: return
        idx = int(sel[0])
        outs = self.pools.get(self.current_key, [])
        if 0 <= idx < len(outs):
            del outs[idx]
            self._mark_dirty()
            self._render()

    def _edit_selected(self, _evt=None):
        sel = self.tree.selection()
        if not sel: return
        idx = int(sel[0])
        outs = self.pools.get(self.current_key, [])
        if not (0 <= idx < len(outs)): return
        OutfitEditor(self, outs[idx], self._on_outfit_edited, idx).load()

    def _on_outfit_edited(self, idx, updated):
        outs = self.pools.get(self.current_key, [])
        if 0 <= idx < len(outs):
            outs[idx] = updated
            self._mark_dirty()
            self._render()

    def _save(self):
        outs = self.pools.get(self.current_key, [])
        def do():
            try:
                resp = self.api.gear_sets_save(self.current_key, outs)
                saved = resp.get("saved", "?")
                self.after(0, lambda: messagebox.showinfo(
                    "Saved", f"Server saved {saved} outfits in '{self.current_key}'"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Save failed", str(e)))
        threading.Thread(target=do, daemon=True).start()


class OutfitEditor(ctk.CTkToplevel):
    """Modal editor for one outfit row."""
    def __init__(self, parent, outfit, on_save, idx):
        super().__init__(parent)
        self.title("Edit Outfit")
        self.geometry("420x260")
        self.outfit = dict(outfit)
        self.on_save = on_save
        self.idx = idx

        ctk.CTkLabel(self, text="Name:").pack(anchor="w", padx=20, pady=(20, 2))
        self.name_var = tk.StringVar(value=outfit.get("name", ""))
        ctk.CTkEntry(self, textvariable=self.name_var, width=380).pack(padx=20)

        for label, key in [("Hat ID (-1 for none):", "hat"),
                           ("Chest ID:", "chest"),
                           ("Legs ID:", "legs")]:
            ctk.CTkLabel(self, text=label).pack(anchor="w", padx=20, pady=(8, 0))
            v = tk.StringVar(value=str(outfit.get(key, -1)))
            setattr(self, f"{key}_var", v)
            ctk.CTkEntry(self, textvariable=v, width=120).pack(anchor="w", padx=20)

        btns = ctk.CTkFrame(self, fg_color="transparent")
        btns.pack(side="bottom", fill="x", pady=10)
        ctk.CTkButton(btns, text="Save", width=80, fg_color="#1b6e3a",
                      command=self._save).pack(side="right", padx=10)
        ctk.CTkButton(btns, text="Cancel", width=80,
                      command=self.destroy).pack(side="right")

    def load(self):
        self.transient(self.master)
        self.grab_set()

    def _save(self):
        try:
            updated = {
                "name": self.name_var.get().strip(),
                "hat": int(self.hat_var.get()),
                "chest": int(self.chest_var.get()),
                "legs": int(self.legs_var.get()),
            }
        except ValueError:
            messagebox.showerror("Bad input",
                "Hat / Chest / Legs must be integers (-1 for none on hat).")
            return
        self.on_save(self.idx, updated)
        self.destroy()


# ----- Phantom GE tab -----

class PhantomGEFrame(ctk.CTkFrame):
    """Toggle + tune the auto-GE shadow market and view recent fills.

    The phantom market fills player offers from a virtual counter-party
    at randomized prices within tolerance. This UI exposes:

      - Master enabled/disabled toggle
      - Per-tier fill rate multipliers (BULK / COMBAT / RARE)
      - Base rates (placement + per-tick) and spread tolerance
      - Anti-abuse caps (per-player + per-item / hour)
      - Live fill log (last 50 events: who, what, qty, price, side)

    Edits push immediately to /admin/phantom-ge.
    """
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        self.config = {}
        self.fills = []

        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 5))
        ctk.CTkLabel(top, text="Phantom GE",
                     font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        self.status_label = ctk.CTkLabel(top, text="—",
            font=ctk.CTkFont(size=12))
        self.status_label.pack(side="left", padx=20)

        actions = ctk.CTkFrame(self)
        actions.pack(fill="x", padx=20, pady=4)
        ctk.CTkButton(actions, text="Refresh", width=100,
                      command=self.refresh).pack(side="left", padx=4)
        self.toggle_btn = ctk.CTkButton(actions, text="Enable / Disable",
            width=160, command=self._toggle)
        self.toggle_btn.pack(side="left", padx=4)

        # ------- How-it-works explainer -------
        explainer = ctk.CTkFrame(self, fg_color="#1a1d22")
        explainer.pack(fill="x", padx=20, pady=(0, 8))
        ctk.CTkLabel(explainer, text="How Phantom GE works",
                     font=ctk.CTkFont(size=13, weight="bold"),
                     text_color="#e0a93f", anchor="w").pack(
                     anchor="w", padx=12, pady=(8, 2))
        ctk.CTkLabel(explainer, justify="left", anchor="w", wraplength=900,
            font=ctk.CTkFont(size=11), text_color="#cccccc",
            text=(
"Player places a GE offer. Phantom GE rolls TWO chances to fill it from a virtual counter-party:\n"
"\n"
"  1. ON PLACE  (~5s after Confirm):  rate = fillRateOnPlace × tierMultiplier   (capped at 100%)\n"
"  2. AGING TICK (every 30s, after offer is minAgeBeforeFillMs old):\n"
"                                     rate = fillRatePerTick × tierMultiplier\n"
"\n"
"Tiers come from the item's GE reference price:\n"
"  cheap < 1k  | bulk < 10k | low < 100k | mid < 1m | high < 10m | rare ≥ 10m\n"
"\n"
"Each tier has its own multiplier (below).  Example with current defaults:\n"
"  • bulk item (logs):  on-place = 0.50 × 6 = 100% (instant);  per-tick = 0.25 × 6 = 100% (instant if it survived).\n"
"  • mid item (whip):   on-place = 0.50 × 1.5 = 75%;  per-tick = 37.5% per 30s → ~80s expected.\n"
"  • rare (partyhat):   on-place = 0.50 × 0.3 = 15%;  per-tick = 7.5% per 30s → ~7 min expected.\n"
"\n"
"Items NEVER fill if:\n"
"  (a) item not in the bot trader catalog (no reference price → tryFill returns)\n"
"  (b) listed price outside ±acceptableSpread of catalog (default ±30%)\n"
"  (c) hit a cap: maxFillsPerPlayerPerHour or maxFillsPerItemPerHour\n"
"  (d) owner logged out\n"
"  (e) Phantom GE disabled\n"
"\n"
"Add missing items via the GE Prices tab (sets reference price) or the Items tab (tradeable + price).")
            ).pack(anchor="w", padx=12, pady=(0, 8), fill="x")

        # Two columns: left = config knobs, right = fill log
        body = ctk.CTkFrame(self, fg_color="transparent")
        body.pack(fill="both", expand=True, padx=20, pady=10)
        body.grid_columnconfigure(0, weight=1)
        body.grid_columnconfigure(1, weight=1)
        body.grid_rowconfigure(0, weight=1)

        # ------- Left: config knobs -------
        cfg_frame = ctk.CTkScrollableFrame(body, label_text="Config")
        cfg_frame.grid(row=0, column=0, sticky="nsew", padx=(0, 5))

        # Each knob: label + entry + apply button
        self.knob_vars = {}
        knobs = [
            ("enabled",
                "Master toggle. true=phantom fills are active, false=disabled "
                "entirely (no auto-fills, P2P matching still works)."),
            ("fillRateOnPlace",
                "Base chance an offer fills ~5s after placement. "
                "Multiplied by the tier mult below. Default 0.50 → "
                "bulk = 100%, mid = 75%, high = 50%, rare = 15%."),
            ("fillRatePerTick",
                "Base chance per 30s tick that an aging offer fills. "
                "Default 0.25 → mid = 37%/30s (~80s avg), high = 25%/30s "
                "(~2 min avg), rare = 7.5%/30s (~7 min avg)."),
            ("acceptableSpread",
                "Price tolerance vs the GE reference. 0.30 = phantom only "
                "fills if player's price is within ±30%. Rejects 'wtb phat 1gp' "
                "trolls. Lower = stricter, raise to allow extreme listings."),
            ("minAgeBeforeFillMs",
                "Offer must sit this long before any phantom fill or aging "
                "tick. Lets players cancel misclicks. Default 10000 = 10s."),
            ("cheapMultiplier",
                "Items < 1k gp (eg copper ore, raw shrimps). 10x default = "
                "always instant on placement (capped at 100%)."),
            ("bulkMultiplier",
                "Items < 10k gp (logs, ores, runes, raw fish). 6x default = "
                "instant on placement, fills any aging offer in 1-2 ticks."),
            ("lowMultiplier",
                "Items < 100k gp (rune armor, dragon scim, basic amulets). "
                "3x default = ~150% on place (instant), ~75%/tick aging."),
            ("midMultiplier",
                "Items < 1m gp (whips, dragon weapons, mystic, mid jewelry). "
                "1.5x default = 75% on place, ~37%/30s aging (1-2 min avg)."),
            ("highMultiplier",
                "Items < 10m gp (bandos, armadyl, fury, ags/bgs). 1.0x "
                "default = 50% on place, 25%/30s aging (~2 min avg)."),
            ("rareMultiplier",
                "Items ≥ 10m gp (partyhats, hween masks, third age, phats). "
                "0.3x default = 15% on place, 7.5%/30s aging (~7 min avg)."),
            ("maxFillsPerPlayerPerHour",
                "Anti-abuse: max phantom fills any one player can receive per "
                "hour. Default 200 (was 50) - matches the higher fill rates "
                "so casual flippers don't hit the cap."),
            ("maxFillsPerItemPerHour",
                "Anti-abuse: max phantom fills total for any one item id per "
                "hour. Default 100 (was 20). Stops draining a single hot "
                "item's phantom inventory."),
            ("partialFillChance",
                "When phantom DOES fill, this is the chance it only fills "
                "20-70% of the offer instead of all. Mimics multiple small "
                "buyers/sellers vs one big match. 0.40 = 40% of fills are "
                "partial."),
        ]
        for i, (key, helptxt) in enumerate(knobs):
            ctk.CTkLabel(cfg_frame, text=key + ":", anchor="w"
                ).grid(row=i, column=0, sticky="w", padx=4, pady=2)
            v = tk.StringVar(value="—")
            self.knob_vars[key] = v
            ctk.CTkEntry(cfg_frame, textvariable=v, width=120
                ).grid(row=i, column=1, padx=4, pady=2)
            ctk.CTkButton(cfg_frame, text="Apply", width=60,
                command=lambda k=key: self._apply_knob(k)
                ).grid(row=i, column=2, padx=4, pady=2)
            ctk.CTkLabel(cfg_frame, text=helptxt, anchor="w", wraplength=320,
                justify="left", font=ctk.CTkFont(size=10), text_color="#888"
                ).grid(row=i, column=3, sticky="nw", padx=8, pady=4)

        # ------- Right: live fill log -------
        log_frame = ctk.CTkFrame(body)
        log_frame.grid(row=0, column=1, sticky="nsew", padx=(5, 0))
        ctk.CTkLabel(log_frame, text="Recent fills (last 50)",
            font=ctk.CTkFont(size=13, weight="bold")
            ).pack(anchor="w", padx=10, pady=(8, 4))
        log_inner = ctk.CTkFrame(log_frame)
        log_inner.pack(fill="both", expand=True, padx=10, pady=(0, 10))
        cols = ("time", "side", "player", "item", "qty", "price")
        self.fill_tree = ttk.Treeview(log_inner, columns=cols, show="headings",
            selectmode="browse")
        widths = {"time": 80, "side": 50, "player": 100, "item": 60,
                  "qty": 60, "price": 100}
        for c in cols:
            self.fill_tree.heading(c, text=c)
            self.fill_tree.column(c, width=widths.get(c, 70), anchor="w")
        vsb = ttk.Scrollbar(log_inner, orient="vertical",
            command=self.fill_tree.yview)
        self.fill_tree.configure(yscroll=vsb.set)
        vsb.pack(side="right", fill="y")
        self.fill_tree.pack(fill="both", expand=True)

    def on_show(self):
        self.refresh()

    def refresh(self):
        def do():
            try:
                resp = self.api.phantom_ge_get()
                self.after(0, lambda: self._apply(resp))
            except Exception as e:
                self.after(0, lambda: self.status_label.configure(
                    text=f"refresh failed: {e}", text_color="#cc3030"))
        threading.Thread(target=do, daemon=True).start()

    def _apply(self, resp):
        self.config = (resp or {}).get("config", {}) or {}
        self.fills = (resp or {}).get("recentFills", []) or []
        en = self.config.get("enabled", False)
        self.status_label.configure(
            text=f"{'ENABLED' if en else 'disabled'} · {len(self.fills)} fills logged",
            text_color=("#3fbf3f" if en else "#888"))
        self.toggle_btn.configure(
            text="Disable" if en else "Enable",
            fg_color=("#aa3030" if en else "#1b6e3a"))
        for k, v in self.knob_vars.items():
            cur = self.config.get(k)
            if cur is not None:
                v.set(_fmt_knob(cur))
        # Render fill log (newest at top)
        for iid in self.fill_tree.get_children():
            self.fill_tree.delete(iid)
        import datetime
        for i, f in enumerate(reversed(self.fills)):
            ts = datetime.datetime.fromtimestamp(f.get("ts", 0) / 1000.0).strftime("%H:%M:%S")
            side = "BUY" if f.get("buy") else "SELL"
            self.fill_tree.insert("", "end", iid=str(i), values=(
                ts, side, f.get("player", "?"), f.get("itemId", 0),
                f.get("amount", 0), _fmt_gp(f.get("price", 0))))

    def _toggle(self):
        new = not bool(self.config.get("enabled", False))
        self._apply_value("enabled", new)

    def _apply_knob(self, key):
        raw = self.knob_vars[key].get().strip()
        if raw in ("", "—"):
            return
        # Parse: bool / int / float
        if raw.lower() in ("true", "false"):
            v = (raw.lower() == "true")
        else:
            try:
                v = int(raw)
            except ValueError:
                try: v = float(raw)
                except ValueError:
                    messagebox.showerror("Bad value",
                        f"Couldn't parse '{raw}' as number/bool.")
                    return
        self._apply_value(key, v)

    def _apply_value(self, key, value):
        def do():
            try:
                resp = self.api.phantom_ge_set({key: value})
                self.after(0, lambda: messagebox.showinfo(
                    "Applied", f"server applied {resp.get('applied', 0)} change(s)"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Update failed", str(e)))
        threading.Thread(target=do, daemon=True).start()


def _fmt_knob(v):
    """Render a knob value in the entry box."""
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, float):
        # Strip trailing zeros: 0.10 -> 0.1
        s = f"{v:.6f}".rstrip("0").rstrip(".")
        return s or "0"
    return str(v)


# ----- GE Prices tab -----

def _fmt_gp(n):
    """Match the Java BotTradeHandler.fmtGp - 800m / 50k / 1.5b."""
    try: n = int(n)
    except (TypeError, ValueError): return "?"
    sign = "-" if n < 0 else ""
    n = abs(n)
    if n >= 1_000_000_000:
        v = n / 1_000_000_000
        return sign + (f"{int(v)}b" if v == int(v) else f"{v:.1f}b".rstrip("0").rstrip("."))
    if n >= 1_000_000:
        v = n / 1_000_000
        return sign + (f"{int(v)}m" if v == int(v) else f"{v:.1f}m".rstrip("0").rstrip("."))
    if n >= 10_000:
        v = n / 1_000
        return sign + (f"{int(v)}k" if v == int(v) else f"{v:.1f}k".rstrip("0").rstrip("."))
    return sign + str(n)


def _parse_gp(s):
    """Parse '800m', '50k', '1.5b', '12345' -> int. Returns None if junk."""
    if s is None: return None
    s = str(s).strip().lower().replace(",", "").replace("gp", "").strip()
    if not s: return None
    mult = 1
    if s.endswith("k"): mult = 1_000;        s = s[:-1]
    elif s.endswith("m"): mult = 1_000_000;   s = s[:-1]
    elif s.endswith("b"): mult = 1_000_000_000; s = s[:-1]
    try:
        return int(round(float(s.strip()) * mult))
    except ValueError:
        return None


class GePricesFrame(ctk.CTkFrame):
    """View + edit live in-game GE prices for every item in the bot
    catalog. Reads via /admin/ge/prices?catalog=1, edits via
    /admin/ge/prices/bulk. Prices are persisted server-side via
    GrandExchange.savePrices() so they survive a restart.
    """
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        self.rows = []           # list of {id, name, price (int)}
        self.dirty = {}          # id -> new price (int) for unsaved edits

        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 5))
        ctk.CTkLabel(top, text="GE Prices",
                     font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        self.status_label = ctk.CTkLabel(top, text="—",
                                          font=ctk.CTkFont(size=12))
        self.status_label.pack(side="left", padx=20)

        actions = ctk.CTkFrame(self)
        actions.pack(fill="x", padx=20, pady=4)
        ctk.CTkButton(actions, text="Refresh", width=100,
                      command=self.refresh).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Save Edits", width=110, fg_color="#1b6e3a",
                      command=self._save_edits).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Push Catalog Defaults", width=170,
                      command=self._push_catalog,
                      fg_color="#a05522").pack(side="left", padx=4)
        ctk.CTkLabel(actions, text=" │ Filter:").pack(side="left", padx=(20, 4))
        self.filter_var = tk.StringVar(value="")
        ent = ctk.CTkEntry(actions, textvariable=self.filter_var, width=200,
                           placeholder_text="name or id substring")
        ent.pack(side="left", padx=4)
        ent.bind("<KeyRelease>", lambda _e: self._render())

        hint = ctk.CTkLabel(self,
            text="Edit a price (use 800m / 50k / 1.5b shorthand) and Save.\n"
                 "Push Catalog Defaults: copies BotTradeHandler.java prices "
                 "into the live GE.",
            font=ctk.CTkFont(size=11), justify="left", text_color="#888")
        hint.pack(fill="x", padx=20, pady=(0, 4))

        # Table
        tree_frame = ctk.CTkFrame(self)
        tree_frame.pack(fill="both", expand=True, padx=20, pady=10)
        cols = ("id", "name", "price", "fmt")
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Treeview", background="#2b2b2b", foreground="white",
                        fieldbackground="#2b2b2b", borderwidth=0, rowheight=26)
        style.configure("Treeview.Heading", background="#1f6aa5",
                        foreground="white", borderwidth=0)
        style.map("Treeview", background=[("selected", "#1f6aa5")])

        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings",
                                  selectmode="browse")
        widths = {"id": 70, "name": 280, "price": 130, "fmt": 90}
        for c in cols:
            self.tree.heading(c, text=c)
            self.tree.column(c, width=widths.get(c, 100), anchor="w")
        vsb = ttk.Scrollbar(tree_frame, orient="vertical",
                            command=self.tree.yview)
        self.tree.configure(yscroll=vsb.set)
        vsb.pack(side="right", fill="y")
        self.tree.pack(fill="both", expand=True)
        self.tree.bind("<Double-1>", self._edit_selected)

    def on_show(self):
        self.refresh()

    def refresh(self):
        def do():
            try:
                resp = self.api.ge_prices_catalog()
                self.after(0, lambda: self._apply(resp))
            except Exception as e:
                self.after(0, lambda: self.status_label.configure(
                    text=f"refresh failed: {e}", text_color="#cc3030"))
        threading.Thread(target=do, daemon=True).start()

    def _apply(self, resp):
        prices = (resp or {}).get("prices", {})
        names = (resp or {}).get("names", {})
        self.rows = []
        for k, v in prices.items():
            try: iid = int(k)
            except ValueError: continue
            self.rows.append({
                "id": iid,
                "name": names.get(k, "?"),
                "price": int(v) if v is not None else 0,
            })
        self.rows.sort(key=lambda r: r["id"])
        self.dirty.clear()
        self.status_label.configure(text=f"{len(self.rows)} items",
                                     text_color="#3fbf3f")
        self._render()

    def _render(self):
        for iid in self.tree.get_children():
            self.tree.delete(iid)
        flt = self.filter_var.get().strip().lower()
        for r in self.rows:
            if flt and flt not in r["name"].lower() and flt not in str(r["id"]):
                continue
            price = self.dirty.get(r["id"], r["price"])
            tag = ("dirty",) if r["id"] in self.dirty else ()
            self.tree.insert("", "end", iid=str(r["id"]), values=(
                r["id"], r["name"], price, _fmt_gp(price)), tags=tag)
        self.tree.tag_configure("dirty", background="#3a3520")

    def _edit_selected(self, _evt=None):
        sel = self.tree.selection()
        if not sel: return
        iid = int(sel[0])
        row = next((r for r in self.rows if r["id"] == iid), None)
        if not row: return
        cur = self.dirty.get(iid, row["price"])
        new = simpledialog.askstring(
            "Edit price",
            f"{row['name']} (id {iid})\n"
            f"Current: {_fmt_gp(row['price'])} ({row['price']:,}gp)\n\n"
            f"New price (e.g. 800m, 50k, 1.5b, 12345):",
            initialvalue=_fmt_gp(cur))
        if new is None: return
        parsed = _parse_gp(new)
        if parsed is None or parsed < 0:
            messagebox.showerror("Bad input",
                f"Couldn't parse '{new}'.\n"
                f"Use plain ints or k/m/b suffixes (e.g. 50k, 1.5m, 800m, 1b).")
            return
        if parsed > 2_147_483_647:
            messagebox.showerror("Too large",
                "Java int max is 2.147b. Pick a value <= 2b.")
            return
        if parsed == row["price"]:
            self.dirty.pop(iid, None)
        else:
            self.dirty[iid] = parsed
        self._render()

    def _save_edits(self):
        if not self.dirty:
            messagebox.showinfo("Nothing to save",
                "No edits pending. Double-click a row to edit a price.")
            return
        n = len(self.dirty)
        if not messagebox.askyesno("Confirm",
                f"Push {n} edited price{'s' if n != 1 else ''} to the live GE?"):
            return
        def do():
            try:
                resp = self.api.ge_prices_bulk_set(self.dirty)
                applied = resp.get("applied", "?")
                self.after(0, lambda: messagebox.showinfo(
                    "Saved", f"Server applied {applied} of {n} prices"))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Save failed", str(e)))
        threading.Thread(target=do, daemon=True).start()

    def _push_catalog(self):
        if not messagebox.askyesno("Push catalog",
                "Copy every BotTradeHandler.java StockEntry price into the "
                "live GE? This overwrites any current GE prices for those "
                "items with the catalog defaults."):
            return
        # Pulls ge_prices_catalog (which returns CURRENT live values, not
        # catalog values). To push catalog values we need the script.
        # Easier: call /admin/ge/prices/bulk with a special "from-catalog"
        # mode... but our endpoint doesn't have that. Use the existing
        # tool via subprocess instead.
        import subprocess
        def do():
            try:
                base = self.api.base
                token = self.api.cfg.get("token", "")
                cmd = [
                    "python3", "tools/ge_push_catalog.py",
                    "--server", base, "--token", token,
                ]
                proc = subprocess.run(cmd, capture_output=True, text=True,
                                       timeout=60)
                out = (proc.stdout + "\n" + proc.stderr).strip()
                self.after(0, lambda: messagebox.showinfo(
                    "Catalog pushed", out[-1500:]))
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Push failed", str(e)))
        threading.Thread(target=do, daemon=True).start()


# ----- Items tab (all items + tradeable + GE price) -----

# OSRS-style equip slot id -> short label.
EQUIP_SLOT_NAMES = {
    -1: "—", 0: "head", 1: "cape", 2: "neck", 3: "weapon",
    4: "chest", 5: "shield", 7: "legs", 9: "hands", 10: "feet",
    12: "ring", 13: "ammo", 14: "aura", 15: "pocket",
}


class ItemsFrame(ctk.CTkFrame):
    """Browse every item in the cache. Filter, search, set tradeable
    overrides, push GE prices in bulk. Server endpoint /admin/items/all
    pages 200 rows at a time so we never block the UI on a 25k-item dump."""

    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        self.rows = []          # current page rows from server
        self.total = 0
        self.page = 0
        self.page_size = 200
        self.selected_ids = set()
        # Filter state
        self.q_var          = tk.StringVar()
        self.wearable_var   = tk.BooleanVar(value=False)
        self.tradeable_var  = tk.BooleanVar(value=False)
        self.override_var   = tk.BooleanVar(value=False)

        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 10))
        ctk.CTkLabel(top, text="Items", font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        self.total_label = ctk.CTkLabel(top, text="—", font=ctk.CTkFont(size=12))
        self.total_label.pack(side="left", padx=20)

        # ---- filter row ----
        f = ctk.CTkFrame(self)
        f.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(f, text="Search:").pack(side="left", padx=4)
        e = ctk.CTkEntry(f, textvariable=self.q_var, width=260)
        e.pack(side="left", padx=4)
        e.bind("<Return>", lambda _: self._apply_filters())
        ctk.CTkCheckBox(f, text="Equipable only", variable=self.wearable_var,
                        command=self._apply_filters).pack(side="left", padx=8)
        ctk.CTkCheckBox(f, text="Tradeable only", variable=self.tradeable_var,
                        command=self._apply_filters).pack(side="left", padx=8)
        ctk.CTkCheckBox(f, text="Has override", variable=self.override_var,
                        command=self._apply_filters).pack(side="left", padx=8)
        ctk.CTkButton(f, text="Apply", width=70,
                      command=self._apply_filters).pack(side="left", padx=4)
        ctk.CTkButton(f, text="Refresh", width=80,
                      command=self.refresh).pack(side="right", padx=4)

        # ---- bulk action row ----
        a = ctk.CTkFrame(self)
        a.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(a, text="Selected:").pack(side="left", padx=4)
        self.sel_label = ctk.CTkLabel(a, text="0")
        self.sel_label.pack(side="left", padx=4)
        ctk.CTkButton(a, text="Make Tradeable", width=130, fg_color="#1b6e3a",
                      command=lambda: self._set_tradeable(True)).pack(side="left", padx=4)
        ctk.CTkButton(a, text="Make Untradeable", width=140, fg_color="#aa3030",
                      command=lambda: self._set_tradeable(False)).pack(side="left", padx=4)
        ctk.CTkButton(a, text="Clear Override", width=120,
                      command=self._clear_overrides).pack(side="left", padx=4)
        ctk.CTkLabel(a, text=" │ ").pack(side="left", padx=4)
        ctk.CTkButton(a, text="Set GE Price...", width=120, fg_color="#3a5588",
                      command=self._bulk_set_price).pack(side="left", padx=4)

        # ---- dye action row ----
        d = ctk.CTkFrame(self)
        d.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(d, text="Dyes:").pack(side="left", padx=4)
        ctk.CTkButton(d, text="Scan Cache", width=110,
                      command=self._dye_scan).pack(side="left", padx=4)
        ctk.CTkButton(d, text="Auto-populate Mappings", width=170, fg_color="#1b6e3a",
                      command=self._dye_autopopulate).pack(side="left", padx=4)
        ctk.CTkButton(d, text="Reload JSON", width=110,
                      command=self._dye_reload).pack(side="left", padx=4)
        ctk.CTkLabel(d, text="(scan = list dyes in cache; auto-populate = "
                     "build dye_recolors.json from name patterns)",
                     font=ctk.CTkFont(size=10), text_color="#888"
                     ).pack(side="left", padx=8)

        # ---- old/retro look scan row ----
        o = ctk.CTkFrame(self)
        o.pack(fill="x", padx=20, pady=4)
        ctk.CTkLabel(o, text="Retro Look:").pack(side="left", padx=4)
        ctk.CTkButton(o, text="Scan Cache", width=110,
                      command=self._oldlook_scan).pack(side="left", padx=4)
        ctk.CTkButton(o, text="Auto-build Swap Map", width=160, fg_color="#1b6e3a",
                      command=self._retro_autopopulate).pack(side="left", padx=4)
        ctk.CTkButton(o, text="Reload Map", width=110,
                      command=self._retro_reload).pack(side="left", padx=4)
        ctk.CTkLabel(o, text="(scan = list cache items; auto-build = "
                     "fill data/items/retro_swaps.json from name pairs)",
                     font=ctk.CTkFont(size=10), text_color="#888"
                     ).pack(side="left", padx=8)

        # ---- table (Tk Treeview - faster than custom widgets at 200 rows) ----
        from tkinter import ttk
        cols = ("id", "name", "slot", "wearable", "tradeable", "override", "price")
        wrap = ctk.CTkFrame(self)
        wrap.pack(fill="both", expand=True, padx=20, pady=4)
        self.tree = ttk.Treeview(wrap, columns=cols, show="headings",
                                 selectmode="extended", height=20)
        widths = {"id": 70, "name": 320, "slot": 80,
                  "wearable": 80, "tradeable": 80, "override": 100, "price": 100}
        for c in cols:
            self.tree.heading(c, text=c.title())
            self.tree.column(c, width=widths[c], anchor="w")
        sb = ttk.Scrollbar(wrap, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscrollcommand=sb.set)
        sb.pack(side="right", fill="y")
        self.tree.pack(side="left", fill="both", expand=True)
        self.tree.bind("<<TreeviewSelect>>", self._on_select)

        # ---- paging ----
        p = ctk.CTkFrame(self)
        p.pack(fill="x", padx=20, pady=4)
        ctk.CTkButton(p, text="◀ Prev", width=80,
                      command=self._prev_page).pack(side="left", padx=4)
        self.page_label = ctk.CTkLabel(p, text="page 1 / 1")
        self.page_label.pack(side="left", padx=8)
        ctk.CTkButton(p, text="Next ▶", width=80,
                      command=self._next_page).pack(side="left", padx=4)

    def on_show(self):
        if not self.rows:
            self.refresh()

    def _apply_filters(self):
        self.page = 0
        self.refresh()

    def refresh(self):
        def do():
            try:
                resp = self.api.items_all(
                    q=(self.q_var.get() or None),
                    wearable=self.wearable_var.get(),
                    tradeable=self.tradeable_var.get(),
                    override=self.override_var.get(),
                    page=self.page, page_size=self.page_size)
                self.after(0, lambda: self._apply_data(resp))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Items refresh failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _apply_data(self, resp):
        self.rows = resp.get("rows", []) if resp else []
        self.total = resp.get("total", 0) if resp else 0
        # Refresh selection set against new rows on the page.
        self.selected_ids = set()
        self.tree.delete(*self.tree.get_children())
        for r in self.rows:
            slot_label = EQUIP_SLOT_NAMES.get(r.get("slot", -1), str(r.get("slot")))
            ov = "—"
            if r.get("overrideT"): ov = "T"
            elif r.get("overrideU"): ov = "U"
            price = r.get("price", -1)
            price_str = "—" if price < 0 else f"{price:,}"
            self.tree.insert("", "end", iid=str(r["id"]), values=(
                r["id"], r.get("name", "?"), slot_label,
                "yes" if r.get("wearable") else "no",
                "yes" if r.get("tradeable") else "no",
                ov, price_str))
        max_pages = max(1, (self.total + self.page_size - 1) // self.page_size)
        self.page_label.configure(
            text=f"page {self.page + 1} / {max_pages}")
        self.total_label.configure(
            text=f"{self.total:,} matching items", text_color="#3fbf3f")
        self._on_select()

    def _on_select(self, _evt=None):
        ids = []
        for iid in self.tree.selection():
            try: ids.append(int(iid))
            except Exception: pass
        self.selected_ids = set(ids)
        self.sel_label.configure(text=str(len(ids)))

    def _prev_page(self):
        if self.page == 0: return
        self.page -= 1
        self.refresh()

    def _next_page(self):
        max_pages = max(1, (self.total + self.page_size - 1) // self.page_size)
        if self.page + 1 >= max_pages: return
        self.page += 1
        self.refresh()

    def _set_tradeable(self, t):
        if not self.selected_ids:
            messagebox.showinfo("No selection", "Select rows first.")
            return
        ids = sorted(self.selected_ids)
        def do():
            try:
                self.api.items_set_tradeable(ids, t)
                self.after(0, self.refresh)
                self.after(0, lambda: messagebox.showinfo("Done",
                    f"{len(ids)} item(s) -> "
                    + ("tradeable" if t else "untradeable")))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Tradeable update failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _clear_overrides(self):
        if not self.selected_ids:
            messagebox.showinfo("No selection", "Select rows first.")
            return
        ids = sorted(self.selected_ids)
        def do():
            try:
                self.api.items_clear_override(ids)
                self.after(0, self.refresh)
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Clear override failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _bulk_set_price(self):
        if not self.selected_ids:
            messagebox.showinfo("No selection", "Select rows first.")
            return
        ids = sorted(self.selected_ids)
        # Quick prompt - one price for all selected. For per-item edits,
        # use the GE Prices tab's bulk import.
        from tkinter import simpledialog
        v = simpledialog.askstring(
            "Set GE Price",
            f"GE price (gp) to apply to {len(ids)} item(s):",
            parent=self)
        if not v: return
        try:
            price = int(v.replace(",", "").replace("k", "000")
                         .replace("m", "000000").replace("b", "000000000"))
        except Exception:
            messagebox.showerror("Bad price", f"could not parse: {v}")
            return
        def do():
            try:
                self.api.ge_prices_bulk_set({i: price for i in ids})
                self.after(0, self.refresh)
                self.after(0, lambda: messagebox.showinfo(
                    "Done", f"set GE price for {len(ids)} item(s) to {price:,}"))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Set price failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _dye_scan(self):
        def do():
            try:
                resp = self.api.dyes_scan()
                dyes = resp.get("dyes", []) if resp else []
                tints = resp.get("tints", []) if resp else []
                # Build a readable summary window
                lines = [f"Found {len(dyes)} dye item(s) and {len(tints)} dyed-variant item(s).\n"]
                if dyes:
                    lines.append("=== DYES ===")
                    for d in dyes[:50]:
                        lines.append(f"  {d['id']:>6}  {d['name']}")
                    if len(dyes) > 50:
                        lines.append(f"  ... and {len(dyes) - 50} more")
                if tints:
                    lines.append("\n=== TINTED VARIANTS (first 50) ===")
                    for t in tints[:50]:
                        lines.append(f"  {t['id']:>6}  {t['name']}")
                    if len(tints) > 50:
                        lines.append(f"  ... and {len(tints) - 50} more")
                if not dyes and not tints:
                    lines.append("\nNo dyes / tinted items found in the cache.\n"
                                 "If your dyes have unusual names (not ending in "
                                 "' dye'), tell Claude what they're called.")
                self.after(0, lambda: self._show_text_window(
                    "Dye Cache Scan", "\n".join(lines)))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Scan failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _dye_autopopulate(self):
        if not messagebox.askyesno("Auto-populate Dye Mappings",
            "Walk the cache and auto-build data/items/dye_recolors.json from "
            "name patterns:\n\n"
            "  Pattern A: '<prefix> X' + 'X' present  ->  X dyes to '<prefix> X'\n"
            "  Pattern B: 'X (<prefix>)' + 'X' present  ->  X dyes to 'X (<prefix>)'\n\n"
            "OVERWRITES the existing JSON file. Continue?"):
            return
        def do():
            try:
                resp = self.api.dyes_autopopulate()
                added = resp.get("added", 0) if resp else 0
                dyes = resp.get("dyes", 0) if resp else 0
                self.after(0, lambda: messagebox.showinfo(
                    "Auto-populate done",
                    f"Wrote {dyes} dye(s) with {added} mapping(s) to "
                    f"data/items/dye_recolors.json.\n\n"
                    f"Dyes are live now (hot-reloaded). Test in-game by using "
                    f"a dye on a matching weapon."))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Auto-populate failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _dye_reload(self):
        def do():
            try:
                resp = self.api.dyes_reload()
                n = resp.get("dyes", 0) if resp else 0
                self.after(0, lambda: messagebox.showinfo(
                    "Reloaded", f"data/items/dye_recolors.json reloaded.\n"
                    f"{n} dye(s) registered."))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Reload failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _oldlook_scan(self):
        def do():
            try:
                resp = self.api.items_oldlook_scan(limit=2000)
                total   = resp.get("totalItems", 0)
                opcode  = resp.get("withOpcodeOldLook", 0)
                named   = resp.get("withNamePair", 0)
                rows    = resp.get("rows", []) or []
                lines = [
                    f"Cache total items scanned        : {total:,}",
                    f"Items with cache-opcode old-look : {opcode:,}",
                    f"Items with retro/replica name pair: {named:,}",
                    f"TOTAL retro/old-look candidates  : {opcode + named:,}",
                    "",
                    "Rows below: id [source] name -> baseId baseName",
                    "  source = 'opcode' (cache opcodes 242-251) /",
                    "           'retro'  (name starts with 'Retro ' or '(retro)') /",
                    "           'replica' (same for 'Replica ').",
                    "",
                ]
                for r in rows:
                    src = r.get("source", "?")
                    base = r.get("baseId", -1)
                    bn   = r.get("baseName", "")
                    base_str = f"-> {base:>6} {bn}" if base >= 0 else ""
                    lines.append(f"  {r['id']:>6}  [{src:<8}]  {r.get('name','?'):<35}  {base_str}")
                self.after(0, lambda: self._show_text_window(
                    "Old-Look Items Scan", "\n".join(lines)))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Old-look scan failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _retro_autopopulate(self):
        if not messagebox.askyesno("Auto-build Retro Swap Map",
            "Walk the cache + populate data/items/retro_swaps.json from "
            "every Retro <X> / Replica <X> name pair. Overwrites the "
            "existing file. Continue?"):
            return
        def do():
            try:
                resp = self.api.retro_autopopulate()
                added = resp.get("added", 0) if resp else 0
                self.after(0, lambda: messagebox.showinfo(
                    "Done", f"Wrote {added} swap mappings to "
                    f"data/items/retro_swaps.json. Live now (hot-reloaded).\n\n"
                    f"Toggle in-game via Oracle of Dawn -> Account & Character "
                    f"management -> Switch to retro items look."))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Auto-build failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _retro_reload(self):
        def do():
            try:
                resp = self.api.retro_reload()
                n = resp.get("swaps", 0) if resp else 0
                self.after(0, lambda: messagebox.showinfo(
                    "Reloaded", f"retro_swaps.json reloaded.\n{n} swap(s) registered."))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror(
                    "Reload failed", f"{type(e).__name__}: {e}"))
        threading.Thread(target=do, daemon=True).start()

    def _show_text_window(self, title, body):
        # Keep a strong reference on self so the popup isn't GC'd the
        # moment the function returns (the bug where the window
        # appeared then immediately closed). _open_popups also lets us
        # not lose track if the user has multiple open at once.
        if not hasattr(self, "_open_popups"):
            self._open_popups = []
        win = ctk.CTkToplevel(self)
        win.title(title)
        win.geometry("900x600")
        win.transient(self.winfo_toplevel())
        text = ctk.CTkTextbox(win, font=("Consolas", 11))
        text.pack(fill="both", expand=True, padx=10, pady=10)
        # Tk Text has a default insert limit of ~1MB - chunked for large bodies.
        for i in range(0, len(body), 50_000):
            text.insert("end", body[i:i + 50_000])
        text.configure(state="disabled")
        def close():
            try: self._open_popups.remove(win)
            except Exception: pass
            win.destroy()
        ctk.CTkButton(win, text="Close", command=close,
                      width=100).pack(pady=8)
        win.protocol("WM_DELETE_WINDOW", close)
        self._open_popups.append(win)
        # Force visibility - some WMs hide unparented popups behind the
        # main window if we don't lift + focus explicitly.
        win.after(50, lambda: (win.lift(), win.focus_force()))


# ----- Players tab -----

class PlayersFrame(ctk.CTkFrame):
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api

        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 5))
        ctk.CTkLabel(top, text="Online Players", font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        self.count_label = ctk.CTkLabel(top, text="—", font=ctk.CTkFont(size=12))
        self.count_label.pack(side="left", padx=20)
        ctk.CTkButton(top, text="Refresh", command=self.refresh, width=100).pack(side="right", padx=4)

        ctk.CTkLabel(self, text="Right-click a player for admin actions. Double-click to view stats.",
                     font=ctk.CTkFont(size=11)).pack(anchor="w", padx=20)

        tree_frame = ctk.CTkFrame(self)
        tree_frame.pack(fill="both", expand=True, padx=20, pady=10)

        cols = ("name", "rights", "donator", "extreme", "supporter", "muted", "combat", "total", "x", "y", "plane")
        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings", selectmode="browse")
        widths = (160, 70, 70, 70, 80, 60, 70, 60, 60, 60, 50)
        for c, w in zip(cols, widths):
            self.tree.heading(c, text=c.title(), anchor="w")
            self.tree.column(c, width=w, anchor="w")
        self.tree.pack(side="left", fill="both", expand=True)
        sb = ttk.Scrollbar(tree_frame, orient="vertical", command=self.tree.yview)
        sb.pack(side="right", fill="y")
        self.tree.configure(yscrollcommand=sb.set)

        self._build_menu()
        self.tree.bind("<Button-3>", self._on_right_click)
        self.tree.bind("<Double-1>", lambda e: self._action("view_stats"))
        self.players = []

    def _build_menu(self):
        m = tk.Menu(self, tearoff=0)
        m.add_command(label="View Stats", command=lambda: self._action("view_stats"))
        m.add_separator()

        rights_menu = tk.Menu(m, tearoff=0)
        rights_menu.add_command(label="Player (0)",  command=lambda: self._action("rights", 0))
        rights_menu.add_command(label="Mod (1)",     command=lambda: self._action("rights", 1))
        rights_menu.add_command(label="Admin (2)",   command=lambda: self._action("rights", 2))
        m.add_cascade(label="Set Rights", menu=rights_menu)

        donator_menu = tk.Menu(m, tearoff=0)
        donator_menu.add_command(label="Toggle Donator",         command=lambda: self._action("toggle_flag", "donator"))
        donator_menu.add_command(label="Toggle Extreme Donator", command=lambda: self._action("toggle_flag", "extreme"))
        donator_menu.add_command(label="Toggle Supporter",       command=lambda: self._action("toggle_flag", "supporter"))
        m.add_cascade(label="Donator Flags", menu=donator_menu)

        m.add_separator()
        m.add_command(label="Heal",                 command=lambda: self._action("heal"))
        m.add_command(label="Toggle Godmode",       command=lambda: self._action("toggle_flag", "invulnerable"))
        m.add_command(label="Give Item...",         command=lambda: self._action("give"))
        m.add_command(label="Teleport to Coords...",command=lambda: self._action("teleport"))
        m.add_separator()

        stats_menu = tk.Menu(m, tearoff=0)
        stats_menu.add_command(label="Max All Stats (99)",      command=lambda: self._action("max_stats"))
        stats_menu.add_command(label="Reset Stats (back to 3)", command=lambda: self._action("reset_stats"))
        stats_menu.add_command(label="Set One Skill...",        command=lambda: self._action("set_stat"))
        m.add_cascade(label="Stats", menu=stats_menu)

        m.add_separator()
        m.add_command(label="Toggle Mute", command=lambda: self._action("toggle_mute"))
        m.add_command(label="Kick (Disconnect)", command=lambda: self._action("kick"))
        self.menu = m

    def on_show(self): self.refresh()

    def refresh(self):
        def do():
            try:
                r = self.api.players()
                self.players = r.get("players", [])
                self.after(0, self._update)
                player_count = len(self.players)
                self.after(0, lambda: self.count_label.configure(text=f"{player_count} online"))
            except Exception as e:
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()

    def _update(self):
        self.tree.delete(*self.tree.get_children())
        for p in self.players:
            self.tree.insert("", "end", values=(
                p.get("name",""), rights_label(p.get("rights", 0)),
                "✓" if p.get("donator") else "", "✓" if p.get("extreme") else "",
                "✓" if p.get("supporter") else "", "✓" if p.get("muted") else "",
                p.get("combat",""), p.get("total",""),
                p.get("x",""), p.get("y",""), p.get("plane","")))

    def _on_right_click(self, e):
        row = self.tree.identify_row(e.y)
        if row:
            self.tree.selection_set(row)
            self.menu.post(e.x_root, e.y_root)

    def _selected_player(self):
        sel = self.tree.selection()
        if not sel: return None
        name = self.tree.item(sel[0], "values")[0]
        return next((p for p in self.players if p.get("name") == name), None)

    def _action(self, kind, *args):
        p = self._selected_player()
        if not p: return
        name = p.get("name")

        def call(fn, refresh=True, success=None):
            def do():
                try:
                    fn()
                    if success: 
                        success_msg = success
                        self.after(0, lambda: messagebox.showinfo("Done", success_msg))
                    if refresh: self.after(0, self.refresh)
                except Exception as e:
                    error_msg = str(e)
                    self.after(0, lambda: messagebox.showerror("Error", error_msg))
            threading.Thread(target=do, daemon=True).start()

        if kind == "view_stats":
            def do():
                try:
                    r = self.api.player_inspect(name)
                    self.after(0, lambda: StatsWindow(self, r))
                except Exception as e:
                    error_msg = str(e)
                    self.after(0, lambda: messagebox.showerror("Error", error_msg))
            threading.Thread(target=do, daemon=True).start()

        elif kind == "heal":
            call(lambda: self.api.player_heal(name), success=f"Healed {name}")
        elif kind == "kick":
            if messagebox.askyesno("Kick", f"Disconnect {name}?"):
                call(lambda: self.api.player_kick(name), success=f"Kicked {name}")
        elif kind == "rights":
            level = args[0]; label = rights_label(level)
            if messagebox.askyesno("Set rights", f"Set {name} to {label} ({level})?"):
                call(lambda: self.api.player_rights(name, level), success=f"Rights → {label}")
        elif kind == "toggle_flag":
            flag = args[0]
            curval = bool(p.get(flag))
            label = {"donator":"Donator", "extreme":"Extreme Donator",
                     "supporter":"Supporter", "invulnerable":"Godmode"}[flag]
            new = not curval
            if messagebox.askyesno("Toggle flag", f"{name}: set {label} to {'ON' if new else 'OFF'}?"):
                call(lambda: self.api.player_flag(name, flag, new), success=f"{label}: {'ON' if new else 'OFF'}")
        elif kind == "toggle_mute":
            new = not bool(p.get("muted"))
            if messagebox.askyesno("Toggle mute", f"{name}: {'MUTE' if new else 'UNMUTE'}?"):
                call(lambda: self.api.player_mute(name, new), success=f"Muted: {new}")
        elif kind == "max_stats":
            if messagebox.askyesno("Max stats", f"Set ALL stats to 99 on {name}?"):
                call(lambda: self.api.player_maxstats(name), success="All stats → 99")
        elif kind == "reset_stats":
            if messagebox.askyesno("Reset stats", f"Reset {name} stats to level 3 starter?"):
                call(lambda: self.api.player_resetstats(name), success="Stats reset")
        elif kind == "set_stat":
            dlg = SkillLevelDialog(self, name)
            self.wait_window(dlg)
            if dlg.result:
                skill, level = dlg.result
                call(lambda: self.api.player_setstat(name, skill, level), success=f"{skill} → {level}")
        elif kind == "give":
            dlg = SimpleInputDialog(self, "Give item", ["Item ID:", "Amount:"])
            self.wait_window(dlg)
            if dlg.result:
                try:
                    item_id = int(dlg.result[0]); amount = int(dlg.result[1])
                    call(lambda: self.api.player_give(name, item_id, amount), success=f"Gave {amount}x item {item_id}")
                except ValueError: messagebox.showerror("Error", "Numbers only")
        elif kind == "teleport":
            dlg = SimpleInputDialog(self, "Teleport to coords", ["X:", "Y:", "Plane:"])
            self.wait_window(dlg)
            if dlg.result:
                try:
                    x = int(dlg.result[0]); y = int(dlg.result[1]); plane = int(dlg.result[2])
                    call(lambda: self.api.player_teleport(name, x, y, plane), success="Teleported")
                except ValueError: messagebox.showerror("Error", "Numbers only")


# ----- Server / Backups / Log / Settings -----

class ServerFrame(ctk.CTkFrame):
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        ctk.CTkLabel(self, text="Server Control", font=ctk.CTkFont(size=22, weight="bold")).pack(anchor="w", padx=20, pady=(20, 10))
        ctk.CTkButton(self, text="Save All Now", height=40, command=lambda: self._action(self.api.server_save, "Save")).pack(fill="x", padx=20, pady=5)
        ctk.CTkButton(self, text="Reload Shop Definitions", height=40, command=lambda: self._action(self.api.server_reload, "Reload")).pack(fill="x", padx=20, pady=5)

        bcast = ctk.CTkFrame(self)
        bcast.pack(fill="x", padx=20, pady=10)
        ctk.CTkLabel(bcast, text="Broadcast Message").pack(anchor="w", padx=10, pady=(10, 0))
        self.bcast_entry = ctk.CTkEntry(bcast, placeholder_text="message...")
        self.bcast_entry.pack(fill="x", padx=10, pady=5)
        ctk.CTkButton(bcast, text="Send Broadcast", command=self._send_bcast).pack(padx=10, pady=(5, 10))

        restart = ctk.CTkFrame(self)
        restart.pack(fill="x", padx=20, pady=10)
        ctk.CTkLabel(restart, text="Restart Server").pack(anchor="w", padx=10, pady=(10, 0))
        ctk.CTkLabel(restart, text="Delay (seconds):").pack(anchor="w", padx=10)
        self.restart_delay = ctk.CTkEntry(restart, placeholder_text="60")
        self.restart_delay.insert(0, "60")
        self.restart_delay.pack(fill="x", padx=10, pady=5)
        ctk.CTkButton(restart, text="Restart Server (DESTRUCTIVE)", fg_color="#aa3030", command=self._restart).pack(padx=10, pady=(5, 10))

    def _action(self, fn, label):
        def do():
            try: 
                r = fn()
                result_text = json.dumps(r, indent=2)
                self.after(0, lambda: messagebox.showinfo(label, result_text))
            except Exception as e: 
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()

    def _send_bcast(self):
        msg = self.bcast_entry.get().strip()
        if not msg: return
        def do():
            try:
                self.api.server_broadcast(msg)
                self.after(0, lambda: self.bcast_entry.delete(0, "end"))
            except Exception as e: 
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()

    def _restart(self):
        try: delay = int(self.restart_delay.get())
        except ValueError: return messagebox.showerror("Error", "Delay must be a number")
        if not messagebox.askyesno("Restart", f"Really restart server in {delay} seconds?"): return
        def do():
            try: self.api.server_restart(delay)
            except Exception as e: 
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()


class BackupsFrame(ctk.CTkFrame):
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 10))
        ctk.CTkLabel(top, text="Backups", font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        actions = ctk.CTkFrame(self)
        actions.pack(fill="x", padx=20, pady=5)
        ctk.CTkButton(actions, text="Refresh", command=self.refresh).pack(side="left", padx=4)
        ctk.CTkButton(actions, text="Take Snapshot Now", command=self._take).pack(side="left", padx=4)
        tree_frame = ctk.CTkFrame(self)
        tree_frame.pack(fill="both", expand=True, padx=20, pady=10)
        cols = ("name", "size", "modified")
        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings", selectmode="browse")
        for c, w in zip(cols, (400, 100, 200)):
            self.tree.heading(c, text=c.title(), anchor="w")
            self.tree.column(c, width=w, anchor="w")
        self.tree.pack(side="left", fill="both", expand=True)
        sb = ttk.Scrollbar(tree_frame, orient="vertical", command=self.tree.yview)
        sb.pack(side="right", fill="y")
        self.tree.configure(yscrollcommand=sb.set)

    def on_show(self): self.refresh()

    def refresh(self):
        def do():
            try:
                r = self.api.snapshots()
                snapshots = r.get("snapshots", [])
                self.after(0, lambda: self._update(snapshots))
            except Exception as e: 
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()

    def _update(self, snaps):
        self.tree.delete(*self.tree.get_children())
        for s in snaps:
            self.tree.insert("", "end", values=(s.get("name",""), fmt_size(s.get("size_bytes", 0)), fmt_time(s.get("modified_ms", 0))))

    def _take(self):
        def do():
            try:
                self.api.snapshot_take(); time.sleep(2)
                self.after(0, self.refresh)
            except Exception as e: 
                error_msg = str(e)
                self.after(0, lambda: messagebox.showerror("Error", error_msg))
        threading.Thread(target=do, daemon=True).start()


class LogFrame(ctk.CTkFrame):
    def __init__(self, master, api):
        super().__init__(master)
        self.api = api
        top = ctk.CTkFrame(self, fg_color="transparent")
        top.pack(fill="x", padx=20, pady=(20, 10))
        ctk.CTkLabel(top, text="Live Log", font=ctk.CTkFont(size=22, weight="bold")).pack(side="left")
        ctk.CTkButton(top, text="Refresh", command=self.refresh).pack(side="right", padx=4)
        self.auto_var = tk.BooleanVar(value=True)
        ctk.CTkCheckBox(top, text="Auto-refresh (2s)", variable=self.auto_var, command=self._toggle_auto).pack(side="right", padx=4)
        self.text = ctk.CTkTextbox(self, font=("Consolas", 11))
        self.text.pack(fill="both", expand=True, padx=20, pady=10)
        self._poll_id = None

    def on_show(self):
        self.refresh()
        if self.auto_var.get() and self._poll_id is None: self._auto()

    def _toggle_auto(self):
        if self.auto_var.get() and self._poll_id is None: self._auto()

    def _auto(self):
        if not self.auto_var.get(): self._poll_id = None; return
        self.refresh()
        self._poll_id = self.after(2000, self._auto)

    def refresh(self):
        def do():
            try:
                r = self.api.log_tail(300)
                lines = r.get("lines", [])
                self.after(0, lambda: self._update(lines))
            except Exception as e: 
                error_msg = f"<error: {e}>"
                self.after(0, lambda: self._update([error_msg]))
        threading.Thread(target=do, daemon=True).start()

    def _update(self, lines):
        self.text.delete("1.0", "end")
        self.text.insert("1.0", "\n".join(lines))
        self.text.see("end")


class SettingsFrame(ctk.CTkFrame):
    def __init__(self, master, api, on_save):
        super().__init__(master)
        self.api = api
        self.on_save = on_save
        ctk.CTkLabel(self, text="Settings", font=ctk.CTkFont(size=22, weight="bold")).pack(anchor="w", padx=20, pady=(20, 10))
        form = ctk.CTkFrame(self)
        form.pack(fill="x", padx=20, pady=10)
        ctk.CTkLabel(form, text="Server Host").pack(anchor="w", padx=10, pady=(10, 0))
        self.host_entry = ctk.CTkEntry(form, placeholder_text="107.202.173.6")
        self.host_entry.insert(0, api.cfg.get("host", ""))
        self.host_entry.pack(fill="x", padx=10, pady=5)
        ctk.CTkLabel(form, text="Port").pack(anchor="w", padx=10, pady=(10, 0))
        self.port_entry = ctk.CTkEntry(form, placeholder_text="8090")
        self.port_entry.insert(0, str(api.cfg.get("port", 8090)))
        self.port_entry.pack(fill="x", padx=10, pady=5)
        ctk.CTkLabel(form, text="Auth Token").pack(anchor="w", padx=10, pady=(10, 0))
        self.token_entry = ctk.CTkEntry(form, placeholder_text="paste token here")
        self.token_entry.insert(0, api.cfg.get("token", ""))
        self.token_entry.pack(fill="x", padx=10, pady=5)
        btns = ctk.CTkFrame(self)
        btns.pack(fill="x", padx=20, pady=10)
        ctk.CTkButton(btns, text="Save", command=self._save).pack(side="left", padx=4)
        ctk.CTkButton(btns, text="Test Connection", command=self._test).pack(side="left", padx=4)
        self.status = ctk.CTkLabel(self, text="")
        self.status.pack(padx=20, pady=10)

    def _save(self):
        try:
            self.api.update(self.host_entry.get().strip(), self.port_entry.get().strip(), self.token_entry.get().strip())
            self.on_save()
            self.status.configure(text="Saved.", text_color="#3fbf3f")
        except Exception as e: 
            error_msg = str(e)
            self.status.configure(text=f"Error: {error_msg}", text_color="#cc3030")

    def _test(self):
        host = self.host_entry.get().strip(); port = self.port_entry.get().strip(); token = self.token_entry.get().strip()
        def do():
            try:
                r = requests.get(f"http://{host}:{port}/admin/ping", headers={"Authorization": f"Bearer {token}"}, timeout=3)
                if r.status_code == 200: 
                    self.after(0, lambda: self.status.configure(text="Connected ✓", text_color="#3fbf3f"))
                else: 
                    status_msg = f"HTTP {r.status_code}"
                    self.after(0, lambda: self.status.configure(text=status_msg, text_color="#cc3030"))
            except Exception as e: 
                error_msg = str(e)
                self.after(0, lambda: self.status.configure(text=f"Failed: {error_msg}", text_color="#cc3030"))
        threading.Thread(target=do, daemon=True).start()


# ----- Helper dialogs -----

class SimpleInputDialog(ctk.CTkToplevel):
    def __init__(self, master, title, labels):
        super().__init__(master)
        self.title(title)
        self.geometry("400x{}".format(80 + 60 * len(labels)))
        self.result = None
        self.entries = []
        for i, label in enumerate(labels):
            ctk.CTkLabel(self, text=label).pack(anchor="w", padx=20, pady=(15 if i == 0 else 5, 0))
            e = ctk.CTkEntry(self)
            e.pack(fill="x", padx=20, pady=2)
            self.entries.append(e)
        btns = ctk.CTkFrame(self)
        btns.pack(fill="x", padx=20, pady=10)
        ctk.CTkButton(btns, text="OK", command=self._ok).pack(side="left", padx=4)
        ctk.CTkButton(btns, text="Cancel", command=self.destroy, fg_color="#666").pack(side="left", padx=4)
        self.entries[0].focus()
        self.transient(master); self.grab_set()

    def _ok(self):
        self.result = [e.get().strip() for e in self.entries]
        self.destroy()


class SkillLevelDialog(ctk.CTkToplevel):
    def __init__(self, master, player_name):
        super().__init__(master)
        self.title(f"Set skill - {player_name}")
        self.geometry("360x220")
        self.result = None
        ctk.CTkLabel(self, text="Skill:").pack(anchor="w", padx=20, pady=(20, 0))
        self.skill_var = tk.StringVar(value=SKILLS[0])
        ctk.CTkOptionMenu(self, variable=self.skill_var, values=SKILLS).pack(fill="x", padx=20, pady=2)
        ctk.CTkLabel(self, text="Level (1-99):").pack(anchor="w", padx=20, pady=(10, 0))
        self.level_entry = ctk.CTkEntry(self)
        self.level_entry.insert(0, "99")
        self.level_entry.pack(fill="x", padx=20, pady=2)
        btns = ctk.CTkFrame(self)
        btns.pack(fill="x", padx=20, pady=15)
        ctk.CTkButton(btns, text="OK", command=self._ok).pack(side="left", padx=4)
        ctk.CTkButton(btns, text="Cancel", command=self.destroy, fg_color="#666").pack(side="left", padx=4)
        self.transient(master); self.grab_set()

    def _ok(self):
        try:
            level = int(self.level_entry.get())
            if level < 1 or level > 99:
                messagebox.showerror("Error", "Level must be 1-99"); return
            self.result = (self.skill_var.get(), level)
            self.destroy()
        except ValueError:
            messagebox.showerror("Error", "Level must be a number")


class GenerateDialog(ctk.CTkToplevel):
    """Generate bots dialog with mode picker + archetype picker + level slider."""
    MODES = [
        ("Default (level 3 starter)", "default"),
        ("Set combat level",          "set"),
        ("Random combat level",       "random_combat"),
        ("Random skills",             "random_skills"),
    ]

    ARCHETYPES = [
        ("Random ANY archetype",    "random_any"),
        ("Random combat archetype", "random"),
        ("Melee",                   "melee"),
        ("Ranged",                  "ranged"),
        ("Magic",                   "magic"),
        ("Hybrid",                  "hybrid"),
        ("Tank",                    "tank"),
        ("Pure (1 def)",            "pure"),
        ("Main (balanced)",         "main"),
        ("Skiller (combat 3)",      "skiller"),
        ("F2P only",                "f2p"),
        ("Maxed (all 99)",          "maxed"),
    ]

    def __init__(self, master, default_count):
        super().__init__(master)
        self.title("Generate bots")
        self.geometry("480x640")
        self.result = None

        ctk.CTkLabel(self, text="How many bots:").pack(anchor="w", padx=20, pady=(20, 0))
        self.count_var = tk.StringVar(value=str(default_count))
        ctk.CTkEntry(self, textvariable=self.count_var).pack(fill="x", padx=20, pady=2)

        ctk.CTkLabel(self, text="Mode:").pack(anchor="w", padx=20, pady=(15, 0))
        self.mode_var = tk.StringVar(value="default")
        for label, val in self.MODES:
            ctk.CTkRadioButton(self, text=label, variable=self.mode_var, value=val,
                               command=self._update_state).pack(anchor="w", padx=30, pady=2)

        ctk.CTkLabel(self, text="Archetype:").pack(anchor="w", padx=20, pady=(15, 0))
        self.arch_var = tk.StringVar(value="random")
        labels = [a[0] for a in self.ARCHETYPES]
        self.arch_menu = ctk.CTkOptionMenu(self, values=labels, command=self._on_arch_change)
        self.arch_menu.set(self.ARCHETYPES[0][0])
        self.arch_menu.pack(fill="x", padx=20, pady=2)

        self.level_label = ctk.CTkLabel(self, text="Combat level: 3")
        self.level_label.pack(anchor="w", padx=20, pady=(15, 0))
        self.level_var = tk.IntVar(value=3)
        self.level_slider = ctk.CTkSlider(self, from_=3, to=138, number_of_steps=135,
                                          command=self._on_level_change)
        self.level_slider.set(3)
        self.level_slider.pack(fill="x", padx=20, pady=2)

        btns = ctk.CTkFrame(self)
        btns.pack(fill="x", padx=20, pady=20)
        ctk.CTkButton(btns, text="Generate", command=self._ok, width=120).pack(side="left", padx=4)
        ctk.CTkButton(btns, text="Cancel", command=self.destroy, fg_color="#666", width=120).pack(side="left", padx=4)

        self._update_state()
        self.transient(master); self.grab_set()

    def _on_arch_change(self, label):
        for l, v in self.ARCHETYPES:
            if l == label:
                self.arch_var.set(v); break
        self._update_state()

    def _on_level_change(self, val):
        n = int(round(val))
        self.level_var.set(n)
        if self.mode_var.get() == "set":
            self.level_label.configure(text=f"Combat level: {n}")

    def _update_state(self):
        mode = self.mode_var.get()
        arch = self.arch_var.get()

        if mode in ("set", "random_combat"):
            self.arch_menu.configure(state="normal")
        else:
            self.arch_menu.configure(state="disabled")

        slider_active = (mode == "set" and arch not in ("skiller", "maxed"))
        if slider_active:
            self.level_slider.configure(state="normal")
            self.level_label.configure(text=f"Combat level: {self.level_var.get()}")
        else:
            self.level_slider.configure(state="disabled")
            if arch == "skiller": self.level_label.configure(text="Combat level: (skiller — combat 3)")
            elif arch == "maxed": self.level_label.configure(text="Combat level: (maxed — combat 138)")
            elif mode == "random_combat": self.level_label.configure(text="Combat level: (random)")
            else: self.level_label.configure(text="Combat level: (n/a)")

    def _ok(self):
        try:
            count = int(self.count_var.get())
            if count < 1 or count > 500:
                messagebox.showerror("Error", "Count must be 1-500"); return
        except ValueError:
            messagebox.showerror("Error", "Count must be a number"); return
        mode = self.mode_var.get()
        level = self.level_var.get()
        archetype = self.arch_var.get()
        self.result = (count, mode, level, archetype)
        self.destroy()


class StatsWindow(ctk.CTkToplevel):
    """Popup showing the full skill array of a player or bot."""
    def __init__(self, master, inspect_data):
        super().__init__(master)
        name = inspect_data.get("name", "?")
        self.title(f"Stats: {name}")
        self.geometry("420x600")

        header = ctk.CTkFrame(self)
        header.pack(fill="x", padx=15, pady=10)
        ctk.CTkLabel(header, text=name, font=ctk.CTkFont(size=18, weight="bold")).pack(anchor="w")
        ctk.CTkLabel(header,
                     text=f"Combat: {inspect_data.get('combat','?')}    Total: {inspect_data.get('total_level','?')}",
                     font=ctk.CTkFont(size=12)).pack(anchor="w")
        ctk.CTkLabel(header,
                     text=f"Position: ({inspect_data.get('x','?')}, {inspect_data.get('y','?')}, plane {inspect_data.get('plane','?')})",
                     font=ctk.CTkFont(size=11)).pack(anchor="w")

        skills = inspect_data.get("skills", {})
        scroll = ctk.CTkScrollableFrame(self)
        scroll.pack(fill="both", expand=True, padx=15, pady=10)
        for skill_name, level in skills.items():
            row = ctk.CTkFrame(scroll, fg_color="transparent")
            row.pack(fill="x", pady=1)
            ctk.CTkLabel(row, text=skill_name, font=ctk.CTkFont(size=12), width=140, anchor="w").pack(side="left")
            ctk.CTkLabel(row, text=str(level), font=ctk.CTkFont(size=12, weight="bold"),
                         text_color="#ffaa20" if level >= 99 else "#ffffff").pack(side="left")

        ctk.CTkButton(self, text="Close", command=self.destroy).pack(pady=10)
        self.transient(master); self.grab_set()


# ----- Entrypoint -----

if __name__ == "__main__":
    App().mainloop()