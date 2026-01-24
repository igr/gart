package dev.oblac.gart.fluid.all

import dev.oblac.gart.math.rndi
import org.jetbrains.skia.Point
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Particle system for fluid visualization.
 */
class FluidParticles(
    private val width: Int,
    private val height: Int,
    startingParticles: List<Point>,
    private val lifetime: Int = 1000,
    private val numRenderSteps: Int = 3
) {
    val numParticles: Int = startingParticles.size

    // Particle positions (x, y, displacementX, displacementY)
    private val positionsX = FloatArray(numParticles)
    private val positionsY = FloatArray(numParticles)
    private val displacementsX = FloatArray(numParticles)
    private val displacementsY = FloatArray(numParticles)

    // Initial positions for resetting particles
    private val initialPositionsX = FloatArray(numParticles)
    private val initialPositionsY = FloatArray(numParticles)

    // Particle ages
    private val ages = IntArray(numParticles)

    init {
        startingParticles.forEachIndexed { i, point ->
            positionsX[i] = point.x
            positionsY[i] = point.y
            displacementsX[i] = 0f
            displacementsY[i] = 0f

            initialPositionsX[i] = point.x
            initialPositionsY[i] = point.y

            ages[i] = rndi(lifetime)
        }
    }

    /**
     * Update particles based on velocity field.
     * Performs multiple sub-steps for smoother trails.
     */
    fun update(solver: FluidSolver) {
        // Age particles
        ages.indices.forEach { i ->
            ages[i] = (ages[i] + 1) % lifetime
        }

        // Advect particles
        val dt = 1f / numRenderSteps
        repeat(numRenderSteps) {
            for (i in 0 until numParticles) {
                // Check if particle should be reset
                if (ages[i] == 0) {
                    positionsX[i] = initialPositionsX[i]
                    positionsY[i] = initialPositionsY[i]
                    displacementsX[i] = 0f
                    displacementsY[i] = 0f
                    continue
                }

                // Get current position
                val x = positionsX[i] + displacementsX[i]
                val y = positionsY[i] + displacementsY[i]

                // RK2 integration for smoother advection
                val (vel1x, vel1y) = solver.velocityAt(x, y)
                val halfStepX = x + vel1x * 0.5f * dt
                val halfStepY = y + vel1y * 0.5f * dt
                val (vel2x, vel2y) = solver.velocityAt(halfStepX, halfStepY)

                // Update displacement
                displacementsX[i] += vel2x * dt
                displacementsY[i] += vel2y * dt

                // Merge displacement with position if needed
                val dispMagSq = displacementsX[i] * displacementsX[i] + displacementsY[i] * displacementsY[i]
                if (dispMagSq > DISPLACEMENT_THRESHOLD_SQ) {
                    positionsX[i] = (positionsX[i] + displacementsX[i]).wrapWidth()
                    positionsY[i] = (positionsY[i] + displacementsY[i]).wrapHeight()
                    displacementsX[i] = 0f
                    displacementsY[i] = 0f
                }
            }
        }
    }

    /**
     * Get particle data for rendering.
     */
    fun forEachParticle(action: (x: Float, y: Float, age: Int, opacity: Float, velocityMag: Float) -> Unit) {
        repeat(numParticles) { i ->
            val x = (positionsX[i] + displacementsX[i]).wrapWidth()
            val y = (positionsY[i] + displacementsY[i]).wrapHeight()
            val age = ages[i]
            val opacity = calculateOpacity(age)

            action(x, y, age, opacity, 0f)
        }
    }

    /**
     * Get particle position and velocity for rendering.
     */
    fun forEachParticleWithVelocity(solver: FluidSolver, action: (x: Float, y: Float, opacity: Float, velX: Float, velY: Float) -> Unit) {
        repeat(numParticles) { i ->
            val x = (positionsX[i] + displacementsX[i]).wrapWidth()
            val y = (positionsY[i] + displacementsY[i]).wrapHeight()
            val opacity = calculateOpacity(ages[i])

            val (velX, velY) = solver.velocityAt(x, y)
            val velMag = sqrt(velX * velX + velY * velY)
            val multiplier = (velMag * velMag * 0.05f + 0.7f).coerceIn(0f, 1f)

            action(x, y, opacity * multiplier, velX, velY)
        }
    }

    /**
     * Reset particles to initial state.
     */
    fun reset() {
        repeat(numParticles) { i ->
            positionsX[i] = initialPositionsX[i]
            positionsY[i] = initialPositionsY[i]
            displacementsX[i] = 0f
            displacementsY[i] = 0f
            ages[i] = rndi(lifetime)
        }
    }

    /**
     * Particle count.
     */
    fun count(): Int = numParticles

    private fun calculateOpacity(age: Int): Float {
        val ageFraction = age.toFloat() / lifetime
        return min(ageFraction * 10f, 1f) * (1f - maxOf(ageFraction * 10f - 9f, 0f))
    }

    private fun Float.wrapWidth() = ((this % width) + width) % width
    private fun Float.wrapHeight() = ((this % height) + height) % height

    private companion object {
        const val DISPLACEMENT_THRESHOLD_SQ = 400f  // threshold of 20^2
    }
}
