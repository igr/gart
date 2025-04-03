package dev.oblac.gart.rotoro

import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.gfx.intersectionsOf
import dev.oblac.gart.math.rndb
import org.jetbrains.skia.*

fun main() {
    val width = 1024
    val gart = Gart.of("rotoro2", width, width)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, width)

    gart.saveImage(g)
    gart.window().showImage(g)
}

private val background = NipponColors.col234_GOFUN
private val whitePaint = fillOfWhite()
private val blackPaint = fillOfBlack()

private val accentPaint = fillOf(NipponColors.col099_TAMAGO)
private val accentPaint2 = fillOf(NipponColors.col176_MIZU)
private val accentPaint3 = fillOf(NipponColors.col011_NAKABENI)

private val strokePaint = Paint().apply {
    color = Color.BLACK
    mode = PaintMode.STROKE
    strokeWidth = 20f
    strokeCap = PaintStrokeCap.ROUND
}
private val strokePaint2 = Paint().apply {
    color = Color.BLACK
    mode = PaintMode.FILL
    //strokeWidth = 20f
    strokeCap = PaintStrokeCap.ROUND
}


private fun draw(canvas: Canvas, canvasWidth: Int) {
    canvas.clear(background)

    // Calculate dimensions based on canvas size
    val cellSize = canvasWidth / 4f
    val radius = cellSize * 0.375f  // Circle radius relative to cell size

    val circles = mutableListOf<Circle>()
    for (row in 0..3) {
        for (col in 0..3) {
            val centerX = col * cellSize + cellSize / 2
            val centerY = row * cellSize + cellSize / 2
            circles.add(Circle(centerX, centerY, radius))
        }
    }

    val cells_ = generateClosedPath().dropLast(1)   // the last cell is the same as the first
    val cells = cells_ + cells_

    val tape = tape(canvas, cells, circles)

    val paths = tape.flatMap { it.allPaths() }.sortedBy { it.size }.reversed()


    // Draw the longest path
    val layer1 = paths[3].drop(1).dropLast(1)  // the first line is root tree, not correct
    val p = Path()
    layer1.forEachIndexed { ndx, line ->
        if (ndx == 0) {
            p.moveTo(line.a.x, line.a.y)
            p.lineTo(line.b.x, line.b.y)
        } else {
            p.lineTo(line.b.x, line.b.y)
        }
        layer1.getOrNull(ndx + 1)?.let {
            // actual arc
            val cell = cells[(ndx + 3) % cells.size]
            val circle = circles[cell.first * 4 + cell.second]
            val circleRect = circle.rect()

            // produzi liniju za isto toliko
            val pointC = Point(line.b.x + (line.b.x - line.a.x), line.b.y + (line.b.y - line.a.y))
            //canvas.drawLine(line.a, pointC, strokeOfGreen(4f))

            // find the closest point to rect points
            val closest = circleRect.points().minByOrNull { it.distanceTo(pointC) }!!
            //canvas.drawPoint(closest, strokeOfGreen(12f))
            //canvas.drawLine(pointC, closest, strokeOfGreen(1f))

            // find the intersection between rectangle corner-center and circle
            val pointK = intersectionsOf(Line(closest, circle.center), circle).first()

            //canvas.drawRect(circleRect, strokeOfGreen(2f))

//            p.lineTo(it.a.x, it.a.y)
            p.lineTo(pointK.x, pointK.y)
            p.lineTo(it.a.x, it.a.y)
        }
    }
    p.closePath()

    canvas.drawPath(p, strokePaint2)

    // Draw all circles
    circles.forEach {
        val i = isCircleInPath(p, it)
        when (i) {
            IntersectionType.NONE -> canvas.drawCircle(it, if (rndb(3, 10)) accentPaint3 else strokePaint)
            IntersectionType.INTERSECT -> canvas.drawCircle(it, fillOfWhite())
            IntersectionType.CONTAIN -> canvas.drawCircle(it, strokePaint2)
        }
        //canvas.drawCircle(it, strokePaint)
    }
}

enum class IntersectionType {
    NONE, INTERSECT, CONTAIN
}

private fun isCircleInPath(path: Path, circle: Circle): IntersectionType {
    val cx = circle.center.x
    val cy = circle.center.y
    val radius = circle.radius

    val circlePath = Path().apply {
        addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
    }

    val circleRegion = circlePath.toRegion()

    // Check if they intersect
    val pathRegion = path.toRegion()
    val intersect = pathRegion.intersects(circleRegion)
    val contain = pathRegion.contains(circle.x.toInt(), circle.y.toInt())
    if (contain) {
        return IntersectionType.CONTAIN
    }
    if (intersect) {
        return IntersectionType.INTERSECT
    }
    return IntersectionType.NONE
}


