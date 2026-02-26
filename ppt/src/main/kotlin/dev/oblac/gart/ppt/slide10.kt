package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.coral
import dev.oblac.gart.color.CssColors.deepSkyBlue
import dev.oblac.gart.color.CssColors.gold
import dev.oblac.gart.color.CssColors.hotPink
import dev.oblac.gart.color.CssColors.lime
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.color.toStrokePaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.splitToGrid
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import dev.oblac.gart.text.drawTextOnPath
import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.*

val slide10 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.020f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 40f, rect.right, rect.bottom - 5f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    c.clear(CssColors.darkCyan)
    c.drawTitle("Text Rendering")

    val grid = contentBox.shrink(20f).splitToGrid(2, 2)
    val tinyFont = font(FontFamily.IBMPlexMono, screen.height * 0.018f)
    val lineH = screen.height * 0.08f

    // 1. SkFont & basic drawing
    val g1 = grid[0].shrink(10f)
    //--- src: 1 SkFont
    val serif = font(FontFamily.Literata, screen.height * 0.05f)
    val sans = font(FontFamily.RethinkSans, screen.height * 0.05f)
    val mono = font(FontFamily.IBMPlexMono, screen.height * 0.04f)
    val cy1 = g1.top + g1.height * 0.2f
    c.drawString("Serif (Literata)", g1.left + 20f, cy1, serif, white.toFillPaint())
    c.drawString("Sans (Rethink)", g1.left + 20f, cy1 + lineH, sans, coral.toFillPaint())
    c.drawString("Mono (IBM Plex)", g1.left + 20f, cy1 + lineH * 2, mono, deepSkyBlue.toFillPaint())
    //--- crs: 1
    c.drawLabel(g1, "SkFont + Typeface")

    // 2. Glyph positioning & metrics
    val g3 = grid[1].shrink(10f)
    //--- src: 2 Glyph positioning
    val metricsFont = font(FontFamily.Literata, screen.height * 0.07f)
    val sampleText = "Hgpfy"
    val metrics = metricsFont.metrics
    val baseY = g3.top + g3.height * 0.5f
    val textX = g3.left + 30f
    //--- crs: 2

    // baseline
    val guideStroke = white.toStrokePaint(1.5f).apply {
        pathEffect = PathEffect.makeDash(floatArrayOf(8f, 6f), 0f)
    }
    c.drawLine(g3.left + 10f, baseY, g3.right - 10f, baseY, lime.toStrokePaint(2f))
    // ascent line
    c.drawLine(g3.left + 10f, baseY + metrics.ascent, g3.right - 10f, baseY + metrics.ascent, guideStroke)
    // descent line
    c.drawLine(g3.left + 10f, baseY + metrics.descent, g3.right - 10f, baseY + metrics.descent, guideStroke)

    c.drawString(sampleText, textX, baseY, metricsFont, white.toFillPaint())

    // small labels for metric lines
    c.drawString("baseline", g3.right - 160f, baseY - 5f, tinyFont, lime.toFillPaint())
    c.drawString("ascent", g3.right - 160f, baseY + metrics.ascent - 5f, tinyFont, hotPink.toFillPaint())
    c.drawString("descent", g3.right - 160f, baseY + metrics.descent + 18f, tinyFont, hotPink.toFillPaint())
    c.drawLabel(g3, "Glyph positioning")

    // 3. Paragraph — rich text with mixed styles
    val g4 = grid[2].shrink(10f)
    val tfp = TypefaceFontProvider()
    tfp.registerTypeface(font(FontFamily.RethinkSans, 12f).typeface, "Sans")
    tfp.registerTypeface(font(FontFamily.Literata, 12f).typeface, "Serif")
    tfp.registerTypeface(font(FontFamily.IBMPlexMonoBold, 12f).typeface, "Mono")
    val fc = FontCollection()
    fc.setDefaultFontManager(FontMgr.default)
    fc.setAssetFontManager(tfp)
    val ps = ParagraphStyle().apply {
        alignment = Alignment.LEFT
    }
    val fontSize = screen.height * 0.036f
    //--- src: 3 Paragraph
    val para = ParagraphBuilder(ps, fc).apply {
        pushStyle(TextStyle().setFontFamily("Sans")
            .setFontSize(fontSize).setColor(white))
        addText("Mixed ")
        popStyle()
        pushStyle(TextStyle().setFontFamily("Serif")
            .setFontSize(fontSize).setColor(coral))
        addText("styles ")
        popStyle()
        pushStyle(TextStyle().setFontFamily("Mono")
            .setFontSize(fontSize).setColor(deepSkyBlue))
        addText("in one\n")
        popStyle()
        pushStyle(TextStyle().setFontFamily("Sans")
            .setFontSize(fontSize).setColor(gold)
            .setDecorationStyle(DecorationStyle(
                true, false, false, true,
                gold, DecorationLineStyle.WAVY, 2f
            )))
        addText("paragraph!")
        popStyle()
    }.build()
    para.layout(g4.width - 40f)
    para.paint(c, g4.left + 20f, g4.top + 20f)
    //--- crs: 3
    c.drawLabel(g4, "Paragraph (rich text)")

    // 4. Text on path
    val g5 = grid[3].shrink(10f)
    //--- src: 4 Text on path
    val pathFont = font(FontFamily.SpaceMonoBold, screen.height * 0.030f)
    val arcPath = PathBuilder().apply {
        val cx = g5.left + g5.width / 2
        val cy = g5.top + g5.height / 2
        val rx = g5.width * 0.38f
        val ry = g5.height * 0.30f
        addArc(Rect(cx - rx, cy - ry, cx + rx, cy + ry), 200f, 280f)
    }.detach()
    c.drawTextOnPath(arcPath, "Text follows the path curvature!", pathFont, gold.toFillPaint())
    c.drawPath(arcPath, white.toStrokePaint(1f).apply {
        pathEffect = PathEffect.makeDash(floatArrayOf(4f, 4f), 0f)
    })
    //--- crs: 4
    c.drawLabel(g5, "Text on Path")
}
