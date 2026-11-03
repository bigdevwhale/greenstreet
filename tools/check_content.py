"""Validates the content TSVs (see docs/CONTENT.md).

python tools/check_content.py              # everything
python tools/check_content.py people/vera  # one file (plus the shared tables it refers to)
python tools/check_content.py --stats      # also print counts per file / per year

Prints every problem and exits with 1 if there are any. Missing files are reported, not fatal, so a
module can be checked before the rest of the content exists.
"""
import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
C = ROOT / "app/src/main/assets/content"

CROPS = """radish lettuce spinach sorrel dill parsley coriander onion garlic leek peas beans carrot beet turnip
potato cabbage kohlrabi cucumber zucchini pumpkin tomato corn sunflower rye wheat oats buckwheat flax
poppy mint basil chamomile calendula cornflower nasturtium marigold sweetpea aster cosmos tulip daffodil
crocus lavender thyme valerian catnip woad madder strawberry rhubarb horseradish topinambur lupine
microgreens sprouts cress green_onion hyacinth amaryllis
pepper eggplant cherry_tomato physalis melon chili
raspberry currant gooseberry honeysuckle apple_tree""".split()
YARD = "egg honey wax feather".split()
FISH = "roach perch ruff bream pike crucian tench smelt burbot ide zander carp rudd gudgeon catfish".split()
FINDS = """birch_sap willow_twig nettle dandelion morel lime_blossom wild_strawberry wild_raspberry elderberry
chanterelle boletus suillus honey_fungus porcini russula rowan rosehip hazelnut acorn conker maple_leaf
pine_cone spruce_cone birch_bark sea_buckthorn ranetka bird_cherry driftwood pebble four_leaf_clover""".split()
STAPLES = "sugar salt flour milk butter yeast vinegar tea_black spices".split()
PRODUCTS = """bread_rye bread_wheat pie_cabbage pie_apple pie_berry pirozhki bubliki blini oat_cookies
gingerbread medovik kalach syrniki pumpkin_porridge buckwheat_porridge shchi green_shchi borscht ukha
mushroom_soup pea_soup okroshka draniki vinegret salad_radish salad_spring zucchini_caviar baked_pumpkin
stuffed_peppers vegetable_stew cottage_cheese jam_strawberry jam_raspberry jam_rowan jam_currant
jam_gooseberry jam_pumpkin pickled_cucumbers sauerkraut pickled_mushrooms salted_tomatoes lecho
dried_mushrooms dried_apples khrenovina pickled_garlic kvass mors kompot sbiten kissel tea_chamomile
tea_mint tea_lime tea_rosehip tea_thyme tea_garden salve_calendula soap_herbal candle linen_cloth
cloth_blue cloth_red cloth_yellow oil_sunflower flour_rye flour_buckwheat seed_bomb wreath bouquet
herbarium lavender_sachet bird_feeder dried_flowers poppy_filling""".split()
FLOWERS = "chamomile calendula cornflower nasturtium marigold sweetpea aster cosmos tulip daffodil crocus lavender hyacinth amaryllis".split()
HERBS = "dill parsley coriander mint basil thyme valerian catnip sorrel".split()
BERRIES = "strawberry raspberry currant gooseberry honeysuckle wild_strawberry wild_raspberry rowan rosehip elderberry sea_buckthorn bird_cherry".split()
NONFOOD = set(FLOWERS) | {"flax", "woad", "madder", "lupine", "catnip", "valerian"}
VEG = [c for c in CROPS if c not in FLOWERS and c not in HERBS and c not in BERRIES and c not in NONFOOD]
GROUPS = {"anyfish": FISH, "anymush": "chanterelle boletus suillus honey_fungus porcini russula morel".split(),
          "anyberry": BERRIES, "anyflower": FLOWERS, "anyherb": HERBS, "anyveg": VEG,
          "anyjam": [p for p in PRODUCTS if p.startswith("jam_")], "anytea": [p for p in PRODUCTS if p.startswith("tea_")],
          "anybread": "bread_rye bread_wheat kalach bubliki".split(), "anycloth": "linen_cloth cloth_blue cloth_red cloth_yellow".split(),
          "anyfind": FINDS}
