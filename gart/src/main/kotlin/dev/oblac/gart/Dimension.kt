package dev.oblac.gart

import dev.oblac.gart.math.f
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.sqrt

/**
 * Represents a virtual dimensions.
 * todo w/h should be floats
 */
data class Dimension(val w: Int, val h: Int) {
    val width = w.toFloat()
    val height = h.toFloat()
    val wf = w.toFloat()
    val hf = h.toFloat()
    val wd = w.toDouble()
    val hd = h.toDouble()

    val aspectRatio = wf / hf

    val rightBottom = Point(w.toFloat(), h.toFloat())
    val leftTop = Point(0f, 0f)
    val leftBottom = Point(0f, h.f())
    val rightTop = Point(w.f(), 0f)
    val leftMiddle = Point(0f, h / 2f)
    val rightMiddle = Point(w.f(), h / 2f)

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

    val w3x2 = w3 * 2f

    /**
     * Third of the height.
     */
    val h3 = h / 3f

    val h3x2 = h3 * 2f

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

    /**
     * Diagonal.
     */
    val diag = (sqrt(wf * wf + hf * hf)) / 2f

    fun isInside(x: Float, y: Float) = x.toInt() in 0 until w && y.toInt() in 0 until h

    /**
     * Iterates over the all elements of the rectangle dimension.
     */
    fun forEach(step: Int = 1, consumer: (x: Int, y: Int) -> Unit) {
        for (j in 0 until h step step) {
            for (i in 0 until w step step) {
                consumer(i, j)
            }
        }
    }

    fun loop(step: Int = 1, consumer: (x: Int, y: Int) -> Unit) = forEach(step, consumer)

    /**
     * Grows the dimension by the factor.
     */
    operator fun times(factor: Number) = Dimension((w * factor.toFloat()).toInt(), (h * factor.toFloat()).toInt())

    /**
     * Returns relative width for given factor.
     */
    fun ofW(x: Float) = wf * x

    /**
     * Returns relative height for given factor.
     */
    fun ofH(y: Float) = hf * y

    fun normH(value: Float) = value / hf
    fun normW(value: Float) = value / wf

    /**
     * Returns the smaller of width and height.
     */
    fun min() = if (w < h) w else h

    companion object {
        val DESKTOP_FULL_HD = Dimension(1920, 1080)
        val DESKTOP_FULL__LANDSCAPE_HD = Dimension(1080, 1920)
        val LAPTOP_FULL_HD = Dimension(1366, 768)
        fun of(w: Float, h: Float) = Dimension(w.toInt(), h.toInt())
    }
}
