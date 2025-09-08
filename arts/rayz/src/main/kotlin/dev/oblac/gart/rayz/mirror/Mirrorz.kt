package dev.oblac.gart.rayz.mirror

import dev.oblac.gart.*
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.PalettesNavigator
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.ray.Mirror
import dev.oblac.gart.ray.Ray
import dev.oblac.gart.ray.RayTrace
import dev.oblac.gart.ray.traceRayWithReflections
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

private val pn = PalettesNavigator()

private var pls = Palettes.cool47.expand(100).split(MAX_REFLECTIONS + 1)

//private var pls = Palettes.colormap097.expand(100).split(MAX_REFLECTIONS + 1)
private var redraw = false
private fun applyPalette() {
    pls = pn.palette().expand(100).split(MAX_REFLECTIONS + 1)
    println("Palette: ${pn.name()}")
    redraw = true
}

fun main() {
    val gart = Gart.of("mirrorz", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).onKey {
        when (it) {
            Key.KEY_Q -> {
                pn.previousSet()
                applyPalette()
            }

            Key.KEY_E -> {
                pn.nextSet()
                applyPalette()
            }

            Key.KEY_W -> {
                pn.previousPalette()
                applyPalette()
            }

            Key.KEY_S -> {
                pn.nextPalette()
                applyPalette()
            }

            else -> { /* no-op */
            }
        }
    }.hotReload(g)
}
/**
 * Hot reload requires a real class to be created, not a lambda.
 */
private class MyDraw(val g: Gartvas) : Drawing(g) {
    //val b = Gartmap(g)
    var lightSource = Point.relative(0.50f, 0.5f, g.d)

    init {
        draw(g.canvas, g.d, lightSource)
    }
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
//        f.onEveryFrame(3) {
//            lightSource = lightSource.offset(0f, 2f)
//            redraw = true
//        }
        if (redraw) {
            draw(g.canvas, g.d, lightSource)
            redraw = false
        }
        //b.updatePixelsFromCanvas()
        // draw pixels
        //b.drawToCanvas()
        c.draw(g)
    }
}

private const val MAX_REFLECTIONS = 4

private fun draw(c: Canvas, d: Dimension, lightSource: Point) {
    c.clear(RetroColors.black01)

    val total = 7
    val mirrors = mirrors(total, Point.relative(0.50f, 0.5f, d), radius = 0.2f, mirrorLength = 0.075f, d)

    val rays = generateSequence(0f) { it + 0.25f }.take(360 * 4).map {
        Ray(DLine.of(lightSource, Degrees.of(it)))
    }.toList()

    // Trace rays with reflections
    val allRayTraces = mutableListOf<RayTrace>()
    rays.forEach { ray ->
        allRayTraces.addAll(
            traceRayWithReflections(ray, mirrors, MAX_REFLECTIONS)
        )
    }

    drawRayTraces(c, allRayTraces)
    // Draw mirrors
    //debugDrawMirrors(c, mirrors)
}

private fun mirrors(
    total: Int,
    center: Point,
    radius: Float = 0.3f,           // distance from light source
    mirrorLength: Float = 0.08f,    // length of each mirror segment
    d: Dimension
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
    val perpAngle = centerAngle + 90f
    val halfLength = mirrorLength / 2f
    val dx = halfLength * cos(Math.toRadians(perpAngle.toDouble())).toFloat() * d.w
    val dy = halfLength * sin(Math.toRadians(perpAngle.toDouble())).toFloat() * d.h

    Mirror(
        Point(centerX - dx, centerY - dy),
        Point(centerX + dx, centerY + dy),
        0.7f
    )
}

private fun drawRayTraces(c: Canvas, rayTraces: List<RayTrace>) {
    repeat(MAX_REFLECTIONS + 1) { iteration ->
        val palette = pls[iteration]
        rayTraces
            .filter { it.iteration == iteration }
            .forEach { rayTrace ->
                val ray = rayTrace.ray
                val alpha = (ray.intensity * 255).toInt().coerceIn(0, 255)

                // Draw from 'from' point to 'to' point if 'to' exists, otherwise draw a long line
                val line = if (rayTrace.to != null) {
                    Line(rayTrace.from, rayTrace.to!!)
                } else {
                    ray.dline.toLine(rayTrace.from, 2000f)
                }
                val color = palette.random()
                c.drawLine(line, strokeOf(color, 5f + iteration * 8f).apply {
                    this.alpha = alpha
                    this.strokeCap = PaintStrokeCap.ROUND
                })
            }
    }

}

private fun debugDrawMirrors(c: Canvas, mirrors: List<Mirror>) {
    mirrors.forEach { mirror ->
        //c.drawLine(mirror.line, strokeOfWhite(4f))
    }
}
