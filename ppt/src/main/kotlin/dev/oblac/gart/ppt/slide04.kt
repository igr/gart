package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.coral
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
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

val slide04 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.024f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 50f, rect.right, rect.bottom - 10f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    fun Canvas.drawColorfulContent(rect: Rect) {
        val colors = listOf(coral, mediumSeaGreen, gold, deepSkyBlue, orchid, tomato)
        val stripeW = rect.width / colors.size
        for ((i, color) in colors.withIndex()) {
            drawRect(Rect(rect.left + i * stripeW, rect.top, rect.left + (i + 1) * stripeW, rect.bottom), color.toFillPaint())
        }
        val cx = rect.left + rect.width / 2
        val cy = rect.top + rect.height / 2
        drawCircle(cx, cy, rect.height * 0.29f, hotPink.toFillPaint())
    }

    c.clear(CssColors.darkOliveGreen)
    c.drawTitle("Clipping")

    val grid = contentBox.shrink(20f).splitToGrid(3, 2)

    // 1. clipRect
    val g1 = grid[0].shrink(10f)
    //--- src: 1 clipRect
    val clipR = Rect.ofCenter(g1.center(), g1.width * 0.5f, g1.height * 0.5f)
    c.save()
    c.clipRect(clipR)
    c.drawColorfulContent(g1)
    c.restore()
    c.drawRect(clipR, white.toStrokePaint(3f))
    //--- crs: 1
    c.drawLabel(g1, "clipRect")

    // 2. clipRRect
    val g2 = grid[1].shrink(10f)
    val clipRR = Rect.ofCenter(g2.center(), g2.width * 0.5f, g2.height * 0.5f).toRRect(30f)
    c.save()
    c.clipRRect(clipRR, true)
    c.drawColorfulContent(g2)
    c.restore()
    c.drawRRect(clipRR, white.toStrokePaint(3f))
    c.drawLabel(g2, "clipRRect")

    // 3. clipPath (circle)
    val g3 = grid[2].shrink(10f)
    val circlePath = Circle(g3.center(), g3.height * 0.35f).toPath()
    c.save()
    c.clipPath(circlePath)
    c.drawColorfulContent(g3)
    c.restore()
    c.drawPath(circlePath, white.toStrokePaint(3f))
    c.drawLabel(g3, "clipPath")

    // 4. clipPath (star)
    val g4 = grid[3].shrink(10f)
    val starPath = PathBuilder().apply {
        val cx = g4.center().x
        val cy = g4.center().y
        val outerR = g4.height * 0.38f
        val innerR = outerR * 0.4f
        for (i in 0 until 10) {
            val angle = toRadians((i * 36 - 90).toDouble())
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        closePath()
    }.detach()
    //--- src: 4 clipPath (star)
    c.save()
    c.clipPath(starPath)
    c.drawColorfulContent(g4)
    c.restore()
    c.drawPath(starPath, white.toStrokePaint(3f))
    //--- crs: 4
    c.drawLabel(g4, "clipPath (star)")

    // 5. ClipMode.DIFFERENCE
    val g5 = grid[4].shrink(10f)
    //--- src: 5 ClipMode.DIFFERENCE
    val diffCircle = Circle(g5.center(), g5.height * 0.3f).toPath()
    c.save()
    c.clipPath(diffCircle, ClipMode.DIFFERENCE)
    c.drawColorfulContent(g5.shrink(80f))
    c.restore()
    c.drawPath(diffCircle, white.toStrokePaint(3f))
    //--- crs: 5
    c.drawLabel(g5, "DIFFERENCE")

    // 6. Save/Restore with nested clips
    val g6 = grid[5].shrink(10f)
    //--- src: 6 Nested clips
    val outerRect = Rect.ofCenter(g6.center(), g6.width * 0.6f, g6.height * 0.6f)
    val innerCircle = Circle(g6.center(), g6.height * 0.22f).toPath()
    c.save()
    c.clipRect(outerRect)
    c.drawColorfulContent(g6)
    c.save()
    c.clipPath(innerCircle, ClipMode.DIFFERENCE)
    c.drawRect(outerRect, CssColors.darkOliveGreen.toFillPaint())
    c.restore()
    c.restore()
    c.drawRect(outerRect, white.toStrokePaint(3f))
    c.drawPath(innerCircle, white.toStrokePaint(3f))
    //--- crs: 6
    c.drawLabel(g6, "Nested clips")
}
