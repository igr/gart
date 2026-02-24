package dev.oblac.gart.color.space

/**
 * Interpolates between two hue angles (in degrees) using the shortest path.
 */
internal fun mixHue(hue0: Float, hue1: Float, f: Float): Float {
    val dh = when {
        hue1 > hue0 && hue1 - hue0 > 180 -> hue1 - (hue0 + 360)
        hue1 < hue0 && hue0 - hue1 > 180 -> hue1 + 360 - hue0
        else -> hue1 - hue0
    }
    return (hue0 + f * dh + 360f) % 360f
}
