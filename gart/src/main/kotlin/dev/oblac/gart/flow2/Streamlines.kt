package dev.oblac.gart.flow2

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.toPath
import dev.oblac.gart.vector.MutableVec2
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * One line from [streamlines]: its [points] in order along the flow, and for each point its
 * [clearance], the distance to the nearest other line, capped at the separation. [taper] turns
 * the clearance into a width factor, to thin a line where it closes in on another one.
 */
class Streamline internal constructor(
    val points: List<Point>,
    val clearance: FloatArray,
    private val dSep: Float,
    private val dTest: Float,
) {

    /**
     * A width factor for point [i]: 1 in the open, down to 0 where the line stopped next to another
     * one. It is `(clearance - dTest) / (dSep - dTest)`, kept in 0..1. When dTest is dSep no line
     * closes in on another, so it is 1 everywhere.
     */
    fun taper(i: Int): Float = if (dSep > dTest) ((clearance[i] - dTest) / (dSep - dTest)).coerceIn(0f, 1f) else 1f

    /** The length of the line, in px. */
    val length: Float = run {
        var sum = 0f
        for (i in 1 until points.size) sum += dist(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y)
        sum
    }

    fun toPath(): Path = points.toPath()
}

/**
 * Evenly spaced streamlines of this field over the canvas [d].
 *
 * Each line grows from a seed that is at least [dSep] from all lines so far. It grows both ways,
 * with the flow and against it, until it comes closer than [dTest] to another line or to an
 * earlier part of itself, leaves the canvas plus [margin], reaches a point with no direction, or
 * reaches [maxLength]. Seeds for the next lines go [dSep] to both sides of each point of each
 * new line, so new lines grow next to the old ones. When no seed like that is left, one more
 * pass tries seeds on a [dSep] grid over the whole area, so no open part stays empty.
 *
 * - [dTest] less than [dSep] makes the lines longer, because a line can close in on another one
 *   before it stops. Half of [dSep] gives long lines. Near [dSep], the lines are shorter and
 *   the gaps are more even.
 * - [step] is the length of one step, in px. It must not be more than [dTest], or a line can
 *   jump across another one.
 * - A line shorter than [minLength] is dropped, and its space is free again.
 * - [rnd] picks the next seed, so the same random seed gives the same lines.
 */
fun VectorField.streamlines(
    d: Dimension,
    rnd: Random,
    dSep: Float,
    dTest: Float = dSep * 0.5f,
    step: Float = 1f,
    minLength: Float = dSep,
    maxLength: Float = Float.POSITIVE_INFINITY,
    margin: Float = 0f,
    rk: Integrator = Integrator.RK2,
): List<Streamline> {
    require(dSep > 0f) { "dSep must be positive, got $dSep" }
    require(dTest > 0f && dTest <= dSep) { "dTest must be more than 0 and not more than dSep ($dSep), got $dTest" }
    require(step > 0f && step <= dTest) { "step must be more than 0 and not more than dTest ($dTest), got $step" }
    require(margin >= 0f) { "margin must not be negative, got $margin" }
    require(minLength <= maxLength) { "minLength ($minLength) must not be more than maxLength ($maxLength)" }
    return Placer(this, rnd, dSep, dTest, step, minLength, maxLength, rk, -margin, -margin, d.wf + margin, d.hf + margin).place()
}

// a seed dSep away from its own line has to pass the dSep test, this gives it 1% of room
private const val SEED_ROOM = 0.99f

// a line ignores its own points closer than this many dTest along it, else every bend would stop it
private const val SELF_SKIP = 2f

// a guard against a field that keeps a line going for ever. the self test should stop it first
private const val MAX_STEPS = 1 shl 20

private fun dist(x0: Float, y0: Float, x1: Float, y1: Float): Float {
    val dx = x1 - x0
    val dy = y1 - y0
    return sqrt(dx * dx + dy * dy)
}

