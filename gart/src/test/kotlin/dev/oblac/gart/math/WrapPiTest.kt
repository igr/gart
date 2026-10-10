package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.roundToLong
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WrapPiTest {

    // the loop the pieces write by hand
    private fun handLoop(a: Float): Float {
        var b = a
        while (b > PIf) b -= TAUf
        while (b < -PIf) b += TAUf
        return b
    }

    @Test
    fun `angles inside the range come back as they are`() {
        for (a in floatArrayOf(0f, 1f, -1f, 3f, -3f, PIf, -PIf)) {
            assertEquals(a, wrapPi(a))
        }
    }

    @Test
    fun `whole turns go away`() {
        assertEquals(1f, wrapPi(1f + TAUf), 1e-6f)
        assertEquals(-1f, wrapPi(-1f - TAUf), 1e-6f)
        assertEquals(PIf - 1f, wrapPi(-PIf - 1f), 1e-6f)
        assertEquals(1f - PIf, wrapPi(PIf + 1f), 1e-6f)
    }

    @Test
    fun `up to two turns it gives the same bits as the hand loop`() {
        val n = 20_000
        for (i in -n..n) {
            val a = (i / n.toFloat()) * 2 * TAUf
            assertEquals(handLoop(a).toRawBits(), wrapPi(a).toRawBits(), "a = $a")
        }
        for (a in floatArrayOf(PIf.nextUp(), (-PIf).nextDown(), TAUf, -TAUf, 2 * TAUf, -2 * TAUf)) {
            assertEquals(handLoop(a).toRawBits(), wrapPi(a).toRawBits(), "a = $a")
        }
    }

    @Test
    fun `big angles lose whole turns only`() {
        for (a in floatArrayOf(100f, -100f, 1000f, -12345.6f, 1e6f, -3e7f)) {
            val w = wrapPi(a)
            assertTrue(w in -PIf..PIf, "a = $a gave $w")
            val turns = (a.toDouble() - w.toDouble()) / TAUf.toDouble()
            assertTrue(abs(turns - turns.roundToLong()) < 1e-6, "a = $a gave $w, $turns turns")
        }
    }

    @Test
    fun `infinity and NaN give NaN`() {
        assertTrue(wrapPi(Float.POSITIVE_INFINITY).isNaN())
        assertTrue(wrapPi(Float.NEGATIVE_INFINITY).isNaN())
        assertTrue(wrapPi(Float.NaN).isNaN())
    }
}
