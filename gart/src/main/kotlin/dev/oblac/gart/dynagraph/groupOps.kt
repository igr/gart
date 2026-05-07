package dev.oblac.gart.dynagraph

/**
 * Set-style operations between named groups in a [DynaGraph]. All groups
 * share the same vertex pool, so these ops shuffle **edges** between groups.
 * Every "into" target is additive: edges are added to the existing contents
 * of `into`, never replaced. Call [clearGroup] first if you want a fresh
 * result.
 *
 * Source groups missing from the graph are treated as empty sets. Target
 * groups are created lazily when needed (matching [DynaGraph.ensureGroup]).
 */

/**
 * Removes every edge from [group]. The group entry is retained (so future
 * lookups still succeed) but its `closed` flag is cleared. Returns the
 * number of edges removed.
 */
fun DynaGraph.clearGroup(group: GroupId): Int {
    val g = groupOrNull(group) ?: return 0
    val removed = g.edges().toList().count { g.del(it.a, it.b) }
    g.closed = false
    return removed
}

/**
 * Copies every edge from [from] into [into], skipping duplicates already
 * present in the destination. Returns the number of edges actually added.
 * No-op when `from == into`.
 */
fun DynaGraph.copyGroup(from: GroupId, into: GroupId): Int {
    if (from == into) return 0
    val src = groupOrNull(from) ?: return 0
    val dst = ensureGroup(into)
    return src.edges().toList().count { dst.add(it.a, it.b) }
}

/**
 * Equivalent to [copyGroup] followed by [clearGroup] on the source. Returns
 * the number of edges added to the destination. No-op when `from == into`.
 */
fun DynaGraph.moveGroup(from: GroupId, into: GroupId): Int {
    if (from == into) return 0
    val added = copyGroup(from, into)
    clearGroup(from)
    return added
}

/**
 * Adds the union of [a] and [b] (edges in either) into [into]. Safe when
 * `into` equals `a` or `b` — the snapshot is taken before modification.
 * Returns the number of edges added to the destination.
 */
fun DynaGraph.unionInto(a: GroupId, b: GroupId, into: GroupId): Int {
    val edges = HashSet<Edge>()
    groupOrNull(a)?.edges()?.forEach { edges.add(it) }
    groupOrNull(b)?.edges()?.forEach { edges.add(it) }
    val dst = ensureGroup(into)
    return edges.count { dst.add(it.a, it.b) }
}

/**
 * Adds the intersection of [a] and [b] (edges in both) into [into]. Returns
 * the number of edges added.
 *
 * Note: if `into` equals `a` or `b`, the destination already contains every
 * intersection edge, so the return value will be 0 — but `into` is not
 * trimmed. Call [clearGroup] first if you want `into` to *be* the
 * intersection rather than a superset of it.
 */
fun DynaGraph.intersectInto(a: GroupId, b: GroupId, into: GroupId): Int {
    val ga = groupOrNull(a) ?: return 0
    val gb = groupOrNull(b) ?: return 0
    val dst = ensureGroup(into)
    return ga.edges().toList().count { gb.mem(it.a, it.b) && dst.add(it.a, it.b) }
}

/**
 * Adds the set difference `a \ b` (edges in [a] but not in [b]) into [into].
 * Missing [b] is treated as empty, so the difference is just `a`. Returns
 * the number of edges added.
 */
fun DynaGraph.differenceInto(a: GroupId, b: GroupId, into: GroupId): Int {
    val ga = groupOrNull(a) ?: return 0
    val gb = groupOrNull(b)
    val dst = ensureGroup(into)
    return ga.edges().toList().count {
        (gb == null || !gb.mem(it.a, it.b)) && dst.add(it.a, it.b)
    }
}

/**
 * Adds the symmetric difference `a △ b` (edges in exactly one of [a], [b])
 * into [into]. Returns the number of edges added.
 */
fun DynaGraph.symmetricDifferenceInto(a: GroupId, b: GroupId, into: GroupId): Int {
    val ga = groupOrNull(a)
    val gb = groupOrNull(b)
    val aSnap = ga?.edges()?.toList().orEmpty()
    val bSnap = gb?.edges()?.toList().orEmpty()
    val dst = ensureGroup(into)
    val fromA = aSnap.count { (gb == null || !gb.mem(it.a, it.b)) && dst.add(it.a, it.b) }
    val fromB = bSnap.count { (ga == null || !ga.mem(it.a, it.b)) && dst.add(it.a, it.b) }
    return fromA + fromB
}
