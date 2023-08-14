package studio.oblac.gart.bubbles

import studio.oblac.gart.Box
import studio.oblac.gart.Gartvas
import studio.oblac.gart.gfx.fillOfWhite
import studio.oblac.gart.gfx.strokeOfBlack
import studio.oblac.gart.skia.Rect
import studio.oblac.gart.writeGartvasAsImage
import kotlin.random.Random

const val name2 = "Bubbles2"

fun main() {
    println(name2)

    val box = Box(1024, 1024)
    val g = Gartvas(box)

    val list = mutableListOf<studio.oblac.gart.bubbles.Bubble>()
    val maxR = box.w / 6

    g.canvas.drawRect(Rect(0f, 0f, box.wf, box.hf), fillOfWhite())
    var tries = 10000
    while (tries-- > 0) {
        val x = Random.nextInt(box.w).toFloat()
        val y = Random.nextInt(box.h).toFloat()

        // is it valid?

        if (!list.stream().anyMatch{ it.contains(x, y) }) {
            // new dot
            var newBubble = studio.oblac.gart.bubbles.Bubble(box, x, y, 1f, 0, strokeOfBlack(2f))
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

    writeGartvasAsImage(g, "$name2.png")
}
