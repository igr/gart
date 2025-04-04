package dev.oblac.gart.rayz

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.angles.cosf
import dev.oblac.gart.angles.sinf
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("rayz", 1024, 1024, 15)
    println(gart)

    val g = gart.gartvas()
    val w = gart.window()
    val m = gart.movieGif()

    var paletteOffset = 0f
//    w.show { c, d, f ->
//        paletteOffset -= 3f
//        draw(c, d, paletteOffset.toInt())
//    }
    m.record(w).show { c, d, f ->
        paletteOffset -= 3f
        draw(c, d, paletteOffset.toInt())
        f.onFrame(1) {
            gart.saveImage(c)
        }
        f.onFrame(280) {
            m.stopRecording()
        }
    }
}

private const val POINTS: Int = 1000
private val ppp = Palettes.cool37
private val palette = (ppp + ppp.reversed()).expand(POINTS)

private fun draw(c: Canvas, d: Dimension, paletteOffset: Int) {
    val xo = 600f + sin(paletteOffset.toFloat() / 100) * 90f
    val yo = 900f + cos(paletteOffset.toFloat() / 100) * 50f
    val xe = 900f + sin(paletteOffset.toFloat() / 100) * 90f
    val ye = 600f + cos(paletteOffset.toFloat() / 100) * 50f
    val obstacle = Obstacle(Point(xo, yo), Point(xe, ye), 100f)

    c.clear(BgColors.elegantDark)

    val from = drawSun(c, d, palette.safe(paletteOffset))

    for (i in 0..360 step 3) {
        drawRay(c, from, d.w, Degrees.of(i), obstacle, paletteOffset)
    }

    drawObstacle(c, obstacle)
}

private fun drawSun(c: Canvas, d: Dimension, safe: Int): Point {
    val center = d.center
    c.drawCircle(center, 80f, fillOf(safe))
    return center
}


private fun drawRay(c: Canvas, from: Point, len: Int, angle: Degrees, o: Obstacle, paletteOffset: Int) {
    val center = from
    val length = len
    val endX = center.x + length * cosf(angle)
    val endY = center.y + length * sinf(angle)
    val line = Line(center, Point(endX, endY))

    val ix = intersectionOf(o.line, line)
    val rayLine = if (ix != null) {
        //c.drawPoint(ix, strokeOfRed(5f))
        val p = calcWayAround(line, o, ix)
        chaikinSmooth(p, 12).toQuadPath()
    } else {
        line.toPath()
    }

    val allPoints = rayLine.toPoints(POINTS)
    allPoints.forEachIndexed { index, point ->
        c.drawPoint(point, strokeOf(palette.safe(index + paletteOffset), 2f + sin(index.toFloat() / 20) * 1f))
    }
}

/**
 * Line that goes around the obstacle.
 */
internal fun calcWayAround(line: Line, obstacle: Obstacle, intersectionPoint: Point, symmetrical: Boolean = true): List<Point> {
    val points = mutableListOf<Point>()
    points.add(line.a)

    // make a new line
    val lineToIntersectionPointA = Line(line.a, intersectionPoint)
    val turningPoint1 = lineToIntersectionPointA.pointFromEndLen(obstacle.gap)
    points.add(turningPoint1)

    // find the closest point on the obstacle
    val dA = turningPoint1.distanceTo(obstacle.a)
    val dB = turningPoint1.distanceTo(obstacle.b)
    // slightly move the closestObstaclePoint inside the obstacle
    // depending on where the intersection point is
    val a_ix = intersectionPoint.distanceTo(obstacle.a)
    val b_ix = intersectionPoint.distanceTo(obstacle.b)
    val ix_len = obstacle.line.length()

    val closestObstaclePoint = if (symmetrical) {
        if (dA < dB) {
            val delta = a_ix / ix_len
            obstacle.a.moveTowards(obstacle.b, delta * 50f)
        } else {
            val delta = b_ix / ix_len
            obstacle.b.moveTowards(obstacle.a, delta * 50f)
        }
    } else {
        val delta = b_ix / ix_len
        obstacle.b.moveTowards(obstacle.a, delta * 50f)
    }

    points.add(closestObstaclePoint)

    // NOW do the same from the bottom side
    val lineToIntersectionPointB = Line(line.b, intersectionPoint)
    val turningPoint2 = lineToIntersectionPointB.pointFromEndLen(obstacle.gap)
    points.add(turningPoint2)
    points.add(line.b)

    return points
}

val oPal = Palettes.gradient(BgColors.elegantDark, BgColors.milkMustache, 10)

private fun drawObstacle(c: Canvas, obstacle: Obstacle) {
    val oc = obstacle.line.centerPoint()

    repeat(3) {
        c.drawCircle(oc.offset(rndf(-5f, 5f), rndf(-5f, 5f)), 55f + rndf(-10f, 10f), strokeOf(oPal.random(), 2f))
    }
    repeat(3) {
        randomEquilateralTriangle(oc, 100f + rndf(-10f, 10f)).path.let {
            c.drawPath(it, strokeOf(oPal.random(), 2f))
        }
    }
    repeat(3) {
        randomSquareAroundPoint(oc, 100f + rndf(-10f, 10f)).path.let {
            c.drawPath(it, strokeOf(oPal.random(), 2f))
        }
    }


//    c.drawLine(obstacle.line, strokeOfRed(2f))
//    obstacle.line.toFatLine(20f).let {
//        c.drawPath(it, strokeOfBlue(2f))
//    }
}

data class Obstacle(val a: Point, val b: Point, val gap: Float = 100f) {
    val line = Line(a, b)
}
