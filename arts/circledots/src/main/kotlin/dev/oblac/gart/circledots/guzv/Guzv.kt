package dev.oblac.gart.circledots.guzv

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.f
import dev.oblac.gart.math.rndi
import dev.oblac.gart.smooth.toCardinalSpline
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("guzv", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class MyDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private val pal = Palettes.cool56

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    repeat(160) { lines ->
        val p = randomPoint(d.times(0.5f)).offset(-100f, 200f)
        val line = Line(p, d.center)
        val rp = createSpiral(p, line.length() * 3, 12, Degrees.of(lines), rndi(1, 2))

        val cc = pal.random()
        val s = rndi(18, 24)
        repeat(s) { ndx ->
            fun isSpecialPoint(i: Int) = i == 0 || i >= s - 2
            val color = if (isSpecialPoint(ndx)) RetroColors.black01 else cc
            val factor = 1.2f
            val spline = rp.mapIndexed { i, p -> p.offset(ndx.f() * (1f + i.mod(2) * factor), ndx.f() * (1f + i.mod(2) * factor)) }.toCardinalSpline(0.1f, 30)
            c.drawPath(spline, strokeOf(3f, color))
        }
    }

    c.drawCircle(d.center.offset(80f, -100f), 180f, fillOf(RetroColors.black01))
}
