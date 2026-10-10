package dev.oblac.gart.math

import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.Radians
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NormalizeAngleTest {

    // the loop that normalizeRad, normalizeDeg and both normalize() had before
    private fun oldLoop(v: Float, turn: Float): Float {
        var a = v
        while (a < 0) a += turn
        while (a > turn) a -= turn
        return a
    }

    @Test
    fun `up to two turns it gives the same bits as the old loop`() {
        val n = 20_000
        for (i in -n..n) {
            val rad = (i / n.toFloat()) * 2 * DOUBLE_PIf
            val deg = (i / n.toFloat()) * 720f
            val oldRad = oldLoop(rad, DOUBLE_PIf).toRawBits()
            val oldDeg = oldLoop(deg, 360f).toRawBits()
            assertEquals(oldRad, normalizeRad(rad).toRawBits(), "rad = $rad")
            assertEquals(oldRad, Radians(rad).normalize().value.toRawBits(), "rad = $rad")
            assertEquals(oldDeg, normalizeDeg(deg).toRawBits(), "deg = $deg")
            assertEquals(oldDeg, Degrees(deg).normalize().value.toRawBits(), "deg = $deg")
        }
    }

    @Test
    fun `big angles land in the range and lose whole turns only`() {
        for (v in floatArrayOf(100f, -100f, 1000f, -12345.6f, 1e6f, -3e7f, 1e9f)) {
            val r = normalizeRad(v)
            assertTrue(r in 0f..DOUBLE_PIf, "rad $v gave $r")
            assertWholeTurns(v, r, DOUBLE_PIf)
            val d = normalizeDeg(v)
            assertTrue(d in 0f..360f, "deg $v gave $d")
            assertWholeTurns(v, d, 360f)
        }
    }

    @Test
    fun `infinity and NaN give NaN`() {
        for (v in floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN)) {
            assertTrue(normalizeRad(v).isNaN(), "rad $v")
            assertTrue(Radians(v).normalize().value.isNaN(), "Radians $v")
            assertTrue(Degrees(v).normalize().value.isNaN(), "Degrees $v")
            assertTrue(v.subDeg(10f).isNaN(), "subDeg $v")
            assertTrue(v.addDeg(10f).isNaN(), "addDeg $v")
        }
    }

    private fun assertWholeTurns(v: Float, w: Float, turn: Float) {
        val turns = (v.toDouble() - w.toDouble()) / turn.toDouble()
        assertTrue(abs(turns - turns.roundToLong()) < 1e-6, "$v gave $w, $turns turns")
    }
}
