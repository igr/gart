package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.coral
import dev.oblac.gart.color.CssColors.deepSkyBlue
import dev.oblac.gart.color.CssColors.gold
import dev.oblac.gart.color.CssColors.mediumSeaGreen
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.center
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.splitToGrid
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.*
import org.jetbrains.skia.ImageFilter.Companion.makeBlur

val slide11 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.020f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 40f, rect.right, rect.bottom - 5f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    // Shared scene: three overlapping colored circles + a rectangle
    fun Canvas.drawScene(rect: Rect) {
        val cx = rect.left + rect.width / 2
        val cy = rect.top + rect.height / 2 - 15f
        val r = rect.height * 0.17f
        drawCircle(cx - r * 0.7f, cy - r * 0.4f, r, coral.toFillPaint())
        drawCircle(cx + r * 0.7f, cy - r * 0.4f, r, mediumSeaGreen.toFillPaint())
        drawCircle(cx, cy + r * 0.5f, r, deepSkyBlue.toFillPaint())
        drawRect(Rect(cx - r * 1.1f, cy + r * 0.9f, cx + r * 1.1f, cy + r * 1.2f), gold.toFillPaint())
    }

    c.clear(CssColors.darkSlateGray)
    c.drawTitle("Layers")

    val grid = contentBox.shrink(20f).splitToGrid(3, 2)

    // 1. No layer — direct drawing (baseline)
    val g1 = grid[0].shrink(8f)
    //--- src: 1 Direct drawing
    c.save()
    c.clipRect(g1)
    c.drawCircle(g1.center().x - 30f, g1.center().y - 20f, g1.height * 0.17f, paint().apply {
        color = coral; imageFilter = makeBlur(6f, 6f, FilterTileMode.CLAMP)
    })
    c.drawCircle(g1.center().x + 30f, g1.center().y - 20f, g1.height * 0.17f, paint().apply {
        color = mediumSeaGreen; imageFilter = makeBlur(6f, 6f, FilterTileMode.CLAMP)
    })
    c.drawCircle(g1.center().x, g1.center().y + 25f, g1.height * 0.17f, paint().apply {
        color = deepSkyBlue; imageFilter = makeBlur(6f, 6f, FilterTileMode.CLAMP)
    })
    c.restore()
    //--- crs: 1
    c.drawLabel(g1, "Per-shape blur")

    // 2. saveLayer — blur applied to the group
    val g2 = grid[1].shrink(8f)
    //--- src: 2 saveLayer + blur
    c.save()
    c.clipRect(g2)
    c.saveLayer(null, paint().apply {
        imageFilter = makeBlur(
            6f, 6f, FilterTileMode.CLAMP
        )
    })
    c.drawScene(g2)
    c.restore() // restores saveLayer (applies blur)
    c.restore() // restores clipRect
    //--- crs: 2
    c.drawLabel(g2, "saveLayer + blur")

    // 3. saveLayer with alpha — group opacity
    val g3 = grid[2].shrink(8f)
    //--- src: 3 saveLayer + alpha
    c.save()
    c.clipRect(g3)
    c.saveLayer(null, paint().apply {
        alpha = 100  // ~40% opacity for entire group
    })
    c.drawScene(g3)
    c.restore()
    c.restore()
    //--- crs: 3
    c.drawLabel(g3, "Group opacity (alpha)")

    // 4. saveLayer with drop shadow on grouped shapes
    val g4 = grid[3].shrink(8f)
    //--- src: 4 saveLayer + shadow
    c.save()
    c.clipRect(g4)
    c.saveLayer(null, Paint().apply {
        imageFilter = ImageFilter.makeDropShadow(
            12f, 12f, 8f, 8f, CssColors.black
        )
    })
    c.drawScene(g4)
    c.restore()
    c.restore()
    //--- crs: 4
    c.drawLabel(g4, "Group drop shadow")

    // 5. Nested layers — inner sharp, outer blurred
    val g5 = grid[4].shrink(8f)
    //--- src: 5 Nested layers
    c.save()
    c.clipRect(g5)
    // outer layer: blur
    c.saveLayer(null, Paint().apply {
        imageFilter = makeBlur(
            4f, 4f, FilterTileMode.CLAMP
        )
    })
    // inner layer: draw sharp, then restore
    // applies blur to entire composite
    val cx5 = g5.center().x
    val cy5 = g5.center().y - 15f
    val r5 = g5.height * 0.20f
    c.drawCircle(cx5, cy5, r5, coral.toFillPaint())
    // inner saveLayer with color filter
    c.saveLayer(null, Paint().apply {
        colorFilter = ColorFilter.makeBlend(
            0x6600FF00, BlendMode.SRC_ATOP
        )
    })
    c.drawCircle(cx5, cy5 + r5, r5, deepSkyBlue.toFillPaint())
    c.restore() // inner layer (color tinted)
    c.restore() // outer layer (blurred)
    c.restore() // clip
    //--- crs: 5
    c.drawLabel(g5, "Nested layers")

    // 6. Isolated blending via saveLayer
    val g6 = grid[5].shrink(8f)
    //--- src: 6 Isolated blending
    val cx6 = g6.center().x
    val cy6 = g6.center().y - 15f
    val r6 = g6.height * 0.18f
    // first: draw background circle
    c.drawCircle(cx6, cy6, r6 * 1.8f, paint().apply {
        color = 0xFF334455.toInt();
    })
    // isolated layer: blend only within this layer
    c.save()
    c.clipRect(g6)
    c.saveLayer(null, null)
    c.drawCircle(cx6 - r6 * 0.5f, cy6, r6, coral.toFillPaint())
    c.drawCircle(cx6 + r6 * 0.5f, cy6, r6, paint().apply {
        color = mediumSeaGreen;
        blendMode = BlendMode.DIFFERENCE
    })
    c.restore() // layer composites onto background
    c.restore()
    //--- crs: 6
    c.drawLabel(g6, "Isolated blending")
}
