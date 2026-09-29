package dev.oblac.gart.flow2

import dev.oblac.gart.Dimension
import dev.oblac.gart.math.lerp
import dev.oblac.gart.vector.MutableVec2
import kotlin.math.ceil
import kotlin.math.min

/**
 * A [VectorField] stored on a grid of nodes [cell] px apart, with the first node at
 * ([left], [top]). A read between the nodes interpolates the x and y parts bilinearly. It does
 * not interpolate angles, because an angle jumps where it wraps around. Off the grid, the value
 * at the nearest edge holds.
 *
 * Make one with [bake], or fill the arrays yourself (row by row, [cols] values per row) when
 * the vectors come from data, such as a simulation.
 */
class VectorGrid(
    val left: Float,
    val top: Float,
    val cell: Float,
    val cols: Int,
    val rows: Int,
    private val vx: FloatArray,
    private val vy: FloatArray,
) : VectorField {

    init {
        require(cell > 0f) { "cell must be positive, got $cell" }
        require(cols >= 2 && rows >= 2) { "a grid needs at least 2x2 nodes, got ${cols}x$rows" }
        require(vx.size == cols * rows && vy.size == cols * rows) { "the arrays must hold cols * rows values" }
    }

    override fun at(x: Float, y: Float, out: MutableVec2) {
        val fx = ((x - left) / cell).coerceIn(0f, (cols - 1).toFloat())
        val fy = ((y - top) / cell).coerceIn(0f, (rows - 1).toFloat())
        val i = min(fx.toInt(), cols - 2)
        val j = min(fy.toInt(), rows - 2)
        val tx = fx - i
        val ty = fy - j
        val k = j * cols + i
        out.set(
            lerp(lerp(vx[k], vx[k + 1], tx), lerp(vx[k + cols], vx[k + cols + 1], tx), ty),
            lerp(lerp(vy[k], vy[k + 1], tx), lerp(vy[k + cols], vy[k + cols + 1], tx), ty),
        )
    }
}

/**
 * Samples this field on a grid of nodes [cell] px apart, over the canvas plus [margin] px on
 * each side. Use it when the field is slow to compute, for example many octaves of noise, and a
 * tracer asks for it many times. The grid loses the detail that is smaller than a cell.
 */
fun VectorField.bake(d: Dimension, cell: Float = 8f, margin: Float = 0f): VectorGrid {
    val cols = ceil((d.wf + 2 * margin) / cell).toInt() + 1
    val rows = ceil((d.hf + 2 * margin) / cell).toInt() + 1
    val vx = FloatArray(cols * rows)
    val vy = FloatArray(cols * rows)
    val out = MutableVec2()
    for (j in 0 until rows) {
        for (i in 0 until cols) {
            at(-margin + i * cell, -margin + j * cell, out)
            vx[j * cols + i] = out.x
            vy[j * cols + i] = out.y
        }
    }
    return VectorGrid(-margin, -margin, cell, cols, rows, vx, vy)
}
