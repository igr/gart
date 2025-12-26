package dev.oblac.gart.whfast

import kotlin.math.*

/**
 * Solves Kepler's equation and provides utilities for Keplerian motion.
 * Uses Newton-Raphson iteration with Danby's initial guess for fast convergence.
 */
object KeplerSolver {

    private const val MAX_ITERATIONS = 50
    private const val TOLERANCE = 1e-12f

    /**
     * Solves Kepler's equation: M = E - e * sin(E)
     * Returns the eccentric anomaly E given mean anomaly M and eccentricity e.
     *
     * Uses Newton-Raphson iteration with optimized initial guess.
     */
    fun solveElliptic(M: Float, e: Float): Float {
        // Normalize M to [0, 2π)
        var meanAnomaly = M % (2f * PI.toFloat())
        if (meanAnomaly < 0) meanAnomaly += 2f * PI.toFloat()

        // Initial guess (Danby's starter)
        var E = if (meanAnomaly < PI.toFloat()) {
            meanAnomaly + e / 2f
        } else {
            meanAnomaly - e / 2f
        }

        // For high eccentricity, use better initial guess
        if (e > 0.8f) {
            E = PI.toFloat()
        }

        // Newton-Raphson iteration
        for (i in 0 until MAX_ITERATIONS) {
            val sinE = sin(E)
            val cosE = cos(E)
            val f = E - e * sinE - meanAnomaly
            val fPrime = 1f - e * cosE

            if (abs(fPrime) < 1e-10f) {
                // Derivative too small, use bisection step
                E = if (f > 0) E - 0.1f else E + 0.1f
                continue
            }

            val delta = f / fPrime

            // Halley's method for faster convergence
            val fDoublePrime = e * sinE
            val deltaHalley = delta / (1f - 0.5f * delta * fDoublePrime / fPrime)

            E -= deltaHalley

            if (abs(deltaHalley) < TOLERANCE) {
                return E
            }
        }

        return E // Return best estimate if not converged
    }

    /**
     * Solves the hyperbolic Kepler equation: M = e * sinh(H) - H
     * Returns the hyperbolic anomaly H given mean anomaly M and eccentricity e.
     */
    fun solveHyperbolic(M: Float, e: Float): Float {
        // Initial guess
        var H = if (abs(M) < 1f) {
            M
        } else {
            sign(M) * ln(2f * abs(M) / e + 1.8f)
        }

        // Newton-Raphson iteration
        for (i in 0 until MAX_ITERATIONS) {
            val sinhH = sinh(H)
            val coshH = cosh(H)
            val f = e * sinhH - H - M
            val fPrime = e * coshH - 1f

            if (abs(fPrime) < 1e-10f) {
                break
            }

            val delta = f / fPrime
            H -= delta

            if (abs(delta) < TOLERANCE) {
                return H
            }
        }

        return H
    }

    /**
     * Convert eccentric anomaly E to true anomaly nu for elliptic orbits.
     */
    fun eccentricToTrue(E: Float, e: Float): Float {
        val beta = e / (1f + sqrt(1f - e * e))
        return E + 2f * atan2(beta * sin(E), 1f - beta * cos(E))
    }

    /**
     * Convert true anomaly nu to eccentric anomaly E for elliptic orbits.
     */
    fun trueToEccentric(nu: Float, e: Float): Float {
        return atan2(
            sqrt(1f - e * e) * sin(nu),
            e + cos(nu)
        )
    }

    /**
     * Convert hyperbolic anomaly H to true anomaly nu.
     */
    fun hyperbolicToTrue(H: Float, e: Float): Float {
        return 2f * atan2(
            sqrt(e + 1f) * sinh(H / 2f),
            sqrt(e - 1f) * cosh(H / 2f)
        )
    }

    /**
     * Compute radius from eccentric anomaly for elliptic orbit.
     */
    fun radiusFromEccentric(a: Float, e: Float, E: Float): Float {
        return a * (1f - e * cos(E))
    }

    /**
     * Compute radius from true anomaly.
     */
    fun radiusFromTrue(a: Float, e: Float, nu: Float): Float {
        val p = a * (1f - e * e) // Semi-latus rectum
        return p / (1f + e * cos(nu))
    }

    private fun sinh(x: Float): Float = (exp(x) - exp(-x)) / 2f
    private fun cosh(x: Float): Float = (exp(x) + exp(-x)) / 2f
}
