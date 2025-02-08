package dev.oblac.gart.sun.ns2

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fluid.navstr.NSSolverImplicit
import dev.oblac.gart.gfx.drawBorder

val p = Palettes.cool31.expand(300)

fun main() {
    val gart = Gart.of("sqr", 400, 400, 10)
    val d = gart.d
    val g = gart.gartvas()
    val w = gart.window()
    val m = gart.movieGif()

    val s2 = NSSolverImplicit(400, 400, 1.0, 1.0)
    for (x in 0..<s2.nx) {
        for (y in 0..<s2.ny) {
            s2.setWall(x, y)
        }
    }

    s2.setVel(133, 133, 1.9, true)

    val bitmap = gart.gartmap(g)
    //w.show { c, _, f ->

    m.record(w, recording = false).show { c, _, f ->
        for (x in 1..<s2.nx - 1) {
            for (y in 1..<s2.ny - 1) {
                bitmap[x, y] = p.bound(s2.vS(x, y) * 1e8)
            }
        }

        c.drawImage(bitmap.image(), 0f, 0f)
        c.drawBorder(d, 10.0f, 0xff264653.toInt())

        s2.step(0.005)

        f.onFrame(160) {
            m.startRecording()
        }
        f.onFrame(420L) {
            m.stopRecording()
            gart.saveMovie(m)
        }
    }


}
