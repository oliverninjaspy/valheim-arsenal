"""
Procedural 16x16 pixel art in the vanilla Minecraft item style.

Each sprite is composed on a 16x16 canvas of "parts" (metal head, wooden shaft,
leather grip, glowing accent, bowstring). Long shapes are laid along the vanilla
tool diagonal (handle bottom-left, business end top-right). Shapes are described
in diagonal coordinates:

    u = x - y         runs from the bottom-left corner (-15) to the top-right corner (15)
    v = x + y - 15    runs across the diagonal; negative = top-left side, positive = bottom-right

Each part is shaded from its own 5-tone palette (lit from the top left, as in
vanilla) and finished with an outline in that palette's darkest tone.

All art here is original.
"""
import math
import os

from PIL import Image

# ---------------------------------------------------------------------------
# Palettes: outline, dark, mid, light, highlight
# ---------------------------------------------------------------------------
P = {
    "iron":        ["#2c2e33", "#6c7076", "#a6aaae", "#d3d6d8", "#f5f6f6"],
    "bronze":      ["#3a210d", "#87491b", "#bf7530", "#e1a457", "#f7d48f"],
    "copper":      ["#3b1c10", "#8c4528", "#c3683d", "#e8925f", "#fac39c"],
    "silver":      ["#363c49", "#8a93a6", "#bac4d5", "#e2e8f2", "#ffffff"],
    "blackmetal":  ["#0c0d10", "#25282f", "#3c414c", "#5d6574", "#8f98aa"],
    "flametal":    ["#2b0e04", "#7d2c10", "#bd5419", "#ec8c2c", "#ffd37a"],
    "flint":       ["#1b1c21", "#3c3f49", "#5c616f", "#848b99", "#aeb5c1"],
    "stone":       ["#262626", "#575757", "#7b7b7b", "#9d9d9d", "#c2c2c2"],
    "frost":       ["#173149", "#3a73a2", "#6bafd9", "#a9def5", "#ecfcff"],
    "mist":        ["#262d45", "#58678c", "#8ea2c7", "#c5d5f0", "#f2f7ff"],
    "chitin":      ["#18272b", "#3a6064", "#5e9890", "#9dcfbe", "#e2f6ea"],
    "fang":        ["#3a3125", "#8b7e64", "#c8bc9f", "#e9e1ca", "#fcf9ef"],
    "bone":        ["#3a3125", "#8b7e64", "#c8bc9f", "#e9e1ca", "#fcf9ef"],
    "carapace":    ["#170f14", "#382830", "#5a414f", "#876677", "#be98ab"],
    "crystal":     ["#1d3b52", "#4a8ab2", "#83c3e6", "#c3ecfb", "#ffffff"],
    "serpent":     ["#0f2427", "#1d4a4d", "#2c726c", "#4ca18c", "#8fd5b4"],
    "oak":         ["#382812", "#694823", "#9a713f", "#c3995b", "#e0be86"],
    "spruce":      ["#2a1c0e", "#4d3317", "#714e25", "#986c39", "#bd9056"],
    "dark_oak":    ["#211407", "#3d2911", "#5b3e1d", "#7c592e", "#a07a46"],
    "birch":       ["#47402f", "#a99c74", "#d3c9a1", "#ece5c6", "#fffbea"],
    "ashwood":     ["#1a0d0a", "#391d16", "#5a2e21", "#834533", "#b06549"],
    "yggdrasil":   ["#131820", "#283241", "#3d4d61", "#5a6f87", "#8aa3bd"],
    "ancient_bark": ["#1c190f", "#3a3422", "#5a5134", "#7e734b", "#a6996b"],
    "leather":     ["#2a180c", "#573318", "#7d4c26", "#a46d3d", "#c99363"],
    "linen":       ["#383226", "#8a7f64", "#b7ac8d", "#dcd2b4", "#f3ecd7"],
    "antler":      ["#3a2e1f", "#7d6a4e", "#b29c78", "#d6c4a0", "#efe3c8"],
    # glowing accents
    "ice":         ["#173149", "#3f78a6", "#7fc6ec", "#c8f2ff", "#ffffff"],
    "eitr":        ["#1d1240", "#3b2a7a", "#6a52d6", "#a796ff", "#ebe5ff"],
    "fire":        ["#4a0d03", "#a3260b", "#f2641b", "#ffb23f", "#fff1a8"],
    "ember":       ["#3a0e04", "#8a1f08", "#d2440f", "#ff8a2a", "#ffd27a"],
    "poison":      ["#17290b", "#2e5216", "#5e9e2c", "#9be05a", "#e3ffb0"],
    "lightning":   ["#1d2d63", "#3e5fbf", "#7fb4ff", "#c8e4ff", "#ffffff"],
    "wisp":        ["#22344a", "#4b6a8c", "#9ad4ff", "#d6f3ff", "#ffffff"],
    "blood":       ["#260509", "#5a0d16", "#9c1a2b", "#d8364d", "#ff8a9a"],
    "leaf":        ["#17300c", "#2f5a1c", "#4f8f2c", "#86c94a", "#c9f28a"],
    "grausten":    ["#0e0d10", "#2a2930", "#46444e", "#67646f", "#918e9b"],
    "red_paint":   ["#2e0707", "#5a1010", "#9a1c1c", "#c83a2a", "#e86a50"],
    "wolf_blue":   ["#0f1830", "#1e2f5a", "#33508f", "#5a7cc0", "#9ab4e6"],
    "linen_paint": ["#383226", "#6b6350", "#b8ad8e", "#ddd3b5", "#f3ecd6"],
    "oak_dark":    ["#211407", "#3d2911", "#5b3e1d", "#7c592e", "#a07a46"],
    "nidhogg":     ["#1c0505", "#4a1210", "#7a2219", "#a8402a", "#d97a52"],
    "string":      ["#2f2f2f", "#8e8e8e", "#c9c9c9", "#e9e9e9", "#ffffff"],
}
ALIASES = {"iron_guard": "iron", "silver_gem": "wolf_blue", "silver_bind": "silver", "copper_band": "copper",
           "iron_band": "iron"}

