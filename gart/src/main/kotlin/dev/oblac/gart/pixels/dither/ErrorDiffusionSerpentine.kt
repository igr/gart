package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization

/**
 * Error diffusion with serpentine (alternating direction) scanning.
 * Reduces directional artifacts by alternating scan direction each row.
 * Can be used with any kernel from [DitherKernels].
 */
fun ditherErrorDiffusionSerpentine(bitmap: Pixels, kernel: Array<DitherKernelEntry>, pixelSize: Int = 1, colorCount: Int = 256) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    var leftToRight = true

    for (y in 0 until height step pixelSize) {
        val xRange = (0 until width step pixelSize).let {
            if (leftToRight) it else it.reversed()
        }

        for (x in xRange) {
            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val newR = oldColor.r.roundToNearestQuantization(stepSize)
            val newG = oldColor.g.roundToNearestQuantization(stepSize)
            val newB = oldColor.b.roundToNearestQuantization(stepSize)

            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            val newColor = RGBA.of(newR, newG, newB, oldColor.a)
            bitmap.setBlock(x, y, pixelSize, newColor.value)

            val dxSign = if (leftToRight) 1 else -1

            for (entry in kernel) {
                val nx = x + entry.dx * dxSign * pixelSize
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

        leftToRight = !leftToRight
    }
}
