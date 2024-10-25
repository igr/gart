package dev.oblac.gart.flowforce.eclipse

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angles.Radians
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Colors
import dev.oblac.gart.force.Flow
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.force.ForceGenerator
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.rndf
import dev.oblac.gart.shader.createMarbledFilter
import dev.oblac.gart.util.loop
import org.jetbrains.skia.*
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val gart = Gart.of("Eclipse", 1024, 1024)
val rnd = Random()

fun main() {
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    val moonR = 300f

    // background

    c.clear(BgColors.dark01)

    // rays

    drawRays(c, d, moonR)

    // halo

    drawHalo(c, d, moonR)

    // black sun

    c.drawCircle(d.cx, d.cy, moonR, fillOfBlack().also {
        it.imageFilter = createMarbledFilter(0.9f, g.d)
    })
//    c.drawCircle(d.cx, d.cy, moonR - 40f, fillOfBlack().apply {
//        imageFilter = ImageFilter.makeBlur(10f, 10f, FilterTileMode.DECAL)
//    })

    // show

    gart.window().showImage(g)
    gart.saveImage(g)
}


fun drawHalo(c: Canvas, d: Dimension, moonR: Float) {
    val haloPoints = Array(100) {
        randomPoint(d.cx, d.cy, moonR)
    }
    val haloPaint = fillOfWhite().also {
        it.imageFilter = ImageFilter.makeBlur(40f, 40f, FilterTileMode.DECAL)
        it.alpha = 180
    }

    haloPoints.forEach { p ->
        c.drawCircle(p.x, p.y, rndf(1f, 40f), haloPaint)
    }
}


fun drawRays(c: Canvas, d: Dimension, moonR: Float) {
    val flow = ForceGenerator { x, y ->
        val dx = x - d.cx
        val dy = y - d.cy
        val theta = atan2(dy, dx) + PIf / 2f + rndf(-0.3f, 0.3f)
        Flow(Radians(theta), 2f)
    }
    val ff = ForceField.of(gart.d) { x, y -> flow(x, y) }

    val rayPoints = Array(500) {
        PointsTrail(1).apply {
            val angle = (if (rnd.nextBoolean()) 330f else 0f) + rnd.nextGaussian() / 2f

            val r = rndf(10f, moonR)
            val x = d.cx + r * cos(angle)
            val y = d.cy + r * sin(angle)
            add(Point(x.toFloat(), y.toFloat()))
        }
    }.toList()

    val rayPaint = Paint().apply {
        color = Colors.white
        strokeWidth = 1f
        alpha = 30
    }

    loop(200) {
        rayPoints
            .filter { it.isActive() }
            .forEach { trail ->
                trail.update {
                    ff[it].offset(it)
                }
            }
        rayPoints.forEach { trail ->
            trail
                .filter { it.isInside(d) }
                .sequence()
                .forEach { p ->
                    c.drawPoint(p.x, p.y, rayPaint)
                }
        }
    }

}
