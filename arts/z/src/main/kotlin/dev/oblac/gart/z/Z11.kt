package dev.oblac.gart.z

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.*
import dev.oblac.gart.noise.poissonDiskSampling
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("z11", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d, 2f)

    gart.saveImage(g)
    w.show { c, _, _ ->
        c.drawImage(g.snapshot())
    }
}

private var p = Palettes.colormap030
    .expandReversed()
    .expand(256)

// f(z) applied iteratively
private fun f(z: Complex): Complex {
    return (z / (exp(z) + Complex(0.355, 0.355))) * 0.01
}

private fun draw(c: Canvas, d: Dimension, koef: Float) {
    c.clear(CssColors.black)

    for (j in 0 until d.h) {
        val y = map(j, 0, d.h, -2.0, 2.0)
        for (i in 0 until d.w) {
            val x = map(i, 0, d.w, -2.0, 2.0)

            val (z, iter, convergence, maxIter) = zfunc(
                x, y, maxIterations = 100, escapeRadius = 5f, f = ::f)

            // color by iteration count with smooth shading
            val color = if (convergence == Convergence.CONVERGED) {
                val v = (z.phase() / Math.PI + 1.0) / koef
                p.safe((v * 255).toInt())
            } else {
                val v = iter.toDouble() / maxIter
                p.safe((v * 255).toInt())
            }

            c.drawCircle(i.toFloat(), j.toFloat(), 0.7f, fillOf(color))
        }
    }

    poissonDiskSampling(d.rect, 20f).forEach { (x, y) ->
        c.drawCircle(x, y, 1.5f, fillOf(CssColors.white).alpha(170))
    }

    c.drawBorder(d, 20f, BgColors.clottedCream)
}
