package dev.oblac.gart.sun

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fluid.lbh.LatticeBoltzmannFluidSimulation
import kotlin.math.abs
import kotlin.math.sin

val p = Palettes.cool43.expand(200)

fun main() {
    val gart = Gart.of("sun", 1024, 1024, 10)
    val g = gart.gartvas()
    val w = gart.window()
    val m = gart.movieGif()

    val lbf = LatticeBoltzmannFluidSimulation(1024, 1024)
    lbf.init { l, x, y ->
        l.density = sin(y / 512f) + sin(x / 512f) - 1f
        l.velocityX = 0.3f
        l.velocityY = 0.0f
    }

    val bitmap = gart.gartmap(g)
    val movieEnds = 100L
    m.record(w).show { c, _, f ->
    //w.show { c, _, f ->
        c.clear(Colors.white)
        lbf.simulate()
        lbf.lattices { l, x, y ->
            val a = abs(l.density % 1f)
            val clr = p.safe(a * p.size)
            bitmap[x, y] = clr
        }

        c.drawImage(bitmap.image(), 0f, 0f)

        f.onFrame(movieEnds) {
            m.stopRecording()
            gart.saveMovie(m)
        }
    }
}
