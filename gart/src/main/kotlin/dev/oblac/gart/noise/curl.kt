package dev.oblac.gart.noise

import dev.oblac.gart.vector.MutableVec2
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Point

/**
 * Curl of a scalar potential field, by central difference: `v = (dn/dy, -dn/dx)`.
 *
 * The result is divergence free — no sources, no sinks — so anything advected through it keeps
 * moving instead of pooling in a basin or draining to a point. That is what makes curl noise
 * the usual choice for organic-looking flow: a direction field read straight out of noise has
 * both, and particles dropped into it collect in a handful of places within a few hundred steps.
 *
 * [eps] is in the coordinates of [potential], not in pixels, and it is not purely a precision
 * knob. A small step approximates the true derivative and keeps every wrinkle of the potential;
 * a deliberately large one returns a *smoothed* curl, with the fine structure averaged out and
 * only the broad eddies surviving. Both are useful — choose it for the eddy scale you want, and
 * scale the input coordinates for the feature size.
 *
 * @param eps       central-difference half-step, in the potential's own coordinates
 * @param potential scalar field to take the curl of, sampled as `(x, y)`; defaults to
 *                  [SimplexNoise]. Wrap [fbm] for a multi-octave potential.
 */
fun curl(
    p: Point,
    eps: Float = 1f,
    potential: (Float, Float) -> Float = SimplexNoise::noise,
): Vec2 {
    val n1 = potential(p.x, p.y + eps)
    val n2 = potential(p.x, p.y - eps)
    val n3 = potential(p.x + eps, p.y)
    val n4 = potential(p.x - eps, p.y)
    return Vec2((n1 - n2) / (2f * eps), -(n3 - n4) / (2f * eps))
}

/**
 * Curl over raw float coordinates, written into [out].
 *
 * Same field as the [Point] overload, but allocates nothing: that one builds a [Vec2] per call,
 * and a Vec2 costs five allocations rather than one (see [MutableVec2]), which rules it out of
 * a per-pixel or per-step loop. Reach for this one whenever the call sits in a hot path — [out]
 * is written in place and is meant to be reused across calls.
 */
fun curl(
    x: Float,
    y: Float,
    out: MutableVec2,
    eps: Float = 1f,
    potential: (Float, Float) -> Float = SimplexNoise::noise,
) {
    val n1 = potential(x, y + eps)
    val n2 = potential(x, y - eps)
    val n3 = potential(x + eps, y)
    val n4 = potential(x - eps, y)
    out.set((n1 - n2) / (2f * eps), -(n3 - n4) / (2f * eps))
}
