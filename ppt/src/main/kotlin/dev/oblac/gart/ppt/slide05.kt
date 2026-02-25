package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.coral
import dev.oblac.gart.color.CssColors.deepSkyBlue
import dev.oblac.gart.color.CssColors.gold
import dev.oblac.gart.color.CssColors.mediumSeaGreen
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.color.toStrokePaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.multiply
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.Rect

val slide05 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.024f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 50f, rect.right, rect.bottom - 10f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    // Draw a small house shape as the reference object
    fun Canvas.drawHouse(cx: Float, cy: Float, size: Float, paint: org.jetbrains.skia.Paint) {
        val half = size / 2
        // body
        drawRect(Rect(cx - half, cy - half * 0.3f, cx + half, cy + half), paint)
        // roof
        val roof = org.jetbrains.skia.PathBuilder()
            .moveTo(cx - half * 1.2f, cy - half * 0.3f)
            .lineTo(cx, cy - half * 1.2f)
            .lineTo(cx + half * 1.2f, cy - half * 0.3f)
            .closePath()
            .detach()
        drawPath(roof, paint)
    }

    c.clear(CssColors.midnightBlue)
    c.drawTitle("Transforms")

    val grid = contentBox.shrink(20f).splitToGrid(3, 2)
    val houseSize = grid[0].height * 0.18f

    // 1. Translate
    val g1 = grid[0].shrink(10f)
    //--- src: 1 translate
    val center1 = g1.center()
    // original (ghost)
    c.drawHouse(center1.x, center1.y, houseSize, white.toStrokePaint(2f))
    // translated
    c.save()
    c.translate(g1.width * 0.2f, -g1.height * 0.15f)
    c.drawHouse(center1.x, center1.y, houseSize, coral.toFillPaint())
    c.drawHouse(center1.x, center1.y, houseSize, white.toStrokePaint(3f))
    c.restore()
    //--- crs: 1
    c.drawLabel(g1, "translate")

    // 2. Scale
    val g2 = grid[1].shrink(10f)
    //--- src: 2 scale
    val center2 = g2.center()
    c.drawHouse(center2.x, center2.y, houseSize, white.toStrokePaint(2f))
    c.save()
    c.translate(center2.x, center2.y)
    c.scale(1.5f, 1.5f)
    c.translate(-center2.x, -center2.y)
    c.drawHouse(center2.x, center2.y, houseSize, mediumSeaGreen.toFillPaint())
    c.drawHouse(center2.x, center2.y, houseSize, white.toStrokePaint(3f))
    c.restore()
    //--- crs: 2
    c.drawLabel(g2, "scale")

    // 3. Rotate
    val g3 = grid[2].shrink(10f)
    //--- src: 3 rotate
    val center3 = g3.center()
    c.drawHouse(center3.x, center3.y, houseSize, white.toStrokePaint(2f))
    c.save()
    c.rotate(25f, center3.x, center3.y)
    c.drawHouse(center3.x, center3.y, houseSize, gold.toFillPaint())
    c.drawHouse(center3.x, center3.y, houseSize, white.toStrokePaint(3f))
    c.restore()
    //--- crs: 3
    c.drawLabel(g3, "rotate")

    // 4. Skew
    val g4 = grid[3].shrink(10f)
    //--- src: 4 skew
    val center4 = g4.center()
    c.drawHouse(center4.x, center4.y, houseSize, white.toStrokePaint(2f))
    c.save()
    c.translate(center4.x, center4.y)
    c.skew(-0.3f, 0f)
    c.translate(-center4.x, -center4.y)
    c.drawHouse(center4.x, center4.y, houseSize, deepSkyBlue.toFillPaint())
    c.drawHouse(center4.x, center4.y, houseSize, white.toStrokePaint(3f))
    c.restore()
    //--- crs: 4
    c.drawLabel(g4, "skew")

    // 5. Matrix (combined rotate + scale)
    val g5 = grid[4].shrink(10f)
    //--- src: 5 Matrix33
    val center5 = g5.center()
    c.drawHouse(center5.x, center5.y, houseSize, white.toStrokePaint(2f))
    val toOrigin = Matrix33.makeTranslate(-center5.x, -center5.y)
    val fromOrigin = Matrix33.makeTranslate(center5.x, center5.y)
    val rot = Matrix33.makeRotate(15f)
    val scl = Matrix33.makeScale(1.3f, 0.7f)
    val matrix = Matrix33.multiply(
        fromOrigin,
        Matrix33.multiply(rot, Matrix33.multiply(scl, toOrigin))
    )
    c.save()
    c.concat(matrix)
    c.drawHouse(center5.x, center5.y, houseSize, CssColors.orchid.toFillPaint())
    c.drawHouse(center5.x, center5.y, houseSize, white.toStrokePaint(3f))
    c.restore()
    //--- crs: 5
    c.drawLabel(g5, "Matrix33")

    // 6. Animated transform
    val g6 = grid[5].shrink(10f)
    val center6 = g6.center()
    val angle = f.timeSeconds * 30
    c.save()
    c.rotate(angle, center6.x, center6.y)
    c.drawHouse(center6.x, center6.y, houseSize, CssColors.tomato.toFillPaint())
    c.drawHouse(center6.x, center6.y, houseSize, white.toStrokePaint(3f))
    c.restore()
    c.drawLabel(g6, "animated")
}
