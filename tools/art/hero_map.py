"""The player on the map, drawn by hand pixel by pixel: at 18 pixels tall a shrunk portrait loses the face, and the
player has to be told apart from the neighbours at a glance. Writes assets/art/people/hero.m.png (process.py leaves it be)."""
import os

from PIL import Image
rows = [
"...PP....",
"...rr....",
"..rRrRr..",
"..rrRrr..",
".rRrrrRr.",
".kkkkkkk.",
".hsssssh.",
".hsesesh.",
"..sssss..",
".yyyyyyy.",
"gGyyGGGGg",
"gGGyGGGBb",
"sGGyGGGBb",
".GGGGGGBb",
".GgGGgGG.",
"..ttLtt..",
"..tt.tt..",
".bbb.bbb.",
]
pal = {
 'P': (238,232,214), 'r': (196,52,42), 'R': (226,88,62), 'k': (130,30,28),
 'h': (110,58,30), 's': (242,184,138), 'e': (43,26,20),
 'y': (240,176,48), 'G': (62,112,74), 'g': (40,78,52),
 'B': (226,212,166), 'b': (150,120,80),
 't': (92,52,40), 'L': (70,38,30),
}
w = len(rows[0]); h = len(rows)
im = Image.new('RGBA', (w, h), (0,0,0,0))
for y, row in enumerate(rows):
    assert len(row) == w, (y, row)
    for x, ch in enumerate(row):
        if ch != '.':
            c = pal[ch]
            # boots: last row uses dark brown
            if y == h-1: c = (58,36,24)
            im.putpixel((x, y), c + (255,))
im.save(os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "app", "src", "main", "assets", "art", "people", "hero.m.png"), optimize=True)
