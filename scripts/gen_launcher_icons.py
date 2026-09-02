"""Render the legacy launcher PNGs from the same geometry as the adaptive icon.

minSdk is 24, so API 24/25 launchers get no adaptive-icon support and need real
bitmaps. Rather than let those two versions drift from the vector mark, both are
generated from the constants below -- keep them in sync with
res/drawable/ic_launcher_{background,foreground}.xml and re-run:

    python scripts/gen_launcher_icons.py
"""

from pathlib import Path

from PIL import Image, ImageDraw

VIEWPORT = 108          # adaptive-icon coordinate space
VISIBLE = 72            # portion of it a launcher mask actually shows
MARK_SCALE = 0.92       # matches the <group> scale in the vector drawables
SS = 10                 # supersample factor before the final downscale

GOLD = (138, 180, 248, 255)          # Palette.Accent
GRADIENT = [                          # offset -> RGB, matching the background vector
    (0.00, (0x1B, 0x1B, 0x22)),
    (0.55, (0x10, 0x10, 0x14)),
    (1.00, (0x08, 0x08, 0x0A)),
]

# Yen glyph: arms + stem as one polygon, then the two crossbars.
STEM_AND_ARMS = [
    (32, 29), (41.3, 29), (54, 43.62), (66.7, 29), (76, 29),
    (57.5, 50.3), (57.5, 78.5), (50.5, 78.5), (50.5, 50.3),
]
BAR_UPPER = [(33, 55.5), (75, 55.5), (75, 61.5), (33, 61.5)]
BAR_LOWER = [(33, 66.5), (75, 66.5), (75, 72.5), (33, 72.5)]

# Density buckets and the launcher icon size each one expects, in px.
DENSITIES = {
    "mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192,
}

RES = Path(__file__).resolve().parents[1] / "app/src/main/res"


def _lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def _gradient_color(t):
    """Sample the background gradient at position t along its axis."""
    t = min(max(t, 0.0), 1.0)
    for (o0, c0), (o1, c1) in zip(GRADIENT, GRADIENT[1:]):
        if t <= o1:
            return _lerp(c0, c1, (t - o0) / (o1 - o0))
    return GRADIENT[-1][1]


def _scaled(points):
    """Apply the mark's group scale about the centre of the viewport."""
    c = VIEWPORT / 2
    return [((c + (x - c) * MARK_SCALE) * SS, (c + (y - c) * MARK_SCALE) * SS)
            for x, y in points]


def _gradient_tile(size):
    """The background ramp, drawn small and scaled up -- it has no detail to lose."""
    n = VIEWPORT * 2
    ax, ay = 6 / VIEWPORT * n, 0.0
    dx, dy = (102 - 6) / VIEWPORT * n, float(n)
    denom = dx * dx + dy * dy
    pixels = [
        _gradient_color(((x - ax) * dx + (y - ay) * dy) / denom) + (255,)
        for y in range(n) for x in range(n)
    ]
    tile = Image.new("RGBA", (n, n))
    tile.putdata(pixels)
    return tile.resize((size, size), Image.BILINEAR)


def render_full():
    """The whole 108x108 tile at supersampled resolution."""
    size = VIEWPORT * SS
    img = _gradient_tile(size)
    draw = ImageDraw.Draw(img)
    for poly in (STEM_AND_ARMS, BAR_UPPER, BAR_LOWER):
        draw.polygon(_scaled(poly), fill=GOLD)
    return img


def crop_to_visible(img):
    """Legacy icons have no mask, so show the adaptive icon's visible window."""
    inset = (VIEWPORT - VISIBLE) / 2 * SS
    return img.crop((int(inset), int(inset),
                     int(img.width - inset), int(img.height - inset)))


def masked(img, size, shape):
    out = img.resize((size * 4, size * 4), Image.LANCZOS)
    mask = Image.new("L", out.size, 0)
    d = ImageDraw.Draw(mask)
    if shape == "round":
        d.ellipse((0, 0, out.width - 1, out.height - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, out.width - 1, out.height - 1),
                            radius=int(out.width * 0.2), fill=255)
    out.putalpha(mask)
    return out.resize((size, size), Image.LANCZOS)


def main():
    full = render_full()
    visible = crop_to_visible(full)

    for bucket, size in DENSITIES.items():
        target = RES / f"mipmap-{bucket}"
        target.mkdir(parents=True, exist_ok=True)
        masked(visible, size, "square").save(target / "ic_launcher.png")
        masked(visible, size, "round").save(target / "ic_launcher_round.png")
        print(f"mipmap-{bucket}: {size}x{size}")

    preview = RES.parents[3] / "docs/screenshots/launcher-icon.png"
    preview.parent.mkdir(parents=True, exist_ok=True)
    masked(visible, 512, "square").save(preview)
    print(f"preview: {preview}")


if __name__ == "__main__":
    main()
