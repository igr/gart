package dev.oblac.gart.gfx

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter

fun Canvas.drawBitmap(b: Gartmap) = this.drawImage(b.image(), 0f, 0f)

fun Canvas.drawImage(image: Image) = drawImage(image, 0f, 0f)

/**
 * Draws the Gartvas image to the canvas.
 */
fun Canvas.draw(g: Gartvas) = drawImage(g.snapshot(), 0f, 0f)

/**
 * Saves a new layer with the specified image filter applied.
 */
fun Canvas.saveLayer(imageFilter: ImageFilter) = this.saveLayer(null, paintOfImageFilter(imageFilter))


