package dev.oblac.gart.color

import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa

data class ColorRGBA(val r: Float, val g: Float, val b: Float, val a: Float) {
    private val c = ColorRGBa(r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble())

    fun toHSLA(): ColorHSLA {
        val hsla = c.toHSLa()
        return ColorHSLA(hsla.h.toFloat(), hsla.s.toFloat(), hsla.l.toFloat(), hsla.alpha.toFloat())
    }

    fun toRGBA(): RGBA {
        return RGBA((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), (a * 255).toInt())
    }

    companion object {
        fun ofColor(color: Int): ColorRGBA {
            return ColorRGBA(
                red(color) / 255f,
                green(color) / 255f,
                blue(color) / 255f,
                alpha(color) / 255f
            )
        }
    }
}

data class ColorHSLA(val h: Float, val s: Float, val l: Float, val a: Float) {
    private val c = ColorHSLa(h.toDouble(), s.toDouble(), l.toDouble(), a.toDouble())

    fun toRGBA(): ColorRGBA {
        val rgba = c.toRGBa()
        return ColorRGBA(rgba.r.toFloat(), rgba.g.toFloat(), rgba.b.toFloat(), rgba.alpha.toFloat())
    }

    fun shade(factor: Float): ColorHSLA {
        val c2 = c.shade(factor.toDouble())
        return ColorHSLA(c2.h.toFloat(), c2.s.toFloat(), c2.l.toFloat(), c2.alpha.toFloat())
    }

    fun saturate(factor: Float): ColorHSLA {
        val c2 = c.saturate(factor.toDouble())
        return ColorHSLA(c2.h.toFloat(), c2.s.toFloat(), c2.l.toFloat(), c2.alpha.toFloat())
    }
}

data class RGBA(
	val r: Int,
	val g: Int,
	val b: Int,
	val a: Int = 255
) {
    val value: Int = argb(a, r, g, b)

    companion object {
		fun of(value: Long): RGBA {
            return of(value.toInt())
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

}
