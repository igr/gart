package dev.oblac.gart.color.space

data class ColorHSLA(val h: Float, val s: Float, val l: Float, val a: Float) {

    fun toColorRGBA(): ColorRGBA {
        val h = h / 360f // Convert degrees to 0-1 range

        val r: Float
        val g: Float
        val b: Float

        if (s == 0f) {
            // Achromatic (gray) - no saturation
            r = l
            g = l
            b = l
        } else {
            // Helper function to convert hue to RGB
            fun hueToRgb(p: Float, q: Float, t: Float): Float {
                var hue = t
                if (hue < 0f) hue += 1f
                if (hue > 1f) hue -= 1f

                return when {
                    hue < 1f / 6f -> p + (q - p) * 6f * hue
                    hue < 1f / 2f -> q
                    hue < 2f / 3f -> p + (q - p) * (2f / 3f - hue) * 6f
                    else -> p
                }
            }

            val q = if (l < 0.5f) {
                l * (1f + s)
            } else {
                l + s - l * s
            }
            val p = 2f * l - q

            r = hueToRgb(p, q, h + 1f / 3f)
            g = hueToRgb(p, q, h)
            b = hueToRgb(p, q, h - 1f / 3f)
        }

        return ColorRGBA(r, g, b, a)
    }

    fun shade(factor: Float) = copy(l = l * factor)
    fun saturate(factor: Float) = copy(s = s * factor)
}
