package dev.oblac.gart.kaleiircle

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawable
import dev.oblac.gart.math.sinDeg
import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.Path

class MakeWaves(private val d: Dimension) {
    fun invoke(angle: Float = 0f, amplitude: Float = 20f, gap: Float = 10f, speed: Float = 2f): Drawable {
        val paths = mutableListOf<Path>()

        var y = -amplitude
        while (y < d.h + amplitude) {
            var x = 0f
            val p = Path().moveTo(x, y)
            while (x < d.w) {
                val dy = amplitude * sinDeg(x * speed)
                x++
                p.lineTo(x, y + dy)
            }
            paths.add(p)
            y += gap
        }

        val stroke = Paint().apply {
            color = 0x22000000
            strokeWidth = 1f
            setStroke(true)
        }

        return Drawable { canvas ->
            canvas.save()
            canvas.rotate(angle, d.cx, d.cy)
            paths.forEach {
                canvas.drawPath(it, stroke)
            }
            canvas.restore()
        }
    }
}
