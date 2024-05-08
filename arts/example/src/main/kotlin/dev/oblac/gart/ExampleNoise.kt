package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.drawPoint
import dev.oblac.gart.gfx.random
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.noise.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

val color = strokeOf(Colors.black, 2f)
const val POINTS = 10000

fun main() {
    val gart = Gart.of("ExampleNoise", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val d = gart.d
    val c = g.canvas

    // show image
    gart.window()
        .showImage(g)
        .onKey {
            when (it) {
                Key.KEY_1 -> random(c, d)
                Key.KEY_2 -> halton(c, d)
                Key.KEY_3 -> perlin(c, d)
                Key.KEY_4 -> perlin2(c, d)
                Key.KEY_5 -> poissonDiskSampling(c, d)
                Key.KEY_6 -> poissonDiskSampling2(c, d)
                else -> {}
            }
        }
}

fun random(c: Canvas, d: Dimension) {
    c.clear(Colors.white)
    for (i in 0 until POINTS) {
        val p = Point.random(d)
        c.drawPoint(p, color)
    }
}

fun halton(c: Canvas, d: Dimension) {
    c.clear(Colors.white)
    val halton = HaltonSequenceGenerator(2)
    for (i in 0 until POINTS) {
        halton.get().toList().zipWithNext().forEach { (x, y) ->
            val a = x.toFloat() * d.wf
            val b = y.toFloat() * d.hf
            c.drawPoint(a, b, color)
        }
    }
}

fun perlin(c: Canvas, d: Dimension) {
    c.clear(Colors.white)
    val perlin = PerlinNoise(8)
    for (i in 0 until POINTS) {
        val x = perlin.noise(i.toDouble(), 0.0)
        val y = perlin.noise(0.0, i.toDouble())
        val a = x * d.wf
        val b = y * d.hf
        c.drawPoint(a, b, color)
    }
}

fun perlin2(c: Canvas, d: Dimension) {
    c.clear(Colors.white)
    for (i in 0 until POINTS) {
        val x = Perlin.noise()
        val y = Perlin.noise()
        val a = x * d.wf
        val b = y * d.hf
        c.drawPoint(a.toFloat(), b.toFloat(), color)
    }
}

fun poissonDiskSampling(c: Canvas, d: Dimension) {
    c.clear(Colors.white)
    val noise = PoissonDiskSamplingNoise()
    val samples = noise.generate(0.0, 0.0, d.w.toDouble(), d.h.toDouble(), POINTS)
    for (sample in samples) {
        c.drawPoint(sample.x, sample.y, color)
    }
}

fun poissonDiskSampling2(c: Canvas, d: Dimension) {
    c.clear(Colors.white)
    orxPoissonDiskSamplingNoise(d, 10.0).forEach {
        c.drawPoint(it, color)
    }
}
