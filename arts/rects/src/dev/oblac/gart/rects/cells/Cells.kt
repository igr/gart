package dev.oblac.gart.rects.cells

import dev.oblac.gart.Gart
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.closedPathOf
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.grow
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import java.util.stream.IntStream.range
import kotlin.math.cos
import kotlin.math.sin

val gart = Gart.of(
    "cells",
    1024, 1024, 1
)

fun main() {
    println(gart)
    val g = gart.gartvas()
    g.canvas.clear(CssColors.black)

    val rectW = 64f
    val xCount = (gart.d.rect.width / rectW).toInt() + 1
    val yCount = (gart.d.rect.height / rectW).toInt() + 1

    range(0, xCount).forEach { x ->
        range(0, yCount).forEach { y ->
            drawRect1(g.canvas, x * rectW, y * rectW, rectW + 1)
        }
    }
    range(0, xCount).forEach { x ->
        range(0, yCount).forEach { y ->
            drawRect2(g.canvas, x * rectW, y * rectW, rectW + 1)
        }
    }
    //g.canvas.drawBorder(g.d, rectW / 2, Colors.warmBlack1)
    gart.window().showImage(g)
    gart.saveImage(g)
}

val palette = Palettes.cool14

private fun drawRect1(canvas: Canvas, x: Float, y: Float, r: Float) {
    val backRect = Rect(x, y, x + r, y + r).grow(10f)
    val pb = palette.safe((sin(x / 10) * 7 + cos(y / 40) * 6).toInt())
    canvas.drawRect(backRect, fillOf(pb))

    // inner rect2
    val delta = rndf(-10f, 10f)
    canvas.rotate(delta, x, y)

    val s = rndf(6, 10)
    val rect = Rect(
        x + rndf(-s, s), y + rndf(-s, s), x + r + rndf(-s, s), y + r + rndf(-s, s)
    )
    val p = palette.safe((sin(x / 30) * 6 + cos(y / 10) * 6).toInt())
    canvas.drawRect(rect, fillOf(p))
    canvas.rotate(-delta, x, y)
}

private fun drawRect2(canvas: Canvas, x: Float, y: Float, r: Float) {
    if (rndb(2, 3)) {
        val triangle = if (rndb())
            closedPathOf(
                Point(x, y),
                Point(x + r, y + r),
                Point(x + r, y),
            )
        else {
            closedPathOf(
                Point(x + r, y),
                Point(x, y + r),
                Point(x, y),
            )
        }
        val p2 = palette.safe((sin(10 + x / 20) * 3 + cos(20 + y / 10) * 2).toInt())
        val delta = rndf(-10f, 10f)
        canvas.rotate(delta, x, y)
        canvas.drawPath(triangle, fillOf(p2).also { it.colorFilter = ColorFilter.luma })
        canvas.rotate(-delta, x, y)
    }
}

