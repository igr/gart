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
import dev.oblac.gart.math.*
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("z10", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g)
    w.show { c, _, _ ->
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

// f(z) applied iteratively
private fun f(z: Complex): Complex {
    return sin(z to 2) + Complex(0.355, 0.355)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(CssColors.black)

    for (j in 0 until d.h) {
        val y = map(j, 0, d.h, -2.0, 2.0)
        for (i in 0 until d.w) {
            val x = map(i, 0, d.w, -2.0, 2.0)

            val (z, iter, convergence, maxIter) = zfunc(
                x, y, maxIterations = 100, escapeRadius = 4f, f = ::f)

            // color by iteration count with smooth shading
            val color = if (convergence == Convergence.CONVERGED) {
                // converged: color by final value's phase
                val v = (z.phase() / Math.PI + 1.0) / 2.0
                p.safe((v * 255).toInt())
            } else {
                // diverged: color by iteration count
                val v = iter.toDouble() / maxIter
                p.safe((v * 255).toInt())
            }

            c.drawPoint(i.toFloat(), j.toFloat(), fillOf(color))
        }
    }
    c.drawBorder(d, 20f, BgColors.clottedCream)
}
