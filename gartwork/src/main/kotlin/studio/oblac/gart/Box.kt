package studio.oblac.gart

/**
 * Represents a virtual dimensions.
 */
data class Box(val w: Int, val h: Int) {
    val wf = w.toFloat()
    val hf = h.toFloat()
    val cx = w / 2f
    val cy = h / 2f
    /**
     * Right edge.
     */
    val r = w - 1
    val rf = r.toFloat()
    /**
     * Bottom edge.
     */
    val b = h - 1

    val bf = b.toFloat()

    /**
     * Box area.
     */
    val area = w * h

}
