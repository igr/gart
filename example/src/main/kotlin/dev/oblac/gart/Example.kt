package dev.oblac.gart

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.noise.HaltonSequenceGenerator
import dev.oblac.gart.pixels.pixelSorter
import dev.oblac.gart.util.loadResourceAsImage
import org.jetbrains.skia.Rect


fun main() {
    val gart = Gart.of("example", 662, 600)
    println(gart)

    // ✅ second canvas

    val d2 = Dimension(10, 10)
    val g2 = Gartvas(d2)

    // ✅ draw on canvas #2
    g2.canvas.drawCircle(5f, 5f, 5f, fillOf(CssColors.coral))
    val snapshot2 = g2.snapshot()

    // ✅ main canvas

    val d1 = Dimension(662, 600)
    val g1 = Gartvas(d1)

    // ✅ draw on canvas #1
    g1.draw { c, d ->
        c.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(0xFF174185))
        c.drawCircle(d.w / 2f, d.h / 2f, 30f, fillOfRed())
        c.drawImage(snapshot2, 30f, 30f)
        c.drawImage(snapshot2, 30f, 50f)
    }

    // ✅ bitmap #1

    val b = Gartmap(g1)
    println(b[0, 0].toHexString())

    b.forEach { x, y, v ->
        if (v == 0xFFFF0000.toInt()) {  // red detected
            if ((x + y).mod(2) == 0) {
                b[x, y] = 0xFFFFFF00
            }
        }
    }

    // add bitmap noise

    val halton = HaltonSequenceGenerator(2)
    repeat(10_000) {
        halton.get().toList().zipWithNext().forEach {
            val (x, y) = it
            val x1 = (x * b.d.w).toInt()
            val y1 = (y * b.d.h).toInt()
            b[x1, y1] = CssColors.black
        }
    }

    // draw a line and a dot
    for (x in 0 until b.d.w) {
        b[x, 0] = 0xFFFF0044
    }
    b[0, 0] = 0xFF00FF00

    // row
    val rowSix = b.row(50)
    for (i in 0 until 662) {
        rowSix[i] = CssColors.navy
    }
    b.row(50, rowSix)

    // column
    val columnSix = b.column(50)
    for (i in 0 until 600) {
        columnSix[i] = CssColors.navy
    }
    b.column(50, columnSix)

    // ✅ draw bitmap back to canvas
    b.drawToCanvas()

    // ✅ load sprite
    val sprite = loadResourceAsImage("/nature.jpg")
    g1.canvas.drawImage(sprite, 0f, 350f)

    // ✅ pixel sorter
    //b.updatePixelsFromCanvas()
    //pixelSortWithThreshold(b)
    //b.drawToCanvas()

    // ✅ THE END, save

    gart.saveImage(g1)

    // ✅ show image
    gart.window().showImage(g1)
}


fun pixelSortWithThreshold(bitmap: Pixels, brightnessThreshold: Int = -10) {
    pixelSorter(bitmap, brightnessThreshold) { color -> brightness(color) }
}

// Helper function to calculate brightness
private fun brightness(color: Int): Int {
    val r = red(color)
    val g = green(color)
    val b = blue(color)
    return (0.299 * r + 0.587 * g + 0.114 * b).toInt() // Luma formula
}
