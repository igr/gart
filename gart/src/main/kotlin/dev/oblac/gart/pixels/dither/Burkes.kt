package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels

/**
 * Seven years after Stucki published his improvement to Jarvis, Judice, Ninke dithering,
 * Daniel Burkes suggested a further improvement.
 */
fun ditherBurkes(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
	ditherErrorDiffusion(bitmap, DitherKernels.BURKES, pixelSize, colorCount)
}
