package dev.oblac.gart.color

import dev.oblac.gart.color.space.ColorLAB
import dev.oblac.gart.color.space.of
import org.jetbrains.skia.Color4f
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generates color palettes.
 * When bezier=false, uses linear RGB interpolation between input colors.
 * When bezier=true, uses Bezier interpolation in LAB color space.
 * correctLightness adjusts sampling so lightness changes linearly.
 */
object PaletteGenerator {

    /**
     * Generates a sequential palette from the given input colors.
     *
     * @param colors input colors (at least 2)
     * @param numColors number of output colors
     * @param bezier if true, uses Bezier interpolation in LAB space; otherwise linear RGB
     * @param correctLightness if true, corrects lightness to change linearly
     */
    fun sequential(
        colors: List<Int>,
        numColors: Int,
        bezier: Boolean = true,
        correctLightness: Boolean = true
    ): Palette {
        require(colors.size >= 2) { "At least 2 input colors required" }
        val interpolator: (Float) -> Color4f = if (bezier) {
            val labColors = colors.map { ColorLAB.of(Color4f.of(it)) }
            val labInterp = bezierInterpolator(labColors);
            { t: Float -> labInterp(t).toColor4f() }
        } else {
            linearRgbInterpolator(colors)
        }
        val finalInterp = if (correctLightness) lightnessCorrect(interpolator) else interpolator
        return samplePalette(finalInterp, numColors)
    }

    /**
     * Generates a diverging palette from two sets of input colors meeting at a neutral midpoint.
     *
     * @param colorsLeft left side colors (low end)
     * @param colorsRight right side colors (high end)
     * @param numColors total number of output colors
     * @param bezier if true, uses Bezier interpolation in LAB space
     * @param correctLightness if true, corrects lightness to change linearly
     */
    fun diverging(
        colorsLeft: List<Int>,
        colorsRight: List<Int>,
        numColors: Int,
        bezier: Boolean = true,
        correctLightness: Boolean = true
    ): Palette {
        require(colorsLeft.size >= 2) { "At least 2 left colors required" }
        require(colorsRight.size >= 2) { "At least 2 right colors required" }

        val even = numColors % 2 == 0
        val numLeft = numColors / 2 + 1
        val numRight = numColors / 2 + 1

        val leftPalette = sequential(colorsLeft, numLeft, bezier, correctLightness)
        val rightPalette = sequential(colorsRight, numRight, bezier, correctLightness)

        // Join: for even counts, drop the last element of left half to avoid duplicate midpoint
        val leftColors = if (even) leftPalette.colors.dropLast(1) else leftPalette.colors.toList()
        val rightColors = rightPalette.colors.drop(1)

        return Palette.of(leftColors + rightColors)
    }

    /**
     * Generates a sequential palette from hex color strings.
     */
    fun sequentialHex(
        vararg hexColors: String,
        numColors: Int,
        bezier: Boolean = true,
        correctLightness: Boolean = true
    ): Palette {
        return sequential(hexColors.map { it.parseColor() }, numColors, bezier, correctLightness)
    }

    /**
     * Generates a diverging palette from hex color strings.
     */
    fun divergingHex(
        colorsLeft: List<String>,
        colorsRight: List<String>,
        numColors: Int,
        bezier: Boolean = true,
        correctLightness: Boolean = true
    ): Palette {
        return diverging(
            colorsLeft.map { it.parseColor() },
            colorsRight.map { it.parseColor() },
            numColors,
            bezier,
            correctLightness
        )
    }

    // -- Interpolators --

    /**
     * Linear interpolation in RGB space between adjacent input colors.
     * Matches chroma.scale(colors) with default 'rgb' mode.
     */
    private fun linearRgbInterpolator(colors: List<Int>): (Float) -> Color4f {
        val n = colors.size - 1
        return { t: Float ->
            val tc = t.coerceIn(0f, 1f)
            val scaled = tc * n
            val i = scaled.toInt().coerceAtMost(n - 1)
            val f = scaled - i
            val c0 = colors[i]
            val c1 = colors[i + 1]
            val r = red(c0) + f * (red(c1) - red(c0))
            val g = green(c0) + f * (green(c1) - green(c0))
            val b = blue(c0) + f * (blue(c1) - blue(c0))
            val a = alpha(c0) + f * (alpha(c1) - alpha(c0))
            Color4f(r / 255f, g / 255f, b / 255f, a / 255f)
        }
    }

    /**
     * Bezier interpolation through LAB color space using De Casteljau's algorithm.
     */
    private fun bezierInterpolator(labColors: List<ColorLAB>): (Float) -> ColorLAB {
        return { t: Float ->
            val tc = t.coerceIn(0f, 1f)
            deCasteljau(labColors, tc)
        }
    }

    private fun deCasteljau(points: List<ColorLAB>, t: Float): ColorLAB {
        if (points.size == 1) return points[0]
        val next = (0 until points.size - 1).map { i ->
            points[i].mix(points[i + 1], t)
        }
        return deCasteljau(next, t)
    }

    // -- Lightness correction --

    /**
     * Wraps an interpolator so that the lightness of the output changes linearly
     * across the [0..1] range, matching chroma.js correctLightness behavior.
     * For each t, binary-searches for the corrected t where L equals the
     * linearly interpolated lightness between the endpoint lightnesses.
     */
    private fun lightnessCorrect(interpolator: (Float) -> Color4f): (Float) -> Color4f {
        val l0 = ColorLAB.of(interpolator(0f)).l
        val l1 = ColorLAB.of(interpolator(1f)).l
        val pol = l0 > l1

        return { t: Float ->
            val tc = t.coerceIn(0f, 1f)
            val lIdeal = l0 + (l1 - l0) * tc
            var lActual = ColorLAB.of(interpolator(tc)).l
            var lDiff = lActual - lIdeal
            var t0 = 0f
            var t1 = 1f
            var correctedT = tc
            var maxIter = 20

            while (abs(lDiff) > 1e-2f && maxIter-- > 0) {
                var adjustedDiff = lDiff
                if (pol) adjustedDiff *= -1f
                if (adjustedDiff < 0) {
                    t0 = correctedT
                    correctedT += (t1 - correctedT) * 0.5f
                } else {
                    t1 = correctedT
                    correctedT += (t0 - correctedT) * 0.5f
                }
                lActual = ColorLAB.of(interpolator(correctedT)).l
                lDiff = lActual - lIdeal
            }
            interpolator(correctedT)
        }
    }

    // -- Sampling --

    private fun samplePalette(interpolator: (Float) -> Color4f, numColors: Int): Palette {
        val colors = IntArray(numColors) { i ->
            val t = if (numColors == 1) 0.5f else i.toFloat() / (numColors - 1)
            val c = interpolator(t)
            argb(
                (c.a * 255).roundToInt().coerceIn(0, 255),
                (c.r * 255).roundToInt().coerceIn(0, 255),
                (c.g * 255).roundToInt().coerceIn(0, 255),
                (c.b * 255).roundToInt().coerceIn(0, 255)
            )
        }
        return Palette(colors)
    }
}
