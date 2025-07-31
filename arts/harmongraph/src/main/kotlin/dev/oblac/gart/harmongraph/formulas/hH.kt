package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.plus
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("hH", 1024, 1024)
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
private val colorBold = RetroColors.black01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    drawTriangles(c, d, colorBack, colorFront)
    repeat(900) { i ->
        val deltaPhase = i * 0.1f
        funD(deltaPhase, deltaPhase)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 100))
    }
}

private fun funD(dp1: Float, dp2: Float): List<Point> {
    return generateSequence(0.0f) { it + 0.01f }
        .take(250)
        .map { flowerOfLife(it, dp1, dp2) }
        .toList()
}

private fun flowerOfLife(t: Float, dp1: Float, dp2: Float): Point {
    val scale = 280f
    val x = (sin(t + dp1) + sin(3 * t + dp1) / 3 + sin(5 * t + dp1) / 5) * scale
    val y = (cos(t + dp2) + cos(3 * t + dp2) / 3 + cos(5 * t + dp2) / 5) * scale
    return Point(x, y)
}
