package ac.obl.gart.bubbles

import ac.obl.gart.Box
import ac.obl.gart.Gartvas
import ac.obl.gart.ImageWriter
import ac.obl.gart.gfx.fillOfWhite
import ac.obl.gart.gfx.strokeOfBlack
import ac.obl.gart.skia.Rect
import kotlin.random.Random

const val name2 = "Bubbles2"

fun main() {
    println(name2)

    val box = Box(1024, 1024)
    val g = Gartvas(box)

    val list = mutableListOf<Bubble>()
    val maxR = box.w / 6

    g.canvas.drawRect(Rect(0f, 0f, box.wf, box.hf), fillOfWhite())
    var tries = 10000
    while (tries-- > 0) {
        val x = Random.nextInt(box.w).toFloat()
        val y = Random.nextInt(box.h).toFloat()

        // is it valid?

        if (!list.stream().anyMatch{ it.contains(x, y) }) {
            // new dot
            var newBubble = Bubble(box, x, y, 1f, 0, strokeOfBlack(2f))
            while(true) {
                if (newBubble.r >= maxR) break
                if (list.stream().anyMatch{newBubble.collide(it)}) break
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

    ImageWriter(g).save("$name2.png")
}
