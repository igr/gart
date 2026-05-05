package dev.oblac.gart.dynagraph

/**
 * Undirected edge with canonical ordering (`a <= b`) so equality and
 * hashing are symmetric across the pair.
 */
data class Edge(val a: Int, val b: Int) {
    companion object {
        fun of(a: Int, b: Int): Edge = if (a <= b) Edge(a, b) else Edge(b, a)
    }
}

/**
 * Undirected graph backed by an adjacency-list. Vertex IDs are arbitrary `Int`
 * values supplied by the caller; `Graph` itself is position-agnostic: geometry
 * lives on `DynaGraph`.
 */
class Graph(initialCapacity: Int = 16) {
    private val topo: MutableMap<Int, IntNeighbors> = HashMap(initialCapacity)

    var numEdges: Int = 0
        private set

    /** True if this graph represents a closed cycle (set by builders). */
    var closed: Boolean = false
        internal set

    val numVertices: Int get() = topo.size

    fun vertices(): Set<Int> = topo.keys

    fun vmem(v: Int): Boolean = topo.containsKey(v)

    fun mem(a: Int, b: Int): Boolean = topo[a]?.contains(b) == true

    fun add(a: Int, b: Int): Boolean {
        if (a == b) return false
        val sa = topo.getOrPut(a) { IntNeighbors() }
        if (!sa.addValue(b)) return false
        topo.getOrPut(b) { IntNeighbors() }.addValue(a)
        numEdges += 1
        return true
    }

    fun del(a: Int, b: Int): Boolean {
        val sa = topo[a] ?: return false
        if (!sa.removeValue(b)) return false
        if (sa.isEmpty()) topo.remove(a)
        val sb = topo[b]
        if (sb != null) {
            sb.removeValue(a)
            if (sb.isEmpty()) topo.remove(b)
        }
        numEdges -= 1
        return true
    }

    fun neighbors(v: Int): Set<Int> = topo[v] ?: emptySet()

    /** All unique edges in canonical (a < b) order. */
    fun edges(): Sequence<Edge> = sequence {
        for ((a, ns) in topo) {
            for (b in ns) {
                if (a < b) yield(Edge(a, b))
            }
        }
    }

    /** All edges incident to `v`. Yields each as `Edge.of(v, w)`. */
    fun incidentEdges(v: Int): Sequence<Edge> = sequence {
        val ns = topo[v] ?: return@sequence
        for (w in ns) yield(Edge.of(v, w))
    }

    /** Invokes [block] once per unique edge. */
    inline fun forEachEdge(block: (Int, Int) -> Unit) {
        for (e in edges()) block(e.a, e.b)
    }

    /**
     * Returns the ordered cycle of vertex IDs if this graph is a single closed
     * loop (every vertex has degree 2 and the graph is connected); returns null
     * otherwise. Used by builders that operate on closed paths.
     */
    fun loop(): List<Int>? {
        if (numVertices < 3 || numEdges != numVertices) return null
        for (ns in topo.values) if (ns.size != 2) return null

        val start = topo.keys.first()
        val out = ArrayList<Int>(numVertices)
        out.add(start)
        var prev = -1
        var cur = start
        while (true) {
            val next = topo.getValue(cur).firstExcept(prev)
                ?: return null
            if (next == start) {
                return if (out.size == numVertices) out else null
            }
            out.add(next)
            prev = cur
            cur = next
            if (out.size > numVertices) return null
        }
    }
}

private class IntNeighbors(initialCapacity: Int = 4) : AbstractSet<Int>() {
    private var data = IntArray(initialCapacity)

    override var size: Int = 0
        private set

    override fun contains(element: Int): Boolean = indexOf(element) >= 0

    override fun iterator(): Iterator<Int> = object : IntIterator() {
        private var index = 0

        override fun hasNext(): Boolean = index < size

        override fun nextInt(): Int {
            if (!hasNext()) throw NoSuchElementException()
            return data[index++]
        }
    }

    fun addValue(value: Int): Boolean {
        if (contains(value)) return false
        ensureCapacity(size + 1)
        data[size++] = value
        return true
    }

    fun removeValue(value: Int): Boolean {
        val index = indexOf(value)
        if (index < 0) return false
        val lastIndex = size - 1
        data[index] = data[lastIndex]
        size = lastIndex
        return true
    }

    fun firstExcept(value: Int): Int? {
        for (i in indices) {
            val candidate = data[i]
            if (candidate != value) return candidate
        }
        return null
    }

    private fun indexOf(value: Int): Int {
        for (i in indices) {
            if (data[i] == value) return i
        }
        return -1
    }

    private fun ensureCapacity(required: Int) {
        if (required <= data.size) return
        data = data.copyOf(data.size * 2)
    }
}
