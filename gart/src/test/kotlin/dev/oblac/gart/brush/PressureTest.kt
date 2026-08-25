package dev.oblac.gart.brush

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PressureTest {

    @Test
    fun bell_staysBetweenEndsAndPeak() {
        val bell = Pressure.Bell(ends = 1.2f, peak = 0.9f)
        repeat(20) { seed ->
            val fn = bell.along(Random(seed), 500f)
            for (s in 0..500 step 5) {
                val p = fn(s.toFloat())
                assertTrue(p >= min(1.2f, 0.9f) - 1e-4f && p <= max(1.2f, 0.9f) + 1e-4f, "seed $seed s=$s p=$p")
            }
        }
    }

    @Test
    fun bell_landsPartWayLiftsAtEndsAndPeaksInTheMiddle() {
        val bell = Pressure.Bell(ends = 0.5f, peak = 1.5f, drift = 0.1f, spread = 0.2f)
        repeat(20) { seed ->
            val fn = bell.along(Random(seed), 1000f)
            // the landing end is anywhere between the two, the lift-off end close to `ends`
            val start = fn(0f)
            assertTrue(start > 0.55f && start < 1.45f, "seed $seed start=$start")
            assertTrue(fn(1000f) < 0.75f, "seed $seed end=${fn(1000f)}")
            val best = (300..700 step 10).maxOf { fn(it.toFloat()) }
            assertTrue(abs(best - 1.5f) < 0.05f, "seed $seed peak=$best")
        }
    }

    @Test
    fun bell_isFixedPerStroke() {
        val bell = Pressure.Bell()
        val a = bell.along(Random(7), 200f)
        val b = bell.along(Random(7), 200f)
        for (s in 0..200 step 7) assertEquals(a(s.toFloat()), b(s.toFloat()))
    }

    @Test
    fun curve_passesThroughNormalised() {
        val fn = Pressure.Curve { t -> 2f * t }.along(Random(1), 50f)
        assertEquals(0f, fn(0f))
        assertEquals(1f, fn(25f))
        assertEquals(2f, fn(50f))
        assertEquals(2f, fn(80f)) // clamped
    }

    @Test
    fun flat_isOne() {
        val fn = Pressure.Flat.along(Random(1), 10f)
        assertEquals(1f, fn(3f))
    }

    @Test
    fun brush_rejectsNonsense() {
        val ok = Brush(weight = 1f, scatter = 0f, opacity = 0.5f, spacing = 0.5f)
        assertEquals(1f, ok.sharpness)
        kotlin.test.assertFailsWith<IllegalArgumentException> { ok.copy(weight = 0f) }
        kotlin.test.assertFailsWith<IllegalArgumentException> { ok.copy(opacity = 1.5f) }
        kotlin.test.assertFailsWith<IllegalArgumentException> { ok.copy(spacing = 0f) }
        kotlin.test.assertFailsWith<IllegalArgumentException> { ok.copy(sharpness = -0.1f) }
    }
}
