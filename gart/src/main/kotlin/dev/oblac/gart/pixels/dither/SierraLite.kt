package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels

fun ditherSierraLite(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
	ditherErrorDiffusion(bitmap, DitherKernels.SIERRA_LITE, pixelSize, colorCount)
}
