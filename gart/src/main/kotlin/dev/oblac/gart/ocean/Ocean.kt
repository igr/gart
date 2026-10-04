package dev.oblac.gart.ocean

import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.isPowerOfTwo
import dev.oblac.gart.math.lnGamma
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

private const val G = 9.81f

/**
 * The JONSWAP peak factor: [gamma] to the power `e^(-(w - wp)^2 / (2 sigma^2 wp^2))`, with sigma 0.07
 * under the peak frequency [wp] and 0.09 over it. At the peak it is [gamma], far from it 1.
 */
internal fun peakFactor(w: Float, wp: Float, gamma: Float): Float {
    val sg = if (w <= wp) 0.07f else 0.09f
    return gamma.pow(exp(-(w - wp) * (w - wp) / (2f * sg * sg * wp * wp)))
}

/**
 * Mitsuyasu's directional spread: `cos(angle / 2)^(2s)` around the heading, times
 * `G0(s) = gamma(s + 1) / (2 sqrt(pi) gamma(s + 1/2))`. With `G0` it adds up to 1 over a full turn,
 * so the spread moves energy between directions and never between wave numbers.
 */
internal fun spreading(angle: Float, s: Float): Float {
    val g0 = exp(lnGamma(s + 1.0) - lnGamma(s + 0.5)) / (2.0 * sqrt(PI))
    return (g0 * abs(cos(angle / 2.0)).pow(2.0 * s)).toFloat()
}

// points for the squeeze quantile in foamLimit. at a 5% share one standard error is 0.1%
private const val SQUEEZE_SAMPLES = 40000

// those points roll from their own random stream, so they take no dice from the waves.
// the prime spreads nearby seeds apart, the salt tells this stream from others
private const val SIDE_PRIME = 7919
private const val SQUEEZE_SALT = 3

