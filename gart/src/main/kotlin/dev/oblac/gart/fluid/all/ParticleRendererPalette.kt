package dev.oblac.gart.fluid.all

import dev.oblac.gart.color.Palette
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode

/**
 * Particle renderer that uses a palette expanded to 255 colors.
 * Color at index 0 is used as background.
 */
class ParticleRendererPalette(
    private val canvas: Canvas,
    private val palette: Palette
) : ParticleRenderer {

    private val paint = Paint().apply { mode = PaintMode.FILL }
    private val backgroundColor = palette[0]

    override fun clear() {
        canvas.clear(backgroundColor)
    }

    override fun renderPixel(x: Int, y: Int, value: Float, blockSize: Float) {
        paint.color = palette.bound(value * (palette.size - 1))
        //canvas.drawRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), blockSize, blockSize), paint)
        canvas.drawCircle(x.toFloat(), y.toFloat(), (blockSize / 2), paint)
    }
}
