package dev.oblac.gart.brush

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.PathBuilder
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertTrue

class NibTest {

    private val n = 120

    private fun dark(c: Int) = (c and 0xFF) < 128

    // one straight stroke through the middle, then the dark px on a line across it
    private fun inkAcross(brush: Brush, horizontal: Boolean): Int {
        val g = Gartvas(Dimension(n, n))
        g.canvas.clear(Color.WHITE)
        val m = n / 2f
        val path = if (horizontal) PathBuilder().moveTo(10f, m).lineTo(n - 10f, m).detach()
        else PathBuilder().moveTo(m, 10f).lineTo(m, n - 10f).detach()
        g.canvas.drawBrush(path, brush, Color.BLACK, 1f, Random(1))
        val px = Gartmap(g).use { it.pixels.copyOf() }
        return if (horizontal) (0 until n).count { dark(px[it * n + n / 2]) }
        else (0 until n).count { dark(px[n / 2 * n + it]) }
    }

    @Test
    fun aStrokeAcrossTheNibIsWideAndAStrokeAlongItIsAHairline() {
        // at angle 0 the nib lies along x, so a stroke down the page drags it sideways
        val flat = Brushes.nib(width = 20f, hair = 2f, angle = 0f).copy(pressure = Pressure.Flat)
        val across = inkAcross(flat, horizontal = false)
        val along = inkAcross(flat, horizontal = true)
        assertTrue(across in 18..22, "across the nib: $across px")
        assertTrue(along in 1..3, "along the nib: $along px")
    }

    @Test
    fun theAngleTurnsTheNib() {
        val flat = Brushes.nib(width = 20f, hair = 2f, angle = 90f).copy(pressure = Pressure.Flat)
        val across = inkAcross(flat, horizontal = true)
        val along = inkAcross(flat, horizontal = false)
        assertTrue(across in 18..22, "across the nib: $across px")
        assertTrue(along in 1..3, "along the nib: $along px")
    }

    @Test
    fun theNibIsAStockBrush() {
        assertTrue("nib" in Brushes.all)
    }
}
