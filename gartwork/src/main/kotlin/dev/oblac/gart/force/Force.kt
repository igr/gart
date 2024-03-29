package dev.oblac.gart.force

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
        fun of(d: Dimension, fn: (Float, Float) -> Force) = ForceField(d.w, d.h,
            Array(d.w) { x ->
                Array(d.h) { y ->
                    fn(x.toFloat(), y.toFloat())
                }
            })

        fun ofVectors(d: Dimension, fn: (Float, Float) -> Vector) = ForceField(d.w, d.h,
            Array(d.w) {
                Array(d.h) {
                    object : Force {
                        override fun apply(p: Point): Vector {
                            return fn(p.x, p.y)
                        }
                    }
                }
            })

        /**
         * Creates a force field from a function that generates a vector for each point.
         * Points are defined with the index. Used when mapping from one space into another.
         */
        fun from(d: Dimension, fn: (Int, Int) -> Vector) = ForceField(d.w, d.h,
            Array(d.w) { x ->
                Array(d.h) { y ->
                    object : Force {
                        override fun apply(p: Point): Vector {
                            return fn(x, y)
                        }
                    }
                }
            })

        fun of(d: Dimension, forceGenerator: ForceGenerator) = of(d.w, d.h, forceGenerator)

        fun of(width: Int, height: Int, forceGenerator: ForceGenerator): ForceField {
            return ForceField(width, height,
                Array(width) { x ->
                    Array(height) { y ->
                        forceGenerator(x.toFloat(), y.toFloat())
                    }
                })
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
