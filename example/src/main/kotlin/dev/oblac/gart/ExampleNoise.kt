package dev.oblac.gart

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.drawImage
import dev.oblac.gart.gfx.drawPoint
import dev.oblac.gart.gfx.random
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.noise.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

private val color = strokeOf(CssColors.black, 2f)
const val POINTS = 10000

fun main() {
    val gart = Gart.of("ExampleNoise", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val d = gart.d
    val c = g.canvas

    // show image
    gart.window()
        .show { cc, _, _ ->
            cc.drawImage(g.snapshot())
        }
        .onKey(KeyHandlers.showKey)
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

private fun random(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    repeat(POINTS) {
        val p = Point.random(d)
        c.drawPoint(p, color)
    }
}

private fun halton(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    val halton = HaltonSequenceGenerator(2)
    repeat(POINTS) {
        halton.get().toList().zipWithNext().forEach { (x, y) ->
            val a = x.toFloat() * d.wf
            val b = y.toFloat() * d.hf
            c.drawPoint(a, b, color)
        }
    }
}

private fun perlin(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    val perlin = PerlinNoise(8)
    repeat(POINTS) { i ->
        val x = perlin.noise(i.toDouble(), 0.0)
        val y = perlin.noise(0.0, i.toDouble())
        val a = x * d.wf
        val b = y * d.hf
        c.drawPoint(a, b, color)
    }
}

private fun perlin2(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    repeat(POINTS) {
        val x = Perlin.noise()
        val y = Perlin.noise()
        val a = x * d.wf
        val b = y * d.hf
        c.drawPoint(a.toFloat(), b.toFloat(), color)
    }
}

private fun poissonDiskSampling(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    val noise = PoissonDiskSamplingNoise()
    val samples = noise.generate(0.0, 0.0, d.w.toDouble(), d.h.toDouble(), POINTS)
    for (sample in samples) {
        c.drawPoint(sample.x, sample.y, color)
    }
}

private fun poissonDiskSampling2(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    poissonDiskSamplingNoise(d, 10.0).forEach {
        c.drawPoint(it, color)
    }
}
