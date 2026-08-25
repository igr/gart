package dev.oblac.gart.fx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.pixels.boxDownsample
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

fun Image.scaleImage(newWidth: Int, newHeight: Int): Image {
    val surface = Surface.makeRasterN32Premul(newWidth, newHeight)
    val canvas = surface.canvas

    canvas.drawImageRect(
        this,
        Rect(0f, 0f, width.toFloat(), height.toFloat()),
        Rect(0f, 0f, newWidth.toFloat(), newHeight.toFloat()),
        SamplingMode.DEFAULT,
        null,
        true
    )
    return surface.makeImageSnapshot()
}

/**
 * Shrinks a supersampled canvas by [ss] with a plain box average - see [boxDownsample]. Use this
 * rather than [scaleImage] to bring a 2x/3x render down: Skia's samplers only ever look at a
 * fixed couple of source px whatever the ratio, so hairlines drawn at that spacing keep their
 * aliasing and beat against the grid into ripples. The box takes every source px once.
 */
fun Gartvas.downsample(ss: Int): Gartvas {
    val out = Gartvas(Dimension(d.w / ss, d.h / ss))
    val dst = Gartmap(out)
    boxDownsample(Gartmap(this).pixels, ss, dst)
    dst.drawToCanvas()
    return out
}
