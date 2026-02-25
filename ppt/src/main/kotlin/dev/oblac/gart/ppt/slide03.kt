package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.deepSkyBlue
import dev.oblac.gart.color.CssColors.gold
import dev.oblac.gart.color.CssColors.hotPink
import dev.oblac.gart.color.CssColors.mediumSeaGreen
import dev.oblac.gart.color.CssColors.orchid
import dev.oblac.gart.color.CssColors.tomato
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.color.toStrokePaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.*
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.*
import org.jetbrains.skia.PathEffect.Companion.makeDash

val slide03 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.024f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 50f, rect.right, rect.bottom - 10f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }


    c.clear(CssColors.darkSlateBlue)
    c.drawTitle("Canvas")

    val grid = contentBox.shrink(20f).splitToGrid(4, 2)

    /*** 1. Circle ***/
    val g1 = grid[0].shrink(10f)
    //--- src: 1 Draw Circle
    val fillColor = paint().apply {
        color = CssColors.coral
    }
    val strokeColor = paint().apply {
        color = white
        mode = PaintMode.STROKE
        strokeWidth = 10f
    }
    c.drawCircle(g1.center(), g1.height / 3, fillColor)
    c.drawCircle(g1.center(), g1.height / 3, strokeColor)
    //--- crs: 1
    c.drawLabel(g1, "drawCircle")

    // 2. Rectangle
    val g2 = grid[1].shrink(10f)
    //--- src: 2 Draw Rectangle
    val rw = g2.width * 0.5f
    val rh = g2.height * 0.4f
    val rx = g2.left + (g2.width - rw) / 2
    val ry = g2.top + (g2.height - rh) / 2
    c.drawRectWH(rx, ry, rw, rh, mediumSeaGreen.toFillPaint())
    c.drawRectWH(rx, ry, rw, rh, white.toStrokePaint(10f))
    //--- crs: 2
    c.drawLabel(g2, "drawRect")

    // 3. Triangle
    val g3 = grid[2].shrink(10f)
    val tmx = g3.left + g3.width / 2
    val tri = Triangle(
        Point(tmx, g3.top + g3.height * 0.15f),
        Point(tmx - g3.width * 0.25f, g3.top + g3.height * 0.85f),
        Point(tmx + g3.width * 0.25f, g3.top + g3.height * 0.85f)
    )
    c.drawTriangle(tri, gold.toFillPaint())
    c.drawTriangle(tri, white.toStrokePaint(10f))
    c.drawLabel(g3, "drawTriangle")

    // 4. Lines
    val g4 = grid[3].shrink(10f)
    val lx = g4.left + g4.width * 0.2f
    val lxe = g4.right - g4.width * 0.2f
    val ly = g4.top + g4.height * 0.2f
    val lye = g4.bottom - g4.height * 0.2f
    //--- src: 4 Draw Lines
    c.drawLine(
        Point(lx, ly), Point(lxe, lye),
        deepSkyBlue.toStrokePaint(10f).apply {
            pathEffect = PathEffect.makeDiscrete(0.25f, 1f, 123)
        }
    )
    c.drawLine(
        Point(lxe, ly), Point(lx, lye),
        hotPink.toStrokePaint(10f).apply {
            pathEffect = makeDash(floatArrayOf(20f, 10f), 0f)
        }
    )
    c.drawLine(
        Point(g4.left + g4.width / 2, ly), Point(g4.left + g4.width / 2, lye),
        white.toStrokePaint(10f)
    )
    //--- crs: 4
    c.drawLabel(g4, "drawLine")

    // 5. Arc
    val g5 = grid[4].shrink(10f)
    //--- src: 5 Draw Arc
    val arcSize = g5.height * 0.35f
    val arcRect = Rect.ofCenter(g5.center(), arcSize * 2, arcSize * 2)
    c.drawArc(arcRect, -180f, 230f, true, tomato.toFillPaint())
    c.drawArc(arcRect, -180f, 230f, true, white.toStrokePaint(10f))
    //--- crs: 5
    c.drawLabel(g5, "drawArc")

    // 6. Path
    val g6 = grid[5].shrink(10f)
    val px = g6.left + g6.width * 0.15f
    val pxe = g6.right - g6.width * 0.15f
    val py = g6.top + g6.height * 0.5f
    //--- src: 6 Draw Path
    val curvePath = PathBuilder()
        .moveTo(px, py)
        .cubicTo(
            px + g6.width * 0.2f, g6.top + g6.height * 0.1f,
            pxe - g6.width * 0.2f, g6.top + g6.height * 0.9f,
            pxe, py
        )
        .detach()
    c.drawPath(curvePath, orchid.toStrokePaint(10f))
    //--- crs: 6
    c.drawLabel(g6, "drawPath")

    // 7. Points
    val g7 = grid[6].shrink(10f)
    val pts = listOf(
        Point(g7.left + g7.width * 0.5f, g7.top + g7.height * 0.2f),
        Point(g7.left + g7.width * 0.25f, g7.top + g7.height * 0.5f),
        Point(g7.left + g7.width * 0.75f, g7.top + g7.height * 0.5f),
        Point(g7.left + g7.width * 0.3f, g7.top + g7.height * 0.8f),
        Point(g7.left + g7.width * 0.7f, g7.top + g7.height * 0.8f),
    )
    //--- src: 7 Draw Path
    val angle = f.timeSeconds * 10
    val center = g7.center()
    c.save()
    c.rotate(angle, center.x, center.y)
    c.drawPointsAsCircles(pts, CssColors.deepPink.toFillPaint(), 40f)
    c.drawPointsAsCircles(pts, white.toStrokePaint(4f), 40f)
    c.restore()
    //--- crs: 7
    c.drawLabel(g7, "drawPoints")

    // 8. Poly4
    val g8 = grid[7].shrink(10f)
    val poly = Poly4(
        Point(g8.left + g8.width * 0.3f, g8.top + g8.height * 0.15f),
        Point(g8.left + g8.width * 0.8f, g8.top + g8.height * 0.25f),
        Point(g8.left + g8.width * 0.7f, g8.top + g8.height * 0.85f),
        Point(g8.left + g8.width * 0.15f, g8.top + g8.height * 0.7f),
    )
    c.drawPoly4(poly, CssColors.darkTurquoise.toFillPaint())
    c.drawPoly4(poly, white.toStrokePaint(10f))
    c.drawLabel(g8, "drawPoly4")
}
