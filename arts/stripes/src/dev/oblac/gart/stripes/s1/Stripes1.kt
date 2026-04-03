package dev.oblac.gart.stripes.s1

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import kotlin.math.sin

fun main() {
    val gart = Gart.of("stripes1", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val d = g.d
    val c = g.canvas
    val w = gart.window()

    draw(c, d)
    gart.saveImage(g)
    w.showImage(g)
}

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    val gap = 40

    drawFromBottom(c, d, gap)
    drawFromTop(c, d, gap)

    c.drawBorder(d, 30f, colorBack)

    // delete
    c.drawLine(d.wf - 32f, 0f, d.w - 32f, d.hf, strokeOf(colorBack, 5f))
}

private val stroke = 20f

private fun drawFromBottom(c: Canvas, d: Dimension, gap: Int) {
    val h = 800f

    val yyOff = rndf(-0.4f, 0.4f)
    val yyPeriod = 0.005f//rndf(0.0005f, 0.002f)

    (0 until d.w step gap).forEachIndexed { i, x ->
        val xx = x.toFloat()
        val yy = sin(yyOff + xx * yyPeriod) * h / 2 + 300f

        val line = Line.of(xx, yy, xx, d.hf)
        c.drawLine(line, strokeOf(colorInk, stroke).apply {
            this.strokeCap = PaintStrokeCap.ROUND
        })
    }
}

private fun drawFromTop(c: Canvas, d: Dimension, gap: Int) {
    val h = 600f
    val y = d.hf

    val yyPeriod = 0.004f
    (0 until d.w step gap).forEachIndexed { i, x ->
        val xx = (x + gap / 2).toFloat()
        val yy = y - d.cy + sin(xx * yyPeriod) * h / 2

        val line = Line.of(xx, yy, xx, 0f)

        c.drawLine(line, strokeOf(colorInk, stroke).apply {
            this.strokeCap = PaintStrokeCap.ROUND
        })

        if (i == 7) {
            c.drawCircle(300f, 280f, 180f, fillOf(colorBold))
        }
    }
}
