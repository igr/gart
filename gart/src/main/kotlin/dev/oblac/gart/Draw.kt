package dev.oblac.gart

import org.jetbrains.skia.Canvas

fun interface Draw {
    operator fun invoke(canvas: Canvas, dimension: Dimension)
}

fun interface DrawFrame {
    operator fun invoke(canvas: Canvas, dimension: Dimension, frames: Frames)
}
