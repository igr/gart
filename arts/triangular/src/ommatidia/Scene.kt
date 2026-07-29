package ommatidia

import dev.oblac.gart.color.lerpColors
import dev.oblac.gart.color.space.ColorOKLCH
import dev.oblac.gart.color.space.color4f
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.toRadians
import dev.oblac.gart.noise.fbm
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private const val OPAQUE = 0xFF000000.toInt()

/**
 * the hidden scene. it is never drawn
 */
internal class Scene(private val p: Params, private val colors: Colors, private val nzOff: Float) {
    private val dirX = cos(p.bandAng.toRadians())
    private val dirY = sin(p.bandAng.toRadians())

    fun sample(x: Float, y: Float): Int {
        val w1 = fbm(x * p.warpF + nzOff, y * p.warpF - nzOff, 3, NZ_LAC, NZ_GAIN)
        val w2 = fbm(x * p.warpF * 2.3f - nzOff, y * p.warpF * 2.3f + nzOff, 2, NZ_LAC, NZ_GAIN)
        val u = (x * dirX + y * dirY) * p.bandF + p.warp * (w1 + 0.45f * w2)
        var t = 0.5f + 0.5f * sin(u * TAUf)


        // push the bands toward the ends of the ramp
        if (p.bandC != 0f) t = 0.5f + (t - 0.5f) * (1f + p.bandC)

        val col = lerpColors(colors.ramp, t)

        val dx = (x - p.sunX) / p.sunR
        val dy = (y - p.sunY) / p.sunR
        val glow = (exp(-(dx * dx + dy * dy)) * p.sunI).coerceIn(0f, 1f)
        if (glow < 0.004f) return col

        val o = ColorOKLCH.of(col.color4f())
        var dh = p.sunHue - o.h
        while (dh > 180f) dh -= 360f
        while (dh < -180f) dh += 360f
        return ColorOKLCH(
            o.l + (1f - o.l) * glow,
            o.c * (1f - glow * p.sunDesat),
            o.h + dh * glow * p.sunWarm,
        ).toColor4f().toColor() or OPAQUE
    }
}