NOT_OUTLINED = {"s"}
OUTLINE_PRIORITY = ["m", "a", "h", "g", "s"]


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def pal(key):
    key = ALIASES.get(key, key)
    return [hexrgb(c) for c in P[key]]


def uv_cells():
    for y in range(16):
        for x in range(16):
            yield x, y, x - y, x + y - 15


def to_xy(u, v):
    """Diagonal coordinates back to the nearest pixel."""
    return int(round((15 + v + u) / 2.0)), int(round((15 + v - u) / 2.0))


# ---------------------------------------------------------------------------
# Canvas
# ---------------------------------------------------------------------------
class Canvas:
    def __init__(self):
        self.cells = [[None] * 16 for _ in range(16)]

    def set(self, x, y, part, shade):
        if 0 <= x < 16 and 0 <= y < 16:
            self.cells[y][x] = (part, shade)

    def get(self, x, y):
        if 0 <= x < 16 and 0 <= y < 16:
            return self.cells[y][x]
        return None

    def fill(self, part, cond, shade, keep=False):
        """Fill pixels where cond(u, v) holds; shade is an int or a function of (u, v)."""
        for x, y, u, v in uv_cells():
            if cond(u, v):
                if keep and self.cells[y][x] is not None:
                    continue
                self.cells[y][x] = (part, shade(u, v) if callable(shade) else shade)

    def line(self, x0, y0, x1, y1, part, shade):
        """Bresenham line in pixel space (used for bowstrings and arrows)."""
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, part, shade)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def stamp(self, x0, y0, rows, legend=None):
        legend = legend or STD
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch in legend:
                    if legend[ch] is None:
                        if 0 <= x0 + dx < 16 and 0 <= y0 + dy < 16:
                            self.cells[y0 + dy][x0 + dx] = None
                    else:
                        self.set(x0 + dx, y0 + dy, *legend[ch])

    def overlay(self, other, darken=0):
        for y in range(16):
            for x in range(16):
                cell = other.cells[y][x]
                if cell is not None and self.cells[y][x] is None:
                    self.cells[y][x] = (cell[0], max(1, cell[1] - darken) if cell[1] else 0)

    def shifted(self, dx, dy):
        out = Canvas()
        for y in range(16):
            for x in range(16):
                if self.cells[y][x] is not None and 0 <= x + dx < 16 and 0 <= y + dy < 16:
                    out.cells[y + dy][x + dx] = self.cells[y][x]
        return out

    def mirror(self):
        self.cells = [list(reversed(row)) for row in self.cells]

    def outline(self):
        add = []
        for y in range(16):
            for x in range(16):
                if self.cells[y][x] is not None:
                    continue
                parts = []
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    c = self.get(nx, ny)
                    if c is not None and c[1] != 0 and c[0] not in NOT_OUTLINED:
                        parts.append(c[0])
                if parts:
                    add.append((x, y, min(parts, key=OUTLINE_PRIORITY.index)))
        for x, y, part in add:
            self.cells[y][x] = (part, 0)

    def render(self, palettes):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                c = self.cells[y][x]
                if c is not None:
                    img.putpixel((x, y), palettes[c[0]][c[1]])
        return img


STD = {
    "O": ("m", 0), "1": ("m", 1), "2": ("m", 2), "3": ("m", 3), "4": ("m", 4),
    "o": ("h", 0), "a": ("h", 1), "b": ("h", 2), "c": ("h", 3), "d": ("h", 4),
    "g": ("g", 1), "G": ("g", 3), "q": ("g", 0),
    "X": ("a", 0), "x": ("a", 1), "y": ("a", 2), "z": ("a", 3), "w": ("a", 4),
    "s": ("s", 2), "_": None,
}

