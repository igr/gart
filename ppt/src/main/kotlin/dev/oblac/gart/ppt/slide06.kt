package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.splitToGrid
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

val slide06 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.024f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 50f, rect.right, rect.bottom - 10f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    fun Canvas.drawBlendDemo(rect: Rect, mode: BlendMode) {
        val cx = rect.left + rect.width / 2
        val cy = rect.top + rect.height / 2
        val radius = rect.height * 0.28f
        val offset = radius * 0.45f

        // red circle (base)
        drawCircle(cx - offset, cy, radius, paint().apply {
            color = 0xFFE04040.toInt()
        })
        // green circle with blend mode
        drawCircle(cx + offset, cy - offset, radius, paint().apply {
            color = 0xFF40C040.toInt()
            blendMode = mode
        })
        // blue circle with blend mode
        drawCircle(cx, cy + offset, radius, paint().apply {
            color = 0xFF4080E0.toInt()
            blendMode = mode
        })
    }

    c.clear(CssColors.darkCyan)
    c.drawTitle("Blend Modes")

    val grid = contentBox.shrink(20f).splitToGrid(4, 2)

    // 1. SRC_OVER (default)
    val g1 = grid[0].shrink(10f)
    //--- src: 1 SRC_OVER
    c.drawBlendDemo(g1, BlendMode.SRC_OVER)
    //--- crs: 1
    c.drawLabel(g1, "SRC_OVER")

    // 2. MULTIPLY
    val g2 = grid[1].shrink(10f)
    //--- src: 2 MULTIPLY
    c.drawBlendDemo(g2, BlendMode.MULTIPLY)
    //--- crs: 2
    c.drawLabel(g2, "MULTIPLY")

    // 3. SCREEN
    val g3 = grid[2].shrink(10f)
    //--- src: 3 SCREEN
    c.drawBlendDemo(g3, BlendMode.SCREEN)
    //--- crs: 3
    c.drawLabel(g3, "SCREEN")

    // 4. OVERLAY
    val g4 = grid[3].shrink(10f)
    c.drawBlendDemo(g4, BlendMode.OVERLAY)
    c.drawLabel(g4, "OVERLAY")

    // 5. DARKEN
    val g5 = grid[4].shrink(10f)
    c.drawBlendDemo(g5, BlendMode.DARKEN)
    c.drawLabel(g5, "DARKEN")

    // 6. LIGHTEN
    val g6 = grid[5].shrink(10f)
    c.drawBlendDemo(g6, BlendMode.LIGHTEN)
    c.drawLabel(g6, "LIGHTEN")

    // 7. DIFFERENCE
    val g7 = grid[6].shrink(10f)
    c.drawBlendDemo(g7, BlendMode.DIFFERENCE)
    c.drawLabel(g7, "DIFFERENCE")

    // 8. EXCLUSION
    val g8 = grid[7].shrink(10f)
    c.drawBlendDemo(g8, BlendMode.EXCLUSION)
    c.drawLabel(g8, "EXCLUSION")
}
