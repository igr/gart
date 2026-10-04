package dev.oblac.gart.color

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RgbTest {

    @Test
    fun floatChannelsPackWithFullAlpha() {
        // 0.5 * 255 = 127.5, cut to 127 like argb does
        assertEquals(0xFFFF7F00.toInt(), rgb(1f, 0.5f, 0f))
    }

    @Test
    fun floatChannelsClamp() {
        assertEquals(0xFFFF0033.toInt(), rgb(2f, -1f, 0.2f))
    }

    @Test
    fun floatRgbIsArgbWithAlphaOne() {
        for ((r, g, b) in listOf(Triple(0.1f, 0.6f, 0.9f), Triple(0f, 0f, 0f), Triple(0.999f, 0.004f, 0.5f))) {
            assertEquals(argb(1f, r, g, b), rgb(r, g, b))
        }
    }
}
