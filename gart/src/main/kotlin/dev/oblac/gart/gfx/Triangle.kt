package dev.oblac.gart.gfx

import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.dist
import dev.oblac.gart.math.rnd
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.*

data class Triangle(val a: Point, val b: Point, val c: Point) {
    fun points() = arrayOf(a, b, c)
    val path = Path()
        .moveTo(a)
        .lineTo(b)
        .lineTo(c)
        .closePath()

    fun contains(point: Point): Boolean {
        val circumcircle = calculateCircumcircle()
        val distance = dist(point, circumcircle.center)
        return distance < circumcircle.radius
    }

    fun calculateArea(): Double {
        val x1 = a.x
        val y1 = a.y
        val x2 = b.x
        val y2 = b.y
        val x3 = c.x
        val y3 = c.y
        return 0.5 * abs(x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2))
    }

    fun calculateCircumcircle(): Circle {
        val ax = a.x
        val ay = a.y
        val bx = b.x
        val by = b.y
        val cx = c.x
        val cy = c.y

        val d = 2 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by))
        val ux = ((ax.pow(2) + ay.pow(2)) * (by - cy) +
            (bx.pow(2) + by.pow(2)) * (cy - ay) +
            (cx.pow(2) + cy.pow(2)) * (ay - by)) / d
        val uy = ((ax.pow(2) + ay.pow(2)) * (cx - bx) +
            (bx.pow(2) + by.pow(2)) * (ax - cx) +
            (cx.pow(2) + cy.pow(2)) * (bx - ax)) / d

        val center = Point(ux, uy)
        val radius = sqrt((ax - ux).pow(2) + (ay - uy).pow(2))

        return Circle.of(center, radius)
    }
}

fun Canvas.drawTriangle(triangle: Triangle, paint: Paint) = this.drawPath(triangle.path, paint)

fun randomEquilateralTriangle(c: Point, sideLength: Float): Triangle {
    val centerX = c.x
    val centerY = c.y
    val angle = rnd(0.0, 360.0) // Random rotation in degrees
    val radians = Math.toRadians(angle).toFloat()

    // Calculate the 3 vertices
    val x1 = centerX + cos(radians) * sideLength / 2
    val y1 = centerY + sin(radians) * sideLength / 2

    val x2 = centerX + cos(radians + 2 * PIf / 3) * sideLength / 2
    val y2 = centerY + sin(radians + 2 * PIf / 3) * sideLength / 2

    val x3 = centerX + cos(radians + 4 * PIf / 3) * sideLength / 2
    val y3 = centerY + sin(radians + 4 * PIf / 3) * sideLength / 2

    return Triangle(Point(x1, y1), Point(x2, y2), Point(x3, y3))
}
