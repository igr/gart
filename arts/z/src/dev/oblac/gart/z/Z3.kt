package dev.oblac.gart.z

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.drawImage
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.Complex
import dev.oblac.gart.math.map
import dev.oblac.gart.math.sin
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("z3", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    // window 1 - static image
    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g)
    w.show { c, d, _ ->
        c.drawImage(g.snapshot())
    }.onKey {
        when (it) {
            Key.KEY_W -> ndx++
            Key.KEY_S -> ndx--
            else -> {}
        }
        p = Palettes.colormapPalette(ndx)
        println(ndx)
        draw(c, d)
    }
}

private var ndx = 1

private var p = Palettes.colormap093.expand(128)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(CssColors.black)
//    c.rotate(-90f, d.cx, d.cy)

    for (j in 0 until d.h) {
        val y = map(j, 0, d.h, -4, 2)
        for (i in 0 until d.w) {
            val x = map(i, 0, d.w, -4, 2)

            // formula
            val z1 = sin(Complex(x, y)) / Complex(x, y)

            // draw
            val z3 = z1
//            val r = z3.norm() + (x - 2 * y) + (z3.phase() / 4)
            val r = z3.norm() + (z3.phase() / 4)
            val v = r / 2

            val color = p.safe((v * 255).toInt())

            c.drawPoint(i.toFloat(), j.toFloat(), fillOf(color))
        }
    }
    c.drawBorder(d, 20f, BgColors.elegantDark)
}
