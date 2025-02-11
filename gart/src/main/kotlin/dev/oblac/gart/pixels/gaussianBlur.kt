package dev.oblac.gart.pixels

import dev.oblac.gart.Gartmap
import dev.oblac.gart.MemPixels
import dev.oblac.gart.color.*

/**
 * Anti-aliased bitmap using a 3x3 Gaussian blur filter.
 */
fun applyGaussianBlur(b: Gartmap) {
    val width = b.d.w
    val height = b.d.h
    val newBitmap = MemPixels(b.d)

    // Gaussian kernel (3x3)
    val kernel = arrayOf(
        arrayOf(1, 2, 1),
        arrayOf(2, 4, 2),
        arrayOf(1, 2, 1)
    )
    val kernelSum = 16 // Sum of all kernel values

    for (x in 0 until width) {
        for (y in 0 until height) {
            var sumR = 0
            var sumG = 0
            var sumB = 0
            var sumA = 0

            // Apply kernel to surrounding pixels
            for (dx in -1..1) {
                for (dy in -1..1) {
                    // Handle edges by clamping coordinates
                    val sampleX = (x + dx).coerceIn(0, width - 1)
                    val sampleY = (y + dy).coerceIn(0, height - 1)

                    val pixel = b[sampleX, sampleY]
                    val weight = kernel[dx + 1][dy + 1]

                    sumR += red(pixel) * weight
                    sumG += green(pixel) * weight
                    sumB += blue(pixel) * weight
                    sumA += alpha(pixel) * weight
                }
            }

            // Normalize and clamp values
            val newR = (sumR / kernelSum).coerceIn(0, 255)
            val newG = (sumG / kernelSum).coerceIn(0, 255)
            val newB = (sumB / kernelSum).coerceIn(0, 255)
            val newA = (sumA / kernelSum).coerceIn(0, 255)

            newBitmap[x, y] = RGBA(newR, newG, newB, newA).value
        }
    }

    b.copyPixelsFrom(newBitmap)
}
