package dev.oblac.gart.ocean

import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.fft2InPlace
import dev.oblac.gart.math.rndGaussianf
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

internal const val NF = 8 // per texel: h, dx, dz, sx, sz, jxx, jzz, jxz

/**
 * One band of the spectrum, wave numbers [kLo] to [kHi], as [n] x [n] texels that repeat every
 * [size] meters.
 *
 * The math, in short:
 * - The height at wave number k is `h0(k) + conj(h0(-k))`. This symmetry makes the map real.
 * - The other fields are the height times a factor: `i k` for the slopes, `i k / |k|` for the
 *   chop move, and `-k k / |k|` for the change of that move, which gives the squeeze.
 * - One complex FFT makes two real fields: one in the real part, one in the imaginary part.
 */
internal class Tile(val n: Int, val size: Float, val kLo: Float, val kHi: Float, kp: Float) {
    private val mask = n - 1
    val f = FloatArray(n * n * NF)
    val inv = n / size
    val mid = TAUf / sqrt(max(kLo, kp * 0.5f) * kHi) // a wavelength in the middle of the band
    var slopeVar = 0f // what the tile adds to the roughness once it is too small to see
    var maxGrad = 0f // the steepest height, on the bilinear field the march sees
    var maxMove = 0f // the steepest sideways move, a frobenius bound on its jacobian
    var maxH = 0f

    // inside a cell every gradient of the bilinear fields is a blend of the differences across its
    // edges, so the largest edge differences bound the whole cell
    fun bounds() {
        var g = 0f
        var m = 0f
        for (z in 0 until n) for (x in 0 until n) {
            val a = (z * n + x) * NF
            val b = (z * n + ((x + 1) and mask)) * NF
            val c = (((z + 1) and mask) * n + x) * NF
            val d = (((z + 1) and mask) * n + ((x + 1) and mask)) * NF
            var hx = 0f
            var hz = 0f
            var mm = 0f
            for (k in 0 until 3) {
                val ex = max(abs(f[b + k] - f[a + k]), abs(f[d + k] - f[c + k])) * inv
                val ez = max(abs(f[c + k] - f[a + k]), abs(f[d + k] - f[b + k])) * inv
                if (k == 0) {
                    hx = ex
                    hz = ez
                } else mm += ex * ex + ez * ez
            }
            g = max(g, sqrt(hx * hx + hz * hz))
            m = max(m, sqrt(mm))
        }
        maxGrad = g
        maxMove = m
    }

    // bilinear, wrapping. adds w * fields [k0, k1) at world (x, z) into acc
    fun sample(x: Float, z: Float, k0: Int, k1: Int, w: Float, acc: FloatArray) {
        val u = x * inv
        val v = z * inv
        val fu = floor(u)
        val fv = floor(v)
        val tu = u - fu
        val tv = v - fv
        val x0 = fu.toInt() and mask
        val z0 = fv.toInt() and mask
        val x1 = (x0 + 1) and mask
        val z1 = (z0 + 1) and mask
        val a = (z0 * n + x0) * NF
        val b = (z0 * n + x1) * NF
        val c = (z1 * n + x0) * NF
        val d = (z1 * n + x1) * NF
        val wa = (1f - tu) * (1f - tv) * w
        val wb = tu * (1f - tv) * w
        val wc = (1f - tu) * tv * w
        val wd = tu * tv * w
        for (k in k0 until k1) acc[k] += f[a + k] * wa + f[b + k] * wb + f[c + k] * wc + f[d + k] * wd
    }

    fun build(rnd: Random, spectrum: (Float, Float) -> Float) {
        val dk = TAUf / size
        val h0r = FloatArray(n * n)
        val h0i = FloatArray(n * n)
        for (j in 0 until n) for (m in 0 until n) {
            val kx = (if (m < n / 2) m else m - n) * dk
            val kz = (if (j < n / 2) j else j - n) * dk
            // always roll, so the wind reshapes the same sea instead of dealing a new one
            val gr = rnd.rndGaussianf()
            val gi = rnd.rndGaussianf()
            val k = hypot(kx, kz)
            if (k < kLo || k >= kHi) continue
            val amp = sqrt(spectrum(kx, kz) / 2f) * dk
            h0r[j * n + m] = gr * amp
            h0i[j * n + m] = gi * amp
        }
        // h(k) = h0(k) + conj(h0(-k)), then every field is h times something
        val spec = Array(NF) { FloatArray(n * n) }
        val speci = Array(NF) { FloatArray(n * n) }
        for (j in 0 until n) for (m in 0 until n) {
            val i = j * n + m
            val jj = ((n - j) and mask) * n + ((n - m) and mask)
            val hr = h0r[i] + h0r[jj]
            val hi = h0i[i] - h0i[jj]
            val kx = (if (m < n / 2) m else m - n) * dk
            val kz = (if (j < n / 2) j else j - n) * dk
            val k = max(hypot(kx, kz), 1e-6f)
            spec[0][i] = hr
            speci[0][i] = hi
            // i k/k h, so the water runs toward the crests. the other sign spreads them flat
            spec[1][i] = -kx / k * hi
            speci[1][i] = kx / k * hr
            spec[2][i] = -kz / k * hi
            speci[2][i] = kz / k * hr
            spec[3][i] = -kx * hi // i kx h
            speci[3][i] = kx * hr
            spec[4][i] = -kz * hi
            speci[4][i] = kz * hr
            spec[5][i] = -kx * kx / k * hr
            speci[5][i] = -kx * kx / k * hi
            spec[6][i] = -kz * kz / k * hr
            speci[6][i] = -kz * kz / k * hi
            spec[7][i] = -kx * kz / k * hr
            speci[7][i] = -kx * kz / k * hi
        }
        // two real fields per transform, a + ib. both spectra are hermitian, so both come back real
        for (p in 0 until NF step 2) {
            val re = DoubleArray(n * n) { (spec[p][it] - speci[p + 1][it]).toDouble() }
            val im = DoubleArray(n * n) { (speci[p][it] + spec[p + 1][it]).toDouble() }
            fft2InPlace(re, im, n, n, inverse = true)
            for (i in 0 until n * n) {
                f[i * NF + p] = re[i].toFloat()
                f[i * NF + p + 1] = im[i].toFloat()
            }
        }
    }
}
