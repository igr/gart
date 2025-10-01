package dev.oblac.gart.bubbles.bubbub

import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.pack.simpleCirclePacker

val fillBack = fillOf(BgColors.elegantDark)
//val pal = Palettes.cool24
//val pal = Palettes.cool28
val pal = Palettes.cool35

fun main() {
    val gart = Gart.of("bubbub", 1024, 1024)

    val d = gart.d
    val g = gart.gartvas()
    val w = gart.window()
    val c = g.canvas

    c.clear(fillBack.color)

    fun List<Circle>.bubblesOverlap(circle: Circle): Boolean = this.none { circle.center.distanceTo(it.center) < (circle.radius + it.radius + 10) }

    val r = d.rect
    val box = d.rect.shrink(40f)
    val bubbles1 = simpleCirclePacker(r,
        attempts = 100_000,
        minRadius = 80f, maxRadius = 90f,
        growth = 1,
        padding = 10,
        isInside = { it.isInsideOf(box) }
    )
    bubbles1.forEach {
        val color = pal.random()
        c.drawCircle(it, fillOf(color))

        val r1 = it.rect().shrink(1f)
        simpleCirclePacker(r1,
            attempts = 10_000,
            minRadius = 20f, maxRadius = 60f,
            growth = 1,
            padding = 10,
            isInside = { cc -> cc.isInsideOf(it) }
        ).forEach { cc ->
            val color1 = pal.randomExclude(color)
            c.drawCircle(cc, fillOf(color1))

            if (cc.radius < 30) return@forEach
            val r2 = cc.rect().shrink(1f)
            simpleCirclePacker(r2,
                attempts = 10_000,
                minRadius = 5f, maxRadius = 30f,
                growth = 1,
                padding = 10,
                isInside = { ccc -> ccc.isInsideOf(cc) }
            ).forEach { ccc ->
                val color2 = pal.randomExclude(color, color1)
                c.drawCircle(ccc, fillOf(color2))
            }
        }
    }

    val bubbles2 = simpleCirclePacker(r,
        attempts = 1_000_000,
        minRadius = 40f, maxRadius = 80f,
        growth = 5,
        padding = 10,
        isInside = { it.isInsideOf(box) && bubbles1.bubblesOverlap(it) }
    )
    bubbles2.forEach {
        val color = pal.random()
        c.drawCircle(it, fillOf(color))

        val r1 = it.rect().shrink(1f)
        simpleCirclePacker(r1,
            attempts = 10_000,
            minRadius = 5f, maxRadius = 40f,
            growth = 1,
            padding = 10,
            isInside = { cc -> cc.isInsideOf(it) }
        ).forEach { cc ->
            val color1 = pal.randomExclude(color)
            c.drawCircle(cc, fillOf(color1))
        }
    }

    val bubbles3 = simpleCirclePacker(r,
        attempts = 1_000_000,
        minRadius = 10f, maxRadius = 40f,
        growth = 5,
        padding = 10,
        isInside = { it.isInsideOf(box) && bubbles1.bubblesOverlap(it) && bubbles2.bubblesOverlap(it) }
    )
    bubbles3.forEach {
        val color = pal.random()
        c.drawCircle(it, fillOf(color))
    }

    c.drawBorder(d, strokeOf(fillBack.color, 40f))

    gart.saveImage(g)
    w.showImage(g)
}
