package dev.oblac.gart.z

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.Complex.Companion.imag
import dev.oblac.gart.math.Complex.Companion.real
import dev.oblac.gart.math.ln
import dev.oblac.gart.math.map
import org.jetbrains.skia.Canvas
import kotlin.math.floor

fun main() {
    val gart = Gart.of("z5", 1024, 1024)
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

private var p = Palettes.colormap029
    .expandReversed()
    .expand(256)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(CssColors.black)
    c.save()
    c.rotate(0f, d.cx, d.cy)

    for (j in 0 until d.h step 20) {
        val y = map(j, 0, d.h, -0.8, 0.2)
        for (i in 0 until d.w step 20) {
            val x = map(i, 0, d.w, -0.8, 0.2)

            // formula
            val z = real(floor(6 * y + x * 4)) + ln(imag(4 * x * y))
            // draw
            val r = z.real
            val v = r + z.phase()

            val color = p.safe(v * 255)
//            c.drawCircle(i.toFloat(), j.toFloat(), 20f, fillOf(color))

            val p4 = Poly4.squareAroundPoint(
                Point(i, j), 60f * (x + y), Radians.of(z.phase() * 80)
            )
            c.drawPoly4(p4, fillOf(color))
        }
    }
    c.restore()

    c.drawBorder(d, 20f, BgColors.clottedCream)
}
