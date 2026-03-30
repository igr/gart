package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization

data class DitherKernelEntry(val dx: Int, val dy: Int, val weight: Double)

fun ditherErrorDiffusion(bitmap: Pixels, kernel: Array<DitherKernelEntry>, pixelSize: Int = 1, colorCount: Int = 256) {
    require(pixelSize >= 1) { "Pixel size must be 1 or greater" }
    require(colorCount >= 2) { "Color count must be 2 or greater" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val stepSize = 255 / (colorCount - 1)

    for (y in 0 until height step pixelSize) {
        for (x in 0 until width step pixelSize) {
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

object DitherKernels {

    val ATKINSON = arrayOf(
        DitherKernelEntry(1, 0, 1.0 / 8),
        DitherKernelEntry(2, 0, 1.0 / 8),
        DitherKernelEntry(-1, 1, 1.0 / 8),
        DitherKernelEntry(0, 1, 1.0 / 8),
        DitherKernelEntry(1, 1, 1.0 / 8),
        DitherKernelEntry(0, 2, 1.0 / 8),
    )

    val FLOYD_STEINBERG = arrayOf(
        DitherKernelEntry(1, 0, 7.0 / 16),
        DitherKernelEntry(-1, 1, 3.0 / 16),
        DitherKernelEntry(0, 1, 5.0 / 16),
        DitherKernelEntry(1, 1, 1.0 / 16),
    )

    val JARVIS_JUDICE_NINKE = arrayOf(
        DitherKernelEntry(1, 0, 7.0 / 48),
        DitherKernelEntry(2, 0, 5.0 / 48),
        DitherKernelEntry(-2, 1, 3.0 / 48),
        DitherKernelEntry(-1, 1, 5.0 / 48),
        DitherKernelEntry(0, 1, 7.0 / 48),
        DitherKernelEntry(1, 1, 5.0 / 48),
        DitherKernelEntry(2, 1, 3.0 / 48),
        DitherKernelEntry(-2, 2, 1.0 / 48),
        DitherKernelEntry(-1, 2, 3.0 / 48),
        DitherKernelEntry(0, 2, 5.0 / 48),
        DitherKernelEntry(1, 2, 3.0 / 48),
        DitherKernelEntry(2, 2, 1.0 / 48),
    )

    val STUCKI = arrayOf(
        DitherKernelEntry(1, 0, 8.0 / 42),
        DitherKernelEntry(2, 0, 4.0 / 42),
        DitherKernelEntry(-2, 1, 2.0 / 42),
        DitherKernelEntry(-1, 1, 4.0 / 42),
        DitherKernelEntry(0, 1, 8.0 / 42),
        DitherKernelEntry(1, 1, 4.0 / 42),
        DitherKernelEntry(2, 1, 2.0 / 42),
        DitherKernelEntry(-2, 2, 1.0 / 42),
        DitherKernelEntry(-1, 2, 2.0 / 42),
        DitherKernelEntry(0, 2, 4.0 / 42),
        DitherKernelEntry(1, 2, 2.0 / 42),
        DitherKernelEntry(2, 2, 1.0 / 42),
    )

    val BURKES = arrayOf(
        DitherKernelEntry(1, 0, 8.0 / 32),
        DitherKernelEntry(2, 0, 4.0 / 32),
        DitherKernelEntry(-2, 1, 2.0 / 32),
        DitherKernelEntry(-1, 1, 4.0 / 32),
        DitherKernelEntry(0, 1, 8.0 / 32),
        DitherKernelEntry(1, 1, 4.0 / 32),
        DitherKernelEntry(2, 1, 2.0 / 32),
    )

    val SIERRA3 = arrayOf(
        DitherKernelEntry(1, 0, 5.0 / 32),
        DitherKernelEntry(2, 0, 3.0 / 32),
        DitherKernelEntry(-2, 1, 2.0 / 32),
        DitherKernelEntry(-1, 1, 4.0 / 32),
        DitherKernelEntry(0, 1, 5.0 / 32),
        DitherKernelEntry(1, 1, 4.0 / 32),
        DitherKernelEntry(2, 1, 2.0 / 32),
        DitherKernelEntry(-1, 2, 2.0 / 32),
        DitherKernelEntry(0, 2, 3.0 / 32),
        DitherKernelEntry(1, 2, 2.0 / 32),
    )

    val SIERRA2 = arrayOf(
        DitherKernelEntry(1, 0, 4.0 / 16),
        DitherKernelEntry(2, 0, 3.0 / 16),
        DitherKernelEntry(-2, 1, 1.0 / 16),
        DitherKernelEntry(-1, 1, 2.0 / 16),
        DitherKernelEntry(0, 1, 3.0 / 16),
        DitherKernelEntry(1, 1, 2.0 / 16),
        DitherKernelEntry(2, 1, 1.0 / 16),
    )

    val SIERRA_LITE = arrayOf(
        DitherKernelEntry(1, 0, 2.0 / 4),
        DitherKernelEntry(-1, 1, 1.0 / 4),
        DitherKernelEntry(0, 1, 1.0 / 4),
    )

    val SHIAU_FAN1 = arrayOf(
        DitherKernelEntry(1, 0, 1.0 / 2),
        DitherKernelEntry(-2, 1, 1.0 / 8),
        DitherKernelEntry(-1, 1, 1.0 / 8),
        DitherKernelEntry(0, 1, 1.0 / 4),
    )

    val SHIAU_FAN2 = arrayOf(
        DitherKernelEntry(1, 0, 1.0 / 2),
        DitherKernelEntry(-3, 1, 1.0 / 16),
        DitherKernelEntry(-2, 1, 1.0 / 16),
        DitherKernelEntry(-1, 1, 1.0 / 8),
        DitherKernelEntry(0, 1, 1.0 / 4),
    )

    val WONG_ALLEBACH = arrayOf(
        DitherKernelEntry(1, 0, 0.2911),
        DitherKernelEntry(-1, 1, 0.1373),
        DitherKernelEntry(0, 1, 0.3457),
        DitherKernelEntry(1, 1, 0.2258),
    )

    val FEDOSEEV = arrayOf(
        DitherKernelEntry(1, 0, 0.5423),
        DitherKernelEntry(2, 0, 0.0533),
        DitherKernelEntry(-2, 1, 0.0246),
        DitherKernelEntry(-1, 1, 0.2191),
        DitherKernelEntry(0, 1, 0.4715),
        DitherKernelEntry(1, 1, -0.0023),
        DitherKernelEntry(2, 1, -0.1241),
        DitherKernelEntry(-2, 2, -0.0065),
        DitherKernelEntry(-1, 2, -0.0692),
        DitherKernelEntry(0, 2, 0.0168),
        DitherKernelEntry(1, 2, -0.0952),
        DitherKernelEntry(2, 2, -0.0304),
    )

    val FEDOSEEV2 = arrayOf(
        DitherKernelEntry(1, 0, 0.4364),
        DitherKernelEntry(0, 1, 0.5636),
    )

    val FEDOSEEV3 = arrayOf(
        DitherKernelEntry(1, 0, 0.4473),
        DitherKernelEntry(-1, 1, 0.1654),
        DitherKernelEntry(0, 1, 0.3872),
    )

    val FEDOSEEV4 = arrayOf(
        DitherKernelEntry(1, 0, 0.5221),
        DitherKernelEntry(-1, 1, 0.1854),
        DitherKernelEntry(0, 1, 0.4689),
        DitherKernelEntry(1, 2, -0.1763),
    )

}
