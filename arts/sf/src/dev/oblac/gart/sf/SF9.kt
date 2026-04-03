package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.noise.Perlin
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("sf9", 1024, 1024)
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

    val center = d.center.offset(100f, 100f)

    repeat(600) {
        drawWiggyCircle(c, center.offset(
            dx = sin(it * 0.1f + 1f) * 100f,
            dy = cos(it * 0.04f) * 100f,
        ), it * 8f, colorInk)
    }

    val moonCenter = center.offset(50f, 100f)
    val moonRadius = 80f
    val moon = Circle(moonCenter, moonRadius)

    c.drawCircle(moon, fillOf(colorBold))

    c.drawRoundBorder(d, 10f, 40f, colorInk)
}

private fun drawWiggyCircle(c: Canvas, center: Point, radius: Float, color: Int) {
    val pscale = 0.01

    Circle(center.x, center.y, radius)
        .points(300)
        .map {
            val noise = Perlin.noise(it.x * pscale, it.y * pscale, 0.0)
            it.moveTowards(center, -noise.toFloat() * 20f)
        }
        .toClosedPath()
        .let {
            c.drawPath(it, strokeOf(color, 1f).also {
                it.alpha = 190
            })
        }

}
