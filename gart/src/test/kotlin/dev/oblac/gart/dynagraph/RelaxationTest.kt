package dev.oblac.gart.dynagraph

import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelaxationTest {

    @Test
    fun relaxSpringsConvergesTowardRestLength() {
        val graph = DynaGraph(maxVertices = 4)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(20f, 0f).newVert!!
        graph.addEdge(a, b)

        repeat(100) { graph.relaxSprings(restLen = 5f, k = 0.4f) }

        assertEquals(5f, graph.edgeLength(a, b), 1e-2f)
    }

    @Test
    fun relaxSpringsBothEndpointsMoveSymmetrically() {
        val graph = DynaGraph(maxVertices = 4)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(10f, 0f).newVert!!
        graph.addEdge(a, b)

        graph.relaxSprings(restLen = 4f, k = 0.5f)

        // dx_a should equal -dx_b (symmetric force)
        val moveA = graph.x(a)
        val moveB = 10f - graph.x(b)
        assertEquals(moveA, moveB, 1e-4f)
    }

    @Test
    fun smoothLaplacianMovesTowardCentroid() {
        // V-shape: center vertex sits below the line between its two neighbors.
        // Laplacian should pull it up toward the midpoint.
        val graph = DynaGraph(maxVertices = 8)
        val left = graph.addVert(0f, 10f).newVert!!
        val mid = graph.addVert(5f, 0f).newVert!!
        val right = graph.addVert(10f, 10f).newVert!!
        graph.addEdge(left, mid)
        graph.addEdge(mid, right)

        val before = graph.y(mid)
        graph.smoothLaplacian(alpha = 0.5f)
        val after = graph.y(mid)

        // Centroid of neighbors at y=10; mid was at y=0; alpha=0.5 → halfway.
        assertEquals(5f, after, 1e-4f)
        assertTrue(after > before, "vertex moved toward neighbors")
    }

    @Test
    fun repelVerticesPushesClosePairsApart() {
        val graph = DynaGraph(maxVertices = 4)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(2f, 0f).newVert!!
        // Put both in a group for the function to find them.
        graph.addEdge(a, b)
        val before = graph.edgeLength(a, b)

        graph.repelVertices(radius = 5f, strength = 0.5f)

        assertTrue(graph.edgeLength(a, b) > before, "close pair pushed apart")
    }

    @Test
    fun pinnedAnchorsHoldFixedThroughRelaxation() {
        val graph = DynaGraph(maxVertices = 8)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(3f, 0f).newVert!! // offset toward a
        val c = graph.addVert(20f, 0f).newVert!!
        graph.addEdge(a, b)
        graph.addEdge(b, c)

        graph.pinned(a, c) {
            repeat(100) { graph.relaxSprings(restLen = 1f, k = 0.4f) }
        }

        // Anchors must not move regardless of force.
        assertEquals(Point(0f, 0f), graph.point(a))
        assertEquals(Point(20f, 0f), graph.point(c))
        // b should be pulled away from the off-center start toward the
        // anchor-balanced midpoint.
        assertTrue(graph.x(b) > 3f)
        assertTrue(abs(graph.x(b) - 10f) < 5f)
    }
}
