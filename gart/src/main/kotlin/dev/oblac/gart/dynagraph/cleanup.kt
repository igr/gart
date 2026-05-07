package dev.oblac.gart.dynagraph

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Collapses every cluster of vertices in [group] whose pairwise distance is
 * `<= eps` into a single representative (the lowest-ID vertex in the cluster).
 * Edges are rewritten onto the representative; resulting self-loops and
 * duplicates are dropped. Skipped-out vertices remain in the pool but no
 * longer participate in [group].
 *
 * Returns the count of edges that were rewritten or removed. Naive O(V²) over
 * the group's vertices.
 */
fun DynaGraph.mergeCloseVertices(
    eps: Float,
    group: GroupId = DynaGraph.MAIN,
): Int {
    val g = groupOrNull(group) ?: return 0
    val verts = g.vertices().toIntArray()
    if (verts.size < 2) return 0

    val parent = IntArray(verts.size) { it }
    fun find(i: Int): Int {
        var r = i
        while (parent[r] != r) r = parent[r]
        var c = i
        while (parent[c] != c) {
            val next = parent[c]
            parent[c] = r
            c = next
        }
        return r
    }

    val eps2 = eps * eps
    for (i in verts.indices) {
        val xi = x(verts[i]); val yi = y(verts[i])
        for (j in i + 1 until verts.size) {
            val dx = x(verts[j]) - xi
            val dy = y(verts[j]) - yi
            if (dx * dx + dy * dy <= eps2) {
                val ra = find(i); val rb = find(j)
                if (ra != rb) parent[ra] = rb
            }
        }
    }

    // Pick the lowest-ID vertex in each cluster as its representative.
    val rootToRep = HashMap<Int, Int>()
    for (i in verts.indices) rootToRep.merge(find(i), verts[i], Math::min)
    val finalRep = verts.indices.associate { verts[it] to rootToRep.getValue(find(it)) }

    var changed = 0
    val edges = g.edges().toList()
    for (e in edges) {
        val ra = finalRep.getValue(e.a)
        val rb = finalRep.getValue(e.b)
        if (ra == e.a && rb == e.b) continue
        if (g.del(e.a, e.b)) changed += 1
        if (ra != rb) g.add(ra, rb)
    }
    return changed
}

/**
 * Iteratively splits every edge in [group] longer than [maxLen] at its
 * midpoint, repeating until no edge exceeds the limit (or [maxIterations]
 * passes are spent). Returns the total number of split operations performed.
 */
fun DynaGraph.subdivideLongerThan(
    maxLen: Float,
    group: GroupId = DynaGraph.MAIN,
    maxIterations: Int = 8,
): Int {
    var splits = 0
    repeat(maxIterations) {
        val g = groupOrNull(group) ?: return splits
        val toSplit = g.edges().filter { edgeLength(it) > maxLen }.toList()
        if (toSplit.isEmpty()) return splits
        splits += toSplit.count { splitEdge(it.a, it.b, group = group).isOk }
    }
    return splits
}

/**
 * Drops edges belonging to connected components in [group] with fewer than
 * [minSize] vertices. Useful for cleaning up specks left by stochastic
 * growth. Returns the number of edges removed.
 */
fun DynaGraph.pruneSmallComponents(
    minSize: Int,
    group: GroupId = DynaGraph.MAIN,
): Int {
    val g = groupOrNull(group) ?: return 0
    return g.connectedComponents()
        .filter { it.size < minSize }
        .sumOf { comp ->
            val set = comp.toHashSet()
            g.edges().toList().count { it.a in set && it.b in set && g.del(it.a, it.b) }
        }
}

/**
 * Collapses each edge in [group] shorter than [minLen] by merging its
 * endpoints onto the lower-ID one. Iterates until no short edges remain.
 * Returns the number of edges collapsed.
 */
fun DynaGraph.mergeShortEdges(
    minLen: Float,
    group: GroupId = DynaGraph.MAIN,
): Int {
    val g = groupOrNull(group) ?: return 0
    var merged = 0
    while (true) {
        val short = g.edges().firstOrNull { edgeLength(it) < minLen } ?: break
        val keep = minOf(short.a, short.b)
        val drop = maxOf(short.a, short.b)
        val others = g.neighbors(drop).filter { it != keep }.toList()
        for (n in others) {
            g.del(drop, n)
            g.add(keep, n)
        }
        g.del(keep, drop)
        merged += 1
    }
    return merged
}

/**
 * Douglas–Peucker simplification on a [group] that forms a single open chain
 * or closed loop. Vertices whose perpendicular distance from the simplified
 * chord is `<= eps` are dropped from the chain (they remain in the vertex
 * pool but no longer participate in [group]'s topology).
 *
 * Returns the number of vertices removed from the chain. Returns 0 if the
 * group is not chain/loop-shaped — use [groupAsPath] to verify the topology.
 */
fun DynaGraph.simplifyChain(
    eps: Float,
    group: GroupId = DynaGraph.MAIN,
): Int {
    val g = groupOrNull(group) ?: return 0
    val ordered = orderChainOrLoop(g) ?: return 0
    if (ordered.size < 3) return 0
    val isLoop = g.closed && g.numEdges == ordered.size

    val keep = BooleanArray(ordered.size)
    keep[0] = true
    keep[ordered.size - 1] = true

    fun simplify(start: Int, end: Int) {
        if (end <= start + 1) return
        val ax = x(ordered[start]); val ay = y(ordered[start])
        val bx = x(ordered[end]); val by = y(ordered[end])
        var maxD = 0f
        var maxI = -1
        for (i in start + 1 until end) {
            val d = perpDist(x(ordered[i]), y(ordered[i]), ax, ay, bx, by)
            if (d > maxD) {
                maxD = d
                maxI = i
            }
        }
        if (maxD > eps && maxI >= 0) {
            keep[maxI] = true
            simplify(start, maxI)
            simplify(maxI, end)
        }
    }

    simplify(0, ordered.size - 1)

    val kept = ordered.filterIndexed { i, _ -> keep[i] }
    if (kept.size == ordered.size) return 0

    for (e in g.edges().toList()) g.del(e.a, e.b)
    for (i in 0 until kept.size - 1) g.add(kept[i], kept[i + 1])
    if (isLoop && kept.size >= 3) {
        g.add(kept.last(), kept.first())
        g.closed = true
    }
    return ordered.size - kept.size
}

/**
 * Returns vertices of [g] in chain or loop order if the topology is a single
 * open chain (two degree-1 endpoints, all others degree 2) or closed loop
 * (every vertex degree 2). Returns null otherwise.
 */
private fun orderChainOrLoop(g: Graph): List<Int>? {
    if (g.numVertices < 2 || g.numEdges < 1) return null

    val loop = g.loop()
    if (loop != null) return loop

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

    val out = ArrayList<Int>(g.numVertices)
    var prev = -1
    var cur = endpoint
    while (true) {
        out.add(cur)
        val ns = g.neighbors(cur)
        if (ns.size == 1 && out.size > 1) break
        val next = ns.firstOrNull { it != prev } ?: break
        prev = cur
        cur = next
        if (out.size > g.numVertices) return null
    }
    return if (out.size == g.numVertices) out else null
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
