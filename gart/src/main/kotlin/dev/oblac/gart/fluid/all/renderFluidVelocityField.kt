package dev.oblac.gart.fluid.all

import dev.oblac.gart.color.BgColors
import dev.oblac.gart.gfx.strokeOfBlack
import org.jetbrains.skia.Canvas
import kotlin.math.sqrt

/**
 * Render velocity vector field directly on the canvas.
 */
fun renderFluidVelocityField(
    canvas: Canvas,
    solver: FluidSolver,
    vectorSpacing: Int = 10,
    vectorScale: Float = 2.5f,
    arrowSize: Float = 3f
) {
    canvas.clear(BgColors.coconutMilk)

    val width = solver.width
    val velocityU = solver.velocityU()
    val velocityV = solver.velocityV()
    val velWidth = solver.velWidth
    val velHeight = solver.velHeight
    val scale = width.toFloat() / velWidth

    val paint = strokeOfBlack(2f)

    for (y in vectorSpacing / 2 until velHeight step vectorSpacing) {
        for (x in vectorSpacing / 2 until velWidth step vectorSpacing) {
            val idx = y * velWidth + x
            val u = velocityU[idx]
            val v = velocityV[idx]

            val startX = x * scale
            val startY = y * scale
            val endX = startX + u * vectorScale
            val endY = startY + v * vectorScale

            canvas.drawLine(startX, startY, endX, endY, paint)

            // Draw arrow head
            val mag = sqrt(u * u + v * v)
            if (mag > 0.1f) {
                val dirX = u / mag
                val dirY = v / mag
                val perpX = -dirY * arrowSize
                val perpY = dirX * arrowSize

                canvas.drawLine(
                    endX, endY,
                    endX - dirX * arrowSize + perpX * 0.5f,
                    endY - dirY * arrowSize + perpY * 0.5f,
                    paint
                )
                canvas.drawLine(
                    endX, endY,
                    endX - dirX * arrowSize - perpX * 0.5f,
                    endY - dirY * arrowSize - perpY * 0.5f,
                    paint
                )
            }
        }
    }
}
