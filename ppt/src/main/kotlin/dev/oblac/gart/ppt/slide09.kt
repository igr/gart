package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.ColorMatrices
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.coral
import dev.oblac.gart.color.CssColors.deepSkyBlue
import dev.oblac.gart.color.CssColors.gold
import dev.oblac.gart.color.CssColors.hotPink
import dev.oblac.gart.color.CssColors.mediumSeaGreen
import dev.oblac.gart.color.CssColors.saddleBrown
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.toColorFilter
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.splitToGrid
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.*

val slide09 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.020f)
    val labelPaint = white.toFillPaint()
    val headerFont = font(FontFamily.RethinkSans, screen.height * 0.032f)

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 40f, rect.right, rect.bottom - 5f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    fun Canvas.drawHeader(rect: Rect, text: String) {
        drawStringInRect(text, rect, headerFont, white.toFillPaint(), HorizontalAlign.CENTER)
    }

    // -- Scene drawing for Image Filters & Color Filters columns --
    fun Canvas.drawScene(rect: Rect) {
        val cx = rect.left + rect.width / 2
        val cy = rect.top + rect.height / 2 - 10f
        val r = rect.height * 0.18f
        drawCircle(cx - r * 0.6f, cy - r * 0.3f, r, coral.toFillPaint())
        drawCircle(cx + r * 0.6f, cy - r * 0.3f, r, mediumSeaGreen.toFillPaint())
        drawCircle(cx, cy + r * 0.4f, r, deepSkyBlue.toFillPaint())
        drawRect(Rect(cx - r * 1.2f, cy + r * 0.8f, cx + r * 1.2f, cy + r * 1.1f), gold.toFillPaint())
    }

    // -- Star shape for Mask Filters column --
    fun Canvas.drawStar(rect: Rect, paint: Paint) {
        val cx = rect.left + rect.width / 2
        val cy = rect.top + rect.height / 2 - 10f
        val r = rect.height * 0.25f
        val path = PathBuilder().apply {
            val outerR = r
            val innerR = r * 0.45f
            for (i in 0 until 10) {
                val angle = Math.toRadians((i * 36 - 90).toDouble())
                val radius = if (i % 2 == 0) outerR else innerR
                val px = cx + (radius * kotlin.math.cos(angle)).toFloat()
                val py = cy + (radius * kotlin.math.sin(angle)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            closePath()
        }.detach()
        drawPath(path, paint)
    }

    c.clear(saddleBrown)
    c.drawTitle("Filters")

    // 3 columns × 4 rows: row 0 = header, rows 1-3 = examples
    val grid = contentBox.shrink(20f).splitToGrid(3, 4)

    // ==================== Column 1: Image Filters ====================
    c.drawHeader(grid[0].shrink(5f), "Image Filters")

    // Blur
    val if1 = grid[3].shrink(8f)
    //--- src: 1 Blur
    val blurFilter = ImageFilter.makeBlur(
        8f, 8f, FilterTileMode.CLAMP
    )
    c.save()
    c.clipRect(if1)
    c.saveLayer(null, Paint().apply {
        imageFilter = blurFilter
    })
    c.drawScene(if1)
    c.restore()
    c.restore()
    //--- crs: 1
    c.drawLabel(if1, "Blur")

    // Drop shadow
    val if2 = grid[6].shrink(8f)
    //--- src: 2 Drop shadow
    val shadowFilter = ImageFilter.makeDropShadow(
        10f, 10f, 8f, 8f, CssColors.black
    )
    c.save()
    c.clipRect(if2)
    c.saveLayer(null, Paint().apply {
        imageFilter = shadowFilter
    })
    c.drawScene(if2)
    c.restore()
    c.restore()
    //--- crs: 2
    c.drawLabel(if2, "Drop shadow")

    // Dilate (morphology)
    val if3 = grid[9].shrink(8f)
    //--- src: 3 Dilate
    val dilateFilter = ImageFilter.makeDilate(
        4f, 4f, null, null
    )
    c.save()
    c.clipRect(if3)
    c.saveLayer(null, Paint().apply {
        imageFilter = dilateFilter
    })
    c.drawScene(if3)
    c.restore()
    c.restore()
    //--- crs: 3
    c.drawLabel(if3, "Dilate")

    // ==================== Column 2: Mask Filters ====================
    c.drawHeader(grid[1].shrink(5f), "Mask Filters")

    // Blur NORMAL
    val mf1 = grid[4].shrink(8f)
    //--- src: 4 Blur NORMAL
    val normalMask = MaskFilter.makeBlur(
        FilterBlurMode.NORMAL, 16f
    )
    c.drawStar(mf1, paint().apply {
        color = hotPink
        maskFilter = normalMask
    })
    //--- crs: 4
    c.drawLabel(mf1, "Blur NORMAL")

    // Blur OUTER
    val mf2 = grid[7].shrink(8f)
    //--- src: 5 Blur OUTER
    val outerMask = MaskFilter.makeBlur(
        FilterBlurMode.OUTER, 16f
    )
    c.drawStar(mf2, paint().apply {
        color = hotPink
        maskFilter = outerMask
    })
    //--- crs: 5
    c.drawLabel(mf2, "Blur OUTER")

    // Blur INNER
    val mf3 = grid[10].shrink(8f)
    //--- src: 6 Blur INNER
    val innerMask = MaskFilter.makeBlur(
        FilterBlurMode.INNER, 16f
    )
    c.drawStar(mf3, paint().apply {
        color = hotPink
        maskFilter = innerMask
    })
    //--- crs: 6
    c.drawLabel(mf3, "Blur INNER")

    // ==================== Column 3: Color Filters ====================
    c.drawHeader(grid[2].shrink(5f), "Color Filters")

    // helper: draw scene with color filter via saveLayer
    fun Canvas.drawFilteredScene(rect: Rect, colorFilter: ColorFilter) {
        save()
        clipRect(rect)
        saveLayer(null, Paint().apply {
            this.colorFilter = colorFilter
        })
        drawScene(rect)
        restore()
        restore()
    }

    // Grayscale
    val cf1 = grid[5].shrink(8f)
    //--- src: 7 Grayscale
    val grayscaleFilter =
        ColorMatrices.grayscale().toColorFilter()
    c.drawFilteredScene(cf1, grayscaleFilter)
    //--- crs: 7
    c.drawLabel(cf1, "Grayscale")

    // Sepia
    val cf2 = grid[8].shrink(8f)
    //--- src: 8 Sepia
    val sepiaFilter =
        ColorMatrices.sepia().toColorFilter()
    c.drawFilteredScene(cf2, sepiaFilter)
    //--- crs: 8
    c.drawLabel(cf2, "Sepia")

    // Hue rotate
    val cf3 = grid[11].shrink(8f)
    //--- src: 9 Hue rotate
    val hueFilter =
        ColorMatrices.hueRotate(90f).toColorFilter()
    c.drawFilteredScene(cf3, hueFilter)
    //--- crs: 9
    c.drawLabel(cf3, "Hue rotate 90°")
}
