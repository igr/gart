package dev.oblac.gart.fx

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Pixels
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.math.PIf
import kotlin.math.pow
import kotlin.math.sin

/**
 * Darkens the rows of [target] in the scanline pattern of an old CRT, in place.
 *
 * Row `y` gets the scale `1 - depth * sin²(π y / pitch)`. Row 0 keeps its color, and the pattern repeats
 * every [pitch] px. The darkest rows are halfway between two bright rows. The scale changes red, green
 * and blue with [colorScale]. Alpha stays the same.
 *
 * @param pitch the distance between two scanlines, in px
 * @param depth the darkness of the darkest rows, `0f`..`1f`. `0f` changes nothing, `1f` makes them black
 */
fun addScanlines(target: Pixels, pitch: Float = 4f, depth: Float = 0.3f) {
    val w = target.d.w
    val px = target.pixels
    for (y in 0 until target.d.h) {
        val line = 1f - depth * sin(PIf * y / pitch).pow(2)
        for (i in y * w until (y + 1) * w) {
            px[i] = colorScale(px[i], line).alpha(alpha(px[i]))
        }
    }
}

/**
 * Darkens the rows of [gartvas] in the scanline pattern of an old CRT, in place.
 * It reads the pixels, does the same as the [Pixels] version, and writes the pixels back.
 */
fun addScanlines(gartvas: Gartvas, pitch: Float = 4f, depth: Float = 0.3f) {
    Gartmap(gartvas).use {
        addScanlines(it, pitch, depth)
        it.drawToCanvas()
    }
}
