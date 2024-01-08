package dev.oblac.gart

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FramesMarksTest {

    @Test
    fun testAfter() {
        val fc = FramesCounter(25)
        val mark = fc.marker().atNumber(15)

        repeat(10) { fc.tick() }
        assertFalse(mark.after())
        assertTrue(mark.before())
        assertFalse(mark.now())

        repeat(5) { fc.tick() }
        assertFalse(mark.after())
        assertFalse(mark.before())
        assertTrue(mark.now())

        repeat(1) { fc.tick() }
        assertTrue(mark.after())
        assertFalse(mark.before())
        assertFalse(mark.now())
    }
}
