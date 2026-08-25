package dev.oblac.gart.brush

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Color
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatercolorTest {

    private val w = 300
    private val h = 300

    private fun render(seed: Int, path: Path, wc: Watercolor = Watercolor(), blend: BlendMode = BlendMode.SRC_OVER): IntArray {
        val g = Gartvas(Dimension(w, h))
        g.canvas.clear(Color.WHITE)
        g.canvas.drawWatercolor(path, 0xFF2040C0.toInt(), wc, Random(seed), blend)
        return Gartmap(g).use { it.pixels.copyOf() }
    }

    private fun circle(): Path = PathBuilder().addCircle(150f, 150f, 70f).detach()

    private fun isInk(c: Int) = c != Color.WHITE

    private fun inkCount(px: IntArray, pred: (x: Int, y: Int) -> Boolean): Int {
        var n = 0
        for (i in px.indices) if (isInk(px[i]) && pred(i % w, i / w)) n++
        return n
    }

    @Test
    fun sameSeedSamePixels() {
        val a = render(3, circle())
        val b = render(3, circle())
        val c = render(4, circle())
        assertTrue(a.contentEquals(b))
        assertFalse(a.contentEquals(c))
    }

    @Test
    fun coversTheCentreAndStaysNearTheShape() {
        val px = render(1, circle())
        // the middle is well covered
        var dark = 0
        for (y in 135..165) for (x in 135..165) {
            val c = px[y * w + x]
            val r = (c shr 16) and 0xFF
            if (r < 200) dark++
        }
        assertTrue(dark > 31 * 31 * 0.8, "centre only $dark of ${31 * 31} px are tinted")
        // nothing far away (the bleed is a fraction of the edge length, not of the shape)
        val far = inkCount(px) { x, y -> (x - 150) * (x - 150) + (y - 150) * (y - 150) > 110 * 110 }
        assertEquals(0, far, "ink far outside the circle")
    }

    @Test
    fun inwardBleedKeepsTheOutsideCleaner() {
        // body only: the sparse flecks deliberately bleed the other way
        val out = render(2, circle(), Watercolor(bleed = 0.4f, outward = true, scatter = false, texture = 0f))
        val inn = render(2, circle(), Watercolor(bleed = 0.4f, outward = false, scatter = false, texture = 0f))
        val outside = { x: Int, y: Int -> (x - 150) * (x - 150) + (y - 150) * (y - 150) > 76 * 76 }
        val a = inkCount(out, outside)
        val b = inkCount(inn, outside)
        assertTrue(b < a, "inward $b should leave less ink outside than outward $a")
    }

    @Test
    fun holeStaysMostlyClean() {
        val path = PathBuilder()
            .addRect(Rect(50f, 50f, 250f, 250f))
            .addRect(Rect(110f, 110f, 190f, 190f))
            .detach()
        val px = render(5, path, Watercolor(bleed = 0.05f))
        val ring = inkCount(px) { x, y -> (x in 60..100 || x in 200..240) && y in 60..240 }
        val hole = inkCount(px) { x, y -> x in 130..170 && y in 130..170 }
        assertTrue(ring > 2 * 40 * 180 * 0.8, "ring barely painted: $ring")
        assertTrue(hole < 41 * 41 * 0.15, "hole got painted: $hole px")
    }

    @Test
    fun emptyPathDrawsNothing() {
        val px = render(1, Path())
        assertEquals(0, inkCount(px) { _, _ -> true })
    }

    @Test
    fun multiplyAndFewLayersWork() {
        val px = render(1, circle(), Watercolor(layers = 3, texture = 0f, scatter = false), BlendMode.MULTIPLY)
        assertTrue(inkCount(px) { _, _ -> true } > 1000)
    }

    @Test
    fun ringsKeepCornersAndSplitLongEdges() {
        val rings = washRings(PathBuilder().addRect(Rect(0f, 0f, 200f, 100f)).detach(), 0.07f)
        assertEquals(1, rings.size)
        val ring = rings[0]
        for (corner in listOf(0f to 0f, 200f to 0f, 200f to 100f, 0f to 100f)) {
            assertTrue(ring.any { kotlin.math.abs(it.x - corner.first) < 0.01f && kotlin.math.abs(it.y - corner.second) < 0.01f }, "corner $corner lost")
        }
        // spacing = 200 * lerp(0.15, 0.45, 0.07 / 0.6) = 37 -> 6 + 3 + 6 + 3 = 18 vertices
        assertEquals(18, ring.size)
        // a dry wash gets a fine polygon
        assertEquals(76, washRings(PathBuilder().addRect(Rect(0f, 0f, 200f, 100f)).detach(), 0.02f)[0].size)
    }

    @Test
    fun rejectsNonsense() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { Watercolor(opacity = 2f) }
        kotlin.test.assertFailsWith<IllegalArgumentException> { Watercolor(layers = 0) }
    }
}
