package dev.oblac.gart.reactiondiffusion

/**
 * Continuous reaction-diffusion simulation over a rectangular grid.
 *
 * Three built-in models are provided: [GrayScott], [FitzHughNagumo] and
 * [BelousovZhabotinskyContinuous]. Each stores one or more species as
 * `FloatArray(width * height)` with an internal double buffer; [step] advances
 * one iteration and swaps buffers, so no allocation occurs per step.
 *
 * Reaction-diffusion is a mathematical model describing how two chemicals might
 * react to each other as they diffuse through a medium together. It was proposed
 * by Alan Turing in 1952 as a possible explanation for how the interesting patterns
 * of stripes and spots that are seen on the skin/fur of animals like giraffes and leopards form.
 *
 * The reaction-diffusion equations really only describes how the concentrations of the
 * chemicals change over time, which means that all of the interesting patterns and
 * behaviors that we see are emergent phenomena.
 *
 * ## Seeding sources and obstacles
 *
 * Gart models expose direct per-species setters so callers can write any shape:
 *
 * ```kotlin
 * // Seed a circular activator patch (equivalent to addSource)
 * val r = 10
 * for (dy in -r..r) for (dx in -r..r) {
 *     if (dx * dx + dy * dy <= r * r) rd.setV(cx + dx, cy + dy, 1f)
 * }
 *
 * // Enforce an obstacle by re-masking a region after every step
 * fun mask() {
 *     for ((x, y) in obstaclePixels) { rd.setU(x, y, 1f); rd.setV(x, y, 0f) }
 * }
 * repeat(steps) { rd.step(); mask() }
 * ```
 */
interface ReactionDiffusion {
    val width: Int
    val height: Int

    /**
     * Per-step time multiplier. Matches the `passes` uniform in the original
     * GLSL shaders. Changing this value takes effect on the next [step].
     */
    var passes: Float

    /** Advance the simulation by one iteration. */
    fun step()

    /**
     * Scalar value at `(x, y)` used by [dev.oblac.gart.reactiondiffusion.RDColoring]
     * for visualization (range: roughly `0f..1f`).
     */
    fun displayValue(x: Int, y: Int): Float

    /** Restore the model's initial state. */
    fun reset()
}
