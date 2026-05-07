package dev.oblac.gart.dynagraph

import org.jetbrains.skia.Point
import kotlin.math.sqrt

/**
 * One step of Hookean spring relaxation: each edge in [group] pulls its
 * endpoints toward [restLen]. Negative `length - restLen` (compressed) pushes
 * apart, positive (stretched) pulls together. [k] is the spring constant
 * (0..1 typically); larger values converge faster but can oscillate.
 *
 * Edge forces are accumulated against the pre-step positions and applied
 * once at the end, so order of edges does not bias the result.
 */
fun DynaGraph.relaxSprings(
    restLen: Float,
    k: Float = 0.1f,
    group: GroupId = DynaGraph.MAIN,
) {
    val g = groupOrNull(group) ?: return
    val dxs = FloatArray(verticesCount)
    val dys = FloatArray(verticesCount)
    for (e in g.edges()) {
        val ax = x(e.a); val ay = y(e.a)
        val bx = x(e.b); val by = y(e.b)
        val dx = bx - ax; val dy = by - ay
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1e-6f) continue
        val factor = k * (len - restLen) / len * 0.5f
        val fx = dx * factor
        val fy = dy * factor
        dxs[e.a] += fx; dys[e.a] += fy
        dxs[e.b] -= fx; dys[e.b] -= fy
    }
    applyDeltas(dxs, dys)
}

/**
 * One step of Laplacian smoothing on [group]: each vertex moves toward the
 * mean position of its neighbors, blended by [alpha] (0 = no move, 1 = jump
 * fully to the centroid). All deltas are computed against pre-step positions
 * and applied together.
 */
fun DynaGraph.smoothLaplacian(
    alpha: Float = 0.2f,
    group: GroupId = DynaGraph.MAIN,
) {
    val g = groupOrNull(group) ?: return
    val dxs = FloatArray(verticesCount)
    val dys = FloatArray(verticesCount)
    for (v in g.vertices()) {
        val ns = g.neighbors(v)
        if (ns.isEmpty()) continue
        var sx = 0f; var sy = 0f
        for (n in ns) {
            sx += x(n); sy += y(n)
        }
        val inv = 1f / ns.size
        val cx = sx * inv; val cy = sy * inv
        dxs[v] = (cx - x(v)) * alpha
        dys[v] = (cy - y(v)) * alpha
    }
    applyDeltas(dxs, dys)
}

/**
 * One step of pairwise repulsion: vertices in [group] within [radius] push
 * each other apart with a linearly-decaying force scaled by [strength].
 *
 * Naive O(n²) over the group's vertices — fine for hundreds of points, slow
 * for thousands. For larger sets, bucket the vertices yourself and call this
 * per cell.
 */
fun DynaGraph.repelVertices(
    radius: Float,
    strength: Float = 0.5f,
    group: GroupId = DynaGraph.MAIN,
) {
    val g = groupOrNull(group) ?: return
    val verts = g.vertices().toIntArray()
    val dxs = FloatArray(verticesCount)
    val dys = FloatArray(verticesCount)
    val r2 = radius * radius
    for (i in verts.indices) {
        val u = verts[i]
        val ux = x(u); val uy = y(u)
        for (j in i + 1 until verts.size) {
            val v = verts[j]
            val dx = x(v) - ux
            val dy = y(v) - uy
            val d2 = dx * dx + dy * dy
            if (d2 >= r2 || d2 < 1e-8f) continue
            val d = sqrt(d2)
            val factor = strength * (1f - d / radius) / d * 0.5f
            val fx = dx * factor
            val fy = dy * factor
            dxs[u] -= fx; dys[u] -= fy
            dxs[v] += fx; dys[v] += fy
        }
    }
    applyDeltas(dxs, dys)
}

/**
 * Pin [vs] against any pending mutation step in [block] by recording the
 * current position and snapping it back after [block] returns. Useful for
 * running a relaxation pass while keeping anchor points fixed.
 */
fun DynaGraph.pinned(vararg vs: Int, block: () -> Unit) {
    val saved = vs.map { it to point(it) }
    block()
    for ((v, p) in saved) setPoint(v, p)
}

private fun DynaGraph.applyDeltas(dxs: FloatArray, dys: FloatArray) {
    for (v in 0 until verticesCount) {
        if (dxs[v] != 0f || dys[v] != 0f) moveVert(v, Point(dxs[v], dys[v]))
    }
}
