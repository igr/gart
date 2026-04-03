package dev.oblac.gart.z

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.Complex.Companion.imag
import dev.oblac.gart.math.Complex.Companion.real
import dev.oblac.gart.math.arcsinh
import dev.oblac.gart.math.map
import org.jetbrains.skia.Canvas
import kotlin.math.floor

fun main() {
    val gart = Gart.of("z1", 1024, 1024)
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

private val p = Palettes.colormap092.expand(256)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(CssColors.black)

    for (j in 0 until d.h) {
        val y = map(j, 0, d.h, -1, 1)
        for (i in 0 until d.w) {
            val x = map(i, 0, d.w, -1, 1)

            // formula
            val z = real(floor(4 * (x + y))) + arcsinh(imag(8.0 * (x - y)))

            // draw this
            val r = z.real

            val normalized = ((r + 10.0) / 14.0).coerceIn(0.0, 1.0)

            val color = p[(normalized * 255).toInt()]

            c.drawPoint(i.toFloat(), j.toFloat(), fillOf(color))
        }
    }
}
