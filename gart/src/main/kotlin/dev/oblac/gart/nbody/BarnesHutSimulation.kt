package dev.oblac.gart.nbody

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Barnes-Hut N-body gravity simulation with O(N log N) force computation.
 * Uses quadtree spatial partitioning and KDK (Kick-Drift-Kick) leapfrog integration.
 *
 * @param G Gravitational constant (default: 1)
 * @param theta Barnes-Hut opening angle (0.5 = accurate, 1.0 = fast; default: 0.7)
 * @param softening Softening parameter to avoid singularities (default: 0.01)
 */
class BarnesHutSimulation(
    val G: Float = 1f,
    val theta: Float = 0.7f,
    val softening: Float = 0.01f
) {
    val particles = GravityParticles()
    private val quadTree = QuadTree()

    /** Current simulation time */
    var time: Float = 0f
        private set

    /** Number of particles */
    val count: Int get() = particles.count

    /**
     * Add a particle to the simulation.
     */
    fun addParticle(x: Float, y: Float, vx: Float = 0f, vy: Float = 0f, mass: Float = 1f): Int {
        return particles.add(x, y, vx, vy, mass)
    }

    /**
     * Add particles in a disk distribution (galaxy-like).
     *
     * @param centerX Center X position
     * @param centerY Center Y position
     * @param innerRadius Inner radius of the disk
     * @param outerRadius Outer radius of the disk
     * @param count Number of particles
     * @param totalMass Total mass distributed among particles
     * @param centralMass Mass at center (if > 0, particles orbit this)
     * @param random Random source
     */
    fun addDisk(
        centerX: Float,
        centerY: Float,
        innerRadius: Float,
        outerRadius: Float,
        count: Int,
        totalMass: Float,
        centralMass: Float = 0f,
        random: Random = Random.Default
    ) {
        val particleMass = totalMass / count

        // Add central mass if specified
        if (centralMass > 0f) {
            addParticle(centerX, centerY, 0f, 0f, centralMass)
        }

        val effectiveCentralMass = if (centralMass > 0f) centralMass else totalMass

        for (i in 0 until count) {
            // Random radius with uniform area distribution
            val u = random.nextFloat()
            val r = sqrt(innerRadius * innerRadius + u * (outerRadius * outerRadius - innerRadius * innerRadius))

            // Random angle
            val angle = random.nextFloat() * 2f * kotlin.math.PI.toFloat()

            val x = centerX + r * kotlin.math.cos(angle)
            val y = centerY + r * kotlin.math.sin(angle)

            // Circular velocity for stable orbit
            val orbitalSpeed = sqrt(G * effectiveCentralMass / r)

            // Perpendicular to radius (counter-clockwise)
            val vx = -orbitalSpeed * kotlin.math.sin(angle)
            val vy = orbitalSpeed * kotlin.math.cos(angle)

            // Add some velocity dispersion for realism
            val dispersion = orbitalSpeed * 0.05f
            val vxFinal = vx + (random.nextFloat() - 0.5f) * dispersion
            val vyFinal = vy + (random.nextFloat() - 0.5f) * dispersion

            addParticle(x, y, vxFinal, vyFinal, particleMass)
        }
    }

    /**
     * Add particles in a uniform distribution within a rectangle.
     */
    fun addUniform(
        minX: Float, minY: Float,
        maxX: Float, maxY: Float,
        count: Int,
        totalMass: Float,
        random: Random = Random.Default
    ) {
        val particleMass = totalMass / count
        for (i in 0 until count) {
            val x = minX + random.nextFloat() * (maxX - minX)
            val y = minY + random.nextFloat() * (maxY - minY)
            addParticle(x, y, 0f, 0f, particleMass)
        }
    }

    /**
     * Add particles in a Plummer sphere distribution (2D projection).
     * Good for self-gravitating clusters.
     *
     * @param centerX Center X
     * @param centerY Center Y
     * @param scaleRadius Plummer scale radius
     * @param count Number of particles
     * @param totalMass Total mass
     */
    fun addPlummer(
        centerX: Float,
        centerY: Float,
        scaleRadius: Float,
        count: Int,
        totalMass: Float,
        random: Random = Random.Default
    ) {
        val particleMass = totalMass / count

        for (i in 0 until count) {
            // Plummer distribution: inverse CDF sampling
            val u = random.nextFloat()
            val r = scaleRadius / sqrt((1f / (u * u)) - 1f).coerceAtMost(100f * scaleRadius)

            val angle = random.nextFloat() * 2f * kotlin.math.PI.toFloat()
            val x = centerX + r * kotlin.math.cos(angle)
            val y = centerY + r * kotlin.math.sin(angle)

            // Velocity from Plummer model (simplified)
            val escapeSpeed = sqrt(2f * G * totalMass / sqrt(r * r + scaleRadius * scaleRadius))
            val speed = escapeSpeed * 0.5f * random.nextFloat()
            val vAngle = random.nextFloat() * 2f * kotlin.math.PI.toFloat()
            val vx = speed * kotlin.math.cos(vAngle)
            val vy = speed * kotlin.math.sin(vAngle)

            addParticle(x, y, vx, vy, particleMass)
        }
    }

    /**
     * Perform one integration step using KDK leapfrog.
     *
     * Kick-Drift-Kick scheme:
     * 1. Kick: v += a(x) * dt/2
     * 2. Drift: x += v * dt
     * 3. Rebuild tree + recompute a
     * 4. Kick: v += a(x) * dt/2
     */
    fun step(dt: Float) {
        if (particles.count == 0) return

        val halfDt = dt / 2f

        // First kick (half step)
        computeAccelerations()
        particles.kick(halfDt)

        // Drift (full step)
        particles.drift(dt)

        // Second kick (half step) - need to recompute accelerations at new positions
        computeAccelerations()
        particles.kick(halfDt)

        time += dt
    }

    /**
     * Advance simulation by multiple steps.
     */
    fun advance(dt: Float, steps: Int) {
        repeat(steps) {
            step(dt)
        }
    }

    /**
     * Compute accelerations for all particles using Barnes-Hut.
     */
    private fun computeAccelerations() {
        // Get bounds and build tree
        val bounds = particles.bounds() ?: return
        quadTree.build(particles, bounds)

        // Compute acceleration for each particle
        particles.clearAccelerations()

        for (i in 0 until particles.count) {
            val (ax, ay) = quadTree.computeAcceleration(
                particles.x[i],
                particles.y[i],
                i,
                theta,
                G,
                softening
            )
            particles.ax[i] = ax
            particles.ay[i] = ay
        }
    }

    /**
     * Compute total energy (kinetic + potential).
     * Note: Potential energy computation is O(N²), use sparingly for large N.
     */
    fun totalEnergy(): Float {
        val ke = particles.kineticEnergy()

        // Potential energy (expensive for large N)
        var pe = 0f
        for (i in 0 until particles.count) {
            for (j in i + 1 until particles.count) {
                val dx = particles.x[j] - particles.x[i]
                val dy = particles.y[j] - particles.y[i]
                val dist = sqrt(dx * dx + dy * dy + softening * softening)
                pe -= G * particles.mass[i] * particles.mass[j] / dist
            }
        }

        return ke + pe
    }

    /**
     * Compute approximate total energy using tree (O(N log N)).
     */
    fun approximateTotalEnergy(): Float {
        val ke = particles.kineticEnergy()

        // Approximate potential using tree
        val bounds = particles.bounds() ?: return ke
        quadTree.build(particles, bounds)

        var pe = 0f
        for (i in 0 until particles.count) {
            val (ax, ay) = quadTree.computeAcceleration(
                particles.x[i], particles.y[i], i, theta, G, softening
            )
            // φ ≈ -a·r (rough approximation)
            val r = sqrt(particles.x[i] * particles.x[i] + particles.y[i] * particles.y[i])
            val aMag = sqrt(ax * ax + ay * ay)
            pe -= 0.5f * particles.mass[i] * aMag * r
        }

        return ke + pe
    }

    /**
     * Clear all particles and reset time.
     */
    fun reset() {
        particles.clear()
        time = 0f
    }

    /**
     * Get current quadtree node count (for monitoring).
     */
    val treeNodeCount: Int get() = quadTree.size

    companion object {
        /**
         * Create a two-galaxy collision simulation.
         */
        fun galaxyCollision(
            particlesPerGalaxy: Int = 5000,
            G: Float = 1f,
            theta: Float = 0.7f
        ): BarnesHutSimulation {
            val sim = BarnesHutSimulation(G, theta, softening = 0.1f)

            // Galaxy 1 - left, moving right
            sim.addDisk(
                centerX = -50f, centerY = 0f,
                innerRadius = 2f, outerRadius = 20f,
                count = particlesPerGalaxy,
                totalMass = 100f,
                centralMass = 1000f
            )
            // Give galaxy 1 bulk velocity to the right
            for (i in 0 until particlesPerGalaxy + 1) {
                sim.particles.vx[i] += 2f
            }

            val offset = particlesPerGalaxy + 1

            // Galaxy 2 - right, moving left
            sim.addDisk(
                centerX = 50f, centerY = 10f,
                innerRadius = 2f, outerRadius = 15f,
                count = particlesPerGalaxy,
                totalMass = 80f,
                centralMass = 800f
            )
            // Give galaxy 2 bulk velocity to the left
            for (i in offset until sim.particles.count) {
                sim.particles.vx[i] -= 2f
            }

            return sim
        }

        /**
         * Create a single galaxy simulation.
         */
        fun galaxy(
            particleCount: Int = 10000,
            radius: Float = 50f,
            centralMass: Float = 10000f,
            diskMass: Float = 1000f,
            G: Float = 1f,
            theta: Float = 0.7f
        ): BarnesHutSimulation {
            val sim = BarnesHutSimulation(G, theta, softening = radius / 500f)
            sim.addDisk(
                centerX = 0f, centerY = 0f,
                innerRadius = radius * 0.05f,
                outerRadius = radius,
                count = particleCount,
                totalMass = diskMass,
                centralMass = centralMass
            )
            return sim
        }

        /**
         * Create a gravitational collapse simulation.
         */
        fun collapse(
            particleCount: Int = 10000,
            radius: Float = 50f,
            totalMass: Float = 1000f,
            G: Float = 1f,
            theta: Float = 0.7f
        ): BarnesHutSimulation {
            val sim = BarnesHutSimulation(G, theta, softening = radius / 100f)
            sim.addPlummer(
                centerX = 0f, centerY = 0f,
                scaleRadius = radius,
                count = particleCount,
                totalMass = totalMass
            )
            return sim
        }
    }
}
