package dev.oblac.gart.ppt

import dev.oblac.gart.Dimension
import dev.oblac.gart.DrawFrame
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.coral
import dev.oblac.gart.color.CssColors.deepSkyBlue
import dev.oblac.gart.color.CssColors.gold
import dev.oblac.gart.color.CssColors.mediumSeaGreen
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.fx.scaleImage
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.splitToGrid
import dev.oblac.gart.pixels.dither.ditherFloydSteinberg
import dev.oblac.gart.pixels.makeGray
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.*

val slide12 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.020f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 40f, rect.right, rect.bottom - 5f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    // Helper: draw a sample scene onto a Gartvas
    fun drawSampleScene(gv: Gartvas) {
        val gc = gv.canvas
        gc.clear(CssColors.darkSlateGray)
        val w = gv.d.wf
        val h = gv.d.hf
        val r = h * 0.22f
        // colorful circles
        gc.drawCircle(w * 0.33f, h * 0.38f, r, coral.toFillPaint())
        gc.drawCircle(w * 0.66f, h * 0.38f, r, mediumSeaGreen.toFillPaint())
        gc.drawCircle(w * 0.50f, h * 0.65f, r, deepSkyBlue.toFillPaint())
        // small rectangle
        gc.drawRect(Rect(w * 0.20f, h * 0.78f, w * 0.80f, h * 0.88f), gold.toFillPaint())
    }

    c.clear(CssColors.darkOliveGreen)
    c.drawTitle("Bitmaps")

    val grid = contentBox.shrink(20f).splitToGrid(3, 2)

    // Cell dimensions for off-screen rendering (leave room for label)
    val cellW = (grid[0].width * 0.75f).toInt()
    val cellH = (grid[0].height * 0.70f).toInt()
    val cellDim = Dimension(cellW, cellH)

    // Helper: draw scaled image centered in the grid cell, above the label
    fun Canvas.drawCentered(image: Image, rect: Rect) {
        val imgW = cellW.toFloat()
        val imgH = cellH.toFloat()
        val x = rect.left + (rect.width - imgW) / 2
        val y = rect.top + (rect.height - 45f - imgH) / 2
        drawImage(image, x, y)
    }

    // 1. Gartvas — off-screen canvas + snapshot
    val g1 = grid[0].shrink(8f)
    //--- src: 1 Gartvas + snapshot
    val gv1 = Gartvas(cellDim)
    drawSampleScene(gv1)
    val img1 = gv1.snapshot()
    c.drawCentered(img1, g1)
    //--- crs: 1
    c.drawLabel(g1, "Gartvas + snapshot()")

    // 2. Pixel-level manipulation — invert colors via Pixels interface
    val g2 = grid[1].shrink(8f)
    //--- src: 2 Pixel manipulation
    val gv2 = Gartvas(cellDim)
    drawSampleScene(gv2)
    val bm2 = Gartmap(gv2)
    bm2.pixelBytes.let { pb ->
        for (y in 0 until cellDim.h) {
            for (x in 0 until cellDim.w) {
                val pixel = pb.get(x, y)
                val inv = argb(
                    alpha(pixel),
                    255 - red(pixel),
                    255 - green(pixel),
                    255 - blue(pixel)
                )
                pb.set(x, y, inv)
            }
        }
    }
    c.drawCentered(bm2.image(), g2)
    //--- crs: 2
    c.drawLabel(g2, "Pixel invert via Gartmap")

    // 3. Grayscale conversion
    val g3 = grid[2].shrink(8f)
    //--- src: 3 Grayscale
    val gv3 = Gartvas(cellDim)
    drawSampleScene(gv3)
    val bm3 = Gartmap(gv3)
    makeGray(bm3)
    c.drawCentered(bm3.image(), g3)
    //--- crs: 3
    c.drawLabel(g3, "makeGray()")

    // 4. Floyd-Steinberg dithering
    val g4 = grid[3].shrink(8f)
    //--- src: 4 Dithering
    val gv4 = Gartvas(cellDim)
    drawSampleScene(gv4)
    val bm4 = Gartmap(gv4)
    makeGray(bm4)
    ditherFloydSteinberg(bm4, pixelSize = 2, colorCount = 4)
    c.drawCentered(bm4.image(), g4)
    //--- crs: 4
    c.drawLabel(g4, "ditherFloydSteinberg()")

    // 5. Image scaling — downscale and upscale
    val g5 = grid[4].shrink(8f)
    //--- src: 5 Image scaling
    val gv5 = Gartvas(cellDim)
    drawSampleScene(gv5)
    val small = gv5.snapshot()
        .scaleImage(cellW / 8, cellH / 8)
    val upscaled = small
        .scaleImage(cellW, cellH)
    c.drawCentered(upscaled, g5)
    //--- crs: 5
    c.drawLabel(g5, "scaleImage() pixelate")

    // 6. Row/column pixel sorting
    val g6 = grid[5].shrink(8f)
    //--- src: 6 Row pixel sort
    val gv6 = Gartvas(cellDim)
    drawSampleScene(gv6)
    val bm6 = Gartmap(gv6)
    for (y in 0 until cellDim.h) {
        val row = bm6.row(y)
        row.sort()
        bm6.row(y, row)
    }
    c.drawCentered(bm6.image(), g6)
    //--- crs: 6
    c.drawLabel(g6, "Row pixel sorting")
}
