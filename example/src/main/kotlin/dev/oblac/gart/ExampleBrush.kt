package dev.oblac.gart

import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.brush.Brush
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.Hatch
import dev.oblac.gart.brush.Pressure
import dev.oblac.gart.brush.Rotate
import dev.oblac.gart.brush.Tip
import dev.oblac.gart.brush.Wobble
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.brush.drawBrushHatch
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.drawBlackText
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.gfx.pathOf
import dev.oblac.gart.io.detectHeadlessFlags
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sampler sheet of the brush package: every stock brush on a straight line at size 1 and 3, on
 * a wave, and with a hand wobble; below that hatching, an image tip, a custom nib and a curved
 * wobble. Run with `-Dheadless=1` to just write `output/exampleBrush.png`.
 */
fun main(args: Array<String>) {
    val gart = Gart.of("exampleBrush", 1400, 2000)
    val g = gart.gartvas()
    val c = g.canvas
    val rnd = Random(42)
    c.clear(CssColors.white)
    val ink = 0xFF1A1A1A.toInt()

    val t0 = System.nanoTime()

    // every brush: size 1, size 3, a wave, a hand wobble
    var y = 70f
    for ((name, brush) in Brushes.all) {
        c.drawBlackText(name, 30f, y + 6f)
        c.drawBrush(Point(160f, y), Point(400f, y), brush, ink, 1f, rnd)
        c.drawBrush(Point(440f, y), Point(690f, y), brush, ink, 3f, rnd)
        c.drawBrush(wave(730f, y, 250f, 28f), brush, ink, 3f, rnd)
        c.drawBrush(Point(1020f, y), Point(1370f, y), brush, ink, 3f, rnd, wobble = Wobble.hand(rnd))
        y += 115f
    }

    // hatching: a circle with the hatch brush, a star cross-hatched in pencil
    val hy = 1640f
    c.drawBlackText("hatch", 30f, 1500f)
    c.drawBrushHatch(
        Circle(250f, hy, 140f).toPath(),
        Hatch(dist = 6f, angle = Degrees(45f), rand = 0.3f, overshoot = 4f),
        Brushes.hatch, ink, 2f, rnd,
    )
    val star = star(600f, hy, 150f, 70f, 5)
    c.drawBrushHatch(
        star, Hatch(dist = 7f, angle = Degrees(-30f), gradient = 1.5f, rand = 0.2f),
        Brushes.pencil2B, ink, 2f, rnd, blend = BlendMode.MULTIPLY,
    )
    c.drawBrushHatch(
        star, Hatch(dist = 14f, angle = Degrees(60f), rand = 0.2f),
        Brushes.pencil2B, ink, 2f, rnd, blend = BlendMode.MULTIPLY,
    )

    // image tip: a leaf, turned along the stroke
    val leaf = Gartvas(Dimension(48, 48)).also { s ->
        s.canvas.clear(0x00000000)
        s.canvas.rotate(-35f, 24f, 24f)
        s.canvas.drawOval(Rect(4f, 17f, 44f, 31f), paint().apply { color = CssColors.black; })
    }.snapshot()
    val leafBrush = Brush(
        tip = Tip.Image(leaf, Rotate.NATURAL),
        weight = 18f, scatter = 3f, opacity = 0.55f, spacing = 5f,
        pressure = Pressure.Bell(ends = 0.6f, peak = 1.1f),
    )
    c.drawBlackText("image tip", 800f, 1500f)
    c.drawBrush(wave(820f, 1560f, 520f, 40f), leafBrush, 0xFF2E7D32.toInt(), 1f, rnd)

    // custom tip: a flat nib held at 40 degrees, thick one way and thin the other
    val nib = Brush(
        tip = Tip.Custom(Rotate.NONE) { cv, p ->
            cv.rotate(-40f)
            cv.drawRect(Rect(-0.5f, -0.08f, 0.5f, 0.08f), p)
        },
        weight = 22f, scatter = 0f, opacity = 0.9f, spacing = 0.6f, pressure = Pressure.Flat,
    )
    c.drawBlackText("custom nib", 800f, 1660f)
    c.drawBrush(Circle(1000f, 1760f, 90f).toPath(), nib, 0xFF283593.toInt(), 1f, rnd)
    c.drawBrush(wave(1120f, 1760f, 230f, 50f), nib, 0xFF283593.toInt(), 1f, rnd)

    // curved wobble: straight lines bent by a noise field
    c.drawBlackText("curved wobble", 30f, 1830f)
    val curved = Wobble.curved(rnd, amount = 0.6f, scale = 0.003f)
    for (i in 0 until 6) {
        val yy = 1860f + i * 20f
        c.drawBrush(Point(160f, yy), Point(700f, yy), Brushes.pen, ink, 2f, rnd, wobble = curved)
    }

    println("brushes: ${(System.nanoTime() - t0) / 1_000_000} ms")

    File("output").mkdirs()
    gart.saveImage(g, "output/exampleBrush.png")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

private fun wave(x: Float, y: Float, w: Float, amp: Float): Path {
    val pts = (0..80).map { i ->
        val t = i / 80f
        Point(x + t * w, y + amp * sin(t * 2f * PI.toFloat() * 1.5f))
    }
    return pathOf(pts)
}

private fun star(cx: Float, cy: Float, outer: Float, inner: Float, n: Int): Path {
    val pts = (0 until 2 * n).map { i ->
        val a = -PI.toFloat() / 2f + i * PI.toFloat() / n
        val r = if (i % 2 == 0) outer else inner
        Point(cx + r * cos(a), cy + r * sin(a))
    }
    return closedPathOf(pts)
}
