package dev.oblac.gart.dynagraph

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphTest {

    @Test
    fun storesUndirectedEdgesWithoutDuplicates() {
        val graph = Graph()

        assertTrue(graph.add(1, 2))
        assertFalse(graph.add(1, 2))
        assertFalse(graph.add(2, 1))
        assertFalse(graph.add(1, 1))

        assertEquals(1, graph.numEdges)
        assertEquals(setOf(2), graph.neighbors(1))
        assertEquals(setOf(1), graph.neighbors(2))
        assertTrue(graph.mem(1, 2))
        assertTrue(graph.mem(2, 1))
    }

    @Test
    fun removesEmptyAdjacencyEntries() {
        val graph = Graph()

        graph.add(1, 2)
        graph.add(1, 3)

        assertTrue(graph.del(1, 2))
        assertEquals(1, graph.numEdges)
        assertEquals(setOf(3), graph.neighbors(1))
        assertFalse(graph.vmem(2))

        assertTrue(graph.del(1, 3))
        assertEquals(0, graph.numEdges)
        assertFalse(graph.vmem(1))
        assertFalse(graph.vmem(3))
    }
}
