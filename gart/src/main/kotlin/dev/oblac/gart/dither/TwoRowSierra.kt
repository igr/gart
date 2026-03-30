package dev.oblac.gart.dither

import dev.oblac.gart.Pixels

fun ditherTwoRowSierra(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.SIERRA2,
        pixelSize,
        colorCount
    )
}
