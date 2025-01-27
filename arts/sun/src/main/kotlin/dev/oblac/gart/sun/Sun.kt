package dev.oblac.gart.sun

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fluid.lbh.Simulation

val p = Palettes.cool43.expand(200)

fun main() {
    val gart = Gart.of("sun", 1025, 1025, 10)
    val g = gart.gartvas()
    val w = gart.window()
    val m = gart.movieGif()

    val lbf = Simulation(1025, 1025)

    val bitmap = gart.gartmap(g)
    val movieEnds = 100L
//    m.record(w).show { c, _, f ->
    w.show { c, _, f ->
        c.clear(Colors.white)
        lbf.iterate()
        lbf.renderByDensity(c)
//        lbf.renderByVelocity(c)

//        f.onFrame(movieEnds) {
//            m.stopRecording()
//            gart.saveMovie(m)
//        }
    }
}
