package dev.oblac.gart.flow2

import dev.oblac.gart.vector.MutableVec2
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * How a tracer moves one step along a field. By default a tracer uses only the direction of the
 * field, so the step length is always the step it asks for. With `velocity = true` it uses the
 * vector as it is, and the step is a time step.
 */
enum class Integrator {
    /** One sample per step, at its start. Cheap, but it drifts off curves: on a circle it moves out by about 3 px per turn at 1 px steps. */
    EULER,

    /** Two samples per step, the direction at the midpoint. Holds curves well at 1 px steps. */
    RK2,

    /** Four samples per step. For long steps on tight curves. */
    RK4,
}

// below this length a vector has no direction, and a tracer stops there
private const val STILL = 1e-6f

// in direction mode every step asks for the full step length. one that moves less than this
// share of it went nowhere: the samples canceled out, which rk4 does within half a step of a
// sink, and a tracer stops there. with velocity a short step is just a slow place, so there
// only a step that does not move at all counts as going nowhere
private const val NO_PROGRESS = 1e-3f

// the length of (x, y), worked out in double so a big finite vector does not overflow to
// infinity. NaN when a part is not finite
internal fun lengthOf(x: Float, y: Float): Double {
    if (!x.isFinite() || !y.isFinite()) return Double.NaN
    val dx = x.toDouble()
    val dy = y.toDouble()
    return sqrt(dx * dx + dy * dy)
}

// samples the field, scaled to length 1 when [unit], and returns the length it had; 0 where it
// has no direction. the negated test is on purpose, it is true for NaN too
private fun VectorField.sampleAt(x: Float, y: Float, v: MutableVec2, unit: Boolean): Double {
    at(x, y, v)
    val len = lengthOf(v.x, v.y)
    if (!(len > STILL)) return 0.0
    if (unit) v.set((v.x / len).toFloat(), (v.y / len).toFloat())
    return len
}

/**
 * Moves (x, y) one step along the field and writes the new point into [out]. The step is [h] px
 * along the direction, or with [velocity] the vector times [h]. A negative [h] moves against the
 * flow. [v] is scratch space. Returns false, and leaves [out] alone, where a sample has no
 * direction or the step goes nowhere.
 */
internal fun VectorField.advance(
    x: Float,
    y: Float,
    h: Float,
    rk: Integrator,
    v: MutableVec2,
    out: MutableVec2,
    velocity: Boolean = false,
): Boolean {
    val unit = !velocity
    if (sampleAt(x, y, v, unit) == 0.0) return false
    val nx: Float
    val ny: Float
    when (rk) {
        Integrator.EULER -> {
            nx = x + h * v.x
            ny = y + h * v.y
        }

        Integrator.RK2 -> {
            if (sampleAt(x + 0.5f * h * v.x, y + 0.5f * h * v.y, v, unit) == 0.0) return false
            nx = x + h * v.x
            ny = y + h * v.y
        }

        Integrator.RK4 -> {
            val k1x = v.x
            val k1y = v.y
            if (sampleAt(x + 0.5f * h * k1x, y + 0.5f * h * k1y, v, unit) == 0.0) return false
            val k2x = v.x
            val k2y = v.y
            if (sampleAt(x + 0.5f * h * k2x, y + 0.5f * h * k2y, v, unit) == 0.0) return false
            val k3x = v.x
            val k3y = v.y
            if (sampleAt(x + h * k3x, y + h * k3y, v, unit) == 0.0) return false
            // summed in double: with velocity four big samples can pass the largest float
            // before the step scales them down
            nx = (x + h / 6.0 * (k1x.toDouble() + 2.0 * k2x + 2.0 * k3x + v.x)).toFloat()
            ny = (y + h / 6.0 * (k1y.toDouble() + 2.0 * k2y + 2.0 * k3y + v.y)).toFloat()
        }
    }
    if (!nx.isFinite() || !ny.isFinite()) return false
    if (nx == x && ny == y) return false
    if (unit && lengthOf(nx - x, ny - y) < NO_PROGRESS * abs(h)) return false
    out.set(nx, ny)
    return true
}

private fun Rect.holds(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom

/**
 * Follows the field from [start] for up to [steps] steps of [step] px, and returns the path with
 * [start] first. A negative step walks against the flow. The path ends early where the field has
 * no direction (a zero vector, or not a number), where a step goes nowhere (its samples cancel
 * out, as next to a sink), and before a step would leave [bounds].
 *
 * With [velocity] the field is a velocity: a step moves the vector times [step], so the path
 * goes faster where the vector is longer. [step] is then a time step, not a length.
 */
fun VectorField.trace(
    start: Point,
    steps: Int,
    step: Float = 1f,
    rk: Integrator = Integrator.RK2,
    bounds: Rect? = null,
    velocity: Boolean = false,
): List<Point> {
    require(steps >= 0) { "steps must not be negative, got $steps" }
    val path = ArrayList<Point>(steps + 1)
    path.add(start)
    val v = MutableVec2()
    val p = MutableVec2(start.x, start.y)
    for (i in 0 until steps) {
        if (!advance(p.x, p.y, step, rk, v, p, velocity)) break
        if (bounds != null && !bounds.holds(p.x, p.y)) break
        path.add(Point(p.x, p.y))
    }
    return path
}

/**
 * Moves each point one step along the field and returns the points that moved, in their order.
 * Call it once per frame to animate a crowd of points. [onMove] sees each move, from and to,
 * to draw a trail.
 *
 * A point is dropped where the field has no direction or its step goes nowhere, when it is
 * outside [bounds], or when its step would leave them. [step] and [velocity] work as in [trace].
 */
fun VectorField.move(
    points: List<Point>,
    step: Float = 1f,
    rk: Integrator = Integrator.RK2,
    bounds: Rect? = null,
    velocity: Boolean = false,
    onMove: (from: Point, to: Point) -> Unit = { _, _ -> },
): List<Point> {
    val moved = ArrayList<Point>(points.size)
    val v = MutableVec2()
    val p = MutableVec2()
    for (from in points) {
        if (bounds != null && !bounds.holds(from.x, from.y)) continue
        if (!advance(from.x, from.y, step, rk, v, p, velocity)) continue
        if (bounds != null && !bounds.holds(p.x, p.y)) continue
        val to = Point(p.x, p.y)
        onMove(from, to)
        moved.add(to)
    }
    return moved
}
