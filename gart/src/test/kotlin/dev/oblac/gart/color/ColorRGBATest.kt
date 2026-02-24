package dev.oblac.gart.color

import dev.oblac.gart.color.space.RGBA
import org.jetbrains.skia.Color4f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Color4fTest {

    @Test
    fun testColor4f1() {
        val rgba = RGBA.of(Color4f(0.5f, 0.5f, 0.5f, 1f))
        assertEquals(127, rgba.r)
        assertEquals(127, rgba.g)
        assertEquals(127, rgba.b)
        assertEquals(255, rgba.a)

        val c = rgba.toColor4f()
        assertEquals(0.5f, c.r, 0.05f)
        assertEquals(0.5f, c.g, 0.05f)
        assertEquals(0.5f, c.b, 0.05f)
        assertEquals(1f, c.a)
    }

    @Test
    fun testColor4f2() {
        val rgba = RGBA.of(Color4f(0.1f, 0.2f, 0.3f, 1f))
        assertEquals(25, rgba.r)
        assertEquals(51, rgba.g)
        assertEquals(76, rgba.b)
        assertEquals(255, rgba.a)

        val c = rgba.toColor4f()
        assertEquals(0.1f, c.r, 0.05f)
        assertEquals(0.2f, c.g, 0.05f)
        assertEquals(0.3f, c.b, 0.05f)
        assertEquals(1f, c.a)
    }
}
