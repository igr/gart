package dev.oblac.gart.fx

import dev.oblac.gart.Gartvas
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint

fun blur(gartvas: Gartvas, intensity: Float = 2f) {
    val image = gartvas.snapshot()
    gartvas.canvas.drawImage(image, 0f, 0f, Paint()
        .apply { imageFilter = ImageFilter.makeBlur(intensity, intensity, FilterTileMode.CLAMP) })
}
