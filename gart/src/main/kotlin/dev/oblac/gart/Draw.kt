package dev.oblac.gart

import org.jetbrains.skia.Canvas

fun interface Draw {
    operator fun invoke(canvas: Canvas, dimension: Dimension)
}

fun interface DrawFrame {
    operator fun invoke(canvas: Canvas, dimension: Dimension, frames: Frames)
}

/**
 * Base class for drawing, used mostly with the Hot Reload feature.
 */
abstract class Drawing(private val g: Gartvas? = null) : DrawFrame {
    final override fun invoke(canvas: Canvas, dimension: Dimension, frames: Frames) {
        draw(canvas, dimension, frames)

        if (g == null) return
        val snapshot = g.snapshot()
        canvas.drawImage(snapshot, 0f, 0f)
    }

    open fun draw(canvas: Canvas, dimension: Dimension, frames: Frames) {}
}
