package dev.oblac.gart.z

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.Complex
import dev.oblac.gart.math.Complex.Companion.real
import dev.oblac.gart.math.floor
import dev.oblac.gart.math.map
import dev.oblac.gart.math.sqrt
import org.jetbrains.skia.Canvas
import kotlin.math.E

fun main() {
    val gart = Gart.of("z2", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    // window 1 - static image
    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g)
    w.showImage(g)
}

//private val p = Palettes.colormap058.expand(256)
private val p = Palettes.colormap100.expand(256)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(CssColors.black)
    c.rotate(-90f, d.cx, d.cy)

    for (j in 0 until d.h) {
        val y = map(j, 0, d.h, -4, 4)
        for (i in 0 until d.w) {
            val x = map(i, 0, d.w, -4, 4)

            // formula
            val z = Complex(x, y)
            val z2 = sqrt(real(1) + real(E).pow(-z))

            // draw
            val z3 = floor(z2 * 2)
            val r = z3.norm() + (x-2*y) + (z3.phase() / 4)
            val v = r / 18

            val color = p.safe((v * 255).toInt())

            c.drawPoint(i.toFloat(), j.toFloat(), fillOf(color))
        }
    }
    c.drawBorder(d, 20f, BgColors.coconutMilk)
}
