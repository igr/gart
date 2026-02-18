package dev.oblac.gart.bubbles.stripe2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.PalettesOf4
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndf
import dev.oblac.gart.pack.simpleCirclePacker
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathEffect

fun main() {
    val gart = Gart.of("bubble-stripe-2", 1024 * GOLDEN_RATIO, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = MyDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw)//.hotReload(g)
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private val pal = PalettesOf4.q19

private fun draw(c: Canvas, d: Dimension) {
    c.clear(BgColors.obsidian)

    val r = d.rect
    val box = d.rect.shrink(40f)

    val path = PathBuilder()
    val templates = simpleCirclePacker(
        r,
        attempts = 100_000,
        minRadius = 60f, maxRadius = 180f,
        growth = 1,
        padding = 10,
        isInside = { it.isInsideOf(box) }
    )
    templates.forEach {
        path.addCircle(it)
    }
    c.clipPath(path.detach(), ClipMode.INTERSECT, true)

    val bubbles = simpleCirclePacker(
        r,
        attempts = 100_000,
        minRadius = 20f, maxRadius = 160f,
        growth = 1,
        padding = 10,
        isInside = { it.isInsideOf(box) }
    )
    bubbles.forEach {
        val color = pal.random()
        val circlePoints = it.points(200)
        repeat(40) { i ->
            val cp = deformPath(circlePoints, 10f + i * 6f)
            c.drawPath(cp.toPath(), fillOf(color).apply {
                this.alpha = 20
            })
        }
    }

    templates
        .map { it.scale(0.9f) }
        .forEach { circle ->
            repeat(10) {
                val circ = circle.resize(circle.radius + it)
                c.drawCircle(circ, strokeOf(2f, BgColors.obsidian).apply {
                    this.pathEffect = PathEffect.makeDash(
                        floatArrayOf(rndf(80, 120), rndf(40, 80)), rndf()
                    )
                    this.alpha = 200
                })
            }
        }
}
