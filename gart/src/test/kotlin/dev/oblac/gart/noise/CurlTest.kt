package dev.oblac.gart.noise

import dev.oblac.gart.vector.MutableVec2
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class CurlTest {

    @Test
    fun bothOverloadsAgree() {
        val out = MutableVec2()
        curl(1.7f, -0.4f, out, eps = 0.25f)
        val v = curl(Point(1.7f, -0.4f), eps = 0.25f)
        assertEquals(v.x, out.x)
        assertEquals(v.y, out.y)
    }

    @Test
    fun outIsReusable() {
        val out = MutableVec2()
        curl(0.3f, 0.9f, out)
        val first = out.toVec2()
        curl(0.3f, 0.9f, out)
        assertEquals(first.x, out.x)
        assertEquals(first.y, out.y)
    }

    /**
     * The whole point of taking a curl: no sources, no sinks, so nothing advected through the
     * field can pool. Both components come from the same mixed second difference of the
     * potential with opposite signs, so on the operator's own stencil they cancel to the last
     * bit — measure the divergence with the same step the curl was taken with and it is zero.
     *
     * Probing with a step other than [eps] does NOT give zero, and that isn't a bug: the two
     * halves then land on different stencils and each carries its own truncation error. Worth
     * knowing before anyone "tightens" the step in here and watches this go red.
     */
    @Test
    fun fieldIsDivergenceFree() {
        val h = 0.5f
        val a = MutableVec2()
        val b = MutableVec2()

        for (i in 0 until 20) {
            val x = -3f + i * 0.37f
            val y = 2f - i * 0.29f

            curl(x + h, y, a, h)
            curl(x - h, y, b, h)
            val dudx = (a.x - b.x) / (2f * h)

            curl(x, y + h, a, h)
            curl(x, y - h, b, h)
            val dvdy = (a.y - b.y) / (2f * h)

            assertTrue(abs(dudx + dvdy) < 1e-5f, "divergence at ($x, $y) was ${dudx + dvdy}")
        }
    }

    @Test
    fun scalesWithTheEddySize() {
        // a wide difference averages the potential's fine structure away, so it returns a
        // smaller, smoother vector than a tight one taken at the same place
        val tight = MutableVec2()
        val wide = MutableVec2()
        var tightSum = 0f
        var wideSum = 0f
        for (i in 0 until 40) {
            val x = i * 0.13f
            curl(x, 5f, tight, eps = 0.05f)
            curl(x, 5f, wide, eps = 2.5f)
            tightSum += abs(tight.x) + abs(tight.y)
            wideSum += abs(wide.x) + abs(wide.y)
        }
        assertTrue(wideSum < tightSum, "wide=$wideSum tight=$tightSum")
    }

    @Test
    fun takesAnFbmPotential() {
        val out = MutableVec2()
        curl(0.8f, 1.3f, out, eps = 0.4f) { a, b -> fbm(a, b, octaves = 3) }
        assertTrue(out.x != 0f || out.y != 0f)
    }
}
