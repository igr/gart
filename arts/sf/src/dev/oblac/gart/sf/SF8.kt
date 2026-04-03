package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import dev.oblac.gart.smooth.chaikinSmooth
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("sf8", 1024, 1024)
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

    val circle1 = Circle(d.center.offset(0f, -200f), 160f)
    val circle2 = Circle(d.center.offset(0f, -200f), 1320f)

    val points = circle2.points(120)
    points.forEach { drawRay(c, circle1, it) }

    drawCloud(c, d)

    c.drawCircle(circle1, fillOf(colorBack))

    c.drawRoundBorder(d, 10f, 40f, colorInk)
}

private fun drawCloud(c: Canvas, d: Dimension) {
    val y = d.cy
    val line = Line(Point(0f, y + rndf(100f, 200f)), Point(d.w, y + rndf(100f, 200f)))
    val p = line.points(10).map {
        it.offset(0f, rndf(-50f, 250f))
    }

    //c.drawCircle(p[2], 100f, fillOf(colorBold))

    val p2 = chaikinSmooth(p, iterations = 4, closed = false, bias = 0.2)
    println(p2.size)
    p2.forEach {
        c.drawCircle(Circle(it, rndGaussian(50f, 20f)), fillOf(colorInk))
    }

//    p.forEach {
//        c.drawCircle(Circle(it, rndf(120f, 150f)), fillOf(colorBold))
//    }

    val path = p.toPathBuilder()
    path.lineTo(d.rightBottom)
    path.lineTo(d.leftBottom)
    path.closePath()
    c.drawPath(path.detach(), fillOf(colorInk))
//    c.drawPath(path, strokeOf(colorInk, 2f))

//    p.forEach {
//        c.drawPoint(it, strokeOf(colorBold, 6f))
//    }
}

private fun Line.points(count: Int): List<Point> {
    val step = this.length() / (count - 1)
    return (0 until count).map { i ->
        val t = i * step
        this.pointFromStartLen(t)
    }
}

private fun drawRay(c: Canvas, circle: Circle, lastPoint: Point) {
    val line = Line(circle.center, lastPoint.offset(rndf(-20f, 20f), rndf(-20f, 20f)))
    c.drawLine(line, strokeOf(colorInk, rndf(2f, 3f)))
}
