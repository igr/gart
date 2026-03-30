package dev.oblac.gart.dither

import dev.oblac.gart.Pixels

/**
 * Shiau-Fan set out to reduce worm artifacts in error diffusion dithering.
 */
fun ditherShiauFan1(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.SHIAU_FAN1,
        pixelSize,
        colorCount
    )
}

fun ditherShiauFan2(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.SHIAU_FAN2,
        pixelSize,
        colorCount
    )
}
