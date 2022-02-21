package ac.obl.gart.kaleiircle

import ac.obl.gart.Shape
import ac.obl.gart.math.sind
import ac.obl.gart.skia.Canvas
import ac.obl.gart.skia.Paint
import ac.obl.gart.skia.Path

class MakeWaves(private val box: Box) {
    fun invoke(angle: Float = 0f, amplitude: Float = 20f, gap: Float = 10f, speed: Float = 2f): Shape {
        val paths = mutableListOf<Path>()

        var y = -amplitude
        while (y < box.h + amplitude) {
            var x = 0f
            val p = Path().moveTo(x, y)
            while (x < box.w) {
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
                canvas.rotate(angle, box.cx, box.cy)
                paths.forEach{
                    canvas.drawPath(it, stroke)
                }
                canvas.restore()
            }
        }
    }
}
