package dev.oblac.gart.whfast

import dev.oblac.gart.vector.Vector2
import kotlin.math.*

/**
 * Keplerian orbital elements in 2D.
 *
 * @param a Semi-major axis (negative for hyperbolic orbits)
 * @param e Eccentricity (0 = circular, 0 < e < 1 = ellipse, e = 1 = parabola, e > 1 = hyperbola)
 * @param omega Argument of periapsis (longitude of perihelion in 2D)
 * @param M Mean anomaly
 * @param mu Gravitational parameter (G * (M1 + M2))
 */
data class OrbitalElements2D(
    val a: Float,
    val e: Float,
    val omega: Float,
    val M: Float,
    val mu: Float
) {
    /**
     * Orbital period for elliptical orbits
     */
    val period: Float
        get() = if (a > 0 && e < 1) {
            2f * PI.toFloat() * sqrt(a * a * a / mu)
        } else {
            Float.POSITIVE_INFINITY
        }

    /**
     * Mean motion (radians per time unit)
     */
    val n: Float get() = sqrt(mu / abs(a * a * a))

    /**
     * Periapsis distance
     */
    val periapsis: Float get() = a * (1f - e)

    /**
     * Apoapsis distance (only valid for elliptic orbits)
     */
    val apoapsis: Float get() = if (e < 1f) a * (1f + e) else Float.POSITIVE_INFINITY

    /**
     * Specific orbital energy
     */
    val energy: Float get() = -mu / (2f * a)

    /**
     * Specific angular momentum magnitude
     */
    val angularMomentum: Float get() = sqrt(mu * a * abs(1f - e * e))

    companion object {
        /**
         * Create orbital elements from Cartesian state vectors.
         */
        fun fromCartesian(position: Vector2, velocity: Vector2, mu: Float): OrbitalElements2D {
            val r = position.length()
            val v = velocity.length()

            // Specific angular momentum (scalar in 2D, z-component of cross product)
            val h = position.cross(velocity)

            // Eccentricity vector
            val evx = (v * v / mu - 1f / r) * position.x - (position.dot(velocity) / mu) * velocity.x
            val evy = (v * v / mu - 1f / r) * position.y - (position.dot(velocity) / mu) * velocity.y
            val eVec = Vector2(evx, evy)
            val e = eVec.length()

            // Semi-major axis from vis-viva equation
            val a = 1f / (2f / r - v * v / mu)

            // Argument of periapsis
            val omega = if (e > 1e-8f) {
                atan2(eVec.y, eVec.x)
            } else {
                0f // Circular orbit, omega is undefined
            }

            // True anomaly
            val nu = if (e > 1e-8f) {
                val cosNu = eVec.dot(position) / (e * r)
                val sinNu = h / (e * r * sqrt(mu / abs(a * (1f - e * e)))) *
                    (position.dot(velocity) / r)
                atan2(sinNu, cosNu.coerceIn(-1f, 1f))
            } else {
                // Circular orbit: use position angle
                atan2(position.y, position.x)
            }

            // Mean anomaly from true anomaly
            val M = if (e < 1f) {
                // Elliptic orbit
                val E = atan2(
                    sqrt(1f - e * e) * sin(nu),
                    e + cos(nu)
                )
                var meanAnomaly = E - e * sin(E)
                // Normalize to [0, 2π)
                while (meanAnomaly < 0) meanAnomaly += 2f * PI.toFloat()
                while (meanAnomaly >= 2f * PI.toFloat()) meanAnomaly -= 2f * PI.toFloat()
                meanAnomaly
            } else {
                // Hyperbolic orbit
                val sinhH = sqrt(e * e - 1f) * sin(nu) / (1f + e * cos(nu))
                val H = asinh(sinhH)
                e * sinh(H) - H
            }

            return OrbitalElements2D(a, e, omega, M, mu)
        }

        /**
         * Create elements for a circular orbit at given radius.
         */
        fun circular(radius: Float, mu: Float, omega: Float = 0f): OrbitalElements2D {
            return OrbitalElements2D(
                a = radius,
                e = 0f,
                omega = omega,
                M = 0f,
                mu = mu
            )
        }
    }
}

private fun asinh(x: Float): Float = ln(x + sqrt(x * x + 1))
private fun sinh(x: Float): Float = (exp(x) - exp(-x)) / 2f
