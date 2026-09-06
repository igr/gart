package dev.oblac.gart.gfx

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Signed distance from `(x, y)` to a box centred on the origin, half extents [hw] and [hh], with
 * its own radius on each corner: [tl], [tr], [br], [bl] in screen terms (y down, so `tl` is the
 * corner at negative x and negative y). Negative inside, zero on the outline, positive outside,
 * and it is the true euclidean distance, so `sd <= 0f` at pixel centres rasterises the shape and
 * `abs(sd) < w` is a stroke of width `2w`. A radius of `0f` leaves that corner sharp.
 *
 * To place a rotated box, bring the point into the box's own frame first (subtract the centre,
 * rotate by minus its angle) and call this on the result.
 */
fun sdRoundBox(x: Float, y: Float, hw: Float, hh: Float, tl: Float, tr: Float, br: Float, bl: Float): Float {
    val r = when {
        x > 0f && y > 0f -> br
        x > 0f -> tr
        y > 0f -> bl
        else -> tl
    }
    val qx = abs(x) - hw + r
    val qy = abs(y) - hh + r
    return min(max(qx, qy), 0f) + hypot(max(qx, 0f), max(qy, 0f)) - r
}

/** [sdRoundBox] with the same radius [r] on all four corners; `0f` is a plain box. */
fun sdRoundBox(x: Float, y: Float, hw: Float, hh: Float, r: Float): Float = sdRoundBox(x, y, hw, hh, r, r, r, r)
