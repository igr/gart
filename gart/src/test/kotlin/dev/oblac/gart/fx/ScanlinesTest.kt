package dev.oblac.gart.fx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ScanlinesTest {

    // odd channels: x 0.5 and x 0.75 land between two ints, so float noise in the scale cant flip a truncation
    private val c = 0xFFCB6737.toInt()      // 203, 103, 55
    private val quarter = 0xFF984D29.toInt() // x 0.75 = 152, 77, 41
    private val half = 0xFF65331B.toInt()    // x 0.5 = 101, 51, 27

    @Test
    fun `each row is scaled by 1 - depth sin2 of its phase`() {
        val want = listOf(c, quarter, half, quarter, c, quarter, half, quarter, c)
        Gartmap(Dimension(3, want.size)).use { m ->
            m.pixels.fill(c)
            addScanlines(m, pitch = 4f, depth = 0.5f)
            for (y in want.indices) for (x in 0 until 3) {
                assertEquals(want[y], m[x, y], "at $x, $y")
            }
        }
    }

    @Test
    fun `alpha is kept`() {
        Gartmap(Dimension(1, 3)).use { m ->
            m.pixels.fill(0x8065331B.toInt())
            addScanlines(m, pitch = 4f, depth = 0.5f)
            assertEquals(0x8065331B.toInt(), m[0, 0])
            assertEquals(0x8032190D.toInt(), m[0, 2])  // 50, 25, 13
        }
    }

    @Test
    fun `a gartvas gets the rows written back`() {
        val g = Gartvas(Dimension(2, 3))
        g.canvas.clear(c)
        addScanlines(g, pitch = 4f, depth = 0.5f)
        Gartmap(g).use { m ->
            assertEquals(c, m[1, 0])
            assertEquals(half, m[1, 2])
        }
    }
}
