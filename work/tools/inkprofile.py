#!/usr/bin/env python3
"""Ink profile of a render, for comparing against the pinna reference.

Usage: python3 work/tools/inkprofile.py <image.png> [threshold_pct]

Prints the same measurements recorded in the pinna design spec, so a render
can be compared to ~/Downloads/tree.jpg numerically instead of by eye.
"""
import subprocess
import sys

THRESHOLD = int(sys.argv[2]) if len(sys.argv) > 2 else 45
SRC = sys.argv[1]


def mask(path):
    """1-byte-per-pixel ink mask: 255 = ink. Returns (bytes, w, h)."""
    dims = subprocess.run(
        ["magick", "identify", "-format", "%w %h", path],
        capture_output=True, text=True, check=True).stdout.split()
    w, h = int(dims[0]), int(dims[1])
    raw = subprocess.run(
        ["magick", path, "-colorspace", "Gray", "-threshold", f"{THRESHOLD}%",
         "-negate", "-depth", "8", "gray:-"],
        capture_output=True, check=True).stdout
    assert len(raw) == w * h, f"expected {w * h} bytes, got {len(raw)}"
    return raw, w, h


def runs(line):
    """[(start, length)] of ink runs."""
    out, s = [], None
    for i, v in enumerate(line):
        if v > 127 and s is None:
            s = i
        elif v <= 127 and s is not None:
            out.append((s, i - s))
            s = None
    if s is not None:
        out.append((s, len(line) - s))
    return out


def main():
    m, w, h = mask(SRC)
    rows = [m[y * w:(y + 1) * w] for y in range(h)]

    on_x = [x for x in range(w) if any(r[x] > 127 for r in rows)]
    on_y = [y for y in range(h) if any(v > 127 for v in rows[y])]
    if not on_x:
        print("BBOX  (no ink)")
        return
    x0, x1, y0, y1 = min(on_x), max(on_x), min(on_y), max(on_y)
    print(f"BBOX  x {x0}..{x1} ({x1 - x0})  y {y0}..{y1} ({y1 - y0})   frame {w}x{h}")

    # canopy = the bbox minus the trunk tail: rows whose ink spans more than half the bbox width
    wide = [y for y in on_y if sum(1 for v in rows[y] if v > 127) > (x1 - x0) * 0.25]
    if wide:
        cy0, cy1 = min(wide), max(wide)
        area = (x1 - x0 + 1) * (cy1 - cy0 + 1)
        ink = sum(1 for y in range(cy0, cy1 + 1) for v in rows[y][x0:x1 + 1] if v > 127)
        print(f"CANOPY  y {cy0}..{cy1}   ink {100.0 * ink / area:.1f}% of the canopy box")

    print("COLS  (ink % per 1/40 of width)")
    bw = max(1, w // 40)
    for i in range(0, w, bw):
        c = sum(1 for r in rows for v in r[i:i + bw] if v > 127)
        pct = 100.0 * c / (bw * h)
        print(f"  x={i:4d}  {pct:5.1f}%  {'#' * int(pct / 2)}")

    print("ROWS  (ink % per 1/50 of height)")
    bh = max(1, h // 50)
    for j in range(0, h, bh):
        c = sum(1 for y in range(j, min(j + bh, h)) for v in rows[y] if v > 127)
        pct = 100.0 * c / (bh * w)
        print(f"  y={j:4d}  {pct:5.1f}%  {'#' * int(pct / 2)}")

    print("HATCH  (ink-run / gap widths on horizontal scans through leaf mass)")
    for y in (int(h * 0.2), int(h * 0.33), int(h * 0.6)):
        r = [q for q in runs(rows[y][x0:x1]) if q[1] > 0]
        if len(r) < 4:
            print(f"  y={y}: {len(r)} runs (too few to profile)")
            continue
        ws = sorted(q[1] for q in r)
        gaps = sorted(r[i + 1][0] - (r[i][0] + r[i][1]) for i in range(len(r) - 1))
        print(f"  y={y}: {len(r):4d} runs   ink median={ws[len(ws) // 2]}  "
              f"gap median={gaps[len(gaps) // 2]}  period={ws[len(ws) // 2] + gaps[len(gaps) // 2]}")

    print("TRUNK  (widest ink run per row, below the canopy)")
    start = cy1 + 1 if wide else int(h * 0.85)
    peak_y, peak_w = None, -1
    for y in range(start, y1 + 1):
        r = [q for q in runs(rows[y]) if q[1] > 3]
        if not r:
            continue
        b = max(r, key=lambda q: q[1])
        if b[1] > peak_w:
            peak_y, peak_w = y, b[1]
    for y in range(start, y1 + 1, max(1, (y1 - start) // 12 or 1)):
        r = [q for q in runs(rows[y]) if q[1] > 3]
        if not r:
            continue
        b = max(r, key=lambda q: q[1])
        print(f"  y={y:4d}  width={b[1]:4d}  centre={b[0] + b[1] / 2:6.1f}")
    if peak_y is not None:
        print(f"  peak  y={peak_y}  width={peak_w}")


main()
