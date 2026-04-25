package dev.oblac.gart.reactiondiffusion

import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.ColorRamp

/**
 * Render the simulation's display value into a [Gartmap] through the given
 * [coloring] and flush to its backing canvas.
 *
 * The [Gartmap] dimensions must match `width`/`height` of the simulation.
 * Resizing is out of scope — render at the simulation size, then draw the
 * resulting canvas scaled.
 *
 * Usage:
 * ```kotlin
 * val g = gart.gartvas()
 * val map = gart.gartmap(g)
 * val rd = GrayScott(g.d.w, g.d.h)
 * repeat(steps) { rd.step() }
 * rd.renderTo(map)
 * ```
 */
fun ReactionDiffusion.renderTo(
    map: Gartmap,
    coloring: ColorRamp = ColorRamp.Default,
) {
    require(map.w == width && map.h == height) {
        "Gartmap (${map.w}x${map.h}) must match reaction-diffusion grid (${width}x$height)"
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            map[x, y] = coloring.colorAt(displayValue(x, y))
        }
    }
    map.drawToCanvas()
}
