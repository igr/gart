package dev.oblac.gart.dynagraph

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueriesTest {

    @Test
    fun degreeCountsNeighbors() {
        val graph = Graph()
        graph.add(0, 1)
        graph.add(0, 2)
        graph.add(0, 3)

        assertEquals(3, graph.degree(0))
        assertEquals(1, graph.degree(1))
        assertEquals(0, graph.degree(99))
    }

    @Test
    fun bfsYieldsReachableVerticesOnce() {
        val graph = Graph()
        // 0 - 1 - 2
        //     |
        //     3
        graph.add(0, 1)
        graph.add(1, 2)
        graph.add(1, 3)

        val order = graph.bfs(0).toList()
        assertEquals(4, order.size)
        assertEquals(0, order.first())
        assertEquals(setOf(0, 1, 2, 3), order.toSet())
    }

    @Test
    fun bfsOnUnknownVertexYieldsNothing() {
        val graph = Graph()
        graph.add(0, 1)
        assertEquals(emptyList(), graph.bfs(99).toList())
    }

    @Test
    fun connectedComponentsSplitsDisjointGraphs() {
        val graph = Graph()
        // Component A: 0-1-2
        graph.add(0, 1); graph.add(1, 2)
        // Component B: 3-4
        graph.add(3, 4)

        val comps = graph.connectedComponents()
        assertEquals(2, comps.size)
        assertEquals(3, comps[0].size) // sorted by size desc
        assertEquals(2, comps[1].size)
        assertEquals(setOf(0, 1, 2), comps[0].toSet())
        assertEquals(setOf(3, 4), comps[1].toSet())
    }

    @Test
    fun shortestPathFindsMinimumLengthRoute() {
        val graph = Graph()
        // Triangle plus a far-side dangler:
        // 0-1-2-0  3 attached at 2.
        graph.add(0, 1); graph.add(1, 2); graph.add(2, 0)
        graph.add(2, 3)

        val path = graph.shortestPath(0, 3)
        assertNotNull(path)
        assertEquals(0, path.first())
        assertEquals(3, path.last())
        // length 2 (0-2-3), not 3 (0-1-2-3)
        assertEquals(3, path.size)
    }

    @Test
    fun shortestPathReturnsNullWhenDisconnected() {
        val graph = Graph()
        graph.add(0, 1)
        graph.add(2, 3)

        assertNull(graph.shortestPath(0, 3))
    }

    @Test
    fun shortestPathSelfReturnsSingleton() {
        val graph = Graph()
        graph.add(0, 1)
        assertEquals(listOf(0), graph.shortestPath(0, 0))
    }

    @Test
    fun dynaGraphScopedQueriesUseGroup() {
        val graph = DynaGraph(maxVertices = 4)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(1f, 0f).newVert!!
        val c = graph.addVert(2f, 0f).newVert!!
        val grp = GroupId("test")
        graph.addEdge(a, b, grp)
        graph.addEdge(b, c, grp)

        assertEquals(2, graph.degree(b, grp))
        assertEquals(0, graph.degree(b)) // default MAIN group has no edges
        assertEquals(listOf(a, b, c), graph.shortestPath(a, c, grp))
    }
}
