package dev.oblac.gart.sun.two

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors

fun main() {
    val gart = Gart.of("echoes", 1025, 1025, 20)
    val g = gart.gartvas()
    val w = gart.window()

    val lbf = BoltzmannFluidSimulation(g.d.w, g.d.h)

    //val bitmap = gart.gartmap(g)
    val theEnd = 108L
    w.show { c, _, f ->
        c.clear(Colors.white)
        lbf.iterate()
        lbf.render(c)

        f.onFrame(theEnd) {
            //gart.saveImage(c)
        }

        f.onFrame(20) {
            lbf.addRandomSolid(100, 100)
        }
        f.onFrame(30) {
            lbf.addRandomSolid(115, 110)
        }
        f.onFrame(40) {
            lbf.addRandomSolid(140, 120)
        }
        f.onFrame(81) {
            //lbf.reset()
        }
    }
}
