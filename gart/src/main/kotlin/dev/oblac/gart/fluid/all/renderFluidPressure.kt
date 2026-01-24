package dev.oblac.gart.fluid.all

import dev.oblac.gart.color.argb
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect

/**
 * Render pressure field visualization.
 */
fun renderFluidPressure(canvas: Canvas, solver: FluidSolver) {
    canvas.clear(argb(255, 128, 128, 128))

    val width = solver.width
    val pressure = solver.pressure()
    val velWidth = solver.velWidth
    val velHeight = solver.velHeight
    val scale = width.toFloat() / velWidth

    val paint = Paint().apply {
        mode = PaintMode.FILL
    }

    for (y in 0 until velHeight) {
        for (x in 0 until velWidth) {
            val p = pressure[y * velWidth + x]

            // Map pressure to color (blue for negative, red for positive)
            val intensity = (p * 0.5f).coerceIn(-1f, 1f)
            val (r, g, b) = if (intensity > 0) {
                Triple(
                    (128 + 127 * intensity).toInt(),
                    (128 - 64 * intensity).toInt(),
                    (128 - 64 * intensity).toInt()
                )
            } else {
                Triple(
                    (128 + 64 * intensity).toInt(),
                    (128 + 64 * intensity).toInt(),
                    (128 - 127 * intensity).toInt()
                )
            }
            paint.color = argb(255, r, g, b)
            canvas.drawRect(
                Rect.makeXYWH(
                    x * scale, y * scale,
                    scale, scale
                ),
                paint
            )
        }
    }
}
