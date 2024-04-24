package dev.oblac.gart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FramesTest {

    @Test
    fun testFrame0() {
        val fc = FrameCounter(25)

        assertEquals(0, fc.frame)
        assertEquals(0, fc.time.inWholeMilliseconds)
        assertEquals(0.seconds, fc.time)
    }

    @Test
    fun testFrame1() {
        val fc = FrameCounter(25)
        repeat(1) { fc.tick() }

        assertEquals(1, fc.frame)
        assertEquals(40, fc.time.inWholeMilliseconds)
        assertEquals(0.04.seconds, fc.time)
    }

    @Test
    fun testFrame24() {
        val fc = FrameCounter(25)
        repeat(24) { fc.tick() }

        assertEquals(24, fc.frame)
        assertEquals(960, fc.time.inWholeMilliseconds)
        assertEquals(0.96.seconds, fc.time)
    }

    @Test
    fun testFrame25() {
        val fc = FrameCounter(25)
        repeat(25) { fc.tick() }

        assertEquals(25, fc.frame)
        assertEquals(1000, fc.time.inWholeMilliseconds)
        assertEquals(1.00.seconds, fc.time)
    }

}
