package studio.oblac.gart.kaleiircle

import studio.oblac.gart.Dimension
import studio.oblac.gart.Shape
import studio.oblac.gart.math.sind
import studio.oblac.gart.skia.Canvas
import studio.oblac.gart.skia.Paint
import studio.oblac.gart.skia.Path

class MakeWaves(private val d: Dimension) {
    fun invoke(angle: Float = 0f, amplitude: Float = 20f, gap: Float = 10f, speed: Float = 2f): Shape {
        val paths = mutableListOf<Path>()

        var y = -amplitude
        while (y < d.h + amplitude) {
            var x = 0f
            val p = Path().moveTo(x, y)
            while (x < d.w) {
                val dy = amplitude * sind(x * speed)
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

        return object: Shape {
            override fun draw(canvas: Canvas) {
                canvas.save()
                canvas.rotate(angle, d.cx, d.cy)
                paths.forEach{
                    canvas.drawPath(it, stroke)
                }
                canvas.restore()
            }
        }
    }
}