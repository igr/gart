package dev.oblac.gart

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FramesTest {

    @Test
    fun testFrame0() {
        val fc = FramesCounter(25)

        assertEquals(0, fc.count)
        assertEquals(0f, fc.time, 0.01f)
    }

    @Test
    fun testFrame1() {
        val fc = FramesCounter(25)
        repeat(1) { fc.tick() }

        assertEquals(1, fc.count)
        assertEquals(0.04f, fc.time, 0.01f)
    }

    @Test
    fun testFrame24() {
        val fc = FramesCounter(25)
        repeat(24) { fc.tick() }

        assertEquals(24, fc.count)
        assertEquals(0.96f, fc.time, 0.01f)
    }

    @Test
    fun testFrame25() {
        val fc = FramesCounter(25)
        repeat(25) { fc.tick() }

        assertEquals(25, fc.count)
        assertEquals(1.00f, fc.time, 0.01f)
    }


}
