package dev.oblac.gart.dither

import dev.oblac.gart.Pixels

fun ditherJarvisJudiceNinke(bitmap: Pixels, pixelSize: Int = 1, colorCount: Int = 256) {
    ditherErrorDiffusion(
        bitmap,
        DitherKernels.JARVIS_JUDICE_NINKE,
        pixelSize,
        colorCount
    )
}
