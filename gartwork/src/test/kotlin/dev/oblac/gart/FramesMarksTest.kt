package dev.oblac.gart

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FramesMarksTest {

    @Test
    fun testAfter() {
        val fc = FramesCounter(25)
        val mark = FrameMarkerBuilder(fc).atFrame(15)

        repeat(10) { fc.tick() }
        assertFalse(fc after mark)
        assertTrue(fc before mark)
        assertFalse(fc isNow mark)

        repeat(5) { fc.tick() }
        assertFalse(fc after mark)
        assertFalse(fc before mark)
        assertTrue(fc isNow mark)

        repeat(1) { fc.tick() }
        assertTrue(fc after mark)
        assertFalse(fc before mark)
        assertFalse(fc isNow mark)
    }
}
