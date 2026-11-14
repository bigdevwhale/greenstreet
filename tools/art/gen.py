"""Generates art with `codex exec` (its image tool) for every job in jobs(); raw PNGs land in build/art-raw/<name>.png; then run process.py.

Usage: python gen.py [name ...]   (no names = every job whose raw file is missing)
"""
import csv, os, re, shutil, subprocess, sys, glob
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
RAW = os.path.join(REPO, "build", "art-raw")
GEN = os.path.expanduser(r"~\.codex\generated_images")
os.makedirs(RAW, exist_ok=True)

STYLE = ("Cozy retro 16-bit pixel art matching the attached reference images: chunky clearly visible square pixels, "
         "muted warm palette, dark brown ink outlines, a little ordered dithering, no anti-aliasing, no text, no letters, "
         "no UI, no frame or border.")

SKIN = {"1": "fair skin", "2": "light ruddy skin", "3": "olive tanned skin"}
HAIR = {"wood": "brown", "grey": "grey", "dark": "black", "ginger": "ginger", "honey": "golden blonde"}
CLOTH = {"cream": "cream white", "green": "green", "jam": "raspberry red", "roofB": "cornflower blue", "navy": "navy blue",
         "apple": "tomato red", "lilac": "lilac", "oak": "olive khaki", "honey": "mustard yellow", "enamel": "teal green",
         "autumn": "rust orange", "red": "red"}


def look_text(look):
    m = dict((kv.split("=") + ["1"])[:2] for kv in look.split(","))
    bits = [SKIN.get(m.get("skin", "1"), "fair skin"), HAIR.get(m.get("hair", "wood"), "brown") + " hair"]
    if "long" in m: bits.append("long hair")
    if "bun" in m: bits.append("hair in a bun")
    if "curls" in m: bits.append("curly hair")
    if "pigtails" in m: bits.append("two pigtails")
    if "beard" in m: bits.append("a full beard")
    if "moustache" in m: bits.append("a moustache")
    if "glasses" in m: bits.append("round glasses")
    if "earrings" in m: bits.append("gold earrings")
    cloth = CLOTH.get(m.get("cloth", ""), "grey")
    bits.append(f"a {cloth} coat or dress")
    if "apron" in m: bits.append("a white apron")
    if m.get("hat") == "toque": bits.append("a tall white baker's toque")
    if "kerchief" in m: bits.append(f"a {CLOTH.get(m['kerchief'], cloth)} headscarf tied under the chin")
    if "cap" in m or m.get("hat") == "cap": bits.append(f"a flat cap")
    return ", ".join(bits)


PLACE = {
    "bakery": "a bakery: loaves and buns in the shop windows",
    "cafe": "a small cafe: cups and a pastry stand in the windows",
    "florist": "a florist: buckets of flowers in the windows and by the door",
    "garage": "a car repair garage: a wide workshop gate beside a small door, tyres and tools",
    "hardware": "a hardware shop: buckets, watering cans, rakes and tools in the windows",
    "library": "a public library: bookshelves visible through the windows",
    "market": "a covered market stall front: crates of vegetables and fruit",
    "park": "a park keeper's small wooden lodge at the park gate, trees around",
    "pharmacy": "an old pharmacy: glass jars and bottles on shelves in the windows",
    "post": "a post office: a blue post box by the door, parcels in the windows",
    "salon": "a hairdresser's salon: a mirror and a chair visible through the window",
    "school": "a school entrance: a green chalkboard and globe visible through the windows",
    "sports": "a sports ground clubhouse: a football net and balls by the door",
    "station": "a small railway station building: a clock over the door",
    "tailor": "a tailor's studio: bolts of coloured cloth and a dress form in the windows",
    "theatre": "a small old theatre entrance: columns and playbill frames (blank, no text)",
    "vet": "a veterinary clinic: a paw-print sign (no text), pet beds in the window",
    "water": "a granite river embankment with a cast-iron railing, the river and the far bank behind",
}

