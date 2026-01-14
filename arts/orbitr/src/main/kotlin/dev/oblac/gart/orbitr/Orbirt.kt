package dev.oblac.gart.orbitr

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.nbody.BarnesHutSimulation
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import kotlin.math.sqrt

fun main() {
    val gart = Gart.of("orbitr", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = OrbitrDraw(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class OrbitrDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}


private val pal = Palette.of(Color.TRANSPARENT, RetroColors.white01, RetroColors.red01)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

    repeat(200) { n ->
        val r = 20f + n * 1.5f
        val sim = BarnesHutSimulation()
        val v = sqrt(sim.G * 1f / r)  // orbital velocity
        sim.addParticle(x = 0f, y = 0f, vx = 0f, vy = 0f, mass = 150f)
        sim.addParticle(x = r/1.2f, y = 10f - n, vx = 0f, vy = -v * 10f, mass = 1f)
        //sim.addParticle(x = -r * 2.5f, y = -r * 2.5f - n, vx = 0f, vy = v * 10f, mass = 0.01f)

        repeat(400) {
            sim.step(dt = 1f)
            drawParticlesAsDots(c, d, sim)
        }
    }

}

private fun drawParticlesAsDots(c: Canvas, d: Dimension, sim: BarnesHutSimulation) {
    val count = sim.particles.count
    for (i in 0 until count) {
        val x = sim.particles.x[i]
        val y = sim.particles.y[i]

        val sx = d.cx + x * 10f
        val sy = d.cy + y * 10f

        val distance = sqrt(x * x + y * y)
        val size = distance / 10f

        c.drawCircle(sx, sy, size, fillOf(pal[i % pal.size]))
    }
}
