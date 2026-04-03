package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.plus
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("hF", 1024, 1024)
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
private val colorFront = RetroColors.white01
private val colorBold = RetroColors.red01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    drawTriangles(c, d, colorBack, colorFront)
    repeat(600) { i ->
        val deltaPhase = i * 0.1f
        funD(deltaPhase, deltaPhase)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 100))
    }
}

private fun funD(dp1: Float, dp2: Float): List<Point> {
    return generateSequence(0.0f) { it + 0.01f }
        .take(180)
        .map { t ->
            orbitalDance(t, dp1, dp2)
        }
        .toList()
}

private fun orbitalDance(t: Float, dp1: Float, dp2: Float): Point {
    val scale = 320f
    val x = (sin(1.2 * t + dp1) * cos(3.7 * t + dp1) +
        0.6 * sin(2.1 * t + PI / 6 + dp1)).toFloat() * scale
    val y = (cos(0.9 * t + dp2) * sin(4.3 * t + dp2) +
        0.4 * cos(1.8 * t + PI / 3 + dp2)).toFloat() * scale
    return Point(x, y)
}
