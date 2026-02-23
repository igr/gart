package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cos
import dev.oblac.gart.angle.sin
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.pow
import kotlin.math.sqrt

data class Circle(val x: Float, val y: Float, val radius: Float) {
    constructor(center: Point, radius: Number) : this(center.x, center.y, radius.toFloat())
    val center = Point(x, y)
    val topPoint = Point(x, y - radius)
    val bottomPoint = Point(x, y + radius)
    val leftPoint = Point(x - radius, y)
    val rightPoint = Point(x + radius, y)

    companion object {
        fun of(center: Point, radius: Number) = Circle(center.x, center.y, radius.toFloat())
        fun of(a: Point, b: Point, c: Point) = circleFrom3Points(a, b, c)
    }

    fun contains(x: Float, y: Float): Boolean {
        val dx = x - this.x
        val dy = y - this.y
        return dx * dx + dy * dy <= radius * radius
    }

    fun contains(p: Point): Boolean {
        val dx = p.x - this.x
        val dy = p.y - this.y
        return dx * dx + dy * dy <= radius * radius
    }

    fun rect() = Rect(x - radius, y - radius, x + radius, y + radius)

    fun tangentAtPoint(pointOnCircle: Point): DLine {
        // Vector from center to point
        val vx = pointOnCircle.x - x
        val vy = pointOnCircle.y - y

        // Tangent is perpendicular to radius → rotate by 90°: (-vy, vx)
        val rawDx = -vy
        val rawDy = vx

        // Normalize (optional)
        val length = sqrt(rawDx * rawDx + rawDy * rawDy)
        val dx = rawDx / length
        val dy = rawDy / length

        return DLine(pointOnCircle, Vector2(dx, dy))
    }

    fun pointOnCircle(angleRad: Angle): Point {
        val x = x + radius * cos(angleRad)
        val y = y + radius * sin(angleRad)
        return Point(x, y)
    }

    fun movePointAlongCircle(point: Point, angleRadians: Angle): Point {
        // vector from center to point
        val dx = point.x - x
        val dy = point.y - y

        // rotate vector by angle
        val cosA = cos(angleRadians)
        val sinA = sin(angleRadians)

        val rotatedX = dx * cosA - dy * sinA
        val rotatedY = dx * sinA + dy * cosA

        // translate back to absolute position
        return Point(
            x = x + rotatedX,
            y = y + rotatedY
        )
    }

    fun isInsideOf(other: Circle): Boolean {
        val distanceCenters = center.distanceTo(other.center)
        return distanceCenters + radius <= other.radius
    }

    fun isInsideOf(rect: Rect): Boolean {
        return (x - radius >= rect.left) && (x + radius <= rect.right) &&
            (y - radius >= rect.top) && (y + radius <= rect.bottom)
    }

    fun points(count: Int) = createCircleOfPoints(center, radius, count)

    fun points(count: Int, startAngle: Angle, sweepAngle: Angle): List<Point> {
        val points = mutableListOf<Point>()
        val deltaAngle = sweepAngle / count
        for (i in 0 until count) {
            val angle = startAngle + deltaAngle * i
            points.add(pointOnCircle(angle))
        }
        return points
    }

    fun toPath() = PathBuilder().addCircle(x, y, radius).detach()

    /**
     * Scales the circle by the given factor, keeping the center point unchanged.
     */
    fun scale(value: Float) = Circle(x, y, radius * value)

    fun resize(newRadius: Float) = Circle(x, y, newRadius)

    fun grow(delta: Float) = Circle(x, y, radius + delta)
}

fun Canvas.drawCircle(circle: Circle, paint: Paint) = drawCircle(circle.center.x, circle.center.y, circle.radius, paint)

fun createCircleOfPoints(center: Point, radius: Float, steps: Int): List<Point> {
    val points = mutableListOf<Point>()
    val deltaAngle = Degrees(360f / steps)
    for (i in 0 until steps) {
        val angle = deltaAngle * i
        val x = center.x + radius * cos(angle)
        val y = center.y - radius * sin(angle)
        points.add(pointOf(x, y))
    }
    return points
}


fun Canvas.drawCircleArc(
    x: Float,
    y: Float,
    radius: Float,
    paint: Paint,
    start: Degrees = Degrees.ZERO,
    sweep: Degrees = Degrees.D180
) {
    val left = x - radius
    val top = y - radius
    val right = x + radius
    val bottom = y + radius

    // Draw a arc starting at `angleDegrees`
    this.drawArc(
        left,
        top,
        right,
        bottom,
        start.value,    // start angle in degrees
        sweep.value,    // sweep angle
        false,          // don't create PIE shape with center
        paint
    )
}

fun Canvas.drawCirclePie(
    x: Float,
    y: Float,
    radius: Float,
    paint: Paint,
    start: Angle = Degrees.ZERO,
    sweep: Angle = Degrees.D180
) {
    val left = x - radius
    val top = y - radius
    val right = x + radius
    val bottom = y + radius

    // Draw a arc starting at `angleDegrees`
    this.drawArc(
        left,
        top,
        right,
        bottom,
        start.degrees,    // start angle in degrees
        sweep.degrees,    // sweep angle
        true,             // create PIE shape with center
        paint
    )
}


fun circleFrom3Points(a: Point, b: Point, c: Point): Circle {
    // Calculate the midpoints of AB and BC
    val midAB = Point((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val midBC = Point((b.x + c.x) / 2f, (b.y + c.y) / 2f)

    // Slopes of AB and BC
    val dxAB = b.x - a.x
    val dyAB = b.y - a.y
    val dxBC = c.x - b.x
    val dyBC = c.y - b.y

    // Perpendicular slopes (handle vertical lines)
    val slopeAB = if (dyAB == 0f) Float.POSITIVE_INFINITY else -dxAB / dyAB
    val slopeBC = if (dyBC == 0f) Float.POSITIVE_INFINITY else -dxBC / dyBC

    val center = if (slopeAB.isInfinite()) {
        // AB is horizontal, so perpendicular is vertical
        val x = midAB.x
        val y = slopeBC * (x - midBC.x) + midBC.y
        Point(x, y)
    } else if (slopeBC.isInfinite()) {
        // BC is horizontal, so perpendicular is vertical
        val x = midBC.x
        val y = slopeAB * (x - midAB.x) + midAB.y
        Point(x, y)
    } else {
        // General case: intersect the two perpendicular bisectors
        val x = (slopeAB * midAB.x - slopeBC * midBC.x + midBC.y - midAB.y) / (slopeAB - slopeBC)
        val y = slopeAB * (x - midAB.x) + midAB.y
        Point(x, y)
    }

    val radius = sqrt((center.x - a.x).pow(2) + (center.y - a.y).pow(2))
    return Circle.of(center, radius)
}

fun Canvas.clipCircle(circle: Circle) = clipPath(circle.toPath())

fun Canvas.drawCircle(p: Point, r: Float, fill: Paint) = this.drawCircle(p.x, p.y, r, fill)
