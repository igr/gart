package dev.oblac.gart

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.drawImage
import dev.oblac.gart.gfx.drawPoint
import dev.oblac.gart.gfx.random
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.f
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
                Key.KEY_7 -> simplex(c, d)
                Key.KEY_8 -> openSimplex(c, d)
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

fun simplex(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    val simplex = SimplexNoise
    repeat(POINTS) { i ->

        // scale is important for visualization.
        // If it's too small, you'll get a very noisy, almost random pattern.
        // If it's too large, you'll get a very smooth, almost uniform pattern.
        // You want to find a balance that shows the characteristic "cloud-like"
        // structure of simplex noise.
        val scale = 0.01

        val x = simplex.noise(i * scale, 0.0)
        val y = simplex.noise(i * scale, 1000.0)

        val a = ((x + 1) / 2) * d.wf
        val b = ((y + 1) / 2) * d.hf

        c.drawPoint(a.f(), b.f(), color)
    }
}

fun openSimplex(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    val openSimplex = OpenSimplexNoise()
    repeat(POINTS) { i ->
        // This way x and y sample from different regions of the noise field and won't be correlated.
        // Good for visualization, but not so good if you want to use the noise for something like terrain generation where you want x and y to be correlated.
        val x = openSimplex.random2D(i.toDouble(), 0.0)
        val y = openSimplex.random2D(i.toDouble(), 1000.0)

        // since result is in range -1..1, we need to map it to 0..1 before multiplying by width and height
        val a = ((x + 1) / 2) * d.wf
        val b = ((y + 1) / 2) * d.hf

        c.drawPoint(a.f(), b.f(), color)
    }
}
