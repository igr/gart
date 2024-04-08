package dev.oblac.gart.cotton.circles2

import dev.oblac.gart.Gart
import dev.oblac.gart.gfx.BgColors
import dev.oblac.gart.gfx.Palettes
import dev.oblac.gart.shader.createNoiseGrainFilter
import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.Path
import dev.oblac.gart.skia.Rect
import dev.oblac.gart.util.loop
import kotlin.math.sin

val gart = Gart.of(
    "cotton-circles2",
    1024, 1024
)

fun main() {
    println(gart)
    val palette = Palettes.cool31
    val colors = palette.map {
        Paint().apply {
            color = it
            imageFilter = createNoiseGrainFilter(0.2f, gart.d)
            isAntiAlias = true
        }
    }

    val max = gart.d.cx - 20f
    val count = 10
    val delta = max / count
    val random = Array(count) { (Math.random() * 360).toFloat() }

    val w = gart.window()
    val m = gart.movie()
    val end = 2 * 2 * (3.14f * 100).toLong()

//    m.record(w).show { c, d, f ->
    w.show { c, d, f ->
        c.clear(BgColors.dark01)
        loop(count) { x ->
            drawCCircle(
                c,
                d.cx, d.cy,
                max - x.toFloat() * delta,
                colors[x % colors.size],
                colors[(x + 1) % colors.size],
                f.frame,
                random[x]
            )
            c.drawCircle(d.cx, d.cy, delta + 1, colors.last())
        }
        if (f.frame == end) {
            m.stopRecording()
        }
    }
    //gart.showImage(g)
    //gart.saveImage(g)
}

fun drawCCircle(c: Canvas, x: Float, y: Float, r: Float, color1: Paint, color2: Paint, frametime: Long, random: Float) {
    val s = sin(frametime.toFloat() / 300f)
    var angle = -60f + 5 * frametime + s * s * 20f * (r / 14f)

    //val angle = 90f + random

    val rect = Rect(x - r, y - r, x + r, y + r)
    loop(4) { a ->
        val c1 = color1.makeClone().also { it.alpha = 255 - a * 50 }
        val c2 = color2.makeClone().also { it.alpha = 255 - a * 50 }

        val xx = a * 40f
        c.drawPath(Path().addArc(rect, angle + a * 20f, 180f + xx).closePath(), c1)
        c.drawPath(Path().addArc(rect, angle + a * 20f + 180f, 180f - xx).closePath(), c2)
    }

}
