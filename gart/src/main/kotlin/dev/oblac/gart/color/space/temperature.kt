package dev.oblac.gart.color.space

import dev.oblac.gart.math.f
import org.jetbrains.skia.Color4f
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Estimates the color temperature in Kelvin for this color.
 * Based on Neil Bartlett's color-temperature implementation.
 * Uses binary search to find the temperature whose blue/red ratio
 * matches this color's blue/red ratio.
 */
val Color4f.temperature: Int
    get() {
        var minTemp = 1000f
        var maxTemp = 40000f
        val eps = 0.4f
        while (maxTemp - minTemp > eps) {
            val temp = (maxTemp + minTemp) * 0.5f
            val rgb = temperature2rgb(temp)
            if (rgb.b / rgb.r >= b / r) {
                maxTemp = temp
            } else {
                minTemp = temp
            }
        }
        return ((maxTemp + minTemp) * 0.5f).roundToInt()
    }

/**
 * Creates a Color4f from a color temperature in Kelvin.
 * Based on Neil Bartlett's color-temperature implementation.
 */
fun Color4f.Companion.ofTemperature(kelvin: Float): Color4f {
    val rgb = temperature2rgb(kelvin)
    return Color4f(
        rgb.r.coerceIn(0f, 255f) / 255f,
        rgb.g.coerceIn(0f, 255f) / 255f,
        rgb.b.coerceIn(0f, 255f) / 255f,
        1f
    )
}

private fun temperature2rgb(kelvin: Float): Color4f {
    val temp = kelvin / 100.0
    val r: Double
    val g: Double
    val b: Double
    if (temp < 66) {
        r = 255.0
        g = if (temp < 6) 0.0
        else {
            val gt = temp - 2
            -155.25485562709179 - 0.44596950469579133 * gt + 104.49216199393888 * ln(gt)
        }
        b = if (temp < 20) 0.0
        else {
            val bt = temp - 10
            -254.76935184120902 + 0.8274096064007395 * bt + 115.67994401066147 * ln(bt)
        }
    } else {
        val rt = temp - 55
        r = 351.97690566805693 + 0.114206453784165 * rt - 40.25366309332127 * ln(rt)
        val gt = temp - 50
        g = 325.4494125711974 + 0.07943456536662342 * gt - 28.0852963507957 * ln(gt)
        b = 255.0
    }
    return Color4f(r.f(), g.f(), b.f())
}
