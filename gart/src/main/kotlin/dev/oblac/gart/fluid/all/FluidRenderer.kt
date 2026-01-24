package dev.oblac.gart.fluid.all

/**
 * Renderer for fluid simulation visualization.
 */
class FluidRenderer(
    private val solver: FluidSolver,
    private val particles: FluidParticles,
    private val trailLength: Int = 15,
    private val blockSize: Int = 2
) {
    private val width = solver.width
    private val height = solver.height

    // Trail buffer for particle trails
    private val trailBuffer = FloatArray(width * height)

    /**
     * Render fluid with particle trails.
     */
    fun renderFluid(particleRenderer: ParticleRenderer) {
        // Fade trails
        val fadeIncrement = -1f / trailLength
        trailBuffer.indices.forEach { i ->
            trailBuffer[i] = maxOf(trailBuffer[i] + fadeIncrement, 0f)
        }

        // Add particles to trail buffer
        particles.forEachParticleWithVelocity(solver) { x, y, opacity, _, _ ->
            val px = x.toInt().coerceIn(0, width - 1)
            val py = y.toInt().coerceIn(0, height - 1)
            val idx = py * width + px
            trailBuffer[idx] = minOf(trailBuffer[idx] + opacity, 1f)

            // Draw a small area for each particle for better visibility
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val nx = (px + dx).coerceIn(0, width - 1)
                    val ny = (py + dy).coerceIn(0, height - 1)
                    val nidx = ny * width + nx
                    trailBuffer[nidx] = minOf(trailBuffer[nidx] + opacity * 0.3f, 1f)
                }
            }
        }

        // Render trail buffer
        particleRenderer.clear()

        // Render in blocks for better performance
        for (by in 0 until height step blockSize) {
            for (bx in 0 until width step blockSize) {
                // Sample trail value at block center
                val cx = minOf(bx + blockSize / 2, width - 1)
                val cy = minOf(by + blockSize / 2, height - 1)
                val value = trailBuffer[cy * width + cx]

                if (value > 0.01f) {
                    particleRenderer.renderPixel(bx, by, value, blockSize.toFloat())
                }
            }
        }
    }

    /**
     * Clear the trail buffer.
     */
    fun clearTrails() {
        trailBuffer.fill(0f)
    }
}
