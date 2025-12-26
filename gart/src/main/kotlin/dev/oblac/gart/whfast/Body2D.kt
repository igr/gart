package dev.oblac.gart.whfast

import dev.oblac.gart.vector.Vector2
import kotlin.math.sqrt

/**
 * Represents a celestial body in 2D space.
 * Position and velocity are in Jacobi coordinates relative to the center of mass.
 *
 * @param position Position vector (x, y)
 * @param velocity Velocity vector (vx, vy)
 * @param mass Mass of the body (in arbitrary units, typically solar masses)
 * @param name Optional name for identification
 */
data class Body2D(
    val position: Vector2,
    val velocity: Vector2,
    val mass: Float,
    val name: String = ""
) {
    /**
     * Kinetic energy of the body: 0.5 * m * v^2
     */
    val kineticEnergy: Float get() = 0.5f * mass * velocity.dot(velocity)

    /**
     * Distance from origin
     */
    val distanceFromOrigin: Float get() = position.length()

    /**
     * Speed (magnitude of velocity)
     */
    val speed: Float get() = velocity.length()

    /**
     * Returns a new body with updated position
     */
    fun withPosition(newPosition: Vector2) = copy(position = newPosition)

    /**
     * Returns a new body with updated velocity
     */
    fun withVelocity(newVelocity: Vector2) = copy(velocity = newVelocity)

    /**
     * Returns a new body with position offset by delta
     */
    fun offsetPosition(delta: Vector2) = copy(position = position + delta)

    /**
     * Returns a new body with velocity offset by delta (kick)
     */
    fun kick(deltaV: Vector2) = copy(velocity = velocity + deltaV)

    /**
     * Returns a new body drifted by time dt (position updated by velocity * dt)
     */
    fun drift(dt: Float) = copy(position = position + velocity * dt)

    companion object {
        /**
         * Creates a body at rest at the origin
         */
        fun atOrigin(mass: Float, name: String = "") = Body2D(
            position = Vector2.ZERO,
            velocity = Vector2.ZERO,
            mass = mass,
            name = name
        )

        /**
         * Creates a body with circular orbit velocity at given position around central mass.
         * @param position Initial position
         * @param mass Mass of this body
         * @param centralMass Mass of the central body
         * @param prograde True for counter-clockwise orbit, false for clockwise
         */
        fun circularOrbit(
            position: Vector2,
            mass: Float,
            centralMass: Float,
            G: Float = 1f,
            prograde: Boolean = true,
            name: String = ""
        ): Body2D {
            val r = position.length()
            val speed = sqrt(G * centralMass / r)
            // Velocity perpendicular to position vector
            val direction = if (prograde) 1f else -1f
            val velocity = Vector2(-position.y, position.x).normalize() * speed * direction
            return Body2D(position, velocity, mass, name)
        }
    }
}
