package dev.oblac.gart

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FramesTest {

    @Test
    fun testFrame0() {
        val fc = FramesCounter(25)

        assertEquals(FramesCount.ZERO, fc.count)
        assertEquals(0, fc.time.inWholeMilliseconds)
        assertEquals(0.seconds, fc.time)
    }

    @Test
    fun testFrame1() {
        val fc = FramesCounter(25)
        repeat(1) { fc.tick() }

        assertEquals(FramesCount(1), fc.count)
        assertEquals(400, fc.time.inWholeMilliseconds)
        assertEquals(0.04.seconds, fc.time)
    }

    @Test
    fun testFrame24() {
        val fc = FramesCounter(25)
        repeat(24) { fc.tick() }

        assertEquals(FramesCount(24), fc.count)
        assertEquals(960, fc.time.inWholeMilliseconds)
        assertEquals(0.96.seconds, fc.time)
    }

    @Test
    fun testFrame25() {
        val fc = FramesCounter(25)
        repeat(25) { fc.tick() }

        assertEquals(FramesCount(25), fc.count)
        assertEquals(1000, fc.time.inWholeMilliseconds)
        assertEquals(1.00.seconds, fc.time)
    }


}
