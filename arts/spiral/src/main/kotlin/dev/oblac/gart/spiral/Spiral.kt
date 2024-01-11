package dev.oblac.gart.spiral

import dev.oblac.gart.Gart
import dev.oblac.gart.Media
import dev.oblac.gart.gfx.Palette
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.gfx.strokeOfWhite
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

val gart = Gart.of(
    "spiral",
    800, 800,
    30
)

fun main() {
    with(gart) {
        println(name)

        w.show()
        val endMarker = f.marker().atTime(12.seconds)

        a.record()
        a.draw {
            draw()
            if (endMarker.after()) {
                a.stop()
            }
        }

        Media.saveImage(this)
        Media.saveVideo(this)
    }
}

fun draw() {
    val g = gart.g
    val d = gart.d

    g.canvas.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), fillOf(0xFF121212))

    // draw every tick
    drawSpiral(g.canvas, d.cx - 100, d.cy - 450, 1f, 1f, true)
    drawSpiral(g.canvas, d.cx + 100, d.cy + 100, 11f, -1f, true)
    drawSpiral(g.canvas, d.cx - 100, d.cy - 150, 1f, 1f, false)
    drawSpiral(g.canvas, d.cx + 100, d.cy + 100, 11f, -1f, false)

    g.canvas.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), strokeOfWhite(40f))
}

var a = 12f
var step = 0.35f
fun drawSpiral(canvas: Canvas, cx: Float, cy: Float, from: Float = 0.01f, direction: Float = 1f, ff: Boolean = false) {
    var flipFlop = ff
    var t = from.toDouble()
    var i = 0
    while (t < 2* PI * 20) {
        // archimedes
        //val r = a * t
        // fermat
        val r = (a * t.pow(0.5)).toFloat() * 6
        val x = (cos(t) * r * direction).toFloat()
        val y = (sin(t) * r).toFloat()

        val color = palette[i]
        i++
        if (i >= palette.size) {
            i = 0
        }

        if (flipFlop) {
            val size = 60f + sin(i/2.1f) * 20f
            canvas.drawCircle((cx + x), (cy + y), size, strokeOfBlack(1f))
            canvas.drawCircle((cx + x), (cy + y), size, fillOf(color))
        }
        flipFlop = !flipFlop

        t += step
    }
    a += 0.001f
    step -= 0.00002f
}

const val black = 0x55121212.toLong()
const val white = 0xcceeeeee
const val offcolor = 0xcc005f73

val palette = Palette(
    black,
    black,
    offcolor,
    black,
    black,
    white,
    black,
    black,
    offcolor,
    black,
    white,
    black,
    offcolor,
    black,
    black,
    black,
)
