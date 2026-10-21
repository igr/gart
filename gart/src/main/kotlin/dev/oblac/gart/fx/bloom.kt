package dev.oblac.gart.fx

import dev.oblac.gart.gfx.blurPaint
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image

/**
 * Draws [image] back over the canvas, blurred by [sigma] and blended with [blend] at [strength]
 * (`0f..1f`, clamped). For a tight core with wide halos, call it a few times with a bigger sigma
 * each time. The blur repeats the edge pixels (`CLAMP`), so the edges of a full-canvas copy do not
 * go dark.
 */
fun bloom(canvas: Canvas, image: Image, sigma: Float, strength: Float, blend: BlendMode = BlendMode.SCREEN) {
    canvas.drawImage(image, 0f, 0f, blurPaint(sigma, blend, (strength.coerceIn(0f, 1f) * 255).toInt(), FilterTileMode.CLAMP))
}
