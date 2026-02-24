package dev.oblac.gart.hills3

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndf
import dev.oblac.gart.midpoint.midpointDisplacementY
import org.jetbrains.skia.Canvas

val pal = Palettes.cool32.expand(30)

fun main() {
    val gart = Gart.of("horizons", 1024, 1024 * GOLDEN_RATIO)
    println(gart)

    val g = gart.gartvas()
    val d = gart.d
    val w = gart.window()

    val c = g.canvas

    draw(c, d)

    gart.saveImage(g)

    w.showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    val gap = 60f
    val max = (d.hf / gap).toInt()
    for (i in -1 until max) {
        val y = gap * i
        if (i == 12) {
            drawSun1(c, d, y)
        }
        if (i == 24) {
            drawSun2(c, d, y)
        }
        drawHill(c, d, y, gap, pal.safe(i))
    }
    c.drawBorder(d, 20f, CssColors.white)
}

private fun drawSun1(c: Canvas, d: Dimension, y: Float) {
    val cirlce = Circle(d.w3, y, 200f)
    c.drawCircle(cirlce, fillOfWhite())
}

private fun drawSun2(c: Canvas, d: Dimension, y: Float) {
    val cirlce = Circle(d.w3x2, y, 80f)
    c.drawCircle(cirlce, fillOfWhite())
}

private fun drawHill(c: Canvas, d: Dimension, y: Float, hillHeight: Float, color: Int) {
    val layer = midpointDisplacementY(Point(0, y), Point(d.w, y), 0.7f + y / 900f, hillHeight, 10)
    layer.forEach {
        c.drawLine(it.x, it.y, it.x, d.hf, strokeOf(color, 1f))
    }
    val gap = rndf(20f, 40f)
    layer.forEach {
        c.drawCircle(it.x - 10f, it.y - gap, 8f, strokeOf(color, 1f).apply {
            this.alpha = 80
        })
    }
}
