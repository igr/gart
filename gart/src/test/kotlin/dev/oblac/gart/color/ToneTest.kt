package dev.oblac.gart.color

import org.junit.jupiter.api.Test
import kotlin.math.ln
import kotlin.test.assertEquals

class ToneTest {

    @Test
    fun toneCurveGoesFromZeroToOne() {
        val tone = ToneCurve(1.3f, 1.8f)
        assertEquals(0f, tone(0f))
        assertEquals(1f, tone(100f), 1e-6f)
    }

    @Test
    fun toneCurveHalvesTheDistanceToWhiteEveryLnTwo() {
        // 1 - e^(-ln 2) = 1 - 1/2
        assertEquals(0.5f, ToneCurve(1f)(ln(2f)), 1e-6f)
        assertEquals(0.25f, ToneCurve(1f, 2f)(ln(2f)), 1e-6f)
    }

    @Test
    fun inkRampEndsAreTheInkAndThePaper() {
        val ramp = InkRamp(0xFF151B2C.toInt(), 0xFFEEE8DB.toInt())
        assertEquals(0xFF151B2C.toInt(), ramp.at(0f))
        assertEquals(0xFFEEE8DB.toInt(), ramp.at(1f))
    }

    @Test
    fun inkRampMixesInLinearLight() {
        // black to white, half way in light is (0.5^(1/2.2)) * 255 = 186, not the srgb 128
        assertEquals(0xFFBABABA.toInt(), InkRamp(0xFF000000.toInt(), 0xFFFFFFFF.toInt()).at(0.5f))
    }

    @Test
    fun inkRampClamps() {
        val ramp = InkRamp(0xFF151B2C.toInt(), 0xFFEEE8DB.toInt())
        assertEquals(ramp.at(0f), ramp.at(-3f))
        assertEquals(ramp.at(1f), ramp.at(7f))
    }

    @Test
    fun rgbWalksEachChannelOnItsOwn() {
        assertEquals(0xFF00FFBA.toInt(), InkRamp(0xFF000000.toInt(), 0xFFFFFFFF.toInt()).rgb(0f, 1f, 0.5f))
    }
}
