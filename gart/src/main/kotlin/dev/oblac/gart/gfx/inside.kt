package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.alpha
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.Point

class IsPointInPath(private val d: Dimension, path: Path) {
    private val g = Gartvas(d)
    private val b: Gartmap

    init {
        val canvas = g.canvas

        // Clear canvas
        canvas.clear(0x00000000)

        // Fill path with opaque color
        val paint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            mode = PaintMode.FILL
            isAntiAlias = true
        }

        canvas.drawPath(path, paint)

        b = Gartmap(g)
    }

    fun check(p: Point): Boolean {
        if (!p.isInside(d)) {
            // Point is outside the canvas, so it cannot be inside the path
            return false
        }
        val pixel = b[p.x.toInt(), p.y.toInt()]

        // if alpha channel is not 0, point is inside
        return alpha(pixel) > 0
    }

}

/**
 * Walk through each contour and use a ray-casting algorithm to test if the point is inside.
 */
fun isPointInsideClosedPath(path: Path, x: Float, y: Float, precision: Float = 1f): Boolean {
    val measure = PathMeasure(path, false)
    var totalCrossings = 0

    do {
        val length = measure.length
        val points = mutableListOf<Point>()
        var distance = 0f

        while (distance <= length) {
            val pos = measure.getPosition(distance)!!
            points.add(Point(pos.x, pos.y))
            distance += precision
        }

        // Ray-casting: Count how many times a horizontal ray crosses the path
        var crossings = 0
        for (i in points.indices) {
            val p1 = points[i]
            val x1 = p1.x
            val y1 = p1.y
            val p2 = points[(i + 1) % points.size]
            val x2 = p2.x
            val y2 = p2.y

            // Check if ray crosses segment
            if ((y1 > y) != (y2 > y)) {
                val intersectX = x1 + (y - y1) * (x2 - x1) / (y2 - y1)
                if (intersectX > x) {
                    crossings++
                }
            }
        }

        totalCrossings += crossings

    } while (measure.nextContour())

    return totalCrossings % 2 == 1
}
