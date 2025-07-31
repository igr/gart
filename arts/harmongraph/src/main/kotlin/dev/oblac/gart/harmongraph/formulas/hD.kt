package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.plus
import dev.oblac.gart.math.Transform
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

fun main() {
    val gart = Gart.of("hD", 1024, 1024)
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
private val colorBold = RetroColors.teal01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    drawTriangles(c, d, colorBack, colorFront)
    val r = Transform.rotate(d.cx, d.cy, Degrees.of(45)) + Transform.rotate(d.cx, d.cy, Degrees.of(45))
    repeat(600) { i ->
        val deltaPhase = i * 0.1f
        funD(deltaPhase, deltaPhase)
            .map { it + d.center }
            .map { r(it) }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 180))
    }
}

private fun funD(dp1: Float, dp2: Float): List<Point> {
    return generateSequence(0.0f) { it + 0.01f }
        .take(150)
        .map { morphingLissajous(it, dp1, dp2) }
        .toList()
}

private fun morphingLissajous(t: Float, dp1: Float, dp2: Float): Point {
    val scale = 300f
    val timeVar1 = 0.02f * t * t
    val timeVar2 = 0.015f * t * t
    val decay1 = exp(-0.005 * t)
    val decay2 = exp(-0.003 * t)

    val x = (sin(5 * t + timeVar1 + dp1) + 0.3 * sin(7 * t + dp1) * decay1) * scale
    val y = (cos(3 * t - timeVar2 + dp2) + 0.4 * cos(11 * t + dp2) * decay2) * scale
    return Point(x, y)
}
