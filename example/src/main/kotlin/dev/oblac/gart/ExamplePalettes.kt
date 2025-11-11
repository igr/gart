package dev.oblac.gart

import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.ofXYWH
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

private val coolPalette: (Int) -> Palette = { Palettes.coolPalette(it) }
private val mixPalette: (Int) -> Palette = { Palettes.mixPalette(it) }
private val colormapPalette: (Int) -> Palette = { Palettes.colormapPalette(it) }

private fun paletteOf(type: Int) = when (type) {
    1 -> coolPalette
    2 -> mixPalette
    3 -> colormapPalette
    else -> throw IllegalArgumentException("Unknown palette type: $type")
}

fun main() {
    val gart = Gart.of("example palettes", 1024, 1024)
    println(gart)

    val w = gart.window()
    var index = 1
    var horizontal = false
    var reverse = false
    var type = 1

    w.show { c, d, f ->
        val p = if (reverse) {
            paletteOf(type)(index).reversed()
        } else {
            paletteOf(type)(index)
        }
        if (horizontal) {
            drawPaletteL2R(c, d, p)
        } else {
            drawPaletteT2B(c, d, p)
        }
    }.onKey {
        when (it) {
            Key.KEY_W -> {
                index--
            }

            Key.KEY_S -> {
                index++
            }

            Key.KEY_Q -> {
                horizontal = !horizontal
            }

            Key.KEY_E -> {
                reverse = !reverse
            }

            Key.KEY_SPACE -> {
                type++
                if (type == 4) {
                    type = 1
                }
                index = 1
            }

            else -> {}
        }
        val typeString = when (type) {
            1 -> "cool"
            2 -> "mix"
            3 -> "colormap"
            else -> throw IllegalArgumentException("Unknown palette type: $type")
        }
        println("$typeString[$index]")
    }
}

// draw rectangles of the palette, left to right
fun drawPaletteL2R(c: Canvas, d: Dimension, p: Palette) {
    val w = d.wf / p.size
    val h = d.hf
    for (i in p.indices) {
        val color = p[i]
        c.drawRect(
            Rect.ofXYWH(i * w, 0f, w, h), fillOf(color)
        )
    }
}

fun drawPaletteT2B(c: Canvas, d: Dimension, p: Palette) {
    val w = d.wf
    val h = d.hf / p.size
    for (i in p.indices) {
        val color = p[i]
        c.drawRect(
            Rect.ofXYWH(0f, i * h, w, h), fillOf(color)
        )
    }
}
