package dev.oblac.gart

import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.noise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathEffect
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// inspired by: https://graphtoy.com

typealias GfxFun = (x: Float, t: Float) -> Float

val f1: GfxFun = { x, t -> 4 + 4 * smoothstep(0f, 0.7f, sin(x + t)) }
val f2: GfxFun = { x, t -> sqrt(9 * 9 - x * x) }
val f3: GfxFun = { x, t -> 3 * sin(x) / x }
val f4: GfxFun = { x, t -> 2 * noise(3 * x + t) + f3(x, t) }
val f5: GfxFun = { x, t -> (t + floor(x - t)) / 2 - 5 }
val f6: GfxFun = { x, t -> sin(f5(x, t)) - 5 }

val functions = arrayOf(f1, f2, f3, f4, f6)

private var maxX = 10f
private var maxY = 10f
private var timeResolution = 0.01f

fun main() {
    val gart = Gart.of("fun-graph", 1024, 1024)
    println(gart)

    val w = gart.window()

    // Hot reload requires a real class to be created, not a lambda!
    val draw = FunDraw()
    w.show(draw).onKey { key ->
        when (key) {
            Key.KEY_W -> maxY += 0.1f
            Key.KEY_S -> maxY -= 0.1f
            Key.KEY_A -> maxX -= 0.1f
            Key.KEY_D -> maxX += 0.1f
            Key.KEY_E -> timeResolution += 0.005f
            Key.KEY_Q -> timeResolution -= 0.005f
            else -> {}
        }
    }
}

private class FunDraw : Drawing() {
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(c, d, f.frame)
    }
}

private fun draw(c: Canvas, d: Dimension, frame: Long) {
    c.clear(BgColors.coolDark)


    drawGrid(c, d, maxX, maxY)
    drawFunctions(c, d, maxX, maxY, frame * timeResolution, functions)
}

private val mix1 = Palettes.mix1

private fun drawFunctions(c: Canvas, d: Dimension, maxX: Float, maxY: Float, t: Float, functions: Array<GfxFun>) {
    // Calculate scaling factors to map mathematical coordinates to pixel coordinates
    val scaleX = d.wf / (2 * maxX)
    val scaleY = d.hf / (2 * maxY)

    // Helper functions to convert mathematical coordinates to pixel coordinates
    fun mathToPixelX(x: Float): Float = d.cx + x * scaleX
    fun mathToPixelY(y: Float): Float = d.cy - y * scaleY  // Subtract because Y is inverted in screen coordinates

    // Sampling rate: sample every pixel for smooth curves
    val samplesPerUnit = (d.wf / (2 * maxX)).toInt().coerceAtLeast(1)
    val step = 1f / samplesPerUnit

    // Draw each function
    functions.forEachIndexed { index, func ->
        // Paint for drawing function curves
        val functionPaint = strokeOf(mix1.safe(index), 3f)

        var x = -maxX
        var prevPixelX: Float? = null
        var prevPixelY: Float? = null

        while (x <= maxX) {
            // Evaluate the function at this x coordinate
            val y = func(x, t)

            // Convert to pixel coordinates
            val pixelX = mathToPixelX(x)
            val pixelY = mathToPixelY(y)

            // Draw line from previous point to current point
            if (prevPixelX != null && prevPixelY != null) {
                // Only draw if both points are within reasonable bounds
                // (we could add clipping here if needed, but let Skia handle it for now)
                c.drawLine(prevPixelX, prevPixelY, pixelX, pixelY, functionPaint)
            }

            prevPixelX = pixelX
            prevPixelY = pixelY

            x += step
        }
    }
}

