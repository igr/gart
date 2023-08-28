package studio.oblac.gart.util

import studio.oblac.gart.Dimension
import studio.oblac.gart.Gartvas
import studio.oblac.gart.gfx.strokeOfBlue
import studio.oblac.gart.gfx.strokeOfRed
import studio.oblac.gart.math.middleAngle
import studio.oblac.gart.skia.Point
import kotlin.math.cos
import kotlin.math.sin

/**
 * Flow is a vector that represents the direction and magnitude of a flow.
 * @param direction in radians, indicates the direction of the flow. The angle is measured from the negative x-axis.
 * 0 is up, PI/2 is right, PI is down, 3PI/2 is left.
 */
data class Flow(val direction: Float, val magnitude: Float = 1f) {
    operator fun plus(flow: Flow): Flow {
        return Flow(middleAngle(direction, flow.direction), (magnitude + flow.magnitude) / 2)
    }

    /**
     * Calculates the offset of a point by the flow.
     */
    fun offset(p: Point): Point {
        val dx = sin(direction) * magnitude
        val dy = -cos(direction) * magnitude
        return Point(p.x + dx, p.y + dy)
    }
}

typealias FlowGenerator = (Float, Float) -> Flow

class FlowField(val w: Int, val h: Int, private val field: Array<Array<Flow>>) {

    operator fun get(x: Int, y: Int): Flow {
        return field[x][y]
    }

    companion object {
        fun of(d: Dimension, supplier: FlowGenerator) =
            of(d.w, d.h, supplier)

        fun of(width: Int, height: Int, supplier: FlowGenerator): FlowField {
            val field = Array(width) { x ->
                Array(height) { y ->
                    supplier(x.toFloat(), y.toFloat())
                }
            }
            return FlowField(width, height, field)
        }
    }


    /**
     * Utility to visualise the field.
     */
    fun drawField(g: Gartvas, gap: Int = 20) {
        g.d.forEach { x, y ->
            if (x % gap != 0 || y % gap != 0) {
                return@forEach
            }
            val f = this[x, y]
            val xf = x.toFloat()
            val yf = y.toFloat()
            g.canvas.drawPoint(xf, yf, strokeOfRed(2.1f))
            val dx = sin(f.direction) * f.magnitude * gap
            val dy = -cos(f.direction) * f.magnitude * gap
            g.canvas.drawLine(xf, yf, x + dx, y + dy, strokeOfBlue(1f))
        }
    }

}
