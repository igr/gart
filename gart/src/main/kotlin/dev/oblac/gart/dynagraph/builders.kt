package dev.oblac.gart.dynagraph

import dev.oblac.gart.angle.Radians
import dev.oblac.gart.gfx.createNtagonPoints
import org.jetbrains.skia.Point

/**
 * Adds [count] vertices evenly spaced on a circle of radius [radius] around
 * [center], plus edges connecting consecutive vertices in [group]. When
 * [closed] is true (default) the loop is closed and the group is marked closed.
 * Returns the IDs of the newly-created vertices (in order).
     */
fun DynaGraph.addCircle(
    center: Point,
    radius: Float,
    count: Int,
    group: GroupId = DynaGraph.MAIN,
    closed: Boolean = true,
): IntArray {
    require(count >= 3) { "count must be >= 3, was $count" }
    return addPath(createNtagonPoints(count, center.x, center.y, radius), group, closed)
}

/**
 * Regular polygon — convenience over [addCircle] with explicit rotation.
 */
fun DynaGraph.addPolygon(
    center: Point,
    radius: Float,
    sides: Int,
    rotation: Radians = Radians(0f),
    group: GroupId = DynaGraph.MAIN,
): IntArray {
    require(sides >= 3) { "sides must be >= 3, was $sides" }
    return addPath(
        createNtagonPoints(sides, center.x, center.y, radius, rotation.value),
        group,
        closed = true,
    )
}

/**
 * Adds two new vertices at [a] and [b] and an edge between them. Returns the IDs.
 */
fun DynaGraph.addLine(a: Point, b: Point, group: GroupId = DynaGraph.MAIN): Pair<Int, Int> {
    val verts = vaddEdge(a, b, group).newVerts
    return verts.takeIf { it.size == 2 }?.let { it[0] to it[1] }
        ?: error(" DynaGraph vertex capacity reached")
}

/**
 * Adds a vertex per element of [points] and edges between consecutive vertices.
 * When [closed] is true the last vertex is connected back to the first.
 * Returns the IDs in input order.
 */
fun DynaGraph.addPath(
    points: List<Point>,
    group: GroupId = DynaGraph.MAIN,
    closed: Boolean = false,
): IntArray {
    require(points.size >= 2) { "at least 2 points required" }
    val ids = IntArray(points.size)
    for (i in points.indices) {
        ids[i] = addVert(points[i]).newVert ?: error("DynaGraph vertex capacity reached")
    }
    val g = ensureGroup(group)
    for (i in 0 until ids.size - 1) g.add(ids[i], ids[i + 1])
    if (closed) {
        g.add(ids[ids.size - 1], ids[0])
        g.closed = true
    }
    return ids
}

/**
 * Connects existing vertices in order with edges. With [closed] = true the
 * last vertex is connected back to the first. Returns the count of edges added.
 */
fun DynaGraph.addPathByVertices(
    vertices: IntArray,
    group: GroupId = DynaGraph.MAIN,
    closed: Boolean = false,
): Int {
    require(vertices.size >= 2) { "at least 2 vertices required" }
    val g = ensureGroup(group)
    var added = 0
    for (i in 0 until vertices.size - 1) {
        if (g.add(vertices[i], vertices[i + 1])) added += 1
    }
    if (closed) {
        if (g.add(vertices.last(), vertices.first())) added += 1
        g.closed = true
    }
    return added
}
