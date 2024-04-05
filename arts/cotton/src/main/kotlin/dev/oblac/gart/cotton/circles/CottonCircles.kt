package dev.oblac.gart.cotton.circles

import dev.oblac.gart.Gart
import dev.oblac.gart.gfx.BgColors
import dev.oblac.gart.gfx.Palettes
import dev.oblac.gart.math.rnd
import dev.oblac.gart.noise.NoiseColor
import dev.oblac.gart.shader.toPaint
import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.Path
import dev.oblac.gart.skia.Rect
import dev.oblac.gart.util.loop
import dev.oblac.gart.util.randomExcept

val gart = Gart.of(
    "cotton-circles",
    1000, 1000
)


fun main() {
    println(gart)
    val g = gart.gartvas()

    val noiseColor = NoiseColor(noiseType = NoiseColor.NoiseType.TURBULENCE)
//    val palette = Palettes.cool14
//    val palette = Palettes.cool33
    val palette = Palettes.cool28
    val colors = palette.map {
        noiseColor.composeShader(it, blend = NoiseColor.BlendType.NOISE).toPaint()
            .apply {
                //imageFilter = ImageFilter.makeBlur(1f, 1f, FilterTileMode.CLAMP)
            }
    }

    g.canvas.clear(BgColors.bg01)
    //g.canvas.drawRect(Rect(100f, 100f, 600f, 600f), paint)

    val count = 10
    val delta = gart.d.w / count

    loop(count, count) { i, j ->
        val x = delta * i
        val y = delta * j
        drawRectCircle(g.canvas, x.toFloat(), y.toFloat(), delta, colors)
    }

    gart.showImage(g)
    gart.saveImage(g)
}

fun drawRectCircle(c: Canvas, x: Float, y: Float, delta: Int, colors: List<Paint>) {
    val fill = colors.random()
    val circleFill = colors.randomExcept(fill)

    c.drawRect(Rect(x, y, x + delta, y + delta), fill)

    val d2 = delta / 2
    val rnd = rnd(0, 4)
    val r = when (rnd) {
        0 -> Rect(x, y - d2, x + delta, y + d2)
        1 -> Rect(x + d2, y, x + delta + d2, y + delta)
        2 -> Rect(x, y + d2, x + delta, y + delta + d2)
        3 -> Rect(x - d2, y, x + d2, y + delta)
        else -> throw IllegalStateException()
    }

    c.drawPath(Path().addArc(r, 90f * rnd, 180f).closePath(), circleFill)
}

