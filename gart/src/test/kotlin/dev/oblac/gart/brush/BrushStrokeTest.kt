package dev.oblac.gart.brush

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrushStrokeTest {

    private val w = 200
    private val h = 100
    private val y0 = 50f

    private fun render(seed: Int, brush: Brush, size: Float = 2f, wobble: Wobble? = null, path: Path? = null): IntArray {
        val g = Gartvas(Dimension(w, h))
        g.canvas.clear(Color.WHITE)
        val p = path ?: PathBuilder().moveTo(20f, y0).lineTo(180f, y0).detach()
        g.canvas.drawBrush(p, brush, Color.BLACK, size, Random(seed), wobble)
        return Gartmap(g).use { it.pixels.copyOf() }
    }

    private fun inked(px: IntArray): List<Pair<Int, Int>> =
        px.indices.filter { px[it] != Color.WHITE }.map { it % w to it / w }

    private val tight = Brush(weight = 2f, scatter = 0f, opacity = 0.8f, spacing = 0.3f, pressure = Pressure.Flat)

    @Test
    fun sameSeedSamePixels() {
        val a = render(11, Brushes.pencil2B)
        val b = render(11, Brushes.pencil2B)
        val c = render(12, Brushes.pencil2B)
        assertTrue(a.contentEquals(b))
        assertFalse(a.contentEquals(c))
    }

    @Test
    fun noScatterStaysOnThePath() {
        val px = render(1, tight)
        val ink = inked(px)
        assertTrue(ink.size > 100, "expected ink, got ${ink.size} px")
        for ((x, y) in ink) {
            assertTrue(abs(y + 0.5f - y0) <= 2f + 1f, "ink off the line at $x,$y")
            assertTrue(x >= 17 && x <= 183, "ink past the ends at $x,$y")
        }
    }

    @Test
    fun wobbleDriftsOffThePath() {
        val px = render(1, tight, wobble = { _, _ -> 0.3f })
        val far = inked(px).count { (_, y) -> abs(y - y0) > 10f }
        assertTrue(far > 50, "expected drift, only $far px far from the line")
    }

    @Test
    fun emptyPathDrawsNothing() {
        val px = render(1, tight, path = Path())
        assertEquals(0, inked(px).size)
    }

    @Test
    fun everyStockBrushLeavesInk() {
        for ((name, brush) in Brushes.all) {
            val px = render(5, brush, size = 3f)
            assertTrue(inked(px).size > 20, "$name left ${inked(px).size} px")
        }
    }

    @Test
    fun customAndImageTipsLeaveInk() {
        val custom = Brush(
            tip = Tip.Custom { c, paint -> c.drawRect(org.jetbrains.skia.Rect(-0.5f, -0.1f, 0.5f, 0.1f), paint) },
            weight = 6f, scatter = 0f, opacity = 0.5f, spacing = 1f,
        )
        assertTrue(inked(render(2, custom)).size > 50)

        val stamp = Gartvas(Dimension(16, 16))
        stamp.canvas.drawCircle(8f, 8f, 6f, org.jetbrains.skia.Paint().apply { color = Color.RED })
        val image = Brush(tip = Tip.Image(stamp.snapshot()), weight = 6f, scatter = 0f, opacity = 0.5f, spacing = 1f)
        val px = render(2, image)
        val ink = inked(px)
        assertTrue(ink.size > 50)
        // the image is only a shape: the ink is the stroke colour, not the image's red
        for (i in px.indices) {
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            assertTrue(r <= g + 1, "red leaked through at ${i % w},${i / w}: ${Integer.toHexString(c)}")
        }
    }

    @Test
    fun polylineAndLineOverloads() {
        val g = Gartvas(Dimension(w, h))
        g.canvas.clear(Color.WHITE)
        g.canvas.drawBrush(listOf(Point(10f, 10f), Point(100f, 60f), Point(190f, 10f)), tight, Color.BLACK, 2f, Random(1))
        g.canvas.drawBrush(Point(10f, 90f), Point(190f, 90f), tight, Color.BLACK, 2f, Random(1))
        g.canvas.drawBrush(listOf(Point(10f, 10f)), tight, Color.BLACK, 2f, Random(1)) // nothing
        val ink = inked(Gartmap(g).use { it.pixels.copyOf() })
        assertTrue(ink.any { (x, y) -> abs(x - 100) < 3 && abs(y - 60) < 3 })
        assertTrue(ink.any { (x, y) -> abs(x - 100) < 3 && abs(y - 90) < 3 })
    }
}
