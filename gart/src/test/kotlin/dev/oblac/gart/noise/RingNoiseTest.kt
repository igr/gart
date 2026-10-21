package dev.oblac.gart.noise

import dev.oblac.gart.math.TAUf
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals

class RingNoiseTest {

    @Test
    fun `reads simplex noise on the circle`() {
        for (a in listOf(0.3f, 1.7f, 4.2f)) {
            assertEquals(SimplexNoise.noise(cos(a) * 3f + 1.5f, sin(a) * 3f - 2f), ringNoise(a, 3f, 1.5f, -2f))
        }
    }

    @Test
    fun `one turn comes back to the start`() {
        assertEquals(ringNoise(0.4f, 2.3f, 7f), ringNoise(0.4f + TAUf, 2.3f, 7f), 1e-4f)
    }

    @Test
    fun `radius zero is the noise at the centre`() {
        assertEquals(SimplexNoise.noise(1.3f, 2.7f), ringNoise(2f, 0f, 1.3f, 2.7f))
    }

    @Test
    fun `a point centre is the same as the floats`() {
        assertEquals(ringNoise(1f, 4f, 0.5f, 9f), ringNoise(1f, 4f, Point(0.5f, 9f)))
    }
}
