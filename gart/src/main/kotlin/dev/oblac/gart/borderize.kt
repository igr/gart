package dev.oblac.gart

/**
 * Clones the gart to new Gart with a uniform border.
 */
fun borderize(src: GartGG, border: Int, color: Int): GartGG {
    val sourceImage = src.g.snapshot()

    val dest = Gart.of(src.gart.name, Dimension(src.g.d.w + border * 2, src.g.d.h + border * 2)).gg()
    dest.g.canvas.clear(color)
    dest.g.canvas.drawImage(sourceImage, border.toFloat(), border.toFloat())
    return dest
}