package dev.oblac.gart

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.draw
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.pixels.dither.*
import dev.oblac.gart.pixels.makeGray
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Shader

private var ditherAlgorithm: (Gartmap) -> Unit = {}
private var drawAlgorithm: (c: Canvas, d: Dimension) -> Unit = ::draw1
private var preProcess: (Gartmap) -> Unit = {}

fun main() {
    val gart = Gart.of("exampleDither", 1024, 1024)
    val g = gart.gartvas()
    val b = Gartmap(g)
    var maxColors = 4
    var pixelSize = 4

    gart.window().show { c, d, _ ->
        drawAlgorithm(g.canvas, d)
        b.updatePixelsFromCanvas()
        preProcess(b)
        ditherAlgorithm(b)
        b.drawToCanvas()
        c.draw(g)
    }.onKey {
        when (it) {
            Key.KEY_F -> {
                preProcess = { b -> makeGray(b) }
                println("Switched to GRAY")
            }

            Key.KEY_R -> {
                preProcess = {}
                println("Switched to NO PREPROCESS")
            }
            Key.KEY_E -> {
                drawAlgorithm = ::draw1
                println("Switched to RED")
            }

            Key.KEY_D -> {
                drawAlgorithm = ::draw2
                println("Switched to BLACK")
            }
            Key.KEY_Q -> {
                pixelSize++
                println("Increased pixel size to: $pixelSize")
            }

            Key.KEY_A -> {
                pixelSize--
                println("Decreased pixel size to: $pixelSize")
            }

            Key.KEY_W -> {
                maxColors++
                println("Increased max colors to: $maxColors")
            }

            Key.KEY_S -> {
                maxColors--
                println("Decreased max colors to: $maxColors")
            }

            Key.KEY_Z -> ditherAlgorithm = { b -> ditherAtkinson(b, pixelSize, maxColors) }
            Key.KEY_X -> ditherAlgorithm = { b -> ditherBurkes(b, pixelSize, maxColors) }
            Key.KEY_C -> ditherAlgorithm = { b -> ditherFloydSteinberg(b, pixelSize, maxColors) }
            Key.KEY_V -> ditherAlgorithm = { b -> ditherJarvisJudiceNinke(b, pixelSize, maxColors) }
            Key.KEY_B -> ditherAlgorithm = { b -> ditherOrdered2By2Bayer(b, pixelSize, maxColors) }
            Key.KEY_N -> ditherAlgorithm = { b -> ditherOrdered3By3Bayer(b, pixelSize, maxColors) }
            Key.KEY_M -> ditherAlgorithm = { b -> ditherOrdered4By4Bayer(b, pixelSize, maxColors) }
            Key.KEY_L -> ditherAlgorithm = { b -> ditherOrdered8By8Bayer(b, pixelSize, maxColors) }
            Key.KEY_K -> ditherAlgorithm = { b -> ditherSierra(b, pixelSize, maxColors) }
            Key.KEY_J -> ditherAlgorithm = { b -> ditherSierraLite(b, pixelSize, maxColors) }
            Key.KEY_H -> ditherAlgorithm = { b -> ditherStucki(b, pixelSize, maxColors) }
            Key.KEY_G -> ditherAlgorithm = { b -> ditherTwoRowSierra(b, pixelSize, maxColors) }

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
