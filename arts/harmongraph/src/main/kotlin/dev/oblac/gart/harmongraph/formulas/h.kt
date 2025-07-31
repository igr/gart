package dev.oblac.gart.harmongraph.formulas

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

internal fun drawHarmonographLine(c: Canvas, d: Dimension, colorFront: Int, colorBold: Int, alpha: Int): (Pair<Point, Point>) -> Unit = { (a, b) ->
    val colorF = strokeOf(colorFront, 1f).apply { this.alpha = alpha }
    val colorB = strokeOf(colorBold, 1f).apply { this.alpha = alpha }
    val diagonal = Line(d.leftTop, d.rightBottom)

    if (a.x < a.y && b.x > b.y) {
        val intersection = intersectionOf(Line(a, b), diagonal)
        if (intersection == null) {
            c.drawLine(a, b, colorF)
        } else {
            c.drawLine(a, intersection, colorB)
            c.drawLine(intersection, b, colorF)
        }
    } else if (a.x > a.y && b.x < b.y) {
        val intersection = intersectionOf(Line(a, b), diagonal)
        if (intersection == null) {
            c.drawLine(a, b, colorB)
        } else {
            c.drawLine(a, intersection, colorF)
            c.drawLine(intersection, b, colorB)
        }
    } else if (a.x > a.y) {
        c.drawLine(a, b, colorF)
    } else {
        c.drawLine(b, a, colorB)
    }
}

internal fun drawHarmonographLineSimple(c: Canvas, d: Dimension, colorFront: Int, colorBold: Int, alpha: Int): (Pair<Point, Point>) -> Unit = { (a, b) ->
    val colorF = strokeOf(colorFront, 1f).apply { this.alpha = alpha }
    val colorB = strokeOf(colorBold, 1f).apply { this.alpha = alpha }

    if (a.x > a.y) {
        c.drawLine(a, b, colorF)
    } else {
        c.drawLine(b, a, colorB)
    }
}

internal fun drawTriangles(c: Canvas, d: Dimension, colorBack: Int, colorFront: Int) {
    val backTriangle1 = Triangle(
        d.leftTop,
        d.rightTop,
        d.rightBottom
    )
    val backTriangle2 = Triangle(
        d.leftTop,
        d.leftBottom,
        d.rightBottom
    )
    c.drawTriangle(backTriangle1, fillOf(colorBack))
    c.drawTriangle(backTriangle2, fillOf(colorFront))
}
