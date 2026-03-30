package dev.oblac.gart.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.pixels.roundToNearestQuantization

/**
 * A simple and efficient error-diffusion algorithm by Ostromoukhov (SIGGRAPH 2001).
 * Uses intensity-dependent variable coefficients for error distribution,
 * producing higher quality results than fixed-coefficient methods.
 * Uses serpentine (alternating direction) scanning.
 *
 * https://doi.org/10.1145/383259.383326
 */
fun ditherOstromoukhov(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
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

            // Coefficient lookup based on luminance
            val luminance = ((0.299 * oldColor.r + 0.587 * oldColor.g + 0.114 * oldColor.b).toInt()).coerceIn(0, 255)
            val coeff = ostromoukhovCoefficients(luminance)
            val cRight = coeff[0]
            val cBottomLeft = coeff[1]
            val cBottom = coeff[2]
            val cSum = coeff[3].toDouble()

            val dxSign = if (leftToRight) 1 else -1

            // Right (flips to left when going R-to-L)
            val rightX = x + dxSign * pixelSize
            if (rightX in 0 until width) {
                bitmap.addBlockColor(
                    rightX, y, pixelSize,
                    (errorR * cRight / cSum).toInt(),
                    (errorG * cRight / cSum).toInt(),
                    (errorB * cRight / cSum).toInt()
                )
            }

            val bottomY = y + pixelSize
            if (bottomY < height) {
                // Bottom-left (flips to bottom-right when going R-to-L)
                val bottomLeftX = x - dxSign * pixelSize
                if (bottomLeftX in 0 until width) {
                    bitmap.addBlockColor(
                        bottomLeftX, bottomY, pixelSize,
                        (errorR * cBottomLeft / cSum).toInt(),
                        (errorG * cBottomLeft / cSum).toInt(),
                        (errorB * cBottomLeft / cSum).toInt()
                    )
                }

                // Bottom (always directly below)
                bitmap.addBlockColor(
                    x, bottomY, pixelSize,
                    (errorR * cBottom / cSum).toInt(),
                    (errorG * cBottom / cSum).toInt(),
                    (errorB * cBottom / cSum).toInt()
                )
            }
        }

        leftToRight = !leftToRight
    }
}

/**
 * Returns the Ostromoukhov coefficients [right, bottomLeft, bottom, sum]
 * for the given intensity (0-255). The table is symmetric around 127.5.
 */
internal fun ostromoukhovCoefficients(intensity: Int): IntArray {
    val i = intensity.coerceIn(0, 255)
    return if (i < 128) OSTROMOUKHOV_TABLE[i] else OSTROMOUKHOV_TABLE[255 - i]
}

