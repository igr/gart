package dev.oblac.gart.legoo.v2

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Key
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.RectIsometricLeft
import dev.oblac.gart.gfx.RectIsometricRight
import dev.oblac.gart.gfx.RectIsometricTop
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect

const val a = 100f
val angle = Degrees(30f)
const val sc = 86f
const val aa = a * 2 + a / 2

fun main() {
    val gart = Gart.of("Legoo2", 1024, 1024)
    println(gart)

    // main canvas
    val g = gart.gartvas()

    draw(g)

    // show image
    gart.window().showImage(g).onKey {
        when (it) {
            Key.KEY_UP -> {
                pindex++
                p = Palettes.coolPalette(pindex)
                draw(g)
            }

            Key.KEY_DOWN -> {
                pindex--
                p = Palettes.coolPalette(pindex)
                draw(g)
            }

            Key.KEY_D -> {
                draw(g)
            }

            Key.KEY_S -> {
                gart.saveImage(g)
                println("Image saved.")
            }

            else -> {}
        }
        println("Palette: $pindex")


    }
}

fun draw(g: Gartvas) {
    g.draw { c, d ->
        c.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(BgColors.elegantDark))

        val randomRow = rndi(2, 6)
        val randomCol = rndi(2, 6)

        for (row in 0..10) {
            val x = d.cx - sc // top-center
            val y = 0f + row * 100f
            for (k in -8..8) {
                kocka(x + offf() + k * sc, y + offf(), c)
                if (row == randomRow && randomCol == k) {
                    c.drawCircle(x + k * sc + sc / 2, y + sc / 2, 120f, fillOf(0xFFc53a32))
                }
            }
        }
    }
}


var pindex = 1
var p = Palettes.coolPalette(pindex)

private fun kocka(x: Float, y: Float, c: Canvas) {
    val alpha = rndi(120, 250)
    val fill = fillOf(p.random()).also {
        it.alpha = alpha
        it.mode = PaintMode.STROKE_AND_FILL
        it.strokeWidth = 1f
    }
    RectIsometricTop(x, y, a, a, angle).path().let { c.drawPath(it, fill) }
    RectIsometricLeft(x, y, a, a, angle).path().let { c.drawPath(it, fill) }
    RectIsometricRight(x + sc, y + a / 2, a, a, angle).path().let { c.drawPath(it, fill) }
}

private fun offf(): Int {
    val f = rndf(-1f, 1f) * 10
    return when {
        f < -8 -> -1
        f > 8 -> 1
        else -> 0
    } * 30
}
