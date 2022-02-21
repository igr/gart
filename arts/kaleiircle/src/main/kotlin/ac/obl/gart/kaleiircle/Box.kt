package ac.obl.gart.kaleiircle

/**
 * Represents a virtual dimensions.
 */
data class Box(val w: Int, val h: Int) {
    val wf = w.toFloat()
    val hf = w.toFloat()
    val cx = w / 2f
    val cy = h / 2f
}
