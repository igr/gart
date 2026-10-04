package dev.oblac.gart.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin

// lanczos, g = 7, 9 terms. good to about 1e-15
private val LANCZOS = doubleArrayOf(
    0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
    -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
    1.5056327351493116e-7,
)

/**
 * The natural log of the gamma function, for [x] > 0.
 *
 * The gamma function goes on from the factorial: `gamma(n) = (n - 1)!`. Its log does not overflow,
 * so a ratio like `gamma(a) / gamma(b)` is `exp(lnGamma(a) - lnGamma(b))`. It uses the Lanczos
 * approximation, and the reflection `gamma(x) gamma(1 - x) = pi / sin(pi x)` under 0.5.
 *
 * Reference: Lanczos, C. (1964). A precision approximation of the gamma function. J. SIAM Numer.
 * Anal. Ser. B 1, 86-96. <https://doi.org/10.1137/0701008> The coefficients are the g = 7, 9-term
 * set in common use.
 */
fun lnGamma(x: Double): Double {
    require(x > 0.0) { "x must be positive, got $x" }
    if (x < 0.5) return ln(PI / abs(sin(PI * x))) - lnGamma(1.0 - x)
    val y = x - 1.0
    var a = LANCZOS[0]
    for (i in 1 until LANCZOS.size) a += LANCZOS[i] / (y + i)
    val t = y + 7.5
    return 0.5 * ln(2.0 * PI) + (y + 0.5) * ln(t) - t + ln(a)
}
