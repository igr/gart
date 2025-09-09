package dev.oblac.gart.rayz.mirror2

import dev.oblac.gart.*
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndi
import dev.oblac.gart.ray.Mirror
import dev.oblac.gart.ray.Ray
import dev.oblac.gart.ray.RayTrace
import dev.oblac.gart.ray.traceRayWithReflections
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

//private var pls = Palettes.colormap097.expand(100).split(MAX_REFLECTIONS + 1)
private var redraw = false

fun main() {
    val gart = Gart.of("mirrorz2", 1024, 1024, fps = 30)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val m = gart.movieGif()
    val draw = MyDraw(g, m)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(MyDraw(g, m)).hotReload(g)
//    m.record(w).show(MyDraw(g, m))
}
/**
 * Hot reload requires a real class to be created, not a lambda.
 */
private class MyDraw(val g: Gartvas, val m: Movie) : Drawing(g) {
    //val b = Gartmap(g)
    var lightSource = Point.relative(0.60f, 0.6f, g.d)

    init {
        draw(g.canvas, g.d, lightSource, 26)
    }
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        f.onEveryFrame(1L) {
            //redraw = true
        }
        f.onFrame(260L) {
            //m.stopRecording()
        }
        if (redraw) {
            draw(g.canvas, g.d, lightSource, 73)
            redraw = false
        }
    }
}

private const val MAX_REFLECTIONS = 2

private fun draw(c: Canvas, d: Dimension, lightSource: Point, tick: Long) {
    c.clear(RetroColors.black01)

    val mirrors = mirrors(
        20,
        Point.relative(0.5f, 0.5f, d),
        radius = 0.38f,
        mirrorLength = 0.2f,
        d, tick)

    val rays = generateSequence(0f) { it + 4f }.take(90).map {
        Ray(DLine.of(lightSource, Degrees.of(it)))
    }.toList()

    // Trace rays with reflections
    val allRayTraces = mutableListOf<Pair<Int, RayTrace>>()
    rays.forEachIndexed { index, ray ->
        allRayTraces.addAll(
            traceRayWithReflections(ray, mirrors, MAX_REFLECTIONS).map {
                index to it
            }
        )
    }
    drawRayTraces(c, allRayTraces)
}

private fun mirrors(
    total: Int,
    center: Point,
    radius: Float,           // distance from light source
    mirrorLength: Float,    // length of each mirror segment
    d: Dimension,
    tick: Long
): List<Mirror> = List(total) { i ->
    // Create circular arrangement around lightSource with a gap
    val angleStep = 360f / total
    val gapAngle = 60f // gap in degrees
    val startAngle = gapAngle / 2f // start after half the gap

    val angle1 = startAngle + i * angleStep
    val angle2 = angle1 + angleStep - (gapAngle / total)

    // Calculate positions relative to light source
    val centerAngle = (angle1 + angle2) / 2f
    val centerX = center.x + radius * cos(Math.toRadians(centerAngle.toDouble())).toFloat() * d.w
    val centerY = center.y + radius * sin(Math.toRadians(centerAngle.toDouble())).toFloat() * d.h

    // Mirror endpoints perpendicular to radius
    val perpAngle = centerAngle + (tick % 360) + 90 * i
    val halfLength = mirrorLength / 2f
    val dx = halfLength * cos(Math.toRadians(perpAngle.toDouble())).toFloat() * d.w
    val dy = halfLength * sin(Math.toRadians(perpAngle.toDouble())).toFloat() * d.h

    Mirror(
        Point(centerX - dx, centerY - dy),
        Point(centerX + dx, centerY + dy),
        0.5f
    )
}

private fun drawRayTraces(c: Canvas, rayTraces: MutableList<Pair<Int, RayTrace>>) {
    repeat(MAX_REFLECTIONS + 1) { iteration ->
//        if (iteration == 1) {
//            c.drawCircle(300f, 300f, 100f, fillOf(RetroColors.black01))
//            c.drawCircle(700f, 700f, 100f, fillOf(RetroColors.black01))
//        }
        rayTraces
            .filter { it.second.iteration == iteration }
            .forEach { (rayIndex, rayTrace) ->
                val ray = rayTrace.ray
                val alpha = (ray.intensity * 255).toInt().coerceIn(0, 255)

                // Draw from 'from' point to 'to' point if 'to' exists, otherwise draw a long line
                val line = if (rayTrace.to != null) {
                    Line(rayTrace.from, rayTrace.to!!)
                } else {
                    ray.dline.toLine(rayTrace.from, 2000f)
                }

                val color = RetroColors.white01

                // draw aura
                val alpha1 = (alpha * 0.14).toInt()
                c.drawLine(line, strokeOf(color, 40f + iteration * 20f).apply {
                    this.alpha = alpha1
                    this.strokeCap = PaintStrokeCap.ROUND
                    this.pathEffect = PathEffect.makeDiscrete(40f, 10f, rndi())
                })
                c.drawLine(line, strokeOf(color, 20f).apply {
                    this.alpha = (alpha1 * 2).toInt()
                    this.strokeCap = PaintStrokeCap.ROUND
                })
                // draw stroke
                c.drawLine(line, strokeOf(color, 2f).apply {
                    this.alpha = alpha
                    this.strokeCap = PaintStrokeCap.ROUND
                })
            }

//        rayTraces
//            .filter { it.second.iteration == 0 }
//            .forEach { (rayIndex, rayTrace) ->
//                val ray = rayTrace.ray
//
//                val line = if (rayTrace.to != null) {
//                    Line(rayTrace.from, rayTrace.to!!)
//                } else {
//                    ray.dline.toLine(rayTrace.from, 2000f)
//                }
//                val line2 = line.lineFromStartLen(rndf(30f, 100f))
//                c.drawLine(line2, strokeOf(RetroColors.red01, 2f).apply {
//                    this.alpha = alpha
//                    this.pathEffect = PathEffect.makeDash(floatArrayOf(rndf(10f, 20f), rndf(4f, 8f)), rndf(0, 100f))
//                })
//            }
    }

}
