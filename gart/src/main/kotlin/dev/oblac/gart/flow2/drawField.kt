package dev.oblac.gart.flow2

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.vector.MutableVec2
import org.jetbrains.skia.Canvas
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// arrow heads: barbs 0.3 of the arrow but never over 4 px, turned 25° off the shaft
private const val HEAD = 0.3f
private const val HEAD_MAX = 4f
private val HEAD_COS = cos(Math.toRadians(25.0)).toFloat()
private val HEAD_SIN = sin(Math.toRadians(25.0)).toFloat()

/**
 * Draws [field] as arrows on a grid [gap] px apart, for a quick look at what a field does. Each
 * arrow starts at a dot on a grid point and points along the vector there. All arrows are 0.8 of
 * [gap] long; with [scaled] only the longest one is, and the others are shorter in proportion to
 * their vectors. A point where the field has no direction gets only its dot.
 */
fun Canvas.drawField(field: VectorField, d: Dimension, gap: Float = 20f, color: Int = 0xFF2F55D4.toInt(), scaled: Boolean = false) {
    // grid points at gap / 2 + i * gap, so the arrows sit clear of the edges
    val nx = ceil((d.wf - gap / 2f) / gap).toInt()
    val ny = ceil((d.hf - gap / 2f) / gap).toInt()
    if (nx <= 0 || ny <= 0) return
    val vx = FloatArray(nx * ny)
    val vy = FloatArray(nx * ny)
    val v = MutableVec2()
    var longest = 0.0
    for (j in 0 until ny) {
        for (i in 0 until nx) {
            field.at(gap / 2f + i * gap, gap / 2f + j * gap, v)
            vx[j * nx + i] = v.x
            vy[j * nx + i] = v.y
            val len = lengthOf(v.x, v.y)
            if (len > longest) longest = len  // a plain max would let a NaN through
        }
    }

    val ink = strokeOf(color, 1f)
    val dot = fillOf(color)
    for (j in 0 until ny) {
        for (i in 0 until nx) {
            val x = gap / 2f + i * gap
            val y = gap / 2f + j * gap
            drawCircle(x, y, 1.5f, dot)
            val k = j * nx + i
            val len = lengthOf(vx[k], vy[k])
            if (!(len > 1e-6)) continue  // true for NaN too, that is a point with no direction
            val arrow = if (scaled) (0.8 * gap * len / longest).toFloat() else 0.8f * gap
            val ux = (vx[k] / len).toFloat()
            val uy = (vy[k] / len).toFloat()
            val tx = x + ux * arrow
            val ty = y + uy * arrow
            drawLine(x, y, tx, ty, ink)
            val head = min(HEAD * arrow, HEAD_MAX)
            drawLine(tx, ty, tx - head * (ux * HEAD_COS - uy * HEAD_SIN), ty - head * (ux * HEAD_SIN + uy * HEAD_COS), ink)
            drawLine(tx, ty, tx - head * (ux * HEAD_COS + uy * HEAD_SIN), ty - head * (uy * HEAD_COS - ux * HEAD_SIN), ink)
        }
    }
}
