package dev.oblac.gart.march

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A surface `y = height(x, z)`, with y up, for [march] to find along a ray.
 *
 * The footprint `fp` is how wide a ray is where it meets the surface, in world units. A field
 * can drop detail smaller than the footprint, so a far surface does not shimmer. A field can
 * keep scratch memory between calls, so give every thread its own.
 */
interface HeightField {
    /**
     * No point of the surface is higher than this, or lower than its negative.
     */
    val top: Float

    fun height(x: Float, z: Float, fp: Float): Float

    /**
     * A bound on the slope at footprint [fp]. A bound that is too low can step over peaks of any
     * size, one that is too high only makes the march slow. For the guarantee in [march], the bound
     * must also cover the height change that a growing footprint makes over one step.
     */
    fun slope(fp: Float): Float
}

/**
 * Distance along a ray to the first point under the surface, or -1 if there is none before [far].
 *
 * The ray starts at ([ox], [oy], [oz]) and goes along the unit vector ([dx], [dy], [dz]). It
 * enters the slab `|y| <= top` first.
 *
 * The math, in short: the gap is the height of the ray over the surface. Along the ray, the gap
 * cannot decrease faster than `|dy| + slope`: the drop of the ray plus the rise of the surface. If
 * the bound is valid, a step of the gap over this sum stays over the surface. This is sphere
 * tracing for a height field. When the ray is under the surface, each bisection step halves the
 * last interval, and 8 steps find the hit to 1/256 of it.
 *
 * The hit is approximate. Each step is the safe step, the gap over `|dy| + slope`, but never shorter
 * than `max(t * spread / 2, minStep)`. As a result:
 * - If the slope bound holds over the whole step, the march finds each stretch of the ray under the
 *   surface that is longer than that minimum step. The bound must also cover the height change from
 *   the growth of the footprint during the step, not only the slope at one footprint.
 * - The march can miss a thinner peak. Near the camera [minStep] can be longer than the footprint.
 *   Fields can also filter detail below the footprint.
 *
 * [spread] is the angle between two neighboring rays. The footprint at distance t is
 * `t * spread / sqrt(max(|dy|, 0.005))`: a ray that grazes the surface covers a long strip of it.
 *
 * The ray goes in as six floats, not a vector. One object per ray made a full render 45% slower.
 *
 * Reference: Hart, J. C. (1996). Sphere tracing: a geometric method for the antialiased ray tracing
 * of implicit surfaces. The Visual Computer 12(10), 527-545. <https://doi.org/10.1007/s003710050084>
 */
fun HeightField.march(
    ox: Float, oy: Float, oz: Float,
    dx: Float, dy: Float, dz: Float,
    spread: Float, far: Float, minStep: Float = 0.004f,
): Float {
    val fps = footprint(1f, dy, spread) // per unit of distance
    var t = if (oy > top) {
        if (dy >= 0f) return -1f
        (oy - top) / -dy
    } else 0f
    if (t > far) return -1f
    // a start under the surface is a hit, also under the slab where the range below is empty
    var gap = oy + dy * t - height(ox + dx * t, oz + dz * t, t * fps)
    if (gap <= 0f) return t
    // where the ray leaves the slab. a level ray never does
    val exit = when {
        dy < 0f -> (oy + top) / -dy
        dy > 0f -> (top - oy) / dy
        else -> Float.POSITIVE_INFINITY
    }
    val end = min(exit, far)
    if (t >= end) return -1f
    while (t < end) {
        // the last step stops at the end, so no hit past far ever counts
        val tn = min(t + max(gap / (abs(dy) + slope(t * fps)), max(t * spread * 0.5f, minStep)), end)
        val gn = oy + dy * tn - height(ox + dx * tn, oz + dz * tn, tn * fps)
        if (gn <= 0f) return refine(ox, oy, oz, dx, dy, dz, fps, t, tn)
        t = tn
        gap = gn
    }
    return -1f
}

/**
 * How wide a ray with the vertical part [dy] is where it meets a flat surface at distance [t].
 * [spread] is the angle between two neighboring rays. A grazing ray covers a long strip, but the
 * stretch stops growing below `|dy| = 0.005`. [march] uses the same footprint.
 */
fun footprint(t: Float, dy: Float, spread: Float): Float = t * (spread / sqrt(max(abs(dy), 0.005f)))

// bisection between a point over the surface and one under it. its own function, so march stays small for the jit
private fun HeightField.refine(
    ox: Float, oy: Float, oz: Float,
    dx: Float, dy: Float, dz: Float,
    fps: Float, over: Float, under: Float,
): Float {
    var lo = over
    var hi = under
    repeat(8) {
        val m = (lo + hi) * 0.5f
        if (oy + dy * m - height(ox + dx * m, oz + dz * m, m * fps) > 0f) lo = m else hi = m
    }
    return hi
}
