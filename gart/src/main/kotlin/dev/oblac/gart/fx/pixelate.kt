package dev.oblac.gart.fx

import dev.oblac.gart.Gartvas
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

fun pixelate(gartvas: Gartvas, pixelSize: Int) {
    val width = gartvas.d.w
    val height = gartvas.d.h
    val image = gartvas.snapshot();

    // downscale
    val smallWidth = width / pixelSize
    val smallHeight = height / pixelSize
    val scaledImage = image.scaleImage(smallWidth, smallHeight)

    // upscale back with nearest-neighbor effect
    gartvas.canvas.drawImageRect(
        scaledImage,
        Rect(0f, 0f, smallWidth.toFloat(), smallHeight.toFloat()),
        Rect(0f, 0f, width.toFloat(), height.toFloat()),
        SamplingMode.DEFAULT,
        null,
        true
    )
}
