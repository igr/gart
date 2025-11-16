package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.argb
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.vector.Vec3
import dev.oblac.gart.vector.Vector3
import dev.oblac.gart.vector.cos
import org.jetbrains.skia.*

private data class Waves(
    val r: Wave,
    val g: Wave,
    val b: Wave,
) {
    fun forEach(block: (Int, Wave) -> Unit) {
        listOf(r, g, b).forEachIndexed(block)
    }

    fun a(): Vec3 = Vec3(r.dc, g.dc, b.dc)
    fun b(): Vec3 = Vec3(r.amp, g.amp, b.amp)
    fun c(): Vec3 = Vec3(r.freq, g.freq, b.freq)
    fun d(): Vec3 = Vec3(r.phase, g.phase, b.phase)
    fun values() {
        val a = a()
        val b = b()
        val c = c()
        val d = d()
        print("a=[${a.x} ${a.y} ${a.z}] ")
        print("b=[${b.x} ${b.y} ${b.z}] ")
        print("c=[${c.x} ${c.y} ${c.z}] ")
        print("d=[${d.x} ${d.y} ${d.z}]")
        println()
    }
}

private data class Wave(
    val dc: Float,
    val amp: Float,
    val freq: Float,
    val phase: Float,
)

private var waves: Waves = Waves(
    Wave(0.5f, 0.5f, 1f, 0f),
    Wave(0.5f, 0.5f, 1f, 0.33333f),
    Wave(0.5f, 0.5f, 1f, 0.66666f),
)

// Function that selects a single wave, allowing modification of that wave and keeping others unchanged
// returning the updated Waves object.
private fun selectWave(
    waves: Waves,
    selector: (Waves) -> Wave
): ((Wave) -> Wave) -> Waves {

    return { modify ->
        when (selector(waves)) {
            waves.r -> waves.copy(r = modify(waves.r))
            waves.g -> waves.copy(g = modify(waves.g))
            waves.b -> waves.copy(b = modify(waves.b))
            else -> waves
        }
    }
}

private var wave: (Waves) -> Wave = { it.r }

fun main() {
    val gart = Gart.of("gradient-generator", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    w.show(DrawGG(g)).onKey { key ->
        var redraw = false
        when (key) {
            Key.KEY_R -> {
                wave = { it.r }
            }

            Key.KEY_G -> {
                wave = { it.g }
            }

            Key.KEY_B -> {
                wave = { it.b }
            }

            Key.KEY_Q -> {
                waves = selectWave(waves, wave)({ it.copy(dc = it.dc + 0.01f) })
                redraw = true
            }

            Key.KEY_A -> {
                waves = selectWave(waves, wave)({ it.copy(dc = it.dc - 0.01f) })
                redraw = true
            }

            Key.KEY_W -> {
                waves = selectWave(waves, wave)({ it.copy(amp = it.amp + 0.01f) })
                redraw = true
            }

            Key.KEY_S -> {
                waves = selectWave(waves, wave)({ it.copy(amp = it.amp - 0.01f) })
                redraw = true
            }

            Key.KEY_E -> {
                waves = selectWave(waves, wave)({ it.copy(freq = it.freq + 0.05f) })
                redraw = true
            }

            Key.KEY_D -> {
                waves = selectWave(waves, wave)({ it.copy(freq = it.freq - 0.05f) })
                redraw = true
            }

            Key.KEY_Z -> {
                waves = selectWave(waves, wave)({ it.copy(phase = it.phase - 0.01f) })
                redraw = true
            }

            Key.KEY_X -> {
                waves = selectWave(waves, wave)({ it.copy(phase = it.phase + 0.01f) })
                redraw = true
            }

            else -> {}
        }
        if (redraw) {
            draw(g.canvas, g.d)
            println(waves.values())
        }
    }
}

private class DrawGG(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

data class GGen(
    val gapA: Float,
    val gapB: Float,
)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.white)

    val ggen = GGen(gapA = 50f, gapB = 180f)
    drawGrid(c, d, ggen)
    drawWavePlot(c, d, ggen, waves)

    val wa = waves.a()
    val wb = waves.b()
    val wc = waves.c()
    val wd = waves.d()
    val palette: (Float) -> Vector3 = { t: Float ->
        wa + wb * cos(Vec3.TWO_PI * (wc * t + wd))
    }
    drawGradientBar(c, d, ggen, palette)
}

