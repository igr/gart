package dev.oblac.gart.brush

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Color
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BristlesTest {

    private val w = 600
    private val h = 120
    private val ramp = Palette(0xFF000000, 0xFF808080, 0xFFFFFFFF)

    // a straight spine along the middle, 20 px half width. long enough for the ends to taper
    // and the middle to be at full width
    private fun spine() = Spine.along(PathBuilder().moveTo(20f, h / 2f).lineTo(w - 20f, h / 2f).detach()) { 20f }

    private fun render(seed: Int, bounds: Rect? = null, load: ((Float) -> Float)? = null, roundHead: Boolean = false): IntArray {
        val g = Gartvas(Dimension(w, h))
        g.canvas.clear(Color.WHITE)
        g.canvas.drawBristles(spine(), Bristles(ramp), 0.5f, Random(seed), bounds = bounds, load = load, roundHead = roundHead)
        return Gartmap(g).use { it.pixels.copyOf() }
    }

    // inked px with x in x0 until x1
    private fun inked(px: IntArray, x0: Int, x1: Int) = (0 until w * h).count { it % w in x0 until x1 && !white(px[it]) }

    private fun white(c: Int) = (c and 0xFFFFFF) == 0xFFFFFF

    // distinct colours in a block well inside the stroke, x from x0 to x1
    private fun colours(px: IntArray, x0: Int, x1: Int) =
        (h / 2 - 10..h / 2 + 10).flatMap { y -> (x0 until x1).map { px[y * w + it] } }.toSet().size

    @Test
    fun aSpineAlongALineIsSampledEveryStep() {
        val s = Spine.along(PathBuilder().moveTo(0f, 0f).lineTo(100f, 0f).detach(), step = 4f) { t -> 10f + 10f * t }
        assertEquals(26, s.size)
        assertEquals(40f, s.points[10].at.x, 1e-4f)
        assertEquals(40f, s.points[10].arc, 1e-4f)
        // along +x the normal points down the page, +y
        assertEquals(0f, s.points[10].normal.x, 1e-6f)
        assertEquals(1f, s.points[10].normal.y, 1e-6f)
        assertEquals(10f, s.points[0].half, 1e-6f)
        assertEquals(20f, s.points[25].half, 1e-6f)
    }

    @Test
    fun aSpineNeedsTwoPoints() {
        assertThrows<IllegalArgumentException> { Spine(spine().points.take(1), 3f) }
    }

    @Test
    fun theStrokeStaysNearItsWidth() {
        val px = render(1)
        val inked = (0 until w * h).filter { !white(px[it]) }
        assertTrue(inked.size > 5000, "inked: ${inked.size}")
        // 20 px half width, plus the half of a bristle, its wander and a px of anti-aliasing
        assertTrue(inked.all { abs(it / w - h / 2) <= 26 }, "ink found far from the spine")
    }

    @Test
    fun theSameRandomGivesTheSameStroke() {
        assertContentEquals(render(3), render(3))
    }

    @Test
    fun noBristlesOutsideTheBounds() {
        val all = render(5)
        val left = render(5, bounds = Rect(0f, 0f, w / 2f, h.toFloat()))
        // on the right only the flat body is left, the bristles went down on the left half only
        assertEquals(1, colours(left, w / 2 + 20, w - 250))
        assertTrue(colours(all, w / 2 + 20, w - 250) > 10)
        assertTrue(colours(left, 250, w / 2 - 20) > 10)
    }

    @Test
    fun theOutlineComesToAPointAtTheEnds() {
        val spine = spine()
        val outline = Bristles(ramp).outline(spine)
        val bounds = outline.bounds
        assertEquals(20f, bounds.left, 0.5f)
        assertEquals(spine.points.last().at.x, bounds.right, 0.5f)
        assertEquals(h / 2f - 20f, bounds.top, 0.5f)
        assertEquals(h / 2f + 20f, bounds.bottom, 0.5f)
        // 30 px in from an end the body is not yet a px wide
        assertTrue(!outline.contains(50f, h / 2f + 1f))
        assertTrue(outline.contains(w / 2f, h / 2f + 19f))
    }

    @Test
    fun aFullLoadIsTheSameAsNoLoad() {
        assertContentEquals(render(7), render(7, load = { 1f }))
    }

    @Test
    fun aStrokeThatRunsDryHasLessInkAtItsEnd() {
        val full = render(9)
        val dry = render(9, load = { t -> 1f - t })
        // the first and last quarter of the stroke, ends left out where the bristles lift anyway
        val a = 80 until 200
        val b = w - 200 until w - 80
        assertTrue(inked(dry, b.first, b.last) < 0.7f * inked(dry, a.first, a.last), "the dry end has as much ink as the start")
        assertTrue(inked(full, b.first, b.last) > 0.9f * inked(full, a.first, a.last), "a full stroke thins on its own")
    }

    @Test
    fun theBodyNarrowsAwayWhereTheLoadRunsOut() {
        val spine = spine()
        val full = Bristles(ramp).outline(spine)
        val dry = Bristles(ramp).outline(spine, load = { t -> 1f - t * t })
        fun x(t: Float) = 20f + t * (w - 40)
        // half way a quarter of the load is gone, the body is still near its full width
        assertTrue(dry.contains(x(0.5f), h / 2f + 15f))
        // at three quarters more than half is gone: a sliver, where a full body is wide
        assertTrue(full.contains(x(0.75f), h / 2f + 10f))
        assertTrue(!dry.contains(x(0.75f), h / 2f + 10f))
    }

    @Test
    fun aRoundHeadStartsWhereAPointedOneHasNoInkYet() {
        // the spine starts at x 20. pointed, the first bristles start 30 px in
        assertEquals(0, inked(render(11), 0, 45))
        assertTrue(inked(render(11, roundHead = true), 0, 45) > 300)
    }

    @Test
    fun aRoundHeadIsFullWidthAfterItsCapAndStillLiftsAtTheEnd() {
        val spine = spine()
        val round = Bristles(ramp).outline(spine, roundHead = true)
        val pointed = Bristles(ramp).outline(spine)
        // the cap is 0.8 of the 20 px half width long
        assertTrue(round.contains(20f + 17f, h / 2f + 15f))
        assertTrue(!pointed.contains(20f + 17f, h / 2f + 2f))
        // the end lifts the same way
        val end = spine.points.last().at.x - 17f
        assertTrue(!round.contains(end, h / 2f + 2f))
    }

    @Test
    fun theLightMustBeAUnitVectorOrShorter() {
        assertThrows<IllegalArgumentException> { Bristles(ramp, light = Vec2(1f, 1f)) }
    }
}
