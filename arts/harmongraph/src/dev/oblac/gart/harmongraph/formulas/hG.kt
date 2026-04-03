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
    val gart = Gart.of("hG", 1024, 1024)
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
private val colorBold = RetroColors.orange01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    drawTriangles(c, d, colorBack, colorFront)
    repeat(800) { i ->
        val deltaPhase = i * 0.15f
        funD(deltaPhase, deltaPhase)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 80))
    }
}

private fun funD(dp1: Float, dp2: Float): List<Point> {
    return generateSequence(0.0f) { it + 0.01f }
        .take(450)
        .map { t -> celticKnot(t, dp1, dp2) }
        .toList()
}

private fun celticKnot(t: Float, dp1: Float, dp2: Float): Point {
    val scale = 280f
    val x = (sin(3 * t + PI / 4 + dp1) + 0.5 * sin(5 * t + dp1)).toFloat() * scale
    val y = (cos(2 * t + dp2) + 0.7 * cos(7 * t + PI / 3 + dp2)).toFloat() * scale
    return Point(x, y)
}
