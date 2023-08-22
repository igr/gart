package studio.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals

class SqrtTest {

    @Test
    fun testFastSqrt() {
        for (i in 1..20000) {
            val realSqrt = sqrt(i.toDouble())
            val sqrt = fastSqrt(i.toDouble())
            val sqrt2 = fastFastSqrt(i.toDouble())

            assertEquals(realSqrt, sqrt, 0.17)
            assertEquals(realSqrt, sqrt2, 3.7)
        }
    }
}
