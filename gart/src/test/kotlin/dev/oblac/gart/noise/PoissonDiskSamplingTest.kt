package dev.oblac.gart.noise

import dev.oblac.gart.Dimension
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PoissonDiskSamplingTest {

    private val bounds = Rect(0f, 0f, 300f, 200f)

    @Test
    fun theSameSeedGivesTheSamePoints() {
        val a = poissonDiskSampling(bounds, 12f, random = Random(3))
        assertEquals(a, poissonDiskSampling(bounds, 12f, random = Random(3)))
        assertNotEquals(a, poissonDiskSampling(bounds, 12f, random = Random(4)))
    }

    @Test
    fun theSameSeedGivesTheSamePointsOnACircle() {
        val a = poissonDiskSampling(bounds, 12f, randomOnRing = false, random = Random(3))
        assertEquals(a, poissonDiskSampling(bounds, 12f, randomOnRing = false, random = Random(3)))
    }

    @Test
    fun theNoiseTakesASeedToo() {
        val d = Dimension(300, 200)
        assertEquals(poissonDiskSamplingNoise(d, 15f, Random(5)), poissonDiskSamplingNoise(d, 15f, Random(5)))
    }

    @Test
    fun pointsKeepTheRadiusAndTheBounds() {
        val pts = poissonDiskSampling(bounds, 12f, random = Random(6))
        assertTrue(pts.size > 100, "${pts.size} points")
        var closest = Float.MAX_VALUE
        for (i in pts.indices) for (j in i + 1 until pts.size) {
            closest = minOf(closest, hypot(pts[i].x - pts[j].x, pts[i].y - pts[j].y))
        }
        assertTrue(closest > 12f, "two points are $closest apart")
        assertTrue(pts.all { it.x >= 0f && it.x < 300f && it.y >= 0f && it.y < 200f })
    }

    @Test
    fun uniformRingStaysInTheRingAndFollowsTheSeed() {
        for (s in 0 until 500) {
            val p = Point.uniformRing(5f, 10f, Random(s))
            val r = hypot(p.x, p.y)
            assertTrue(r >= 5f - 1e-4f && r < 10f, "radius $r")
        }
        assertEquals(Point.uniformRing(5f, 10f, Random(1)), Point.uniformRing(5f, 10f, Random(1)))
        assertEquals(Point.uniformRing(7f, 7f, Random(1)), Point.uniformRing(7f, 7f, Random(1)))
    }
}
