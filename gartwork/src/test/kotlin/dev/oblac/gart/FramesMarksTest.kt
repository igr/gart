package dev.oblac.gart

import org.junit.jupiter.api.Test
import kotlin.test.fail

class FramesMarksTest {

    @Test
    fun testAfter() {
        val fc = FrameCounter(25)
        val mark = 15L

        repeat(10) { fc.tick() }
        fc.onAfterFrame(mark) { fail() }
        fc.onFrame(mark) { fail() }

        repeat(5) { fc.tick() }
        fc.onBeforeFrame(mark) { fail() }
        fc.onAfterFrame(mark) { fail() }

        repeat(1) { fc.tick() }
        fc.onBeforeFrame(mark) { fail() }
        fc.onFrame(mark) { fail() }
    }
}
