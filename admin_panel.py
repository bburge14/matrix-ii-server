import json
import os
import threading
import time
import tkinter as tk
from datetime import datetime
from pathlib import Path

import customtkinter as ctk
import requests
from tkinter import messagebox, ttk

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
        r.raise_for_status()
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
                     font=ctk.CTkFont(size=18, weight="bold")).pack(pady=20)

        self.tabs = {}
        for name in ("Dashboard", "Bots", "Bot AI", "Players", "Server", "Backups", "Log", "Settings"):
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

        tree_frame = ctk.CTkFrame(self)
        tree_frame.pack(fill="both", expand=True, padx=20, pady=10)

        cols = ("name", "location", "state", "goal", "personality", "emotions")
        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings", selectmode="browse")
        widths = (150, 120, 100, 180, 150, 180)
        for c, w in zip(cols, widths):
            self.tree.heading(c, text=c.title(), anchor="w")
            self.tree.column(c, width=w, anchor="w")
        self.tree.pack(side="left", fill="both", expand=True)

        sb = ttk.Scrollbar(tree_frame, orient="vertical", command=self.tree.yview)
        sb.pack(side="right", fill="y")
        self.tree.configure(yscrollcommand=sb.set)
        
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

    def _update(self, bots):
        self.tree.delete(*self.tree.get_children())
        for bot in bots:
            self.tree.insert("", "end", values=(
                bot.get("name", ""),
                bot.get("location", ""),
                bot.get("state", "UNKNOWN"),
                bot.get("goal", "None"),
                bot.get("personality", "Unknown"),
                bot.get("emotions", "Unknown")
            ))

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