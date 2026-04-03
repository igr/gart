package dev.oblac.gart.flamebrush

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.dist
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("flamebrush1", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val d = gart.d
    val c = g.canvas

    c.clear(CssColors.black)

    // spirals
    for (i in 0 until 10) {
        srp(d, c, i.toFloat() * 40f)
    }

    gart.saveImage(g)
    gart.window().showImage(g)
}

private fun srp(d: Dimension, c: Canvas, offset: Float) {
    val brush = SpiralBrush(20)
    val p = circlePath(d.cx + offset, d.cy, 400f - offset)
    val motionPath = pointsOn(p, 300, EaseFn.ExpoInOut)
    var prev = motionPath[0]
    motionPath.forEach {
        drawFoo(c, prev, it, brush)
        prev = it
        brush.tick()
    }
}

val p = Palettes.cool22.reversed()

fun drawFoo(c: Canvas, prev: Point, point: Point, brush: SpiralBrush) {
    val d = dist(prev, point)
    brush.points(point, d * 4).forEach {
        c.drawCircle(it, 1f + d / 60f + rndf(40), fillOf(p.safe(d.toInt() / 6)))
    }
}


fun circlePath(x: Float, y: Float, r: Float): Path {
    val points = mutableListOf<Point>()
    val steps = 40
    val angle = 2 * Math.PI / steps
    for (i in 0 until steps) {
        val x1 = x + r * cos(i * angle)
        val y1 = y + r * sin(i * angle)
        points.add((x1 to y1).toPoint())
    }

    return points.toPath()

}