// the state of one streamlines() call
private class Placer(
    private val field: VectorField,
    private val rnd: Random,
    private val dSep: Float,
    private val dTest: Float,
    private val step: Float,
    private val minLength: Float,
    private val maxLength: Float,
    private val rk: Integrator,
    private val left: Float,
    private val top: Float,
    private val right: Float,
    private val bottom: Float,
) {
    private val grid = SpacingGrid(left, top, right, bottom, dSep)

    // a seed is also the first point of its line, so it has to keep dTest too
    private val seedRoom = max(dTest, dSep * SEED_ROOM)

    // every point of every line so far, by id. arc is the signed distance from the seed along
    // the line, plus with the flow and minus against it, so two points of one line are
    // |arc1 - arc2| apart along it
    private val xs = FloatList()
    private val ys = FloatList()
    private val arcs = FloatList()
    private val owners = IntList()

    private val seeds = FloatList()  // x, y pairs
    private val lines = ArrayList<IntArray>()

    private val v = MutableVec2()
    private val p = MutableVec2()

    fun place(): List<Streamline> {
        val cover = coverSeeds()
        var next = 0
        while (true) {
            val sx: Float
            val sy: Float
            val n = seeds.size / 2
            if (n > 0) {
                val k = rnd.nextInt(n)
                sx = seeds[2 * k]
                sy = seeds[2 * k + 1]
                seeds[2 * k] = seeds[2 * n - 2]
                seeds[2 * k + 1] = seeds[2 * n - 1]
                seeds.size -= 2
            } else if (next < cover.size) {
                sx = cover[next]
                sy = cover[next + 1]
                next += 2
            } else {
                break
            }
            if (inside(sx, sy) && isFree(sx, sy, seedRoom, -1, 0f)) grow(sx, sy)
        }
        return lines.mapIndexed { line, ids ->
            Streamline(ids.map { Point(xs[it], ys[it]) }, FloatArray(ids.size) { clearance(ids[it], line) }, dSep, dTest)
        }
    }

    // the middles of a dSep grid over the area, in random order. a cell that hangs over the far
    // edge gets the middle of its part inside, else a thin area gets no seed at all
    private fun coverSeeds(): FloatArray {
        val cols = ceil((right - left) / dSep).toInt()
        val rows = ceil((bottom - top) / dSep).toInt()
        val order = IntArray(cols * rows) { it }
        order.shuffle(rnd)
        val out = FloatArray(order.size * 2)
        for (k in order.indices) {
            val x0 = left + (order[k] % cols) * dSep
            val y0 = top + (order[k] / cols) * dSep
            out[2 * k] = (x0 + min(x0 + dSep, right)) / 2f
            out[2 * k + 1] = (y0 + min(y0 + dSep, bottom)) / 2f
        }
        return out
    }

    private fun grow(sx: Float, sy: Float) {
        val line = lines.size
        val first = xs.size
        add(sx, sy, 0f, line)
        val ahead = walk(sx, sy, step, line, maxLength)
        val mid = xs.size
        val behind = walk(sx, sy, -step, line, maxLength - ahead)
        val end = xs.size
        if (end - first < 2 || ahead + behind < minLength) {
            for (id in end - 1 downTo first) grid.removeLast(xs[id], ys[id])
            xs.size = first
            ys.size = first
            arcs.size = first
            owners.size = first
            return
        }
        // along the flow: the backward points reversed, then the seed and the forward points
        val ids = IntArray(end - first)
        var k = 0
        for (id in end - 1 downTo mid) ids[k++] = id
        for (id in first until mid) ids[k++] = id
        lines.add(ids)
        seedAlong(ids)
    }

    // adds points from the seed one way while there is room, returns the length it walked
    private fun walk(sx: Float, sy: Float, h: Float, line: Int, budget: Float): Float {
        var x = sx
        var y = sy
        var walked = 0f
        for (n in 0 until MAX_STEPS) {
            if (!field.advance(x, y, h, rk, v, p)) break
            val len = dist(x, y, p.x, p.y)
            if (walked + len > budget) break
            val arc = if (h > 0f) walked + len else -(walked + len)
            if (!inside(p.x, p.y) || !isFree(p.x, p.y, dTest, line, arc)) break
            walked += len
            x = p.x
            y = p.y
            add(x, y, arc, line)
        }
        return walked
    }

    // seed candidates dSep to both sides of each point, across the local direction of the line
    private fun seedAlong(ids: IntArray) {
        for (k in ids.indices) {
            val a = ids[max(k - 1, 0)]
            val b = ids[min(k + 1, ids.size - 1)]
            val len = dist(xs[a], ys[a], xs[b], ys[b])
            if (len == 0f) continue
            val nx = -(ys[b] - ys[a]) / len * dSep
            val ny = (xs[b] - xs[a]) / len * dSep
            val x = xs[ids[k]]
            val y = ys[ids[k]]
            seeds.add(x + nx)
            seeds.add(y + ny)
            seeds.add(x - nx)
            seeds.add(y - ny)
        }
    }

    private fun add(x: Float, y: Float, arc: Float, line: Int) {
        grid.add(xs.size, x, y)
        xs.add(x)
        ys.add(y)
        arcs.add(arc)
        owners.add(line)
    }

    private fun inside(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom

    // true when no point is closer than r to (x, y), other than the points of [line] itself that
    // are near along it. line -1 takes every point into account
    private fun isFree(x: Float, y: Float, r: Float, line: Int, arc: Float): Boolean {
        val r2 = r * r
        val skip = SELF_SKIP * dTest
        grid.forEachNear(x, y) { id ->
            val dx = xs[id] - x
            val dy = ys[id] - y
            if (dx * dx + dy * dy < r2 && (owners[id] != line || abs(arcs[id] - arc) > skip)) return false
        }
        return true
    }

    private fun clearance(id: Int, line: Int): Float {
        var best = dSep * dSep
        val x = xs[id]
        val y = ys[id]
        grid.forEachNear(x, y) { o ->
            if (owners[o] != line) {
                val dx = xs[o] - x
                val dy = ys[o] - y
                best = min(best, dx * dx + dy * dy)
            }
        }
        return sqrt(best)
    }
}

// point ids in square cells as big as the separation, so a test within dSep only has to look
// at the 3x3 cells around a point
private class SpacingGrid(val left: Float, val top: Float, right: Float, bottom: Float, val cs: Float) {
    val cols = ceil((right - left) / cs).toInt() + 1
    val rows = ceil((bottom - top) / cs).toInt() + 1
    val ids = arrayOfNulls<IntArray>(cols * rows)
    val count = IntArray(cols * rows)

    fun col(x: Float) = ((x - left) / cs).toInt().coerceIn(0, cols - 1)
    fun row(y: Float) = ((y - top) / cs).toInt().coerceIn(0, rows - 1)

    fun add(id: Int, x: Float, y: Float) {
        val c = row(y) * cols + col(x)
        var a = ids[c]
        if (a == null) {
            a = IntArray(8)
            ids[c] = a
        } else if (count[c] == a.size) {
            a = a.copyOf(a.size * 2)
            ids[c] = a
        }
        a[count[c]++] = id
    }

    // takes back the id that was added last to the cell under (x, y)
    fun removeLast(x: Float, y: Float) {
        count[row(y) * cols + col(x)]--
    }

    inline fun forEachNear(x: Float, y: Float, f: (Int) -> Unit) {
        val cx = col(x)
        val cy = row(y)
        for (j in max(cy - 1, 0)..min(cy + 1, rows - 1)) {
            for (i in max(cx - 1, 0)..min(cx + 1, cols - 1)) {
                val c = j * cols + i
                val a = ids[c] ?: continue
                for (k in 0 until count[c]) f(a[k])
            }
        }
    }
}

// growable primitive arrays, boxing a few hundred thousand points is silly
private class FloatList {
    var data = FloatArray(256)
    var size = 0

    fun add(v: Float) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = v
    }

    operator fun get(i: Int) = data[i]
    operator fun set(i: Int, v: Float) {
        data[i] = v
    }
}

private class IntList {
    var data = IntArray(256)
    var size = 0

    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = v
    }

    operator fun get(i: Int) = data[i]
}
