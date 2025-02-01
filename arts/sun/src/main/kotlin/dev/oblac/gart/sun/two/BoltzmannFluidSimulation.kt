package dev.oblac.gart.sun.two

import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fluid.lbh.BoltzmannFluid
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.ofPWH
import dev.oblac.gart.math.map
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

val p = Palettes.cool10.expand(200)

class BoltzmannFluidSimulation(rows: Int, cols: Int) {
    private val fluid: BoltzmannFluid
    private val m = 5
    private val r = rows / m
    private val c = cols / m

    init {
        fluid = BoltzmannFluid(
            overallVelocity = 0.1f,
            viscosity = 0.001f,
            r, c
        )
    }

    fun addRandomSolid(x: Int, y: Int) {
        fluid.setSolid(x, y + 1)
        fluid.setSolid(x, y + 3)
        fluid.setSolid(x, y + 4)
        fluid.setSolid(x, y + 5)
        fluid.setSolid(x, y + 6)
        fluid.setSolid(x, y + 7)

        val x2 = x + 1
        fluid.setSolid(x2, y + 1)
        fluid.setSolid(x2, y + 3)
        fluid.setSolid(x2, y + 4)
        fluid.setSolid(x2, y + 5)
        fluid.setSolid(x2, y + 6)
        fluid.setSolid(x2, y + 7)

    }

    fun iterate() {
        fluid.iterate()
    }
    fun reset() {
        fluid.resetSolids()
    }

    fun renderDensity(c: Canvas) {
        fluid.forEach { x, y, s, d, vx, vy, _ ->
            val colorNdx = map(d, 0.998f, 1.011f, 0, p.size).toInt()
            val color = p.bound(colorNdx)
            if (s) {
                c.drawRect(Rect.ofPWH(x * m, y * m, m, m), fillOf(p[0]))
            } else {
                c.drawRect(Rect.ofPWH(x * m, y * m, m, m), fillOf(color))
            }
        }
    }

}
