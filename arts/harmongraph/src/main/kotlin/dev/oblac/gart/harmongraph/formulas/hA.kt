package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

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
    drawTriangles(c, d)
    repeat(800) { i ->
        val deltaPhase = i * 0.1f
        funA(deltaPhase, deltaPhase)
            .map { it + d.center }
            .zipWithNext()
            .forEach(drawLine(c, d))
    }
}

private fun drawLine(c: Canvas, d: Dimension): (Pair<Point, Point>) -> Unit = { (a, b) ->
    val colorF = strokeOf(colorFront, 1f).apply { alpha = 100 }
    val colorB = strokeOf(colorBold, 1f).apply { alpha = 100 }
    val diagonal = Line(d.leftTop, d.rightBottom)

    if (a.x < a.y && b.x > b.y) {
        val intersection = intersectionOf(Line(a, b), diagonal)!!
        c.drawLine(a, intersection, colorB)
        c.drawLine(intersection, b, colorF)
    } else if (a.x > a.y && b.x < b.y) {
        val intersection = intersectionOf(Line(a, b), diagonal)!!
        c.drawLine(a, intersection, colorF)
        c.drawLine(intersection, b, colorB)
    } else if (a.x > a.y) {
        c.drawLine(a, b, colorF)
    } else {
        c.drawLine(b, a, colorB)
    }
}

private fun drawTriangles(c: Canvas, d: Dimension) {
    val backTriangle1 = Triangle(
        d.leftTop,
        d.rightTop,
        d.rightBottom
    )
    val backTriangle2 = Triangle(
        d.leftTop,
        d.leftBottom,
        d.rightBottom
    )
    c.drawTriangle(backTriangle1, fillOf(colorBack))
    c.drawTriangle(backTriangle2, fillOf(colorFront))
}

private fun funA(dp1: Float, dp2: Float) = harmongraph(
    iterations = 70,
    delta = 0.1f,
    A = 400f,
    B = 100f,
    a = 1.7f,
    b = 0.2f,
    p1 = -7f + dp1,
    p2 = -7f + dp2,
    d1 = -0.6f,
    d2 = 0.2f
)

