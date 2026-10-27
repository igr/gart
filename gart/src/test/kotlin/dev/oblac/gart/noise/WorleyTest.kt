package dev.oblac.gart.noise

import dev.oblac.gart.math.hash01
import dev.oblac.gart.vector.Vec3
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorleyTest {

    @Test
    fun `worley is zero on a feature point`() {
        // the point of the cell (2, -1, 5) for seed 7
        val p = Vec3(2 + hash01(2, -1, 5, 7), -1 + hash01(2, -1, 5, 8), 5 + hash01(2, -1, 5, 9))
        assertEquals(0f, worley(p, 7))
    }

    @Test
    fun `worley stays under the diagonal of a cell`() {
        for (i in 0 until 1000) {
            val p = Vec3(i * 0.37f, i * 0.11f - 20f, i * 0.53f)
            val w = worley(p, 3)
            assertTrue(w >= 0f && w <= sqrt(3f), "$p -> $w")
        }
    }
}
