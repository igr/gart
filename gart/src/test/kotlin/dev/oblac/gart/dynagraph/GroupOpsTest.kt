package dev.oblac.gart.dynagraph

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupOpsTest {

    private val A = GroupId("A")
    private val B = GroupId("B")
    private val OUT = GroupId("out")

    private fun seed(): DynaGraph {
        val graph = DynaGraph(maxVertices = 8)
        repeat(4) { graph.addVert(it.toFloat(), 0f) }
        return graph
    }

    @Test
    fun clearGroupRemovesEveryEdge() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)

        val removed = graph.clearGroup(A)

        assertEquals(2, removed)
        assertEquals(0, graph.group(A).numEdges)
        assertFalse(graph.group(A).closed)
    }

    @Test
    fun clearGroupOnMissingReturnsZero() {
        val graph = seed()
        assertEquals(0, graph.clearGroup(GroupId("nope")))
    }

    @Test
    fun copyGroupAddsSourceEdgesIntoDestination() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(2, 3, A)

        val added = graph.copyGroup(A, B)

        assertEquals(2, added)
        assertTrue(graph.group(B).mem(0, 1))
        assertTrue(graph.group(B).mem(2, 3))
        // Source unchanged
        assertEquals(2, graph.group(A).numEdges)
    }

    @Test
    fun copyGroupSkipsDuplicatesAlreadyInDestination() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)
        graph.addEdge(0, 1, B) // already there

        val added = graph.copyGroup(A, B)

        assertEquals(1, added) // only (1, 2) was new
        assertEquals(2, graph.group(B).numEdges)
    }

    @Test
    fun copyGroupSelfIsNoOp() {
        val graph = seed()
        graph.addEdge(0, 1, A)

        assertEquals(0, graph.copyGroup(A, A))
        assertEquals(1, graph.group(A).numEdges)
    }

    @Test
    fun moveGroupClearsSource() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)

        val moved = graph.moveGroup(A, B)

        assertEquals(2, moved)
        assertEquals(0, graph.group(A).numEdges)
        assertEquals(2, graph.group(B).numEdges)
    }

    @Test
    fun unionIntoCombinesBothSources() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)
        graph.addEdge(1, 2, B) // overlap with A
        graph.addEdge(2, 3, B)

        val added = graph.unionInto(A, B, OUT)

        assertEquals(3, added) // (0,1) (1,2) (2,3)
        assertEquals(3, graph.group(OUT).numEdges)
    }

    @Test
    fun unionIntoSafeWhenDestinationOverlapsSource() {
        // into == a: should keep all of a + add b's new edges, no double-counts.
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)
        graph.addEdge(2, 3, B)

        val added = graph.unionInto(A, B, A)

        // Only the (2, 3) edge is new in A.
        assertEquals(1, added)
        assertEquals(3, graph.group(A).numEdges)
    }

    @Test
    fun intersectIntoKeepsCommonEdges() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)
        graph.addEdge(2, 3, A)
        graph.addEdge(1, 2, B)
        graph.addEdge(2, 3, B)

        val added = graph.intersectInto(A, B, OUT)

        assertEquals(2, added)
        assertTrue(graph.group(OUT).mem(1, 2))
        assertTrue(graph.group(OUT).mem(2, 3))
        assertFalse(graph.group(OUT).mem(0, 1))
    }

    @Test
    fun intersectIntoEmptyWhenNoOverlap() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(2, 3, B)

        assertEquals(0, graph.intersectInto(A, B, OUT))
    }

    @Test
    fun differenceIntoSubtractsB() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)
        graph.addEdge(2, 3, A)
        graph.addEdge(1, 2, B)

        val added = graph.differenceInto(A, B, OUT)

        assertEquals(2, added)
        assertTrue(graph.group(OUT).mem(0, 1))
        assertTrue(graph.group(OUT).mem(2, 3))
        assertFalse(graph.group(OUT).mem(1, 2))
    }

    @Test
    fun differenceIntoMissingBYieldsAllOfA() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)

        val added = graph.differenceInto(A, GroupId("missing"), OUT)

        assertEquals(2, added)
    }

    @Test
    fun symmetricDifferenceKeepsExclusiveEdges() {
        val graph = seed()
        graph.addEdge(0, 1, A)
        graph.addEdge(1, 2, A)
        graph.addEdge(1, 2, B) // shared
        graph.addEdge(2, 3, B)

        val added = graph.symmetricDifferenceInto(A, B, OUT)

        assertEquals(2, added)
        assertTrue(graph.group(OUT).mem(0, 1))
        assertTrue(graph.group(OUT).mem(2, 3))
        assertFalse(graph.group(OUT).mem(1, 2))
    }
}
