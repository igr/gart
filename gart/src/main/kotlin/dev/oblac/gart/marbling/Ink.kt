package dev.oblac.gart.marbling

import dev.oblac.gart.MemPixels
import dev.oblac.gart.Pixels
import dev.oblac.gart.SampleMode

/**
 * What the bath looks like before anything lands on it: the colour the reverse walk arrives at
 * when no drop claims a point. A flat colour is plain marbling paper. An image means the whole
 * history of drops and strokes is applied to that picture - the way to rake a finished render.
 */
fun interface Ink {
    fun at(x: Float, y: Float): Int

    companion object {
        fun flat(color: Int) = Ink { _, _ -> color }

        /**
         * [pixels] sampled bilinearly, [mode] says what lies past the edges. Pixel centres sit at
         * +0.5, the same convention the renderer samples with, so with no ops the picture comes
         * back untouched. The picture is copied here: rendering the result back over the same
         * buffer is fine, and later changes to it are not seen - make a new ink for a new frame.
         */
        fun image(pixels: Pixels, mode: SampleMode = SampleMode.CLAMP, background: Int = 0): Ink {
            val copy = MemPixels(pixels.d)
            copy.copyPixelsFrom(pixels)
            return Ink { x, y -> copy.sampleBilinear((x - 0.5f).toDouble(), (y - 0.5f).toDouble(), mode, background) }
        }
    }
}
