package dev.oblac.gart.dither

import dev.oblac.gart.Pixels

/**
 * Wong-Allebach optimized error diffusion kernel.
 * https://doi.org/10.1117/12.271597
 */
fun ditherWongAllebach(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.WONG_ALLEBACH,
        pixelSize,
        colorCount
    )
}
