package ommatidia

import dev.oblac.gart.noise.fbm
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

// textbook fbm: frequency doubles and amplitude halves each octave. 2.03 rather than a flat
// 2 so successive octaves don't keep landing on the same grid alignment. the lib's fbm
// defaults to 4.0/0.6, so both go in explicitly at every call.
internal const val NZ_LAC = 2.03f
internal const val NZ_GAIN = 0.5f

/**
 * the surface nothing ever draws. a handful of gaussian swells plus fbm ripple.
 */
internal class Membrane(p: Params, rnd: Random, private val nzOff: Float) {
    private val count = p.swells
    private val cx = FloatArray(count)
    private val cy = FloatArray(count)
    private val rad = FloatArray(count)
    private val amp = FloatArray(count)
    private val gain = p.swellAmp
    private val ripple = p.ripple
    private val rippleF = p.rippleF
    private val relief = p.relief

    init {
        for (i in 0 until count) {
            if (i == 0) {
                // the dominant swell. near the middle but never dead centre
                cx[i] = 0.5f + (rnd.nextFloat() - 0.5f) * 0.34f
                cy[i] = 0.5f + (rnd.nextFloat() - 0.5f) * 0.34f
                rad[i] = p.swellR
                amp[i] = 1f
            } else {
                // the rest scatter, some of them off the edge so the frame reads as a crop
                cx[i] = -0.15f + rnd.nextFloat() * 1.3f
                cy[i] = -0.15f + rnd.nextFloat() * 1.3f
                rad[i] = p.swellR * (0.22f + rnd.nextFloat() * 0.5f)


                amp[i] = (0.25f + rnd.nextFloat() * 0.55f) * if (rnd.nextFloat() < p.dimples) -1f else 1f
            }
        }
    }

    fun height(x: Float, y: Float): Float {
        var v = 0f
        for (i in 0 until count) {
            val dx = (x - cx[i]) / rad[i]
            val dy = (y - cy[i]) / rad[i]
            v += amp[i] * exp(-(dx * dx + dy * dy))
        }
        // 3 octaves
        if (ripple != 0f) v += ripple * fbm(x * rippleF + nzOff, y * rippleF - nzOff, 3, NZ_LAC, NZ_GAIN)
        return v * gain
    }

    fun normalInto(x: Float, y: Float, out: FloatArray) {
        val e = 0.0015f
        val hx = (height(x + e, y) - height(x - e, y)) / (2f * e)
        val hy = (height(x, y + e) - height(x, y - e)) / (2f * e)
        val nx = -hx * relief
        val ny = -hy * relief
        val len = sqrt(nx * nx + ny * ny + 1f)
        out[0] = nx / len
        out[1] = ny / len
        out[2] = 1f / len
    }
}

/**
 * refract the view ray
 */
internal fun opticalSample(
    x: Float,
    y: Float,
    nx: Float,
    ny: Float,
    nz: Float,
    eta: Float,
    depth: Float,
    out: FloatArray,
) {
    val k = 1f - eta * eta * (1f - nz * nz)
    if (k <= 0f) {
        // total internal reflection
        out[0] = x
        out[1] = y
        return
    }
    val f = eta * nz - sqrt(k)
    val rz = -eta + f * nz
    if (rz >= -1e-4f) {
        out[0] = x
        out[1] = y
        return
    }
    val t = depth / -rz
    out[0] = x + f * nx * t
    out[1] = y + f * ny * t
}
