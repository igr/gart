package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels

/**
 * During the mid-1980's, dithering became increasingly popular as
 * computer hardware advanced to support more powerful video drivers and displays.
 * One of the best dithering algorithms from this era was developed by Bill Atkinson,
 * a Apple employee who worked on everything from MacPaint
 * (which he wrote from scratch for the original Macintosh) to HyperCard and QuickDraw.
 */
fun ditherAtkinson(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
	ditherErrorDiffusion(bitmap, DitherKernels.ATKINSON, pixelSize, colorCount)
}
