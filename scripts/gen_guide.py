"""Builds the in-game guide (assets/constructchip/guide/index.json + lang) from Project: Edgerunner's guide text
(A24-N22, used with permission), adjusted to how this mod works. Run: python3 -I scripts/gen_guide.py"""
import json, os, re
R = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "constructchip")
EDGE = "/tmp/claude-0/-home-user-constructchip/0373e500-2042-50f1-80a0-d96b417c3161/scratchpad/edge/assets/project_edgerunner/lang/en_us.json"
src = json.load(open(EDGE))
P = "book.edgerunner.guide."

FIX = [  # their mod's mechanics -> ours
    (r"\*\*Tailoring Bench\*\*", "**crafting table** (with the Soldering Iron in the grid)"),
    (r"Tailoring Bench", "crafting table"), (r"tailoring assembly", "crafting recipe"),
    (r"in the Tailoring Bench tool slot", "in the crafting grid"),
    (r"Place Soldering Iron in Tool slot, add intermediates, and the cyberware appears on the right\.", "Put the Soldering Iron and the parts in a crafting grid; you get the iron back."),
    (r"Upgrades are accessed from the \*\*inventory screen\*\* via the dedicated cyberware button\.", "Upgrades are bought at the **Ripperdoc** surgery screen with Internal Parts."),
    (r"\*\*Right-click\*\* the RipperDoc to open the surgical interface\.", "**Sneak + right-click** the Soul Killer station (our RipperDoc) to open surgery."),
    (r"A cinematic plays on successful implantation\.", "The screen glitches when the surgery takes."),
    (r"\(Arms keybind\)", "(B)"), (r"Circulatory keybind", "J"), (r"Face keybind", "V"),
    (r"press \*\*Extra\*\* in your inventory", "right-click them"),
    (r"Insert optics into a Cyberware Painting Station to paint and preview your Face Overlay in-game\.", ""),
    (r"RipperDoc", "Ripperdoc"),
]

def fix(t):
    for a, b in FIX: t = re.sub(a, b, t)
    return t.replace("\\\n", "\n").replace("\\", "").strip()

entries = {}  # (category, entry) -> {"name","desc","pages":[(title,text)]}
for k, v in src.items():
    if not k.startswith(P): continue
    parts = k[len(P):].split(".")
    if len(parts) < 2 or parts[0] in ("tooltip", "description", "name"): continue
    cat, entry = parts[0], parts[1]
    e = entries.setdefault((cat, entry), {"name": entry.replace("_", " ").title(), "desc": "", "pages": {}})
    if len(parts) == 3 and parts[2] == "name": e["name"] = v
    elif len(parts) == 3 and parts[2] == "description": e["desc"] = v
    elif len(parts) >= 4 and re.fullmatch(r"page\d+", parts[2]):
        n = int(parts[2][4:]); pg = e["pages"].setdefault(n, {"title": "", "text": ""})
        if parts[3] == "text": pg["text"] = fix(v)
        elif parts[3] in ("title", "title1"): pg["title"] = pg["title"] or fix(v)
    elif len(parts) == 3 and parts[2] == "text":  # shared text like crafted_at_tailoring_bench
        pass

CATS = [("getting_started", "Getting Started"), ("cyberware", "Cyberware"), ("cyberpsychosis", "Cyberpsychosis"), ("crafting", "Crafting"), ("constructs", "Constructs & Chrome")]
# our own chapter
entries[("constructs", "overview")] = {"name": "Constructs and chrome", "desc": "How a chip in your head meets your implants.", "pages": {
    0: {"title": "Your construct", "text": "A personality construct in your head is chrome too: it counts **20 Load**.\n\nConstructs can't use implants themselves (they have no body)."},
    1: {"title": "Construct skills", "text": "**Chrome Link**: they see your load and cyberpsychosis.\n**Neural Buffer**: your pressure fades faster and the disease grows slower.\n**Chrome Override**: they fire your Sandevistan or Blood Pump for you."},
    2: {"title": "Hacking chrome", "text": "**Short Circuit** shuts off a target's active implants and puts them on cooldown.\n**System Lock** locks them out for 30 s.\n**Deep Scan** lists a target's implants."},
    3: {"title": "Painting", "text": "Put an implant in a **Cyberware Painting Station** and paint its model texture: brush, bucket, eraser, picker, undo. **Save** writes it to the item; it shows on your body."}}}
lang_p = os.path.join(R, "lang", "en_us.json"); lang = json.load(open(lang_p))
lang = {k: v for k, v in lang.items() if not k.startswith("guide.constructchip.")}
index = []
for cat, title in CATS:
    lang[f"guide.constructchip.{cat}"] = title
    items = []
    for (c, entry), e in entries.items():
        if c != cat or not e["pages"]: continue
        key = f"guide.constructchip.{cat}.{entry}"
        lang[key] = e["name"]; lang[key + ".desc"] = e["desc"]
        pages = [e["pages"][n] for n in sorted(e["pages"]) if e["pages"][n]["text"]]
        for i, pg in enumerate(pages):
            lang[f"{key}.p{i}.title"] = pg["title"] or e["name"]
            lang[f"{key}.p{i}.text"] = pg["text"]
        items.append({"id": entry, "pages": len(pages)})
    index.append({"id": cat, "entries": items})
lang["item.constructchip.guide"] = "Edgerunner's Guide"
lang["gui.constructchip.guide.back"] = "Back"
os.makedirs(os.path.join(R, "guide"), exist_ok=True)
json.dump(index, open(os.path.join(R, "guide", "index.json"), "w"), indent=2)
json.dump(dict(sorted(lang.items())), open(lang_p, "w"), indent=2, ensure_ascii=False); open(lang_p, "a").write("\n")
print({c["id"]: len(c["entries"]) for c in index})
