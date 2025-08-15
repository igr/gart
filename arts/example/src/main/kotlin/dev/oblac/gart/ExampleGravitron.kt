package dev.oblac.gart

import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.gravitron.Gravitron
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path

fun main() {
    val gart = Gart.of("ExampleGravitron", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val c = g.canvas

    draw(c, g.d)
    gart.window().showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.black)

    val g = Gravitron(
        x = 512f,
        y = 512f,
        radius = 400f,
        angle = Degrees.of(90f)
    )

    c.drawCircle(g.circle, strokeOfWhite(2f))

    // line: left to right, top to bottom
    Line(
        a = Point(0f, 500f),
        b = Point(d.cx, d.cy - 200f)
    ).let { line ->
        val p = Path().moveTo(line.a)
        g.applyTo(line, p)
            ?.let { c.drawLine(it, strokeOfRed(2f)) }
        c.drawPath(p, strokeOfRed(8f))
    }

    // line: right to left, top to bottom
    Line(
        a = Point(1024f, 500f),
        b = Point(d.cx, d.cy - 200f)
    ).let { line ->
        val p = Path().moveTo(line.a)
        g.applyTo(line, p)
            ?.let { c.drawLine(it, strokeOfBlue(2f)) }
        c.drawPath(p, strokeOfBlue(8f))
    }

    // line: left to right, bottom to top
    Line(
        a = Point(0f, 500f),
        b = Point(d.cx, d.cy + 200f)
    ).let { line ->
        val p = Path().moveTo(line.a)
        g.applyTo(line, p)
            ?.let { c.drawLine(it, strokeOfYellow(2f)) }
        c.drawPath(p, strokeOfYellow(8f))
    }

    // line: right to left, bottom to top
    Line(
        a = Point(1024f, 500f),
        b = Point(d.cx, d.cy + 200f)
    ).let { line ->
        val p = Path().moveTo(line.a)
        g.applyTo(line, p)
            ?.let { c.drawLine(it, strokeOfMagenta(2f)) }
        c.drawPath(p, strokeOfMagenta(8f))
    }
}
