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
    lbf.init {
        it.density = sin(it.y.toFloat() / 512) + sin(it.x.toFloat() / 512) - 1f
        it.velocityX = 0.3f
        it.velocityY = 0.0f
    }

    val bitmap = gart.gartmap(g)
    val movieEnds = 100L
    m.record(w).show { c, _, f ->
    //w.show { c, _, f ->
        c.clear(Colors.white)
        lbf.simulate()
        lbf.latices().forEach {
            val a = abs(it.density % 1f)
            val clr = p.safe(a * p.size)
            bitmap[it.x, it.y] = clr
        }

        c.drawImage(bitmap.image(), 0f, 0f)

        f.onFrame(movieEnds) {
            m.stopRecording()
            gart.saveMovie(m)
        }
    }
}