SCENE_RULES = ("Straight-on front elevation, flat orthographic view, like a side-scrolling game background. "
               "The sky must be a perfectly flat solid magenta #FF00FF (it will be replaced). "
               "Window glass is pale grey-blue. No people, no animals. Landscape 1536x1024.")


def jobs():
    out = {}
    with open(os.path.join(REPO, r"app\src\main\assets\content\people.tsv"), encoding="utf-8") as f:
        for p in csv.DictReader(f, delimiter="\t"):
            pid = p["id"]
            if pid == "pryanik":
                out["people/pryanik"] = ("portrait", "A single pixel-art sprite of a big fat ginger tabby cat lying curled up and dozing, "
                                         "seen from the side, whole cat visible, centred, on a perfectly flat solid magenta #FF00FF background, no shadow, no ground.")
                continue
            out[f"people/{pid}"] = ("portrait",
                f"A single full-body pixel-art character sprite: {p['name_en']}, {p['role_en']}. {p['about_en']} "
                f"Appearance: {look_text(p['look'])}. Standing, facing the viewer, arms down, feet at the bottom, the whole figure "
                f"visible and centred, filling most of the height, on a perfectly flat solid magenta #FF00FF background with no "
                f"shadow, no ground and no other objects.")
    out["people/hero"] = ("portrait",
        "A single full-body pixel-art character sprite: the player character of a cozy walking-and-gardening game, a young city "
        "dweller of about thirty who has just inherited a great-aunt's yard and hens: a red knitted hat with a pompom, a mustard "
        "yellow scarf, a teal-green short coat, dark trousers, sturdy boots, a canvas shoulder bag with a few seed packets peeking "
        "out, a friendly hopeful face. Standing, facing the viewer, arms down, feet at the bottom, the whole figure visible and "
        "centred, filling most of the height, on a perfectly flat solid magenta #FF00FF background with no shadow, no ground and no "
        "other objects.")
    for k, what in PLACE.items():
        out[f"places/{k}"] = ("landscape",
            f"A pixel-art game background: the street front of {what}, in an old 19th-century St Petersburg building on a quiet street. "
            + ("" if k in ("water", "park") else "The entrance door is open and empty, exactly in the horizontal centre. ")
            + "A strip of granite pavement runs along the bottom fifth of the picture. " + SCENE_RULES)
    out["places/stroll"] = ("landscape",
        "A pixel-art game background: a path through a small city park, the path running from the bottom centre into the distance, "
        "lawns, a few trees, a bench and a cast-iron street lamp, old city houses beyond the trees. " + SCENE_RULES)
    out["yard/well"] = ("landscape",
        "A pixel-art game background: an old St Petersburg courtyard-well seen from inside: tall plastered walls of the house on three "
        "sides with rows of windows, an arch to the street on the left of the back wall, a green front door with a little canopy and a "
        "lamp on the right of the back wall, a drainpipe, cobbled ground, which must fill the whole bottom 38% of the picture (the foot of the walls at 62% of the height), the ground mostly empty. " + SCENE_RULES)
    out["yard/block"] = ("landscape",
        "A pixel-art game background: a Soviet panel block of flats seen across its open courtyard: panel seams, rows of small windows, "
        "a few glazed balconies, an entrance door with a canopy in the middle, two birches at the sides, a swing, asphalt and "
        "worn grass on the ground, which must fill the whole bottom 38% of the picture (the foot of the walls at 62% of the height), the ground mostly empty. " + SCENE_RULES)
    out["yard/house"] = ("landscape",
        "A pixel-art game background: a Russian wooden log house of one's own with carved white window frames and a gable roof, "
        "a porch and door in the middle, a picket fence, an apple tree on the right, a small log well, a dark spruce forest beyond, "
        "grass and a dirt path on the ground, which must fill the whole bottom 38% of the picture (the foot of the walls at 62% of the height), the ground mostly empty. " + SCENE_RULES)
    out["intro/cover"] = ("landscape",
        "Key art cover illustration for a cozy walking-and-gardening game called Green Street: an old St Petersburg street in late "
        "summer seen from the pavement, pastel 19th-century houses with shop fronts, small vegetable and flower beds along the lawns "
        "by the pavement (sunflowers, calendula, radishes), a bakery with a striped awning, a tram stop, a ginger cat on a bench, "
        "two hens, a few friendly neighbours, warm golden evening light, a soft sky with clouds. Wide landscape 1536x1024, rich detail, "
        "no text, no logo.")
    out["intro/desk"] = ("landscape",
        "A pixel-art scene: late night at a desk by a big window, a laptop with a cold blue glowing screen, a mug of cold tea, a dead "
        "pot plant, behind the window a dark rainy city of tall blocks with many small lit windows. Melancholic, blue and brown. "
        "Wide landscape 1536x1024, no people, no text.")
    out["intro/letter"] = ("landscape",
        "A pixel-art scene seen from above: an old wooden kitchen table with a red and white checked oilcloth in a corner, an opened "
        "paper envelope with a stamp and a handwritten letter (scribbles, no readable text), an old round tin full of small paper "
        "seed packets, an old iron key on a string with a cardboard tag, a pressed orange calendula flower. Warm and nostalgic. "
        "Wide landscape 1536x1024, no people, no text.")
    out["intro/loading"] = ("landscape",
        "A cozy pixel-art illustration seen from above: an old wooden table with a large sheet of paper on it, on which a small city "
        "district map is being drawn by hand: streets in brown ink, little coloured houses, green parks, a blue river, half of the map "
        "still blank and only pencil-sketched; a pencil lying on the paper, a compass, a cup of tea, a sprig of calendula. Warm "
        "morning light. Wide landscape 1536x1024, no people, no text, no letters.")
    out["intro/verge"] = ("landscape",
        "A pixel-art game background seen from straight above (top-down, like a map tile): along the top quarter a strip of grey granite "
        "pavement slabs, below it a lush green city lawn with a few clover patches and tiny wild flowers filling the rest, completely "
        "flat and empty (no beds, no objects, no people). Wide landscape 1536x768, no text.")
    out["icon/fg"] = ("square",
        "The foreground layer of a mobile app icon for a cozy pixel-art walking-and-gardening game called Green Street: a small raised "
        "wooden garden bed full of dark soil, with a bright green sprout with two big leaves and a cheerful orange calendula flower "
        "growing from it, bold simple readable shapes, thick dark brown outline, chunky pixels, centred, filling about 60% of the "
        "square, on a perfectly flat solid magenta #FF00FF background, no shadow, no text. Square 1024x1024.")
    out["icon/bg"] = ("square",
        "The background layer of a mobile app icon in cozy pixel art, no objects at all: a soft clear sky, light blue at the top "
        "fading to warm pale yellow near the middle, a strip of grey granite pavement slabs and a band of green grass along the bottom "
        "quarter, chunky pixels, simple and calm, no text. Square 1024x1024.")
    for n, (keys, lines) in enumerate(bed_sheets()):
        cells = " ".join(f"Row {i // 4 + 1}, cell {i % 4 + 1}: {t}." for i, t in enumerate(lines))
        out[f"beds/sheet{n:02d}"] = ("square",
            "A sprite sheet of 16 separate garden bed sprites for a cozy pixel-art farming game map, laid out on an invisible 4 by 4 grid: "
            "each sprite centred in its own equal square cell with the bed at the bottom of the cell and the plants rising above it, never "
            "touching a neighbouring cell, dark brown outlines, warm palette, on a perfectly flat solid magenta #FF00FF background, no grid "
            "lines, no shadows on the background, no labels, no text. " + cells + " Square 1024x1024.")
    for n, (keys, lines) in enumerate(ui_sheets()):
        cells = " ".join(f"Row {i // 4 + 1}, cell {i % 4 + 1}: {t}." for i, t in enumerate(lines))
        out[f"ui/sheet{n:02d}"] = ("square",
            "A sprite sheet of 16 separate small UI icons for a cozy pixel-art game's menus, laid out on an invisible 4 by 4 grid: each icon "
            "drawn large and centred in its own equal square cell, never touching a neighbouring cell, simple bold readable shapes with a dark "
            "brown outline, warm palette, on a perfectly flat solid magenta #FF00FF background, no grid lines, no labels, no text. "
            + cells + " Square 1024x1024.")
    for n, (keys, lines) in enumerate(item_sheets()):
        cells = " ".join(f"Row {i // 4 + 1}, cell {i % 4 + 1}: {t}." for i, t in enumerate(lines))
        out[f"items/sheet{n:02d}"] = ("square",
            "A sprite sheet of 16 separate inventory item icons for the game, laid out on an invisible 4 by 4 grid: each item drawn "
            "large and centred in its own equal square cell, items never touching each other or crossing into a neighbouring cell, "
            "on a perfectly flat solid magenta #FF00FF background, no grid lines, no shadows on the background, no labels, no numbers, "
            "no text. Each item is a single object seen slightly from above. " + cells + " Square 1024x1024.")
    return out


