package dev.oblac.gart.repetition

import dev.oblac.gart.Gart
import dev.oblac.gart.Media
import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.PIf
import dev.oblac.gart.skia.BlendMode
import dev.oblac.gart.skia.RRect
import kotlin.math.cos
import kotlin.math.sin

private val gart = Gart.of(
    "Repetition1",
    1280, 1280
)

private const val count = 14
private const val gap = 40f
private const val offset = 80

private const val blur = 8f
private const val speed = 0.05f

private const val size = 24f

private fun drawAll(tick: Long) {
    val offsetsB = Array(count) { Pair(
        sin(tick * speed) * blur,
        cos(tick * speed) * blur,
    )}
    val offsetsR = Array(count) { Pair(
        sin(tick * speed + 2 * PIf / 4) * blur,
        cos(tick * speed - 2 * PIf / 4) * blur,
    )}
    val offsetsG = Array(count) { Pair(
        sin(tick * speed - PIf / 4) * blur,
        cos(tick * speed + PIf / 4) * blur,
    )}

    with(gart) {
        for (i in 0 until count) {
            val offsetsI = Array(count) { Pair(
                sin(tick * i * 0.02f) * blur,
                cos(tick * i * 0.02f) * blur,
            )}

            val x = offset + i * gap + offsetsI[i].first
            val y = offset + i * gap + offsetsI[i].second
            val w = d.wf - 2 * x
            val h = d.hf - 2 * y

            offsetsB[i].let { (dx, dy) ->
                RRect.makeXYWH(x + dx, y + dy, w, h, 40f).let {
                    g.canvas.drawRRect(it, strokeOf(Colors.blue, size).apply { blendMode = BlendMode.SCREEN })
                }
            }
            offsetsG[i].let { (dx, dy) ->
                RRect.makeXYWH(x + dx, y + dy, w, h, 40f).let {
                    g.canvas.drawRRect(it, strokeOf(Colors.lime, size).apply { blendMode = BlendMode.SCREEN })
                }
            }
            offsetsR[i].let { (dx, dy) ->
                RRect.makeXYWH(x + dx, y + dy, w, h, 40f).let {
                    g.canvas.drawRRect(it, strokeOf(Colors.red, size).apply { blendMode = BlendMode.SCREEN })
                }
            }
        }
    }
}

fun main() {
    with(gart) {
        println(name)
        val marker = f.marker().atFrame(600)
        w.show()
        a.record()
        a.draw {
            g.fill(Colors.black)
            drawAll(f.count.value)

            if (f after marker) {
                a.stop()
                return@draw
            }
        }
        Media.saveImage(this)
        Media.saveVideo(this)
    }
}
