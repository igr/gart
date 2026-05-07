package dev.oblac.gart.dynagraph

import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

/** Internal accessor: returns the named [Graph] or null. Avoids the throwing `group()`. */
internal fun DynaGraph.groupOrNull(name: GroupId): Graph? = if (name in groupIds()) group(name) else null

/**
 * Axis-aligned bounding box of the vertices that participate in [group].
 * Returns `Rect(0,0,0,0)` if the group is empty or missing.
 */
fun DynaGraph.bounds(group: GroupId = DynaGraph.MAIN): Rect {
    val g = groupOrNull(group) ?: return Rect.makeXYWH(0f, 0f, 0f, 0f)
    if (g.numVertices == 0) return Rect.makeXYWH(0f, 0f, 0f, 0f)
    var xMin = Float.POSITIVE_INFINITY
    var yMin = Float.POSITIVE_INFINITY
    var xMax = Float.NEGATIVE_INFINITY
    var yMax = Float.NEGATIVE_INFINITY
    for (v in g.vertices()) {
        val px = x(v); val py = y(v)
        if (px < xMin) xMin = px; if (px > xMax) xMax = px
        if (py < yMin) yMin = py; if (py > yMax) yMax = py
    }
    return Rect.makeLTRB(xMin, yMin, xMax, yMax)
}

/**
 * Centroid (mean position) of the vertices in [group]. Returns `Point(0,0)`
 * if the group is empty or missing.
 */
fun DynaGraph.centroid(group: GroupId = DynaGraph.MAIN): Point {
    val g = groupOrNull(group) ?: return Point(0f, 0f)
    if (g.numVertices == 0) return Point(0f, 0f)
    var sx = 0.0
    var sy = 0.0
    for (v in g.vertices()) {
        sx += x(v).toDouble()
        sy += y(v).toDouble()
    }
    val n = g.numVertices.toDouble()
    return Point((sx / n).toFloat(), (sy / n).toFloat())
}

/**
 * Translates **all** vertices in the graph so the bounding-box center of [group]
 * lands on [target]. The chosen group anchors the translation, but every vertex
 * moves in lockstep so relationships between
 * groups are preserved. Returns the previous bbox center.
 */
fun DynaGraph.center(group: GroupId = DynaGraph.MAIN, target: Point = Point(0f, 0f)): Point {
    val b = bounds(group)
    val cx = (b.left + b.right) * 0.5f
    val cy = (b.top + b.bottom) * 0.5f
    val dx = target.x - cx
    val dy = target.y - cy
    for (v in 0 until verticesCount) setPoint(v, x(v) + dx, y(v) + dy)
    return Point(cx, cy)
}

/** Lengths of every edge in [group] (canonical order: matches `Graph.edges()`). */
fun DynaGraph.edgeLengths(group: GroupId = DynaGraph.MAIN): FloatArray {
    val g = groupOrNull(group) ?: return FloatArray(0)
    val out = FloatArray(g.numEdges)
    var i = 0
    for (e in g.edges()) {
        out[i++] = edgeLength(e.a, e.b)
    }
    return out
}

/**
 * Removes edges from [group] for which [keep]`(length)` returns false. Takes a
 * Kotlin predicate so callers write `graph.pruneEdgesByLen { it < 50f }`.
 * Returns the number of edges removed.
 */
fun DynaGraph.pruneEdgesByLen(group: GroupId = DynaGraph.MAIN, keep: (Float) -> Boolean): Int {
    val g = groupOrNull(group) ?: return 0
    val toRemove = ArrayList<Edge>()
    for (e in g.edges()) if (!keep(edgeLength(e.a, e.b))) toRemove.add(e)
    var removed = 0
    for (e in toRemove) if (g.del(e.a, e.b)) removed += 1
    return removed
}

/**
 * Returns a Skia [Path] tracing [group] when it forms a single chain (two
 * degree-1 endpoints, all interior vertices degree 2) or a single closed cycle
 * (every vertex degree 2). Returns null otherwise — callers wanting to render
 * arbitrary topologies should use [DynaGraph.toPath] instead.
 */
fun DynaGraph.groupAsPath(group: GroupId = DynaGraph.MAIN): Path? {
    val g = groupOrNull(group) ?: return null
    if (g.numVertices < 2 || g.numEdges < 1) return null

    val loop = g.loop()
    if (loop != null) {
        val pb = PathBuilder()
        pb.moveTo(x(loop[0]), y(loop[0]))
        for (i in 1 until loop.size) pb.lineTo(x(loop[i]), y(loop[i]))
        pb.closePath()
        return pb.detach()
    }

    var endpoint = -1
    for (v in g.vertices()) {
        val d = g.neighbors(v).size
        if (d == 1) {
            if (endpoint == -1) endpoint = v
        } else if (d != 2) {
            return null
        }
    }
    if (endpoint == -1) return null

    val ordered = ArrayList<Int>(g.numVertices)
    var prev = -1
    var cur = endpoint
    while (true) {
        ordered.add(cur)
        val ns = g.neighbors(cur)
        if (ns.size == 1 && ordered.size > 1) break
        val next = ns.firstOrNull { it != prev } ?: break
        prev = cur
        cur = next
        if (ordered.size > g.numVertices) return null
    }
    if (ordered.size != g.numVertices) return null

    val pb = PathBuilder()
    pb.moveTo(x(ordered[0]), y(ordered[0]))
    for (i in 1 until ordered.size) pb.lineTo(x(ordered[i]), y(ordered[i]))
    return pb.detach()
}

/**
 * Builds the relative-neighborhood graph (RNG) over **all** graph vertices
 * (filtered by [radius]) into [targetGroup], creating the target group if
 * missing.
 *
 * An edge `(u, v)` is added iff `dist(u, v) <= radius` and no other vertex `w`
 * satisfies `dist(u, w) <= dist(u, v)` AND `dist(v, w) <= dist(u, v)` (the
 * closed-lune RNG condition).
 *
 * Naive O(n³) implementation — appropriate for the "core" port. Returns the
 * number of edges added.
 */
fun DynaGraph.relativeNeighborhood(
    targetGroup: GroupId,
    radius: Float,
): Int {
    val n = verticesCount
    if (n < 2) return 0
    ensureGroup(targetGroup)

    val r2 = radius * radius
    var added = 0
    for (u in 0 until n) {
        val ux = x(u); val uy = y(u)
        for (v in u + 1 until n) {
            val vx = x(v); val vy = y(v)
            val dxuv = ux - vx
            val dyuv = uy - vy
            val duv2 = dxuv * dxuv + dyuv * dyuv
            if (duv2 > r2) continue

            var blocked = false
            for (w in 0 until n) {
                if (w == u || w == v) continue
                val wx = x(w); val wy = y(w)
                val duw = (ux - wx) * (ux - wx) + (uy - wy) * (uy - wy)
                if (duw > duv2) continue
                val dvw = (vx - wx) * (vx - wx) + (vy - wy) * (vy - wy)
                if (dvw <= duv2) {
                    blocked = true
                    break
                }
            }
            if (!blocked && addEdge(u, v, targetGroup).isOk) added += 1
        }
    }
    return added
}
