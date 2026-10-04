package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.ln
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GammaTest {

    @Test
    fun lnGammaOfWholeNumbersIsLnOfTheFactorial() {
        assertEquals(0.0, lnGamma(1.0), 1e-13)
        assertEquals(0.0, lnGamma(2.0), 1e-13)
        assertEquals(ln(24.0), lnGamma(5.0), 1e-13)
        assertEquals(359.1342053695754, lnGamma(100.0), 1e-10) // ln 99!
    }

    @Test
    fun lnGammaOfHalvesAndSmallValues() {
        assertEquals(0.5 * ln(PI), lnGamma(0.5), 1e-13) // gamma(1/2) = sqrt(pi)
        assertEquals(ln(9.513507698668732), lnGamma(0.1), 1e-12)
    }

    @Test
    fun lnGammaTakesOnlyPositiveValues() {
        assertFailsWith<IllegalArgumentException> { lnGamma(0.0) }
        assertFailsWith<IllegalArgumentException> { lnGamma(-2.5) }
    }
}
