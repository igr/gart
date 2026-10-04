package dev.oblac.gart.march

import dev.oblac.gart.vector.Vec3
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarchTest {

    // y = a x + b z + c
    private class Plane(val a: Float = 0f, val b: Float = 0f, val c: Float = 0f, override val top: Float = 1f) : HeightField {
        override fun height(x: Float, z: Float, fp: Float) = a * x + b * z + c
        override fun slope(fp: Float) = hypot(a, b)
    }

    // y = 0.5 sin(x) cos(0.7 z), slope at most 0.5 * sqrt(1 + 0.49)
    private class Waves : HeightField {
        override val top = 0.5f
        override fun height(x: Float, z: Float, fp: Float) = 0.5f * sin(x) * cos(0.7f * z)
        override fun slope(fp: Float) = 0.62f
    }

    private fun unit(x: Float, y: Float, z: Float): FloatArray {
        val l = sqrt(x * x + y * y + z * z)
        return floatArrayOf(x / l, y / l, z / l)
    }

    private fun HeightField.march(o: FloatArray, d: FloatArray, far: Float = 1000f) =
        march(o[0], o[1], o[2], d[0], d[1], d[2], 0.001f, far)

    @Test
    fun aFlatPlaneIsHitAtTheHeightOverTheDrop() {
        val d = unit(0f, -0.5f, 1f)
        assertEquals(3f / -d[1], Plane().march(floatArrayOf(0f, 3f, 0f), d), 0.01f)
    }

    @Test
    fun aTiltedPlaneIsHitWhereTheRayMeetsIt() {
        val d = unit(1f, -0.3f, 1f)
        // 2 + dy t = 0.2 dx t
        val want = 2f / (0.2f * d[0] - d[1])
        assertEquals(want, Plane(a = 0.2f, top = 50f).march(floatArrayOf(0f, 2f, 0f), d), 0.01f)
    }

    @Test
    fun wavesAgreeWithABruteForceMarch() {
        val field = Waves()
        val o = floatArrayOf(0.3f, 1.2f, -0.4f)
        for (d in listOf(unit(0.2f, -0.4f, 1f), unit(-0.7f, -0.2f, 1f), unit(1f, -0.05f, 0.3f), unit(0f, -1f, 0.1f))) {
            var t = 0f
            while (o[1] + d[1] * t > field.height(o[0] + d[0] * t, o[2] + d[2] * t, 0f)) t += 1e-4f
            assertEquals(t, field.march(o, d), 2e-3f)
        }
    }

    @Test
    fun aRayThatGoesUpMissesTheSurface() {
        assertEquals(-1f, Plane().march(floatArrayOf(0f, 3f, 0f), unit(0f, 0.2f, 1f)))
        // from inside the slab it leaves through the top
        assertEquals(-1f, Waves().march(floatArrayOf(0f, 0.45f, 0f), unit(0f, 0.3f, 1f)))
    }

    @Test
    fun aLevelRayFromTheTopOfTheSlabStillMarches() {
        // y = 0.01 z - 1 reaches y = 2 at z = 300
        val ramp = Plane(b = 0.01f, c = -1f, top = 2f)
        assertEquals(300f, ramp.march(floatArrayOf(0f, 2f, 0f), floatArrayOf(0f, 0f, 1f)), 0.01f)
    }

    @Test
    fun anAlmostLevelRayIsNotCutShort() {
        // it leaves the slab after 10 km, not after the 100 m a 1e-6 floor on dy would give
        val ramp = Plane(b = 0.01f, c = -1f, top = 2f)
        val d = unit(0f, 1e-8f, 1f)
        val want = (1.9999f + 1f) / (0.01f - d[1])
        assertEquals(want, ramp.march(floatArrayOf(0f, 1.9999f, 0f), d), 0.01f)
    }

    @Test
    fun anOriginUnderTheSurfaceIsAHitAtZero() {
        val d = unit(0f, -0.5f, 1f)
        assertEquals(0f, Plane().march(floatArrayOf(0f, -3f, 0f), d)) // under the slab
        assertEquals(0f, Plane().march(floatArrayOf(0f, -0.5f, 0f), d)) // in it
    }

    @Test
    fun aRidgeLongerThanTheMinimumStepIsFound() {
        // one example of the contract, on a field that does not change with the footprint.
        // y = 1 - 50 |z - 50| is over the level ray y = 0 for 0.04 m. at t = 50 the minimum step is
        // 50 * 0.001 / 2 = 0.025, so the march must land in it. the safe step is 0.02 here
        val ridge = object : HeightField {
            override val top = 1f
            override fun height(x: Float, z: Float, fp: Float) = max(-1f, 1f - 50f * abs(z - 50f))
            override fun slope(fp: Float) = 50f
        }
        assertEquals(49.98f, ridge.march(0f, 0f, 0f, 0f, 0f, 1f, 0.001f, 1000f), 0.005f)
    }

    @Test
    fun ridgesLongerThanTheMinimumStepAreFoundAtRandom() {
        // more examples, not a proof: level rays, ridges 1.1 to 3 minimum steps long at random
        // distances and steepness. the minimum step grows with t, so sizing by the step at the
        // ridge centre is on the safe side
        val rnd = kotlin.random.Random(4)
        val spread = 0.001f
        repeat(200) {
            val at = 5f + rnd.nextFloat() * 495f
            val len = (1.1f + rnd.nextFloat() * 1.9f) * max(at * spread * 0.5f, 0.004f)
            val k = 20f + rnd.nextFloat() * 80f
            val peak = k * len / 2f
            val ridge = object : HeightField {
                override val top = max(peak, 1f)
                override fun height(x: Float, z: Float, fp: Float) = max(-1f, peak - k * abs(z - at))
                override fun slope(fp: Float) = k
            }
            val t = ridge.march(0f, 0f, 0f, 0f, 0f, 1f, spread, 1000f)
            assertTrue(t >= at - len / 2f - 1e-3f && t <= at + len / 2f, "ridge $it at $at, $len long: t = $t")
        }
    }

    @Test
    fun nothingPastFarCounts() {
        // the plane is 6.7 away along this ray
        assertEquals(-1f, Plane().march(floatArrayOf(0f, 3f, 0f), unit(0f, -0.5f, 1f), far = 5f))
    }

    @Test
    fun theFootprintGrowsWithDistanceAndAtGrazingAngles() {
        val seen = ArrayList<Pair<Float, Float>>() // (t, fp)
        val d = unit(0f, -0.1f, 1f)
        val field = object : HeightField {
            override val top = 1f
            override fun height(x: Float, z: Float, fp: Float): Float {
                seen += z / d[2] to fp
                return 0f
            }
            override fun slope(fp: Float) = 0f
        }
        field.march(0f, 3f, 0f, d[0], d[1], d[2], 0.002f, 1000f)
        val sq = 1f / sqrt(max(abs(d[1]), 0.005f))
        for ((t, fp) in seen) {
            assertEquals(t * 0.002f * sq, fp, 1e-4f)
            assertEquals(footprint(t, d[1], 0.002f), fp, 1e-4f)
        }
    }

    @Test
    fun footprintStopsGrowingAtGrazingAngles() {
        // under |dy| 0.005 the stretch holds at 1 / sqrt(0.005)
        assertEquals(footprint(10f, -0.005f, 0.001f), footprint(10f, 0f, 0.001f))
        assertEquals(10f * 0.001f, footprint(10f, -1f, 0.001f), 1e-9f)
    }

    @Test
    fun cameraLooksAlongZAndPitchTurnsItDown() {
        val level = RayCamera(Vec3(0f, 3f, 0f), 0f, 24f, 1200, 1500).ray(600f, 750f)
        assertEquals(0f, level.x, 1e-6f)
        assertEquals(0f, level.y, 1e-6f)
        assertEquals(1f, level.z, 1e-6f)
        val down = RayCamera(Vec3(0f, 3f, 0f), -4f, 24f, 1200, 1500).ray(600f, 750f)
        assertEquals(sin(-4.0 * PI / 180).toFloat(), down.y, 1e-6f)
        assertEquals(cos(-4.0 * PI / 180).toFloat(), down.z, 1e-6f)
    }

    @Test
    fun cameraRaysAreUnitAndTheTopEdgeIsHalfTheFovUp() {
        val cam = RayCamera(Vec3(0f, 0f, 0f), 0f, 24f, 1200, 1500)
        for ((px, py) in listOf(0f to 0f, 1200f to 1500f, 37f to 900f)) {
            assertEquals(1f, cam.ray(px, py).length(), 1e-5f)
        }
        val top = cam.ray(600f, 0f)
        assertEquals(12f, (atan2(top.y, top.z) * 180 / PI).toFloat(), 1e-4f)
    }

    @Test
    fun spreadIsTheAngleBetweenTwoRays() {
        val tanF = kotlin.math.tan(12.0 * PI / 180).toFloat()
        assertEquals(2f * tanF / 1500 / 3, RayCamera(Vec3(0f, 0f, 0f), 0f, 24f, 1200, 1500, ss = 3).spread, 1e-9f)
    }
}
