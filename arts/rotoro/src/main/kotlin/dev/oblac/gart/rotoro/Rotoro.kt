package dev.oblac.gart.rotoro

import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.circleTangents
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.*

fun main() {
    val width = 1024
    val gart = Gart.of("rotoro", width, width)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, width)

    gart.saveImage(g)
    gart.window().showImage(g)
}

val background = NipponColors.col234_GOFUN
val whitePaint = fillOfWhite()
val blackPaint = fillOfBlack()

val accentPaint = fillOf(NipponColors.col099_TAMAGO)
//val accentPaint = fillOf(NipponColors.col176_MIZU)
//val accentPaint = fillOf(NipponColors.col011_NAKABENI)

val strokePaint = Paint().apply {
    color = Color.BLACK
    mode = PaintMode.STROKE
    strokeWidth = 20f
    strokeCap = PaintStrokeCap.ROUND
}

fun draw(canvas: Canvas, canvasWidth: Int) {
    canvas.clear(background)

    // Calculate dimensions based on canvas size
    val cellSize = canvasWidth / 4f
    val radius = cellSize * 0.375f  // Circle radius relative to cell size

    val circles = mutableListOf<Circle>()

    // Draw all circles
    for (row in 0..3) {
        for (col in 0..3) {
            val centerX = col * cellSize + cellSize / 2
            val centerY = row * cellSize + cellSize / 2

            val circle = Circle(centerX, centerY, radius)
            circles.add(circle)

            // Draw filled circle with appropriate color
            val fillPaint = when (rndi(11)) {
                in 0..6 -> whitePaint
                in 7..8 -> blackPaint
                else -> accentPaint
            }

            canvas.drawCircle(circle, fillPaint)

            // Draw a circle border
            canvas.drawCircle(circle, strokePaint)
        }
    }

    val cells_ = generateClosedPath().dropLast(1)   // the last cell is the same as the first
    val cells = cells_ + cells_

    val tape = tape(canvas, cells, circles)

    val paths = tape.flatMap { it.allPaths() }.sortedBy { it.size }.reversed()

    // Draw the longest path
    paths.first().drop(1).apply {   // the first line is root tree, not correct
        forEach { canvas.drawLine(it, strokePaint) }
    }

    // draw random path
//    paths.filter { it.size > 10 }.random().drop(1).apply {
//        forEach { canvas.drawLine(it, strokePaint) }
//    }
}

fun tape(canvas: Canvas, cells: List<Cell>, circles: List<Circle>): List<TreeNode<Line>> {
    val cell = cells[1]
    val circle = circles[cell.first * 4 + cell.second]
    val previousCell = cells[0]
    val previousCircle = circles[previousCell.first * 4 + previousCell.second]

    val tIns = circleTangents(previousCircle, circle).map { it.toLine().pointACloserTo(previousCircle) }
    val trees = tIns.map { TreeNode.root(it) }

    trees.forEach { treeNode ->
        tape(canvas, cells, circles, 2, treeNode)
    }

    return trees
}

fun tape(canvas: Canvas, cells: List<Cell>, circles: List<Circle>, index: Int, treeNodeIn: TreeNode<Line>) {
    if (index == 15) {
        return
    }
    val cell = cells[index % cells.size]
    val circle = circles[cell.first * 4 + cell.second]
    val nextCell = cells[(index + 1) % cells.size]
    val nextCircle = circles[nextCell.first * 4 + nextCell.second]
    val previousCell = cells[(index - 1 + cells.size) % cells.size]
    val previousCircle = circles[previousCell.first * 4 + previousCell.second]

    val tOuts = circleTangents(circle, nextCircle).map { it.toLine().pointACloserTo(circle) }
    val nextLines = tape2(treeNodeIn.value, tOuts, previousCircle, circle, nextCircle)

    nextLines.forEach { line ->
        val treeNodeOut = treeNodeIn.add(line)
        tape(canvas, cells, circles, index + 1, treeNodeOut)
    }
}

fun tape2(tIn: Line, tOuts: List<Line>, previousCircle: Circle, circle: Circle, nextCircle: Circle): List<Line> {
    // there are 2 cases
    if (collinear(previousCircle.center, circle.center, nextCircle.center)) {
        // 1) when prev, current and next circles are aligned
        // continue with two closest next tangents
        val sorted = tOuts.sortedBy { tIn.b.distanceTo(it.a) }
        require(sorted.size == 4)
        return listOf(sorted[0], sorted[1])

    } else {
        // 2) when prev, current and next circles are not aligned and form a triangle
        val triangle = Triangle(previousCircle.center, circle.center, nextCircle.center)
        if (triangle.contains(tIn.b)) {
            return emptyList()
        }
        return tOuts.filter { !triangle.contains(it.a) }
    }
}

// returns a line that has A point closer to the circle
private fun Line.pointACloserTo(circle: Circle): Line {
    return if (a.distanceTo(circle.center) < b.distanceTo(circle.center)) {
        this
    } else {
        Line(b, a)
    }
}
