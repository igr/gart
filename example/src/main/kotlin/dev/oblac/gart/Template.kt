package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOfRed

fun main() {
    val gart = Gart.of("template", 512, 512)
    println(gart)

    val d = gart.d
    val w = gart.window()

    // window 1 - static image
    val g = gart.gartvas()
    val c = g.canvas
    c.clear(Colors.navy)
    c.drawCircle(d.center, 100f, fillOfRed())
    w.showImage(g)

    // window 2 - animated
    w.show { c, d, f ->
        if (f.new) {
            if (f.frame % 100 == 0L) {
                println("Frame: ${f.frame}")
            }
        }
        c.clear(Colors.darkGreen)
        c.drawCircle(d.center, 100f, fillOfRed())
    }
}
