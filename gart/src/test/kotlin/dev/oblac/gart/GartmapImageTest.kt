package dev.oblac.gart

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GartmapImageTest {

    /**
     * Regression guard for the per-frame native-image leak.
     *
     * [Gartmap.image] mints a Skia [org.jetbrains.skia.Image] via `Image.makeFromBitmap`,
     * a native (off-heap) object. Render loops call it once per frame and never closed the
     * result, so off-heap memory grew the longer the sketch ran. The Gartmap now owns the
     * image and releases the previous one as soon as a new one is requested.
     */
    @Test
    fun imageReleasesPreviouslyReturnedNativeImage() {
        val map = Gartmap(Dimension(8, 8))

        val first = map.image()
        assertFalse(first.isClosed, "a freshly returned image must be open")

        val second = map.image()
        assertTrue(first.isClosed, "requesting a new image must close the previous one (no per-frame leak)")
        assertFalse(second.isClosed, "the latest image must be open")
    }

    /** Simulates a render loop: after N frames there must still be at most one live native image. */
    @Test
    fun repeatedImageCallsKeepAtMostOneLiveNativeImage() {
        val map = Gartmap(Dimension(8, 8))
        var previous = map.image()
        repeat(64) {
            val current = map.image()
            assertTrue(previous.isClosed, "previous frame's image must be released")
            assertFalse(current.isClosed, "current frame's image must be live")
            previous = current
        }
    }
}
