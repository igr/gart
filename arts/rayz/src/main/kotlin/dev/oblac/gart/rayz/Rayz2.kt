package dev.oblac.gart.rayz

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.angles.Angle
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.angles.cosf
import dev.oblac.gart.angles.sinf
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

private const val POINTS: Int = 1000

private fun p(p: Palette) = (p + p.reversed()).expand(POINTS)

private val palette = p(Palettes.cool37.reversed())
private val palette2 = p(Palettes.cool16)
private val palette3 = p(Palettes.cool11)

private var backColor = BgColors.elegantDark
private var x1 = 500f
private var y1 = 500f
private var x2 = 500f
private var y2 = 900f
private var gap = 80f
private var drawRayPoints: (Canvas, List<Point>) -> Unit = { c, allPoints ->
    allPoints.drop(150).forEachIndexed { index, point ->
        c.drawPoint(point, strokeOf(palette.safe(index), 1f + index / 80f))
    }
}

const val style = 3
fun main() {
    when (style) {
        1 -> {
            x1 = 200f
            y1 = 0f
            x2 = 900f
            y2 = 1250f
            gap = 105f
            drawRayPoints = { c, allPoints ->
                allPoints.drop(150).forEachIndexed { index, point ->
                    c.drawPoint(point, strokeOf(palette.safe(index), 1f + index / 80f))
                }
            }
        }

        2 -> {
            backColor = palette2[0]
            x1 = 200f
            y1 = 550f
            x2 = 700f
            y2 = 600f
            gap = 275f
            drawRayPoints = { c, allPoints ->
                allPoints.drop(rndi(80, 150)).forEachIndexed { index, point ->
                    if (index in 450..500) {
                        c.drawPoint(point, strokeOf(palette2.safe(index), 1f))
                    } else {
                        c.drawPoint(point, strokeOf(palette2.safe(index), 1f + index / 120f))
                    }
                }
            }
        }

        3 -> {
            backColor = palette3[600]
            x1 = -850f
            y1 = 100f
            x2 = 700f
            y2 = 850f
            gap = 225f
            drawRayPoints = { c, allPoints ->
                allPoints.drop(80).forEachIndexed { index, point ->
                    if (index / 400 % 2 == 0) {
                        c.drawPoint(point, strokeOf(palette3.safe(index), 0f + index / 15f))
                    }
                }
            }
        }

        else -> {}
    }

    val gart = Gart.of("rayz2-$style", 1024, 1024, 15)
    println(gart)

    val g = gart.gartvas()
    val w = gart.window()
    val m = gart.movieGif()

    w.show { c, d, f ->
        draw(c, d)
        f.onFrame(1) {
            gart.saveImage(c)
        }
    }.onKey { key ->
        when (key) {
            Key.KEY_W -> y1 -= 50f
            Key.KEY_S -> y1 += 50f
            Key.KEY_A -> x1 -= 50f
            Key.KEY_D -> x1 += 50f

            Key.KEY_I -> y2 -= 50f
            Key.KEY_K -> y2 += 50f
            Key.KEY_J -> x2 -= 50f
            Key.KEY_L -> x2 += 50f

            Key.KEY_Q -> gap -= 5f
            Key.KEY_E -> gap += 5f
            else -> {}
        }
        println("x1: $x1 y1: $y1 x2: $x2 y2: $y2 gap: $gap")
    }
}

private fun draw(c: Canvas, d: Dimension) {
    val obstacle = Obstacle(Point(x1, y1), Point(x2, y2), gap)

    c.clear(backColor)

    val from = drawSun(c, d)

    for (i in 0..360 step 5) {
        drawRay(c, from, d.w, Degrees.of(i), obstacle)
    }

    //drawObstacle(c, obstacle)
}

private fun drawSun(c: Canvas, d: Dimension): Point {
    val center = d.center
    if (style == 1) {
        c.drawCircle(center, 120f, fillOf(NipponColors.col014_KARAKURENAI))
    }
    return center
}

private fun drawRay(c: Canvas, from: Point, len: Int, angle: Angle, o: Obstacle) {
    val endX = from.x + len * cosf(angle)
    val endY = from.y + len * sinf(angle)
    val line = Line(from, Point(endX, endY))

    val ix = intersectionOf(o.line, line)
    val rayLine = if (ix != null) {
        //c.drawPoint(ix, strokeOfRed(5f))
        val p = calcWayAround(line, o, ix, false)
        chaikinSmooth(p, 12).toQuadPath()
    } else {
        line.toPath()
    }

    val allPoints = rayLine.toPoints(POINTS)
    drawRayPoints(c, allPoints)
//    allPoints.drop(150).forEachIndexed { index, point ->
//        c.drawPoint(point, strokeOf(palette.safe(index), 1f + index / 100f))
//    }
}
