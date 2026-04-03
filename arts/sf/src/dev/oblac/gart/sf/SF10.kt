package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.PathEffect
import kotlin.math.sin

fun main() {
    val gart = Gart.of("sf10", 1024, 1024)
    println(gart)
    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g)

    w.showImage(g)
}

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)

    val circle = Circle(d.w3x2, d.h3x2, d.w * 0.3f)
    c.drawCircle(circle, fillOf(colorBold))

    val linesCount = 800
    val delta = d.wf / linesCount + 0.3f
    var xx = 0f
    c.save()
    c.clipPath(circle.toPath())
    c.rotate(-25f, d.wf, d.hf)
    repeat(linesCount) {
        xx += delta
        drawYline(c, d, xx, 600 + 200 * it / d.w + xx * sin(xx /4) * 0.35f + rndf(150f))
    }
    c.restore()

    repeat(10) {
        val circle = Circle(d.w3x2 - it * 20, d.h3x2 - it * 40, d.w * (0.3f + it * 0.05f))
        c.drawCircle(circle, strokeOf(colorBold, 20f - it * 2).apply {
            this.pathEffect = PathEffect.makeDiscrete(10f, 10f, 173)
            this.alpha = 255 - it * 20
            this.maskFilter = MaskFilter.makeBlur(
                FilterBlurMode.SOLID,
                (it + 1).toFloat(),
                false
            )
        })
    }
}

private fun drawYline(c: Canvas, d: Dimension, x: Float, upTo: Float) {
    for (y in upTo.toInt() until d.h) {
        c.drawPoint(x + rndf(-4f, 4f), y.toFloat(), strokeOf(colorInk, 2f))
    }
}
