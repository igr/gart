package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization

fun ditherOrdered8By8Bayer(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)
    
    for (y in 0 until height step pixelSize) {
        for (x in 0 until width step pixelSize) {
            // For blocks, use average color of the block
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            
            val oldColor = RGBA.of(blockPixel)
            
            val indexX = (x / pixelSize) % 8
            val indexY = (y / pixelSize) % 8
            val threshold = (order8By8Bayer[indexY][indexX] * stepSize / 64)
            
            // Quantize each channel with threshold
            val newR = (oldColor.r + threshold).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + threshold).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + threshold).roundToNearestQuantization(stepSize)
            
            // Set new quantized color
            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)
        }
    }
}

private val order8By8Bayer = arrayOf(
    intArrayOf(0, 32, 8, 40, 2, 34, 10, 42),
    intArrayOf(48, 16, 56, 24, 50, 18, 58, 26),
    intArrayOf(12, 44, 4, 36, 14, 46, 6, 38),
    intArrayOf(60, 28, 52, 20, 62, 30, 54, 22),
    intArrayOf(3, 35, 11, 43, 1, 33, 9, 41),
    intArrayOf(51, 19, 59, 27, 49, 17, 57, 25),
    intArrayOf(15, 47, 7, 39, 13, 45, 5, 37),
    intArrayOf(63, 31, 55, 23, 61, 29, 53, 21)
)
