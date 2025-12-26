package dev.oblac.gart.whfast

import dev.oblac.gart.vector.Vector2
import kotlin.math.*

/**
 * Coordinate transformations for orbital mechanics in 2D.
 * Handles conversions between Cartesian state vectors and Keplerian orbital elements.
 */
object CoordinateTransform {

    /**
     * Convert Keplerian orbital elements to Cartesian state vectors.
     * Returns Pair<position, velocity>.
     */
    fun toCartesian(elements: OrbitalElements2D): Pair<Vector2, Vector2> {
        val (a, e, omega, M, mu) = elements

        if (e < 1f) {
            return ellipticToCartesian(a, e, omega, M, mu)
        } else {
            return hyperbolicToCartesian(a, e, omega, M, mu)
        }
    }

    private fun ellipticToCartesian(
        a: Float, e: Float, omega: Float, M: Float, mu: Float
    ): Pair<Vector2, Vector2> {
        // Solve Kepler's equation for eccentric anomaly
        val E = KeplerSolver.solveElliptic(M, e)

        // Position in orbital plane (perifocal coordinates)
        val cosE = cos(E)
        val sinE = sin(E)

        val x_orb = a * (cosE - e)
        val y_orb = a * sqrt(1f - e * e) * sinE

        // Velocity in orbital plane
        val r = a * (1f - e * cosE)
        val n = sqrt(mu / (a * a * a))  // Mean motion

        val vx_orb = -a * n * sinE / (1f - e * cosE) * r / a
        val vy_orb = a * n * sqrt(1f - e * e) * cosE / (1f - e * cosE) * r / a

        // Simplified velocity calculation
        val factor = sqrt(mu * a) / r
        val vx_orbital = -factor * sinE
        val vy_orbital = factor * sqrt(1f - e * e) * cosE

        // Rotate to inertial frame
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)

        val x = x_orb * cosOmega - y_orb * sinOmega
        val y = x_orb * sinOmega + y_orb * cosOmega

        val vx = vx_orbital * cosOmega - vy_orbital * sinOmega
        val vy = vx_orbital * sinOmega + vy_orbital * cosOmega

        return Pair(Vector2(x, y), Vector2(vx, vy))
    }

    private fun hyperbolicToCartesian(
        a: Float, e: Float, omega: Float, M: Float, mu: Float
    ): Pair<Vector2, Vector2> {
        // For hyperbolic orbits, a is negative
        val aAbs = abs(a)

        // Solve hyperbolic Kepler's equation
        val H = KeplerSolver.solveHyperbolic(M, e)

        val coshH = cosh(H)
        val sinhH = sinh(H)

        // Position in orbital plane
        val x_orb = aAbs * (e - coshH)
        val y_orb = aAbs * sqrt(e * e - 1f) * sinhH

        // Velocity in orbital plane
        val r = aAbs * (e * coshH - 1f)
        val factor = sqrt(mu * aAbs) / r

        val vx_orbital = -factor * sinhH
        val vy_orbital = factor * sqrt(e * e - 1f) * coshH

        // Rotate to inertial frame
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)

        val x = x_orb * cosOmega - y_orb * sinOmega
        val y = x_orb * sinOmega + y_orb * cosOmega

        val vx = vx_orbital * cosOmega - vy_orbital * sinOmega
        val vy = vx_orbital * sinOmega + vy_orbital * cosOmega

        return Pair(Vector2(x, y), Vector2(vx, vy))
    }

    /**
     * Convert Cartesian state vectors to Keplerian orbital elements.
     */
    fun toOrbitalElements(position: Vector2, velocity: Vector2, mu: Float): OrbitalElements2D {
        return OrbitalElements2D.fromCartesian(position, velocity, mu)
    }

    /**
     * Advance orbital elements by time dt (analytical Kepler propagation).
     * This is the "drift" step in the Wisdom-Holman integrator.
     */
    fun advanceOrbit(elements: OrbitalElements2D, dt: Float): OrbitalElements2D {
        // Mean motion
        val n = sqrt(elements.mu / abs(elements.a * elements.a * elements.a))

        // Advance mean anomaly
        val newM = elements.M + n * dt

        return elements.copy(M = newM)
    }

    /**
     * Convert from heliocentric to Jacobi coordinates.
     * Jacobi coordinates use the center of mass of all interior bodies.
     */
    fun toJacobi(bodies: List<Body2D>): List<Body2D> {
        if (bodies.isEmpty()) return emptyList()

        val result = mutableListOf<Body2D>()

        // First body stays at origin (central body)
        result.add(bodies[0].copy(position = Vector2.ZERO, velocity = Vector2.ZERO))

        // Cumulative mass and center of mass
        var totalMass = bodies[0].mass
        var comPosition = bodies[0].position * bodies[0].mass
        var comVelocity = bodies[0].velocity * bodies[0].mass

        for (i in 1 until bodies.size) {
            val body = bodies[i]

            // Position relative to center of mass of all interior bodies
            val comPos = comPosition / totalMass
            val comVel = comVelocity / totalMass

            val jacobiPos = body.position - comPos
            val jacobiVel = body.velocity - comVel

            result.add(body.copy(position = jacobiPos, velocity = jacobiVel))

            // Update center of mass
            totalMass += body.mass
            comPosition = comPosition + body.position * body.mass
            comVelocity = comVelocity + body.velocity * body.mass
        }

        return result
    }

    /**
     * Convert from Jacobi to heliocentric coordinates.
     */
    fun fromJacobi(jacobiBodies: List<Body2D>): List<Body2D> {
        if (jacobiBodies.isEmpty()) return emptyList()

        val result = mutableListOf<Body2D>()

        // First body at origin
        result.add(jacobiBodies[0])

        var totalMass = jacobiBodies[0].mass
        var comPosition = jacobiBodies[0].position * jacobiBodies[0].mass
        var comVelocity = jacobiBodies[0].velocity * jacobiBodies[0].mass

        for (i in 1 until jacobiBodies.size) {
            val jacobiBody = jacobiBodies[i]

            // Convert back to heliocentric
            val comPos = comPosition / totalMass
            val comVel = comVelocity / totalMass

            val helioPos = jacobiBody.position + comPos
            val helioVel = jacobiBody.velocity + comVel

            val body = jacobiBody.copy(position = helioPos, velocity = helioVel)
            result.add(body)

            // Update center of mass with the converted position
            totalMass += body.mass
            comPosition = comPosition + body.position * body.mass
            comVelocity = comVelocity + body.velocity * body.mass
        }

        return result
    }

    private fun sinh(x: Float): Float = (exp(x) - exp(-x)) / 2f
    private fun cosh(x: Float): Float = (exp(x) + exp(-x)) / 2f
}