# ---------------------------------------------------------------------------
# Building blocks (all in diagonal coordinates)
# ---------------------------------------------------------------------------


def shaft(c, u0, u1, thick=True, part="h"):
    """A wooden shaft. Thick = 2 px per row (light top-left edge, dark bottom-right edge)."""
    if thick:
        c.fill(part, lambda u, v: u0 <= u <= u1 and -1 <= v <= 0, lambda u, v: 3 if v == -1 else 1)
    else:
        c.fill(part, lambda u, v: u0 <= u <= u1 and v == 0, lambda u, v: 3 if (u + 1) % 4 == 0 else 2)


def grip(c, u0, u1, thick=True):
    """Leather wrap: alternating light/dark turns."""
    c.fill("g", lambda u, v: u0 <= u <= u1 and ((-1 <= v <= 0) if thick else v == 0),
           lambda u, v: 3 if (u // 2) % 2 == 0 else 1)


def knob(c, u, part="m"):
    c.fill(part, lambda uu, v: u - 1 <= uu <= u and -1 <= v <= 0, lambda uu, v: 3 if v == -1 else 1)


def blade(c, u0, u1, v0, v1, tip=3, part="m", fuller=False, sparkle=True):
    """Straight blade from u0 to u1 between v0 and v1, tapering to a point over `tip` steps."""
    def inside(u, v):
        if u < u0 or u > u1:
            return False
        lo, hi = v0, v1
        if u > u1 - tip:
            k = u - (u1 - tip)
            lo, hi = v0 + (k + 1) // 2, v1 - k // 2
        return lo <= v <= hi

    def shade(u, v):
        if v == v0:
            return 4 if (sparkle and u % 4 == 1) else 3
        if v == v1:
            return 1
        if fuller and v1 - v0 >= 2 and v == (v0 + v1) // 2 + (1 if v1 - v0 > 2 else 0) and u < u1 - tip:
            return 1
        return 2
    c.fill(part, inside, shade)


def crossguard(c, u, half, part="m", depth=1):
    c.fill(part, lambda uu, v: u - depth + 1 <= uu <= u and -half <= v <= half,
           lambda uu, v: 3 if v < -1 else (2 if v <= 1 else 1))


def axe_head(c, uc, reach=6, part="m", side=-1, flare=True, poll=True):
    """An axe bit on one side of the shaft (side=-1: top-left). The edge flares like a bearded axe."""
    def width(d):  # d = distance from the shaft, 2..reach
        if not flare:
            return 2
        return 1 + (d * 3) // reach

    def inside(u, v):
        d = v * side
        if d < 2 or d > reach:
            return False
        w = width(d)
        return uc - w <= u <= uc + w

    def shade(u, v):
        d = v * side
        if d == reach:
            return 4 if u % 2 == 0 else 3
        if d == reach - 1:
            return 3
        if u >= uc + width(d) - 0 and side < 0:
            return 3
        return 2 if d > 3 else 1
    c.fill(part, inside, shade)
    if poll:
        c.fill(part, lambda u, v: uc - 1 <= u <= uc + 1 and 1 <= v * -side <= 2,
               lambda u, v: 2 if u > uc - 1 else 1)


def hammer_head(c, uc, half_len=5, half_thick=2, part="m"):
    """A sledge block whose long axis crosses the shaft."""
    def inside(u, v):
        if abs(u - uc) > half_thick or abs(v) > half_len:
            return False
        # bevel the four corners
        return not (abs(u - uc) == half_thick and abs(v) == half_len)

    def shade(u, v):
        if u - uc == half_thick or v == -half_len:
            return 4 if (v == -half_len and u - uc == half_thick - 1) else 3
        if u - uc == -half_thick or v == half_len:
            return 1
        return 2
    c.fill(part, inside, shade)


def ball_head(c, cx, cy, r, part="m", spikes=None, spike_part=None):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                # light from the top-left
                lx = (x - cx) + (y - cy)
                if d >= r - 0.6 and lx > 0.5:
                    shade = 1
                elif lx < -r * 0.9:
                    shade = 4
                elif lx < -r * 0.2:
                    shade = 3
                elif lx < r * 0.7:
                    shade = 2
                else:
                    shade = 1
                c.set(x, y, part, shade)
    for (x, y) in spikes or []:
        c.set(x, y, spike_part or part, 3)


# ---------------------------------------------------------------------------
# Templates
# ---------------------------------------------------------------------------


def t_sword(c, fancy=False, long=False, guard_part=None):
    grip(c, -12, -8)
    knob(c, -13, part=guard_part or "m")
    blade(c, -6, 15 if long else 13, -1, 1 if long else 0, tip=3, fuller=long)
    crossguard(c, -7, 3, part=guard_part or "m", depth=1)
    if fancy:
        c.set(*to_xy(-7, 0), "a", 4)


def t_dyrnwyn(c):
    t_sword(c, fancy=True, long=True, guard_part="a")
    # a flame runs along the fuller
    c.fill("a", lambda u, v: -4 <= u <= 11 and v == 0, lambda u, v: 4 if u % 3 == 0 else (3 if u % 3 == 1 else 2))


def t_greatsword(c):
    grip(c, -15, -9)
    knob(c, -16, part="a")
    blade(c, -6, 15, -1, 1, tip=4, fuller=True)
    crossguard(c, -8, 4, part="a", depth=2)


def t_knife(c, guard=True):
    grip(c, -11, -6)
    knob(c, -12)
    blade(c, -4, 6, -1, 0, tip=3)
    if guard:
        crossguard(c, -5, 2, depth=1)


def t_razor(c):
    grip(c, -11, -6)
    knob(c, -12)
    # broad, slightly hooked nacre blade
    blade(c, -5, 7, -2, 0, tip=4)
    c.fill("m", lambda u, v: 4 <= u <= 6 and v == -3, 4)


def t_dual_knives(c):
    skoll = Canvas()
    t_knife(skoll)
    skoll.fill("a", lambda u, v: -3 <= u <= 4 and v == -1, lambda u, v: 4 if u % 3 == 0 else 3)
    hati = Canvas()
    t_knife(hati)
    c.overlay(skoll.shifted(-2, -2))
    c.overlay(hati.shifted(2, 2), darken=1)


def t_cleaver(c):
    grip(c, -11, -5)
    knob(c, -12)
    c.fill("m", lambda u, v: -4 <= u <= 8 and -5 <= v <= 0 and not (u >= 6 and v <= -4),
           lambda u, v: 4 if (v == -5 and u % 3 == 0) else (3 if v <= -4 else (2 if v <= -1 else 1)))
    # rivets
    c.set(*to_xy(-3, -1), "h", 3)


def t_club(c):
    shaft(c, -14, 0)
    c.fill("m", lambda u, v: 1 <= u <= 14 and -2 <= v <= 1 - (1 if u > 12 else 0) and not (u == 14 and v != -1),
           lambda u, v: 3 if v <= -2 else (2 if v <= 0 else 1))
    c.fill("m", lambda u, v: 5 <= u <= 12 and -3 <= v <= 2, lambda u, v: 3 if v < -1 else (2 if v < 1 else 1), keep=True)
    for u, v in [(6, -3), (9, 2), (11, -3)]:
        c.set(*to_xy(u, v), "m", 1)
    for u, v in [(8, -2), (12, -1)]:
        c.set(*to_xy(u, v), "m", 4)


def t_mace(c, flanged=True):
    grip(c, -14, -9)
    knob(c, -15)
    shaft(c, -8, 6)
    ball_head(c, 10.5, 4.5, 3.2)
    if flanged:
        for (x, y) in [(10, 0), (15, 5), (6, 4), (11, 9)]:
            c.set(x, y, "m", 3)


def t_frostner(c):
    grip(c, -14, -9)
    knob(c, -15)
    shaft(c, -8, 5)
    ball_head(c, 10.5, 4.5, 3.4, part="a")
    for (x, y) in [(10, 0), (15, 4), (6, 5), (11, 9), (13, 1), (7, 2)]:
        c.set(x, y, "a", 4)
    for (x, y) in [(9, 3), (11, 5), (10, 4)]:
        c.set(x, y, "a", 4 if (x + y) % 2 else 3)


def t_porcupine(c):
    grip(c, -14, -9)
    knob(c, -15)
    shaft(c, -8, 5)
    ball_head(c, 10.5, 4.5, 3.0)
    spikes = [(10, 0), (15, 4), (6, 5), (11, 9), (14, 1), (7, 1), (14, 8), (13, 0)]
    for (x, y) in spikes:
        c.set(x, y, "a", 3)


def t_sledge(c):
    grip(c, -15, -11)
    shaft(c, -10, 4)
    hammer_head(c, 6, half_len=5, half_thick=3)


def t_demolisher(c):
    t_sledge(c)
    # eitr-lit runes on the head
    for u, v in [(6, -3), (6, -1), (6, 1), (6, 3), (7, 0), (5, 0)]:
        c.set(*to_xy(u, v), "a", 3)
    c.set(*to_xy(6, -1), "a", 4)


def t_stagbreaker(c):
    shaft(c, -15, 6)
    # a heavy wooden mallet...
    hammer_head(c, 8, half_len=4, half_thick=2)
    # ...with antler tines bound to it
    for u, v in [(10, -6), (11, -7), (12, -6), (10, 6), (11, 7), (12, 6), (13, -7), (13, 7)]:
        c.set(*to_xy(u, v), "a", 3)
    c.fill("g", lambda u, v: 6 <= u <= 6 and -3 <= v <= 3, 1)


def t_axe(c, small=False, reach=6):
    grip(c, -15, -12)
    shaft(c, -11, 10 if not small else 8)
    axe_head(c, 7 if not small else 6, reach=reach if not small else 5, poll=not small)


def t_jotun_bane(c):
    t_axe(c)
    for u, v in [(7, -6), (6, -5), (8, -5), (7, -4)]:
        c.set(*to_xy(u, v), "a", 3)
    c.set(*to_xy(5, -4), "a", 4)


def t_dual_axes(c):
    front = Canvas()
    t_axe(front, small=True)
    for u, v in [(6, -4), (6, -3)]:
        front.set(*to_xy(u, v), "a", 3)
    back = Canvas()
    t_axe(back, small=True)
    c.overlay(front.shifted(-2, 1))
    c.overlay(back.shifted(2, 3), darken=1)


def t_battleaxe(c, crystal=False):
    grip(c, -15, -11, thick=False)
    shaft(c, -10, 12, thick=False)
    axe_head(c, 8, reach=6, side=-1, poll=False)
    axe_head(c, 8, reach=5, side=1, poll=False)
    c.fill("m", lambda u, v: 7 <= u <= 9 and -1 <= v <= 1, lambda u, v: 1)
    if crystal:
        for u, v in [(8, -6), (7, -5), (9, -5), (8, 5)]:
            c.set(*to_xy(u, v), "a", 4)
        for u, v in [(8, -4), (8, 4), (6, -4)]:
            c.set(*to_xy(u, v), "a", 3)


def t_spear(c, long=False):
    shaft(c, -15, 5 if not long else 7, thick=False)
    grip(c, -8, -5, thick=False)
    start = 6 if not long else 8
    blade(c, start, 15, -1, 1, tip=4)
    crossguard(c, start - 1, 1, part="a" if long else "m")


def t_fang_spear(c):
    shaft(c, -15, 6, thick=False)
    c.fill("a", lambda u, v: 4 <= u <= 6 and -1 <= v <= 1, lambda u, v: 3 if v < 0 else 1)
    # a curved fang
    blade(c, 7, 15, -1, 1, tip=5, sparkle=False)
    c.set(*to_xy(13, -2), "m", 3)
    c.set(*to_xy(11, -2), "m", 3)


def t_carapace_spear(c):
    shaft(c, -15, 4, thick=False)
    blade(c, 5, 15, -2, 1, tip=5)
    c.fill("a", lambda u, v: 3 <= u <= 4 and -1 <= v <= 1, lambda u, v: 3 if v < 0 else 2)
    c.set(*to_xy(9, -1), "a", 4)


def t_splitnir(c):
    shaft(c, -15, 5, thick=False)
    blade(c, 6, 15, -1, 1, tip=4)
    # side prongs
    c.fill("m", lambda u, v: 4 <= u <= 7 and v in (-3, 3) and not (u == 4), lambda u, v: 3 if v < 0 else 1)
    c.fill("m", lambda u, v: u == 4 and -3 <= v <= 3, 2)
    c.set(*to_xy(9, 0), "a", 4)


def t_atgeir(c, accent=False):
    shaft(c, -15, 3, thick=False)
    grip(c, -9, -6, thick=False)
    # long glaive blade, broader than a spear
    blade(c, 4, 15, -2, 1, tip=5)
    # the forward-curving hook on the back of the blade
    for u, v in [(4, 2), (5, 3), (6, 3), (7, 4)]:
        c.set(*to_xy(u, v), "m", 2 if v < 4 else 3)
    c.fill("m", lambda u, v: 3 <= u <= 6 and 2 <= v <= 3, lambda u, v: 2 if v == 2 else 1, keep=True)
    if accent:
        c.fill("a", lambda u, v: 6 <= u <= 12 and v == -1, lambda u, v: 4 if u % 3 == 0 else 3)


def t_claws(c):
    # three thin hooked blades rising from a leather wrist wrap
    for off in (-5, 0, 5):
        top = 10 - abs(off)
        c.fill("m", lambda u, v, off=off, top=top: -5 <= u <= top and v == off,
               lambda u, v: 4 if u % 3 == 0 else 3)
        c.set(*to_xy(top + 1, off - 1), "m", 3)
    c.fill("g", lambda u, v: -9 <= u <= -6 and -7 <= v <= 7, lambda u, v: 3 if u == -6 else 1)
    c.fill("a", lambda u, v: u == -7 and -6 <= v <= 6 and v % 2 == 0, 3)


def t_bow(c, recurve=False, pull=0):
    # limb centre-line bulges toward the top-left
    def limb_v(u):
        t = u / 13.0
        bend = -5.0 * (1 - t * t)
        if recurve and abs(u) >= 10:
            bend += 1.2 * (abs(u) - 9) / 3.0
        return bend

    for x, y, u, v in uv_cells():
        if -13 <= u <= 13:
            lv = limb_v(u)
            if lv - 1.0 <= v <= lv + 0.6:
                c.set(x, y, "h", 3 if v < lv - 0.2 else 1)
    # grip wrap
    c.fill("g", lambda u, v: -2 <= u <= 2 and -6 <= v <= -3 and abs(v - limb_v(u)) <= 1.2,
           lambda u, v: 3 if u % 2 == 0 else 1)
    # tips
    tip_a = to_xy(-13, round(limb_v(-13)))
    tip_b = to_xy(13, round(limb_v(13)))
    if pull == 0:
        c.line(*tip_a, *tip_b, "s", 3)
    else:
        nock_v = [0, 1, 2, 3][pull]
        nock = to_xy(-1, nock_v) if nock_v % 2 == 0 else to_xy(0, nock_v)
        c.line(*tip_a, *nock, "s", 3)
        c.line(*nock, *tip_b, "s", 3)
        # arrow: from the nock toward the top-left, perpendicular to the string
        head_v = -8
        start = nock
        end = to_xy(0 if nock_v % 2 else -1, head_v)
        c.line(*start, *end, "a", 2)
        c.set(*end, "a", 4)
        c.set(end[0] - 1, end[1], "a", 3)
        c.set(end[0], end[1] - 1, "a", 3)


def t_crossbow(c, state="idle"):
    # stock along the diagonal with a heavier butt
    grip(c, -12, -9)
    shaft(c, -8, 6)
    c.fill("h", lambda u, v: -15 <= u <= -13 and -2 <= v <= 1, lambda u, v: 3 if v < 0 else 1)
    c.fill("h", lambda u, v: -6 <= u <= -5 and 1 <= v <= 2, 1)  # trigger
    pull = {"idle": 0, "pulling_0": 1, "pulling_1": 2, "pulling_2": 3, "arrow": 3}[state]

    # prod: a short bow across the front of the stock, arms swept back
    def prod_u(v):
        return 6 - (v * v) / 10.0

    for x, y, u, v in uv_cells():
        if -7 <= v <= 7 and -0.6 <= u - prod_u(v) <= 0.6:
            c.set(x, y, "m", 4 if v < -3 else (3 if v < 2 else 2))
    c.fill("m", lambda u, v: 5 <= u <= 7 and -1 <= v <= 0, 2)
    tip_a = to_xy(1, -7)
    tip_b = to_xy(1, 7)
    if pull == 0:
        c.line(*tip_a, *tip_b, "s", 3)
    else:
        nock = to_xy([0, -1, -3, -5][pull], 0)
        c.line(*tip_a, *nock, "s", 3)
        c.line(*nock, *tip_b, "s", 3)
    if state == "arrow":
        c.fill("a", lambda u, v: v == -2 and -5 <= u <= 10, lambda u, v: 4 if u >= 9 else 2)


def t_staff(c, head):
    shaft(c, -15, 7)
    grip(c, -9, -6)
    if head == "orb":
        ball_head(c, 12, 3, 2.2, part="a")
        for (x, y) in [(9, 2), (12, 6), (14, 1)]:
            c.set(x, y, "h", 2)
    elif head == "ring":
        for x in range(16):
            for y in range(16):
                d = math.hypot(x - 12, y - 3)
                if 1.8 <= d <= 2.9:
                    c.set(x, y, "h", 3 if x + y < 15 else 1)
        ball_head(c, 12, 3, 1.1, part="a")
    elif head == "fork":
        c.fill("h", lambda u, v: 7 <= u <= 11 and v in (-3, 3), lambda u, v: 3 if v < 0 else 1)
        c.fill("h", lambda u, v: u == 7 and -3 <= v <= 3, 2)
        c.fill("a", lambda u, v: 9 <= u <= 14 and v == 0, lambda u, v: 4 if u % 2 else 3)
        c.set(*to_xy(12, -2), "a", 3)
        c.set(*to_xy(11, 2), "a", 3)
    elif head == "root":
        c.fill("h", lambda u, v: 7 <= u <= 12 and v in (-2, 2) and (u + v) % 2, 2)
        c.fill("h", lambda u, v: 8 <= u <= 13 and v in (-4, 4) and u > 9, 1)
        ball_head(c, 12, 3, 1.6, part="a")
    elif head == "rock":
        ball_head(c, 12, 3, 2.5, part="a")
        for (x, y) in [(11, 2), (13, 4)]:
            c.set(x, y, "a", 4)


def round_shield(c):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 6.6:
                lx = (x - 7.5) + (y - 7.5)
                shade = 3 if lx < -5 else (2 if lx < 5 else 1)
                if d >= 5.6:  # rim
                    c.set(x, y, "h", 3 if lx < 0 else 1)
                else:
                    c.set(x, y, "m", shade)
    # wood grain / planks
    for x in (5, 10):
        for y in range(3, 13):
            cell = c.get(x, y)
            if cell and cell[0] == "m":
                c.set(x, y, "m", max(1, cell[1] - 1))
    boss(c)


def boss(c, cx=7.5, cy=7.5):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 1.6:
                c.set(x, y, "h", 4 if (x < cx and y < cy) else 2)


def t_round_shield(c, pattern=None):
    round_shield(c)
    if pattern:
        for y in range(16):
            for x in range(16):
                cell = c.get(x, y)
                if cell is None or cell[0] != "m":
                    continue
                paint = False
                if pattern == "halves":
                    paint = x > y
                elif pattern == "cross":
                    paint = x in (7, 8) or y in (7, 8)
                elif pattern == "ring":
                    d = math.hypot(x - 7.5, y - 7.5)
                    paint = 3.0 <= d <= 4.4
                elif pattern == "quarters":
                    paint = (x < 8) == (y < 8)
                if paint:
                    c.set(x, y, "a", cell[1] + (1 if cell[1] < 3 else 0))
        boss(c)


def t_banded_shield(c):
    round_shield(c)
    for x, y, u, v in uv_cells():
        cell = c.get(x, y)
        if cell and cell[0] == "m" and (y in (4, 11) or x in (4, 11)):
            c.set(x, y, "h", 2)
    boss(c)


def t_buckler(c):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 5.0:
                lx = (x - 7.5) + (y - 7.5)
                ring = d >= 4.1
                shade = (4 if lx < -6 else 3) if lx < -3 else (2 if lx < 4 else 1)
                c.set(x, y, "m", max(1, shade - (1 if ring and lx > 0 else 0)))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 2.6 <= d <= 3.2:
                cell = c.get(x, y)
                c.set(x, y, "m", 1 if (x + y) > 15 else 3)
    boss(c)


def t_tower_shield(c):
    for y in range(1, 16):
        for x in range(3, 13):
            if y >= 12 and (x - 3 < y - 12 or 12 - x < y - 12):
                continue
            lx = x - 3
            if x in (3, 12) or y == 1:
                c.set(x, y, "h", 3 if x == 3 or y == 1 else 1)
                continue
            shade = 3 if lx <= 2 else (2 if lx <= 6 else 1)
            c.set(x, y, "m", shade)
    for y in range(2, 14):
        for x in (6, 9):
            cell = c.get(x, y)
            if cell and cell[0] == "m":
                c.set(x, y, "m", max(1, cell[1] - 1))
    for (x, y) in [(5, 3), (10, 3), (5, 11), (10, 11)]:
        c.set(x, y, "h", 4)
    boss(c, 7.5, 7.0)


def t_scale_shield(c):
    t_tower_shield(c)
    for y in range(2, 14):
        for x in range(4, 12):
            cell = c.get(x, y)
            if cell and cell[0] == "m":
                if (x + (y % 4 // 2) * 2) % 4 == 0 and y % 2 == 0:
                    c.set(x, y, "m", 4)
                elif (x + (y % 4 // 2) * 2) % 4 == 2 and y % 2 == 1:
                    c.set(x, y, "m", 1)
    boss(c, 7.5, 7.0)


def t_ingot(c):
    c.stamp(2, 5, [
        "....OOOOOOO.",
        "...O4333332O",
        "..O43333321O",
        ".O433333211O",
        "O4333332111O",
        "O2222221111O",
        "O111111111O.",
        ".OOOOOOOOO..",
    ])


def t_eitr(c):
    c.stamp(4, 2, [
        "...OO...",
        "..O43O..",
        "..O32O..",
        ".O4332O.",
        "O433221O",
        "O432211O",
        "O332211O",
        "O322111O",
        ".O2111O.",
        "..O11O..",
        "...OO...",
    ])


TEMPLATES = {
    "sword": lambda c: t_sword(c),
    "sword_fancy": lambda c: t_sword(c, fancy=True, guard_part="a"),
    "mistwalker": lambda c: t_sword(c, fancy=True, guard_part="a"),
    "nidhogg": lambda c: t_sword(c, fancy=True, long=True, guard_part="a"),
    "dyrnwyn": lambda c: t_dyrnwyn(c),
    "greatsword": t_greatsword,
    "knife": t_knife,
    "razor": t_razor,
    "dual_knives": t_dual_knives,
    "cleaver": t_cleaver,
    "club": t_club,
    "mace": t_mace,
    "frostner": t_frostner,
    "porcupine": t_porcupine,
    "sledge": t_sledge,
    "demolisher": t_demolisher,
    "stagbreaker": t_stagbreaker,
    "axe": t_axe,
    "axe_small": lambda c: t_axe(c, small=True),
    "jotun_bane": t_jotun_bane,
    "dual_axes": t_dual_axes,
    "battleaxe": t_battleaxe,
    "crystal_battleaxe": lambda c: t_battleaxe(c, crystal=True),
    "spear": t_spear,
    "spear_long": lambda c: t_spear(c, long=True),
    "fang_spear": t_fang_spear,
    "carapace_spear": t_carapace_spear,
    "splitnir": t_splitnir,
    "atgeir": t_atgeir,
    "himminafl": lambda c: t_atgeir(c, accent=True),
    "claws": t_claws,
    "staff_orb": lambda c: t_staff(c, "orb"),
    "staff_ring": lambda c: t_staff(c, "ring"),
    "staff_fork": lambda c: t_staff(c, "fork"),
    "staff_root": lambda c: t_staff(c, "root"),
    "staff_rock": lambda c: t_staff(c, "rock"),
}

SHIELD_PATTERNS = {"red_paint": "halves", "wolf_blue": "cross", "linen_paint": "ring", "ember": "ring",
                   "eitr": "cross", "oak_dark": "quarters"}


def palettes_for(sprite):
    metal = sprite.get("metal", "iron")
    handle = sprite.get("handle", "oak")
    accent = sprite.get("accent", metal)
    grip_key = "linen" if handle == "linen" else "leather"
    shaft_key = "dark_oak" if handle in ("leather", "linen") else handle
    return {"m": pal(metal), "h": pal(shaft_key), "g": pal(grip_key), "a": pal(accent), "s": pal("string")}


def finish(c, pals):
    c.outline()
    return c.render(pals)


def render_weapon(w):
    sprite = w["sprite"]
    template = sprite["template"]
    pals = palettes_for(sprite)
    images = {}
    if w["family"] == "BOW":
        # bow limbs use the "metal" key (it's the bow's wood); arrows use a plain iron tip
        bow_pals = dict(pals, h=pal(sprite.get("metal", "oak")), a=pal("iron"))
        if "accent" in sprite:
            bow_pals["g"] = pal(sprite["accent"])
        for suffix, pull in (("", 0), ("_pulling_0", 1), ("_pulling_1", 2), ("_pulling_2", 3)):
            c = Canvas()
            t_bow(c, recurve=template == "bow_recurve", pull=pull)
            images[w["id"] + suffix] = finish(c, bow_pals)
    elif w["family"] == "CROSSBOW":
        for suffix, state in (("", "idle"), ("_pulling_0", "pulling_0"), ("_pulling_1", "pulling_1"),
                              ("_pulling_2", "pulling_2"), ("_arrow", "arrow")):
            c = Canvas()
            t_crossbow(c, state)
            cb_pals = dict(pals)
            if state == "arrow":
                cb_pals["a"] = pal("iron")
            images[w["id"] + suffix] = finish(c, cb_pals)
    else:
        c = Canvas()
        TEMPLATES[template](c)
        images[w["id"]] = finish(c, pals)
    return images


def render_shield(sprite):
    template = sprite["template"]
    pals = {"m": pal(sprite.get("metal", "oak")), "h": pal(sprite.get("handle", "iron")),
            "g": pal("leather"), "a": pal(sprite.get("accent", sprite.get("handle", "iron"))),
            "s": pal("string")}
    c = Canvas()
    if template == "round_shield":
        t_round_shield(c, SHIELD_PATTERNS.get(sprite.get("accent")))
    elif template == "banded_shield":
        t_banded_shield(c)
        pals["a"] = pal(sprite["accent"])
    elif template == "buckler":
        t_buckler(c)
    elif template == "tower_shield":
        t_tower_shield(c)
    elif template == "scale_shield":
        t_scale_shield(c)
    return finish(c, pals)


def render_material(mid):
    c = Canvas()
    if mid == "refined_eitr":
        t_eitr(c)
        return c.render({"m": pal("eitr")})
    t_ingot(c)
    key = {"bronze_ingot": "bronze", "silver_ingot": "silver", "black_metal_ingot": "blackmetal",
           "flametal_ingot": "flametal"}[mid]
    return c.render({"m": pal(key)})


def render_all(weapons, shields, shield_sprites, materials, out_dir, icon_path):
    os.makedirs(out_dir, exist_ok=True)
    for f in os.listdir(out_dir):
        if f.endswith(".png"):
            os.remove(os.path.join(out_dir, f))
    count = 0
    for w in weapons:
        for name, img in render_weapon(w).items():
            img.save(os.path.join(out_dir, name + ".png"))
            count += 1
    for sid, *_ in shields:
        render_shield(shield_sprites[sid]).save(os.path.join(out_dir, sid + ".png"))
        count += 1
    for mid, *_ in materials:
        render_material(mid).save(os.path.join(out_dir, mid + ".png"))
        count += 1
    icon = Image.open(os.path.join(out_dir, "frostner.png")).resize((128, 128), Image.NEAREST)
    icon.save(icon_path)
    return count
