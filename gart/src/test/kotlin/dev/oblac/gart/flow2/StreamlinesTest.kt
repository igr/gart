package dev.oblac.gart.flow2

import dev.oblac.gart.Dimension
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StreamlinesTest {

    // smooth swirls with no zero anywhere, so no line stops for a lack of direction
    private val swirl = VectorField.angles { x, y -> sin(x * 0.02f) * 2f + cos(y * 0.03f) * 1.5f }
    private val d = Dimension(160, 160)

    private fun dist(a: Point, b: Point) = hypot(a.x - b.x, a.y - b.y)

    // distance along the polyline from its first point
    private fun arcs(points: List<Point>): FloatArray {
        val a = FloatArray(points.size)
        for (i in 1 until points.size) a[i] = a[i - 1] + dist(points[i - 1], points[i])
        return a
    }

    @Test
    fun linesKeepDTestFromEachOther() {
        val lines = swirl.streamlines(d, Random(1), dSep = 12f)
        assertTrue(lines.size > 5, "${lines.size} lines")
        var closest = Float.MAX_VALUE
        for (i in lines.indices) for (j in i + 1 until lines.size) {
            for (a in lines[i].points) for (b in lines[j].points) closest = min(closest, dist(a, b))
        }
        assertTrue(closest >= 6f - 1e-3f, "two lines come $closest px close")
    }

    @Test
    fun linesKeepDTestWhenItEqualsDSep() {
        // a seed is a point too, so it has to keep dTest as well
        var closest = Float.MAX_VALUE
        for (seed in 1..6) {
            val lines = swirl.streamlines(d, Random(seed), dSep = 12f, dTest = 12f)
            for (i in lines.indices) for (j in i + 1 until lines.size) {
                for (a in lines[i].points) for (b in lines[j].points) closest = min(closest, dist(a, b))
            }
        }
        assertTrue(closest >= 12f - 1e-3f, "two lines come $closest px close")
    }

    @Test
    fun linesCloseInPastDSepBeforeTheyStop() {
        // where the swirls squeeze the flow, a line runs on until dTest, not just until dSep
        val lines = swirl.streamlines(d, Random(1), dSep = 12f)
        var closest = Float.MAX_VALUE
        for (i in lines.indices) for (j in i + 1 until lines.size) {
            for (a in lines[i].points) for (b in lines[j].points) closest = min(closest, dist(a, b))
        }
        assertTrue(closest < 8f, "the closest two lines get is $closest px")
    }

    @Test
    fun aLineKeepsDTestFromItself() {
        // circles round the middle, a line that never stops goes round and round
        val lines = VectorField.vortex(80f, 80f).streamlines(d, Random(2), dSep = 10f)
        assertTrue(lines.size > 3, "${lines.size} lines")
        var closest = Float.MAX_VALUE
        for (l in lines) {
            val arc = arcs(l.points)
            for (i in l.points.indices) for (j in i + 1 until l.points.size) {
                if (arc[j] - arc[i] > 10f) closest = min(closest, dist(l.points[i], l.points[j]))
            }
        }
        assertTrue(closest >= 5f - 1e-3f, "a line comes back $closest px close to itself")
    }

    @Test
    fun everyPartOfTheCanvasIsNearALine() {
        val pts = swirl.streamlines(d, Random(3), dSep = 10f, minLength = 0f).flatMap { it.points }
        assertTrue(pts.isNotEmpty())
        var worst = 0f
        for (y in 0 until 160 step 4) for (x in 0 until 160 step 4) {
            worst = max(worst, pts.minOf { hypot(it.x - x, it.y - y) })
        }
        assertTrue(worst <= 18f, "a spot is $worst px from the nearest line")
    }

    @Test
    fun linesFillBothSidesOfADeadBand() {
        // straight down, with no direction in the band: no line can spread across it by itself
        val walled = VectorField { x, _, out -> if (x in 100f..140f) out.zero() else out.set(0f, 1f) }
        val lines = walled.streamlines(Dimension(240, 100), Random(4), dSep = 10f, minLength = 0f)
        assertTrue(lines.any { l -> l.points.all { it.x < 100f } }, "nothing left of the band")
        assertTrue(lines.any { l -> l.points.all { it.x > 140f } }, "nothing right of the band")
    }

    @Test
    fun theRandomDecidesTheLayout() {
        val a = swirl.streamlines(d, Random(5), dSep = 12f).map { it.points }
        assertTrue(a.isNotEmpty())
        assertEquals(a, swirl.streamlines(d, Random(5), dSep = 12f).map { it.points })
        assertNotEquals(a, swirl.streamlines(d, Random(6), dSep = 12f).map { it.points })
    }

    @Test
    fun eachLineIsAtLeastMinLengthLong() {
        val lines = swirl.streamlines(d, Random(6), dSep = 8f, minLength = 30f)
        assertTrue(lines.size > 3, "${lines.size} lines")
        assertTrue(lines.all { it.length >= 30f }, "shortest ${lines.minOf { it.length }} px")
    }

    @Test
    fun maxLengthCapsEachLine() {
        val lines = swirl.streamlines(d, Random(8), dSep = 12f, minLength = 0f, maxLength = 40f)
        assertTrue(lines.size > 5, "${lines.size} lines")
        assertTrue(lines.all { it.length <= 40f + 1e-3f }, "longest ${lines.maxOf { it.length }} px")
    }

    @Test
    fun lengthIsTheLengthOfThePolyline() {
        val lines = swirl.streamlines(d, Random(7), dSep = 12f)
        assertTrue(lines.isNotEmpty())
        for (l in lines) assertEquals(arcs(l.points).last(), l.length, 1e-2f)
    }

    @Test
    fun pointsAreOneStepApart() {
        val lines = swirl.streamlines(d, Random(9), dSep = 12f, step = 0.5f)
        assertTrue(lines.isNotEmpty())
        for (l in lines) for (i in 1 until l.points.size) assertEquals(0.5f, dist(l.points[i - 1], l.points[i]), 1e-3f)
    }

    @Test
    fun clearanceIsTheGapToTheNearestOtherLine() {
        val lines = swirl.streamlines(d, Random(10), dSep = 12f)
        assertTrue(lines.size > 5, "${lines.size} lines")
        for ((i, l) in lines.withIndex()) {
            assertEquals(l.points.size, l.clearance.size)
            for ((k, p) in l.points.withIndex()) {
                var near = 12f
                for ((j, o) in lines.withIndex()) if (j != i) for (q in o.points) near = min(near, dist(p, q))
                assertEquals(near, l.clearance[k], 1e-3f)
            }
        }
    }

    @Test
    fun taperGoesFromOneInTheOpenToZeroAtDTest() {
        val lines = swirl.streamlines(d, Random(16), dSep = 12f)
        var thin = false
        var full = false
        for (l in lines) for (i in l.points.indices) {
            val want = ((l.clearance[i] - 6f) / 6f).coerceIn(0f, 1f)
            assertEquals(want, l.taper(i), 1e-5f)
            if (want < 0.5f) thin = true
            if (want == 1f) full = true
        }
        assertTrue(thin && full, "thin $thin, full $full")
    }

    @Test
    fun taperIsOneEverywhereWhenDTestEqualsDSep() {
        val lines = swirl.streamlines(d, Random(15), dSep = 12f, dTest = 12f)
        assertTrue(lines.isNotEmpty())
        for (l in lines) for (i in l.points.indices) assertEquals(1f, l.taper(i))
    }

    @Test
    fun withoutAMarginLinesStayOnTheCanvas() {
        val pts = swirl.streamlines(d, Random(11), dSep = 12f).flatMap { it.points }
        assertTrue(pts.isNotEmpty())
        assertTrue(pts.all { it.x >= 0f && it.x < 160f && it.y >= 0f && it.y < 160f })
    }

    @Test
    fun theMarginLetsLinesRunOffTheCanvas() {
        val pts = swirl.streamlines(d, Random(12), dSep = 12f, margin = 30f).flatMap { it.points }
        assertTrue(pts.any { it.x < 0f || it.y < 0f || it.x >= 160f || it.y >= 160f })
        assertTrue(pts.all { it.x >= -30f && it.x < 190f && it.y >= -30f && it.y < 190f })
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun aSinkDoesNotTrapALine() {
        val sink = VectorField.vortex(50f, 50f, spin = 0f, pull = 1f)
        val lines = sink.streamlines(Dimension(100, 100), Random(13), dSep = 10f, rk = Integrator.RK4)
        assertTrue(lines.isNotEmpty())
        for (l in lines) for (i in 1 until l.points.size) assertNotEquals(l.points[i - 1], l.points[i])
    }

    @Test
    fun aCanvasThinnerThanHalfDSepStillGetsALine() {
        // a whole dSep cell would put its middle at y = 5, off this canvas
        val right = VectorField { _, _, out -> out.set(1f, 0f) }
        assertEquals(1, right.streamlines(Dimension(100, 4), Random(14), dSep = 10f).size)
    }

    @Test
    fun badSettingsAreRejected() {
        assertFailsWith<IllegalArgumentException> { swirl.streamlines(d, Random(0), dSep = 0f) }
        assertFailsWith<IllegalArgumentException> { swirl.streamlines(d, Random(0), dSep = 10f, dTest = 11f) }
        assertFailsWith<IllegalArgumentException> { swirl.streamlines(d, Random(0), dSep = 10f, step = 6f) }
        assertFailsWith<IllegalArgumentException> { swirl.streamlines(d, Random(0), dSep = 10f, margin = -1f) }
    }
}
