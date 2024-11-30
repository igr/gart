package dev.oblac.gart

/**
 * GartGG is a simple aggregation.
 */
data class GartGG(
    val gart: Gart,
    val g: Gartvas
) {
    fun saveImage() {
        gart.saveImage(g)
    }

    fun showImage() {
        gart.window().showImage(g)
    }
}
