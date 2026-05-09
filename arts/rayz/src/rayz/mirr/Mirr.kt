package rayz.mirr

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.TWO_PIf
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.painter.SprayPainter
import dev.oblac.gart.ray.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color.TRANSPARENT
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val clLight = NipponColors.col233_SHIRONERI
private val clBack = RetroColors.black01
private val clAccent = NipponColors.col029_GINSYU

fun main() {
    val gart = Gart.of("mirr", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // save image
    draw(g.canvas, g.d)
    gart.saveImage(g)

    w.showImage(g)
//    w.show { canvas, dimension, frames ->
//        draw(canvas, dimension)
//    }
}

private const val MAX_REFLECTIONS = 0

private fun draw(c: Canvas, d: Dimension) {
    val lightPoint = Point.relative(0.76f, 0.76f, d)
    c.clear(clBack)

    val totalMirrors = 35
    val mp = Point.relative(0.4f, 0.6f, d)
    val mirrors = mirrors(totalMirrors, mp, radius = 0.1f, d)

    val rays = generateSequence(0f) { it + 1f }.take(360).map {
        Ray(DLine.of(lightPoint, Degrees.of(it)))
    }.toList()

    // Trace rays with reflections
    val allRayTraces = mutableListOf<List<RayTrace>>()
    rays.forEach { ray ->
        allRayTraces.add(
            traceRayWithReflections(ray, mirrors, MAX_REFLECTIONS)
        )
    }

    drawRayTraces(c, allRayTraces)
    drawBall(c, d, mp, mirrors)
}

private fun mirrors(
    total: Int,
    center: Point,
    radius: Float = 0.4f,
    d: Dimension
): List<Mirror> {
    val points = createNtagonPoints(total, center.x, center.y, radius * d.w, 55f)
    return List(total) { i ->
        Mirror(points[i], points[(i + 1) % total], 1f)
    }
}

private fun drawRayTraces(c: Canvas, rayTraces: List<List<RayTrace>>) {
    val startEnergy = 0.5f
    val decayPerPoint = 0.992f
    val pointSpacing = 4f

    rayTraces.forEach { rays ->
        var energy = startEnergy

        rays.forEach { rayTrace ->
            val ray = rayTrace.ray
            val line = if (rayTrace.to != null) {
                Line(rayTrace.from, rayTrace.to!!)
            } else {
                ray.dline.toLine(rayTrace.from, 2000f)
            }

            val pointCount = (line.length() / pointSpacing).toInt().coerceAtLeast(2)
            val particlesPerPoint = 4
            line.points(pointCount).forEach { p ->
                val discRadius = (1 - energy) * 8f
                repeat(particlesPerPoint) {
                    val r = discRadius * sqrt(rndf(0f, 1f))
                    val theta = rndf(0f, TWO_PIf)
                    val jx = r * cos(theta)
                    val jy = r * sin(theta)
                    val radius = energy * 10f * rndf(0.5f, 1.4f)
                    val alphaInt = (energy * 255f * rndf(0.55f, 1f)).toInt().coerceIn(0, 255)

                    c.drawCircle(p.x + jx, p.y + jy, radius, fillOf(clLight).alpha(alphaInt))
                }
                energy *= decayPerPoint
            }
        }
    }
}

private fun drawBall(c: Canvas, d: Dimension, mp: Point, mirrors: List<Mirror>) {
    val ps = mirrors.toPath().toPoints(100)
    val sp = SprayPainter(d.w, d.h, bg = TRANSPARENT, fg = clAccent.alpha(40))
    val connections = (0 until 1400).map {
        Line(ps[rndi(ps.size)], ps[rndi(ps.size)])
    }
    sp.lines(connections, 180)
    sp.drawTo(c)

    //c.drawPath(mirrors.toPath(), strokeOf(clAccent, 2f))
}
