package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization

fun ditherFloydSteinberg(
    bitmap: Pixels,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    wRight: Double = 7.0 / 16.0,
    wBottomLeft: Double = 3.0 / 16.0,
    wBottom: Double = 5.0 / 16.0,
    wBottomRight: Double = 1.0 / 16.0,
) {
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
            
            // Distribute error using Floyd-Steinberg weights
            val rightX = x + pixelSize
            val bottomY = y + pixelSize
            val bottomLeftX = x - pixelSize
            val bottomRightX = x + pixelSize
            
            if (rightX < width)
                bitmap.addBlockColor(rightX, y, pixelSize, (errorR * wRight).toInt(), (errorG * wRight).toInt(), (errorB * wRight).toInt())

            if (bottomY < height) {
                if (bottomLeftX >= 0)
                    bitmap.addBlockColor(bottomLeftX, bottomY, pixelSize, (errorR * wBottomLeft).toInt(), (errorG * wBottomLeft).toInt(), (errorB * wBottomLeft).toInt())

                bitmap.addBlockColor(x, bottomY, pixelSize, (errorR * wBottom).toInt(), (errorG * wBottom).toInt(), (errorB * wBottom).toInt())

                if (bottomRightX < width)
                    bitmap.addBlockColor(bottomRightX, bottomY, pixelSize, (errorR * wBottomRight).toInt(), (errorG * wBottomRight).toInt(), (errorB * wBottomRight).toInt())
            }
        }
    }
}

