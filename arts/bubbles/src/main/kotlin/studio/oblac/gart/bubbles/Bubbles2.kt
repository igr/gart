package studio.oblac.gart.bubbles

import studio.oblac.gart.Dimension
import studio.oblac.gart.Gartvas
import studio.oblac.gart.gfx.fillOfWhite
import studio.oblac.gart.gfx.strokeOfBlack
import studio.oblac.gart.math.nextFloat
import studio.oblac.gart.skia.Rect

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
        val x = nextFloat(d.w)
        val y = nextFloat(d.h)

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

    g.writeSnapshotAsImage("$name2.png")
}
