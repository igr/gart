package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.plus
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.exp
import kotlin.math.sin

fun main() {
    val gart = Gart.of("hC", 1024, 1024)
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
private val colorBold = RetroColors.purple01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    drawTriangles(c, d, colorBack, colorFront)

    repeat(700) { i ->
        val deltaPhase = i * 0.1f
        funC(deltaPhase, deltaPhase, 200)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 90))
    }

    repeat(700) { i ->
        val deltaPhase = i * 0.1f
        funC2(deltaPhase, deltaPhase, 400)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 90))
    }
}

private fun funC(dp1: Float, dp2: Float, scale: Int): List<Point> {
    return generateSequence(0.0f) { it + 0.01f }
        .take(190)
        .map { stack(it, dp1, dp2, scale) }
        .toList()
}

private fun stack(t: Float, dp1: Float, dp2: Float, scale: Int): Point {
    val x = scale * sin(1.3 * t + 0.03 + dp1) * exp(-1.5 * t)
    val y = scale * sin(-0.5 * t + 0.03 + dp2) * exp(0.1 * t)
    return Point(x, y)
}

private fun funC2(dp1: Float, dp2: Float, scale: Int): List<Point> {
    return generateSequence(0.0f) { it + 0.01f }
        .take(190)
        .map { stack2(it, dp1, dp2, scale) }
        .toList()
}

private fun stack2(t: Float, dp1: Float, dp2: Float, scale: Int): Point {
    val x = scale * sin(1.3 * t + 0.09 + dp1) * exp(-1.5 * t)
    val y = scale * sin(-1.5 * t + 0.03 + dp2) * exp(-0.1 * t)
    return Point(x, y)
}
