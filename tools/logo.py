"""Renders the adaptive launcher icon (background + foreground vectors, all plain rectangles) to docs/logo.png."""
import re
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"
S = 8  # pixels per viewport unit, 108 * 8 = 864


def paint(img, xml):
    d = ImageDraw.Draw(img)
    for color, data in re.findall(r'fillColor="#([0-9A-Fa-f]{8})"\s+android:pathData="([^"]+)"', xml):
        rgba = tuple(int(color[i:i + 2], 16) for i in (2, 4, 6, 0))
        for x, y, w, h in re.findall(r"M([\d.]+),([\d.]+)h([\d.]+)v([\d.]+)", data):
            x, y, w, h = map(float, (x, y, w, h))
            d.rectangle([round(x * S), round(y * S), round((x + w) * S) - 1, round((y + h) * S) - 1], fill=rgba)


img = Image.new("RGBA", (108 * S, 108 * S))
paint(img, (RES / "ic_launcher_background.xml").read_text(encoding="utf-8"))
paint(img, (RES / "ic_launcher_foreground.xml").read_text(encoding="utf-8"))
# the launcher shows the middle 72 of 108 units
c = 18 * S
img = img.crop((c, c, 108 * S - c, 108 * S - c)).resize((512, 512), Image.NEAREST)
mask = Image.new("L", (512, 512), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, 511, 511], radius=112, fill=255)
out = Image.new("RGBA", (512, 512))
out.paste(img, (0, 0), mask)
(ROOT / "docs").mkdir(exist_ok=True)
out.save(ROOT / "docs/logo.png", optimize=True)
print("docs/logo.png")
