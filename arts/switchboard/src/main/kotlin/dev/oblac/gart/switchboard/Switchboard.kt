package dev.oblac.gart.switchboard

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.Matrix
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndi
import dev.oblac.gart.smooth.chaikinSmooth
import org.jetbrains.skia.*

val gart = Gart.of("switchboard", Dimension.DESKTOP_FULL_HD)

const val a = 40    // size of the pixel
val matrix = Matrix(gart.d.w / a, gart.d.h / a) { _, _ -> false }
val coordinates = matrix.coordinates().shuffled()


fun main() {
    println(gart)

    val g = gart.gartvas()
    g.draw { c, _ ->  draw(c) }
    gart.saveImage(g)
    gart.window().showImage(g)
}


val palette = Palettes.cool12
//val palette = Palettes.cool9
//val palette = Palettes.cool35

fun draw(canvas: Canvas) {
    val switches = mutableListOf<Switch>()

    coordinates.forEach { (x, y) ->
        val type = randomSwitchType(x, y) ?: return@forEach
        val s = Switch(x, y, type, palette.random())
        switches.add(s)
        addSwitch(matrix, x, y, type)
    }

    val circles = switches.flatMap { it.draw(canvas) }
    drawConnectionsHangingLines(canvas, circles)
}

fun drawConnectionsHangingLines(canvas: Canvas, circles: List<Point>) {
    repeat(60) {
        val c1 = circles.random()
        val c2 = chooseRandomNearBy(c1, circles)
        val line = listOf(c1, middlePoint(c1, c2), c2)

        chaikinSmooth(line, 12).let { points ->
            points.zipWithNext().forEach { (p1, p2) ->
                canvas.drawLine(p1, p2, wire)
            }
        }
    }
}

fun chooseRandomNearBy(c1: Point, circles: List<Point>): Point {
    // filter out the same point and distant points
    val close = circles.filter { it != c1 && it.distanceTo(c1) < 400 }
    // filter out in the same column or row
    val remaining = close.filter { it.x != c1.x && it.y != c1.y }
    return remaining.random()
}

fun middlePoint(c1: Point, c2: Point): Point {
    // sort points by y
    val (a, b) = if (c1.y < c2.y) c1 to c2 else c2 to c1
    val hang = 40 + rndi(40) * 5
    val closeToLower = rndi(10) * 5
    return Point((a.x + b.x) / 2 - closeToLower, (a.y + b.y) / 2 + hang)
}

fun checkIfTypeFits(matrix: Matrix<Boolean>, x: Int, y: Int, type: SwitchType): Boolean {
    val w = type.w
    val h = type.h

    for (i in x until x + w) {
        for (j in y until y + h) {
            try {
                if (matrix[i, j]) {
                    return false
                }
            } catch (e: Exception) {
                return false
            }
        }
    }
    return true
}

fun addSwitch(matrix: Matrix<Boolean>, x: Int, y: Int, type: SwitchType) {
    val w = type.w
    val h = type.h
    for (i in x until x + w) {
        for (j in y until y + h) {
            matrix[i, j] = true
        }
    }
}

enum class SwitchType(val w: Int, val h: Int) {
    SW_1x1(1, 1),
    SW_2x1(2, 1),
    SW_3x1(3, 1),
    SW_4x1(4, 1),
    SW_5x1(5, 1),
    SW_1x3(1, 3),
    SW_1x4(1, 4),
    SW_1x5(1, 5);

    fun isRectangular() = w != h
}

fun randomSwitchType(x: Int, y: Int): SwitchType? {
    val types = SwitchType.entries.toTypedArray().reversedArray()
    for (type in types) {
        if (checkIfTypeFits(matrix, x, y, type)) {
            return type
        }
    }
    return null
}

data class Switch(val x: Int, val y: Int, val type: SwitchType, val color: Int = Colors.crimson) {
    @Suppress("t")
    fun draw(canvas: Canvas): List<Point> {
        val x0 = x * a
        val y0 = y * a
        val x1 = x0 + a * type.w
        val y1 = y0 + a * type.h

        val circles = mutableListOf<Point>()

        // background
        canvas.drawRect(Rect.of(x0, y0, x1, y1), color.toFillPaint())

        if (type.isRectangular()) {
            // inner circles
            var cx = x0 + a / 2f
            var cy = y0 + a / 2f
            val r = a / 4f
            while (true) {
                canvas.drawCircle(cx, cy, r, strokeOfBlack(3))
                circles.add(Point(cx, cy))
                var change = false
                if (cx + a < x1) {
                    cx += a
                    change = true
                }
                if (cy + a < y1) {
                    cy += a
                    change = true
                }
                if (!change) {
                    break
                }
            }
        } else {
            when(rndi(2)) {
                0 -> diagonalLines(canvas, x0, y0, x1, y1)
                1 -> dotted(canvas, x0, y0, x1, y1)
            }

        }

        // border
        canvas.drawRect(Rect.of(x0, y0, x1, y1), strokeOfBlack(3))

        maybeConnectInnerCircles(canvas, circles)

        return circles
    }

    private fun maybeConnectInnerCircles(canvas: Canvas, circles: MutableList<Point>) {
        if (circles.size < 2) {
            return
        }
        if (rndb(2, 20)) {
            return
        }
        val p1 = circles.random()
        val p2 = circles.random()
        if (p1 == p2) {
            return
        }
        canvas.drawLine(p1, p2, wire)
    }

    private fun dotted(
        canvas: Canvas,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int) {
        val rect = Rect.of(x0, y0, x1, y1)
        canvas.drawRect(rect, hatchPaint)
    }


    private fun diagonalLines(
        canvas: Canvas,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int
    ) {
        canvas.save()
        val rect = Rect.of(x0, y0, x1, y1)
        val clipPath = PathBuilder().addRect(rect).detach()
        canvas.clipPath(clipPath)
        canvas.drawRect(rect.grow(2f), if (rndb()) dashPaint else dashPaint2)
        canvas.restore()
    }
}

val hatchPaint = hatchPaint(Colors.black, density = 4f, dotWidth = 1.4f)
val dashPaint = dashPaint(Colors.black)
val dashPaint2 = dashPaint(Colors.black, angle = Degrees.of(45f))
val wire = strokeOfBlack(5).also { it.strokeCap = PaintStrokeCap.ROUND }
