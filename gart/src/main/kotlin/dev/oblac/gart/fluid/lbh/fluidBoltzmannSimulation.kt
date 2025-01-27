package dev.oblac.gart.fluid.lbh

import dev.oblac.gart.color.alpha
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.ofPWH
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

class BoltzmannFluidSimulation(private val rows: Int, private val cols: Int) {
    private val solution: BoltzmannFluid

    private val scale = 0.101f
    private val m = 5

    init {
        solution = BoltzmannFluid(
            overallVelocity = 0.003f,
            viscosity = 0.005f,
            rows / m,
            cols / m
        )
        for (i in 2..<80) {
            solution.solid[i][i] = true
            solution.solid[i + 1][i + 1] = true
        }
    }

    fun iterate() {
        solution.iterate()
    }

    fun renderByVelocity(c: Canvas) {
        for (i in 0..<rows / m) {
            for (j in 0..<cols / m) {

                val color = scaleValue(solution.velocity(i, j), 0.0f, (0.02f * scale), 0.0f, 100.0f).toInt()
                if (solution.solid(i, j)) {
                    c.drawRect(Rect.ofPWH(i * m, j * m, m, m), fillOfBlack())
                } else {
                    c.drawRect(Rect.ofPWH(i * m, j * m, m, m), fillOf(color.alpha(255)))
                }
            }
        }
    }

    fun renderByDensity(c: Canvas) {
        for (i in 0..<rows / m) {
            for (j in 0..<cols / m) {
                val color = scaleValue(solution.density(i, j), 0.0f, (0.02f * scale), 0.0f, 100.0f)
                if (solution.solid(i, j)) {
                    c.drawRect(Rect.ofPWH(i * m, j * m, m, m), fillOfBlack())
                } else {
                    c.drawRect(Rect.ofPWH(i * m, j * m, m, m), fillOf(color.toInt().alpha(255)))
                }
            }
        }
    }

    private fun scaleValue(value: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        return ((outMax - outMin) * (value - inMin) / (inMax - inMin)) + outMin
    }

}
