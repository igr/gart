package dev.oblac.gart.lines.tapes

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import org.jetbrains.skiko.currentNanoTime

fun main() {
    val gart = Gart.of("tapesB", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDrawB(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDrawB(g: Gartvas) : Drawing(g) {
    init {
        drawB(g.canvas, g.d)
    }
}
private fun drawB(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)
    generateOverlappingClosedPath(7, radius = 400f, centerX = 512f, centerY = 512f - 200f).let { lines ->
        linesToRects(lines, tapeWidth = 60f)
    }.forEach {
        c.save()
        c.clipPath(it.path)
        drawGrungePoly4(c, it, RetroColors.white01)
        c.restore()
    }
    generateOverlappingClosedPath(8, radius = 400f, centerX = 512f, centerY = 512f + 200f).let { lines ->
        linesToRects(lines, tapeWidth = 60f)
    }.forEach {
        c.save()
        c.clipPath(it.path)
        drawGrungePoly4(c, it, RetroColors.red01)
        c.restore()
    }
}

private fun linesToRects(lines: List<Line>, tapeWidth: Float): List<Poly4> {
    if (lines.size < 2) return emptyList()

    val perpLines = mutableListOf<Line>()

    // Iterate through all pairs of consecutive lines
    for (i in 0 until lines.size) {
        val line1 = lines[i]
        val line2 = lines[(i + 1) % lines.size] // Wrap around for closed path

        // Find line between two lines
        val dLineBetween = lineBetweenTwoLines(line1, line2)

        // Rotate DLine for 90 degrees (get perpendicular)
        val rotatedDLine = dLineBetween.perpendicularDLine()

        // Create line from DLine at point line1.b with tapeWidth
        val pline1 = rotatedDLine.toLine(line1.b, tapeWidth)
        val pline2 = rotatedDLine.toLine(line1.b, -tapeWidth)
        perpLines.add(Line(pline1.b, pline2.b))
    }

    // Now construct rectangles from all such new lines by connecting pairs
    val rectangles = mutableListOf<Poly4>()
    for (i in 0 until perpLines.size) {
        val currentLine = perpLines[i]
        val nextLine = perpLines[(i + 1) % perpLines.size] // Wrap around

        // Create rectangle from the four corner points
        var rect = Poly4(currentLine.a, currentLine.b, nextLine.a, nextLine.b)
        if (intersectionOf(Line(rect.b, rect.c), Line(rect.d, rect.a)) != null) {
            rect = Poly4(currentLine.a, currentLine.b, nextLine.b, nextLine.a)
        }
        rectangles.add(rect)
    }

    return rectangles
}

private fun lineBetweenTwoLines(line1: Line, line2: Line): DLine {
    val angle = line1.angleTo(line2) / 2f + line1.angle()
    return DLine.of(line1.b, angle + Radians.PI_HALF)
}

private fun drawGrungePoly4(c: Canvas, poly: Poly4, color: Int) {
    val points = poly.path.toPoints(1000)
    repeat(2000) {
        val p1 = points.random()
        val p2 = points.random()
        c.drawLine(
            p1.x, p1.y, p2.x, p2.y,
            strokeOf(color, 0.3f).apply {
                this.alpha = 150
                this.pathEffect = PathEffect.makeDiscrete(10f, 10f, currentNanoTime().toInt())
            }
        )
    }
    c.drawPoly4(poly, strokeOf(color, 0.3f))
}
