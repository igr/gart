package studio.oblac.gart

import studio.oblac.gart.gfx.fillOf
import studio.oblac.gart.gfx.fillOfRed
import studio.oblac.gart.math.Constants.goldenRatio
import studio.oblac.gart.skia.Rect

fun main() {
    println("Example")

    val h = 100
    val w = (h * goldenRatio).toInt()
    val d = Dimension(w, h)
    val g = Gartvas(d)

    // use canvas

    g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(0xFF174185))
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
    b[0,0] = 0xFF00FF00

    b.draw()

    writeGartvasAsImage(g, "example.png")
}
