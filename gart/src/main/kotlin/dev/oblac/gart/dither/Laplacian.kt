package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.math.globalStdDev
import dev.oblac.gart.math.windowedStdDev
import dev.oblac.gart.pixels.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs

/**
 * Laplacian based structure-aware error diffusion (Kong et al., 2010).
 * Uses the Laplacian and local standard deviation to modulate the
 * quantization threshold, preserving edges and structure.
 *
 * https://doi.org/10.1109/ICIP.2010.5651243
 *
 * @param scaleFactor 0.85 yields similar SSIM as paper, 0.5 is more discreet
 */
fun ditherLaplacian(
    bitmap: Pixels,
    pixelSize: Int = 1,
    colorCount: Int = 256,
    kernel: Array<DitherKernelEntry> = DitherKernels.FLOYD_STEINBERG,
    scaleFactor: Double = 0.5,
) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    // Build luminance map
    val luminance = DoubleArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val p = RGBA.of(bitmap[x, y])
            luminance[y * width + x] = (0.299 * p.r + 0.587 * p.g + 0.114 * p.b) / 255.0
        }
    }

    // Compute Laplacian, clipped to [-0.5, 0.5]
    val lm = laplacianFilter(luminance, width, height)
    for (i in lm.indices) lm[i] = lm[i].coerceIn(-0.5, 0.5)

    // Global and local standard deviations
    val globalStd = globalStdDev(luminance)
    val stddev = windowedStdDev(luminance, width, height, 5, PadMode.REFLECT)
    val localMax = maximumFilter(stddev, width, height, 10)
    val localMin = minimumFilter(stddev, width, height, 10)

    var leftToRight = true

    for (y in 0 until height step pixelSize) {
        val xRange = (0 until width step pixelSize).let {
            if (leftToRight) it else it.reversed()
        }

        for (x in xRange) {
            val idx = y * width + x
            val localStd = stddev[idx]
            val lMax = localMax[idx]
            val lMin = localMin[idx]

            var k = 0.0
            if (lMax - lMin != 0.0) {
                k = scaleFactor / globalStd * (abs(localStd - lMax) / (lMax - lMin)) + scaleFactor
            }

            val n = ThreadLocalRandom.current().nextGaussian() * 0.1
            val t = k * lm[idx] + n
            val modOffset = (t * stepSize).toInt()

            val blockPixel = bitmap.calcAverageBlockColor(x, y, pixelSize)
            val oldColor = RGBA.of(blockPixel)

            val newR = (oldColor.r + modOffset).roundToNearestQuantization(stepSize)
            val newG = (oldColor.g + modOffset).roundToNearestQuantization(stepSize)
            val newB = (oldColor.b + modOffset).roundToNearestQuantization(stepSize)

            val errorR = oldColor.r - newR
            val errorG = oldColor.g - newG
            val errorB = oldColor.b - newB

            bitmap.setBlock(x, y, pixelSize, RGBA.of(newR, newG, newB, oldColor.a).value)

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
