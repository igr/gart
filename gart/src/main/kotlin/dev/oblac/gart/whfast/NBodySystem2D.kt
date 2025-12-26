package dev.oblac.gart.whfast

import dev.oblac.gart.vector.Vector2
import kotlin.math.PI

/**
 * High-level wrapper for N-body simulation using the Wisdom-Holman integrator.
 * Manages the simulation state and provides convenient methods for setup and evolution.
 *
 * @param G Gravitational constant (default: 1 for normalized units)
 */
class NBodySystem2D(
    val G: Float = 1f
) {
    private val integrator = WHIntegrator2D(G)
    private val _bodies = mutableListOf<Body2D>()

    /** Current time in the simulation */
    var time: Float = 0f
        private set

    /** Read-only list of bodies */
    val bodies: List<Body2D> get() = _bodies.toList()

    /** Number of bodies in the system */
    val size: Int get() = _bodies.size

    /** Total mass of the system */
    val totalMass: Float get() = _bodies.sumOf { it.mass.toDouble() }.toFloat()

    /** Total energy (kinetic + potential) */
    val energy: Float get() = WHIntegrator2D.totalEnergy(_bodies, G)

    /** Total angular momentum (scalar in 2D) */
    val angularMomentum: Float get() = WHIntegrator2D.totalAngularMomentum(_bodies)

    /** Center of mass position */
    val centerOfMass: Vector2 get() = WHIntegrator2D.centerOfMass(_bodies)

    /**
     * Add a central body (star) at the origin.
     */
    fun addCentralBody(mass: Float, name: String = "Star"): NBodySystem2D {
        _bodies.add(0, Body2D.atOrigin(mass, name))
        return this
    }

    /**
     * Add a body with explicit position and velocity.
     */
    fun addBody(
        position: Vector2,
        velocity: Vector2,
        mass: Float,
        name: String = ""
    ): NBodySystem2D {
        _bodies.add(Body2D(position, velocity, mass, name))
        return this
    }

    /**
     * Add a body in a circular orbit at the given distance from the central body.
     */
    fun addCircularOrbit(
        distance: Float,
        mass: Float,
        startAngle: Float = 0f,
        prograde: Boolean = true,
        name: String = ""
    ): NBodySystem2D {
        require(_bodies.isNotEmpty()) { "Add central body first" }

        val centralMass = _bodies[0].mass
        val position = Vector2.of(dev.oblac.gart.angle.Radians(startAngle)) * distance
        val body = Body2D.circularOrbit(position, mass, centralMass, G, prograde, name)

        _bodies.add(body)
        return this
    }

    /**
     * Add a body from orbital elements.
     */
    fun addFromElements(
        elements: OrbitalElements2D,
        mass: Float,
        name: String = ""
    ): NBodySystem2D {
        val (position, velocity) = CoordinateTransform.toCartesian(elements)
        _bodies.add(Body2D(position, velocity, mass, name))
        return this
    }

    /**
     * Advance the simulation by one time step.
     */
    fun step(dt: Float, scheme: WHIntegrator2D.Scheme = WHIntegrator2D.Scheme.DKD) {
        if (_bodies.size < 2) return

        val newBodies = integrator.step(_bodies, dt, scheme)
        _bodies.clear()
        _bodies.addAll(newBodies)
        time += dt
    }

    /**
     * Advance the simulation by multiple time steps.
     */
    fun advance(dt: Float, steps: Int, scheme: WHIntegrator2D.Scheme = WHIntegrator2D.Scheme.DKD) {
        repeat(steps) {
            step(dt, scheme)
        }
    }

    /**
     * Advance until a specific time is reached.
     */
    fun advanceTo(targetTime: Float, dt: Float, scheme: WHIntegrator2D.Scheme = WHIntegrator2D.Scheme.DKD) {
        while (time < targetTime) {
            val actualDt = minOf(dt, targetTime - time)
            step(actualDt, scheme)
        }
    }

    /**
     * Get orbital elements for a specific body (relative to central body).
     */
    fun getOrbitalElements(bodyIndex: Int): OrbitalElements2D? {
        if (bodyIndex <= 0 || bodyIndex >= _bodies.size) return null

        val body = _bodies[bodyIndex]
        val centralMass = _bodies[0].mass
        val mu = G * (centralMass + body.mass)

        return OrbitalElements2D.fromCartesian(body.position, body.velocity, mu)
    }

    /**
     * Clear all bodies and reset time.
     */
    fun reset() {
        _bodies.clear()
        time = 0f
    }

    /**
     * Create a snapshot of the current state.
     */
    fun snapshot(): List<Body2D> = bodies

    /**
     * Restore from a snapshot.
     */
    fun restore(snapshot: List<Body2D>, time: Float = 0f) {
        _bodies.clear()
        _bodies.addAll(snapshot)
        this.time = time
    }

    companion object {
        /**
         * Create an inner solar system simulation (normalized units).
         * Uses units where G=1, Sun mass=1, Earth orbit=1 AU, period=2π.
         */
        fun innerSolarSystem(): NBodySystem2D {
            return NBodySystem2D(G = 1f).apply {
                addCentralBody(1f, "Sun")
                // Mercury
                addCircularOrbit(0.387f, 1.66e-7f, 0f, true, "Mercury")
                // Venus
                addCircularOrbit(0.723f, 2.45e-6f, PI.toFloat() / 3f, true, "Venus")
                // Earth
                addCircularOrbit(1f, 3e-6f, PI.toFloat() * 2f / 3f, true, "Earth")
                // Mars
                addCircularOrbit(1.524f, 3.23e-7f, PI.toFloat(), true, "Mars")
            }
        }

        /**
         * Create a two-body system (useful for testing).
         */
        fun twoBody(
            centralMass: Float,
            orbitingMass: Float,
            distance: Float,
            eccentricity: Float = 0f,
            G: Float = 1f
        ): NBodySystem2D {
            val system = NBodySystem2D(G)
            system.addCentralBody(centralMass, "Central")

            if (eccentricity == 0f) {
                system.addCircularOrbit(distance, orbitingMass, name = "Orbiter")
            } else {
                // Elliptical orbit starting at periapsis
                val a = distance / (1f - eccentricity) // semi-major axis from periapsis
                val mu = G * (centralMass + orbitingMass)
                val elements = OrbitalElements2D(a, eccentricity, 0f, 0f, mu)
                system.addFromElements(elements, orbitingMass, "Orbiter")
            }

            return system
        }

        /**
         * Create a three-body figure-8 orbit (special stable solution).
         */
        fun figureEight(G: Float = 1f): NBodySystem2D {
            // Chenciner-Montgomery figure-8 solution (normalized)
            val x1 = 0.97000436f
            val y1 = -0.24308753f
            val vx3 = -0.93240737f
            val vy3 = -0.86473146f

            return NBodySystem2D(G).apply {
                addBody(Vector2(-x1, y1), Vector2(vx3 / 2f, vy3 / 2f), 1f, "Body1")
                addBody(Vector2(x1, -y1), Vector2(vx3 / 2f, vy3 / 2f), 1f, "Body2")
                addBody(Vector2.ZERO, Vector2(-vx3, -vy3), 1f, "Body3")
            }
        }
    }
}