UI_ICONS = [
    ("people", "two neighbours side by side, head and shoulders"),
    ("hen", "a white hen sitting on a straw nest with two eggs"),
    ("sill", "a small sprout in a terracotta pot on a windowsill"),
    ("balcony", "a little glass greenhouse with green plants inside"),
    ("garden", "a raised wooden garden bed with lettuces and a small sign"),
    ("kitchen", "a cooking pot with steam over a little stove flame"),
    ("cellar", "a wooden shelf with jars of jam and pickles"),
    ("board", "a cork notice board with pinned paper notes"),
    ("household", "a small cottage with a coin beside it"),
    ("tools", "a watering can and a garden rake crossed"),
    ("asks", "a paper note with a red exclamation mark"),
    ("letterbox", "a blue post box with a letter in its slot"),
    ("walk", "a walking boot with a leaf"),
    ("seeds", "a paper seed packet with a flower printed on it"),
    ("market", "a market stall with a striped awning and crates"),
    ("fair", "festive bunting flags on a string"),
    ("tab_street", "a folded town map with green parks and a red pin"),
    ("tab_yard", "a cozy house with a red roof and lit windows"),
    ("tab_people", "two friendly villagers, a woman in a headscarf and a man in a cap"),
    ("tab_bag", "a brown leather satchel bag"),
    ("coin", "a gold coin with a square hole in the middle, shiny"),
    ("gear", "a settings cog wheel in warm brass"),
    ("home", "a front door with a little lamp above it"),
    ("map_pin", "a red map pin"),
]


