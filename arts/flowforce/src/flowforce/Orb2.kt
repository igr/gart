package flowforce

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.PointTracer
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Color
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of(
        "orb2",
        1024, 1024
    )
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare field
    val flowField = FlowField.of(d) { x, y ->
        val a = 360f * (
            sin(x * 0.017f) * 0.5f +
            cos(y * 0.013f) * 0.3f +
            sin((x + y) * 0.007f) * 0.4f +
            cos((x - y) * 0.023f) * 0.2f +
            sin(x * y * 0.00005f) * 0.3f
        )
        val m = 0.6f + abs(sin((x + y) * 0.005f)) * 0.8f
        Flow2(Degrees.of(a), m)
    }

    val plt = Palettes.cool85.expandReversed()

    // draw background once
    val clr = RetroColors.black01
    c.clear(clr)
    val circle = Circle(d.cx, d.cy, 400f)
    c.drawCircle(circle, fillOf(BgColors.pearlWhite))

    // source - the list of points that gets
    val totalPoints = 40000
    val source = List(totalPoints) { i ->
        val x = rndf(0, 1024f)
        val y = rndf(0, 1024f)
        PointX(
            Point(
                rndf(0, 1024f),
                y,
            ),
            plt.safe(x * 0.01 + y * 0.01),
        )
    }

    // persistent pool of points that flow through the field until they leave the screen
    var points: List<PointX> = source.shuffled().take(100)

    val tracer = PointTracer(d, flowField)
    val w = gart.window()
    var trace = true
    w.show { wc, _, f ->
        if (trace) {
            c.save()
            c.clipCircle(circle)
            points = points + List(totalPoints - points.size) { source.random() }
            val next = ArrayList<PointX>(points.size)
            for (px in points) {
                val np = tracer.trace(px.point) ?: continue
                val paint = fillOf(px.color).alpha(100)
                //c.drawLine(px.point.x, px.point.y, np.x, np.y, paint)
                c.drawCircle(np.x, np.y, 2f, paint)
                next.add(PointX(np, px.color))
            }
            points = next
            c.restore()
        }
        wc.draw(g)
        if (f.frame == 200L) {
            trace = false
            c.drawCircle(circle.x, circle.y, circle.radius, strokeOf(RetroColors.black01, 15f).apply {
                this.imageFilter = ImageFilter.makeDropShadow(0f, 0f, 20f, 20f, Color.BLACK)
            })
            gart.saveImage(c)
        }
    }

    c.restore()
}
