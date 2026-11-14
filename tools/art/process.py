"""Turns raw Codex images into the app's pixel art: assets/art/{people,places,yard}/*.png (+ .lit.png window maps) and art.tsv."""
import os, sys, glob
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen import item_sheets, ui_sheets, bed_sheets, prop_sheets, RIBBONS
from PIL import Image
from scipy import ndimage

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
RAW = os.path.join(REPO, "build", "art-raw")
OUT = os.path.join(REPO, "app", "src", "main", "assets", "art")
PREVIEW = os.path.join(REPO, "build", "art-preview")

PERSON_H = 58      # a neighbour, feet to the top of the hat, at the doubled resolution
MAP_H = 18         # the same person walking about the map
MAP_H_HERO = 24    # the player stands out a little bigger
CAT_W = 26
PLACE = (256, 200)
YARD = (256, 208)
INTRO = {"loading": (240, 160), "cover": (384, 256), "desk": (256, 160), "letter": (256, 160), "verge": (256, 128)}
DOCS_COVER = os.path.join(REPO, "docs", "cover.png")
YARD_BELOW = 60   # the code-drawn yard life needs this much ground under the walls (30 rows at half resolution)

# Per picture: feet of the person, the ground line, the lamp, the player's windowsill (doubled-resolution pixels).
META = {}
try:
    exec(open(os.path.join(HERE, "meta.py"), encoding="utf-8").read())
except FileNotFoundError:
    pass


def magenta(a):
    r, g, b = a[..., 0].astype(int), a[..., 1].astype(int), a[..., 2].astype(int)
    return (r > 150) & (b > 150) & (g < 120) & (abs(r - b) < 100) & (r - g > 90)


