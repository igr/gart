package dev.oblac.gart.updown

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndf
import dev.oblac.gart.perspective.Block3D
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("dualcity", 1024 * GOLDEN_RATIO, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val rand = gart.rand(false)

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
        drawBack(g.canvas, g.d)
        cityDown(g.canvas, g.d)
        cityUp(g.canvas, g.d)
    }
}

private val pals = Palette.of(RetroColors.black01, RetroColors.white01).expand(100)
private val palr = pals.reversed()

private fun drawBack(c: Canvas, d: Dimension) {
    // Draw horizontal gradient lines from top (black) to bottom (white)
    for (y in 0..d.h) {
        val yf = y.toFloat()
        val colorIndex = (y * (pals.size - 1) / d.h)
        val stroke = strokeOf(pals.safe(colorIndex), 1f)
        c.drawLine(0f, yf, d.wf, yf, stroke)
    }
    c.drawCircle(d.center, 160f, fillOf(RetroColors.red01))
}

/**
 * Draws exponential curves on both sides of the Y axis (center of screen).
 * Highest at center, decreasing exponentially toward the edges.
 */
private fun cityDown(c: Canvas, d: Dimension) {
    val centerX = d.wf / 2
    val bottomY = d.hf

    val maxHeight = 500f  // Maximum height at center
    val k = 0.004f        // Controls how fast the curve falls off

    val vpLeft1 = Point(-500f, 1200f)
    val vpRight1 = Point(1500f, 1200f)

    val list = mutableListOf<Block3D>()
    for (x in 0..d.w step 80) {
        val xf = x.toFloat()
        val distFromCenter = kotlin.math.abs(xf - centerX)

        // Exponential decay: highest at center, decreasing outward
        val height = maxHeight * kotlin.math.exp((-distFromCenter * k).toDouble()).toFloat()

        //c.drawLine(xf, bottomY, xf, bottomY - height, stroke)

        val frontBottom = Point(xf - 10f, bottomY)
        val block = Block3D.of(
            vpLeft = vpLeft1,
            vpRight = vpRight1,
            frontBottom = frontBottom,
            leftWidth = rndf(50f, 80f),
            height = height * rndf(0.8f, 1.2f),
            rightWidth = rndf(50f, 80f)
        )
        list.add(block)
    }

    list
        .sortedBy { it.left.topPoint().y }
        .forEachIndexed { index, block ->
            // Draw the faces
            block.left.let { c.drawPoly4(it, pals.safe(index).toFillPaint()) }
            block.right.let { c.drawPoly4(it, pals.safe(index + 30).toFillPaint()) }
            block.horizontalFace()?.let { c.drawPoly4(it, pals.safe(index + 5).toFillPaint()) }
        }
}

/**
 * Draws exponential curves on both sides of the Y axis (center of screen).
 * Highest at center, decreasing exponentially toward the edges.
 * Buildings grow from top downward.
 */
private fun cityUp(c: Canvas, d: Dimension) {
    val centerX = d.wf / 2
    val topY = 0f

    val maxHeight = 500f  // Maximum height at center
    val k = 0.004f        // Controls how fast the curve falls off

    // Vanishing points above the screen
    val vpLeft1 = Point(-500f, -200f)
    val vpRight1 = Point(1500f, -200f)

    val list = mutableListOf<Block3D>()
    for (x in 0..d.w step 80) {
        val xf = x.toFloat()
        val distFromCenter = kotlin.math.abs(xf - centerX)

        // Exponential decay: highest at center, decreasing outward
        val height = maxHeight * kotlin.math.exp((-distFromCenter * k).toDouble()).toFloat()

        val frontTop = Point(xf - 10f, topY)
        val block = Block3D.of(
            vpLeft = vpLeft1,
            vpRight = vpRight1,
            frontBottom = frontTop,
            leftWidth = rndf(50f, 80f),
            height = -(height * rndf(0.8f, 1.2f)),  // negative height to grow downward
            rightWidth = rndf(50f, 80f)
        )
        list.add(block)
    }

    list
        .sortedByDescending { it.left.topPoint().y }  // reversed sorting
        .forEachIndexed { index, block ->
            // Draw the faces
            block.left.let { c.drawPoly4(it, palr.safe(index).toFillPaint()) }
            block.right.let { c.drawPoly4(it, palr.safe(index + 30).toFillPaint()) }
            //block.horizontalFace()?.let { c.drawPoly4(it, pals.safe(index + 5).toFillPaint()) }
        }
}
