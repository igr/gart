package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.plus
import dev.oblac.gart.harmongraph.harmongraph2
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("hA", 1024, 1024)
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
private val colorBold = RetroColors.blue01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    drawTriangles(c, d, colorBack, colorFront)
    repeat(800) { i ->
        val deltaPhase = i * 0.1f
        funA(deltaPhase, deltaPhase)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawHarmonographLine(c, d, colorFront, colorBold, 100))
    }
}

private fun funA(dp1: Float, dp2: Float) = harmongraph2(
    iterations = 70,
    delta = 0.1f,
    a = 400f,
    b = 100f,
    f1 = 1.7f,
    f2 = 0.2f,
    p1 = -7f + dp1,
    p2 = -7f + dp2,
    d1 = -0.6f,
    d2 = 0.2f
)

