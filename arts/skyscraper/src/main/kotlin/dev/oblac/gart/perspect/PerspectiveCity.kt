package dev.oblac.gart.perspect

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.drawPoly4
import dev.oblac.gart.math.doubleLoopSequence
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import dev.oblac.gart.perspective.Block3D
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("perspective", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = PerspectiveDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class PerspectiveDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

// 32! 29! 24!
private val pal = Palettes.cool24

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.teal01)

    val vpLeft1 = Point(-500f, 900f)
    val vpRight1 = Point(1500f, 900f)
    val vpLeft2 = Point(-600f, 1200f)
    val vpRight2 = Point(1400f, 1100f)

    val topPaint = Colors.transparent.toFillPaint()

    val blocks = doubleLoopSequence(3, 3)
        .map { (x, y) ->
            var frontBottom = Point(100f + x * 300f + y * 200f, 1200f - x * 200f + y * 200f)
            if (frontBottom.y < d.hf) {
                frontBottom = Point(frontBottom.x, d.hf)    // make sure the bottom point is not on screen
            }
            Block3D.of(
                vpLeft = if (rndb()) vpLeft1 else vpLeft2,
                vpRight = if (rndb()) vpRight1 else vpRight2,
                frontBottom = frontBottom,
                height = rndf(700f, 900f),
                leftWidth = rndf(150f, 250f),
                rightWidth = rndf(100f, 200f)
            )
        }
        .sortedBy { it.left.topPoint().y }  // move the highest to the back, drawn first

    blocks.forEachIndexed { index, block ->
        // Draw the faces
        block.left.let { c.drawPoly4(it, pal.safe(index).toFillPaint()) }
        block.right.let { c.drawPoly4(it, pal.safe(index + 3).toFillPaint()) }
        block.horizontalFace()?.let { c.drawPoly4(it, topPaint) }

        if (rndb(8,10)) {
            val window = block.left.shrink(0.3f).move(0f, rndf(-50f, -150f))
            c.drawPoly4(window, RetroColors.black01.toFillPaint())
        }
        if (rndb(8,10)) {
            val window = block.right.shrink(0.3f).move(0f, rndf(-50f, -150f))
            c.drawPoly4(window, RetroColors.black01.toFillPaint())
        }
    }

//    val block = Block3D.of(
//        vpLeft = vpLeft,
//        vpRight = vpRight,
//        frontBottom = Point(500f, 1200f),
//        height = 800f,
//        leftWidth = 180f,
//        rightWidth = 120f
//    )
//
//    // Draw the faces
//    c.drawPoly4(block.left, leftPaint)
//    c.drawPoly4(block.right, rightPaint)
//    c.drawPoly4(block.horizontalFace(), topPaint)
}
