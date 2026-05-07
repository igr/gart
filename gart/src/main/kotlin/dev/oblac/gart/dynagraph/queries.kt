package dev.oblac.gart.dynagraph

import java.util.ArrayDeque

/** Number of neighbors of `v` in this graph. Returns 0 if `v` is unknown. */
fun Graph.degree(v: Int): Int = neighbors(v).size

/**
 * Sequence of vertex IDs in breadth-first order starting from [start].
 * Yields nothing if [start] is not in the graph. Each vertex appears at most
 * once. Edges are traversed in adjacency-list order.
 */
fun Graph.bfs(start: Int): Sequence<Int> = sequence {
    if (!vmem(start)) return@sequence
    val visited = HashSet<Int>()
    val queue = ArrayDeque<Int>()
    queue.add(start)
    visited.add(start)
    while (queue.isNotEmpty()) {
        val v = queue.poll()
        yield(v)
        for (n in neighbors(v)) {
            if (visited.add(n)) queue.add(n)
        }
    }
}

/**
 * Vertex ID lists for every connected component in this graph, ordered by
 * decreasing size. Vertices appearing only as keys with no neighbors are not
 * tracked (this graph drops empty adjacency entries on `del`).
 */
fun Graph.connectedComponents(): List<List<Int>> {
    val seen = HashSet<Int>()
    val out = ArrayList<List<Int>>()
    for (v in vertices()) {
        if (v in seen) continue
        val comp = ArrayList<Int>()
        for (u in bfs(v)) {
            seen.add(u)
            comp.add(u)
        }
        out.add(comp)
    }
    out.sortByDescending { it.size }
    return out
}

/**
 * Shortest path from [start] to [end] in this graph (edges treated as
 * unit-weight). Returns the inclusive vertex list, or null if no path exists.
 */
fun Graph.shortestPath(start: Int, end: Int): List<Int>? {
    if (!vmem(start) || !vmem(end)) return null
    if (start == end) return listOf(start)
    val parent = HashMap<Int, Int>()
    parent[start] = start
    val queue = ArrayDeque<Int>()
    queue.add(start)
    while (queue.isNotEmpty()) {
        val v = queue.poll()
        if (v == end) break
        for (n in neighbors(v)) {
            if (parent.putIfAbsent(n, v) == null) queue.add(n)
        }
    }
    if (end !in parent) return null
    val path = ArrayList<Int>()
    var cur = end
    while (cur != start) {
        path.add(cur)
        cur = parent.getValue(cur)
    }
    path.add(start)
    path.reverse()
    return path
}

/** Group-scoped [Graph.degree]. */
fun DynaGraph.degree(v: Int, group: GroupId = DynaGraph.MAIN): Int =
    groupOrNull(group)?.degree(v) ?: 0

/** Group-scoped [Graph.connectedComponents]. */
fun DynaGraph.connectedComponents(group: GroupId = DynaGraph.MAIN): List<List<Int>> =
    groupOrNull(group)?.connectedComponents() ?: emptyList()

/** Group-scoped [Graph.bfs]. */
fun DynaGraph.bfs(start: Int, group: GroupId = DynaGraph.MAIN): Sequence<Int> =
    groupOrNull(group)?.bfs(start) ?: emptySequence()

/** Group-scoped [Graph.shortestPath]. */
fun DynaGraph.shortestPath(start: Int, end: Int, group: GroupId = DynaGraph.MAIN): List<Int>? =
    groupOrNull(group)?.shortestPath(start, end)
