package dev.oblac.gart.noise

import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simplex noise read round a circle: the value at [angle] on a circle of [radius] about
 * ([ox], [oy]) in noise space. One turn comes back to the same value, so it gives a wobble with no
 * seam round a ring or a closed edge. A bigger [radius] gives more bumps in one turn.
 */
fun ringNoise(angle: Float, radius: Float, ox: Float = 0f, oy: Float = 0f): Float =
    SimplexNoise.noise(cos(angle) * radius + ox, sin(angle) * radius + oy)

fun ringNoise(angle: Float, radius: Float, center: Point): Float = ringNoise(angle, radius, center.x, center.y)
