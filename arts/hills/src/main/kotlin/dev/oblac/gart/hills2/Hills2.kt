package dev.oblac.gart.hills2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.poissonDiskSamplingNoise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder

val back = NipponColors.col235_SHIRONEZUMI
val border = NipponColors.col233_SHIRONERI
//val p1 = Palettes.cool2.expand(9)
//val p = p1 + p1.reversed()

//val p1 = Palettes.cool28
//val p = p1 + p1.reversed()

val p1 = Palettes.cool37
val p = p1 + p1.reversed()

fun main() {
    val gart = Gart.of("february", 1024, 1024 * GOLDEN_RATIO)
    println(gart)

    val g = gart.gartvas()
    val d = gart.d
    val w = gart.window()

    val c = g.canvas

    // DRAW
    c.clear(back)

    drawSun(c, d)
    for (i in 0 until 16) {
        val hill = Hill2(d, 600f + i * 80f, if (i % 2 == 0) 10 else -10).path()
        drawHill(c, d, hill, p.safe(i), p.safe(i + 1))
    }

    c.drawRoundBorder(d, 90f, 30f, border)

    gart.saveImage(g)

    w.showImage(g)
}

private fun drawHill(c: Canvas, d: Dimension, p: Path, color1: Int, color2: Int) {
    // the generation of the hill is shifted to the right by 40 pixels
    val p2 = PathBuilder(p).offset(-20f, 0f).detach()
    //p.offset(-20f, 0f)
    c.drawPath(p2, fillOf(color1))
    //c.drawPath(p, strokeOf(hill, 4f))

    val region = p.toRegion()

    poissonDiskSamplingNoise(d, 20.0)
        .filter { region.contains(it.x.toInt(), it.y.toInt()) }
        .forEach {
            c.drawCircle(it, 8f + rndf(0, 2f), fillOf(color2))
        }

}


fun drawSun(c: Canvas, d: Dimension) {
    val circle = Circle(d.w3x2, 400f, 200f)
//    val circleForContains = Circle(d.w3x2, 680f, 250f)
    val circleForContains = Circle(d.w3x2, 400f, 220f)
    c.drawCircle(circle, fillOf(NipponColors.col109_TOHOH))
    poissonDiskSamplingNoise(d, 20.0)
        .filter { it.isInside(circleForContains) }
        .forEach {
            c.drawCircle(it, 8f + rndf(2f), fillOf(NipponColors.col110_UKON))
        }
}
