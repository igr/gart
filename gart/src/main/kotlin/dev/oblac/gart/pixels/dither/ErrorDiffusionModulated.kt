package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization
import kotlin.random.Random

/**
 * Classic error diffusion with random threshold modulation.
 * The quantization threshold is slightly randomized per pixel,
 * and error values are clamped after distribution to prevent overflow.
 */
fun ditherErrorDiffusionModulated(bitmap: Pixels, kernel: Array<DitherKernelEntry>, pixelSize: Int = 1, colorCount: Int = 256) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    for (y in 0 until height step pixelSize) {
        for (x in 0 until width step pixelSize) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            // Modulated quantization: add small random offset before quantizing
            val mod = (stepSize * 0.15 * Random.nextDouble(-1.0, 1.0)).toInt()

            val newR = (oldColor.r + mod).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + mod).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + mod).roundToNearestQuantization(stepSize)

            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)

            for (entry in kernel) {
                val nx = x + entry.dx * pixelSize
                val ny = y + entry.dy * pixelSize
                if (nx in 0 until width && ny in 0 until height) {
                    bitmap.addBlockColor(
                        nx, ny, pixelSize,
                        (errorR * entry.weight).toInt(),
                        (errorG * entry.weight).toInt(),
                        (errorB * entry.weight).toInt()
                    )
                }
            }
        }
    }
}
