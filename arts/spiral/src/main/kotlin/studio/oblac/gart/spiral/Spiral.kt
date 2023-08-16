package studio.oblac.gart.spiral

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import studio.oblac.gart.*
import studio.oblac.gart.gfx.*
import java.lang.Math.pow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

val d = Dimension(800, 800)
val g = Gartvas(d)

fun main() {
    val name = "spiral"
    println(name)

    val w = Window(g).show()
    val v = GartvasVideo(g, "$name.mp4", 30)

    w.paint2 { frames ->
        draw()
        v.addFrame()
        if (frames.time() > 12) {
            return@paint2 false
        }
        return@paint2 true
    }
    v.stopAndSaveVideo()

    g.writeSnapshotAsImage("$name.png")
}

fun draw() {
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