BED = "a small raised wooden garden bed of light planks full of dark soil, seen from a three-quarter top-down view, wider than tall, the same bed shape in every cell"
BEDS = [
    ("empty", f"{BED}, empty freshly tilled soil"),
    ("sown", f"{BED}, soil with three neat rows of tiny seeds and little marks"),
    ("sprouts", f"{BED}, three rows of tiny green two-leaf sprouts"),
    ("young", f"{BED}, three rows of small young leafy plants"),
    ("grown", f"{BED}, nearly grown, full of lush green leafy plants"),
    ("wild", f"{BED}, overgrown and neglected, weeds, dry brown stalks and dandelions"),
    ("snow", f"{BED}, covered with a cap of white snow, a few dry stalks poking out"),
    ("ripe_bulb", f"{BED}, ripe onions and garlic with their bulbs showing and tall green stalks"),
    ("ripe_bulbflower", f"{BED}, tulips and daffodils in full bloom"),
    ("ripe_bush", f"{BED}, a leafy bush heavy with ripe red berries and tomatoes"),
    ("ripe_flower", f"{BED}, a mass of blooming garden flowers, orange calendula and blue cornflowers"),
    ("ripe_grain", f"{BED}, ripe golden wheat and rye ears standing tall"),
    ("ripe_herb", f"{BED}, bushy fragrant herbs, dill, mint and basil"),
    ("ripe_leafy", f"{BED}, rows of big ripe lettuce and spinach heads"),
    ("ripe_root", f"{BED}, ripe carrots and beets with their orange and red tops showing among leaves"),
    ("ripe_round", f"{BED}, a ripe green cabbage and an orange pumpkin among big leaves"),
    ("ripe_spike", f"{BED}, tall spiky leek and lavender stalks"),
    ("ripe_sprout", f"{BED}, a dense carpet of fresh green microgreens and cress"),
    ("ripe_tall", f"{BED}, two tall sunflowers with big yellow heads"),
    ("ripe_vine", f"{BED}, climbing peas and cucumbers on a little wooden trellis"),
]


