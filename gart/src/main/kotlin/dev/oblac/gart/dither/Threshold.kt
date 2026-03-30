package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization
import kotlin.random.Random

/**
 * Simple intensity thresholding, with optional noise applied to the threshold.
 * Without noise, this is equivalent to plain quantization.
 * With noise > 0, the quantization threshold is randomly perturbed per pixel.
 */
fun ditherThreshold(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256, noise: Double = 0.0) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    for (y in 0 until height step pixelSize) {
        for (x in 0 until width step pixelSize) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val offset = (noise * stepSize * Random.nextDouble(-1.0, 1.0)).toInt()

            val newR = (oldColor.r + offset).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + offset).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + offset).roundToNearestQuantization(stepSize)

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)
        }
    }
}
