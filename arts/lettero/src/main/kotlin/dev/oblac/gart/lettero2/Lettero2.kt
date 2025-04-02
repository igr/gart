package dev.oblac.gart.lettero2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.text.drawTextOnPath
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path

fun main() {
    val gart = Gart.of(
        "lettero2",
        1024, 1024 * GOLDEN_RATIO, 1
    )
    val g = gart.gartvas()
    val c = g.canvas
    val w = gart.window()

    println(gart)

    draw(c, gart.d)

    gart.saveImage(g)
    w.showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(NipponColors.col248_SUMI)

    val gap = 80
    val font = font(FontFamily.NotoSans, 52f)
    val pal = Palettes.cool37.expand(44)
    var i = 0

    for (y in 0 until d.h + gap step gap) {
        val path = Path()
        path.moveTo(0f, y.toFloat())

        val gapx = 20
        val border = d.hf / 3
        val off = 10 + rndi(0, 40)

        for (x in -off until (d.w + off) step gapx) {
            if (y < border) {
                path.lineTo(x.toFloat(), y.toFloat())
            } else {
                val r = border - y
                path.lineTo(x.toFloat(), y.toFloat() + rndf(r / 20f))
            }
        }

        drawTextOnPath(c, path, random01(30), font, fillOf(pal[i]))
        i++
    }
    c.drawBorder(d, strokeOfBlack(40f))
}

private fun random01(len: Int): String = buildString(len) {
    repeat(len) { append(if (rndb()) '0' else '1') }
}
