package dev.oblac.gart.color

import dev.oblac.gart.color.space.contrastRatio
import dev.oblac.gart.color.space.luma
import dev.oblac.gart.color.space.luminance
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LuminanceTest {

    private val white = Color4f(1f, 1f, 1f, 1f)
    private val black = Color4f(0f, 0f, 0f, 1f)
    private val grey = Color4f(0.5f, 0.5f, 0.5f, 1f)

    @Test
    fun luminanceMakesTheChannelsLinearFirst() {
        // ((0.5 + 0.055) / 1.055)^2.4, by hand
        assertEquals(0.21404f, grey.luminance, 1e-5f)
        assertEquals(1f, white.luminance, 1e-6f)
        assertEquals(0f, black.luminance)
    }

    @Test
    fun luminanceOfAPrimaryIsItsWeight() {
        assertEquals(0.2126f, Color4f(1f, 0f, 0f, 1f).luminance, 1e-6f)
        assertEquals(0.7152f, Color4f(0f, 1f, 0f, 1f).luminance, 1e-6f)
        assertEquals(0.0722f, Color4f(0f, 0f, 1f, 1f).luminance, 1e-6f)
    }

    @Test
    fun lumaWeighsTheEncodedChannels() {
        assertEquals(0.5f, grey.luma, 1e-6f)
        assertEquals(0.39358f, Color4f(0.2f, 0.4f, 0.9f, 1f).luma, 1e-6f)
    }

    @Test
    fun contrastRatioFollowsWcag() {
        assertEquals(21.0, white.contrastRatio(black), 1e-6)
        // 1.05 / (0.21404 + 0.05)
        assertEquals(3.97665, white.contrastRatio(grey), 1e-4)
        assertEquals(white.contrastRatio(grey), grey.contrastRatio(white))
    }
}
