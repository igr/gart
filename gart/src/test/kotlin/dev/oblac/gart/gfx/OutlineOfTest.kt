package dev.oblac.gart.gfx

import org.jetbrains.skia.PathBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutlineOfTest {

    @Test
    fun `constant half width along a horizontal line is a rectangle`() {
        val xs = floatArrayOf(0f, 10f, 20f, 30f)
        val ys = floatArrayOf(5f, 5f, 5f, 5f)
        val hw = floatArrayOf(2f, 2f, 2f, 2f)
        val path = outlineOf(xs, ys, hw)
        val b = path.bounds
        assertEquals(0f, b.left)
        assertEquals(30f, b.right)
        assertEquals(3f, b.top)
        assertEquals(7f, b.bottom)
        // up one side, back down the other: every sample twice
        assertEquals(8, path.points().size)
    }

    @Test
    fun `tapered ends run to a point`() {
        val xs = floatArrayOf(0f, 10f, 20f)
        val ys = floatArrayOf(0f, 0f, 0f)
        val hw = floatArrayOf(0f, 3f, 0f)
        val pts = outlineOf(xs, ys, hw).points()
        // the first and last samples collapse onto the centreline, the middle one is offset to
        // the normal side (tangent turned a quarter clockwise: +y for a rightward line) going
        // out, and to the other side coming back
        assertEquals(0f, pts[0].y)
        assertEquals(3f, pts[1].y)
        assertEquals(0f, pts[2].y)
        assertEquals(-3f, pts[4].y)
    }

    @Test
    fun `only the first n samples are read`() {
        val xs = floatArrayOf(0f, 10f, 20f, 999f)
        val ys = floatArrayOf(0f, 0f, 0f, 999f)
        val hw = floatArrayOf(1f, 1f, 1f, 999f)
        val b = outlineOf(xs, ys, hw, 3).bounds
        assertEquals(20f, b.right)
        assertEquals(1f, b.bottom)
    }

    @Test
    fun `fewer than two samples is an empty path`() {
        assertTrue(outlineOf(floatArrayOf(1f), floatArrayOf(1f), floatArrayOf(1f)).isEmpty)
        assertTrue(outlineOf(FloatArray(0), FloatArray(0), FloatArray(0)).isEmpty)
    }

    @Test
    fun `lineOrMove starts a contour and then extends it`() {
        val pb = PathBuilder()
        for (i in 0 until 3) pb.lineOrMove(i == 0, i * 10f, 0f)
        val path = pb.detach()
        assertEquals(3, path.points().size)
        assertEquals(1, path.verbs.count { it == org.jetbrains.skia.PathVerb.MOVE })
    }
}
