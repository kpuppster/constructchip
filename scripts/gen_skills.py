"""Generates the built-in skill tree JSON + lang entries. Run: python3 scripts/gen_skills.py
Edit the SKILLS table, not the generated JSON."""
import json, os, sys

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "data", "constructchip", "constructchip", "skill")
LANG = os.path.join(ROOT, "assets", "constructchip", "lang", "en_us.json")
VISTA = [{"type": "neoforge:mod_loaded", "modid": "vista"}]

# id, branch, tier, cost, requires, column, icon, quickhack, modifiers, name, description, conditions
SKILLS = [
    # CORE: the spine
    ("scanner", "core", 1, 0, [], 3, "minecraft:spyglass", "scanner", [],
     "Scanner", "Look at a block or creature to read it: name, required tool, health, armor, and how hard the host would hit it. Free and always unlocked.", None),
    ("overclock", "core", 2, 2, ["scanner"], 3, "minecraft:blaze_powder", "overclock", [],
     "Overclock", "30 s of healing, more damage and slightly less damage taken for the host, and half-price quickhacks. Afterwards every chip ability locks for 60 s.", None),
    ("neural_overdrive", "core", 3, 3, ["overclock"], 3, "minecraft:nether_star", None, [("lockout_ticks", 600), ("no_cooldowns_while_overclocked", 1)],
     "Neural Overdrive", "While Overclocked, quickhacks have no cooldowns. The lockout afterwards lasts 30 s longer.", None),

    # INTRUSION: offence against creatures and cyberware
    ("deep_scan", "intrusion", 1, 1, ["scanner"], 2, "minecraft:ender_eye", None, [("scan_range", 8)],
     "Deep Scan", "The Scanner also shows status effects, equipment and installed cyberware, and reaches 8 blocks further.", None),
    ("ping", "intrusion", 1, 1, ["scanner"], 1, "minecraft:bell", "ping", [],
     "Ping", "Quickhack: hostile creatures within 24 blocks glow through walls for you and your host for 10 s, and you hear how many are on each side.", None),
    ("short_circuit", "intrusion", 2, 2, ["deep_scan"], 2, "minecraft:redstone", "short_circuit", [],
     "Short Circuit", "Quickhack: disable a target's cyberware for 10 s.", None),
    ("weak_point", "intrusion", 2, 2, ["ping"], 1, "minecraft:target", "weak_point", [],
     "Weak Point", "Quickhack: mark a creature. It takes 15% more damage from the host for 10 s.", None),
    ("system_lock", "intrusion", 3, 3, ["short_circuit"], 2, "minecraft:iron_bars", "system_lock", [],
     "System Lock", "Quickhack: cut a target's active cyberware effects. It can't be turned back on for 30 s.", None),

    ("red_lightning", "intrusion", 2, 2, ["ping"], 0, "minecraft:redstone_block", "red_lightning", [],
     "Red Lightning", "Quickhack: a red arc jumps from your host to a creature and fries it for 6 damage.", None),
    ("lightning_storm", "intrusion", 3, 3, ["red_lightning"], 0, "minecraft:lightning_rod", "lightning_storm", [],
     "Lightning Storm", "Quickhack: red arcs hit every hostile creature within 12 blocks of your host for 5 damage.", None),

    # SUPPORT: keep the host alive
    ("vitals_link", "support", 1, 1, ["scanner"], 4, "minecraft:golden_apple", None, [],
     "Vitals Link", "The host HUD also shows the host's status effects and warns about gear that is about to break.", None),
    ("threat_alert", "support", 1, 1, ["scanner"], 5, "minecraft:goat_horn", None, [],
     "Threat Alert", "When a creature starts targeting the host, the host hears a warning and sees which way it is.", None),
    ("adrenal_surge", "support", 2, 2, ["vitals_link"], 4, "minecraft:sugar", "adrenal_surge", [],
     "Adrenal Surge", "Quickhack: Regeneration I and Speed I on the host for 6 s.", None),
    ("pain_editor", "support", 2, 2, ["threat_alert"], 5, "minecraft:shield", None, [("host_damage_taken", -0.1)],
     "Pain Editor", "While the chip is installed, the host takes 10% less damage.", None),
    ("sustained_overclock", "support", 3, 3, ["adrenal_surge", "overclock"], 4, "minecraft:magma_cream", None,
     [("overclock_ticks", 200), ("lockout_ticks", -400)],
     "Sustained Overclock", "Overclock lasts 10 s longer and the lockout afterwards is 20 s shorter.", None),

    ("patch_job", "support", 3, 3, ["pain_editor"], 5, "minecraft:enchanted_golden_apple", "patch_job", [],
     "Patch Job", "Quickhack: heals your host for 10, gives 4 extra hearts for 30 s, and cures poison, wither and fire.", None),

    # SUPPORT / chrome: working with the host's cyberware
    ("chrome_link", "support", 1, 1, ["scanner"], 10, "minecraft:chain", None, [],
     "Chrome Link", "Your HUD shows your host's cyberware load and cyberpsychosis.", None),
    ("neural_buffer", "support", 2, 2, ["chrome_link"], 10, "minecraft:soul_lantern", None, [],
     "Neural Buffer", "You steady your host's mind: their cyberware pressure fades faster and cyberpsychosis grows 25% slower.", None),
    ("chrome_override", "support", 3, 3, ["neural_buffer"], 10, "minecraft:echo_shard", "chrome_override", [],
     "Chrome Override", "Quickhack: fire your host's Sandevistan for them, or their Blood Pump if the Sandevistan isn't ready.", None),

    # SYSTEMS: RAM and devices
    ("ram_expansion", "systems", 1, 1, ["scanner"], 6, "minecraft:amethyst_shard", None, [("max_ram", 2)],
     "RAM Expansion", "+2 max RAM.", None),
    ("remote_switch", "systems", 1, 1, ["scanner"], 8, "minecraft:lever", "remote_switch", [],
     "Remote Switch", "Quickhack: flip a lever, door, trapdoor, fence gate or button in sight, as if the host used it.", None),
    ("ram_expansion_2", "systems", 2, 2, ["ram_expansion"], 6, "minecraft:amethyst_cluster", None, [("max_ram", 2)],
     "RAM Expansion II", "+2 max RAM.", None),
    ("efficient_code", "systems", 2, 2, ["ram_expansion"], 7, "minecraft:comparator", None,
     [("ram_regen", 0.25), ("ram_cost", -0.1)],
     "Efficient Code", "+0.25 RAM per second, and quickhacks cost 10% less.", None),
    ("signal_inject", "systems", 2, 2, ["remote_switch"], 8, "minecraft:redstone_torch", "signal_inject", [],
     "Signal Inject", "Quickhack: power a block in sight with redstone for 1 s, like pressing a button on it.", None),
    ("ram_expansion_3", "systems", 3, 3, ["ram_expansion_2"], 6, "minecraft:budding_amethyst", None, [("max_ram", 4)],
     "RAM Expansion III", "+4 max RAM.", None),

    # SYSTEMS / Vista compat (only present when Vista is installed)
    ("tv_power", "systems", 1, 1, ["remote_switch"], 9, "minecraft:observer", "tv_power", [],
     "TV Override", "Quickhack: turn a Vista TV on or off.", VISTA),
    ("camera_view", "systems", 2, 2, ["tv_power"], 9, "minecraft:spyglass", "camera_view", [],
     "Camera Jack", "Quickhack: see through a Vista View Finder.", VISTA),
    ("camera_blind", "systems", 3, 3, ["camera_view"], 9, "minecraft:ink_sac", "camera_blind", [],
     "Camera Blind", "Quickhack: cut a View Finder's feed for 20 s. Its TVs show static.", VISTA),
]

