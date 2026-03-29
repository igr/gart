package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels

fun ditherStucki(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
	ditherErrorDiffusion(bitmap, DitherKernels.STUCKI, pixelSize, colorCount)
}
