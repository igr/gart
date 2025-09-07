package dev.oblac.gart.spiral2

import dev.oblac.gart.*
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.drawCircleArc
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.strokeOfWhite
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of(
        "spiral2",
        1024, 1024
    )

    println(gart)

    val w = gart.window()
    w.show { c, d, f ->
        draw(c, d)
        f.onFrame(1L) {
            gart.saveImage(c)
        }
    }
}

private val pal = Palettes.cool73

private fun draw(c: Canvas, d: Dimension) {

    c.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), fillOf(0xFF121212))

    val right = makeCircles(d, 0, 20f).cropRect(d.w3, 0f, d.w3 * 2, d.h.toFloat() * 2)
    val left = makeCircles(d, 1, -10f).cropRect(0f, 0f, d.w3 * 2, d.h.toFloat() * 2)

    c.save()
    c.rotate(30f, d.cx, d.cy)
    c.drawSprite(left){it.down(100f).at(d.cx - left.d.cx, d.cy * 1.5f)}
    c.drawSprite(right){it.at(d.cx + right.d.cx, d.cy * 1.5f )}
    c.restore()

    c.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), strokeOfWhite(40f))
}

private fun makeCircles(d: Dimension, colorOffset: Int, rotate: Float): Sprite {
    val width = 40f

    val g = Gartvas(d *2)
    val c = g.canvas
    c.save()
    c.rotate(rotate, d.cx, d.cy)

    c.clear(pal.safe(0 + colorOffset - 1))

    for (i in 0..20) {
        c.drawCircleArc(d.cx, d.cy, (i + 1) * width, strokeOf(pal.safe(i + colorOffset), width))
    }
    for (i in 0..20) {
        c.drawCircleArc(d.cx, d.cy, (i + 1) * width, strokeOf(pal.safe(i + 2 + colorOffset), width), Degrees.D180)
    }

    c.restore()
    return g.sprite()
}
