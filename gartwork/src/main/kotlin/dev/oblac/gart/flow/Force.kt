package dev.oblac.gart.flow

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.strokeOfBlue
import dev.oblac.gart.gfx.strokeOfRed
import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.Point
import kotlin.math.cos
import kotlin.math.sin

interface Force<T : Force<T>> {
    val direction: Float
    val magnitude: Float

    /**
     * Calculates the offset of the point by the force.
     * Used to determine the next position of the point.
     */
    fun offset(p: Point): Point
    operator fun plus(other: T): T
}

/**
 * Generates a force at the given point.
 */
fun interface ForceGenerator<T : Force<T>> {
    operator fun invoke(x: Float, y: Float): T
}

class ForceField<T : Force<T>>(val w: Int, val h: Int, private val field: Array<Array<T>>) {

    operator fun get(x: Int, y: Int): T {
        return field[x][y]
    }

    companion object {
        inline fun <reified T : Force<T>> of(d: Dimension, supplier: ForceGenerator<T>) = of(d.w, d.h, supplier)

        inline fun <reified T : Force<T>> of(width: Int, height: Int, supplier: ForceGenerator<T>): ForceField<T> {
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
            val dx = sin(f.direction) * f.magnitude * gap
            val dy = -cos(f.direction) * f.magnitude * gap
            c.drawPoint(xf, yf, strokeOfRed(2.5f))
            c.drawLine(xf, yf, x + dx, y + dy, strokeOfBlue(1f))
        }
    }

}
