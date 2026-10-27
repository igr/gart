package dev.oblac.gart.math

import dev.oblac.gart.vector.Vec3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Grid3Test {

    // x adds 1, y adds 10, z adds 100. trilinear gives a line back exactly
    private fun ramp(): Grid3 {
        val g = Grid3(2f, Vec3(10f, 20f, 30f), 3, 3, 3)
        for (j in 0 until 3) for (i in 0 until 3) for (k in 0 until 3) g.values[g.idx(i, j, k)] = i + 10f * j + 100f * k
        return g
    }

    @Test
    fun atASampleGivesThatSample() {
        val g = ramp()
        assertEquals(0f, g.at(Vec3(10f, 20f, 30f)))
        assertEquals(111f, g.at(Vec3(12f, 22f, 32f)))
    }

    @Test
    fun atIsTrilinearBetweenSamples() {
        // a half step on x, a quarter on y, three quarters on z
        assertEquals(78f, ramp().at(Vec3(11f, 20.5f, 31.5f)))
    }

    @Test
    fun zRunsFastestThenXThenY() {
        val g = ramp()
        assertEquals(100f, g.values[1])
        assertEquals(1f, g.values[3])
        assertEquals(10f, g.values[9])
    }

    @Test
    fun readsOutsideClampToTheEdge() {
        assertEquals(0f, ramp().at(Vec3(-100f, -100f, -100f)))
    }

    @Test
    fun aGridWithOneSampleOnAnAxisIsAnError() {
        assertFailsWith<IllegalArgumentException> { Grid3(1f, Vec3.ZERO, 2, 1, 2) }
    }
}
