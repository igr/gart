package dev.oblac.gart.lines

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("triangles", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)
    //gart.saveImage(g)
    w.showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.white01)

    val count = 12
    val gap = d.wf / count

    val allTs = mutableListOf<Triangle>()
    for (i in 0 until count) {
        val sideGap = rndf(20f, 100f)
        val line = Line(
            Point(sideGap, gap / 2 + i * gap),
            Point(d.wf - sideGap, gap / 2 + i * gap)
        )
        val ts = lineOfTriangles(d, line, i % 2 == 0, gap, allTs)
        allTs.addAll(ts)
    }
    c.drawCircle(-100f, d.cy, 600f, fillOf(RetroColors.red01))
    allTs.forEach {
        c.drawTriangle(it, fillOf(RetroColors.black01))
    }
}

private fun lineOfTriangles(d: Dimension, line: Line, firstUp: Boolean, gap: Float, allTriangles: List<Triangle>): List<Triangle> {
    val triangles = mutableListOf<Triangle>()

    val points = 1000
    val ps = line.points(points)

    var ndx = 0
    var up = firstUp
    while (ndx < points) {
        while (true) {
            val len = minOf(rndi(60, 120), points - ndx)
            if (len < 40) {
                ndx += len
                break
            }
            val leftPoint = ps[ndx]
            val rightPoint = ps[ndx + len - 1]

            val middle = len * rndf(0.2f, 0.8f)
            val middlePoint = ps[ndx + middle.toInt()]

            val biiiig = rndb(7, 10)
            val height = if (biiiig) {
                gap * rndf(0.6f, 0.8f) * if (up) -1f else 1f
            } else {
                gap * rndf(0.2f, 0.4f) * if (up) -1f else 1f
            }
//            val height = gap * rndf(0.4f, 0.8f) * if (up) -1f else 1f

            val topPoint = Point(middlePoint.x, middlePoint.y + height)
            if (topPoint.y < 0 || topPoint.y > d.hf) {
                continue
            }
            val triangle = Triangle(leftPoint, topPoint, rightPoint)
            if (isTriangleColliding(triangle, allTriangles)) {
                //ndx += len
                //break
                continue
            }
            triangles.add(triangle)
            ndx += len
            break
        }
        up = !up
    }
    return triangles
}

private fun isTriangleColliding(triangle: Triangle, allTriangles: List<Triangle>): Boolean {
    return allTriangles.any { it != triangle && it.intersect(triangle) }
}
