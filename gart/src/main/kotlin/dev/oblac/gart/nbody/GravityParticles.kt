package dev.oblac.gart.nbody

import kotlin.math.max
import kotlin.math.min

/**
 * Structure-of-Arrays container for gravity particles.
 * Optimized for cache-friendly access patterns and large particle counts (10^6+).
 *
 * @param capacity Initial capacity (will grow automatically)
 */
class GravityParticles(capacity: Int = 1024) {

    private var _capacity = capacity
    private var _count = 0

    // Position arrays
    var x = FloatArray(_capacity)
        private set
    var y = FloatArray(_capacity)
        private set

    // Velocity arrays
    var vx = FloatArray(_capacity)
        private set
    var vy = FloatArray(_capacity)
        private set

    // Acceleration arrays (computed each step)
    var ax = FloatArray(_capacity)
        private set
    var ay = FloatArray(_capacity)
        private set

    // Mass array
    var mass = FloatArray(_capacity)
        private set

    /** Number of particles */
    val count: Int get() = _count

    /** Current capacity */
    val capacity: Int get() = _capacity

    /**
     * Add a particle with given properties.
     * @return Index of the added particle
     */
    fun add(px: Float, py: Float, pvx: Float, pvy: Float, m: Float): Int {
        ensureCapacity(_count + 1)
        val idx = _count
        x[idx] = px
        y[idx] = py
        vx[idx] = pvx
        vy[idx] = pvy
        ax[idx] = 0f
        ay[idx] = 0f
        mass[idx] = m
        _count++
        return idx
    }

    /**
     * Add a particle at position with zero velocity.
     */
    fun add(px: Float, py: Float, m: Float): Int = add(px, py, 0f, 0f, m)

    /**
     * Add multiple particles from arrays.
     */
    fun addAll(
        px: FloatArray, py: FloatArray,
        pvx: FloatArray, pvy: FloatArray,
        m: FloatArray
    ) {
        require(
            px.size == py.size && px.size == pvx.size &&
                px.size == pvy.size && px.size == m.size
        ) {
            "All arrays must have the same size"
        }
        ensureCapacity(_count + px.size)
        for (i in px.indices) {
            x[_count] = px[i]
            y[_count] = py[i]
            vx[_count] = pvx[i]
            vy[_count] = pvy[i]
            ax[_count] = 0f
            ay[_count] = 0f
            mass[_count] = m[i]
            _count++
        }
    }

    /**
     * Clear all particles.
     */
    fun clear() {
        _count = 0
    }

    /**
     * Compute bounding box of all particles.
     * @return Pair of (minX, minY) to (maxX, maxY), or null if empty
     */
    fun bounds(): BoundingBox? {
        if (_count == 0) return null

        var minX = x[0]
        var maxX = x[0]
        var minY = y[0]
        var maxY = y[0]

        for (i in 1 until _count) {
            minX = min(minX, x[i])
            maxX = max(maxX, x[i])
            minY = min(minY, y[i])
            maxY = max(maxY, y[i])
        }

        return BoundingBox(minX, minY, maxX, maxY)
    }

    /**
     * Compute center of mass.
     */
    fun centerOfMass(): Pair<Float, Float> {
        if (_count == 0) return Pair(0f, 0f)

        var totalMass = 0f
        var comX = 0f
        var comY = 0f

        for (i in 0 until _count) {
            totalMass += mass[i]
            comX += x[i] * mass[i]
            comY += y[i] * mass[i]
        }

        return if (totalMass > 0f) {
            Pair(comX / totalMass, comY / totalMass)
        } else {
            Pair(0f, 0f)
        }
    }

    /**
     * Compute total kinetic energy.
     */
    fun kineticEnergy(): Float {
        var ke = 0f
        for (i in 0 until _count) {
            ke += 0.5f * mass[i] * (vx[i] * vx[i] + vy[i] * vy[i])
        }
        return ke
    }

    /**
     * Zero all accelerations (called before force computation).
     */
    fun clearAccelerations() {
        for (i in 0 until _count) {
            ax[i] = 0f
            ay[i] = 0f
        }
    }

    /**
     * Apply velocity kick: v += a * dt
     */
    fun kick(dt: Float) {
        for (i in 0 until _count) {
            vx[i] += ax[i] * dt
            vy[i] += ay[i] * dt
        }
    }

    /**
     * Apply position drift: x += v * dt
     */
    fun drift(dt: Float) {
        for (i in 0 until _count) {
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
        }
    }

    private fun ensureCapacity(required: Int) {
        if (required <= _capacity) return

        val newCapacity = maxOf(required, _capacity * 2)
        x = x.copyOf(newCapacity)
        y = y.copyOf(newCapacity)
        vx = vx.copyOf(newCapacity)
        vy = vy.copyOf(newCapacity)
        ax = ax.copyOf(newCapacity)
        ay = ay.copyOf(newCapacity)
        mass = mass.copyOf(newCapacity)
        _capacity = newCapacity
    }

    /**
     * Iterate over all particles.
     */
    inline fun forEach(action: (index: Int, x: Float, y: Float, vx: Float, vy: Float, mass: Float) -> Unit) {
        for (i in 0 until count) {
            action(i, x[i], y[i], vx[i], vy[i], mass[i])
        }
    }
}

/**
 * Axis-aligned bounding box.
 */
data class BoundingBox(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f

    /**
     * Expand to a square (for quadtree).
     */
    fun toSquare(): BoundingBox {
        val size = max(width, height)
        val cx = centerX
        val cy = centerY
        val halfSize = size / 2f
        return BoundingBox(
            cx - halfSize, cy - halfSize,
            cx + halfSize, cy + halfSize
        )
    }

    /**
     * Expand bounds by a margin.
     */
    fun expand(margin: Float): BoundingBox {
        return BoundingBox(
            minX - margin, minY - margin,
            maxX + margin, maxY + margin
        )
    }

    /**
     * Check if point is inside.
     */
    fun contains(px: Float, py: Float): Boolean {
        return px >= minX && px <= maxX && py >= minY && py <= maxY
    }
}
