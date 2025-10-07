package dev.oblac.gart.sixsix

import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palette
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIOf
import org.jetbrains.skia.Rect


/** CELLS **/

internal val cells = listOf(
    ::cell1,
    ::cell2,
    ::cell3,
    ::cell4,
    ::cell5,
    ::cell6,
    ::cell7,
    ::cell8,
    ::cell9,
    ::cell10,
    ::cell11,
    ::cell12,
    ::cell13,
    ::cell14,
    ::cell15,
)

private const val RATIO = 1f / GOLDEN_RATIOf
private const val LW = 20f  // line width

private fun cell1(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    val pa = d.leftTop
    val pb = Point(d.cx, 0f)
    val pc = d.rightTop
    val pd = Point(d.wf, d.cy)
    val pe = d.rightBottom
    val pf = Point(d.cx, d.hf)
    val pg = d.leftBottom
    val ph = Point(0f, d.cy)

    c.drawTriangle(Triangle(pa, pc, pg), fillOf(p[0]))
    c.drawTriangle(Triangle(pa, pb, ph), fillOf(p[1]))
    c.drawTriangle(Triangle(pe, pc, pg), fillOf(p[2]))
    c.drawTriangle(Triangle(pe, pd, pf), fillOf(p[3]))
    c.drawTriangle(Triangle(pe, pd, pf), fillOf(p[3]))
}

private fun cell2(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // Draw two half-circles
    val centerX = d.cx
    val centerY = d.cy
    val radius = d.wf.coerceAtMost(d.hf * RATIO) / 2f

    c.save()
    c.clipRect(Rect.makeXYWH(0f, 0f, centerX, d.hf))
    c.drawCircle(centerX, centerY, radius, fillOf(p[1]))
    c.restore()

    c.save()
    c.clipRect(Rect.makeXYWH(centerX, 0f, centerX, d.hf))
    c.drawCircle(centerX, centerY, radius, fillOf(p[2]))
    c.restore()
}

private fun cell3(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // Draw two half-circles
    val centerX = d.cx
    val centerY = d.cy
    val radius = d.wf.coerceAtMost(d.hf * 0.25f)

    c.save()
    c.translate(0f, centerY)
    c.clipRect(Rect.makeXYWH(0f, 0f, centerX, d.hf))
    c.drawCircle(centerX, centerY, radius, fillOf(p[1]))
    c.restore()
    c.drawRect(Rect.makeXYWH(centerX, d.hf - radius, centerX, radius), fillOf(p[1]))

    c.save()
    c.translate(0f, -centerY)
    c.clipRect(Rect.makeXYWH(centerX, 0f, centerX, d.hf))
    c.drawCircle(centerX, centerY, radius, fillOf(p[2]))
    c.restore()
    c.drawRect(Rect.makeXYWH(0f, 0f, centerX, radius), fillOf(p[2]))
}

private fun cell4(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // Draw two half-circles
    val centerX = d.cx
    val centerY = d.cy
    val radius = d.wf.coerceAtMost(d.hf * RATIO) / 2f

    c.save()
    c.translate(0f, radius)
    c.clipRect(Rect.makeXYWH(0f, 0f, d.wf, centerY))
    c.drawCircle(centerX, centerY, radius, fillOf(p[1]))
    c.restore()

    c.save()
    c.translate(0f, -radius)
    c.clipRect(Rect.makeXYWH(0f, centerY, d.wf, d.hf))
    c.drawCircle(centerX, centerY, radius, fillOf(p[2]))
    c.restore()
}

private fun cell5(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])
    c.drawTriangle(Triangle(d.leftTop, d.rightTop, d.center), fillOf(p[1]))
    c.drawTriangle(Triangle(d.rightTop, d.rightBottom, d.center), fillOf(p[2]))
    c.drawTriangle(Triangle(d.leftBottom, d.rightBottom, d.center), fillOf(p[3]))
}

private fun cell6(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])
    c.drawCircle(d.leftBottom, d.hf, fillOf(p[1]))

    val pa = Point(d.w3x2, 0f)
    val pb = Point(d.w, d.h3)
    c.drawTriangle(Triangle(pa, pb, d.rightTop), fillOf(p[2]))
}

