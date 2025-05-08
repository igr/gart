package dev.oblac.gart.z

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.drawImage
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.Complex.Companion.imag
import dev.oblac.gart.math.Complex.Companion.real
import dev.oblac.gart.math.ln
import dev.oblac.gart.math.map
import org.jetbrains.skia.Canvas
import kotlin.math.floor

fun main() {
    val gart = Gart.of("z4", 1024, 1024)
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
        p = Palettes.colormapPalette(ndx).expand(256)
        println(ndx)
        draw(c, d)
    }
}

private var ndx = 4

private var p = Palettes.colormap060.expand(256)
private var p2 = Palettes.colormap038.expand(256)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.black)
    //c.rotate(-180f, d.cx, d.cy)

    for (j in 0 until d.h) {
        val y = map(j, 0, d.h, -1, 1)
        for (i in 0 until d.w) {
            val x = map(i, 0, d.w, -2, 0)

            // formula
            val z = real(floor(6 * y + x*2)) + ln(imag(4 * x * x))
            // draw
            val r = z.real
            val v = r / 1.6 + z.phase()

            val color =
                if (z.phase() < 0.55) {
                    p2.safe((v * 255).toInt())
                } else {
                    p.safe((v * 255).toInt())
                }

//            c.drawPoint(i.toFloat(), j.toFloat(), fillOf(color))
            c.drawCircle(i.toFloat(), j.toFloat(), 2f, fillOf(color))
        }
    }
    c.drawBorder(d, 20f, BgColors.clottedCream)
}
