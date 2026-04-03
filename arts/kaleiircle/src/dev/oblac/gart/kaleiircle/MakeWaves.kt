package dev.oblac.gart.kaleiircle

import dev.oblac.gart.Dimension
import dev.oblac.gart.Draw
import dev.oblac.gart.math.sinDeg
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder

class MakeWaves(private val d: Dimension) {
    fun invoke(angle: Float = 0f, amplitude: Float = 20f, gap: Float = 10f, speed: Float = 2f): Draw {
        val paths = mutableListOf<Path>()

        var y = -amplitude
        while (y < d.h + amplitude) {
            var x = 0f
            val p = PathBuilder().moveTo(x, y)
            while (x < d.w) {
                val dy = amplitude * sinDeg(x * speed)
                x++
                p.lineTo(x, y + dy)
            }
            paths.add(p.detach())
            y += gap
        }

        val stroke = Paint().apply {
            color = 0x22000000
            strokeWidth = 1f
            setStroke(true)
        }

        return Draw { canvas, d ->
            canvas.save()
            canvas.rotate(angle, d.cx, d.cy)
            paths.forEach {
                canvas.drawPath(it, stroke)
            }
            canvas.restore()
        }
    }
}