def shrink(rgb, opaque, tw, th):
    """Each target pixel takes the dominant colour of its source block; transparent where most of the block is."""
    h, w = opaque.shape
    out = np.zeros((th, tw, 4), np.uint8)
    xs = np.linspace(0, w, tw + 1).astype(int)
    ys = np.linspace(0, h, th + 1).astype(int)
    q = (rgb // 24).astype(np.int32)
    key = q[..., 0] * 4096 + q[..., 1] * 64 + q[..., 2]
    for j in range(th):
        for i in range(tw):
            sl = (slice(ys[j], ys[j + 1]), slice(xs[i], xs[i + 1]))
            op = opaque[sl]
            if op.mean() < .5:
                continue
            k = key[sl][op]
            vals, counts = np.unique(k, return_counts=True)
            best = vals[counts.argmax()]
            px = rgb[sl][op][k == best]
            out[j, i, :3] = px.mean(0).astype(np.uint8)
            out[j, i, 3] = 255
    return out


def sprite(path, name, height=None):
    a = np.asarray(Image.open(path).convert("RGB"))
    op = ~magenta(a)
    # drop specks: keep the biggest blob and anything touching it closely
    lab, n = ndimage.label(ndimage.binary_dilation(op, iterations=6))
    if n > 1:
        sizes = ndimage.sum(op, lab, range(1, n + 1))
        op &= lab == (1 + int(np.argmax(sizes)))
    ys, xs = np.nonzero(op)
    y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
    a, op = a[y0:y1, x0:x1], op[y0:y1, x0:x1]
    h, w = op.shape
    if name == "pryanik":
        tw = CAT_W; th = max(1, round(h * tw / w))
    else:
        th = height or PERSON_H; tw = max(1, round(w * th / h))
    return shrink(a, op, tw, th)


def scene(path, size, ground=None):
    a = np.asarray(Image.open(path).convert("RGB"))
    tw, th = size
    h, w = a.shape[:2]
    if ground is not None:
        # a yard: the whole width, then the ground continued downwards (mirrored) to YARD_BELOW rows under the walls
        th = round(h * tw / w)
        out = scene_fit(a, tw, th)
        need = ground + YARD_BELOW - th
        if need > 0:
            out = np.concatenate([out, out[th - need - 1:th - 1][::-1]], 0)
        return out[:ground + YARD_BELOW]
    return scene_fit(a, tw, th)


def scene_crop(a, tw, th):
    h, w = a.shape[:2]
    cw = min(w, round(h * tw / th)); ch = min(h, round(w * th / tw))
    x0 = (w - cw) // 2; y0 = (h - ch) // 2
    return a[y0:y0 + ch, x0:x0 + cw]


def scene_fit(a, tw, th):
    h, w = a.shape[:2]
    cw = min(w, round(h * tw / th)); ch = min(h, round(w * th / tw))
    x0 = (w - cw) // 2; y0 = h - ch
    a = a[y0:y0 + ch, x0:x0 + cw]
    mg = magenta(a)
    lab, n = ndimage.label(mg)
    top = set(np.unique(lab[0])) - {0}
    sky = np.isin(lab, list(top)) if top else np.zeros_like(mg)
    a = a.copy()
    a[mg & ~sky] = (46, 38, 38)       # magenta seen through an arch or a gap: shadow, not sky
    return shrink(a, ~sky, tw, th)


def windows(img):
    """Numbers each window: slate-grey glass, panes of one window joined, the whole frame inside lit except bright bars."""
    rgb = img[..., :3].astype(int)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    mx = rgb.max(-1); mn = rgb.min(-1)
    sat = (mx - mn) / np.maximum(mx, 1)
    glass = (img[..., 3] > 0) & (b >= r + 5) & (abs(b - g) <= 22) & (mx >= 45) & (mx <= 150) & (sat < .4)
    grp, n = ndimage.label(ndimage.binary_closing(glass, structure=np.ones((3, 3)), iterations=2))
    ids = np.zeros(glass.shape, np.int32)
    lum = rgb.sum(-1) / 3
    k = 0
    for c, sl in enumerate(ndimage.find_objects(grp), 1):
        m = (grp[sl] == c) & glass[sl]
        bh, bw = m.shape
        area = m.sum()
        if 10 <= area and bw <= 40 and bh <= 50 and bw >= 3 and bh >= 4 and area >= .3 * bw * bh:
            k += 1
            box = ids[sl]
            box[(lum[sl] < 160) & (box == 0)] = k
    out = np.zeros(img.shape, np.uint8)
    out[..., 0] = ids >> 8; out[..., 1] = ids & 255; out[..., 3] = 255
    return out, ids


SHEETS = [k for k, _ in item_sheets()]
# yard props, at the yard's doubled resolution: by width or by height
PROP_SIZE = {
    "hen": ("w", 22), "coop": ("w", 48), "nest": ("w", 28), "nest_eggs": ("w", 28), "planter": ("w", 34), "planter_bare": ("w", 34),
    "bench": ("w", 46), "hive": ("w", 15), "garden_bed": ("w", 28), "garden_bed_bare": ("w", 28), "coldframe": ("w", 38),
    "kid": ("h", 30), "sit": ("h", 36), "snowman": ("h", 30), "newyear_tree": ("h", 40), "spring_tools": ("w", 40),
    "barrel": ("w", 16), "feeder": ("h", 40), "tyre": ("w", 28), "gnome": ("h", 26), "birdhouse": ("h", 84), "kennel": ("w", 24), "dog": ("w", 16),
    "wash": ("w", 18),
}
ICON = 32        # an item icon, at the doubled resolution
ICON_SMALL = 18  # the same, small enough to print on a seed packet
ICON_TINY = 9    # over a ripe bed on the map
BED_W = 19       # a bed's wooden box on the map, in map pixels


def fit(a, op, box):
    h, w = op.shape
    k = box / max(h, w)
    tw, th = max(1, round(w * k)), max(1, round(h * k))
    img = shrink(a, op, tw, th)
    out = np.zeros((box, box, 4), np.uint8)
    y0 = (box - th) // 2; x0 = (box - tw) // 2
    out[y0:y0 + th, x0:x0 + tw] = img
    return out


def items(path, keys, group="items"):
    """Cuts a 4x4 sheet into icons: every blob goes to the cell its centre is in."""
    a = np.asarray(Image.open(path).convert("RGB"))
    op = ~magenta(a)
    h, w = op.shape
    lab, n = ndimage.label(ndimage.binary_dilation(op, iterations=3))
    cells = {}
    for c, sl in enumerate(ndimage.find_objects(lab), 1):
        m = (lab[sl] == c) & op[sl]
        if m.sum() < h * w * .001:
            continue
        cy, cx = ndimage.center_of_mass(m)
        i = min(3, int((sl[0].start + cy) / (h / 4))) * 4 + min(3, int((sl[1].start + cx) / (w / 4)))
        cells.setdefault(i, np.zeros_like(op))[sl] |= m
    os.makedirs(os.path.join(OUT, group), exist_ok=True)
    done = 0
    for i, key in enumerate(keys):
        m = cells.get(i)
        if m is None:
            print("  missing", key)
            continue
        ys, xs = np.nonzero(m)
        y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
        sub, mo = a[y0:y1, x0:x1], m[y0:y1, x0:x1]
        if group == "props":
            ribbon = key in {k for k, _ in RIBBONS}
            sub_dir = "ribbons" if ribbon else "yard/props"
            os.makedirs(os.path.join(OUT, sub_dir), exist_ok=True)
            if ribbon:
                img = np.zeros((ICON, ICON, 4), np.uint8); img[1:-1, 1:-1] = fit(sub, mo, ICON - 2)
            else:
                how, n = PROP_SIZE[key.split("_")[0] if key.startswith(("hen_", "sit_", "wash_")) else key]
                h0, w0 = mo.shape
                tw, th = (n, max(1, round(h0 * n / w0))) if how == "w" else (max(1, round(w0 * n / h0)), n)
                img = shrink(sub, mo, tw, th)
            Image.fromarray(img, "RGBA").save(os.path.join(OUT, sub_dir, key + ".png"), optimize=True)
            done += 1
            continue
        if group == "beds":
            # one scale for every bed, so the boxes match on the map: the box is about 245 source pixels wide
            k = BED_W / (a.shape[1] / 4 * .78)
            tw, th = max(1, round(mo.shape[1] * k)), max(1, round(mo.shape[0] * k))
            Image.fromarray(shrink(sub, mo, tw, th), "RGBA").save(os.path.join(OUT, group, key + ".png"), optimize=True)
            done += 1
            continue
        big = fit(sub, mo, ICON - 2)
        icon = np.zeros((ICON, ICON, 4), np.uint8); icon[1:-1, 1:-1] = big
        Image.fromarray(icon, "RGBA").save(os.path.join(OUT, group, key + ".png"), optimize=True)
        if group == "items":
            Image.fromarray(fit(sub, mo, ICON_SMALL), "RGBA").save(os.path.join(OUT, group, key + ".s.png"), optimize=True)
            Image.fromarray(fit(sub, mo, ICON_TINY), "RGBA").save(os.path.join(OUT, group, key + ".t.png"), optimize=True)
        done += 1
    return done


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    rows = []
    only = set(sys.argv[1:])
    for path in sorted(glob.glob(os.path.join(RAW, "*.png"))):
        name = os.path.basename(path)[:-4].replace("__", "/")
        if only and name not in only:
            continue
        group, key = name.split("/")
        os.makedirs(os.path.join(OUT, group), exist_ok=True)
        if group in ("items", "ui", "beds", "props"):
            keys = {"items": SHEETS, "ui": [k for k, _ in ui_sheets()], "beds": [k for k, _ in bed_sheets()], "props": [k for k, _ in prop_sheets()]}[group][int(key[5:])]
            done = items(path, keys, group)
            print(name, f"{done}/{len(keys)}")
            continue
        if group == "intro":
            tw, th = INTRO[key]
            a = scene_crop(np.asarray(Image.open(path).convert("RGB")), tw, th)
            img = shrink(a, np.ones(a.shape[:2], bool), tw, th)
            Image.fromarray(img, "RGBA").save(os.path.join(OUT, group, key + ".png"), optimize=True)
            Image.fromarray(img, "RGBA").resize((tw * 3, th * 3), Image.NEAREST).save(os.path.join(PREVIEW, f"{group}_{key}.png"))
            if key == "cover":
                Image.fromarray(img, "RGBA").convert("RGB").resize((tw * 2, th * 2), Image.NEAREST).save(DOCS_COVER, optimize=True)
            print(name, img.shape)
            continue
        if group == "people":
            img = sprite(path, key)
            if key != "pryanik":
                Image.fromarray(sprite(path, key, MAP_H_HERO if key == "hero" else MAP_H), "RGBA").save(os.path.join(OUT, group, key + ".m.png"), optimize=True)
            Image.fromarray(img, "RGBA").save(os.path.join(OUT, group, key + ".png"), optimize=True)
            Image.fromarray(img, "RGBA").resize((img.shape[1] * 4, img.shape[0] * 4), Image.NEAREST).save(os.path.join(PREVIEW, f"{group}_{key}.png"))
        else:
            img = scene(path, YARD if group == "yard" else PLACE, META.get(name, {}).get("ground") if group == "yard" else None)
            Image.fromarray(img, "RGBA").save(os.path.join(OUT, group, key + ".png"), optimize=True)
            lit, ids = windows(img)
            if ids.max() > 0:
                Image.fromarray(lit, "RGBA").save(os.path.join(OUT, group, key + ".lit.png"), optimize=True)
            pv = img.copy()
            pv[ids > 0, :3] = (255, 200, 40)
            pv[pv[..., 3] == 0] = (255, 0, 255, 255)
            Image.fromarray(pv, "RGBA").resize((img.shape[1] * 3, img.shape[0] * 3), Image.NEAREST).save(os.path.join(PREVIEW, f"{group}_{key}.png"))
            print(name, "windows", int(ids.max()))
    # the metadata table
    cols = ["feetX", "feetY", "ground", "lampX", "lampY", "sillX", "sillY"]
    with open(os.path.join(OUT, "art.tsv"), "w", encoding="utf-8", newline="\n") as f:
        f.write("name\t" + "\t".join(cols) + "\n")
        for name, m in sorted(META.items()):
            f.write(name + "\t" + "\t".join(str(m.get(c, "")) for c in cols) + "\n")


if __name__ == "__main__":
    main()