/**
 * An open sea from a wave spectrum, built once and sampled anywhere.
 *
 * The spectrum is JONSWAP with a directional spread: the long waves keep to [heading], the
 * ripples go every way. Three tiles hold it. Each tile has [size] x [size] texels with 8 fields:
 * the height, the sideways move, the slopes and the squeeze of the move. The tile sizes have a
 * ratio of 6.73, so their repeats do not line up. The heights are scaled to [hs].
 *
 * The random rolls do not depend on the wind. A different [wind] gives the same sea at another
 * scale, longer and taller together. [swell] lifts the long waves only, [ripples] the short ones.
 *
 * Sample it with a [probe], one per thread.
 *
 * The math, in short:
 * - A spectrum gives the energy of the waves at each length. The sea is a sum of many sine waves,
 *   and the spectrum sets the height of each one.
 * - JONSWAP is a spectrum measured on wind seas in the North Sea. It is the Pierson-Moskowitz
 *   spectrum of a fully grown sea, with its peak made sharper by the factor [gamma].
 * - The peak wave number is `kp = 0.77 g / wind^2`. At 9 m/s the strongest waves are about 68 m long.
 * - The significant height [hs] is the mean height of the highest third of the waves. A fully grown
 *   sea has `hs = 0.21 wind^2 / g`.
 * - In deep water, a wave with the wave number k has the frequency `sqrt(g k)`. This turns the
 *   spectrum from frequency to wave number.
 * - The directional spread is `cos(angle / 2)^(2s)` around the heading (Mitsuyasu). It is scaled to
 *   add up to 1 over a full turn, so it moves energy between directions, not between wave numbers.
 * - The value s is largest at the peak, so the strongest waves keep to the heading.
 * - Each wave number gets two gaussian random numbers. Together they give its wave a random height
 *   and a random phase.
 * - An inverse FFT (fast Fourier transform) adds all the sine waves of a tile into one map that
 *   repeats.
 * - The chop moves each point sideways toward the crest, by `i k / |k|` times the height
 *   (Tessendorf). Crests get sharp and troughs get flat.
 * - The squeeze is the Jacobian of that move: how much a patch of water shrinks. It is 1 with no
 *   move, and less than 1 where a crest pinches.
 * - Whitecaps cover a share of `3.84e-6 wind^3.41` of the sea (Monahan).
 * - Cox and Munk measured the slopes of a real sea from sun glitter. The slope variance is
 *   `0.003 + 0.00512 wind`.
 * - The peak wave number and [hs] are those of a fully grown sea. With [gamma] at 1 the spectrum is
 *   Pierson-Moskowitz, which matches them.
 * - With [gamma], [swell] and [ripples] at 1, the defaults, the sea follows these models. Other
 *   values are an art choice.
 *
 * References:
 * - Pierson, W. J. and Moskowitz, L. (1964). A proposed spectral form for fully developed wind seas
 *   based on the similarity theory of S. A. Kitaigorodskii. J. Geophys. Res. 69(24), 5181-5190.
 *   The peak wave number and [hs] of a fully grown sea come from it.
 *   <https://doi.org/10.1029/JZ069i024p05181>
 * - Hasselmann, K. et al. (1973). Measurements of wind-wave growth and swell decay during the Joint
 *   North Sea Wave Project (JONSWAP). Dtsch. Hydrogr. Z., Erg. A8, Nr. 12. <https://epic.awi.de/10163/>
 * - Longuet-Higgins, M. S. (1952). On the statistical distribution of the heights of sea waves.
 *   J. Mar. Res. 11(3), 245-266. [hs] is 4 times the standard deviation of the height.
 *   <https://elischolar.library.yale.edu/journal_of_marine_research/774>
 * - Longuet-Higgins, M. S., Cartwright, D. E. and Smith, N. D. (1963). Observations of the
 *   directional spectrum of sea waves using the motions of a floating buoy. In Ocean Wave Spectra,
 *   Prentice-Hall, 111-136. The `cos(angle / 2)^(2s)` spread.
 * - Mitsuyasu, H. et al. (1975). Observations of the directional spectrum of ocean waves using a
 *   cloverleaf buoy. J. Phys. Oceanogr. 5, 750-760. The value s by frequency.
 * - Tessendorf, J. (2001). Simulating Ocean Water. SIGGRAPH course notes, revised 2004. The random
 *   sea from a spectrum by FFT, the chop and its squeeze.
 *   <https://jtessen.people.clemson.edu/reports/papers_files/coursenotes2004.pdf>
 * - Monahan, E. C. and O'Muircheartaigh, I. (1980). Optimal power-law description of oceanic
 *   whitecap coverage dependence on wind speed. J. Phys. Oceanogr. 10, 2094-2099.
 *   <https://doi.org/10.1175/1520-0485(1980)010%3C2094:OPLDOO%3E2.0.CO;2>
 * - Cox, C. and Munk, W. (1954). Measurement of the roughness of the sea surface from photographs
 *   of the sun's glitter. J. Opt. Soc. Am. 44(11), 838-850. <https://doi.org/10.1364/JOSA.44.000838>
 *
 * @param wind    m/s, sets how long and how tall the waves are, together
 * @param gamma   the JONSWAP peak factor, how sharp the peak of the spectrum is, at least 1. 1, the
 *                default, is the Pierson-Moskowitz spectrum of a fully grown sea. It matches [hs] and
 *                the peak. 3.3 is the mean value for a young sea that still grows, and real seas go to
 *                about 7. Over 1 the peak and the height stay those of a fully grown sea, so the young
 *                shape on a grown size is an art choice
 * @param swell   multiplies the long waves, the large tile: their height, sideways move and slope.
 *                Their energy goes with the square. 1, the default, gives the model spectrum. Other
 *                values are an art choice: real swell comes from a far storm as its own narrow
 *                spectrum. The slope of the swell does not count against the Cox-Munk total
 * @param spread  how long the crests run, 40 is corduroy
 * @param heading the way the waves travel in the x-z plane, rad. -PI/2 is toward -z
 * @param chop    the sideways pinch: sharp crests and flat troughs. 0 is plain sine-like waves
 * @param ripples multiplies the short waves, the two small tiles, in the same way. 1, the default,
 *                gives the model spectrum. Other values are an art choice: at 1.6 the short waves
 *                carry 2.56 times the energy of the model. They are the wind's own waves, so their
 *                slope counts against the Cox-Munk total, and more ripples leave less to [residualSlope]
 * @param size    FFT grid per tile, a power of two, at least 8
 */
