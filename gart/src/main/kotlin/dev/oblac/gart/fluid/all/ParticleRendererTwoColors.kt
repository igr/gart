package dev.oblac.gart.fluid.all

import dev.oblac.gart.color.*
import dev.oblac.gart.math.lerp
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect

/**
 * Particle renderer that interpolates between background and particle colors.
 */
class ParticleRendererTwoColors(
    private val canvas: Canvas,
    private val backgroundColor: Int = CssColors.oldLace,
    private val particleColor: Int = CssColors.midnightBlue
) : ParticleRenderer {

    private val paint = Paint().apply { mode = PaintMode.FILL }

    private val bgR = red(backgroundColor)
    private val bgG = green(backgroundColor)
    private val bgB = blue(backgroundColor)

    private val pR = red(particleColor)
    private val pG = green(particleColor)
    private val pB = blue(particleColor)

    override fun clear() {
        canvas.clear(backgroundColor)
    }

    override fun renderPixel(x: Int, y: Int, value: Float, blockSize: Float) {
        val r = lerp(bgR, pR, value).toInt()
        val g = lerp(bgG, pG, value).toInt()
        val b = lerp(bgB, pB, value).toInt()
        paint.color = argb(255, r, g, b)
        canvas.drawRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), blockSize, blockSize), paint)
    }
}
