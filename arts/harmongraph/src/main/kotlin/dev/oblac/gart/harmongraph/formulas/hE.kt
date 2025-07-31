package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.plus
import dev.oblac.gart.math.PIf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("hE", 1024, 1024)
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
private val colorBold = RetroColors.pink01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    drawTriangles(c, d, colorBack, colorFront)
    repeat(600) { i ->
        val deltaPhase = i * 0.1f
        funE(deltaPhase, deltaPhase)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 60))
    }
}

private fun funE(dp1: Float, dp2: Float): List<Point> {
    return generateSequence(0.0f) { it + 0.01f }
        .take(250)
        .map { quantumInterference(it, dp1, dp2) }
        .toList()
}

private fun quantumInterference(t: Float, dp1: Float, dp2: Float): Point {
    val scale = 260f
    val x = (sin(PIf * t + dp1) * sin(13 * PIf * t / 7 + dp1) +
        cos(2 * PIf * t + dp1) * sin(17 * PIf * t / 11 + dp1)) * scale
    val y = (cos(PIf * t + dp2) * cos(11 * PIf * t / 6 + dp2) +
        sin(3 * PIf * t + dp2) * cos(19 * PIf * t / 13 + dp2)) * scale
    return Point(x, y)
}
