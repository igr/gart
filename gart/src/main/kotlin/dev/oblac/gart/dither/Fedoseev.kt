package dev.oblac.gart.dither

import dev.oblac.gart.Pixels

/**
 * Fedoseev optimized error diffusion kernels.
 * https://doi.org/10.1117/12.2180540
 */
fun ditherFedoseev(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.FEDOSEEV,
        pixelSize,
        colorCount
    )
}

fun ditherFedoseev2(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.FEDOSEEV2,
        pixelSize,
        colorCount
    )
}

fun ditherFedoseev3(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.FEDOSEEV3,
        pixelSize,
        colorCount
    )
}

fun ditherFedoseev4(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.FEDOSEEV4,
        pixelSize,
        colorCount
    )
}
