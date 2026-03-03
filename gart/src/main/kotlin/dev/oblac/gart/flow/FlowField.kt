package dev.oblac.gart.flow

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.isInside
import dev.oblac.gart.gfx.strokeOfBlue
import dev.oblac.gart.gfx.strokeOfRed
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

class FlowField(val w: Int, val h: Int, private val field: Array<Array<Flow>>) {

    private val fieldDimension = Dimension(w, h)

    operator fun get(x: Int, y: Int): Flow = field[x][y]
    operator fun get(point: Point): Flow = get(point.x, point.y)
    operator fun get(x: Number, y: Number) = get(x.toInt(), y.toInt())

    /**
     * Applies the flow field to the given points and returns the updated list of new points.
     * The pointConsumer is called for each point with the original and new point,
     * allowing for side effects such as drawing or logging.
     * Points that are outside the field dimension are ignored.
     */
    fun apply(points: List<Point>, pointConsumer: (Point, Point) -> Unit): List<Point> {
        if (points.isEmpty()) {
            return points
        }
        val result = mutableListOf<Point>()
        for (point in points) {
            if (!point.isInside(fieldDimension)) {
                continue
            }
            val newPoint = this[point].offset(point)
            pointConsumer(point, newPoint)
            result.add(newPoint);
        }
        return result
    }


    companion object {
        /**
         * Creates a flow field from a function that generates a flow for each point.
         */
        fun of(d: Dimension, fn: (Float, Float) -> Flow) = FlowField(
            d.w, d.h,
            Array(d.w) { x ->
                Array(d.h) { y ->
                    fn(x.toFloat(), y.toFloat())
                }
            })

        /**
         * Creates a force field from a function that generates a vector for each point.
         * Points are defined with the index. Used when mapping from one space into another.
         */
        fun from(d: Dimension, fn: (Int, Int) -> Vector2) = FlowField(
            d.w, d.h,
            Array(d.w) { x ->
                Array(d.h) { y ->
                    Flow { fn(x, y) }
                }
            })
    }

    /**
     * Utility to visualise the field.
     */
    fun drawField(c: Canvas, d: Dimension, gap: Int = 20) {
        d.forEach { x, y ->
            if (x % gap != 0 || y % gap != 0) {
                return@forEach
            }
            val f = this[x, y]
            val xf = x.toFloat()
            val yf = y.toFloat()

            val v = f(Point(xf, yf)).normalize() * 10f

            c.drawPoint(xf, yf, strokeOfRed(2.5f))
            c.drawLine(xf, yf, x + v.x, y + v.y, strokeOfBlue(1f))
        }
    }

}
