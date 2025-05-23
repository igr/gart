package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angles.Angle
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.angles.cosf
import dev.oblac.gart.angles.sinf
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.poissonDiskSamplingNoise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.sin

fun main() {
    val gart = Gart.of("sf2", 1024, 1024 * GOLDEN_RATIO)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g)

    w.showImage(g)
}

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)

    val sunPoint = Point(d.cx, rndf(-d.h3 * 2, -d.h3))

    // this is the angle on both sides from the vertical line
    val angle = Degrees.of(45)

    repeat(200) {
        drawRandomRay(c, d, sunPoint, angle)
    }

    val planets = poissonDiskSamplingNoise(d, 320.0)
        .map { Circle(it, 50f + rndf(50f)) }
        .filter { circleIsInRect(it, d) }
        .shuffled()

    drawRibbon(c, d)

    val rings = Array(planets.size) { false }
    rings[0] = true
    rings.shuffle()
    planets.forEachIndexed { ndx, it -> drawPlanet(c, d, it, rings[ndx]) }

    c.drawRoundBorder(d, 10f, 40f, colorInk)
}

private fun drawRibbon(c: Canvas, d: Dimension) {
    // draw bezier curve
    val p1 = Point(-10f, d.hf * rndf(0.55f, 0.65f))
    val p2 = Point(d.w3x2, d.hf * 0.7f)
    val p3 = Point(d.w3, d.hf * 0.8f)
    val p4 = Point(d.w3, d.hf * 0.9f)
    val p5 = Point(d.w, d.hf * 0.8f)

    val path = listOf(p1, p2, p3, p4, p5)
    chaikinSmooth(path, 10, false)
        .forEachIndexed { index, point ->
            if (index % 2 == 0) {
                val r = (point.x) / 80f
                c.drawCircle(point, r, fillOf(colorBold))
            }
        }
}

private fun drawPlanet(
    c: Canvas,
    d: Dimension,
    circle: Circle,
    hasRing: Boolean
) {

    // Draw the ring first so the planet appears in front of it
    // draw ring around planet
    val ringRadiusX = circle.radius * 1.8f  // Ring is wider than the planet
    val ringRadiusY = circle.radius * 0.3f  // Ring is thinner in the y-direction to create perspective
    val ringThickness = circle.radius * 0.1f
    val outerRingRect = Rect(-ringRadiusX, -ringRadiusY, ringRadiusX, ringRadiusY)
    val rotateRing = rndf(25, 45) * (if (rndb()) 1 else -1)

    // Draw outer ring
    if (hasRing) {
        c.save()
        c.translate(circle.x, circle.y)
        c.rotate(rotateRing)
        c.drawArc(
            outerRingRect.left,
            outerRingRect.top,
            outerRingRect.right,
            outerRingRect.bottom,
            startAngle = -140f,
            sweepAngle = 180f,
            includeCenter = false,
            strokeOf(colorInk, ringThickness)
        )
        c.restore()
    }

    // draw the planet
    c.drawCircle(circle, fillOf(colorInk))
    val offsetY = circle.radius * 0.15f
    val offsetX = (circle.x - d.cx) * 0.01f
    val shadow = Circle(circle.center.offset(offsetX, offsetY), circle.radius)
    c.drawCircle(shadow, fillOf(colorBack))

    if (hasRing) {
        // draw the front half of the ring
        c.save()
        c.translate(circle.x, circle.y)
        c.rotate(rotateRing)
        c.drawArc(
            outerRingRect.left,
            outerRingRect.top,
            outerRingRect.right,
            outerRingRect.bottom,
            startAngle = 40f,
            sweepAngle = 180f,
            includeCenter = false,
            strokeOf(colorInk, ringThickness)
        )
        c.restore()
    }

}

// returns true if circle is in rect
private fun circleIsInRect(circle: Circle, d: Dimension): Boolean {
    if (circle.x - circle.radius < 0) return false
    if (circle.x + circle.radius > d.wf) return false
    if (circle.y - circle.radius < 0) return false
    if (circle.y + circle.radius > d.hf) return false
    return true
}

private fun drawRandomRay(
    c: Canvas,
    d: Dimension,
    sunPoint: Point,
    angle: Angle
) {
    val randomAngle = Degrees.of(rndf(-angle.degrees, angle.degrees) + 90f)

    // draw point from sunPoint at given random angle
    val distance = d.hf * 2  // Distance from sunPoint
    val newX = sunPoint.x + distance * cosf(randomAngle)
    val newY = sunPoint.y + distance * sinf(randomAngle)
    val newPoint = Point(newX, newY)

    val ray = Line(sunPoint, newPoint)

    // the ray is the full line from sunPoint
    // I want only a random part of this line

    // the start must be in the image
    val offsetA = -sunPoint.y
    val offsetB = 0f

    val ray2 = Line(ray.pointFromStartLen(offsetA), ray.pointFromEndLen(offsetB))

    // now, draw only some parts of the line
    val points = ray2.toPath().toPoints(1000)

    val off = rndf(100f)
    val visibilityFn = { i: Int ->
        sin(off + i * 0.04f) > 0
    }
    val stroke = rndf(1, 2)
    points.forEachIndexed { i, point ->
        if (visibilityFn(i)) {
            c.drawPoint(point, strokeOf(colorInk, stroke))
        }
    }
}
