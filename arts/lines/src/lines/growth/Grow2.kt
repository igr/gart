package lines.growth

import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.smooth.toSmoothQuadraticPath
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Point
import kotlin.random.Random

fun main() {
    val clBack = NipponColors.col015_ENJI
    val clAccent = NipponColors.col190_HANADA
    val clLine = NipponColors.col233_SHIRONERI


    val gart = Gart.of("growth2", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val d = g.d
    val c = g.canvas
    c.clear(clBack)

    var paths = List(70) { row ->
        val y = 180f + row * 10f
        List(9) { col ->
            Point(80f + col * 108f, y)
        }
    }

    val rng = Random(173)
    val guidePaint = strokeOf(clLine, 0.35f).apply {
        alpha = 35
        strokeCap = PaintStrokeCap.ROUND
        strokeJoin = PaintStrokeJoin.ROUND
    }

    repeat(180) {
        paths = paths.mapIndexed { row, path ->
            val noisy = path.withVaryingSplineNoise(
                minNoise = 0.4f,
                maxNoise = 3.5f + row * 0.12f,
                random = rng,
                preserveEnds = true,
            )
            val spline = noisy.toSmoothQuadraticPath()
            c.drawPath(spline, guidePaint)
            noisy
        }
        if (it == 150) {
            c.drawCircle(Circle(d.cx + 180, d.cy, 200f), fillOf(clAccent).apply {
                this.blendMode = BlendMode.MODULATE
            })
        }
    }
    c.drawCircle(Circle(220f, d.hf - 160f, 60f), fillOf(clBack).apply {
        this.blendMode = BlendMode.MULTIPLY
    })

    gart.window().showImage(g)
    gart.saveImage(g)
}
