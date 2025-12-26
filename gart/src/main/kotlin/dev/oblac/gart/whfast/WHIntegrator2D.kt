package dev.oblac.gart.whfast

import dev.oblac.gart.vector.Vector2

/**
 * Wisdom-Holman (WHFAST) symplectic integrator for 2D N-body orbital mechanics.
 *
 * The algorithm splits the Hamiltonian into:
 * - H_Kepler: Two-body Keplerian motion around the central mass (solved analytically)
 * - H_interaction: Planet-planet gravitational interactions (applied as velocity kicks)
 *
 * Uses the drift-kick-drift (DKD) or kick-drift-kick (KDK) leapfrog scheme.
 *
 * @param G Gravitational constant
 * @param corrector Apply symplectic corrector for improved accuracy (default: true)
 */
class WHIntegrator2D(
    val G: Float = 1f,
    val corrector: Boolean = true
) {

    /**
     * Integration scheme to use.
     */
    enum class Scheme {
        /** Drift-Kick-Drift: half drift, full kick, half drift */
        DKD,
        /** Kick-Drift-Kick: half kick, full drift, half kick */
        KDK
    }

    /**
     * Perform one integration step.
     *
     * @param bodies List of bodies (first body is the central mass)
     * @param dt Time step
     * @param scheme Integration scheme (DKD or KDK)
     * @return Updated list of bodies
     */
    fun step(
        bodies: List<Body2D>,
        dt: Float,
        scheme: Scheme = Scheme.DKD
    ): List<Body2D> {
        if (bodies.size < 2) return bodies

        return when (scheme) {
            Scheme.DKD -> stepDKD(bodies, dt)
            Scheme.KDK -> stepKDK(bodies, dt)
        }
    }

    /**
     * Drift-Kick-Drift scheme: half drift, full kick, half drift.
     * More commonly used, slightly better energy conservation.
     */
    private fun stepDKD(bodies: List<Body2D>, dt: Float): List<Body2D> {
        val halfDt = dt / 2f

        // Half drift (Keplerian propagation)
        var state = keplerDrift(bodies, halfDt)

        // Full kick (interaction)
        state = interactionKick(state, dt)

        // Half drift
        state = keplerDrift(state, halfDt)

        return state
    }

    /**
     * Kick-Drift-Kick scheme: half kick, full drift, half kick.
     */
    private fun stepKDK(bodies: List<Body2D>, dt: Float): List<Body2D> {
        val halfDt = dt / 2f

        // Half kick
        var state = interactionKick(bodies, halfDt)

        // Full drift
        state = keplerDrift(state, dt)

        // Half kick
        state = interactionKick(state, halfDt)

        return state
    }

    /**
     * Kepler drift: advance each body along its Keplerian orbit around the central mass.
     * This is the analytical solution to the two-body problem.
     */
    private fun keplerDrift(bodies: List<Body2D>, dt: Float): List<Body2D> {
        if (bodies.isEmpty()) return bodies

        val centralMass = bodies[0].mass
        val result = mutableListOf(bodies[0]) // Central body doesn't move

        for (i in 1 until bodies.size) {
            val body = bodies[i]

            // Gravitational parameter for this body orbiting the central mass
            val mu = G * (centralMass + body.mass)

            // Convert to orbital elements
            val elements = OrbitalElements2D.fromCartesian(body.position, body.velocity, mu)

            // Advance mean anomaly
            val advancedElements = CoordinateTransform.advanceOrbit(elements, dt)

            // Convert back to Cartesian
            val (newPos, newVel) = CoordinateTransform.toCartesian(advancedElements)

            result.add(body.copy(position = newPos, velocity = newVel))
        }

        return result
    }

    /**
     * Interaction kick: apply gravitational forces between all body pairs.
     * Updates velocities based on mutual gravitational attraction.
     */
    private fun interactionKick(bodies: List<Body2D>, dt: Float): List<Body2D> {
        val n = bodies.size
        if (n < 2) return bodies

        // Compute accelerations from gravitational interactions
        val accelerations = Array(n) { Vector2.ZERO }

        // Planet-planet interactions (skip central body for self-interaction)
        for (i in 1 until n) {
            for (j in i + 1 until n) {
                val rij = bodies[j].position - bodies[i].position
                val r = rij.length()

                if (r > 1e-10f) {
                    val r3 = r * r * r
                    val direction = rij / r

                    // Gravitational acceleration
                    val accMag_i = G * bodies[j].mass / (r * r)
                    val accMag_j = G * bodies[i].mass / (r * r)

                    accelerations[i] = accelerations[i] + direction * accMag_i
                    accelerations[j] = accelerations[j] - direction * accMag_j
                }
            }
        }

        // Interaction with central body (indirect term for center of mass motion)
        // This accounts for the reflex motion of the central body
        for (i in 1 until n) {
            val r = bodies[i].position
            val rLen = r.length()

            if (rLen > 1e-10f) {
                // Indirect acceleration from planet's pull on the star
                // This term ensures momentum conservation
                val indirectAcc = r * (-G * bodies[i].mass / (rLen * rLen * rLen))
                for (j in 1 until n) {
                    if (j != i) {
                        accelerations[j] = accelerations[j] + indirectAcc
                    }
                }
            }
        }

        // Apply velocity kicks
        return bodies.mapIndexed { i, body ->
            if (i == 0) {
                body // Central body velocity unchanged (in heliocentric coords)
            } else {
                body.kick(accelerations[i] * dt)
            }
        }
    }

    /**
     * Integrate for multiple steps.
     *
     * @param bodies Initial state
     * @param dt Time step per iteration
     * @param steps Number of steps
     * @param scheme Integration scheme
     * @return Final state
     */
    fun integrate(
        bodies: List<Body2D>,
        dt: Float,
        steps: Int,
        scheme: Scheme = Scheme.DKD
    ): List<Body2D> {
        var state = bodies
        repeat(steps) {
            state = step(state, dt, scheme)
        }
        return state
    }

    /**
     * Integrate and collect all intermediate states (for visualization/analysis).
     *
     * @param bodies Initial state
     * @param dt Time step per iteration
     * @param steps Number of steps
     * @param scheme Integration scheme
     * @return List of states at each time step
     */
    fun integrateWithHistory(
        bodies: List<Body2D>,
        dt: Float,
        steps: Int,
        scheme: Scheme = Scheme.DKD
    ): List<List<Body2D>> {
        val history = mutableListOf<List<Body2D>>()
        history.add(bodies)

        var state = bodies
        repeat(steps) {
            state = step(state, dt, scheme)
            history.add(state)
        }
        return history
    }

    companion object {
        /**
         * Compute total energy of the system (kinetic + potential).
         * Useful for checking energy conservation.
         */
        fun totalEnergy(bodies: List<Body2D>, G: Float = 1f): Float {
            var kinetic = 0f
            var potential = 0f

            for (i in bodies.indices) {
                kinetic += bodies[i].kineticEnergy

                for (j in i + 1 until bodies.size) {
                    val r = (bodies[j].position - bodies[i].position).length()
                    if (r > 1e-10f) {
                        potential -= G * bodies[i].mass * bodies[j].mass / r
                    }
                }
            }

            return kinetic + potential
        }

        /**
         * Compute total angular momentum of the system (scalar in 2D).
         */
        fun totalAngularMomentum(bodies: List<Body2D>): Float {
            return bodies.sumOf { body ->
                (body.position.cross(body.velocity) * body.mass).toDouble()
            }.toFloat()
        }

        /**
         * Compute center of mass position.
         */
        fun centerOfMass(bodies: List<Body2D>): Vector2 {
            val totalMass = bodies.sumOf { it.mass.toDouble() }.toFloat()
            var com = Vector2.ZERO
            for (body in bodies) {
                com += body.position * body.mass
            }
            return com / totalMass
        }

        /**
         * Compute center of mass velocity.
         */
        fun centerOfMassVelocity(bodies: List<Body2D>): Vector2 {
            val totalMass = bodies.sumOf { it.mass.toDouble() }.toFloat()
            var comVel = Vector2.ZERO
            for (body in bodies) {
                comVel += body.velocity * body.mass
            }
            return comVel / totalMass
        }
    }
}
