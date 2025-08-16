package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization

fun ditherFloydSteinberg(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
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
                bitmap.addBlockColor(rightX, y, pixelSize, (errorR * 7.0 / 16.0).toInt(), (errorG * 7.0 / 16.0).toInt(), (errorB * 7.0 / 16.0).toInt())
            
            if (bottomY < height) {
                if (bottomLeftX >= 0) 
                    bitmap.addBlockColor(bottomLeftX, bottomY, pixelSize, (errorR * 3.0 / 16.0).toInt(), (errorG * 3.0 / 16.0).toInt(), (errorB * 3.0 / 16.0).toInt())
            
                bitmap.addBlockColor(x, bottomY, pixelSize, (errorR * 5.0 / 16.0).toInt(), (errorG * 5.0 / 16.0).toInt(), (errorB * 5.0 / 16.0).toInt())
            
                if (bottomRightX < width) 
                    bitmap.addBlockColor(bottomRightX, bottomY, pixelSize, (errorR * 1.0 / 16.0).toInt(), (errorG * 1.0 / 16.0).toInt(), (errorB * 1.0 / 16.0).toInt())
            }
        }
    }
}

