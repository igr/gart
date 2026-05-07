package dev.oblac.gart.dynagraph

/**
 * Builds the Euclidean minimum spanning tree over **all** vertices in the
 * graph and writes its edges into [targetGroup] (creating the group if
 * missing). Edges with squared length above `maxLen²` are skipped, so a
 * finite [maxLen] yields a minimum-spanning *forest* on the points (one
 * tree per cluster). Returns the number of edges added.
 *
 * Kruskal with union-find. Generates O(n²) candidate edges and sorts them,
 * so this is fine for hundreds to a few thousand points; for larger sets
 * pre-filter through a Delaunay triangulation and feed those edges in.
 */
fun DynaGraph.minimumSpanningTree(
    targetGroup: GroupId,
    maxLen: Float = Float.POSITIVE_INFINITY,
): Int {
    val n = verticesCount
    if (n < 2) return 0
    val maxLen2 = if (maxLen.isFinite()) maxLen * maxLen else Float.POSITIVE_INFINITY
    ensureGroup(targetGroup)

    // Pack (a, b) into a Long key, weight into parallel array.
    val candidates = ArrayList<Long>(n * 2)
    val weights = ArrayList<Float>(n * 2)
    for (u in 0 until n) {
        val ux = x(u); val uy = y(u)
        for (v in u + 1 until n) {
            val dx = x(v) - ux
            val dy = y(v) - uy
            val d2 = dx * dx + dy * dy
            if (d2 > maxLen2) continue
            candidates.add((u.toLong() shl 32) or (v.toLong() and 0xFFFFFFFFL))
            weights.add(d2)
        }
    }

    val order = (0 until candidates.size).sortedBy { weights[it] }

    val parent = IntArray(n) { it }
    fun find(i: Int): Int {
        var r = i
        while (parent[r] != r) r = parent[r]
        var c = i
        while (parent[c] != c) {
            val next = parent[c]; parent[c] = r; c = next
        }
        return r
    }

    var added = 0
    val target = n - 1
    for (idx in order) {
        val packed = candidates[idx]
        val a = (packed ushr 32).toInt()
        val b = (packed and 0xFFFFFFFFL).toInt()
        val ra = find(a); val rb = find(b)
        if (ra == rb) continue
        parent[ra] = rb
        if (addEdge(a, b, targetGroup).isOk) added += 1
        if (added == target) break
    }
    return added
}
