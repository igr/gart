package dev.oblac.gart.legoo.v2

import dev.oblac.gart.Gart
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.RectIsometricLeft
import dev.oblac.gart.gfx.RectIsometricRight
import dev.oblac.gart.gfx.RectIsometricTop
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
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

    // offset for
    var leftOff = 0
    var rightOff = 0

    // draw on canvas
    g.draw { c, d ->
        c.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(BgColors.elegantDark))

        // 1
        val x = d.cx - sc // top-center
        val y = 100f
        kocka(x, y, c)

        // 2
        var xl = x
        var xr = x
        var yl = y
        var yr = y

        for (i in 1..5) {
            xl -= sc
            xr += sc
            yl += 50f
            yr += 50f
            kocka(xl + offf(), yl + offf(), c)
            if (i < 2) {
                kocka(xr + offf(), yr + offf(), c)
            }
        }
        for (i in 5 downTo -5 step 1) {
            xl += sc
            xr -= sc
            yl += 50f
            yr += 50f
            kocka(xl + offf(), yl + offf(), c)
            kocka(xr + offf(), yr + offf(), c)
        }

    }

    gart.saveImage(g)

    // show image
    gart.window().showImage(g)
}


private fun kocka(x: Float, y: Float, c: Canvas) {
    RectIsometricTop(x, y, a, a, angle).path().let { c.drawPath(it, fillOf(Palettes.cool10.random()).also { it.alpha = 210 }) }
    RectIsometricLeft(x, y, a, a, angle).path().let { c.drawPath(it, fillOf(Palettes.cool10.random()).also { it.alpha = 210 }) }
    RectIsometricRight(x + sc, y + a / 2, a, a, angle).path().let { c.drawPath(it, fillOf(Palettes.cool10.random()).also { it.alpha = 210 }) }
}

private fun offf(): Int {
    val f = rndf(-1f, 1f) * 10
    return when {
        f < -8 -> -1
        f > 8 -> 1
        else -> 0
    } * 30
}
