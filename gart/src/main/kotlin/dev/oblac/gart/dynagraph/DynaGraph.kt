package dev.oblac.gart.dynagraph

import dev.oblac.gart.dynagraph.DynaGraph.Companion.MAIN
import dev.oblac.gart.gfx.segmentsCross
import dev.oblac.gart.math.dist
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Point

@JvmInline
value class GroupId(val value: String) {
    override fun toString(): String = value
}


/**
 * A mutable dot-and-line drawing model: vertices are dots with positions, and
 * groups decide which dots are connected by lines.
 *
 * A shared vertex pool plus a map of named groups,
 * where each group is a `Graph` whose vertex IDs index into the pool.
 *
 * Vertices live in a fixed-size array. Vertex IDs are stable indexes into that
 * array, and each vertex stores its own ID with its current position.
 *
 * The default group identified by [MAIN] is created up front. Other groups are created
 * lazily on first edge insertion (see [addEdge], [appendEdge], etc.).
 */
class DynaGraph(
    val maxVertices: Int = 100_000,
    initialGroupCapacity: Int = 16,
) {
    private val vertices = Array(maxVertices) { Vertex(-1, Point(0f, 0f)) }

    var numVerts: Int = 0
        private set

    private val groups: MutableMap<GroupId, Graph> = HashMap()

    init {
        groups[MAIN] = Graph(initialGroupCapacity)
    }

    fun pos(v: Int): Point {
        require(v in 0 until numVerts) { "vertex $v out of range [0, $numVerts)" }
        return vertices[v].pos
    }

    fun vec(v: Int): Vec2 {
        val p = pos(v)
        return Vec2(p.x, p.y)
    }

    fun x(v: Int): Float {
        return pos(v).x
    }

    fun y(v: Int): Float {
        return pos(v).y
    }

    fun setPos(v: Int, p: Point) {
        require(v in 0 until numVerts) { "vertex $v out of range [0, $numVerts)" }
        vertices[v].pos = p
    }

    fun setPos(v: Int, x: Float, y: Float) = setPos(v, Point(x, y))

    /** Returns the named group. Throws if the group does not exist. */
    fun group(name: GroupId = MAIN): Graph =
        groups[name] ?: error("no group named '$name'")

    /** Returns the named group, creating it if missing. */
    fun ensureGroup(name: GroupId): Graph =
        groups.getOrPut(name) { Graph() }

    fun groupNames(): Set<GroupId> = groups.keys

    /** Adds a new vertex to the pool. On success, [MutationResult.newVert] is the new vertex ID. */
    fun addVert(p: Point): MutationResult {
        val v = addVertId(p) ?: return MutationResult.Failure
        return MutationResult.ok(v)
    }

    fun addVert(x: Float, y: Float): MutationResult = addVert(Point(x, y))

    private fun addVertId(p: Point): Int? {
        if (numVerts >= maxVertices) return null
        val v = numVerts
        vertices[v] = Vertex(v, p)
        numVerts = v + 1
        return v
    }

    /**
     * Adds an edge `(a, b)` to [group] (creating the group lazily). Fails if
     * `a == b`, the edge already exists, or either ID is out of range.
     */
    fun addEdge(a: Int, b: Int, group: GroupId = MAIN): MutationResult =
        if (addEdgeRaw(a, b, group)) MutationResult.ok() else MutationResult.Failure

    private fun addEdgeRaw(a: Int, b: Int, group: GroupId = MAIN): Boolean =
        a != b && a in 0 until numVerts && b in 0 until numVerts && ensureGroup(group).add(a, b)

    /** Removes edge `(a, b)` from [group]. Fails if the group does not exist or the edge is absent. */
    fun delEdge(a: Int, b: Int, group: GroupId = MAIN): MutationResult =
        if (delEdgeRaw(a, b, group)) MutationResult.ok() else MutationResult.Failure

    private fun delEdgeRaw(a: Int, b: Int, group: GroupId = MAIN): Boolean {
        val g = groups[group] ?: return false
        return g.del(a, b)
    }

    /**
     * Moves vertex [v]. With [relative]=true (default), the vector `p` is added
     * to the current position; otherwise the position is set absolutely. Returns
     * [MutationResult.Failure] if `v` is out of range.
     */
    fun moveVert(v: Int, p: Point, relative: Boolean = true): MutationResult =
        if (moveVertRaw(v, p, relative)) MutationResult.ok() else MutationResult.Failure

    private fun moveVertRaw(v: Int, p: Point, relative: Boolean = true): Boolean {
        if (v !in 0 until numVerts) return false
        val vertex = vertices[v]
        val current = vertex.pos
        if (relative) {
            vertex.pos = Point(current.x + p.x, current.y + p.y)
        } else {
            vertex.pos = p
        }
        return true
    }

    fun edgeLength(a: Int, b: Int): Float = dist(vertices[a].pos, vertices[b].pos)

    fun edgeLength(e: Edge): Float = edgeLength(e.a, e.b)

    /**
     * Adds a new vertex offset by [p] from [v] (when [relative], the default) or
     * at the absolute position [p] otherwise, and connects it to [v] in [group].
     * On success, [MutationResult.newVert] is the new vertex ID. Fails on
     * capacity / invalid `v`.
     */
    fun appendEdge(v: Int, p: Point, group: GroupId = MAIN, relative: Boolean = true): MutationResult {
        val w = appendEdgeId(v, p, group, relative) ?: return MutationResult.Failure
        return MutationResult.ok(w)
    }

    private fun appendEdgeId(v: Int, p: Point, group: GroupId = MAIN, relative: Boolean = true): Int? {
        if (v !in 0 until numVerts) return null
        val current = vertices[v].pos
        val px = if (relative) current.x + p.x else p.x
        val py = if (relative) current.y + p.y else p.y
        val w = addVertId(Point(px, py)) ?: return null
        ensureGroup(group).add(v, w)
        return w
    }

    /**
     * Adds two new vertices at [pa] and [pb] and an edge between them in [group].
     * On success, [MutationResult.newVerts] contains `(a, b)` IDs. Fails if
     * [maxVertices] is reached for either.
     */
    fun vaddEdge(pa: Point, pb: Point, group: GroupId = MAIN): MutationResult {
        val ab = vaddEdgeIds(pa, pb, group) ?: return MutationResult.Failure
        return MutationResult.ok(ab.first, ab.second)
    }

    private fun vaddEdgeIds(pa: Point, pb: Point, group: GroupId = MAIN): Pair<Int, Int>? {
        val a = addVertId(pa) ?: return null
        val b = addVertId(pb) ?: run {
            numVerts -= 1
            return null
        }
        ensureGroup(group).add(a, b)
        return a to b
    }

    /**
     * Inserts a new vertex into the middle of edge `(a, b)`, replacing it with
     * `(a, mid)` and `(mid, b)`. On success, [MutationResult.newVert] is the new
     * midpoint vertex ID. Fails if the edge does not exist or [maxVertices] is
     * reached. With [at] set, the new vertex is placed at [at] instead of the midpoint.
     */
    fun splitEdge(a: Int, b: Int, at: Point? = null, group: GroupId = MAIN): MutationResult {
        val mid = splitEdgeId(a, b, at, group) ?: return MutationResult.Failure
        return MutationResult.ok(mid)
    }

    private fun splitEdgeId(a: Int, b: Int, at: Point? = null, group: GroupId = MAIN): Int? {
        val g = groups[group] ?: return null
        if (!g.mem(a, b)) return null
        val pa = vertices[a].pos
        val pb = vertices[b].pos
        val mx = at?.x ?: ((pa.x + pb.x) * 0.5f)
        val my = at?.y ?: ((pa.y + pb.y) * 0.5f)
        val mid = addVertId(Point(mx, my)) ?: return null
        g.del(a, b)
        g.add(a, mid)
        g.add(mid, b)
        return mid
    }

    /**
     * Conditional append: adds a new vertex offset by [p] from [v] (when
     * [relative], the default) or at absolute [p] otherwise, plus an edge
     * `(v, new)` only if the proposed segment satisfies [mustIntersect].
     *
     *  - `mustIntersect=false` (default): succeeds only
     *    when the new segment does **not** cross any existing edge in [group].
     *  - `mustIntersect=true`: succeeds only when it **does** cross at least one
     *    existing edge in [group].
     *
     * On success, [MutationResult.newVert] is the new vertex ID.
     */
    fun appendEdgeSegX(
        v: Int,
        p: Point,
        group: GroupId = MAIN,
        mustIntersect: Boolean = false,
        relative: Boolean = true,
    ): MutationResult {
        val w = appendEdgeSegXId(v, p, group, mustIntersect, relative) ?: return MutationResult.Failure
        return MutationResult.ok(w)
    }

    private fun appendEdgeSegXId(
        v: Int,
        p: Point,
        group: GroupId = MAIN,
        mustIntersect: Boolean = false,
        relative: Boolean = true,
    ): Int? {
        if (v !in 0 until numVerts) return null
        val g = groups[group]
        val a = vertices[v].pos
        val ax = a.x; val ay = a.y
        val bx = if (relative) ax + p.x else p.x
        val by = if (relative) ay + p.y else p.y

        var crosses = false
        if (g != null) {
            for (e in g.edges()) {
                if (e.a == v || e.b == v) continue
                val c = vertices[e.a].pos
                val d = vertices[e.b].pos
                val cx = c.x; val cy = c.y
                val dx = d.x; val dy = d.y
                if (segmentsCross(ax, ay, bx, by, cx, cy, dx, dy)) {
                    crosses = true
                    break
                }
            }
        }

        if (mustIntersect != crosses) return null
        val w = addVertId(Point(bx, by)) ?: return null
        ensureGroup(group).add(v, w)
        return w
    }

    companion object {
        val MAIN: GroupId = GroupId("main")
    }
}

private data class Vertex(val id: Int, var pos: Point)
