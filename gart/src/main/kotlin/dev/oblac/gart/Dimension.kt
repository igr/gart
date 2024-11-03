package dev.oblac.gart

import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

/**
 * Represents a virtual dimensions.
 * todo: migrate w/h to float
 */
data class Dimension(val w: Int, val h: Int) {
    val wf = w.toFloat()
    val hf = h.toFloat()
    val wd = w.toDouble()
    val hd = h.toDouble()

    /**
     * Center X.
     */
    val cx = w / 2f

    /**
     * Center Y.
     */
    val cy = h / 2f

    /**
     * Center point.
     */
    val center = Point(cx, cy)

    /**
     * Third of the width.
     */
    val w3 = w / 3f

    /**
     * Third of the height.
     */
    val h3 = h / 3f

    val rect = Rect(0f, 0f, wf, hf)

    /**
     * Right edge.
     */
    val r = w - 1

    /**
     * Right edge as float.
     */
    val rf = r.toFloat()

    /**
     * Bottom edge.
     */
    val b = h - 1

    /**
     * Bottom edge as float.
     */
    val bf = b.toFloat()

    /**
     * Box area.
     */
    val area = w * h

    fun isInside(x: Float, y: Float) = x.toInt() in 0 until w && y.toInt() in 0 until h

    /**
     * Iterates over the all elements of the rectangle dimension.
     */
    fun forEach(consumer: (x: Int, y: Int) -> Unit) {
        for (j in 0 until h) {
            for (i in 0 until w) {
                consumer(i, j)
            }
        }
    }

    /**
     * Grows the dimension by the factor.
     */
    operator fun times(factor: Number) = Dimension((w * factor.toFloat()).toInt(), (h * factor.toFloat()).toInt())

    companion object {
        val DESKTOP_FULL_HD = Dimension(1920, 1080)
        val LAPTOP_FULL_HD = Dimension(1366, 768)
    }
}
