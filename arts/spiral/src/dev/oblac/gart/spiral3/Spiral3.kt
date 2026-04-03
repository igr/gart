package dev.oblac.gart.spiral3

import dev.oblac.gart.*
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.gfx.strokeOfWhite
import dev.oblac.gart.math.Lissajous
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of(
        "spiral3",
        1024, 1024, 30
    )
    val d = gart.d
    val d2 = d * 2

    val ls = listOf(
        // 0
        Lissajous(
            a = 0.5f, b = 0.5f,
            A = 20f, B = 10f,
            dx = 0.5f, dy = 0.5f,
            center = d2.center
        ),
        Lissajous(
            a = 0.4f, b = 0.5f,
            A = 10f, B = 25f,
            dx = 0.5f, dy = 0.5f,
            center = d2.center
        ),
        Lissajous(
            a = 0.6f, b = 0.4f,
            A = 16f, B = 18f,
            dx = 0.5f, dy = 0.5f,
            center = d2.center
        )
    )

    println(gart)

    val w = gart.window()
    val m = gart.movieGif()

//    w.show { c, d, f ->
    m.record(w).show() { c, d, f ->
        draw(c, d, d2, ls)
        f.onFrame(1L) {
            gart.saveImage(c)
        }
        f.onFrame(400L) {
            m.stopRecording()
        }
    }
}

private fun draw(c: Canvas, d: Dimension, d2: Dimension, ls: List<Lissajous>) {

    c.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), fillOf(0xFF121212))

    val circles = makeCircles(d2, -10f, ls)

    c.save()

    c.rotate(30f, d.cx, d.cy)
    c.drawSprite(circles) { it.at(d.cx, d.cy) }
    c.restore()

    c.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), strokeOfWhite(40f))
}

private val ws = arrayOf(80f, 140f, 80f, 100f, 20f, 10f, 5f, 2f)

private fun makeCircles(d: Dimension, rotate: Float, ls: List<Lissajous>): Sprite {
    val width = 80f

    val g = Gartvas(d)
    val c = g.canvas
    c.save()
    c.rotate(rotate, d.cx, d.cy)

    //
    val l0 = ls[0]
    l0.step(0.1f)
    val p0 = l0.position()

    val l1 = ls[1]
    l1.step(0.1f)
    val p1 = l1.position()

    val l2 = ls[2]
    l2.step(0.1f)
    val p2 = l2.position()

    c.clear(CssColors.black)
    for (i in 0..8 step 2) {

        val pathEffect = when (i) {
            2 -> PathEffect.makeDash(floatArrayOf(40f, 40f), 0f)
            4 -> PathEffect.makeDash(floatArrayOf(10f, 20f), 10f)
            else -> {
                null
            }
        }

        c.drawCircle(
            p0.x, p0.y, (i + 1) * width,
            strokeOf(CssColors.red, ws[i / 2]).apply {
                blendMode = BlendMode.SCREEN
                this.pathEffect = pathEffect
            })
        c.drawCircle(
            p1.x, p1.y, (i + 1) * width,
            strokeOf(CssColors.blue, ws[i / 2]).apply {
                blendMode = BlendMode.SCREEN
                this.pathEffect = pathEffect
            })
        c.drawCircle(
            p2.x, p2.y, (i + 1) * width,
            strokeOf(CssColors.lime, ws[i / 2]).apply {
                blendMode = BlendMode.SCREEN
                this.pathEffect = pathEffect
            })

        if (i == 6) {
            c.drawCircle(
                d.cx, d.cy,
                (i + 2) * width,
                strokeOfBlack(150).apply {
                    this.pathEffect = PathEffect.makeDash(floatArrayOf(40f, 40f), 0f)
                }
            )
        }
    }

    c.restore()
    return g.sprite()
}
