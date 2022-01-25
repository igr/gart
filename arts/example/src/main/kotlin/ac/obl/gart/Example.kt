package ac.obl.gart

import ac.obl.gart.gfx.fillOf
import ac.obl.gart.gfx.fillOfRed
import io.github.humbleui.types.Rect

fun main() {
    println("Example")

    val g = Gartvas(100, 100)

    // use canvas

    g.canvas.drawRect(Rect(0f, 0f, 100f, 100f), fillOf(0xFF174185))
    g.canvas.drawCircle(50f, 50f, 30f, fillOfRed())

    // get bitmap

    val b = Gartmap(g)
    b.forEach { x, y, v ->
        if (v == 0xFFFF0000.toInt()) {  // red detected
            if ((x + y).mod(2) == 0) {
                b.set(x, y, 0xFFFFFF00)
            }
        }
    }
    b.draw()

    ImageWriter(g).save("example.png")
}
