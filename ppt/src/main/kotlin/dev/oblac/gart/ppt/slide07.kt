package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.CssColors.darkRed
import dev.oblac.gart.color.CssColors.deepSkyBlue
import dev.oblac.gart.color.CssColors.gold
import dev.oblac.gart.color.CssColors.hotPink
import dev.oblac.gart.color.CssColors.lime
import dev.oblac.gart.color.CssColors.midnightBlue
import dev.oblac.gart.color.CssColors.purple
import dev.oblac.gart.color.CssColors.red
import dev.oblac.gart.color.CssColors.white
import dev.oblac.gart.color.CssColors.yellow
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.color.toStrokePaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.center
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.gfx.splitToGrid
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeFractalNoise
import org.jetbrains.skia.Shader.Companion.makeLinearGradient
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import org.jetbrains.skia.Shader.Companion.makeSweepGradient
import org.jetbrains.skia.Shader.Companion.makeTwoPointConicalGradient

private val vivid = Palettes.colormap032
private val vividColors = IntArray(vivid.size) { vivid[it] }
private val vividPos = FloatArray(vivid.size) { it.toFloat() / (vivid.size - 1) }

val slide07 = DrawFrame { c, d, f ->
    val labelFont = font(FontFamily.RethinkSans, screen.height * 0.024f)
    val labelPaint = white.toFillPaint()

    fun Canvas.drawLabel(rect: Rect, text: String) {
        val labelRect = Rect(rect.left, rect.bottom - 50f, rect.right, rect.bottom - 10f)
        drawStringInRect(text, labelRect, labelFont, labelPaint, HorizontalAlign.CENTER)
    }

    fun shaderPaint(shader: Shader): Paint = Paint().apply {
        this.shader = shader
        isAntiAlias = true
    }

    fun Canvas.drawShaderCircle(rect: Rect, paint: Paint) {
        val center = rect.center()
        val radius = rect.height * 0.35f
        drawCircle(center.x, center.y, radius, paint)
        drawCircle(center.x, center.y, radius, white.toStrokePaint(3f))
    }

    c.clear(CssColors.darkSlateBlue)
    c.drawTitle("Shaders")

    val grid = contentBox.shrink(20f).splitToGrid(4, 2)

    // 1. Solid color
    val g1 = grid[0].shrink(10f)
    //--- src: 1 Solid color
    val solidPaint = Paint().apply {
        color = CssColors.coral
        isAntiAlias = true
    }
    c.drawShaderCircle(g1, solidPaint)
    //--- crs: 1
    c.drawLabel(g1, "Solid color")

    // 2. Linear gradient
    val g2 = grid[1].shrink(10f)
    //--- src: 2 Linear gradient
    val linearShader = makeLinearGradient(
        g2.left, g2.top, g2.right, g2.bottom,
        vividColors, vividPos,
        GradientStyle.DEFAULT
    )
    c.drawShaderCircle(g2, shaderPaint(linearShader))
    //--- crs: 2
    c.drawLabel(g2, "Linear gradient")

    // 3. Radial gradient
    val g3 = grid[2].shrink(10f)
    //--- src: 3 Radial gradient
    val center3 = g3.center()
    val radialShader = makeRadialGradient(
        center3.x, center3.y, g3.height * 0.35f,
        intArrayOf(CssColors.white, deepSkyBlue, midnightBlue),
        floatArrayOf(0f, 0.5f, 1f),
        GradientStyle.DEFAULT
    )
    c.drawShaderCircle(g3, shaderPaint(radialShader))
    //--- crs: 3
    c.drawLabel(g3, "Radial gradient")

    // 4. Sweep gradient
    val g4 = grid[3].shrink(10f)
    //--- src: 4 Sweep gradient
    val center4 = g4.center()
    val sweepShader = makeSweepGradient(
        center4.x, center4.y,
        vividColors, vividPos,
        GradientStyle.DEFAULT
    )
    c.drawShaderCircle(g4, shaderPaint(sweepShader))
    //--- crs: 4
    c.drawLabel(g4, "Sweep gradient")

    // 5. Two-point conical
    val g5 = grid[4].shrink(10f)
    //--- src: 5 Conical gradient
    val center5 = g5.center()
    val conicalShader = makeTwoPointConicalGradient(
        center5.x - 30f, center5.y - 30f, 20f,
        center5.x, center5.y, g5.height * 0.35f,
        intArrayOf(yellow, red, darkRed),
        floatArrayOf(0f, 0.5f, 1f),
        GradientStyle.DEFAULT
    )
    c.drawShaderCircle(g5, shaderPaint(conicalShader))
    //--- crs: 5
    c.drawLabel(g5, "Conical gradient")

    // 6. Fractal noise
    val g6 = grid[5].shrink(10f)
    //--- src: 6 Fractal noise
    val noiseShader = makeFractalNoise(
        0.02f, 0.02f, 4, 0f
    )
    c.drawShaderCircle(g6, shaderPaint(noiseShader))
    //--- crs: 6
    c.drawLabel(g6, "Fractal noise")

    // 7. Tiling: REPEAT
    val g7 = grid[6].shrink(10f)
    //--- src: 7 Tile REPEAT
    val center7 = g7.center()
    val tileSize = g7.height * 0.15f
    val repeatShader = makeLinearGradient(
        center7.x - tileSize, center7.y - tileSize,
        center7.x + tileSize, center7.y + tileSize,
        intArrayOf(hotPink, gold, hotPink),
        floatArrayOf(0f, 0.5f, 1f),
        GradientStyle(FilterTileMode.REPEAT, true, Matrix33.IDENTITY)
    )
    c.drawShaderCircle(g7, shaderPaint(repeatShader))
    //--- crs: 7
    c.drawLabel(g7, "Tile: REPEAT")

    // 8. Tiling: MIRROR
    val g8 = grid[7].shrink(10f)
    //--- src: 8 Tile MIRROR
    val center8 = g8.center()
    val mirrorShader = makeLinearGradient(
        center8.x - tileSize, center8.y - tileSize,
        center8.x + tileSize, center8.y + tileSize,
        intArrayOf(lime, purple),
        null,
        GradientStyle(FilterTileMode.MIRROR, true, Matrix33.IDENTITY)
    )
    c.drawShaderCircle(g8, shaderPaint(mirrorShader))
    //--- crs: 8
    c.drawLabel(g8, "Tile: MIRROR")
}
