package dev.oblac.gart

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.space.RGBA.Companion.BLACK
import dev.oblac.gart.color.space.RGBA.Companion.CYAN
import dev.oblac.gart.color.space.RGBA.Companion.MAGENTA
import dev.oblac.gart.color.space.RGBA.Companion.YELLOW
import dev.oblac.gart.gfx.draw
import dev.oblac.gart.gfx.drawImage
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.pixels.halftone.*
import dev.oblac.gart.util.loadResourceAsImage
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Shader

/**
 * Example demonstrating the halftone bird effect.
 */
private var flag = false
private var processAlgorithm: (Gartmap) -> Unit = {}
private var drawAlgorithm: (c: Canvas, d: Dimension) -> Unit = ::draw1
private var dotSize = 10
private var dotResolution = 5

// CMYK angle variables
private var yellowAngle = 2.0f
private var cyanAngle = 15.0f
private var magentaAngle = 75.0f
private var keyAngle = 45.0f

private val corgi = loadResourceAsImage("/corgi.png")

fun main() {
    val gart = Gart.of("exampleHalftone", 1024, 1024)
    val g = gart.gartvas()
    val b = Gartmap(g)

    gart.window().show { c, d, _ ->
        drawAlgorithm(g.canvas, d)
        b.updatePixelsFromCanvas()
        processAlgorithm(b)
        b.drawToCanvas()
        c.draw(g)
    }.onKey {
        when (it) {
            Key.KEY_0 -> {
                processAlgorithm = {}
                println("Switched to NO PROCESSING")
            }
            Key.KEY_9 -> {
                processAlgorithm = if (flag) ::defaultHalftone else ::defaultHalftone2
                flag = !flag
                println("Switched to HALFTONE effect")
            }
            Key.KEY_8 -> {
                processAlgorithm = ::halftoneYellow
            }
            Key.KEY_7 -> {
                processAlgorithm = ::halftoneCyan
            }
            Key.KEY_6 -> {
                processAlgorithm = ::halftoneMagenta
            }
            Key.KEY_5 -> {
                processAlgorithm = ::halftoneKey
            }
            Key.KEY_4 -> {
                processAlgorithm = ::halftoneJoin
            }


            Key.KEY_1 -> {
                drawAlgorithm = ::draw1
                println("Switched to RED gradient")
            }
            Key.KEY_2 -> {
                drawAlgorithm = ::draw2
                println("Switched to BLACK gradient")
            }
            Key.KEY_3 -> {
                drawAlgorithm = ::draw3
                println("Switched to TEST pattern")
            }



            Key.KEY_W -> {
                dotSize++
                println("Increased dot size to: $dotSize")
            }

            Key.KEY_S -> {
                dotSize = (dotSize - 1).coerceAtLeast(1)
                println("Decreased dot size to: $dotSize")
            }
            
            Key.KEY_Q -> {
                dotResolution++
                println("Increased dot resolution to: $dotResolution")
            }

            Key.KEY_A -> {
                dotResolution = (dotResolution - 1).coerceAtLeast(1)
                println("Decreased dot resolution to: $dotResolution")
            }
            
            // Yellow angle controls
            Key.KEY_P -> {
                yellowAngle += 5.0f
                println("Yellow angle: $yellowAngle")
            }
            Key.KEY_L -> {
                yellowAngle -= 5.0f
                println("Yellow angle: $yellowAngle")
            }
            
            // Cyan angle controls
            Key.KEY_O -> {
                cyanAngle += 5.0f
                println("Cyan angle: $cyanAngle")
            }
            Key.KEY_K -> {
                cyanAngle -= 5.0f
                println("Cyan angle: $cyanAngle")
            }
            
            // Magenta angle controls
            Key.KEY_I -> {
                magentaAngle += 5.0f
                println("Magenta angle: $magentaAngle")
            }
            Key.KEY_J -> {
                magentaAngle -= 5.0f
                println("Magenta angle: $magentaAngle")
            }
            
            // Key (black) angle controls
            Key.KEY_U -> {
                keyAngle += 5.0f
                println("Key angle: $keyAngle")
            }
            Key.KEY_H -> {
                keyAngle -= 5.0f
                println("Key angle: $keyAngle")
            }

            else -> {}
        }
    }
    
    println("Controls:")
    println("Z - Apply halftone effect")
    println("X - No processing")
    println("1 - Red gradient")
    println("2 - Black gradient")
    println("3 - Test pattern")
    println("4 - Join")
    println("5-8 - Channels")
    println("9 - Halftone ON")
    println("0 - Halftone OFF")
    println("W/S - Increase/decrease dot size")
    println("Q/A - Increase/decrease dot resolution")

    println("P/L - Increase/decrease Yellow angle")
    println("O/K - Increase/decrease Cyan angle")
    println("I/J - Increase/decrease Magenta angle")
    println("U/H - Increase/decrease Key (black) angle")
}