TOWN = "prokhor agafya mila vera kuzmich sych zina lilya mikhalych tanya zhanna gena efim palych slava ada olga zoya".split()
YARDP = "semyonych nina asya gosha pryanik".split()
PEOPLE = TOWN + YARDP
WHEN = set("spring summer autumn winter rain snow frost fog sun heat morning day evening night weekend y1 y2 y3 y2+ y3+".split())
BUILTIN_FLAGS = set("y2 y3 balcony hive garden km100 km500 km1000 km3000".split())
ARTS = set("bulb leafy spike flower grain tall vine round bush bulbflower herb root sprout".split())
RAMPS = set("jam apple honey gold autumn cream lilac roofB enamel green oak ginger".split())
UNLOCK_CROP = {"start", "market", "request", "fest", "letter", "y2", "y3"} | set(PEOPLE)
UNLOCK_REC = {"start", "letter", "fest", "y2", "y3"} | set(PEOPLE)
STATIONS = set("kitchen oven cellar hive workbench".split())
TOOLS = {"can", "basket", "rod", "beds"}

COLS = {
    "crops.tsv": "key crop place sow days tends regrow yield price art ramp unlock name_ru name_en about_ru about_en",
    "goods.tsv": "key kind months where time weather price rare name_ru name_en about_ru about_en",
    "recipes.tsv": "key needs makes qty station unlock price name_ru name_en about_ru about_en",
    "people.tsv": "id home kind osm seeds likes loves dislikes look name_ru name_en role_ru role_en about_ru about_en hours",
    "festivals.tsv": "key md rule year name_ru name_en notice_ru notice_en gift ribbon",
    "almanac.tsv": "md text_ru text_en",
    "board.tsv": "key months needs reward text_ru text_en",
    "ribbons.tsv": "key kind arg name_ru name_en about_ru about_en",
    "acquaint.tsv": "id intro_ru intro_en perk_ru perk_en",
    "facts.tsv": "who hearts ru en",
}
_doc = (ROOT / "docs/CONTENT.md").read_text(encoding="utf-8")
FIXED_VARIETIES = set(re.findall(r"(?<![a-z_])[a-z]+(?:_[a-z]+)*_[a-z]+(?![a-z_])", _doc.split("## Fixed variety keys", 1)[1])) if "## Fixed variety keys" in _doc else set()
PCOLS = "t n gap months when hearts needs reward text_ru text_en reply_ru reply_en sets".split()

problems = []
flags_set, flags_used = set(), []


def bad(where, msg):
    problems.append(f"{where}: {msg}")


def read(rel, cols):
    p = C / rel
    if not p.exists():
        bad(rel, "missing file")
        return []
    lines = p.read_text(encoding="utf-8").split("\n")
    head = lines[0].split("\t")
    if head != cols:
        bad(rel, f"header {head} != {cols}")
        return []
    rows = []
    for i, ln in enumerate(lines[1:], 2):
        if not ln.strip():
            continue
        cells = ln.split("\t")
        if len(cells) != len(cols):
            bad(f"{rel}:{i}", f"{len(cells)} cells, want {len(cols)}")
            continue
        r = dict(zip(cols, cells))
        r["_at"] = f"{rel}:{i}"
        rows.append(r)
    return rows


LAT = re.compile(r"[A-Za-z]")
CYR = re.compile(r"[А-Яа-яЁё]")


def text(r, col, limit, required=True):
    v = r.get(col, "")
    at = r["_at"]
    if not v:
        if required:
            bad(at, f"empty {col}")
        return
    if len(v) > limit:
        bad(at, f"{col} too long ({len(v)} > {limit})")
    if col.endswith("_ru"):
        stripped = re.sub(r"«[^»]*»", "", v)
        if LAT.search(stripped):
            bad(at, f"latin letters in {col}: {v[:60]}")
        if '"' in v:
            bad(at, f"straight quotes in {col}")
        if re.search(r"\bты\b|\bтебя\b|\bтебе\b|\bтвой\b|\bтвоя\b|\bтвои\b", v, re.I) and not r.get("_ty_ok"):
            bad(at, f"«ты» in {col} (neighbours say «вы»): {v[:80]}")
        if " - " in v:
            bad(at, f"hyphen instead of dash in {col}")
    if col.endswith("_en") and CYR.search(v):
        bad(at, f"cyrillic in {col}")
    if "  " in v:
        bad(at, f"double space in {col}")


