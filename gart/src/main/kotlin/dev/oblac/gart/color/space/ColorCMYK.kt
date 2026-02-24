package dev.oblac.gart.color.space

import org.jetbrains.skia.Color4f

/**
 * CMYK color representation (Cyan, Magenta, Yellow, Key/Black).
 * Values are normalized to 0.0-1.0 range.
 */
data class ColorCMYK(val c: Float, val m: Float, val y: Float, val k: Float) {

    /**
     * Converts CMYK to RGB color space.
     */
    fun toColor4f(): Color4f {
        // Standard CMYK to RGB conversion
        val r = (1f - c) * (1f - k)
        val g = (1f - m) * (1f - k)
        val b = (1f - y) * (1f - k)

        return Color4f(r, g, b, 1f)
    }

    /**
     * Converts CMYK to RGBA with integer values (0-255).
     */
    private fun toRGBA() = toColor4f().let { RGBA.of(it) }

    /**
     * Converts to integer color value.
     */
    private val value: Int
        get() = toRGBA().value
    
    fun mix(other: ColorCMYK, f: Float = 0.5f): ColorCMYK {
        return ColorCMYK(
            c = c + f * (other.c - c),
            m = m + f * (other.m - m),
            y = y + f * (other.y - y),
            k = k + f * (other.k - k)
        )
    }

    /**
     * Blends this CMYK color with another using additive ink model.
     */
    fun blend(other: ColorCMYK): ColorCMYK {
        return ColorCMYK(
            c = (c + other.c).coerceAtMost(1f),
            m = (m + other.m).coerceAtMost(1f),
            y = (y + other.y).coerceAtMost(1f),
            k = (k + other.k).coerceAtMost(1f)
        )
    }

    fun blendK(value: Float) = ColorCMYK(
        c = c,
        m = m,
        y = y,
        k = (k + value).coerceAtMost(1f)
    )

    fun blendC(value: Float) = ColorCMYK(
        c = (c + value).coerceAtMost(1f),
        m = m,
        y = y,
        k = k
    )

    fun blendM(value: Float) = ColorCMYK(
        c = c,
        m = (m + value).coerceAtMost(1f),
        y = y,
        k = k
    )

    fun blendY(value: Float) = ColorCMYK(
        c = c,
        m = m,
        y = (y + value).coerceAtMost(1f),
        k = k
    )


    /**
     * Returns the total ink coverage (0.0 to 4.0).
     */
    val totalInk: Float
        get() = c + m + y + k

    companion object {
        fun of(color4f: Color4f): ColorCMYK {
            val r = color4f.r
            val g = color4f.g
            val b = color4f.b

            // Convert RGB to CMYK
            val k = 1f - maxOf(r, g, b)

            if (k >= 1f) {
                return ColorCMYK(0f, 0f, 0f, 1f) // Pure black
            }

            val c = (1f - r - k) / (1f - k)
            val m = (1f - g - k) / (1f - k)
            val y = (1f - b - k) / (1f - k)

            return ColorCMYK(c, m, y, k)
        }

        // Common CMYK colors
        val WHITE = ColorCMYK(0f, 0f, 0f, 0f)
        val BLACK = ColorCMYK(0f, 0f, 0f, 1f)
        val CYAN = ColorCMYK(1f, 0f, 0f, 0f)
        val MAGENTA = ColorCMYK(0f, 1f, 0f, 0f)
        val YELLOW = ColorCMYK(0f, 0f, 1f, 0f)
        val RED = ColorCMYK(0f, 1f, 1f, 0f)      // Magenta + Yellow
        val GREEN = ColorCMYK(1f, 0f, 1f, 0f)     // Cyan + Yellow
        val BLUE = ColorCMYK(1f, 1f, 0f, 0f)      // Cyan + Magenta
    }
}
