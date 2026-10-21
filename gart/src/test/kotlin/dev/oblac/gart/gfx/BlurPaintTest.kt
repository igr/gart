package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.red
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Color
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlurPaintTest {

    @Test
    fun `sets the blur, the blend and the alpha`() {
        val p = blurPaint(3.5f, BlendMode.SCREEN, 170)
        assertNotNull(p.imageFilter)
        assertEquals(BlendMode.SCREEN, p.blendMode)
        assertEquals(170, p.alpha)
        assertTrue(p.isAntiAlias)
    }

    @Test
    fun `defaults to a plain opaque blur`() {
        val p = blurPaint(2f)
        assertEquals(BlendMode.SRC_OVER, p.blendMode)
        assertEquals(255, p.alpha)
    }

    @Test
    fun `a layer through it spreads a dot`() {
        val g = Gartvas(Dimension(9, 9))
        g.canvas.clear(Color.BLACK)
        g.canvas.saveLayer(null, blurPaint(1.5f))
        g.canvas.drawRect(Rect(4f, 4f, 5f, 5f), fillOf(Color.WHITE))
        g.canvas.restore()
        Gartmap(g).use { m ->
            assertTrue(red(m[5, 4]) > 0)
            assertTrue(red(m[4, 4]) < 255)
        }
    }

    // an image copy, not a layer: skia grows a filtered layer past the canvas, so a clear() in it
    // leaves no transparent edge for decal to read
    @Test
    fun `a blurred copy goes dark at the edge with decal, not with clamp`() {
        fun cornerAndMiddle(tile: FilterTileMode): Pair<Int, Int> {
            val white = Gartvas(Dimension(16, 16))
            white.canvas.clear(Color.WHITE)
            val g = Gartvas(Dimension(16, 16))
            g.canvas.clear(Color.BLACK)
            g.canvas.drawImage(white.snapshot(), 0f, 0f, blurPaint(4f, tile = tile))
            return Gartmap(g).use { red(it[0, 0]) to red(it[8, 8]) }
        }
        val (decalCorner, decalMiddle) = cornerAndMiddle(FilterTileMode.DECAL)
        assertTrue(decalCorner < decalMiddle)
        val (clampCorner, clampMiddle) = cornerAndMiddle(FilterTileMode.CLAMP)
        assertEquals(clampMiddle, clampCorner)
    }
}