def months(r, col="months"):
    v = r.get(col, "")
    if not v:
        return
    for m in v.split(","):
        if not m.isdigit() or not 1 <= int(m) <= 12:
            bad(r["_at"], f"bad month {m!r} in {col}")


def num(r, col, lo, hi, required=True):
    v = r.get(col, "")
    if v == "" and not required:
        return None
    try:
        x = float(v)
    except ValueError:
        bad(r["_at"], f"{col} not a number: {v!r}")
        return None
    if not lo <= x <= hi:
        bad(r["_at"], f"{col}={v} outside {lo}..{hi}")
    return x


def main():
    only = [a for a in sys.argv[1:] if not a.startswith("--")]
    stats = "--stats" in sys.argv
    crops = read("crops.tsv", COLS["crops.tsv"].split())
    goods = read("goods.tsv", COLS["goods.tsv"].split())
    recipes = read("recipes.tsv", COLS["recipes.tsv"].split())
    varieties = {r["key"] for r in crops}
    species_seen = {r["crop"] for r in crops}
    known = set(CROPS) | varieties | set(YARD) | set(FISH) | set(FINDS) | set(STAPLES) | set(PRODUCTS) | set(GROUPS)

    def needs(r, col="needs", allow_empty=True):
        v = r.get(col, "")
        if not v:
            if not allow_empty:
                bad(r["_at"], f"empty {col}")
            return
        for part in v.split(","):
            m = re.fullmatch(r"([a-z_0-9]+):(\d+)", part)
            if not m:
                bad(r["_at"], f"bad {col} part {part!r}")
            elif m.group(1) not in known:
                bad(r["_at"], f"unknown key {m.group(1)!r} in {col}")
            elif not 1 <= int(m.group(2)) <= 20:
                bad(r["_at"], f"qty {m.group(2)} in {col}")

    def reward(r, col="reward"):
        v = r.get(col, "")
        if not v:
            return
        for part in v.split(","):
            bits = part.split(":")
            k = bits[0]
            if k == "coins" and len(bits) == 2 and bits[1].isdigit():
                continue
            if k == "seeds" and len(bits) == 3 and bits[1] in varieties and bits[2].isdigit():
                continue
            if k == "seeds" and len(bits) == 3 and not crops and bits[1] in FIXED_VARIETIES and bits[2].isdigit():
                continue
            if k == "item" and len(bits) == 3 and bits[1] in known and bits[2].isdigit():
                continue
            if k == "recipe" and len(bits) == 2 and bits[1] in PRODUCTS:
                continue
            if k == "tool" and len(bits) == 2 and bits[1] in TOOLS:
                continue
            bad(r["_at"], f"bad reward part {part!r}")

    if not only or "crops" in only:
        keys = Counter(r["key"] for r in crops)
        for k, n in keys.items():
            if n > 1:
                bad("crops.tsv", f"duplicate key {k}")
        for r in crops:
            if r["crop"] not in CROPS:
                bad(r["_at"], f"unknown crop {r['crop']}")
            if not r["key"].startswith(r["crop"] + "_"):
                bad(r["_at"], "key must start with crop_")
            if r["place"] not in ("verge", "sill", "balcony", "garden"):
                bad(r["_at"], f"bad place {r['place']}")
            months(r, "sow")
            d = num(r, "days", 5, 400)
            t = num(r, "tends", 1, 300)
            if d and t and t > d:
                bad(r["_at"], "tends > days")
            num(r, "regrow", 0, 365)
            num(r, "yield", 1, 12)
            num(r, "price", 1, 60)
            if r["art"] not in ARTS:
                bad(r["_at"], f"bad art {r['art']}")
            if r["ramp"] not in RAMPS:
                bad(r["_at"], f"bad ramp {r['ramp']}")
            if r["unlock"] not in UNLOCK_CROP:
                bad(r["_at"], f"bad unlock {r['unlock']}")
            for c in ("name_ru", "name_en"):
                text(r, c, 60)
            for c in ("about_ru", "about_en"):
                text(r, c, 160)
        for k in FIXED_VARIETIES:
            if crops and k not in varieties and "_" in k and k.split("_")[0] in {c.split("_")[0] for c in CROPS}:
                bad("crops.tsv", f"fixed variety {k} is not defined")
        for s in CROPS:
            if crops and s not in species_seen:
                bad("crops.tsv", f"species {s} has no variety")
        for s in CROPS:
            rows = [r for r in crops if r["crop"] == s and r["place"] == "verge"]
            if rows and not any(r["unlock"] in {"start", "market"} | set(PEOPLE) for r in rows):
                bad("crops.tsv", f"verge species {s} has no early variety (start/market/neighbour)")

    if not only or "goods" in only:
        gk = {r["key"] for r in goods}
        for k in YARD + FISH + FINDS + STAPLES:
            if goods and k not in gk:
                bad("goods.tsv", f"missing {k}")
        for r in goods:
            if r["key"] not in set(YARD + FISH + FINDS + STAPLES):
                bad(r["_at"], f"unknown key {r['key']}")
            if r["kind"] not in ("yard", "fish", "find", "staple"):
                bad(r["_at"], f"bad kind {r['kind']}")
            months(r)
            num(r, "price", 1, 80)
            num(r, "rare", 0, 3)
            for c in ("name_ru", "name_en"):
                text(r, c, 40)
            for c in ("about_ru", "about_en"):
                text(r, c, 160)

    if not only or "recipes" in only:
        rk = Counter(r["makes"] for r in recipes)
        for p in PRODUCTS:
            if recipes and p not in rk:
                bad("recipes.tsv", f"no recipe makes {p}")
        for r in recipes:
            needs(r, allow_empty=False)
            if r["makes"] not in PRODUCTS:
                bad(r["_at"], f"unknown product {r['makes']}")
            num(r, "qty", 1, 12)
            num(r, "price", 1, 90)
            if r["station"] not in STATIONS:
                bad(r["_at"], f"bad station {r['station']}")
            if r["unlock"] not in UNLOCK_REC:
                bad(r["_at"], f"bad unlock {r['unlock']}")
            for c in ("name_ru", "name_en"):
                text(r, c, 50)
            for c in ("about_ru", "about_en"):
                text(r, c, 160)

    people = read("people.tsv", COLS["people.tsv"].split()) if (not only or "people" in only) else []
    for r in people:
        if r["id"] not in PEOPLE:
            bad(r["_at"], f"unknown id {r['id']}")
        for c in ("seeds",):
            for k in filter(None, r[c].split(",")):
                if crops and k not in varieties:
                    bad(r["_at"], f"seed {k} is not a variety")
        for c in ("likes", "loves", "dislikes"):
            for k in filter(None, r[c].split(",")):
                if k not in known:
                    bad(r["_at"], f"{c}: unknown {k}")
        for c in ("name_ru", "name_en", "role_ru", "role_en"):
            text(r, c, 60)
        for c in ("about_ru", "about_en"):
            text(r, c, 300)

    for pid in PEOPLE:
        rel = f"people/{pid}.tsv"
        if only and not any(o in (rel, f"people/{pid}", pid) for o in only):
            continue
        rows = read(rel, PCOLS)
        cnt = Counter(r["t"] for r in rows)
        last = {}
        for r in rows:
            at = r["_at"]
            t = r["t"]
            if t not in ("line", "req", "note"):
                bad(at, f"bad t {t}")
                continue
            months(r)
            for w in filter(None, r["when"].split(",")):
                if w in WHEN or re.fullmatch(r"h\d+\+", w):
                    continue
                if w.startswith("fest:"):
                    continue
                if w.startswith("after:"):
                    flags_used.append((at, w[6:]))
                    continue
                bad(at, f"bad when {w!r}")
            if r["sets"]:
                if not re.fullmatch(r"[a-z]+\.[a-z_0-9]+", r["sets"]):
                    bad(at, f"bad flag {r['sets']}")
                flags_set.add(r["sets"])
            if r["hearts"]:
                num(r, "hearts", 0, 10)
            if t == "line":
                text(r, "text_ru", 200)
                text(r, "text_en", 220)
            elif t == "req":
                n = num(r, "n", 1, 200)
                if n is not None and n != last.get(t, 0) + 1:
                    bad(at, f"req n={int(n)} out of order")
                last[t] = n or last.get(t, 0)
                num(r, "gap", 0, 120)
                needs(r, allow_empty=False)
                reward(r)
                if "tool:" in r["reward"] and pid != "mikhalych":
                    bad(at, "tool rewards only from mikhalych")
                text(r, "text_ru", 260)
                text(r, "text_en", 280)
                text(r, "reply_ru", 260)
                text(r, "reply_en", 280)
            else:
                n = num(r, "n", 1, 200)
                if n is not None and n != last.get(t, 0) + 1:
                    bad(at, f"note n={int(n)} out of order")
                last[t] = n or last.get(t, 0)
                num(r, "gap", 0, 200)
                for f in filter(None, r["needs"].split(",")):
                    flags_used.append((at, f))
                reward(r)
                text(r, "text_ru", 650)
                text(r, "text_en", 700)
        if stats and rows:
            yrs = Counter(next((w for w in r["when"].split(",") if w.startswith("y")), "-") for r in rows if r["t"] != "line")
            print(f"{rel}: lines {cnt['line']}, requests {cnt['req']}, notes {cnt['note']}  years {dict(yrs)}")

    if not only or "festivals" in only:
        fest = read("festivals.tsv", COLS["festivals.tsv"].split())
        for r in fest:
            if r["md"] and not re.fullmatch(r"\d\d-\d\d", r["md"]):
                bad(r["_at"], f"bad md {r['md']}")
            if not r["md"] and r["rule"] not in ("maslenitsa", "sat_last_09", "sat_last_05", "first_snow", "first_ice", "solstice_s", "solstice_w"):
                bad(r["_at"], f"bad rule {r['rule']}")
            if r["year"] not in ("1", "2", "3", "any"):
                bad(r["_at"], f"bad year {r['year']}")
            if r["gift"]:
                reward({"reward": r["gift"], "_at": r["_at"]})
            for c in ("name_ru", "name_en"):
                text(r, c, 50)
            for c in ("notice_ru", "notice_en"):
                text(r, c, 420)
        by = Counter(r["key"] for r in fest)
        for k in by:
            ys = {r["year"] for r in fest if r["key"] == k}
            if not {"1", "2", "3", "any"} <= ys:
                bad("festivals.tsv", f"{k} lacks rows for years 1,2,3,any: {sorted(ys)}")

    if not only or "almanac" in only:
        alm = read("almanac.tsv", COLS["almanac.tsv"].split())
        mds = [r["md"] for r in alm]
        if alm and len(set(mds)) != 366:
            bad("almanac.tsv", f"{len(set(mds))} distinct days, want 366")
        for r in alm:
            text(r, "text_ru", 170)
            text(r, "text_en", 190)

    if not only or "board" in only:
        for r in read("board.tsv", COLS["board.tsv"].split()):
            months(r)
            needs(r, allow_empty=False)
            reward(r)
            r["_ty_ok"] = True
            text(r, "text_ru", 200)
            text(r, "text_en", 220)

    if not only or "ribbons" in only:
        for r in read("ribbons.tsv", COLS["ribbons.tsv"].split()):
            if r["kind"] not in ("harvest", "variety", "album", "fish", "find", "cook", "km", "beds", "wild", "hearts", "fair", "fest", "days", "season", "request"):
                bad(r["_at"], f"bad kind {r['kind']}")
            for c in ("name_ru", "name_en"):
                text(r, c, 50)
            for c in ("about_ru", "about_en"):
                text(r, c, 160)

    if not only:
        for at, f in flags_used:
            if f not in flags_set and f not in BUILTIN_FLAGS:
                bad(at, f"flag {f} is never set")

    for p in problems:
        print(p)
    print(f"{len(problems)} problem(s)")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
