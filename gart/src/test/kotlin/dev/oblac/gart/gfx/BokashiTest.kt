package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.red
import org.jetbrains.skia.Color
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BokashiTest {

    private val w = 100
    private val h = 200

    private fun render(draw: (Gartvas) -> Unit): IntArray {
        val g = Gartvas(Dimension(w, h))
        g.canvas.clear(Color.WHITE)
        draw(g)
        return Gartmap(g).use { it.pixels.copyOf() }
    }

    // how much ink a row carries, 0 white .. 255 black, averaged across the row
    private fun rowInk(px: IntArray, y: Int): Float = (0 until w).map { 255 - red(px[y * w + it]) }.average().toFloat()

    private fun inked(px: IntArray) = px.indices.filter { px[it] != Color.WHITE }.map { it % w to it / w }

    private val whole: Path = Rect.makeWH(w.toFloat(), h.toFloat()).path()

    @Test
    fun `fade is full at y0, gone at y1 and clamped beyond both`() {
        val b = Bokashi(Color.BLACK, y0 = 10f, y1 = 110f, fall = 1f)
        assertEquals(1f, b.at(10f))
        assertEquals(1f, b.at(-50f))
        assertEquals(0f, b.at(110f))
        assertEquals(0f, b.at(500f))
        assertEquals(0.5f, b.at(60f), 1e-6f)
    }

    @Test
    fun `fall shapes the curve and waver stretches it`() {
        val linear = Bokashi(Color.BLACK, 0f, 100f, fall = 1f)
        val steep = Bokashi(Color.BLACK, 0f, 100f, fall = 3f)
        assertTrue(steep.at(50f) < linear.at(50f))
        // waver 2 runs the wash twice as far, so the old end is only half way
        assertEquals(0.5f, linear.at(100f, waver = 2f), 1e-6f)
    }

    @Test
    fun `a wash can run upward too`() {
        val up = Bokashi(Color.BLACK, y0 = 200f, y1 = 100f, fall = 1f)
        assertEquals(1f, up.at(200f))
        assertEquals(0.5f, up.at(150f), 1e-6f)
        assertEquals(0f, up.at(100f))
        assertEquals(0f, up.at(0f))
    }

    @Test
    fun `bad strength or fall is rejected`() {
        assertFailsWith<IllegalArgumentException> { Bokashi(Color.BLACK, 0f, 10f, strength = 1.5f) }
        assertFailsWith<IllegalArgumentException> { Bokashi(Color.BLACK, 0f, 10f, fall = 0f) }
    }

    @Test
    fun `drawBokashi grades from the edge and leaves the far side clean`() {
        val px = render { it.canvas.drawBokashi(whole, Bokashi(Color.BLACK, y0 = 0f, y1 = 100f, fall = 1f)) }
        val top = rowInk(px, 0)
        val mid = rowInk(px, 50)
        val end = rowInk(px, 99)
        assertTrue(top > 240f, "edge row should be near solid, got $top")
        assertTrue(mid in 100f..160f, "middle row should be about half, got $mid")
        assertTrue(end < 10f, "end row should be near clean, got $end")
        assertEquals(0f, rowInk(px, 150), "past y1 must stay untouched")
    }

    @Test
    fun `drawBokashi at zero strength draws nothing`() {
        val px = render { it.canvas.drawBokashi(whole, Bokashi(Color.BLACK, 0f, 100f, strength = 0f)) }
        assertTrue(inked(px).isEmpty())
    }

    @Test
    fun `dot screen stays inside the region and scales with strength`() {
        val region = Rect.makeLTRB(20f, 20f, 80f, 180f).path()
        val paint = fillOf(Color.BLACK)
        val full = render { it.canvas.drawDotScreen(region, paint, pitch = 8f, angle = Radians(0.3f), strength = 1f) { _, _ -> 1f } }
        val half = render { it.canvas.drawDotScreen(region, paint, pitch = 8f, angle = Radians(0.3f), strength = 0.5f) { _, _ -> 1f } }
        val none = render { it.canvas.drawDotScreen(region, paint, pitch = 8f, strength = 0f) { _, _ -> 1f } }
        val fullInk = inked(full)
        assertTrue(fullInk.size > 500, "expected a screen, got ${fullInk.size} px")
        for ((x, y) in fullInk) assertTrue(x in 20..79 && y in 20..179, "dot outside the region at $x,$y")
        assertTrue(inked(half).size < fullInk.size / 2, "half strength should be well under half the ink")
        assertTrue(inked(none).isEmpty())
    }

    @Test
    fun `bokashi screen carries the fade in dot size`() {
        val wash = Bokashi(Color.BLACK, y0 = 0f, y1 = 160f, fall = 1f)
        val px = render {
            it.canvas.drawBokashiScreen(whole, wash, fillOf(Color.BLACK), pitch = 8f, angle = Radians(0.48f), origin = Point(0f, 0f))
        }
        val near = inked(px).count { (_, y) -> y < 40 }
        val far = inked(px).count { (_, y) -> y in 120 until 160 }
        val past = inked(px).count { (_, y) -> y >= 170 }
        assertTrue(near > far * 3, "dots should shrink along the fade: near $near, far $far")
        assertEquals(0, past, "no dots past y1")
    }
}
