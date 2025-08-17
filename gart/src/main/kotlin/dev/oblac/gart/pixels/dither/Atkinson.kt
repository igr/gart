package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization

/**
 * During the mid-1980’s, dithering became increasingly popular as
 * computer hardware advanced to support more powerful video drivers and displays.
 * One of the best dithering algorithms from this era was developed by Bill Atkinson,
 * a Apple employee who worked on everything from MacPaint
 * (which he wrote from scratch for the original Macintosh) to HyperCard and QuickDraw.
 */
fun ditherAtkinson(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
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

            // Quantize each channel
            val newR = oldColor.r.roundToNearestQuantization(stepSize)
            val newG = oldColor.g.roundToNearestQuantization(stepSize)
            val newB = oldColor.b.roundToNearestQuantization(stepSize)

            // Calculate errors
            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            // Set new quantized color
            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)

            // Distribute error using Atkinson weights (1/8 each to 6 neighbors)
            val rightX = x + pixelSize
            val rightX2 = x + 2 * pixelSize
            val bottomY = y + pixelSize
            val bottomY2 = y + 2 * pixelSize
            val bottomLeftX = x - pixelSize
            val bottomRightX = x + pixelSize

            if (rightX < width)
                bitmap.addBlockColor(rightX, y, pixelSize, (errorR / 8.0).toInt(), (errorG / 8.0).toInt(), (errorB / 8.0).toInt())

            if (rightX2 < width)
                bitmap.addBlockColor(rightX2, y, pixelSize, (errorR / 8.0).toInt(), (errorG / 8.0).toInt(), (errorB / 8.0).toInt())

            if (bottomY < height) {
                if (bottomLeftX >= 0)
                    bitmap.addBlockColor(bottomLeftX, bottomY, pixelSize, (errorR / 8.0).toInt(), (errorG / 8.0).toInt(), (errorB / 8.0).toInt())

                bitmap.addBlockColor(x, bottomY, pixelSize, (errorR / 8.0).toInt(), (errorG / 8.0).toInt(), (errorB / 8.0).toInt())

                if (bottomRightX < width)
                    bitmap.addBlockColor(bottomRightX, bottomY, pixelSize, (errorR / 8.0).toInt(), (errorG / 8.0).toInt(), (errorB / 8.0).toInt())
            }

            if (bottomY2 < height) {
                bitmap.addBlockColor(x, bottomY2, pixelSize, (errorR / 8.0).toInt(), (errorG / 8.0).toInt(), (errorB / 8.0).toInt())
            }
        }
    }
}
