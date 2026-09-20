package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Lut1DTest {

    @Test
    fun readsOnASampleGiveThatSample() {
        val lut = Lut1D(-10f, 0.5f, 41) { x -> x * x }
        assertEquals(100f, lut[-10f], 0f)
        assertEquals(72.25f, lut[-8.5f], 0f)
    }

    @Test
    fun aLineIsExactBetweenSamplesToo() {
        val lut = Lut1D(-10f, 0.5f, 41) { x -> 3f * x + 1f }
        for (x in -10..9) assertEquals(3f * x + 1f, lut[x.toFloat()], 0.0005f)
        assertEquals(3f * -2.37f + 1f, lut[-2.37f], 0.0005f)
    }

    @Test
    fun aFineTableFollowsACurve() {
        val lut = Lut1D(0f, 0.01f, 629) { x -> sin(x) }
        for (i in 0..600) {
            val x = i * 0.01f
            assertEquals(sin(x), lut[x], 0.0001f)
        }
    }

    @Test
    fun readsOutsideTheTableClampToItsEnds() {
        val lut = Lut1D(-10f, 0.5f, 41) { x -> 3f * x + 1f }
        assertEquals(lut[-10f], lut[-4000f], 0f)
        // the far end lands a hair inside the last sample, never past it
        assertEquals(31f, lut[4000f], 0.01f)
        assertEquals(lut[10f], lut[4000f], 0f)
    }

    @Test
    fun aTableNeedsTwoSamplesAndAForwardStep() {
        assertFailsWith<IllegalArgumentException> { Lut1D(0f, 1f, 1) { it } }
        assertFailsWith<IllegalArgumentException> { Lut1D(0f, 0f, 10) { it } }
        assertFailsWith<IllegalArgumentException> { Lut1D(0f, -1f, 10) { it } }
    }

    @Test
    fun theFunctionIsCalledOncePerSampleAndNeverAgain() {
        var calls = 0
        val lut = Lut1D(0f, 1f, 16) { x ->
            calls++
            x
        }
        assertEquals(16, calls)
        repeat(100) { lut[it * 0.13f] }
        assertEquals(16, calls)
    }
}
