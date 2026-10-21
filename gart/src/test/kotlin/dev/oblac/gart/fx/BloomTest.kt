package dev.oblac.gart.fx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.red
import dev.oblac.gart.gfx.fillOf
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Color
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BloomTest {

    // a white dot in the middle of a black 9x9 canvas
    private fun dot() = Gartvas(Dimension(9, 9)).also {
        it.canvas.clear(Color.BLACK)
        it.canvas.drawRect(Rect(4f, 4f, 5f, 5f), fillOf(Color.WHITE))
    }

    @Test
    fun `zero strength changes nothing`() {
        val g = dot()
        val before = Gartmap(g).use { it.pixels.copyOf() }
        bloom(g.canvas, g.snapshot(), 2f, 0f)
        Gartmap(g).use { assertContentEquals(before, it.pixels) }
    }

    @Test
    fun `light spreads round the dot and the dot stays white`() {
        val g = dot()
        bloom(g.canvas, g.snapshot(), 1.5f, 1f)
        Gartmap(g).use { m ->
            assertTrue(red(m[5, 4]) > 0)
            assertEquals(Color.WHITE, m[4, 4])
        }
    }

    @Test
    fun `a flat copy stays flat to the edges`() {
        val g = Gartvas(Dimension(16, 16))
        g.canvas.clear(0xFF404040.toInt())
        bloom(g.canvas, g.snapshot(), 4f, 1f, BlendMode.PLUS)
        Gartmap(g).use { m ->
            assertTrue(red(m[8, 8]) > 0x40)
            assertEquals(m[8, 8], m[0, 0])
        }
    }
}
