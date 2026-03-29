package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels

fun ditherSierra(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
	ditherErrorDiffusion(bitmap, DitherKernels.SIERRA3, pixelSize, colorCount)
}
