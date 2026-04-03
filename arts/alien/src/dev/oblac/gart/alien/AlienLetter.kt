package dev.oblac.gart.alien

import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.ofXYWH
import dev.oblac.gart.gfx.toRRect
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.Rect

private enum class Variant {
    V1, V2, V3
}

fun main() {
    val variant = Variant.V3

    val gart = Gart.of("alien-letters-" + variant.toString().lowercase(), 1024 * GOLDEN_RATIO, 1024, 1)
    val g = gart.gartvas()
    val w = gart.window()
    val c = g.canvas

    println(gart)

    val a = 120
    val gap = 80
    val ww = 4
    val hh = 4

    val fullWidth = a * ww + gap * (ww - 1)
    val fullHeight = a * hh + gap * (hh - 1)

    val x0 = (gart.d.wf - fullWidth) / 2
    val y0 = (gart.d.hf - fullHeight) / 2
    val rect = Rect.ofXYWH(x0, y0, fullWidth.toFloat(), fullHeight.toFloat())

    val letterBoxes = tileRect(rect, ww, hh, gap)

    // VARIANTs
    val backgroundColor = when (variant) {
        Variant.V1 -> NipponColors.col198_RURIKON
        Variant.V2 -> NipponColors.col161_MUSHIAO
        Variant.V3 -> 0xFF2D2926.toInt()
    }
    val palette = when (variant) {
        Variant.V1 -> Palette(intArrayOf(NipponColors.col231_TSUTSUJI))
        Variant.V2 -> Palette(intArrayOf(NipponColors.col213_HASHITA))
        Variant.V3 -> Palettes.cool10
    }
    val round = when (variant) {
        Variant.V1 -> false
        Variant.V2 -> true
        Variant.V3 -> false
    }
    val minRound = when (variant) {
        Variant.V1 -> 0f
        Variant.V2 -> 0f
        Variant.V3 -> 2f
    }
    val letterCounts = when (variant) {
        Variant.V1 -> 3
        Variant.V2 -> 4
        Variant.V3 -> 5
    }

    // DRAW
    val cells = letterBoxes.flatMap { letterfy(it, letterCounts) }
    c.clear(backgroundColor)
    cells.forEach {
        c.drawRRect(it.toRRect(if (round) it.width / 2 else minRound), fillOf(palette.random()))
    }
    gart.saveImage(g)
    w.showImage(g)
}

private fun tileRect(r: Rect, wCount: Int, hCount: Int, gap: Int): Array<Rect> {
    val width = (r.width - (wCount - 1) * gap) / wCount
    val height = (r.height - (hCount - 1) * gap) / hCount

    val rects = mutableListOf<Rect>()
    for (i in 0 until wCount) {
        for (j in 0 until hCount) {
            val x = r.left + i * (width + gap)
            val y = r.top + j * (height + gap)
            rects.add(Rect.ofXYWH(x, y, width, height))
        }
    }
    return rects.toTypedArray()
}

private fun letterfy(r: Rect, letterCounts: Int = 3): List<Rect> {
    return tileRect(r, letterCounts, letterCounts, 10).mapNotNull {
        val i = rndi(0, 4)
        when (i) {
            0 -> null
            1 -> expand(it, 10)
            else -> it
        }
    }
}

private fun expand(rect: Rect, gap: Int): Rect {
    return when (rndi(4)) {
        0 -> return Rect.ofXYWH(rect.left - gap - rect.width, rect.top, rect.width * 2 + gap, rect.height)
        1 -> return Rect.ofXYWH(rect.left, rect.top - gap - rect.height, rect.width, rect.height * 2 + gap)
        2 -> return Rect.ofXYWH(rect.left, rect.top, rect.width * 2 + gap, rect.height)
        3 -> return Rect.ofXYWH(rect.left, rect.top, rect.width, rect.height * 2 + gap)
        else -> rect
    }
}
