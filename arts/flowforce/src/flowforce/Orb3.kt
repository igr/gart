package flowforce

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.PalettesOf4
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.PointTracer
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndsgn
import org.jetbrains.skia.Color
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of(
        "orb3",
        1024, 1024
    )
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    // prepare field
    val vortices = List(12) { Triple(rndf(0, 1024f), rndf(0, 1024f), rndsgn()) }
    val flowField = FlowField.of(d) { x, y ->
        var vx = 0f
        var vy = 0f
        for ((cx, cy, s) in vortices) {
            val dx = x - cx;
            val dy = y - cy
            val d2 = dx * dx + dy * dy + 400f
            vx += -s * dy / d2 * 2000f
            vy += s * dx / d2 * 2000f
        }
        Flow2(Radians.of(atan2(vy, vx) + PIf / 2), 1f)
    }

    val plt = PalettesOf4.q18

    // draw background once
    val clr = plt[0]//RetroColors.black01
    c.clear(clr)
    val circle = Circle(d.cx, d.cy, 400f)
    c.drawCircle(circle, fillOf(BgColors.pearlWhite))

    // source - the list of points that gets
    val totalPoints = 40000
    val source = List(totalPoints) {
        val x = rndf(0, 1024f)
        val y = rndf(0, 1024f)
        PointX(
            Point(x, y),
            //plt.safe(sin(x * 0.005) + y * 0.005),
            plt.safe(cos(x * 0.001) * y * 0.004 + sin(x * x * 0.1) * 0.1),
        )
    } + List(10000) {
        val p = randomPoint(circle.x + 100f, circle.y + 100f, 80f, 0f)
        PointX(
            p,
            plt[3]
        )
    }

    // persistent pool of points that flow through the field until they leave the screen
    var points: List<PointX> = source//.shuffled().take(100)

    val tracer = PointTracer(d, flowField)
    val w = gart.window()
    var trace = true
    w.show { wc, _, f ->
        if (trace) {
            c.save()
            c.clipCircle(circle)
            //points = points + List(totalPoints - points.size) { source.random() }
            val next = ArrayList<PointX>(points.size)
            for (px in points) {
                val np = tracer.trace(px.point) ?: continue
                val paint = fillOf(px.color).alpha(100)
                //c.drawLine(px.point.x, px.point.y, np.x, np.y, paint)
                c.drawCircle(np.x, np.y, 3f, paint)
                next.add(PointX(np, px.color))
            }
            points = next
            c.restore()
        }
        wc.draw(g)
        if (f.frame == 100L) {
            trace = false
            //drawGlassBall(g, d.cx, d.cy, circle.radius + 5f, baseColor = RetroColors.black01, eta = 1.0 / 1.3, rimDarkening = false)
            c.drawCircle(circle.x, circle.y, circle.radius, strokeOf(clr, 15f).apply {
                this.imageFilter = ImageFilter.makeDropShadow(0f, 0f, 20f, 20f, Color.BLACK)
            })
            gart.saveImage(c)
        }
    }

    c.restore()
}
