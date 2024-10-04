package dev.oblac.gart.igor

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.isInside
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.util.loop
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of("igor", 1024, 1024)

    val g = gart.gartvas()
    val c = g.canvas
    val d = g.d

    val a = d.w / 4 // circle size
    val b = a / 6   // bar height
    val b2 = 40f    // how much to move inside

    val backgroundColor = fillOf(0xffdbd1bf.toInt())
    c.drawRect(d.rect, backgroundColor)
    poissonDiskSampling(c, d)

    loop(4, 4) { x, y ->
        val xx = (a * x).toFloat()
        val yy = (a * y).toFloat()

        c.drawCircle(xx + a / 2f, yy + a / 2f, a / 2f - 4f, fillOf(BgColors.dark02))
        val xxc = xx + a / 2f
        val yyc = yy + a / 2f

        val letter = when (x to y) {
            1 to 0 -> "I"
            3 to 1 -> "G"
            2 to 2 -> "O"
            0 to 3 -> "R"
            else -> ""
        }

        if (letter.isNotEmpty()) {
            c.rotate(6f, xxc, yyc)
            c.drawString(letter, xx + a * 0.25f, yyc - 10, font(FontFamily.OdibeeSans, 80f), backgroundColor)
        }

        val rect =  Rect(xx, yyc - b / 2f, xxc + b2, yyc + b / 2f)
        c.drawRect(rect, backgroundColor)
        poissonDiskSamplingInside(c, d, rect)

        if (letter.isNotEmpty()) {
            c.rotate(-6f, xxc, yyc)
        }
    }

    // final

    val s = g.snapshot()
    val gart2 = Gart.of("igor", 980, 1064)
    val g2 = gart2.gartvas()
    g2.canvas.drawRect(g2.d.rect, backgroundColor)
    poissonDiskSampling(g2.canvas, g2.d)
    g2.canvas.drawImage(s, 20f, 20f)

    gart2.saveImage(g2)
    gart2.window().showImage(g2)
}

private fun poissonDiskSampling(c: Canvas, d: Dimension) {
    val noise = PoissonDiskSamplingNoise()
    val samples = noise.generate(0.0, 0.0, d.w.toDouble(), d.h.toDouble(), 60_000)
    for (sample in samples) {
        c.drawPoint(sample.x, sample.y, fillOf(BgColors.dark02).also { it.alpha = 160 })
    }
}

private fun poissonDiskSamplingInside(c: Canvas, d: Dimension, rect: Rect) {
    val noise = PoissonDiskSamplingNoise()
    val samples = noise.generate(0.0, 0.0, d.w.toDouble(), d.h.toDouble(), 60_000)
    for (sample in samples) {
        val p = Point(sample.x, sample.y)
        if (p.isInside(rect)) {
            c.drawPoint(sample.x, sample.y, fillOf(BgColors.dark02).also { it.alpha = 160 })
        }
    }
}