// 128 entries for intensity levels 0-127; mirrored for 128-255.
// Each entry: [right, bottomLeft, bottom, sum]
@Suppress("SpellCheckingInspection")
internal val OSTROMOUKHOV_TABLE = arrayOf(
    intArrayOf(13, 0, 5, 18),
    intArrayOf(13, 0, 5, 18),
    intArrayOf(21, 0, 10, 31),
    intArrayOf(7, 0, 4, 11),
    intArrayOf(8, 0, 5, 13),
    intArrayOf(47, 3, 28, 78),
    intArrayOf(23, 3, 13, 39),
    intArrayOf(15, 3, 8, 26),
    intArrayOf(22, 6, 11, 39),
    intArrayOf(43, 15, 20, 78),
    intArrayOf(7, 3, 3, 13),
    intArrayOf(501, 224, 211, 936),
    intArrayOf(249, 116, 103, 468),
    intArrayOf(165, 80, 67, 312),
    intArrayOf(123, 62, 49, 234),
    intArrayOf(489, 256, 191, 936),
    intArrayOf(81, 44, 31, 156),
    intArrayOf(483, 272, 181, 936),
    intArrayOf(60, 35, 22, 117),
    intArrayOf(53, 32, 19, 104),
    intArrayOf(237, 148, 83, 468),
    intArrayOf(471, 304, 161, 936),
    intArrayOf(3, 2, 1, 6),
    intArrayOf(459, 304, 161, 924),
    intArrayOf(38, 25, 14, 77),
    intArrayOf(453, 296, 175, 924),
    intArrayOf(225, 146, 91, 462),
    intArrayOf(149, 96, 63, 308),
    intArrayOf(111, 71, 49, 231),
    intArrayOf(63, 40, 29, 132),
    intArrayOf(73, 46, 35, 154),
    intArrayOf(435, 272, 217, 924),
    intArrayOf(108, 67, 56, 231),
    intArrayOf(13, 8, 7, 28),
    intArrayOf(213, 130, 119, 462),
    intArrayOf(423, 256, 245, 924),
    intArrayOf(5, 3, 3, 11),
    intArrayOf(281, 173, 162, 616),
    intArrayOf(141, 89, 78, 308),
    intArrayOf(283, 183, 150, 616),
    intArrayOf(71, 47, 36, 154),
    intArrayOf(285, 193, 138, 616),
    intArrayOf(13, 9, 6, 28),
    intArrayOf(41, 29, 18, 88),
    intArrayOf(36, 26, 15, 77),
    intArrayOf(289, 213, 114, 616),
    intArrayOf(145, 109, 54, 308),
    intArrayOf(291, 223, 102, 616),
    intArrayOf(73, 57, 24, 154),
    intArrayOf(293, 233, 90, 616),
    intArrayOf(21, 17, 6, 44),
    intArrayOf(295, 243, 78, 616),
    intArrayOf(37, 31, 9, 77),
    intArrayOf(27, 23, 6, 56),
    intArrayOf(149, 129, 30, 308),
    intArrayOf(299, 263, 54, 616),
    intArrayOf(75, 67, 12, 154),
    intArrayOf(43, 39, 6, 88),
    intArrayOf(151, 139, 18, 308),
    intArrayOf(303, 283, 30, 616),
    intArrayOf(38, 36, 3, 77),
    intArrayOf(305, 293, 18, 616),
    intArrayOf(153, 149, 6, 308),
    intArrayOf(307, 303, 6, 616),
    intArrayOf(1, 1, 0, 2),
    intArrayOf(101, 105, 2, 208),
    intArrayOf(49, 53, 2, 104),
    intArrayOf(95, 107, 6, 208),
    intArrayOf(23, 27, 2, 52),
    intArrayOf(89, 109, 10, 208),
    intArrayOf(43, 55, 6, 104),
    intArrayOf(83, 111, 14, 208),
    intArrayOf(5, 7, 1, 13),
    intArrayOf(172, 181, 37, 390),
    intArrayOf(97, 76, 22, 195),
    intArrayOf(72, 41, 17, 130),
    intArrayOf(119, 47, 29, 195),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(4, 1, 1, 6),
    intArrayOf(65, 18, 17, 100),
    intArrayOf(95, 29, 26, 150),
    intArrayOf(185, 62, 53, 300),
    intArrayOf(30, 11, 9, 50),
    intArrayOf(35, 14, 11, 60),
    intArrayOf(85, 37, 28, 150),
    intArrayOf(55, 26, 19, 100),
    intArrayOf(80, 41, 29, 150),
    intArrayOf(155, 86, 59, 300),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(5, 3, 2, 10),
    intArrayOf(305, 176, 119, 600),
    intArrayOf(155, 86, 59, 300),
    intArrayOf(105, 56, 39, 200),
    intArrayOf(80, 41, 29, 150),
    intArrayOf(65, 32, 23, 120),
    intArrayOf(55, 26, 19, 100),
    intArrayOf(335, 152, 113, 600),
    intArrayOf(85, 37, 28, 150),
    intArrayOf(115, 48, 37, 200),
    intArrayOf(35, 14, 11, 60),
    intArrayOf(355, 136, 109, 600),
    intArrayOf(30, 11, 9, 50),
    intArrayOf(365, 128, 107, 600),
    intArrayOf(185, 62, 53, 300),
    intArrayOf(25, 8, 7, 40),
    intArrayOf(95, 29, 26, 150),
    intArrayOf(385, 112, 103, 600),
    intArrayOf(65, 18, 17, 100),
    intArrayOf(395, 104, 101, 600),
    intArrayOf(4, 1, 1, 6),
)
