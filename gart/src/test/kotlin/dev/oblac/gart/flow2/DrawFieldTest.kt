package dev.oblac.gart.flow2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawFieldTest {

    // renders on white and tells which pixels got ink
    private fun render(w: Int, h: Int, draw: (Canvas) -> Unit): (Int, Int) -> Boolean {
        val g = Gartvas(Dimension(w, h))
        g.canvas.clear(Color.WHITE)
        draw(g.canvas)
        val px = Gartmap(g).use { it.pixels.copyOf() }
        return { x, y ->
            val c = px[y * w + x]
            minOf((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF) < 200
        }
    }

    // grid points sit at gap / 2 + i * gap: (10, 10), (30, 10), ... for the default gap of 20

    @Test
    fun arrowsPointAlongTheField() {
        val d = Dimension(40, 40)
        val right = render(40, 40) { it.drawField(VectorField { _, _, out -> out.set(1f, 0f) }, d) }
        assertTrue(right(20, 10), "along the arrow")
        assertFalse(right(4, 10), "behind the start")
        assertFalse(right(10, 18), "below the start")
        val down = render(40, 40) { it.drawField(VectorField { _, _, out -> out.set(0f, 1f) }, d) }
        assertTrue(down(10, 20), "along the arrow")
        assertFalse(down(20, 10), "to the side")
    }

    @Test
    fun scaledArrowsFollowTheVectorLength() {
        // lengths 0.125, 0.375, 0.625, 0.875 at x 10, 30, 50, 70; the longest arrow is 16 px
        val longer = VectorField { x, _, out -> out.set(x / 80f, 0f) }
        val d = Dimension(80, 20)
        val scaled = render(80, 20) { it.drawField(longer, d, scaled = true) }
        assertTrue(scaled(60, 10), "the arrow at x 50 is 11.4 px long")
        assertFalse(scaled(40, 10), "the arrow at x 30 is 6.9 px long")
        val plain = render(80, 20) { it.drawField(longer, d) }
        assertTrue(plain(40, 10), "without scaling every arrow is 16 px long")
    }

    @Test
    fun aHugeVectorStillGetsItsArrow() {
        val huge = render(40, 40) { it.drawField(VectorField { _, _, out -> out.set(1e20f, 0f) }, Dimension(40, 40)) }
        assertTrue(huge(20, 10), "along the arrow")
        val infinite = render(40, 40) { it.drawField(VectorField { _, _, out -> out.set(Float.POSITIVE_INFINITY, 0f) }, Dimension(40, 40)) }
        assertTrue(infinite(10, 10), "the dot")
        assertFalse(infinite(20, 10), "no arrow")
    }

    @Test
    fun aPointWithNoDirectionGetsOnlyADot() {
        val none = render(40, 40) { it.drawField(VectorField { _, _, out -> out.zero() }, Dimension(40, 40)) }
        assertTrue(none(10, 10), "the dot")
        assertFalse(none(18, 10))
        assertFalse(none(10, 18))
    }
}
