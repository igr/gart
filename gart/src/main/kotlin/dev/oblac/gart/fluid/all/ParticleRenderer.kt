package dev.oblac.gart.fluid.all

/**
 * Interface for particle rendering strategies.
 */
interface ParticleRenderer {
    /**
     * Called before rendering to initialize/clear the canvas.
     */
    fun clear()

    /**
     * Render a single particle block.
     * @param x X position
     * @param y Y position
     * @param value Trail intensity from 0 (none) to 1 (full)
     * @param blockSize Size of the rendering block
     */
    fun renderPixel(x: Int, y: Int, value: Float, blockSize: Float)
}
