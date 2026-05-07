package dev.oblac.gart.gfx

import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimplifyPointsTest {

    @Test
    fun lessThanThreePointsPassesThrough() {
        val pts = listOf(Point(0f, 0f), Point(1f, 1f))
        assertEquals(pts, simplifyPoints(pts, eps = 0.5f))
    }

    @Test
    fun collinearPointsCollapseToEndpoints() {
        val pts = listOf(
            Point(0f, 0f),
            Point(1f, 0f),
            Point(2f, 0f),
            Point(3f, 0f),
            Point(4f, 0f),
        )
        val out = simplifyPoints(pts, eps = 0.01f)
        assertEquals(listOf(Point(0f, 0f), Point(4f, 0f)), out)
    }

    @Test
    fun cornerVertexIsRetained() {
        val pts = listOf(
            Point(0f, 0f),
            Point(5f, 0f),
            Point(5f, 5f),
        )
        val out = simplifyPoints(pts, eps = 0.5f)
        assertEquals(pts, out)
    }

    @Test
    fun smallWobbleBelowEpsIsDropped() {
        val pts = listOf(
            Point(0f, 0f),
            Point(1f, 0.1f),  // tiny perpendicular offset
            Point(2f, 0f),
            Point(3f, 0.05f),
            Point(4f, 0f),
        )
        val out = simplifyPoints(pts, eps = 0.5f)
        assertEquals(2, out.size)
    }

    @Test
    fun zeroEpsKeepsEverything() {
        val pts = listOf(
            Point(0f, 0f),
            Point(1f, 0.001f),
            Point(2f, 0f),
        )
        val out = simplifyPoints(pts, eps = 0f)
        assertEquals(pts.size, out.size)
    }

    @Test
    fun closedLoopSeedsFromFarthestPair() {
        // Square with extra collinear point on the top edge — that point
        // should be dropped while the four corners are preserved.
        val pts = listOf(
            Point(0f, 0f),  // bottom-left
            Point(10f, 0f), // bottom-right
            Point(10f, 10f),// top-right
            Point(5f, 10f), // top-mid (collinear; should be dropped)
            Point(0f, 10f), // top-left
        )
        val out = simplifyPoints(pts, eps = 0.5f, closed = true)
        assertEquals(4, out.size)
        assertTrue(Point(5f, 10f) !in out)
    }

    @Test
    fun resultIsAlwaysASubsequence() {
        val pts = listOf(
            Point(0f, 0f), Point(1f, 0.05f), Point(2f, 0f),
            Point(3f, 5f), Point(4f, 0f), Point(5f, 0.05f), Point(6f, 0f),
        )
        val out = simplifyPoints(pts, eps = 0.5f)
        // Every output point exists in the input, in order.
        var idx = 0
        for (p in out) {
            while (idx < pts.size && pts[idx] != p) idx += 1
            assertTrue(idx < pts.size, "output is not a subsequence of input")
            idx += 1
        }
    }
}
