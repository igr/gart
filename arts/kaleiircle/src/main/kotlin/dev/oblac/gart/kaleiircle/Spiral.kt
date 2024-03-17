package dev.oblac.gart.kaleiircle

import dev.oblac.gart.Dimension
import dev.oblac.gart.Draw
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.cosDeg
import dev.oblac.gart.math.sinDeg
import dev.oblac.gart.skia.Path
import dev.oblac.gart.skia.Point
import kotlin.math.max

class MakeSpiral(private val d: Dimension) {

    operator fun invoke(dx: Float, dy: Float): Draw {
        val center = Point(d.cx + dx, d.cy + dy)
        val radius = max(d.w, d.h)
        val path = Path()

        for (angle in 0..3600) {
            val scaledRadius = radius * angle / 3600
            val x = center.x + scaledRadius * cosDeg(angle)
            val y = center.y + scaledRadius * sinDeg(angle)
            val point = Point(x, y)
            if (angle == 0) {
                path.moveTo(point)
            } else {
                path.lineTo(point)
            }
        }

        return Draw { canvas, d -> canvas.drawPath(path, strokeOf(0x11000000, 4f)) }
    }
}