class Ocean(
    private val seed: Int,
    val wind: Float = 9f,
    val gamma: Float = 1f,
    val swell: Float = 1f,
    val spread: Float = 8f,
    val heading: Float = -PIf / 2f,
    val chop: Float = 0.9f,
    val ripples: Float = 1f,
    val size: Int = 256,
) {
    init {
        require(size.isPowerOfTwo()) { "size must be a power of two, got $size" }
        // under 8, every wave number falls outside its tile's band and the sea is empty
        require(size >= 8) { "size must be at least 8, got $size" }
        require(wind > 0f) { "wind must be positive, got $wind" }
        require(gamma >= 1f) { "gamma must be at least 1, got $gamma" }
    }

    private val kp = 0.77f * G / (wind * wind) // spectral peak, ~68 m waves at 9 m/s

    /**
     * Significant height, m: about the mean height of the highest third of the waves.
     */
    val hs = 0.21f * wind * wind / G

    /**
     * The length of the strongest waves, m.
     */
    val peakLength = TAUf / kp

    internal val tiles: Array<Tile>

    /**
     * No point of the surface is higher than this, or lower than its negative.
     */
    val top: Float

    /**
     * The slope variance that the tiles cannot hold: the Cox-Munk total for the wind, minus what
     * the tiles hold at swell 1 (with the ripples as drawn), at least 0.002. Add it to the roughness
     * of the surface.
     */
    val residualSlope: Float

    init {
        val l0 = 8f * TAUf / kp // the peak sits 8 cells out from the origin
        val ratio = 6.73f
        val sizes = floatArrayOf(l0, l0 / ratio, l0 / ratio / ratio)
        val cut = sizes.map { PIf * size / (2f * it) } // half nyquist, bilinear still draws that fine
        tiles = Array(3) { Tile(size, sizes[it], if (it == 0) 0f else cut[it - 1], cut[it], kp) }
        val rnd = Random(seed)
        tiles.forEach { it.build(rnd, ::spectrum) }

        val cells = size * size
        var v = 0.0
        tiles.forEach { t -> for (i in 0 until cells) v += t.f[i * NF].toDouble().pow(2) / cells }
        val scale = (hs / 4f / sqrt(v)).toFloat()
        var windVar = 0f
        for ((ti, t) in tiles.withIndex()) {
            val s = scale * if (ti == 0) swell else ripples
            // the slope that counts against the roughness. the swell is waves on top of the wind sea,
            // so it stays out. the ripples are the wind's own, so they count as drawn
            val budget = if (ti == 0) scale else s
            var sv = 0.0
            var base = 0.0
            for (i in 0 until cells) {
                val bx = t.f[i * NF + 3] * budget
                val bz = t.f[i * NF + 4] * budget
                base += (bx * bx + bz * bz).toDouble() / cells
                for (k in 0 until NF) t.f[i * NF + k] *= s
                val sx = t.f[i * NF + 3]
                val sz = t.f[i * NF + 4]
                sv += (sx * sx + sz * sz).toDouble() / cells
                t.maxH = max(t.maxH, abs(t.f[i * NF]))
            }
            t.slopeVar = sv.toFloat()
            t.bounds()
            windVar += base.toFloat()
        }
        residualSlope = max(0.003f + 0.00512f * wind - windVar, 0.002f)
        top = tiles.sumOf { it.maxH.toDouble() }.toFloat() * 1.05f + 0.05f
    }

    // the squeeze at random points of the moved surface, sorted. a pinched patch covers less of the
    // world, so the points must be world points, not points of the grid before the chop
    private val squeezes: FloatArray by lazy {
        val l0 = tiles[0].size
        val rnd = Random(seed * SIDE_PRIME + SQUEEZE_SALT)
        val p = probe()
        FloatArray(SQUEEZE_SAMPLES) {
            p.at(rnd.nextFloat() * l0, rnd.nextFloat() * l0, 0f)
            p.squeeze
        }.also { it.sort() }
    }

    /**
     * The squeeze under which the surface is foam, for [times] x the whitecaps of the wind.
     *
     * The squeeze has the same spread at any wind, only breaking grows with it. So foam is a share
     * of the surface, `3.84e-6 wind^3.41` (at most 0.4), and the limit is the squeeze that leaves
     * that share under it. With no chop every squeeze is 1, so the limit is 1 and no point is
     * under it. With [times] 0 the limit is -10, far under any squeeze.
     */
    fun foamLimit(times: Float = 1f): Float {
        val share = min(3.84e-6f * wind.pow(3.41f) * times, 0.4f)
        if (share <= 0f) return -10f
        return squeezes[(share * squeezes.size).toInt()]
    }

    /**
     * A new probe to sample this sea. A probe keeps scratch memory, so each thread needs its own.
     */
    fun probe() = OceanProbe(this)

    // jonswap with no alpha, the heights get scaled later. per dk, not dw
    private fun spectrum(kx: Float, kz: Float): Float {
        val k = hypot(kx, kz)
        if (k < 1e-5f) return 0f
        val w = sqrt(G * k)
        val wp = sqrt(G * kp)
        val s = G * G / w.pow(5) * exp(-1.25f * (wp / w).pow(4)) * peakFactor(w, wp, gamma)
        val sk = s * G / (2f * w)
        // the long waves keep to the heading, the ripples go every way
        val r = k / kp
        val sp = (spread * if (r > 1f) r.pow(-1.25f) else r.pow(2.5f)).coerceIn(0.5f, 60f)
        val d = spreading(atan2(kz, kx) - heading, sp)
        return sk * d / k
    }
}
