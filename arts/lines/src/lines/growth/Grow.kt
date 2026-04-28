package lines.growth

import dev.oblac.gart.Gart
import dev.oblac.gart.color.CyanotypeColors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.grow.Growth
import org.jetbrains.skia.*

private val colorBack = CyanotypeColors.palette1.last()
private val colorFront = CyanotypeColors.palette1[1]
private val colorAccent = NipponColors.col008_ICHIGO

fun main() {
    val gart = Gart.of("growth", 1024, 1024)
    println(gart)


    val g = gart.gartvas()

    val growth = Growth(
        maxEdgeLength = 5f,
        rejectionRadius = 16f,
        attractionStrength = 0.15f,
        rejectionStrength = 0.5f,
        brownianStrength = 0.4f,
        centerX = gart.d.cx,
        centerY = gart.d.cy,
    )

    val rectPath = PathBuilder().apply {
        val ox = gart.d.cx
        val oy = gart.d.cy
        val s = 440f
        moveTo(ox - s, oy - s)
        lineTo(ox + s, oy - s)
        lineTo(ox + s, oy + s)
        lineTo(ox - s, oy + s)
        closePath()
    }.detach()

    val circlePath = Circle(gart.d.cx + 200f, gart.d.cy + 390f, 100f).toPath()
    val jaggedCircle = discretizePath(circlePath, segLength = 8f, deviation = 3f, seed = 0)
    val cutout = combinePathsWithOp(PathOp.DIFFERENCE, rectPath, jaggedCircle)

    growth.setObstacle(cutout)
    growth.seedCircle(gart.d.cx, gart.d.cy, 40f, 3)

    val stroke = strokeOf(colorFront, 0.5f).apply {
        strokeCap = PaintStrokeCap.ROUND
        strokeJoin = PaintStrokeJoin.ROUND
        alpha = 20
    }

    val w = gart.window()
    val gc = g.canvas
    gc.clear(colorBack)

    val circleStamp = Circle(250f, 160f, 120f)
    val stroke2 = fillOf(colorAccent)

    w.show { c, _, f ->
        growth.step()
        gc.drawPath(growth.toPath(), stroke)
        c.draw(g)
        c.drawCircle(circleStamp, stroke2)
        c.drawCircle(circleStamp, strokeOf(colorAccent, 8f).apply {
            this.pathEffect = PathEffect.makeDiscrete(30f, 4f, 0)
        })
        if (f.frame == 821L) {
            gart.saveImage(c)
        }
    }
}
