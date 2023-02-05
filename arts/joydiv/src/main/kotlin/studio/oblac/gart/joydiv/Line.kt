package studio.oblac.gart.joydiv

import studio.oblac.gart.Gartvas
import studio.oblac.gart.gfx.fillOfBlack
import studio.oblac.gart.gfx.strokeOfWhite
import studio.oblac.gart.math.GaussianFunction
import studio.oblac.gart.math.PerlinNoise
import studio.oblac.gart.math.map
import studio.oblac.gart.skia.Path
import studio.oblac.gart.skia.PathFillMode
import studio.oblac.gart.skia.Point
import kotlin.math.abs
import kotlin.random.Random

const val segments = 50
const val gap = w / segments
val gauss = GaussianFunction(100, segments/2, 8)
fun gaussianCurve(x: Number) = gauss(x)
const val variety = 0.04f
val perlin = PerlinNoise()

class Line(private val g: Gartvas, private val offsetY: Float) {

	private var tickOffset = 0f
	private val patternOffset = Random.nextInt(300).toFloat()
	private var dots = dots(tickOffset)

	private var noiseOffset = patternOffset
	private fun dots(noiseOffsetY: Float) = Array(segments + 1) {
		val x = gap * it
		// simple
//		val distanceFromCenter = abs(g.w_2 - x)
//		val variance = max(g.w_2 - 50 - distanceFromCenter, 0)
//		val random = Random.nextFloat() * variance / 2 * -1
//		val y = offsetY + random
//		Point(x, y)

		// noise lines
		val noiseValue = perlin.noise(noiseOffset, noiseOffsetY)
		val gaussValue = gaussianCurve(it)
		val value = abs(map(noiseValue, 0, 1, -gaussValue, gaussValue))

		noiseOffset += variety
		Point(x.toFloat(), offsetY - value)
	}

	fun draw() {
		tickOffset += 0.005f
		noiseOffset = patternOffset
		dots = dots(tickOffset)

		val path = Path()
            .apply {
                fillMode = PathFillMode.EVEN_ODD
            }
			.moveTo(0f, offsetY)

		for (i in 0 until segments) {
			// straight lines
			//path.lineTo(dots[i])

			// quad lines
			val xc = (dots[i].x + dots[i + 1].x) / 2
			val yc = (dots[i].y + dots[i + 1].y) / 2
			path.quadTo(dots[i], Point(xc, yc))
		}

		path.lineTo(wf, offsetY)
			.lineTo(wf, hf)
			.lineTo(0f, hf)
			.closePath()

		g.canvas.drawPath(path, fillOfBlack())
		g.canvas.drawPath(path, strokeOfWhite(2f))
	}
}
