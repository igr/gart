package dev.oblac.gart.marbling

import dev.oblac.gart.Dimension
import dev.oblac.gart.MemPixels
import dev.oblac.gart.angle.Degrees
import org.jetbrains.skia.Color
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContourTest {

    private val red = 0xFFCC2222.toInt()
    private val blue = 0xFF2244CC.toInt()
    private val gold = 0xFFDDAA22.toInt()

    // even-odd crossing count, the plain point-in-polygon
    private fun inside(poly: List<Point>, x: Float, y: Float): Boolean {
        var hit = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[j]
            if ((a.y > y) != (b.y > y) && x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x) hit = !hit
            j = i
        }
        return hit
    }

    @Test
    fun aLoneDropIsItsCircle() {
        val cs = Marbling().drop(50f, 50f, 20f, red).contours(step = 2f)
        assertEquals(1, cs.size)
        val c = cs[0]
        assertEquals(red, c.color)
        assertTrue(c.points.size >= 16)
        for (p in c.points) assertEquals(20f, hypot(p.x - 50f, p.y - 50f), 1e-3f)
        assertTrue(inside(c.points, 50f, 50f))
        assertTrue(!c.path.isEmpty)
        for (i in c.points.indices) {
            val a = c.points[i]
            val b = c.points[(i + 1) % c.points.size]
            assertTrue(hypot(a.x - b.x, a.y - b.y) <= 2f + 1e-3f, "segment $i longer than the step")
        }
        // and the plain circle needs no midpoints: the ring alone is already within the step
        assertTrue(c.points.size <= 70, "a 20 px circle at step 2 is ${c.points.size} points")
    }

    @Test
    fun maxPointsCapsTheOutput() {
        val lone = Marbling().drop(50f, 50f, 20f, red).contours(step = 1.5f, maxPoints = 100)[0]
        assertTrue(lone.points.size in 16..100, "lone drop gave ${lone.points.size} points")
        val m = Marbling().drop(50f, 50f, 20f, red).comb(0f, 50f, Degrees(0f), 60f, 8f, 3, 12f).vortex(70f, 60f, 40f, 10f)
        val capped = m.contours(step = 1f, maxPoints = 300)[0]
        assertTrue(capped.points.size in 16..300, "capped rim gave ${capped.points.size} points")
        val loose = m.contours(step = 1f, maxPoints = 100_000)[0]
        assertTrue(loose.points.size > 300, "the cap should have bitten, loose rim is ${loose.points.size} points")
    }

    @Test
    fun aVeryNarrowTineIsStillTraced() {
        // c = 0.02 px: the tongue is a twentieth of a pixel wide at its base, far under the rim
        // sampling at step. the ring has to be seeded as fine as the sharpest tine after the drop
        val m = Marbling(Color.WHITE).drop(40f, 40f, 20f, red).tine(0f, 40.38f, Degrees(0f), 200f, 0.02f)
        val c = m.contours(step = 1.5f)[0]
        val maxX = c.points.maxOf { it.x }
        assertTrue(maxX > 257f, "the tongue reaches x = 260, contour stops at $maxX")
    }

    @Test
    fun combedRimIsRefinedToTheStep() {
        val m = Marbling().drop(50f, 50f, 20f, red).comb(0f, 50f, Degrees(0f), 60f, 8f, 3, 12f)
        val plain = Marbling().drop(50f, 50f, 20f, red).contours(step = 2f)[0].points.size
        val c = m.contours(step = 2f)[0]
        assertTrue(c.points.size > plain, "refinement added no points")
        for (i in c.points.indices) {
            val a = c.points[i]
            val b = c.points[(i + 1) % c.points.size]
            assertTrue(hypot(a.x - b.x, a.y - b.y) <= 2f + 1e-3f, "segment $i too long")
        }
    }

    @Test
    fun aSpikeBetweenTwoRimSamplesIsStillTraced() {
        // a sharp tine (c = 2) grazing the rim pulls a sliver of it 200 px out. the two rim samples
        // either side of the sliver land close together, so a chord test alone would never split
        // the segment and the whole tongue would be missing
        val m = Marbling(Color.WHITE).drop(40f, 40f, 20f, red).tine(0f, 40.38f, Degrees(0f), 200f, 2f)
        val c = m.contours(step = 1.5f)[0]
        val maxX = c.points.maxOf { it.x }
        assertTrue(maxX > 257f, "the tongue reaches x = 260, contour stops at $maxX")
        // along the row just under the tine the tongue is the sliver 212..252 - the left rim was
        // pulled right as well - and the polygon must agree with the raster on both sides of it
        for (x in listOf(150.5f, 200.5f, 215.5f, 231.5f, 250.5f, 253.5f)) {
            assertEquals(m.colorAt(x, 40.5f) == red, inside(c.points, x, 40.5f), "at x = $x")
        }
        for (i in c.points.indices) {
            val a = c.points[i]
            val b = c.points[(i + 1) % c.points.size]
            assertTrue(hypot(a.x - b.x, a.y - b.y) <= 1.5f + 1e-3f, "segment $i too long")
        }
    }

    @Test
    fun contoursAgreeWithTheRaster() {
        val m = Marbling(Color.WHITE)
        m.drop(40f, 40f, 24f, red).drop(70f, 60f, 18f, blue).drop(50f, 80f, 14f, gold)
        m.comb(0f, 60f, Degrees(0f), 40f, 7f, 4, 14f).whirl(60f, 60f, 20f, 25f, 8f)
        val px = MemPixels(Dimension(120, 120))
        m.render(px, workers = 1)
        val cs = m.contours(step = 1f)
        var agree = 0
        for (y in 0 until 120) {
            for (x in 0 until 120) {
                var col = Color.WHITE
                for (c in cs) if (inside(c.points, x + 0.5f, y + 0.5f)) col = c.color
                if (col == px[x, y]) agree++
            }
        }
        assertTrue(agree >= 120 * 120 * 98 / 100, "only $agree of ${120 * 120} pixels agree")
    }
}