private fun drawGrid(c: Canvas, d: Dimension, maxX: Float, maxY: Float, maxGridLines: Int = 20) {
    // Calculate scaling factors to map mathematical coordinates to pixel coordinates
    // X axis: -maxX to +maxX maps to 0 to d.wf
    // Y axis: -maxY to +maxY maps to 0 to d.hf (inverted for screen coordinates)
    val scaleX = d.wf / (2 * maxX)
    val scaleY = d.hf / (2 * maxY)

    // Helper function to convert mathematical coordinates to pixel coordinates
    fun mathToPixelX(x: Float): Float = d.cx + x * scaleX
    fun mathToPixelY(y: Float): Float = d.cy - y * scaleY  // Subtract because Y is inverted in screen coordinates

    // Calculate appropriate step sizes for grid lines
    fun calculateNiceStep(maxValue: Float, maxLines: Int): Int {
        val roughStep = maxValue.toDouble() / (maxLines / 2.0)
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(roughStep)))
        val normalized = roughStep / magnitude

        val niceNormalized = when {
            normalized <= 1.0 -> 1.0
            normalized <= 2.0 -> 2.0
            normalized <= 5.0 -> 5.0
            else -> 10.0
        }

        return (niceNormalized * magnitude).toInt().coerceAtLeast(1)
    }

    val stepX = calculateNiceStep(maxX, maxGridLines)
    val stepY = calculateNiceStep(maxY, maxGridLines)

    // Draw grid lines
    val gridPaint = strokeOf(BgColors.bg05, 1f).apply {
        // Create dotted line effect: 3px dash, 3px gap
        pathEffect = PathEffect.makeDash(floatArrayOf(3f, 3f), 0f)
    }

    // Draw vertical grid lines
    var x = -maxX
    while (x <= maxX) {
        if (x == 0f) {
            x += stepX
            continue  // Skip the Y-axis, will draw it separately
        }
        val pixelX = mathToPixelX(x)
        c.drawLine(pixelX, 0f, pixelX, d.hf, gridPaint)
        x += stepX
    }

    // Draw horizontal grid lines
    var y = -maxY
    while (y <= maxY) {
        if (y == 0f) {
            y += stepY
            continue  // Skip the X-axis, will draw it separately
        }
        val pixelY = mathToPixelY(y)
        c.drawLine(0f, pixelY, d.wf, pixelY, gridPaint)
        y += stepY
    }

    // Draw X and Y axes with thicker, solid lines
    val axisPaint = strokeOf(BgColors.bg01, 2f)

    // X-axis (horizontal line through center)
    c.drawLine(0f, d.cy, d.wf, d.cy, axisPaint)

    // Y-axis (vertical line through center)
    c.drawLine(d.cx, 0f, d.cx, d.hf, axisPaint)

    // Draw labels at the bottom
    val textPaint = paint().apply {
        color = Colors.white
    }

    val labelY = d.cy + 20f

    // Left label
    c.drawString("%.2f".format(-maxX), 0f, labelY, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Middle left label (1/4 of width)
    c.drawString("%.2f".format(-maxX / 2), d.wf / 4f, labelY, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Center label
    c.drawString("0", d.cx, labelY, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Middle right label (3/4 of width)
    c.drawString("%.2f".format(maxX / 2), d.wf * 3f / 4f, labelY, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Right label
    c.drawString("%.2f".format(maxX), d.wf - 30, labelY, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Y-axis labels (positioned to the left of the Y-axis)
    val labelX = d.cx - 30f

    // Top label (maxY)
    c.drawString("%.2f".format(maxY), labelX, 20f, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Middle top label (maxY/2)
    c.drawString("%.2f".format(maxY / 2), labelX, d.hf / 4f, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Center label (0)
    c.drawString("0", labelX, d.cy, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Middle bottom label (-maxY/2)
    c.drawString("%.2f".format(-maxY / 2), labelX, d.hf * 3f / 4f, font(FontFamily.OdibeeSans, 20f), textPaint)

    // Bottom label (-maxY)
    c.drawString("%.2f".format(-maxY), labelX, d.hf - 5f, font(FontFamily.OdibeeSans, 20f), textPaint)

}
