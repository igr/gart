package dev.oblac.gart.dynagraph

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MstTest {

    private val MST = GroupId("mst")

    @Test
    fun emptyGraphAddsNothing() {
        val graph = DynaGraph(maxVertices = 4)
        assertEquals(0, graph.minimumSpanningTree(MST))
    }

    @Test
    fun singleVertexAddsNothing() {
        val graph = DynaGraph(maxVertices = 4)
        graph.addVert(0f, 0f)
        assertEquals(0, graph.minimumSpanningTree(MST))
    }

    @Test
    fun spansAllVertices() {
        // Five points; an MST has exactly n-1 edges.
        val graph = DynaGraph(maxVertices = 8)
        graph.addVert(0f, 0f); graph.addVert(10f, 0f)
        graph.addVert(0f, 10f); graph.addVert(10f, 10f)
        graph.addVert(20f, 5f)

        val added = graph.minimumSpanningTree(MST)

        assertEquals(4, added)
        assertEquals(1, graph.connectedComponents(MST).size)
    }

    @Test
    fun pickedEdgesMinimizeTotalLength() {
        // Square with 4 unit edges and 2 diagonal sqrt(2) edges; MST should
        // pick exactly 3 unit edges, never a diagonal.
        val graph = DynaGraph(maxVertices = 4)
        graph.addVert(0f, 0f); graph.addVert(1f, 0f)
        graph.addVert(1f, 1f); graph.addVert(0f, 1f)

        graph.minimumSpanningTree(MST)

        val totalLen = graph.edgeLengths(MST).sum()
        // Three unit edges = 3.0 (within fastSqrt tolerance).
        assertTrue(kotlin.math.abs(totalLen - 3f) < 0.05f)
    }

    @Test
    fun maxLenProducesForestNotSingleTree() {
        // Two clusters far apart: each cluster gets its own tree.
        val graph = DynaGraph(maxVertices = 8)
        // Cluster A near origin
        graph.addVert(0f, 0f); graph.addVert(1f, 0f); graph.addVert(0f, 1f)
        // Cluster B far away
        graph.addVert(100f, 100f); graph.addVert(101f, 100f); graph.addVert(100f, 101f)

        graph.minimumSpanningTree(MST, maxLen = 5f)

        val comps = graph.connectedComponents(MST)
        assertEquals(2, comps.size)
        assertEquals(3, comps[0].size)
        assertEquals(3, comps[1].size)
    }

    @Test
    fun targetGroupIsAddedTo() {
        val graph = DynaGraph(maxVertices = 4)
        graph.addVert(0f, 0f); graph.addVert(1f, 0f); graph.addVert(2f, 0f)

        graph.minimumSpanningTree(MST)

        // MST went into MST group, not MAIN.
        assertTrue(MST in graph.groupIds())
        assertEquals(0, graph.group().numEdges)
        assertEquals(2, graph.group(MST).numEdges)
    }
}
