package dev.oblac.gart.spirograph

import dev.oblac.gart.Dimension
import dev.oblac.gart.angles.Angle
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.util.circular
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point

data class Spirograph(
    val points: List<Point>,
    val tangents: List<DLine>
)

fun createSpirograph(
    d: Dimension,
    /**
     * The path to draw the spirograph on.
     * Must be closed and possibly simple.
     */
    path: Path,
    /**
     * Radius of the circle that travels along the path.
     */
    radius: Float,
    /**
     * The angle to rotate the circle while moving along the path.
     */
    deltaAngle: Angle,
    /**
     * The number of samples to take from the path.
     * They are used to calculate the tangents.
     * They should not be too close or too far apart.
     */
    samples: Int = 100,
    /**
     * The number of repetitions to draw the spirograph.
     */
    repetitions: Int = 20,
): Spirograph {
    val isPointInPath = IsPointInPath(d, path)

    val points = path.toPoints(samples).toList().take(samples - 1).circular()

    val spiroPoints = mutableListOf<Point>()
    val tangents = mutableListOf<DLine>()
    var angle = Degrees.of(0f)
    repeat(repetitions) {
        points.forEachIndexed { i, point ->
            val prev = points[i - 1]
            val current = points[i]
            val next = points[i + 1]

            // create triangle to detect if the points are collinear
            val triangle = Triangle(prev, current, next)
            val area = triangle.calculateArea()
            val tan = if (area < 1f) {
                // collinear points, tan == line
                DLine.of(prev, current, next)
            } else {
                // points are not collinear, so tangent is calculated
                val circle = circleFrom3Points(prev, current, next)
                circle.tangentAtPoint(current)
            }


            tangents.add(tan)
            //c.drawDLine(tan, 240f, strokeOfYellow(2f))  // OK, we have tangents

            // ***********

            val perp = tan.perpendicularDLine()

            val c1 = perp.pointFromStart(radius)
            val c2 = perp.pointFromEnd(radius)

            // now we have to check if the circle center is inside the path
            val isInside = isPointInPath.check(c1)
            val center = if (isInside) {
                c2
            } else {
                c1
            }

            val newCircle = Circle.of(center, radius)
            //c.drawPoint(newCircle.center, strokeOfRed(3f))

            val currentAngle = tan.dvec.toRadians() - Degrees.of(90f)
            val point = newCircle.pointOnCircle(currentAngle)
            val point2 = newCircle.pointOnCircle(currentAngle + Degrees.of(180f))

            // the more distant point is the winner
            val distance1 = point.distanceTo(current)
            val distance2 = point2.distanceTo(current)
            val winner = if (distance1 > distance2) {
                point
            } else {
                point2
            }

            // move winner
            val endPoint = newCircle.movePointAlongCircle(winner, angle)
            angle += deltaAngle

            //c.drawLine(Line(newCircle.center, winner), strokeOfGreen(2f))
            //c.drawLine(Line(newCircle.center, endPoint), strokeOfGreen(2f))
            spiroPoints.add(endPoint)
        }
    }
    return Spirograph(spiroPoints, tangents)
}

private val pal = Palettes.colormap008.expand(300)
