package dev.oblac.gart.legoo.v1

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.RectIsometricLeft
import dev.oblac.gart.gfx.RectIsometricRight
import dev.oblac.gart.gfx.RectIsometricTop
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

const val a = 10f
val angle = Degrees(30f)
const val sc = a * 0.86f
const val aa = a * 2 + a / 2

fun main() {
    val gart = Gart.of("Legoo12", 1024, 1024)
    println(gart)

    // main canvas
    val g = gart.gartvas()

    // offset for
    var leftOff = 0
    var rightOff = 0

    // draw on canvas
    g.draw { c, d ->
        c.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(BgColors.elegantDark))
        c.drawCircle(d.cx, d.cy, 260f, fillOf(Colors.crimson))
        c.drawCircle(d.cx + 100f, d.cy - 40f, 200f, fillOf(Colors.crimson))

        // 1
        val x = d.cx - sc // top-center
        val y = 0

        for (i in 1..42) {
            for (j in 1..30) {
                val xleft = x - sc * j * 2
                val xright = x + sc * j * 2
                kocka(xleft, i * aa + 30f + offf() * 30f, c)
                kocka(xright, y - 120f + i * aa + offf() * 30f, c)
            }
        }
    }

    gart.saveImage(g)

    // show image
    gart.window().showImage(g)
}

val cdark = fillOf(BgColors.elegantDark)
val c01 = fillOf(BgColors.bg01).also { it.alpha = 31 }
val c02 = fillOf(BgColors.coconutMilk).also { it.alpha = 31 }

private fun kocka(x: Float, y: Float, c: Canvas) {
    RectIsometricTop(x, y, a, a, angle).path().let { c.drawPath(it, c02) }
    RectIsometricLeft(x, y, a, a, angle).path().let { c.drawPath(it, c01) }
    RectIsometricRight(x + sc, y + a / 2, a, a, angle).path().let { c.drawPath(it, cdark) }
}

private fun offf(): Int {
    val f = rndf(-1f, 1f) * 10
    return when {
        f < -7 -> -1
        f > 7 -> 1
        else -> 0
    }
}
