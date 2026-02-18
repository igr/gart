package dev.oblac.gart.bubbles.pebble

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.multiply
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndsgn
import dev.oblac.gart.smooth.catmullRomSpline
import org.jetbrains.skia.*

fun main() {
    val gart = Gart.of("pebble", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw3(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    val pebs = mutableListOf<Path>()
    repeat(800) {
        val dp = randomPoint(d)
        val peb = createPebble(dp, rndf(50f, 150f))
        pebs.none { pathsOverlap(it, peb) }.let { if (it) pebs.add(peb) }
    }

    pebs.forEach {
        c.drawPath(it, fillOf(RetroColors.white01))
        c.save()
        c.rotate(20f, d.cx, d.cy)
        c.clipPath(it)

        // Draw diagonal lines at angle A
        val spacing = 8f // spacing between lines
        val angleRad = Math.toRadians(45.0).toFloat()
        val cos = kotlin.math.cos(angleRad)
        val sin = kotlin.math.sin(angleRad)

        // Calculate how many lines we need to cover the rectangle
        val diagonal = kotlin.math.sqrt((d.w * d.w + d.h * d.h).toDouble()).toFloat()
        val numLines = (diagonal / spacing).toInt() * 2

        for (i in -numLines..numLines) {
            val offset = i * spacing

            // Start point along one edge
            val startX = d.rect.left + offset
            val startY = d.rect.top

            // Calculate line endpoints using rotation
            val dx = cos * diagonal
            val dy = sin * diagonal

            val p1 = Point(startX, startY)
            val p2 = Point(startX + dx, startY + dy)

            val line = Line(p1, p2)
            c.drawLine(line, strokeOf(3f, RetroColors.black01))
        }

        c.restore()
    }
    pebs.forEach {
        repeat(10) { ndx ->
            c.save()
            val last = it.lastPt!!
            //c.translate(-last.x, -last.y)
            // alternative in 0.144.0:
//            target.addPath(
//                it,
//                Matrix33.multiply(
//                    Matrix33.multiply(
//                        Matrix33.makeTranslate(last.x, last.y),
//                        Matrix33.makeScale(1.2f - ndx * 0.01f)
//                    ),
//                    Matrix33.makeTranslate(-last.x, -last.y),
//                )
//            )
            val target = PathBuilder(it).transform(
                Matrix33.multiply(
                    Matrix33.multiply(
                        Matrix33.makeTranslate(last.x, last.y),
                        Matrix33.makeScale(1.2f - ndx * 0.01f)
                    ),
                    Matrix33.makeTranslate(-last.x, -last.y),
                )
            ).detach()
            c.drawPath(target, strokeOf(1.1f, RetroColors.red01).apply {
                //this.pathEffect = PathEffect.makeDash(floatArrayOf(100f + rndf(10f, 20f), rndf(10f, 50f)), rndf(1f, 100f))
            })
            c.restore()
        }
    }
}

private fun createPebble(p: Point, radius1: Float): Path {
    val c1 = Circle(p, radius1).toPath()
    val c2 = Circle(
        p.offset(
            rndf(radius1 / 2, radius1) * rndsgn(), rndf(radius1 / 2, radius1) * rndsgn()
        ), rndf(radius1 / 2, radius1 * 2 / 3)
    ).toPath()
    val c3 = Circle(
        p.offset(
            -rndf(radius1 / 2, radius1) * rndsgn(), rndf(radius1 / 2, radius1) * rndsgn()
        ), rndf(radius1 / 2, radius1 * 2 / 3)
    ).toPath()


    val combined = combinePathsWithOp(PathOp.UNION, c1, c2, c3)
    val ps = combined.toPoints(10)
    return catmullRomSpline(ps, 40)
}
