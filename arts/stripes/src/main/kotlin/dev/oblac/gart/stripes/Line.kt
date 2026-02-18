package dev.oblac.gart.stripes

import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.gfx.strokeOfWhite
import dev.oblac.gart.math.GaussianFunction
import dev.oblac.gart.math.map
import dev.oblac.gart.noise.PerlinNoise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.random.Random

const val segments = 50
val gap = gart.d.w / segments
val gauss = GaussianFunction(160, segments / 2, 14)
fun gaussianCurve(x: Number) = gauss(x)
const val variety = 0.03f
val perlin = PerlinNoise()

class Line(private val offsetY: Float) {

    private var tickOffset = 0f
    private val patternOffset = Random.nextInt(300).toFloat()
    private var dots = dots(tickOffset)

    private var noiseOffset = patternOffset
    private fun dots(noiseOffsetY: Float) = Array(segments + 1) {
        val x = gap * it

        // noise lines
        val noiseValue = perlin.noise(noiseOffset, noiseOffsetY)
        val gaussValue = gaussianCurve(it)
        val value = abs(map(noiseValue, 0, 1, -gaussValue, gaussValue))

        noiseOffset += variety
        Point(x.toFloat(), offsetY - value)
    }

    fun draw(canvas: Canvas) {
        tickOffset += 0.005f
        noiseOffset = patternOffset
        dots = dots(tickOffset)

        var left: Float? = null

        val path = PathBuilder()
//            .apply {
//                fillMode = PathFillMode.EVEN_ODD
//            }
            .moveTo(0f, offsetY)

        for (i in 0 until segments) {
            // quad lines
            val xc = (dots[i].x + dots[i + 1].x) / 2
            val yc = (dots[i].y + dots[i + 1].y) / 2

            // detect GAPS
            if (left == null && yc < offsetY - a / 2) {
                left = xc
            }

            val aaaa = 10
            if (left != null && yc > offsetY - a / 2) {
                if (yc < gart.d.cy * 0.8) {
                    canvas.drawLine(left, offsetY, xc, offsetY, strokeOfWhite(a - aaaa).also {
                        it.strokeCap = PaintStrokeCap.BUTT
                    })
                } else {
                    canvas.drawLine(left, offsetY, xc, offsetY, strokeOfBlack(a - aaaa).also {
                        it.strokeCap = PaintStrokeCap.BUTT
                    })
                }
                left = null
            }

            path.quadTo(dots[i], Point(xc, yc))
        }
//        canvas.drawPath(path, strokeOfRed(4f))
    }
}
