package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization
import kotlin.random.Random

/**
 * Random intensity thresholding (white noise dithering).
 * Each pixel's quantization threshold is fully randomized.
 */
fun ditherWhiteNoise(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    for (y in 0 until height step pixelSize) {
        for (x in 0 until width step pixelSize) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val offsetR = Random.nextInt(-stepSize / 2, stepSize / 2 + 1)
            val offsetG = Random.nextInt(-stepSize / 2, stepSize / 2 + 1)
            val offsetB = Random.nextInt(-stepSize / 2, stepSize / 2 + 1)

            val newR = (oldColor.r + offsetR).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + offsetG).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + offsetB).roundToNearestQuantization(stepSize)

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)
        }
    }
}
