package dev.oblac.gart.dynagraph

import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynaGraphTest {

    @Test
    fun storesAndMovesPoints() {
        val graph = DynaGraph(maxVertices = 4)
        val v = graph.addVert(Point(1f, 2f)).newVert!!

        assertEquals(Point(1f, 2f), graph.pos(v))

        assertTrue(graph.moveVert(v, Point(3f, -1f)).isOk)
        assertEquals(Point(4f, 1f), graph.pos(v))

        graph.setPos(v, 7f, 8f)
        assertEquals(Point(7f, 8f), graph.pos(v))
    }

    @Test
    fun appendAndSplitUseStoredPoints() {
        val graph = DynaGraph(maxVertices = 4)
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.appendEdge(a, Point(3f, 4f)).newVert!!

        assertEquals(Point(3f, 4f), graph.pos(b))
        assertEquals(5f, graph.edgeLength(a, b), 0.01f)

        val mid = graph.splitEdge(a, b).newVert!!

        assertEquals(Point(1.5f, 2f), graph.pos(mid))
    }

    @Test
    fun mutatingMethodsReturnResults() {
        val graph = DynaGraph(maxVertices = 3)

        val results = listOf(
            graph.addVert(Point(0f, 0f)),
            graph.addVert(Point(2f, 0f)),
            graph.addEdge(0, 1),
            graph.splitEdge(0, 1),
        )

        assertTrue(results.all { it.isOk })
        assertEquals(Point(1f, 0f), graph.pos(2))
        assertEquals(listOf(2), results[3].newVerts.toList())
    }

    @Test
    fun customGroupsUseGroupIds() {
        val graph = DynaGraph(maxVertices = 2)
        val group = GroupId("rng")
        val a = graph.addVert(0f, 0f).newVert!!
        val b = graph.addVert(1f, 0f).newVert!!

        assertTrue(graph.addEdge(a, b, group).isOk)

        assertTrue(group in graph.groupNames())
        assertEquals(setOf(b), graph.group(group).neighbors(a))
        assertEquals(0, graph.group().numEdges)
    }
}
