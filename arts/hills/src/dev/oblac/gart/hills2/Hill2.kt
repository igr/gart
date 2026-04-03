package dev.oblac.gart.hills2

import dev.oblac.gart.Dimension
import dev.oblac.gart.hills.perlin
import dev.oblac.gart.hills.segments
import dev.oblac.gart.hills.variety
import dev.oblac.gart.math.GaussianFunction
import dev.oblac.gart.math.map
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.random.Random

class Hill2(dIn: Dimension, private val offsetY: Float, private val gaussDeltaOffset: Int) {
    private val d = Dimension(dIn.w + 40, dIn.h)
    private val gap = d.w / segments
    private var tickOffset = 0f
    private val patternOffset = Random.nextInt(300).toFloat()
    private var dots = dots(tickOffset)

    private var noiseOffset = patternOffset

    private fun dots(noiseOffsetY: Float) = Array(segments + 1) {
        val x = gap * it

        // noise lines
        val noiseValue = perlin.noise(noiseOffset, noiseOffsetY)

        val gauss = GaussianFunction(60, segments / 2 + gaussDeltaOffset, 10)
        val gaussValue = gauss(it)
        val value = abs(map(noiseValue, 0, 1, -gaussValue, gaussValue)) * 6

        noiseOffset += variety
        Point(x.toFloat(), offsetY - value)
    }

    fun path(): Path {
        noiseOffset = patternOffset
        dots = dots(tickOffset)

        val path = PathBuilder()
            .moveTo(0f, offsetY)

        for (i in 0 until segments) {
            // straight lines
            //path.lineTo(dots[i])

            // quad lines
            val xc = (dots[i].x + dots[i + 1].x) / 2
            val yc = (dots[i].y + dots[i + 1].y) / 2
            path.quadTo(dots[i], Point(xc, yc))
        }


        path.lineTo(d.wf, offsetY)
            .lineTo(d.wf, d.hf)
            .lineTo(0f, d.hf)
            .closePath()

        return path.detach().apply {
            fillMode = PathFillMode.EVEN_ODD
        }

    }
}

