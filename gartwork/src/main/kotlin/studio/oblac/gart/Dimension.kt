package studio.oblac.gart

/**
 * Represents a virtual dimensions.
 */
data class Dimension(val w: Int, val h: Int) {
    val wf = w.toFloat()
    val hf = h.toFloat()
    val cx = w / 2f
    val cy = h / 2f

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
}
