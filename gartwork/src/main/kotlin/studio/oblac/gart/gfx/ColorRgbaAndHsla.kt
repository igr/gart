package studio.oblac.gart.gfx

import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

data class RGBA(
	val r: Int,
	val g: Int,
	val b: Int,
	val a: Int = 255
) {
    val value: Int = argb(a, r, g, b)

    companion object {
		fun of(value: Long): RGBA {
			return this.of(value.toInt())
		}
		fun of(value: Int): RGBA {
			return RGBA(
				a = alpha(value),
				r = red(value),
				g = green(value),
				b = blue(value)
			)
		}
	}

	/**
	 * Converts RGB to HSL
	 */
	fun toHSL(): HSLA {
		val r = r / 255f
		val g = g / 255f
		val b = b / 255f

		val cmin = min(r, min(g, b))
		val cmax = max(r, max(g, b))
		val delta = cmax - cmin

		val h = when (cmax) {
				cmin -> 0f
				r -> ((60 * (g - b) / delta + 360) % 360)
				g -> (60 * (b - r) / delta + 120)
				b -> (60 * (r - g) / delta + 240)
			else -> Float.NaN
		}

		val l: Float = (cmax + cmin) / 2f
		val s: Float = if (cmax == cmin) {
			0f
		} else (if (l <= 0.5f) {
			delta / (cmax + cmin)
		} else {
			delta / (2 - cmax - cmin)
		}).toFloat()

		return HSLA(h, s * 100, l * 100, a / 255f * 100f)
	}

}

data class HSLA(
    /**
     * Hue, degrees, `NaN` for monochrome colors. 0-360 degrees
     */
    val h: Float,
    /**
     * Saturation percentage [0, 100]
     */
    val s: Float,
    /**
     * Lightness percentage [0, 100]
     */
    val l: Float,

    /**
     * Alpha percentage [0, 100].
     */
    val a: Float = 1f
) {
    fun toRGBA(): RGBA {
        val h = h / 360f
        val s = s / 100f
        val l = l / 100f

        val h6 = h * 6

        val c = (1 - (2 * l - 1).absoluteValue) * s
        val x = c * (1 - (h6 % 2 - 1).absoluteValue)
        val m = l - c / 2

        val (r, g, b) = when {
            h6 < 1f -> Triple(c, x, 0f)
            h6 < 2f -> Triple(x, c, 0f)
            h6 < 3f -> Triple(0f, c, x)
            h6 < 4f -> Triple(0f, x, c)
            h6 < 5f -> Triple(x, 0f, c)
            h6 < 6f -> Triple(c, 0f, x)
            else -> Triple(0f, 0f, 0f)
        }

        return RGBA(
            r = ((r + m) * 255).toInt(),
            g = ((g + m) * 255).toInt(),
            b = ((b + m) * 255).toInt(),
            a = (a / 100f * 255).toInt()
        )
    }

    fun shade(factor: Float): HSLA {
        val l = this.l * (1f - factor)
        return copy(
            l = when {
                l < 0f -> 0f
                l > 100f -> 100f
                else -> l
            }
        )
    }

    fun tint(factor: Float): HSLA {
        val l = this.l * (1f + factor)
        return copy(
            l = when {
                l < 0f -> 0f
                l > 100f -> 100f
                else -> l
            }
        )
    }

}
