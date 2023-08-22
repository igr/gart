package studio.oblac.gart

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import studio.oblac.gart.gfx.Palettes
import studio.oblac.gart.gfx.alpha
import studio.oblac.gart.gfx.fillOf
import studio.oblac.gart.gfx.strokeOf
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

val d = Dimension(800, 800)
val g = Gartvas(d)

fun main() {
    val name = "lissajous"
    println(name)

    val w = Window(g).show()
    val v = GartvasVideo(g, "$name.mp4", 30)

    w.paintWhile { frames ->
        draw()
        v.addFrame()
        if (frames.time() > 12) {
            return@paintWhile false
        }
        return@paintWhile true
    }
    v.stopAndSaveVideo()

    g.writeSnapshotAsImage("$name.png")
}


fun draw() {
    g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(0xFF121212))

    // draw every tick
    drawLissajous(g.canvas, d.cx, d.cy, d.wf - 40, d.hf - 40)

    //g.canvas.drawRect(Rect(0f, 0f, d.w.toFloat(), d.h.toFloat()), strokeOfWhite(40f))
}

var dd = 0f

fun drawLissajous(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float) {
    dd += 0.01f
    var t = 0.0f
    val d = dd
    val delta = 0.01f       // number of steps/dots
    val A = width / 2       // dimensions of square
    val B = height / 2      // dimensions ofs
    val a = 6f
    val b = 7f

    val dots = mutableListOf<Dot>()

    while (t < 2 * PI) {
        val x = A * sin(a * t + d)
        val y = B * sin(b * t)
        dots += Dot(cx + x, cy + y)
        t += delta
    }

    t = 0f
    while (t < 2 * PI) {
        val x = A * sin(7 * t + d)
        val y = B * sin(2 * t)
        dots += Dot(cx + x, cy + y)
        t += delta
    }

    drawConnectDots(canvas, dots)
//    dots
//        .filterIndexed { _, _ -> nextBoolean() }
//        .forEach { dot ->
//            dot(canvas, dot.x, dot.y)
//        }
}

private fun dot(canvas: Canvas, x: Float, y: Float) {
    canvas.drawCircle(x, y, 8f, fillOf(0x33FFFFFF))
    canvas.drawCircle(x, y, 4f, fillOf(0x66FFFFFF))
    canvas.drawCircle(x, y, 2f, fillOf(0x99FFFFFF))
    canvas.drawCircle(x, y, 1f, fillOf(0xFFFFFFFF))
}

data class Dot(val x: Float, val y: Float) {
    fun distanceTo(other: Dot): Float {
        return ((x - other.x).pow(2) + (y - other.y).pow(2)).pow(0.5f)
    }
}

fun drawConnectDots(canvas: Canvas, dots: List<Dot>) {
    dots.forEach { dot ->
        val maxDistance = 60f + sin(dot.x / 250) * 20f + sin(dot.y / 250) * 50f
        val color = alpha(palette.getSafe((dot.x / 50).toInt()), 0x33)
        dots.forEach { otherDot ->
            val distance = dot.distanceTo(otherDot)
            if (distance < maxDistance) {
                canvas.drawLine(dot.x, dot.y, otherDot.x, otherDot.y, strokeOf(color, 1.5f))
            }
        }
    }
}
val palette = Palettes.cool3 + Palettes.cool3.reversed()
