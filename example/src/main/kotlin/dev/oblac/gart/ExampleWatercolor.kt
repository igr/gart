package dev.oblac.gart

import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.Watercolor
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.brush.drawWatercolor
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.drawBlackText
import dev.oblac.gart.gfx.drawWhiteText
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sampler sheet of the watercolour wash: overlapping primaries, a bleed ladder, texture and
 * border ladders, a directed wash, a shape with a hole, and a wash on a dark ground. Run with
 * `-Dheadless=1` to just write `output/exampleWatercolor.png`.
 */
fun main(args: Array<String>) {
    val gart = Gart.of("exampleWatercolor", 1400, 1500)
    val g = gart.gartvas()
    val c = g.canvas
    val rnd = Random(7)
    c.clear(CssColors.white)

    val yellow = 0xFFF2C230.toInt()
    val blue = 0xFF2A5DB0.toInt()
    val red = 0xFFC8383A.toInt()
    val green = 0xFF3E8E5A.toInt()

    val t0 = System.nanoTime()

    // overlapping primaries, glazed (multiply) so the overlaps mix like pigment
    c.drawBlackText("glazing (MULTIPLY)", 30f, 40f)
    c.drawWatercolor(Circle(200f, 190f, 120f).toPath(), yellow, Watercolor(bleed = 0.15f), rnd, BlendMode.MULTIPLY)
    c.drawWatercolor(Circle(320f, 190f, 120f).toPath(), blue, Watercolor(bleed = 0.15f), rnd, BlendMode.MULTIPLY)
    c.drawWatercolor(Circle(260f, 290f, 120f).toPath(), red, Watercolor(bleed = 0.15f), rnd, BlendMode.MULTIPLY)

    // the same three with plain alpha
    c.drawBlackText("plain (SRC_OVER)", 520f, 40f)
    c.drawWatercolor(Circle(690f, 190f, 120f).toPath(), yellow, Watercolor(bleed = 0.15f), rnd)
    c.drawWatercolor(Circle(810f, 190f, 120f).toPath(), blue, Watercolor(bleed = 0.15f), rnd)
    c.drawWatercolor(Circle(750f, 290f, 120f).toPath(), red, Watercolor(bleed = 0.15f), rnd)

    // a wash with a pencil line over it, the usual sketch combination
    c.drawBlackText("wash + pencil", 1010f, 40f)
    val leaf = leaf(1190f, 240f, 150f, 80f)
    c.drawWatercolor(leaf, green, Watercolor(bleed = 0.1f, angle = Degrees(-60f)), rnd, BlendMode.MULTIPLY)
    c.drawBrush(leaf, Brushes.pencil2B, 0xFF2B2B2B.toInt(), 2.5f, rnd)

    // bleed ladder
    c.drawBlackText("bleed 0.02 / 0.07 / 0.2 / 0.5 / 0.2 inward", 30f, 470f)
    val bleeds = listOf(0.02f, 0.07f, 0.2f, 0.5f)
    bleeds.forEachIndexed { i, b ->
        c.drawWatercolor(blob(150f + i * 260f, 600f, 95f), blue, Watercolor(bleed = b), rnd, BlendMode.MULTIPLY)
    }
    c.drawWatercolor(blob(1190f, 600f, 95f), blue, Watercolor(bleed = 0.2f, outward = false), rnd, BlendMode.MULTIPLY)

    // texture and border ladders
    c.drawBlackText("texture 0 / 0.5 / 1      border 0 / 1      angle 0 (left to right)", 30f, 790f)
    listOf(0f, 0.5f, 1f).forEachIndexed { i, t ->
        c.drawWatercolor(Rect(60f + i * 230f, 820f, 240f + i * 230f, 1000f).toPath(), red, Watercolor(texture = t, bleed = 0.1f), rnd, BlendMode.MULTIPLY)
    }
    listOf(0f, 1f).forEachIndexed { i, b ->
        c.drawWatercolor(Rect(750f + i * 230f, 820f, 930f + i * 230f, 1000f).toPath(), blue, Watercolor(border = b, bleed = 0.1f), rnd, BlendMode.MULTIPLY)
    }
    c.drawWatercolor(Rect(1210f, 820f, 1380f, 1000f).toPath(), yellow, Watercolor(bleed = 0.12f, angle = Degrees(0f)), rnd, BlendMode.MULTIPLY)

    // a hole, and a dark ground
    c.drawBlackText("hole (inner contour)", 30f, 1080f)
    val ring = PathBuilder().addCircle(200f, 1260f, 130f).addCircle(200f, 1260f, 60f).detach()
    c.drawWatercolor(ring, green, Watercolor(bleed = 0.08f), rnd, BlendMode.MULTIPLY)

    c.drawRect(Rect(420f, 1090f, 1380f, 1470f), fillOf(0xFF1A1B22.toInt()))
    c.drawWhiteText("on a dark ground (SRC_OVER)", 440f, 1120f)
    c.drawWatercolor(blob(620f, 1290f, 110f), 0xFFE8D9A0.toInt(), Watercolor(bleed = 0.15f), rnd)
    c.drawWatercolor(blob(900f, 1290f, 110f), 0xFF7FB8E0.toInt(), Watercolor(bleed = 0.15f, opacity = 0.8f), rnd)
    c.drawWatercolor(star(1180f, 1290f, 130f, 60f, 5), 0xFFE07A7A.toInt(), Watercolor(bleed = 0.06f), rnd)

    println("watercolour: ${(System.nanoTime() - t0) / 1_000_000} ms")

    File("output").mkdirs()
    gart.saveImage(g, "output/exampleWatercolor.png")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

private fun blob(cx: Float, cy: Float, r: Float): Path {
    val pts = (0 until 24).map { i ->
        val a = i * 2f * PI.toFloat() / 24f
        val rr = r * (1f + 0.18f * sin(3f * a) + 0.08f * cos(5f * a))
        Point(cx + rr * cos(a), cy + rr * sin(a))
    }
    return closedPathOf(pts)
}

private fun leaf(cx: Float, cy: Float, w: Float, h: Float): Path {
    val pts = (0 until 40).map { i ->
        val t = i / 40f * 2f * PI.toFloat()
        val x = w * cos(t)
        val y = h * sin(t) * (0.6f + 0.4f * cos(t))
        Point(cx + x * 0.866f - y * 0.5f, cy + x * 0.5f + y * 0.866f)
    }
    return closedPathOf(pts)
}

private fun star(cx: Float, cy: Float, outer: Float, inner: Float, n: Int): Path {
    val pts = (0 until 2 * n).map { i ->
        val a = -PI.toFloat() / 2f + i * PI.toFloat() / n
        val r = if (i % 2 == 0) outer else inner
        Point(cx + r * cos(a), cy + r * sin(a))
    }
    return closedPathOf(pts)
}

private fun Rect.toPath(): Path = PathBuilder().addRect(this).detach()
