package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomGaussianTest {

    @Test
    fun `float gaussian takes exactly two float draws`() {
        val a = Random(5)
        val b = Random(5)
        a.rndGaussianf()
        b.nextFloat()
        b.nextFloat()
        assertEquals(b.nextInt(), a.nextInt())
    }

    @Test
    fun `float gaussian has the asked mean and spread`() {
        for ((mean, sd) in listOf(0f to 1f, 5f to 2f)) {
            val rnd = Random(1)
            val v = FloatArray(200_000) { rnd.rndGaussianf(mean, sd) }
            val m = v.average()
            val s = sqrt(v.sumOf { (it - m) * (it - m) } / v.size)
            assertEquals(mean.toDouble(), m, 0.01 * sd, "mean")
            assertEquals(sd.toDouble(), s, 0.01 * sd, "spread")
        }
    }

    @Test
    fun `a zero draw does not make infinity`() {
        // nextFloat is exactly 0 here, so the log gets the 1e-7 floor: sqrt(-2 ln 1e-7) = 5.6777
        val zeros = object : Random() {
            override fun nextBits(bitCount: Int) = 0
        }
        val z = zeros.rndGaussianf()
        assertTrue(z.isFinite())
        assertEquals(5.6777f, z, 1e-4f)
    }
}
