package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels

fun ditherFloydSteinberg(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
	ditherErrorDiffusion(bitmap, DitherKernels.FLOYD_STEINBERG, pixelSize, colorCount)
}
