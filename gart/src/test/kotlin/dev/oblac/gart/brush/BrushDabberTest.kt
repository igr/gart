package dev.oblac.gart.brush

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.jetbrains.skia.Color
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrushDabberTest {

    private val w = 120
    private val h = 120

    private val tight = Brush(weight = 4f, scatter = 0f, opacity = 0.9f, spacing = 0.3f, pressure = Pressure.Flat)

    private fun render(seed: Int, brush: Brush, size: Float = 1f, dabs: (BrushDabber) -> Unit): IntArray {
        val g = Gartvas(Dimension(w, h))
        g.canvas.clear(Color.WHITE)
        dabs(BrushDabber(g.canvas, brush, Color.BLACK, size, Random(seed)))
        return Gartmap(g).use { it.pixels.copyOf() }
    }

    private fun inked(px: IntArray): List<Pair<Int, Int>> =
        px.indices.filter { px[it] != Color.WHITE }.map { it % w to it / w }

    @Test
    fun oneDabLandsWhereItIsPut() {
        val ink = inked(render(1, tight) { it.dab(60f, 60f) })
        assertTrue(ink.size in 8..30, "expected a 4 px dot, got ${ink.size} px")
        for ((x, y) in ink) assertTrue(abs(x - 60) <= 3 && abs(y - 60) <= 3, "ink off the spot at $x,$y")
    }

    @Test
    fun sameSeedSamePixels() {
        val run: (BrushDabber) -> Unit = { d -> for (i in 0 until 40) d.dab(20f + i * 2f, 60f, 0.6f + 0.4f * (i % 3)) }
        val a = render(7, Brushes.pencil2B, 3f, run)
        val b = render(7, Brushes.pencil2B, 3f, run)
        val c = render(8, Brushes.pencil2B, 3f, run)
        assertTrue(a.contentEquals(b))
        assertFalse(a.contentEquals(c))
    }

    @Test
    fun pressureScalesTheDot() {
        val light = inked(render(1, tight) { it.dab(60f, 60f, 0.5f) }).size
        val full = inked(render(1, tight) { it.dab(60f, 60f, 1f) }).size
        assertTrue(light < full, "light $light px, full $full px")
        assertEquals(0, inked(render(1, tight) { it.dab(60f, 60f, 0f) }).size)
    }

    @Test
    fun everyStockBrushDabsInk() {
        for ((name, brush) in Brushes.all) {
            val px = render(5, brush, 3f) { d -> for (i in 0 until 30) d.dab(20f + i * 2.5f, 60f) }
            assertTrue(inked(px).size > 20, "$name left ${inked(px).size} px")
        }
    }
}
