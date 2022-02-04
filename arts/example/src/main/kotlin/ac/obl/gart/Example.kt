package ac.obl.gart

import ac.obl.gart.gfx.fillOf
import ac.obl.gart.gfx.fillOfRed
import ac.obl.gart.math.GOLDEN_RATIO
import io.github.humbleui.types.Rect

fun main() {
    println("Example")

    val h = 100
    val w = (h * GOLDEN_RATIO).toInt()
    val g = Gartvas(w, h)

    // use canvas

    g.canvas.drawRect(Rect(0f, 0f, g.wf, g.hf), fillOf(0xFF174185))
    g.canvas.drawCircle(w / 2f, h / 2f, 30f, fillOfRed())

    // get bitmap

    val b = Gartmap(g)
    b.forEach { x, y, v ->
        if (v == 0xFFFF0000.toInt()) {  // red detected
            if ((x + y).mod(2) == 0) {
                b[x, y] = 0xFFFFFF00
            }
        }
    }

    for (x in 0 until w) {
        b[x, 0] = 0xFFFF0044
    }

    b.draw()

    ImageWriter(g).save("example.png")
}