def bed_sheets():
    return [([k for k, _ in BEDS[i:i + 16]], [t for _, t in BEDS[i:i + 16]]) for i in range(0, len(BEDS), 16)]


def ui_sheets():
    return [([k for k, _ in UI_ICONS[i:i + 16]], [t for _, t in UI_ICONS[i:i + 16]]) for i in range(0, len(UI_ICONS), 16)]


def first(text):
    return text.split(". ")[0].rstrip(".")


def item_sheets():
    """The items, 16 to a sheet: every variety (its harvest), every good, every dish."""
    base = os.path.join(REPO, "app", "src", "main", "assets", "content")
    def rows(f):
        with open(os.path.join(base, f), encoding="utf-8") as fh:
            return list(csv.DictReader(fh, delimiter="	"))
    items = []
    for r in sorted(rows("crops.tsv"), key=lambda r: (r["crop"], r["key"])):
        what = "a cut bunch of the flowers" if r["art"] in ("flower", "bulbflower") else "the harvest (just the produce, no plant pot)"
        items.append((r["key"], f"{r['name_en']}, {what}: {first(r['about_en'])}"))
    for r in rows("goods.tsv"):
        items.append((r["key"], f"{r['name_en']}: {first(r['about_en'])}"))
    seen = set()
    for r in rows("recipes.tsv"):
        if r["makes"] in seen:
            continue
        seen.add(r["makes"])
        items.append((r["makes"], f"{r['name_en']} (a home-made dish or product, shown served or in its jar, bottle or plate): {first(r['about_en'])}"))
    return [([k for k, _ in items[i:i + 16]], [t for _, t in items[i:i + 16]]) for i in range(0, len(items), 16)]


def refs(kind):
    r = [os.path.join(REPO, "docs", "screenshots", "en", "3-neighbour.jpg")]
    if kind == "square":
        r = [os.path.join(REPO, "docs", "screenshots", "en", "8-bag.jpg")]
    elif kind == "portrait":
        r.append(os.path.join(HERE, "style_person.png"))
    else:
        r.append(os.path.join(REPO, "docs", "screenshots", "en", "4-yard.jpg"))
    return [x for x in r if os.path.exists(x)]


def run(name, kind, prompt):
    dst = os.path.join(RAW, name.replace("/", "__") + ".png")
    full = f"Use your image generation tool to generate exactly ONE image. {STYLE} {prompt} Do not write any files; just generate the image."
    args = ["codex", "exec", "--skip-git-repo-check", "-s", "read-only"]
    for r in refs(kind):
        args += ["-i", r]
    args.append("-")
    p = subprocess.run(args, input=full, capture_output=True, text=True, encoding="utf-8", errors="replace", cwd=HERE, shell=True, timeout=900)
    log = p.stdout + p.stderr
    m = re.search(r"session id: (\S+)", log)
    pngs = sorted(glob.glob(os.path.join(GEN, m.group(1), "*.png")), key=os.path.getmtime) if m else []
    if not pngs:
        print(f"FAIL {name}\n{log[-1500:]}", flush=True)
        return
    shutil.copy(pngs[-1], dst)
    print(f"ok   {name}", flush=True)


if __name__ == "__main__":
    js = jobs()
    want = sys.argv[1:] or [n for n in js if not os.path.exists(os.path.join(RAW, n.replace("/", "__") + ".png"))]
    print(f"{len(want)} jobs", flush=True)
    with ThreadPoolExecutor(5) as ex:
        list(ex.map(lambda n: run(n, *js[n]), want))
