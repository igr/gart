package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.plus
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.PIf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("hB", 1024, 1024)
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
private val colorBold = RetroColors.green01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    drawTriangles(c, d, colorBack, colorFront)
    repeat(400) { i ->
        val deltaPhase = i * 0.1f
        funB(deltaPhase, deltaPhase)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 80))
    }
}

private fun funB(dp1: Float, dp2: Float): List<Point> {
    return generateSequence(0.0f) { it + 16f }
        .take(1000)
        .map { t -> spiralingRose(t, dp1, dp2) }
        .toList()
}

private fun spiralingRose(t: Float, dp1: Float, dp2: Float): Point {
    val A1 = 300f
    val A2 = 300f
    val A3 = 300f
    val A4 = 300f

    val f1 = 2f
    val f2 = 6f
    val f3 = 1f
    val f4 = 3f
    val d1 = 0.004f
    val d2 = 0.004f
    val d3 = 0.004f
    val d4 = 0.004f
    val p1 = dp1
    val p2 = HALF_PIf + dp1
    val p3 = dp2
    val p4 = PIf / 4 + dp2

    val decay1 = kotlin.math.exp(-d1 * t)
    val decay2 = kotlin.math.exp(-d2 * t)
    val decay3 = kotlin.math.exp(-d3 * t)
    val decay4 = kotlin.math.exp(-d4 * t)

    val x = A1 * kotlin.math.sin(f1 * t + p1) * decay1 +
        A2 * kotlin.math.sin(f2 * t + p2) * decay2
    val y = A3 * kotlin.math.sin(f3 * t + p3) * decay3 +
        A4 * kotlin.math.sin(f4 * t + p4) * decay4

    return Point(x, y)
}
