package dev.oblac.gart

import dev.oblac.gart.skia.Canvas

fun interface Draw {
    operator fun invoke(canvas: Canvas, dimension: Dimension)
}

fun interface DrawFrame {
    operator fun invoke(canvas: Canvas, dimension: Dimension, frames: Frames)
}
