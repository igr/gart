package studio.oblac.gart.noise

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PerlinNoiseTest {

    @Test
    fun testPerlinNoise() {
        val n = PerlinNoise().noise(3.14, 42.0, 7.0)
        assertTrue(n != 0.0f)
        assertTrue(n < 1.0f)
    }

    @Test
    fun testPerlinNoise2() {
        assertTrue(Perlin.noise(3.14, 42.0, 7.0) != 0.0)
        assertTrue(Perlin.noise(0.14, 42.0, 7.0) != 0.0)
        assertTrue(Perlin.noise(0.0, 42.0, 7.0) == 0.0)     // no decimals, then 0
    }
}
