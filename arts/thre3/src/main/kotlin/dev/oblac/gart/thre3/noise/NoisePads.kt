package dev.oblac.gart.thre3.noise

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Colors
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.WaveFlow
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.poissonDiskSamplingNoise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

val gart = Gart.of(
    "noisepads",
    1000, 1000
)
val d = gart.d

fun main() {
    val g = gart.gartvas()
    val c = g.canvas

    c.clear(BgColors.cloudDancer)

    val height = 200f
    val count = 3

    val offset = (d.hf - (height * count)) / (count + 1)

    for (i in 0 until count) {
        val y = offset + (height + offset) * i
        val r = Rect(100f, y, 900f, y + height)
        box(c, r, 20 - i * 5.0)
    }

    c.drawBorder(d, 20f, Colors.white);

    c.rotate(-20f)
    c.translate(-80f, 0f)

    drawFlow(c, strokeOf(0x22FF4433, 4f))

    gart.window().showImage(g)

    gart.saveImage(g)
}

fun box(c: Canvas, r: Rect, noise: Double) {
    c.drawHumanRect(r, strokeOf(0xcc333333, 4f))

    val noiseRect = r.shrink(5f)
    val d = noiseRect.dimension()
    poissonDiskSamplingNoise(d, noise).forEach {
        c.drawPoint(it.offset(noiseRect.topLeftPoint()), strokeOfBlack(2f))
    }
}

fun drawFlow(c: Canvas, stroke: Paint) {
    val waveFlow = WaveFlow(0.015f, 0.015f)
    val ff = ForceField.of(gart.d) { x, y -> waveFlow(x, y) }

    val n = 4
    var points = Array(n) {
        Point(200f + rndf(-10f, 10f), d.hf - 1)
    }.toList() + Array(n) {
        Point(400f + rndf(-20f, 20f), d.hf - 1)
    }.toList()

    repeat(1200) { count ->
        points = ff.apply(points) { _, p ->
            //c.drawPoint(p, stroke.apply { alpha = (count / 40f).toInt() })
            c.drawCircle(p, count / 120f, stroke.apply { alpha = (count / 40f).toInt() })
        }
    }
}
