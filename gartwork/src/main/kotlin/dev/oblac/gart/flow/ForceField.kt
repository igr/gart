package dev.oblac.gart.flow

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartvas
import dev.oblac.gart.gfx.strokeOfBlue
import dev.oblac.gart.gfx.strokeOfRed
import kotlin.math.cos
import kotlin.math.sin

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
