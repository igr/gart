package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization

fun ditherOrdered2By2Bayer(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
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
            
            val indexX = (x / pixelSize) % 2
            val indexY = (y / pixelSize) % 2
            val threshold = (order2By2Bayer[indexY][indexX] * stepSize / 4)
            
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

private val order2By2Bayer = arrayOf(
    intArrayOf(0, 2),
    intArrayOf(3, 1)
)
