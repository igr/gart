package dev.oblac.gart.sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("sf1", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)
    gart.saveImage(g)
    w.showImage(g)
}

private val colorBack = RetroColors.black01
private val colorLine = RetroColors.white01
private val colorSun = RetroColors.red01

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)

    // diagonal lines starting from the right corner of the image
    val paths = mutableListOf<Line>()
    var gap = 110f
    val steps = 24
    for (i in 0..steps) {
        val pointBottom = Point(d.wf - i * gap, d.hf)
        val pointRight = Point(d.wf, d.hf - i * gap * 0.5f) // angle

        val line = Line(pointRight, pointBottom)
        val longerLine = line.shortenLen(-50f)

        paths.add(longerLine)

        gap -= 1.5f + sin(i * 0.1f)
    }

    val revs = paths.reversed()
    revs.forEachIndexed { index, line ->

        if (index < revs.size - 1) {
            val strokeWidth = 20.1f - cos(index * 0.1f) * 20f
            // drawing the next line
            val nextLine = revs[index + 1]

            // draw path first
            val path = Path()
            path.moveTo(nextLine.a)
            path.lineTo(nextLine.b)
            path.lineTo(d.wf, d.hf)
            path.closePath()
            c.drawPath(path, fillOf(colorBack))

            // draw the line
            c.drawLine(nextLine, strokeOf(colorLine, strokeWidth))
        }
        if (index == steps - 7) {
            c.drawCircle(0f, d.hf + 100f, 700f, fillOf(colorSun))
        }
    }

    val moonRadius = 40f
    val moonDelta = 10f
    val moonX = d.w3 * 2
    val moonY = d.h3 / 2
    c.drawCircle(moonX, moonY, moonRadius, fillOf(colorLine))
    c.drawCircle(moonX - moonDelta, moonY + moonDelta, moonRadius, fillOf(colorBack))

    c.drawRoundBorder(d, 10f, 44f, colorLine)
}
