package dev.oblac.gart.sixsix

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.PalettesOf4
import dev.oblac.gart.math.doubleLoop
import dev.oblac.gart.sixsix.Cell.CellColor
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import java.util.*

fun main() {
    val gart = Gart.of("sixsix", 1080, 1080)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw3(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
//        val b = Gartmap(g)
        draw(g.canvas, g.d)
//        b.updatePixelsFromCanvas()
//        ditherSierra(b, 10)
//        b.drawToCanvas()
    }
}

//private val pal4 = PalettesOf4.q01
private val pal4 = PalettesOf4.q16
//private val pal4 = PalettesOf4.q18
//private val pal4 = PalettesOf4.q19

private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.white)

    val cellWidth = d.wf / 6f
    val cellHeight = d.hf / 6f
    val allCells = cells.indices.map {
        Cell(
            ndx = it,
            colors = CellColor.entries.shuffled(),
            rotation = Cell.Rotation.entries.random()
        )
    }.shuffled()

    val bitset = BitSet(16 * 6 * 6)
    doubleLoop(6, 6) { (row, col) ->
        val index = (row * 6 + col) % allCells.size
        val cell = allCells[index]
        val cellImage = cell(Dimension.of(cellWidth, cellHeight))
        val x = col * cellWidth
        val y = row * cellHeight
        c.drawImage(cellImage, x, y)
        //c.drawWhiteText("${index+1}", x + 10f, y + 20f)
        val cellBits = cell.pack()
        for (i in 0 until 16) {
            bitset.set((row * 6 + col) * 16 + i, cellBits[i])
        }
    }
    // print based64 of bitset
    val byteArray = bitset.toByteArray()
    val base64 = Base64.getEncoder().encodeToString(byteArray)
    println("#: $base64")
}

private data class Cell(
    val ndx: Int = 0,
    val colors: List<CellColor>,
    val rotation: Rotation = Rotation.R0,
) {
    init {
        require(ndx < 64)
        require(ndx in cells.indices)
        require(colors.size == 4)
    }
    enum class Rotation(val degrees: Float) {
        R0(0f), R90(90f), R180(180f), R270(270f);
        fun toByte(): Byte = this.ordinal.toByte()
    }
    enum class CellColor {
        C0, C1, C2, C3;
        fun toByte() = this.ordinal.toByte()
    }

    private val draw: (Gartvas, Palette) -> Unit = cells[ndx]

    operator fun invoke(d: Dimension): Image {
        val gartvas = Gartvas(d)
        val p = colors
            .map { it.ordinal }
            .map { pal4[it] }
            .toList()
            .let { Palette.of(it) }

        val c = gartvas.canvas
        c.rotate(rotation.degrees, d.cx, d.cy)
        draw(gartvas, p)
        return gartvas.snapshot()
    }

    fun pack(): BitSet {
        val bitset = BitSet()
        // Convert ndx to 6 bits (0-64)
        for (i in 0 until 6) {
            bitset.set(i, (ndx and (1 shl i)) != 0)
        }
        // Convert colors to bits (2 bits per color, 4 colors = 8 bits)
        var bitIndex = 6
        for (color in colors) {
            val colorValue = color.toByte().toInt()
            for (i in 0 until 2) {
                bitset.set(bitIndex++, (colorValue and (1 shl i)) != 0)
            }
        }
        // Convert rotation to 2 bits
        val rotationValue = rotation.toByte().toInt()
        for (i in 0 until 2) {
            bitset.set(bitIndex++, (rotationValue and (1 shl i)) != 0)
        }
        return bitset
    }
}
