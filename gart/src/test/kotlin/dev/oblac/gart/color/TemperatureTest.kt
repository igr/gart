package dev.oblac.gart.color

import dev.oblac.gart.color.space.temperature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TemperatureTest {

    @Test
    fun testTemperature() {
        assertEquals(1000, "0xff3300".parseColor4f().temperature)
        assertEquals(2000, "0xff8a13".parseColor4f().temperature)
        assertEquals(4999, "0xffe3cd".parseColor4f().temperature)
        assertEquals(10115, "0xcbdbff".parseColor4f().temperature)
        assertEquals(15169, "0xb3ccff".parseColor4f().temperature)
    }
}
