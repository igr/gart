package dev.oblac.gart.gfx

import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Douglas–Peucker simplification of a polyline. Returns a subsequence of
 * [points] (in original order) that includes both endpoints plus every
 * interior point whose perpendicular distance from the simplified chord
 * exceeds [eps]. Useful for thinning hand-drawn paths, recorded mouse
 * trails, traced contours, sampled curves, etc.
 *
 * For a closed polyline, set [closed] = true; the algorithm picks the two
 * points farthest apart as the seeded endpoints so the simplified loop is
 * not biased toward the input's first vertex.
 *
 * For chains/loops already living in a [dev.oblac.gart.dynagraph.DynaGraph],
 * use `simplifyChain(group, eps)` instead — it operates on the graph in
 * place.
 */
fun simplifyPoints(points: List<Point>, eps: Float, closed: Boolean = false): List<Point> {
    if (points.size < 3) return points.toList()

    val keep = BooleanArray(points.size)

    if (closed) {
        // Seed with the two points farthest apart along the loop.
        val (a, b) = farthestPair(points)
        keep[a] = true
        keep[b] = true
        val first = minOf(a, b)
        val second = maxOf(a, b)
        simplify(points, first, second, eps, keep)
        // Wrap-around segment: simplify second..end + 0..first as one piece.
        simplifyWrap(points, second, first, eps, keep)
    } else {
        keep[0] = true
        keep[points.size - 1] = true
        simplify(points, 0, points.size - 1, eps, keep)
    }

    val out = ArrayList<Point>(points.size)
    for (i in points.indices) if (keep[i]) out.add(points[i])
    return out
}

private fun simplify(points: List<Point>, start: Int, end: Int, eps: Float, keep: BooleanArray) {
    if (end <= start + 1) return
    val ax = points[start].x; val ay = points[start].y
    val bx = points[end].x; val by = points[end].y
    var maxD = 0f
    var maxI = -1
    for (i in start + 1 until end) {
        val d = perpDist(points[i].x, points[i].y, ax, ay, bx, by)
        if (d > maxD) {
            maxD = d
            maxI = i
        }
    }
    if (maxD > eps && maxI >= 0) {
        keep[maxI] = true
        simplify(points, start, maxI, eps, keep)
        simplify(points, maxI, end, eps, keep)
    }
}

/** Simplify the wrap-around piece of a closed polyline (start..size-1 then 0..end). */
private fun simplifyWrap(points: List<Point>, start: Int, end: Int, eps: Float, keep: BooleanArray) {
    val n = points.size
    val seg = ArrayList<Int>(n)
    var i = start
    while (i != end) {
        seg.add(i)
        i = (i + 1) % n
    }
    seg.add(end)
    if (seg.size < 3) return
    val mappedKeep = BooleanArray(seg.size)
    mappedKeep[0] = true
    mappedKeep[seg.size - 1] = true
    val mappedPoints = seg.map { points[it] }
    simplify(mappedPoints, 0, seg.size - 1, eps, mappedKeep)
    for (k in seg.indices) if (mappedKeep[k]) keep[seg[k]] = true
}

private fun farthestPair(points: List<Point>): Pair<Int, Int> {
    // O(n²); fine for a one-shot seeding of small loops.
    var bestD2 = -1f
    var bestA = 0
    var bestB = 0
    for (i in points.indices) {
        for (j in i + 1 until points.size) {
            val dx = points[j].x - points[i].x
            val dy = points[j].y - points[i].y
            val d2 = dx * dx + dy * dy
            if (d2 > bestD2) {
                bestD2 = d2
                bestA = i
                bestB = j
            }
        }
    }
    return bestA to bestB
}

private fun perpDist(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = bx - ax
    val dy = by - ay
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1e-6f) {
        val ex = px - ax; val ey = py - ay
        return sqrt(ex * ex + ey * ey)
    }
    return abs(dy * px - dx * py + bx * ay - by * ax) / len
}
