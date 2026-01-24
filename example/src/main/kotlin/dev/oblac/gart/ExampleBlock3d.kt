package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.gfx.drawPoly4
import dev.oblac.gart.perspective.Block3D
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("exampleBlock3d", 800, 800)
    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    c.clear(Colors.white)

    val pal = Palettes.cool5

    // === BOTTOM ROW: blocks with TOP face visible ===
    // Vanishing points at middle height - blocks below horizon show top face
    val vpLeftBottom = Point(-200f, d.hf / 2 - 200f)
    val vpRightBottom = Point(d.wf + 200f, d.hf / 2 + 100f)

    val bottomBlocks = mutableListOf<Block3D>()
    for (i in 0 until 5) {
        val x = 150f + i * 120f
        val height = 80f + i * 30f
        val block = Block3D.of(
            vpLeft = vpLeftBottom,
            vpRight = vpRightBottom,
            frontBottom = Point(x, d.hf - 107f),
            height = height,
            leftWidth = 40f,
            rightWidth = 40f
        )
        bottomBlocks.add(block)
    }

    // Sort and draw bottom blocks (back to front)
    val sortedBottomBlocks = bottomBlocks.sortedByDescending { it.left.topPoint().y }
    sortedBottomBlocks.forEachIndexed { index, block ->
        c.drawPoly4(block.left, pal.safe(index).toFillPaint())
        c.drawPoly4(block.right, pal.safe(index + 3).toFillPaint())
        block.horizontalFace()?.let { c.drawPoly4(it, pal.safe(index + 6).toFillPaint()) }
    }

    // === TOP ROW: blocks with BOTTOM face visible ===
    // Vanishing points below the blocks - blocks above horizon show bottom face
    val vpLeftTop = Point(-200f, d.hf + 267f)
    val vpRightTop = Point(d.wf + 200f, d.hf + 267f)

    val topBlocks = mutableListOf<Block3D>()
    for (i in 0 until 5) {
        val x = 150f + i * 120f
        val height = 80f + i * 30f
        val block = Block3D.of(
            vpLeft = vpLeftTop,
            vpRight = vpRightTop,
            frontBottom = Point(x, 120f + height),  // frontBottom is at bottom of block
            height = height,
            leftWidth = 40f,
            rightWidth = 40f
        )
        topBlocks.add(block)
    }

    // Sort and draw top blocks by height (tallest first)
    val sortedTopBlocks = topBlocks.sortedByDescending { it.left.a.y - it.left.b.y }
    sortedTopBlocks.forEachIndexed { index, block ->
        c.drawPoly4(block.left, pal.safe(index + 10).toFillPaint())
        c.drawPoly4(block.right, pal.safe(index + 13).toFillPaint())
        block.horizontalFace()?.let { c.drawPoly4(it, pal.safe(index + 16).toFillPaint()) }
    }

    w.showImage(g)
}
