package dev.oblac.gart.fx

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
