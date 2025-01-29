package dev.oblac.gart.sun.two

import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fluid.lbh.BoltzmannFluid
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.ofPWH
import dev.oblac.gart.math.map
import dev.oblac.gart.math.rndi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

val p = Palettes.cool7.expand(200)

class BoltzmannFluidSimulation(rows: Int, cols: Int) {
    private val fluid: BoltzmannFluid
    private val m = 5
    private val r = rows / m
    private val c = cols / m

    init {
        fluid = BoltzmannFluid(
            overallVelocity = 0.1f,
            viscosity = 0.005f,
            r, c
        )
    }

    fun addRandomSolid() {
        repeat(1) {
            fluid.setSolid(1 + rndi(r / 2 - 2), 1 + rndi(c / 2 - 2))
        }
    }

    fun iterate() {
        fluid.iterate()
        fluid.resetSolids()
    }

    fun renderDensity(c: Canvas) {
        fluid.forEach { x, y, s, d, _ ->
            val colorNdx = map(d, 0.98f, 1.02f, 0, p.size).toInt()
            val color = p.safe(colorNdx)
            if (s) {
                //c.drawRect(Rect.ofPWH(x * m, y * m, m, m), fillOfBlack())
            } else {
                c.drawRect(Rect.ofPWH(x * m, y * m, m, m), fillOf(color))
            }
        }
    }

}
