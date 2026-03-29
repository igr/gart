package dev.oblac.gart

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.draw
import dev.oblac.gart.gfx.drawImage
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.pixels.makeGray
import dev.oblac.gart.stipple.stippleDots
import dev.oblac.gart.stipple.stippleNoisyDotDensity
import dev.oblac.gart.util.loadResourceAsImage
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Shader

private var stippleAlgorithm: (Gartmap) -> Unit = {}
private var drawAlgorithm: (c: Canvas, d: Dimension) -> Unit = ::draw1
private var preProcess: (Gartmap) -> Unit = {}
private val corgi = loadResourceAsImage("/corgi.png")

fun main() {
    val gart = Gart.of("exampleStipple", 1024, 1024)
    val g = gart.gartvas()
    val b = Gartmap(g)
    var maxColors = 4
    var pixelSize = 4

    gart.window().show { c, d, _ ->
        drawAlgorithm(g.canvas, d)
        b.updatePixelsFromCanvas()
        preProcess(b)
        stippleAlgorithm(b)
        b.drawToCanvas()
        c.draw(g)
    }.onKey {
        when (it) {
            // NUMBERS = DRAW
            Key.KEY_0 -> {
                preProcess = {}
                println("Switched to NO PREPROCESS")
            }

            Key.KEY_1 -> {
                preProcess = { b -> makeGray(b) }
                println("Switched to GRAY")
            }

            Key.KEY_2 -> {
                drawAlgorithm = ::draw1
                println("Switched to RED")
            }

            Key.KEY_3 -> {
                drawAlgorithm = ::draw2
                println("Switched to BLACK")
            }

            Key.KEY_4 -> {
                drawAlgorithm = ::draw3
                println("Switched to BLACK")
            }

            // PIXEL SIZE
            Key.KEY_Q -> {
                pixelSize++
                println("Increased pixel size to: $pixelSize")
            }

            Key.KEY_A -> {
                if (pixelSize > 1) {
                    pixelSize--
                }
                println("Decreased pixel size to: $pixelSize")
            }
            // COLORS
            Key.KEY_W -> {
                maxColors++
                println("Increased max colors to: $maxColors")
            }

            Key.KEY_S -> {
                if (maxColors > 2) {
                    maxColors--
                }
                println("Decreased max colors to: $maxColors")
            }

            Key.KEY_Z -> stippleAlgorithm = { b -> stippleNoisyDotDensity(b, pixelSize) }
            Key.KEY_X -> stippleAlgorithm = { b -> stippleDots(b, pixelSize) }

            else -> {}
        }
    }
}

private fun draw1(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    c.drawCircle(d.cx, d.cy, 400f, fillOfRed().apply {
        this.shader = Shader.makeLinearGradient(
            x0 = d.cx, y0 = d.cy - 400f,
            x1 = d.cx, y1 = d.cy + 400f,
            colors = arrayOf(CssColors.white, CssColors.red).toIntArray(),
        )
        this.isDither = true
    })
}

private fun draw2(c: Canvas, d: Dimension) {
    c.clear(CssColors.white)
    c.drawCircle(d.cx, d.cy, 400f, fillOfRed().apply {
        this.shader = Shader.makeLinearGradient(
            x0 = d.cx, y0 = d.cy - 400f,
            x1 = d.cx, y1 = d.cy + 400f,
            colors = arrayOf(CssColors.white, CssColors.black).toIntArray(),
        )
        this.isDither = true
    })
}

private fun draw3(c: Canvas, d: Dimension) {
    c.drawImage(corgi)
}
