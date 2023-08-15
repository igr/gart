package studio.oblac.gart.kaleiircle

import studio.oblac.gart.Dimension
import studio.oblac.gart.Shape
import studio.oblac.gart.gfx.strokeOf
import studio.oblac.gart.math.cosd
import studio.oblac.gart.math.sind
import studio.oblac.gart.skia.Canvas
import studio.oblac.gart.skia.Path
import studio.oblac.gart.skia.Point
import kotlin.math.max

class MakeSpiral(private val d: Dimension) {

    operator fun invoke(dx: Float, dy: Float): Shape {
        val center = Point(d.cx + dx, d.cy + dy)
        val radius = max(d.w, d.h)
        val path = Path()

        for (angle in 0..3600) {
            val scaledRadius = radius * angle / 3600
            val x = center.x + scaledRadius * cosd(angle)
            val y = center.y + scaledRadius * sind(angle)
            val point = Point(x, y)
            if (angle == 0) {
                path.moveTo(point)
            } else {
                path.lineTo(point)
            }
        }

        return object : Shape {
            override fun draw(canvas: Canvas) {
                canvas.drawPath(path, strokeOf(0x11000000, 4f))
            }
        }
    }
}
