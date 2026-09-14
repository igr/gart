package dev.oblac.gart.marbling

import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.vector.MutableVec2
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** One drop's rim after everything that happened to it: a closed polygon, [points] in order round the rim. */
class Contour(val color: Int, val points: List<Point>) {
    val path: Path by lazy { closedPathOf(points) }
}

/**
 * The drops as vector outlines, in placement order - draw them in order and the later ones cover
 * the earlier, same as the raster. Each rim starts as its circle, sampled no coarser than [step]
 * and no coarser than the sharpest tine that comes after the drop (its `c`), and is carried
 * forward through every op after the drop. Every stretch between two rim points is checked at
 * its middle (on the original circle, carried through the same ops): if either half is longer
 * than [step] the stretch is split and both halves checked again, so a stretched rim fills in
 * with real rim points and never with a chord, and a tongue a sharp tine pulled out from between
 * two samples that landed close together is still found. A tongue narrower than the rim sampling
 * in original-circle terms can still slip through when a sharp tine follows heavy stretching -
 * lower [step] then. [maxPoints] is a hard cap on the points of one rim: past it a wound-up rim
 * stops splitting and closes with what it has.
 */
fun Marbling.contours(step: Float = 1.5f, maxPoints: Int = 100_000): List<Contour> {
    require(step > 0f) { "step must be positive" }
    require(maxPoints >= 16) { "maxPoints must be at least 16" }
    val ops = this.ops
    val cursor = MutableVec2()
    val out = ArrayList<Contour>()
    for ((k, op) in ops.withIndex()) {
        if (op !is Drop) continue
        var spacing = step
        for (i in k + 1 until ops.size) {
            val later = ops[i]
            if (later is Comb) spacing = min(spacing, later.c)
            if (later is Whirl) spacing = min(spacing, later.c)
        }
        out += Contour(op.color, Rim(op, ops, k + 1, cursor).trace(step, spacing, maxPoints))
    }
    return out
}

private class Rim(val drop: Drop, val ops: List<MarblingOp>, val from: Int, val p: MutableVec2) {

    fun at(theta: Float): Point {
        p.set(drop.cx + drop.r * cos(theta), drop.cy + drop.r * sin(theta))
        for (i in from until ops.size) ops[i].forward(p)
        return Point(p.x, p.y)
    }

    fun trace(step: Float, spacing: Float, maxPoints: Int): List<Point> {
        val n0 = ceil(TAUf * drop.r / spacing).toInt().coerceIn(16, maxPoints)
        val step2 = step * step
        val flat2 = step2 / 16f // the middle may sit this far off the chord's middle and still be "on it"
        val pts = ArrayList<Point>(n0 * 2)
        // every split and every extra middle costs one point of this, so the rim never passes the cap
        var budget = maxPoints - n0

        fun dist2(a: Point, b: Point): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            return dx * dx + dy * dy
        }

        // emits the start of every leaf stretch (and its middle, unless the stretch is short and
        // straight), so the ring closes without a repeated point
        fun refine(t0: Float, p0: Point, t1: Float, p1: Point, depth: Int) {
            val tm = (t0 + t1) / 2f
            val pm = at(tm)
            val short = dist2(p0, pm) <= step2 && dist2(pm, p1) <= step2
            if (!short && depth < 20 && budget > 0) {
                budget--
                refine(t0, p0, tm, pm, depth + 1)
                refine(tm, pm, t1, p1, depth + 1)
                return
            }
            pts += p0
            val mid = Point((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
            if (budget > 0 && (!short || dist2(p0, p1) > step2 || dist2(pm, mid) > flat2)) {
                pts += pm
                budget--
            }
        }

        val thetas = FloatArray(n0 + 1) { it * TAUf / n0 }
        val ring = Array(n0 + 1) { at(thetas[it]) }
        for (i in 0 until n0) refine(thetas[i], ring[i], thetas[i + 1], ring[i + 1], 0)
        return pts
    }
}