private fun config() = HalftoneConfiguration(
    dotSize = dotSize,
    dotResolution = dotResolution,
    yellowAngle,
    cyanAngle,
    magentaAngle,
    keyAngle,
)

private fun halftoneJoin(gartmap: Gartmap) {
    val yellowChannel = extractColorChannel(gartmap, ColorChannel.YELLOW)
    val magentaChannel = extractColorChannel(gartmap, ColorChannel.MAGENTA)
    val cyanChannel = extractColorChannel(gartmap, ColorChannel.CYAN)
    val keyChannel = extractColorChannel(gartmap, ColorChannel.KEY)

    val mem = MemPixels(gartmap.d)
    joinGrayscaleCMYKChannels(cyanChannel, magentaChannel, yellowChannel, keyChannel, mem)
    gartmap.copyPixelsFrom(mem)
}

private fun defaultHalftone2(gartmap: Gartmap) {
    val config = config()
    val result = halftoneProcess(gartmap, config)
    gartmap.copyPixelsFrom(result)
}

private fun defaultHalftone(gartmap: Gartmap) {
    val config = config()
    val mem = processHalftoneWithJoin(
        gartmap,
        config = config,
    )
    gartmap.copyPixelsFrom(mem)
}

private fun halftoneYellow(gartmap: Gartmap) {
    val config = config()
    val yellowChannel = extractColorChannel(gartmap, ColorChannel.YELLOW)
    val mem = MemPixels(gartmap.d)
    renderHalftone(
        yellowChannel, mem,
        angle = yellowAngle,
        dotSize = config.dotSize,
        dotResolution = config.dotResolution,
        color = YELLOW
    )
    gartmap.copyPixelsFrom(mem)
}

private fun halftoneCyan(gartmap: Gartmap) {
    val config = config()
    val cyanChannel = extractColorChannel(gartmap, ColorChannel.CYAN)
    val mem = MemPixels(gartmap.d)
    renderHalftone(
        cyanChannel, mem,
        angle = cyanAngle,
        dotSize = config.dotSize,
        dotResolution = config.dotResolution,
        color = CYAN
    )
    gartmap.copyPixelsFrom(mem)
}

private fun halftoneMagenta(gartmap: Gartmap) {
    val config = config()
    val magentaChannel = extractColorChannel(gartmap, ColorChannel.MAGENTA)
    val mem = MemPixels(gartmap.d)
    renderHalftone(
        magentaChannel, mem,
        angle = magentaAngle,
        dotSize = config.dotSize,
        dotResolution = config.dotResolution,
        color = MAGENTA
    )
    gartmap.copyPixelsFrom(mem)
}

private fun halftoneKey(gartmap: Gartmap) {
    val config = config()
    val keyChannel = extractColorChannel(gartmap, ColorChannel.KEY)
    val mem = MemPixels(gartmap.d)
    renderHalftone(
        keyChannel, mem,
        angle = magentaAngle,
        dotSize = config.dotSize,
        dotResolution = config.dotResolution,
        color = BLACK
    )
    gartmap.copyPixelsFrom(mem)
}

private fun draw1(c: Canvas, d: Dimension) {
    c.drawImage(corgi)
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
    c.clear(CssColors.white)
    for (i in 0..3) {
        val x = d.w / 4 * i + d.w / 8
        val color = when (i) {
            0 -> CssColors.red
            1 -> CssColors.green
            2 -> CssColors.blue
            else -> CssColors.yellow
        }
        c.drawCircle(x.toFloat(), d.cy, 100f, fillOfRed().apply {
            this.color = color
        })
    }
}
