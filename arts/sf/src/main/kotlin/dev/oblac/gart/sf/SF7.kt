package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.rndGaussian
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.PaintStrokeCap

fun main() {
    val gart = Gart.of("sf7", 1024, 1024)
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

    val sun = Circle(d.center, 400f)
    c.save()
    c.rotate(-10f, d.cx, d.cy)
    drawSun(sun, c, d, colorInk, 80f) { x, y -> (x + y) / 2 + 140 }
    drawSun(sun, c, d, colorBold, 80f) { x, y -> (x + y) / 2 - 40 }
    c.restore()

}

private fun drawSun(sun: Circle, c: Canvas, d: Dimension, colorInk: Int, stdev: Float, mean: (Float, Float) -> Float) {
    c.save()
    c.clipPath(sun.toPath(), ClipMode.INTERSECT, true)
    val fromY = (sun.center.y - sun.radius).toInt()
    val toY = (sun.center.y + sun.radius).toInt()

    val fromX = sun.center.x - sun.radius
    val toX = sun.center.x + sun.radius / 3
    val meanX = mean(fromX, toX)

    // draw random lines from left edge
    for (i in fromY until toY step 18) {
//        val x = rndf(fromX, toX)
        val x = rndGaussian(meanX, stdev)
        val y = i.toFloat()
        val stroke = 10f
        c.drawLine(0f, y, x, y, strokeOf(colorInk, stroke).apply {
            this.strokeCap = PaintStrokeCap.ROUND
        })
    }
    c.restore()
}
