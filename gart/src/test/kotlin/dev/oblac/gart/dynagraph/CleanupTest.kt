package dev.oblac.gart.dynagraph

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CleanupTest {

    @Test
    fun mergeCloseVerticesCollapsesNearbyClusters() {
        val graph = DynaGraph(maxVertices = 8)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(0.05f, 0f).newVert!!  // close to a
        val c = graph.addVert(10f, 0f).newVert!!
        graph.addEdge(a, c)
        graph.addEdge(b, c)

        val changed = graph.mergeCloseVertices(eps = 0.1f)

        assertTrue(changed > 0)
        // After merge: a and b folded into one (lowest ID = a). One edge a-c remains.
        assertEquals(1, graph.group().numEdges)
        assertTrue(graph.group().mem(a, c))
    }

    @Test
    fun mergeCloseVerticesDropsSelfLoops() {
        val graph = DynaGraph(maxVertices = 4)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(0.01f, 0f).newVert!!  // very close to a
        graph.addEdge(a, b)

        graph.mergeCloseVertices(eps = 0.1f)

        // Edge collapsed onto self → dropped.
        assertEquals(0, graph.group().numEdges)
    }

    @Test
    fun subdivideLongerThanSplitsLongEdges() {
        val graph = DynaGraph(maxVertices = 16)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(8f, 0f).newVert!!
        graph.addEdge(a, b)

        val splits = graph.subdivideLongerThan(maxLen = 1f)

        assertTrue(splits > 0)
        // Every remaining edge must be <= maxLen.
        for (e in graph.group().edges()) {
            assertTrue(graph.edgeLength(e.a, e.b) <= 1f + 1e-4f)
        }
    }

    @Test
    fun pruneSmallComponentsRemovesSpecks() {
        val graph = DynaGraph(maxVertices = 8)
        // Big component: 4 verts in a chain.
        val v0 = graph.addVert(0f, 0f).newVert!!
        val v1 = graph.addVert(1f, 0f).newVert!!
        val v2 = graph.addVert(2f, 0f).newVert!!
        val v3 = graph.addVert(3f, 0f).newVert!!
        graph.addEdge(v0, v1); graph.addEdge(v1, v2); graph.addEdge(v2, v3)
        // Speck: 2-vert component.
        val s0 = graph.addVert(100f, 0f).newVert!!
        val s1 = graph.addVert(101f, 0f).newVert!!
        graph.addEdge(s0, s1)

        val removed = graph.pruneSmallComponents(minSize = 3)

        assertEquals(1, removed)
        assertEquals(3, graph.group().numEdges)
        assertEquals(1, graph.connectedComponents().size)
    }

    @Test
    fun mergeShortEdgesCollapsesUntilDone() {
        val graph = DynaGraph(maxVertices = 8)
        // Chain of 4 vertices, each 0.5 apart — all edges below threshold.
        val v0 = graph.addVert(0f, 0f).newVert!!
        val v1 = graph.addVert(0.5f, 0f).newVert!!
        val v2 = graph.addVert(1f, 0f).newVert!!
        val v3 = graph.addVert(1.5f, 0f).newVert!!
        graph.addEdge(v0, v1); graph.addEdge(v1, v2); graph.addEdge(v2, v3)

        graph.mergeShortEdges(minLen = 100f)  // merge everything

        // All collapsed onto v0 (lowest ID); no edges remain.
        assertEquals(0, graph.group().numEdges)
    }

    @Test
    fun simplifyChainDropsCollinearVertices() {
        val graph = DynaGraph(maxVertices = 8)
        // Chain of 4 collinear points: should reduce to 2 endpoints.
        val v0 = graph.addVert(0f, 0f).newVert!!
        val v1 = graph.addVert(1f, 0f).newVert!!
        val v2 = graph.addVert(2f, 0f).newVert!!
        val v3 = graph.addVert(3f, 0f).newVert!!
        graph.addEdge(v0, v1); graph.addEdge(v1, v2); graph.addEdge(v2, v3)

        val removed = graph.simplifyChain(eps = 0.001f)

        assertEquals(2, removed)
        assertEquals(1, graph.group().numEdges) // single edge v0-v3
        assertTrue(graph.group().mem(v0, v3))
    }

    @Test
    fun simplifyChainKeepsCornerVertex() {
        val graph = DynaGraph(maxVertices = 8)
        // L-shape: corner at (5, 0); should be kept.
        val v0 = graph.addVert(0f, 0f).newVert!!
        val v1 = graph.addVert(5f, 0f).newVert!!
        val v2 = graph.addVert(5f, 5f).newVert!!
        graph.addEdge(v0, v1); graph.addEdge(v1, v2)

        val removed = graph.simplifyChain(eps = 0.5f)

        assertEquals(0, removed) // corner detected, all kept
    }
}
