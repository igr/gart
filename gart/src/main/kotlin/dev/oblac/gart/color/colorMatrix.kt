package dev.oblac.gart.color

import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import kotlin.math.cos
import kotlin.math.sin

object ColorMatrices {

    fun saturation(sat: Float): ColorMatrix {
        val r = 0.213f
        val g = 0.715f
        val b = 0.072f

        val arr = floatArrayOf(
            r + (1 - r) * sat, g - g * sat, b - b * sat, 0f, 0f,
            r - r * sat, g + (1 - g) * sat, b - b * sat, 0f, 0f,
            r - r * sat, g - g * sat, b + (1 - b) * sat, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(*arr)
    }

    fun brightness(brightness: Float): ColorMatrix {
        val arr = floatArrayOf(
            1f, 0f, 0f, 0f, brightness * 255,
            0f, 1f, 0f, 0f, brightness * 255,
            0f, 0f, 1f, 0f, brightness * 255,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(*arr)
    }

    fun contrast(contrast: Float): ColorMatrix {
        // contrast = 1 → unchanged
        // contrast > 1 → more contrast
        // contrast < 1 → less contrast
        val scale = contrast
        val translate = 128f * (1f - scale)

        val arr = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(*arr)
    }

    fun grayscale(): ColorMatrix {
        val r = 0.213f
        val g = 0.715f
        val b = 0.072f

        val arr = floatArrayOf(
            r, g, b, 0f, 0f,
            r, g, b, 0f, 0f,
            r, g, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(*arr)
    }

    fun invert(): ColorMatrix {
        val arr = floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(*arr)
    }

    fun sepia(): ColorMatrix {
        val arr = floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(*arr)
    }

    fun hueRotate(degrees: Float): ColorMatrix {
        val rad = Math.toRadians(degrees.toDouble())
        val cosVal = cos(rad).toFloat()
        val sinVal = sin(rad).toFloat()

        val r = 0.213f
        val g = 0.715f
        val b = 0.072f

        val arr = floatArrayOf(
            r + cosVal * (1 - r) + sinVal * (-r), g + cosVal * (-g) + sinVal * (-g), b + cosVal * (-b) + sinVal * (1 - b), 0f, 0f,
            r + cosVal * (-r) + sinVal * (0.143f), g + cosVal * (1 - g) + sinVal * (0.140f), b + cosVal * (-b) + sinVal * (-0.283f), 0f, 0f,
            r + cosVal * (-r) + sinVal * (-(1 - r)), g + cosVal * (-g) + sinVal * (g), b + cosVal * (1 - b) + sinVal * (b), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(*arr)
    }
}

fun ColorMatrix.concat(other: ColorMatrix): ColorMatrix {
    val result = FloatArray(20)

    for (row in 0 until 4) {
        for (col in 0 until 5) {
            var sum = 0f
            for (k in 0 until 4) {
                sum += mat[row * 5 + k] * other.mat[k * 5 + col]
            }
            if (col == 4) {
                // Add translation term
                sum += mat[row * 5 + 4]
            }
            result[row * 5 + col] = sum
        }
    }

    return ColorMatrix(*result)
}


fun ColorMatrix.toColorFilter(): ColorFilter = ColorFilter.makeMatrix(this)
