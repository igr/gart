package dev.oblac.gart.color.space

import dev.oblac.gart.color.ColorRGBA
import dev.oblac.gart.color.RGBA

/**
 * CMYK color representation (Cyan, Magenta, Yellow, Key/Black).
 * Values are normalized to 0.0-1.0 range.
 */
data class ColorCMYK(val c: Float, val m: Float, val y: Float, val k: Float) {

    /**
     * Converts CMYK to RGB color space.
     */
    fun toColorRGB(): ColorRGBA {
        // Standard CMYK to RGB conversion
        val r = (1f - c) * (1f - k)
        val g = (1f - m) * (1f - k)
        val b = (1f - y) * (1f - k)

        return ColorRGBA(r, g, b, 1f)
    }

    /**
     * Converts CMYK to RGBA with integer values (0-255).
     */
    fun toRGBA() = toColorRGB().toRGBA()

    /**
     * Converts to integer color value.
     */
    val value: Int
        get() = toRGBA().value

    /**
     * Creates a pure ink color for this CMYK component.
     * Returns the color when only this component has ink.
     */
    fun toPureInk(): RGBA {
        return when {
            c > 0 && m == 0f && y == 0f && k == 0f -> RGBA((255 * (1 - c)).toInt(), 255, 255) // Cyan
            m > 0 && c == 0f && y == 0f && k == 0f -> RGBA(255, (255 * (1 - m)).toInt(), 255) // Magenta
            y > 0 && c == 0f && m == 0f && k == 0f -> RGBA(255, 255, (255 * (1 - y)).toInt()) // Yellow
            k > 0 && c == 0f && m == 0f && y == 0f -> RGBA((255 * (1 - k)).toInt(), (255 * (1 - k)).toInt(), (255 * (1 - k)).toInt()) // Black
            else -> toRGBA() // Mixed color
        }
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
        fun of(rgba: RGBA): ColorCMYK {
            val r = rgba.r / 255f
            val g = rgba.g / 255f
            val b = rgba.b / 255f

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

        fun of(color: Int): ColorCMYK {
            return of(RGBA.of(color))
        }

        fun of(c: Float, m: Float, y: Float, k: Float): ColorCMYK {
            return ColorCMYK(
                c.coerceIn(0f, 1f),
                m.coerceIn(0f, 1f),
                y.coerceIn(0f, 1f),
                k.coerceIn(0f, 1f)
            )
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
