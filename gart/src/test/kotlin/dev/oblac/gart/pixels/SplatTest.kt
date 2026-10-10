package dev.oblac.gart.pixels

import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplatTest {

    private val w = 8
    private val h = 6

    @Test
    fun `a point on a whole pixel goes all to that pixel`() {
        val buf = FloatArray(w * h)
        splat(buf, w, 3f, 2f, 5f)
        assertEquals(5f, buf[2 * w + 3])
        assertEquals(5f, buf.sum())
    }

    @Test
    fun `a point between pixels is shared bilinearly`() {
        val buf = FloatArray(w * h)
        splat(buf, w, 3.25f, 2.5f, 1f)
        assertEquals(0.375f, buf[2 * w + 3], 1e-6f)
        assertEquals(0.125f, buf[2 * w + 4], 1e-6f)
        assertEquals(0.375f, buf[3 * w + 3], 1e-6f)
        assertEquals(0.125f, buf[3 * w + 4], 1e-6f)
        assertEquals(1f, buf.sum(), 1e-6f)
    }

    @Test
    fun `a point gives the same as its x and y`() {
        val a = FloatArray(w * h)
        val b = FloatArray(w * h)
        splat(a, w, 3.25f, 2.5f, 0.7f)
        splat(b, w, Point(3.25f, 2.5f), 0.7f)
        assertTrue(a.contentEquals(b))
    }

    @Test
    fun `shares off the field are dropped`() {
        val buf = FloatArray(w * h)
        splat(buf, w, w - 0.5f, 0f, 1f) // half of it is off the right edge
        assertEquals(0.5f, buf[w - 1], 1e-6f)
        assertEquals(0f, buf[w], "no wrap into the next row")
        splat(buf, w, -0.25f, 3f, 1f) // a quarter is off the left edge
        assertEquals(0.75f, buf[3 * w], 1e-6f)
        assertEquals(1.25f, buf.sum(), 1e-6f)
    }

    @Test
    fun `points off the field and NaN add nothing`() {
        val buf = FloatArray(w * h)
        splat(buf, w, -3f, 2f, 1f)
        splat(buf, w, 2f, h + 1f, 1f)
        splat(buf, w, Float.NaN, 2f, 1f)
        splat(buf, w, 2f, Float.NaN, 1f)
        assertTrue(buf.all { it == 0f })
    }
}
