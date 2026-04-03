package dev.oblac.gart.bubbles

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartvas
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Rect

const val name2 = "Bubbles2"

fun main() {
    println(name2)

    val d = Dimension(1024, 1024)
    val g = Gartvas(d)

    val list = mutableListOf<Bubble>()
    val maxR = d.w / 6

    g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOfWhite())
    var tries = 10000
    while (tries-- > 0) {
        val x = rndf(d.w)
        val y = rndf(d.h)

        // is it valid?

        if (!list.stream().anyMatch { it.contains(x, y) }) {
            // new dot
            var newBubble = Bubble(d, x, y, 1f, 0, strokeOfBlack(2f))
            while (true) {
                if (newBubble.r >= maxR) break
                if (list.stream().anyMatch { newBubble.collide(it) }) break
                if (newBubble.pushedByLeft() != null) break
                if (newBubble.pushedByRight() != null) break
                if (newBubble.pushedByDown() != null) break
                if (newBubble.pushedByUp() != null) break

                newBubble = newBubble.grow()
            }
            list.add(newBubble)
        }
    }

    list.forEach {
        g.canvas.drawCircle(it.x, it.y, it.r, it.paint)
    }

    gart.saveImage(g, "$name2.png")
}
