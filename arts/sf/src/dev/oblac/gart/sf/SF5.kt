package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.doubleLoop
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("sf5", 1024, 1024)
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

    //drawGrid(c, d)

    val circle1 = Circle(d.center, 160f)
    val circle2 = Circle(d.center, 320f)

    val points = circle2.points(1000)
    points.forEach { drawRay(c, d, circle1, it) }

    c.drawCircle(circle1, fillOf(colorBack))
    c.drawCircle(circle1, strokeOf(colorBack, 2f).apply {
        pathEffect = PathEffect.makeDiscrete(0.1f, 4f, rndi(1000))
    })

    c.drawRoundBorder(d, 10f, 40f, colorInk)
}

private fun drawGrid(c: Canvas, d: Dimension) {
    doubleLoop(Pair(100f, 100f), d.wf, d.hf, Pair(200f, 200f)) { (x, y) ->
        val point = Point(x, y)
        c.drawLine(
            Line(point.offset(-10f, -10f), point.offset(10f, 10f)),
            strokeOf(colorBold, 1f)
        )
        c.drawLine(
            Line(point.offset(10f, -10f), point.offset(-10f, 10f)),
            strokeOf(colorBold, 1f)
        )
    }
}

private fun drawRay(
    c: Canvas,
    d: Dimension,
    circle1: Circle,
    lastPoint: Point
) {

    val rayLine = Line(circle1.center, lastPoint)
    val visibleLine = Line(rayLine.pointFromStartLen(circle1.radius), lastPoint)

    visibleLine.toPath().toPoints(100)
    val randomLen = rndGaussian(
        mean = visibleLine.length() / 2f,
        standardDeviation = visibleLine.length() / 4f
    )

    val line = Line(
        visibleLine.a,
        visibleLine.pointFromEndLen(randomLen)
    )

    c.drawLine(line, strokeOf(colorInk, 1.5f).also {
        it.pathEffect = PathEffect.makeDash(floatArrayOf(rndf(90f, 140f), rndf(8f, 28f)), 4f)
    })
}
