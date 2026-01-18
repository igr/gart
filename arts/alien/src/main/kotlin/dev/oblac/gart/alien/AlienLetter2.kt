package dev.oblac.gart.alien

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.toStrokePaint
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PathEffect

fun main() {
    val gart = Gart.of("alien2", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = Alien2Draw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class Alien2Draw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private val pal = Palettes.cool4.shifted(1)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    foo(c, d)
}

private fun foo(c: Canvas, d: Dimension) {
    val dx = (d.w - 2 * 50f) / 6
    repeat(6) {
        val g = 80f
        val x = 50f + it * dx + (dx - g) / 2
        val y = 150f + rndf(10f) * 10f
        val p = pal[it].toStrokePaint(50f).apply {
            this.strokeCap = PaintStrokeCap.ROUND
            this.pathEffect = PathEffect.makeDiscrete(20f, 2f, 2)
        }
        c.drawLine(x, d.hf, x, y, p)
        c.drawLine(x, y, x + g, y, p)
        c.drawLine(x + g, y, x + g, d.hf, p)

        val skew = { rndf(-3f, 3f) }
        val rnd = rndf(100f, 200f)
        if (rndb(8, 10)) {
            c.drawLine(x, y + rnd, x + g, y + rnd + skew(), p)
        }

        val rnd2 = rnd + rndf(100f, 200f)
        c.drawLine(x, y + rnd2, x + g, y + rnd2 + skew(), p)

        val yy = d.hf - rndf(100f, 400f)
        c.drawLine(x, yy, x + g / 2, yy, RetroColors.black01.toStrokePaint(40f).apply {
            this.pathEffect = PathEffect.makeDiscrete(20f, 2f, 2)
        })
        val yyy = d.hf - rndf(100f, 400f)
        c.drawLine(x + g / 2, yyy, x + g, yyy, RetroColors.black01.toStrokePaint(40f).apply {
            this.pathEffect = PathEffect.makeDiscrete(20f, 2f, 2)
        })
    }
}