private fun cell7(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawCircle(d.center, RATIO * d.cx, fillOf(p[1]))
    c.drawCircle(d.center, (RATIO * d.cx) / 2, fillOf(p[2]))
}

private fun cell8(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val step = d.wf / 4f
    for (i in 1..3) {
        val x = i * step
        c.drawLine(Point(x, 0f), Point(x, d.hf), strokeOf(p[1], LW))
    }
    for (i in 1..3) {
        val y = i * step
        c.drawLine(Point(0f, y), Point(d.wf, y), strokeOf(p[2], LW))
    }
}

private fun cell9(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawCircle(d.center, RATIO * d.cx, strokeOf(p[1], LW))
}

private fun cell10(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val m = d.cx * RATIO + 20f
    equilateralTriangle(d.center, m, Degrees.ZERO).let {
        c.drawTriangle(it, fillOf(p[1]))
    }
    equilateralTriangle(d.center.offset(20f, 0f), m - 20f, Degrees.ZERO).let {
        c.drawTriangle(it, fillOf(p[2]))
    }

}

private fun cell11(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawTriangle(Triangle(d.leftBottom, d.rightTop, d.rightBottom), fillOf(p[1]))
    c.drawTriangle(Triangle(Point(d.wf / 4, d.h), Point(d.w, d.hf / 4), d.rightBottom), fillOf(p[2]))
    c.drawTriangle(Triangle(Point(d.wf / 2, d.h), Point(d.w, d.hf / 2), d.rightBottom), fillOf(p[3]))
    c.drawTriangle(Triangle(Point(d.wf * 0.75f, d.h), Point(d.w, d.hf * 0.75f), d.rightBottom), fillOf(p[0]))
}

private fun cell12(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val r1 = (d.cx / RATIO) / 4f
    c.drawCircle(Point(d.w3, d.h3), r1, fillOf(p[1]))
    val r2 = (d.cx / RATIO) / 6f
    c.drawCircle(Point(d.w3x2, d.h3x2), r2, fillOf(p[2]))
}

private fun cell13(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val size = d.wf.coerceAtMost(d.hf) * RATIO

    c.save()
    c.translate(d.cx, d.cy)
    c.rotate(45f)
    c.drawRect(Rect.makeXYWH(-size / 2, -size / 2, size, size), fillOf(p[1]))
    c.restore()

    val smallerSize = size * 0.6f
    c.save()
    c.translate(d.cx, d.cy)
    c.drawRect(Rect.makeXYWH(-smallerSize / 2, -smallerSize / 2, smallerSize, smallerSize), fillOf(p[2]))
    c.restore()
}

private fun cell14(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val radius = d.wf.coerceAtMost(d.hf) * RATIO / 2

    c.drawCircle(Point(d.cx - radius / 2, d.cy), radius, fillOf(p[3]))
    c.drawCircle(Point(d.cx + radius / 2, d.cy), radius, fillOf(p[1]))

    c.drawCircle(d.center, radius / 2, fillOf(p[2]))
}

private fun cell15(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // Draw four triangles pointing outward from center (cross/star pattern)
    val halfSize = d.wf * 0.5f

    // Top triangle
    c.drawTriangle(
        Triangle(
            Point(d.cx - halfSize, d.cy),
            Point(d.cx + halfSize, d.cy),
            Point(d.cx, d.cy - halfSize)
        ),
        fillOf(p[1])
    )

    // Right triangle
    c.drawTriangle(
        Triangle(
            Point(d.cx, d.cy - halfSize),
            Point(d.cx, d.cy + halfSize),
            Point(d.cx + halfSize, d.cy)
        ),
        fillOf(p[2])
    )

    // Bottom triangle
    c.drawTriangle(
        Triangle(
            Point(d.cx - halfSize, d.cy),
            Point(d.cx + halfSize, d.cy),
            Point(d.cx, d.cy + halfSize)
        ),
        fillOf(p[3])
    )

    // Left triangle
    c.drawTriangle(
        Triangle(
            Point(d.cx, d.cy - halfSize),
            Point(d.cx, d.cy + halfSize),
            Point(d.cx - halfSize, d.cy)
        ),
        fillOf(p[1])
    )

    // Center circle
    val size = d.wf.coerceAtMost(d.hf) * RATIO / 2
    c.drawCircle(d.center, size / 2, fillOf(p[2]))
}

