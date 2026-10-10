package dev.oblac.gart.pixels

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class BoxBlurTest {

    private val w = 31
    private val h = 23

    @Test
    fun `one pass is the mean of the clamped window`() {
        val src = FloatArray(w * h) { ((it * 7919) % 101) / 101f }
        val r = 3
        val out = boxBlur(src, w, h, r, passes = 1)
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0.0
            for (j in -r..r) for (i in -r..r) s += src[(y + j).coerceIn(0, h - 1) * w + (x + i).coerceIn(0, w - 1)]
            assertEquals((s / ((2 * r + 1) * (2 * r + 1))).toFloat(), out[y * w + x], 1e-5f, "at $x, $y")
        }
    }

    @Test
    fun `a flat field stays flat`() {
        for (v in boxBlur(FloatArray(w * h) { 0.4f }, w, h, 5)) assertEquals(0.4f, v, 1e-5f)
    }

    @Test
    fun `an impulse spreads evenly and keeps its mass`() {
        val src = FloatArray(w * h)
        val c = 11 * w + 15
        src[c] = 1f
        val out = boxBlur(src, w, h, 2) // three passes reach 6 px, that stays off the edges
        assertEquals(1f, out.sum(), 1e-5f)
        assertEquals(out[c - 4], out[c + 4], 1e-6f)
        assertEquals(out[c - 4 * w], out[c + 4 * w], 1e-6f)
        assertTrue(out[c] > out[c + 1])
    }

    @Test
    fun `src stays and no passes give a copy`() {
        val src = FloatArray(w * h) { it.toFloat() }
        val keep = src.copyOf()
        boxBlur(src, w, h, 4)
        assertTrue(keep.contentEquals(src))
        val copy = boxBlur(src, w, h, 4, passes = 0)
        assertNotSame(src, copy)
        assertTrue(src.contentEquals(copy))
    }
}
