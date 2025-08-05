package dev.oblac.gart.gravitron

import dev.oblac.gart.angles.Angle
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.math.Transform
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathDirection
import org.jetbrains.skia.PathEllipseArc
import org.jetbrains.skia.Point

data class Gravitron(
    val x: Float,
    val y: Float,
    val radius: Float,
    val angle: Angle = Degrees.of(180f),
) {

    val c = Point(x, y)
    val circle = Circle(x, y, radius)

    fun applyTo(line: Line, path: Path): Line? {
        path.lineTo(line.a.x, line.a.y)

        // find perpendicular point on the line
        val perpendicularLine = Line.fromPointToLine(c, line)
        val radi = perpendicularLine.length()
        if (radi >= radius) {
            // If the perpendicular point is outside the radius, just draw the line
            path.lineTo(line.b.x, line.b.y)
            return null
        }
        val pointB = perpendicularLine.b
        path.lineTo(pointB)

        // end point
        val rotatedPoint = Transform.rotate(c, angle)(pointB)
        // figure the path direction:
        val direction = if (line.a.x < x) {
            PathDirection.CLOCKWISE
        } else {
            PathDirection.COUNTER_CLOCKWISE
        }
        path.ellipticalArcTo(radi, radi, angle.degrees, PathEllipseArc.SMALLER, direction, rotatedPoint.x, rotatedPoint.y)

        // find line that is parallel to the original line, but staring from the rotated point
//        val parallelLine = Line.parallelTo(line, rotatedPoint)
//        return parallelLine

        // continue the line in the direction of the arc
        return if (direction == PathDirection.CLOCKWISE) {
            Line.fromPointAtAngle(rotatedPoint, angle, line.length())
        } else {
            Line.fromPointAtAngle(rotatedPoint, angle + Degrees.D180, line.length())
        }
    }

}