def main():
    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):
        if f.endswith(".json"): os.remove(os.path.join(OUT, f))
    ids = {s[0] for s in SKILLS}
    with open(LANG) as fh: lang = json.load(fh)
    lang = {k: v for k, v in lang.items() if not k.startswith("skill.constructchip.")}
    for (sid, branch, tier, cost, req, col, icon, hack, mods, name, desc, cond) in SKILLS:
        for r in req: assert r in ids, f"{sid} requires unknown {r}"
        data = {}
        if cond: data["neoforge:conditions"] = cond
        data.update({
            "name": {"translate": f"skill.constructchip.{sid}"},
            "description": {"translate": f"skill.constructchip.{sid}.desc"},
            "branch": branch, "tier": tier, "cost": cost,
            "requires": [f"constructchip:{r}" for r in req],
            "icon": icon, "column": col,
        })
        if hack: data["quickhack"] = f"constructchip:{hack}"
        if mods: data["modifiers"] = [{"type": t, "amount": a} for t, a in mods]
        with open(os.path.join(OUT, sid + ".json"), "w") as fh:
            json.dump(data, fh, indent=2); fh.write("\n")
        lang[f"skill.constructchip.{sid}"] = name
        lang[f"skill.constructchip.{sid}.desc"] = desc
    with open(LANG, "w") as fh:
        json.dump(lang, fh, indent=2, ensure_ascii=False); fh.write("\n")
    print(f"wrote {len(SKILLS)} skills")

if __name__ == "__main__":
    main()
