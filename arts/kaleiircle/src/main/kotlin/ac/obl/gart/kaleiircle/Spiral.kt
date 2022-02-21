package ac.obl.gart.kaleiircle

import ac.obl.gart.Box
import ac.obl.gart.Shape
import ac.obl.gart.gfx.strokeOf
import ac.obl.gart.math.cosd
import ac.obl.gart.math.sind
import ac.obl.gart.skia.Canvas
import ac.obl.gart.skia.Path
import ac.obl.gart.skia.Point
import kotlin.math.max

class MakeSpiral(private val box: Box) {

    operator fun invoke(dx: Float, dy: Float): Shape {
        val center = Point(box.cx + dx, box.cy + dy)
        val radius = max(box.w, box.h)
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
