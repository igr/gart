package dev.oblac.gart

import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palette
import dev.oblac.gart.gfx.drawBlackText
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.marbling.Marbling
import dev.oblac.gart.math.between
import dev.oblac.gart.math.lerp
import dev.oblac.gart.util.Stopwatch
import org.jetbrains.skia.Point
import java.io.File
import kotlin.random.Random

/**
 * ExampleMarbling: nine traditional patterns, each built from the marbling primitives - drops,
 * combs straight and wavy, whirls, a vortex, a stylus, the sheet wiggled as it is laid on.
 * Every tile is its own bath rendered by inverse mapping at 3x3 samples a pixel.
 * Run with `-Dheadless=1` to just write `output/exampleMarbling.png`.
 */
fun main(args: Array<String>) {
    val t = 460f
    val gap = 30f
    val gart = Gart.of("exampleMarbling", (3 * t + 4 * gap).toInt(), (3 * (t + gap + 20f) + gap).toInt())
    val g = gart.gartvas()
    val c = g.canvas
    c.clear(0xFFFFFFFF.toInt())

    val paper = 0xFFF1E9D6.toInt()
    val inks = Palette(0xFF24364F, 0xFFA0342C, 0xFFD9A441, 0xFF6F8F6A, 0xFF2B2B2B, 0xFFF6F1E6)
    val sw = Stopwatch()

    val tiles = listOf<Pair<String, (Marbling, Random) -> Unit>>(
        "stone" to { m, rnd -> stone(m, rnd, t, inks, 200) },
        "gelgit" to { m, rnd -> gelgit(m, rnd, t, inks) },
        "nonpareil" to { m, rnd -> nonpareil(m, rnd, t, inks) },
        "serpentine" to { m, rnd ->
            gelgit(m, rnd, t, inks)
            m.comb(t / 2, t / 2, Degrees(90f), z = t * 0.22f, c = 9f, tines = 18, spacing = t / 14, amplitude = 14f, wavelength = t / 2.5f)
            m.shift(0f, -t * 0.25f) // the comb carried the whole bath down with it
        },
        "bouquet" to { m, rnd -> bouquet(m, rnd, t, inks) },
        "curls" to { m, rnd ->
            nonpareil(m, rnd, t, inks)
            repeat(5) {
                val r = rnd.between(0.05f, 0.1f) * t
                m.whirl(rnd.between(0.15f, 0.85f) * t, rnd.between(0.15f, 0.85f) * t, r, z = r * rnd.between(2.5f, 4f), c = r * 0.6f)
            }
            m.vortex(t * 0.5f, t * 0.5f, z = t * 0.2f, c = t * 0.12f)
        },
        "spanish wave" to { m, rnd ->
            nonpareil(m, rnd, t, inks)
            m.wave(amplitude = 5f, wavelength = 30f, dir = Degrees(90f)) // the sheet wiggled as it went down on the bath
        },
        "suminagashi" to { m, rnd ->
            rings(m, t * 0.5f, t * 0.5f, t * 0.09f, 44, Palette(0xFF23262B, 0xFFEFEAE0))
            m.wave(amplitude = 22f, wavelength = t * 0.9f, dir = Degrees(15f))
            m.wave(amplitude = 12f, wavelength = t * 0.6f, dir = Degrees(100f), push = Degrees(100f))
            m.stroke(Point(t * 0.15f, t * 0.9f), Point(t * 0.55f, t * 0.35f), c = 40f)
            m.vortex(t * 0.68f, t * 0.3f, z = 90f, c = 70f)
        },
        "strokes" to { m, rnd ->
            rings(m, t * 0.5f, t * 0.5f, t * 0.12f, 22, inks)
            repeat(7) {
                val a = Point(rnd.between(0.1f, 0.9f) * t, rnd.between(0.1f, 0.9f) * t)
                val b = Point(rnd.between(0.1f, 0.9f) * t, rnd.between(0.1f, 0.9f) * t)
                m.stroke(a, b, c = 18f)
            }
        },
    )

    for ((i, tile) in tiles.withIndex()) {
        val (name, recipe) = tile
        val x = gap + (i % 3) * (t + gap)
        val y = gap + 20f + (i / 3) * (t + gap + 20f)
        val m = Marbling(paper)
        recipe(m, Random(11 + i))
        Gartmap(Dimension(t.toInt(), t.toInt())).use { bmp ->
            m.render(bmp, aa = 3)
            c.drawImage(bmp.image(), x, y)
        }
        c.drawBlackText("$name  (${m.ops.size} ops)", x, y - 6f)
        println("$name: ${sw.lap()} ms")
    }
    println("sheet: ${sw.ms} ms")

    File("output").mkdirs()
    gart.saveImage(g, "output/exampleMarbling.png")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

// a bed of drops, big ones first, colours in turn round the palette. the tray is bigger than
// the sheet, so the bed runs well past the tile and combing never drags bare paper in
private fun stone(m: Marbling, rnd: Random, t: Float, inks: Palette, n: Int) {
    for (i in 0 until n) {
        val r = t * lerp(0.13f, 0.02f, i / (n - 1f)) * rnd.between(0.7f, 1.3f)
        m.drop(rnd.between(-0.2f, 1.2f) * t, rnd.between(-0.2f, 1.2f) * t, r, inks[i % inks.size])
    }
}

// n drops of one size at one spot, colours alternating: every drop pushes the rings before it
// out, so they thin toward the edge the way real ones do
private fun rings(m: Marbling, cx: Float, cy: Float, r: Float, n: Int, inks: Palette) {
    for (i in 0 until n) m.drop(cx, cy, r, inks[i % inks.size])
}

// a few big drops so the tray has no bare size left, a stone bed on those, then comb across and
// comb back offset by half a tine - the chevrons
private fun gelgit(m: Marbling, rnd: Random, t: Float, inks: Palette) {
    for (i in 0 until 8) m.drop(rnd.between(0f, 1f) * t, rnd.between(0f, 1f) * t, t * 0.4f, inks[i % inks.size])
    stone(m, rnd, t, inks, 150)
    val s = t / 11
    m.comb(t / 2, t / 2, Degrees(0f), z = t * 0.16f, c = s / 3, tines = 17, spacing = s)
    m.comb(t / 2, t / 2 + s / 2, Degrees(180f), z = t * 0.16f, c = s / 3, tines = 17, spacing = s)
}

// gelgit, then a fine comb straight down over it
private fun nonpareil(m: Marbling, rnd: Random, t: Float, inks: Palette) {
    gelgit(m, rnd, t, inks)
    m.comb(t / 2, t / 2, Degrees(90f), z = t * 0.05f, c = 2.5f, tines = 130, spacing = t / 90)
    m.shift(0f, -t * 0.06f)
}

// two sets of tines, a half spacing apart and wiggling against each other, dragged up over a
// combed bed: the paint between them is squeezed and let go, which reads as scallop shells
private fun bouquet(m: Marbling, rnd: Random, t: Float, inks: Palette) {
    gelgit(m, rnd, t, inks)
    val s = t / 7
    val amp = s * 0.28f
    val len = t / 2.2f
    m.comb(t / 2, t / 2, Degrees(270f), z = t * 0.22f, c = s / 4, tines = 12, spacing = s, amplitude = amp, wavelength = len)
    m.comb(t / 2, t / 2 + s / 2, Degrees(270f), z = t * 0.22f, c = s / 4, tines = 12, spacing = s, amplitude = amp, wavelength = len, phase = 3.1416f)
    m.shift(0f, t * 0.28f)
}