private fun drawGrid(c: Canvas, d: Dimension, ggen: GGen) {
    val W = d.w.toFloat()
    val H = d.h.toFloat()

    // Calculate drawing area with gaps
    val drawX = ggen.gapA
    val drawY = ggen.gapA
    val drawW = W - 2 * ggen.gapA
    val drawH = H - ggen.gapA - ggen.gapB

    val gridPaint = Paint().apply {
        mode = PaintMode.STROKE
        strokeWidth = 1f
        color = 0xFF888888.toInt() // gray color for grid
        isAntiAlias = true
        // Create dotted line effect: 5px dash, 5px gap
        pathEffect = org.jetbrains.skia.PathEffect.makeDash(floatArrayOf(5f, 5f), 0f)
    }

    // Draw 11 vertical lines (x = 0.0, 0.1, 0.2, ..., 1.0)
    for (i in 0..10) {
        val xNorm = i / 10f  // normalized x: 0 to 1
        val x = drawX + xNorm * drawW  // screen x coordinate with gap
        c.drawLine(x, drawY, x, drawY + drawH, gridPaint)
    }

    // Draw 11 horizontal lines (y = 0.0, 0.1, 0.2, ..., 1.0)
    // In mathematical coordinates, y=0 is at bottom, y=1 is at top
    for (i in 0..10) {
        val yNorm = i / 10f  // normalized y: 0 to 1 (mathematical)
        val y = drawY + drawH - yNorm * drawH  // screen y coordinate (inverted) with gap
        c.drawLine(drawX, y, drawX + drawW, y, gridPaint)
    }

    // Draw X and Y axes (solid, darker lines)
    val axisPaint = Paint().apply {
        mode = PaintMode.STROKE
        strokeWidth = 2f
        color = 0xFF000000.toInt() // black color for axes
        isAntiAlias = true
    }

    // X-axis (y = 0, at bottom of grid)
    val xAxisY = drawY + drawH
    c.drawLine(drawX, xAxisY, drawX + drawW, xAxisY, axisPaint)

    // Y-axis (x = 0, at left of grid)
    val yAxisX = drawX
    c.drawLine(yAxisX, drawY, yAxisX, drawY + drawH, axisPaint)

    // Draw axis labels
    val textPaint = Paint().apply {
        color = 0xFF000000.toInt() // black
        isAntiAlias = true
    }

    // X-axis labels (0.0, 0.5, 1.0)
    val xLabels = listOf(0.0f, 0.5f, 1.0f)
    xLabels.forEach { value ->
        val x = drawX + value * drawW
        val text = value.toString()
        c.drawString(text, x - 10f, xAxisY + 25f, font(FontFamily.OdibeeSans, 20f), textPaint)
    }

    // Y-axis labels (0.0, 0.5, 1.0)
    val yLabels = listOf(0.0f, 0.5f, 1.0f)
    yLabels.forEach { value ->
        val y = drawY + drawH - value * drawH
        val text = value.toString()
        c.drawString(text, yAxisX - 30f, y + 5f, font(FontFamily.OdibeeSans, 20f), textPaint)
    }
}

private fun drawWavePlot(c: Canvas, d: Dimension, ggen: GGen, waves: Waves) {
    val W = d.w.toFloat()
    val H = d.h.toFloat()

    // Calculate drawing area with gaps (same as in drawGrid)
    val drawX = ggen.gapA
    val drawY = ggen.gapA
    val drawW = W - 2 * ggen.gapA
    val drawH = H - ggen.gapA - ggen.gapB

    val paint = Paint().apply {
        mode = PaintMode.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    val samples = 2000
    val step = 1f / samples  // step in normalized coordinates (0 to 1)

    waves.forEach { index, wave ->
        paint.color = when (index) {
            0 -> Colors.red
            1 -> Colors.green
            2 -> Colors.blue
            else -> Colors.black
        }

        val path = Path()

        for (i in 0..samples) {
            // Normalized x coordinate (0 to 1)
            val xNorm = i * step

            // Calculate y using cosine function: y = dc + amp * cos(2π * (freq * x + phase))
            val yNorm = wave.dc + wave.amp * kotlin.math.cos(
                2f * Math.PI.toFloat() * (wave.freq * xNorm + wave.phase)
            )

            // Convert normalized coordinates to screen coordinates
            val x = drawX + xNorm * drawW
            val y = (drawY + drawH - yNorm * drawH).let {
                // Clamp y to drawing area
                when {
                    it < drawY -> drawY
                    it > drawY + drawH -> drawY + drawH
                    else -> it
                }
            }

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        c.drawPath(path, paint)
    }
}

private fun drawGradientBar(c: Canvas, d: Dimension, ggen: GGen, palette: (Float) -> Vec3) {
    val W = d.w.toFloat()
    val H = d.h.toFloat()

    val barHeight = 120f
    val barY = H - barHeight - 20f  // 20px margin from bottom
    val barX = ggen.gapA
    val barW = W - 2 * ggen.gapA

    // Draw border
    c.drawRect(Rect(barX, barY, barX + barW, barY + barHeight), strokeOfBlack(2f))

    // Fill with gradient using palette function
    val paint = Paint().apply {
        mode = PaintMode.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    val samples = barW.toInt()  // One vertical line per pixel
    for (i in 0 until samples) {
        val t = i.toFloat() / samples  // normalized x from 0 to 1
        val color = palette(t)

        paint.color = argb(1f, color.x, color.y, color.z)

        val x = barX + i
        c.drawLine(x, barY, x, barY + barHeight, paint)
    }
}

// 🔥
private val wavesRainbow = Waves(
    Wave(0.5f, 0.5f, 1f, 0f),
    Wave(0.5f, 0.5f, 1f, 0.33333f),
    Wave(0.5f, 0.5f, 1f, 0.66666f),
)
private val waves2 = Waves(
    Wave(0.5f, 0.5f, 1f, 0f),
    Wave(0.5f, 0.5f, 1f, 0.10f),
    Wave(0.5f, 0.5f, 1f, 0.20f),
)
private val waves3 = Waves(
    Wave(0.5f, 0.5f, 1f, 0.30f),
    Wave(0.5f, 0.5f, 1f, 0.20f),
    Wave(0.5f, 0.5f, 1f, 0.20f),
)
private val waves4 = Waves(
    Wave(0.5f, 0.5f, 1f, 0.80f),
    Wave(0.5f, 0.5f, 1f, 0.90f),
    Wave(0.5f, 0.5f, 0.5f, 0.30f),
)
private val waves5 = Waves(
    Wave(0.5f, 0.5f, 2f, 0.50f),
    Wave(0.5f, 0.5f, 1f, 0.20f),
    Wave(0.5f, 0.5f, 0f, 0.25f),
)
private val waves6 = Waves(
    Wave(0.8f, 0.2f, 2f, 0.0f),
    Wave(0.5f, 0.4f, 1f, 0.25f),
    Wave(0.4f, 0.2f, 1f, 0.25f),
)
