package dev.oblac.gart.cotton.circles2

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.force.CircularFlow
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.gfx.drawPoints
import dev.oblac.gart.gfx.isInside
import dev.oblac.gart.gfx.randomPoint
import dev.oblac.gart.gfx.strokeOfWhite
import dev.oblac.gart.shader.createNoiseGrainFilter
import dev.oblac.gart.util.loop
import org.jetbrains.skia.*
import kotlin.math.sin

val gart = Gart.of(
    "cotton-circles2",
    1024, 1024
)

const val period = 300f

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
    val random = Array(count) { (Math.random() * 90).toFloat() * 4 }

    val w = gart.window()
    val m = gart.movie()

    val end = 2 * 2 * (3.14f * period).toLong()

    val circleFlowForce = CircularFlow(gart.d.cx, gart.d.cy)
    val forceField = ForceField.of(gart.d) { x, y -> circleFlowForce(x, y) }

    var points = Array(500) {
        randomPoint(gart.d.cx, gart.d.cy, max, 60f)
    }.toList()

    val dotPaint = strokeOfWhite(2f).also {
        it.imageFilter = ImageFilter.makeBlur(3f, 3f, FilterTileMode.DECAL)
    }

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

        c.drawPoints(points, dotPaint)
        points = points
            .filter { it.isInside(d) }
            .map {
                forceField[it.x, it.y].offset(it)
            }
            .toList()

        if (f.frame == end) {
            m.stopRecording()
        }
        if (f.frame == 10L) {
            gart.saveImage(c)
        }
    }.onClose {
        println("done")
    }
    //gart.showImage(g)
    //gart.saveImage(g)
}

fun drawCCircle(
    c: Canvas,
    x: Float,
    y: Float,
    r: Float,
    color1: Paint,
    color2: Paint,
    frametime: Long,
    random: Float
) {
    val s = sin(frametime.toFloat() / period)
//    val angle = -60f + 5 * frametime + s * s * 20f * (r / 14f)

    val angle = 90f + random

    val rect = Rect(x - r, y - r, x + r, y + r)
    loop(4) { a ->
        val c1 = color1.makeClone().also { it.alpha = 255 - a * 50 }
        val c2 = color2.makeClone().also { it.alpha = 255 - a * 50 }

        val xx = a * 40f
        c.drawPath(PathBuilder().addArc(rect, angle + a * 20f, 180f + xx).closePath().detach(), c1)
        c.drawPath(PathBuilder().addArc(rect, angle + a * 20f + 180f, 180f - xx).closePath().detach(), c2)
    }

}
