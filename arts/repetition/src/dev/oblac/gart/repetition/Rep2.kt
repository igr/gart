package dev.oblac.gart.repetition

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.PIf
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import kotlin.math.cos
import kotlin.math.sin


private val gart = Gart.of(
    "Repetition2",
    1280, 1280,
    30
)

private const val count = 14
private const val gap = 40f
private const val offset = 80

private const val blur = 8f
private const val speed = 0.01f

private const val size = 32f

private fun drawAll(canvas: Canvas, tick: Long) {
    val offsetsB = Array(count) {
        Pair(
            sin(tick * speed) * blur,
            cos(tick * speed) * blur,
        )
    }
    val offsetsR = Array(count) {
        Pair(
            sin(tick * speed + 2 * PIf / 4) * blur,
            cos(tick * speed - 2 * PIf / 4) * blur,
        )
    }
    val offsetsG = Array(count) {
        Pair(
            sin(tick * speed - PIf / 4) * blur,
            cos(tick * speed + PIf / 4) * blur,
        )
    }

    with(gart) {
        for (i in 0 until count) {
            val offsetsI = Array(count) {
                Pair(
                    sin(tick * i * 0.02f) * blur,
                    cos(tick * i * 0.02f) * blur,
                )
            }

            val x = offset + i * gap + offsetsI[i].first + 40
            val y = offset + i * gap + offsetsI[i].second + 40
            val w = d.wf - 2 * x
            val h = d.hf - 2 * y

            val ddelta = 80
            offsetsB[i].let { (dx, dy) ->
                canvas.drawCircle(x + dx + ddelta, y + dy + ddelta, w, strokeOf(CssColors.blue, size).apply { blendMode = BlendMode.SCREEN })
            }
            offsetsG[i].let { (dx, dy) ->
                canvas.drawCircle(x + dx + ddelta, y + dy + ddelta, w, strokeOf(CssColors.lime, size).apply { blendMode = BlendMode.SCREEN })
            }
            offsetsR[i].let { (dx, dy) ->
                canvas.drawCircle(x + dx + ddelta, y + dy + ddelta, w, strokeOf(CssColors.red, size).apply { blendMode = BlendMode.SCREEN })
            }
        }
    }
}

fun main() {
    println(gart)
    val g = gart.gartvas()
    val w = gart.window()
    val c = g.canvas
    c.clear(BgColors.warmBlack1)
    drawAll(c, 175)

    gart.saveImage(g)
    w.showImage(g)

//    w.show { c, _, f ->
//        c.clear(Colors.black)
//        println(f.frame)
//
//    }

}
