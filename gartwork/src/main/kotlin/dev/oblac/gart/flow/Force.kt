package dev.oblac.gart.flow

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.offset
import dev.oblac.gart.gfx.strokeOfBlue
import dev.oblac.gart.gfx.strokeOfRed
import dev.oblac.gart.math.Vector
import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.Point

/**
 * Represents a force that acts on a point.
 */
interface Force {

    /**
     * Calculates the force vector (offset) at the given point.
     */
    fun apply(p: Point): Vector

    /**
     * Applies the force to the given point and returns the new point.
     */
    fun offset(p: Point) = apply(p).let { p.offset(it) }

}

/**
 * Generates a force at the given point.
 */
fun interface ForceGenerator {
    operator fun invoke(x: Float, y: Float): Force
}

class ForceField(val w: Int, val h: Int, private val field: Array<Array<Force>>) {

    operator fun get(x: Int, y: Int): Force {
        return field[x][y]
    }
    operator fun get(x: Number, y: Number) = get(x.toInt(), y.toInt())

    companion object {
        fun of(d: Dimension, supplier: ForceGenerator) = of(d.w, d.h, supplier)

        fun of(width: Int, height: Int, supplier: ForceGenerator): ForceField {
            val field = Array(width) { x ->
                Array(height) { y ->
                    supplier(x.toFloat(), y.toFloat())
                }
            }
            return ForceField(width, height, field)
        }
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

            val v = f.apply(Point(xf, yf)).normalize() * 10f

            c.drawPoint(xf, yf, strokeOfRed(2.5f))
            c.drawLine(xf, yf, x + v.x, y + v.y, strokeOfBlue(1f))
        }
    }

}
