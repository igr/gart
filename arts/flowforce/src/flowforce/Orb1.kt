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
import dev.oblac.gart.glass.drawGlassBall
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Color
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

data class PointX(val point: Point, val color: Int)

fun main() {
    val gart = Gart.of(
        "orb1",
        1024, 1024
    )
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare field
    val flowField = FlowField.of(d) { x, y ->
        val a = 90 + sin(x * 0.01f) * 40 + cos(y * 0.005f) * 40
        Flow2(Degrees.of(a), 1f)
    }

    val plt = Palettes.cool101

    // draw background once
    val clr = RetroColors.black01
    c.clear(clr)
    val circle = Circle(d.cx, d.cy, 400f)
    c.drawCircle(circle, fillOf(BgColors.pearlWhite))

    // source - the list of points that gets
    val totalPoints = 20000
    val source = List(totalPoints) { i ->
        val y = rndf(0, 1024f)
        PointX(
            Point(
                rndf(0, 10f),
                y,
            ),
            plt.safe(y * 0.01),
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
        if (f.frame == 900L) {
            trace = false
            drawGlassBall(g, d.cx, d.cy, circle.radius + 5f, baseColor = RetroColors.black01, eta = 1.0 / 1.3, rimDarkening = false)
            c.drawCircle(circle.x, circle.y, circle.radius, strokeOf(RetroColors.black01, 15f).apply {
                this.imageFilter = ImageFilter.makeDropShadow(0f, 0f, 20f, 20f, Color.BLACK)
            })
            gart.saveImage(c)
        }
    }

    c.restore()
}
