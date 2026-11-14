"""Builds the Google Play graphics in docs/google-play/graphics from the demo screenshots.

python tools/play.py

Takes docs/raw/<lang>/<n>-<name>.png (1080x2400 screens of the demo build, -Pdemo -Pshots) and writes the
512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen runs off the bottom edge under
its caption. Captions are in m6x11, the game's own pixel font. Needs `pip install pillow`.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
RAW = ROOT / "docs/raw"
STATUS = 104  # the status bar on the 1080x2400 captures
FONT = ROOT / "app/src/main/res/font/m6x11.ttf"

# The app's own colours: parchment, brown ink, the terracotta button, the kettle green, the lamp.
PAPER = (0xF6, 0xEA, 0xD2)
PAPER_DEEP = (0xE2, 0xCD, 0xA4)
INK = (0x3B, 0x2A, 0x1E)
ACCENT = (0xC4, 0x55, 0x3C)
GREEN = (0x55, 0x7A, 0x45)
GREEN_DEEP = (0x2F, 0x44, 0x30)
LAMP = (0xF5, 0xDC, 0x9C)
FLAGS = [(0xC4, 0x55, 0x3C), (0xF2, 0xBE, 0x5C), (0x55, 0x7A, 0x45), (0x5B, 0x86, 0xC6), (0xE8, 0x9A, 0xB8)]

# The order they appear in the store and the caption: a line in ink and one in terracotta.
PAGES = [
    ("1-street", ("Your real district", "is the farm"), ("Ваш район —", "это ваш огород")),
    ("2-task", ("Walk past a bed", "and it's watered"), ("Прошли мимо грядки —", "она полита")),
    ("3-neighbour", ("22 neighbours with", "three years of stories"), ("22 соседа и их", "истории на три года")),
    ("4-yard", ("Hens, a windowsill", "and a yard of your own"), ("Куры, подоконник", "и свой двор")),
    ("7-neighbours", ("Make friends and", "help with their requests"), ("Подружитесь и", "помогайте соседям")),
    ("5-kitchen", ("95 recipes", "from your harvest"), ("95 рецептов", "из своего урожая")),
    ("8-bag", ("128 varieties", "to grow and collect"), ("128 сортов", "для огорода и коллекции")),
    ("6-household", ("Upgrades for", "your household"), ("Улучшения", "для хозяйства")),
]


def pixel(size):
    # m6x11 is drawn on an 11 px grid of 16 px em: whole multiples keep its pixels square
    return ImageFont.truetype(str(FONT), max(16, size // 16 * 16))


def fit(text, size, width):
    while pixel(size).getlength(text) > width and size > 16:
        size -= 16
    return pixel(size)


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=40, outline=INK):
    """A screen on the board: rounded corners, a soft warm shadow and a thin dark edge, like the app's pictures."""
    sh = Image.new("RGBA", (card.width + 120, card.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((60, 76, 60 + card.width, 76 + card.height), r, fill=(80, 50, 25, 120))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(22)), (x - 60, y - 60))
    g.alpha_composite(rounded(card, r), (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 2, y - 2, x + card.width + 1, y + card.height + 1), r, outline=outline, width=4)


def board(W, H):
    """Parchment with the app's faint dotted weave."""
    g = Image.new("RGBA", (W, H), PAPER + (255,))
    d = ImageDraw.Draw(g)
    step = 36
    for row, y in enumerate(range(step // 2, H, step)):
        for x in range(step // 2 if row % 2 == 0 else step, W, step * 2):
            d.rectangle((x, y, x + 3, y + 3), fill=PAPER_DEEP)
    return g


def bunting(g, W, y, sag=22, step=54, size=22):
    """Fair bunting across the top: a sagging string of little triangular flags."""
    d = ImageDraw.Draw(g)
    span = W / 2
    def at(x):
        t = (x % span) / span
        return y + sag * 4 * t * (1 - t)
    d.line([(x, at(x)) for x in range(0, W + 1, 4)], fill=INK, width=3)
    for i, x in enumerate(range(step // 2, W, step)):
        top = at(x)
        d.polygon([(x - size // 2, top), (x + size // 2, top), (x, top + size * 1.3)], fill=FLAGS[i % len(FLAGS)], outline=INK)


def screenshot(src, caption):
    W, H = 1080, 1920
    g = board(W, H)
    bunting(g, W, 18)
    d = ImageDraw.Draw(g)
    d.text((W / 2, 170), caption[0], font=fit(caption[0], 80, W - 100), fill=INK, anchor="mm")
    d.text((W / 2, 262), caption[1], font=fit(caption[1], 80, W - 100), fill=ACCENT, anchor="mm")
    im = Image.open(src).convert("RGB")
    # the phone's status bar goes: its clock and other apps' icons are not Green Street's
    im = im.crop((0, STATUS, im.width, im.height))
    w = 880
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 340
    # only the top corners show: the screen runs on past the bottom edge
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    """The painted cover across the banner, darkened to the left under the name and the line."""
    W, H = 1024, 500
    cover = Image.open(ROOT / "docs/cover.png").convert("RGB")
    k = max(W / cover.width, H / cover.height)
    cover = cover.resize((round(cover.width * k), round(cover.height * k)), Image.NEAREST)
    x0 = cover.width - W
    g = cover.crop((x0, (cover.height - H) // 2, x0 + W, (cover.height - H) // 2 + H)).convert("RGBA")
    shade = Image.new("L", (W, H), 0)
    sd = ImageDraw.Draw(shade)
    for x in range(W):
        sd.line([(x, 0), (x, H)], fill=max(0, min(235, int(300 * (1 - x / (W * .66))))))
    g.paste(Image.new("RGBA", (W, H), GREEN_DEEP + (255,)), (0, 0), shade)
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((128, 128), Image.NEAREST)
    g.alpha_composite(logo, (56, 70))
    d = ImageDraw.Draw(g)
    name = "Green Street" if lang == "en" else "Зелёная улица"
    lines = ("A cozy farm on the map", "of your own district") if lang == "en" else ("Уютная ферма на карте", "вашего района")
    def shadowed(xy, text, font, fill):
        d.text((xy[0] + 4, xy[1] + 4), text, font=font, fill=(20, 28, 18))
        d.text(xy, text, font=font, fill=fill)
    shadowed((58, 228), name, fit(name, 80, 520), PAPER)
    d.rectangle((60, 322, 260, 327), fill=ACCENT)
    f = min((fit(l, 48, 480) for l in lines), key=lambda x: x.size)
    for i, l in enumerate(lines):
        shadowed((60, 346 + i * 50), l, f, PAPER if i == 0 else LAMP)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # Play rounds the icon itself: the full square, no transparent corners
    bg = Image.open(ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_bg.png").convert("RGBA")
    fg = Image.open(ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_fg.png").convert("RGBA")
    Image.alpha_composite(bg, fg).resize((512, 512), Image.NEAREST).convert("RGB").save(OUT / "icon-512.png", optimize=True)
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(RAW / lang / f"{name}.png", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
