package dev.oblac.gart.fluid.all

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Fluid solver based.
 * Uses Semi-Lagrangian advection and Jacobi iteration for pressure projection.
 * This creates a divergence-free velocity field.
 */
class FluidSolver(
    val width: Int,
    val height: Int,
    private val velocityScale: Int = 8,
    private val numJacobiSteps: Int = 3,
    private val maxVelocity: Float = 30f
) {
    // Velocity field dimensions (lower resolution for efficiency)
    val velWidth = (width + velocityScale - 1) / velocityScale
    val velHeight = (height + velocityScale - 1) / velocityScale

    // Velocity field (u, v components)
    private var velocityU = FloatArray(velWidth * velHeight)
    private var velocityV = FloatArray(velWidth * velHeight)
    private var velocityUTemp = FloatArray(velWidth * velHeight)
    private var velocityVTemp = FloatArray(velWidth * velHeight)

    // Divergence field
    private val divergence = FloatArray(velWidth * velHeight)

    // Pressure field (double-buffered)
    private var pressure = FloatArray(velWidth * velHeight)
    private var pressureTemp = FloatArray(velWidth * velHeight)

    // Jacobi iteration constants
    private val pressureCalcAlpha = -1f
    private val pressureCalcBeta = 0.25f

    // Pixel size for gradient calculations
    private val pxSizeX = 1f / velWidth
    private val pxSizeY = 1f / velHeight

    // Wrapping helpers
    private fun Int.wrapX() = ((this % velWidth) + velWidth) % velWidth
    private fun Int.wrapY() = ((this % velHeight) + velHeight) % velHeight
    private fun Float.wrapX() = ((this % velWidth) + velWidth) % velWidth
    private fun Float.wrapY() = ((this % velHeight) + velHeight) % velHeight

    /**
     * Main simulation step.
     */
    fun step() {
        // 1. Advect the velocity vector field
        advectVelocity()

        // 2. Compute divergence of advected velocity field
        computeDivergence()

        // 3. Compute pressure gradient using Jacobi iteration
        repeat(numJacobiSteps) {
            jacobiIteration()
        }

        // 4. Subtract pressure gradient from velocity (pressure projection)
        subtractPressureGradient()
    }

    /**
     * Semi-Lagrangian advection of velocity field.
     * Traces particles backwards through velocity field.
     */
    private fun advectVelocity() {
        for (y in 0 until velHeight) {
            for (x in 0 until velWidth) {
                val idx = y * velWidth + x

                // Get velocity at current position
                val u = velocityU[idx]
                val v = velocityV[idx]

                // Trace back through velocity field
                val srcX = x - u / velWidth
                val srcY = y - v / velHeight

                // Sample velocity at source position using bilinear interpolation
                val (newU, newV) = sampleVelocity(srcX, srcY)
                velocityUTemp[idx] = newU
                velocityVTemp[idx] = newV
            }
        }

        // Swap buffers
        velocityU = velocityUTemp.also { velocityUTemp = velocityU }
        velocityV = velocityVTemp.also { velocityVTemp = velocityV }
    }

    /**
     * Bilinear interpolation of velocity field.
     */
    private fun sampleVelocity(x: Float, y: Float): Pair<Float, Float> {
        // Wrap coordinates for seamless tiling
        val wx = x.wrapX()
        val wy = y.wrapY()

        val x0 = wx.toInt()
        val y0 = wy.toInt()
        val x1 = (x0 + 1).wrapX()
        val y1 = (y0 + 1).wrapY()

        val fx = wx - x0
        val fy = wy - y0

        val idx00 = y0 * velWidth + x0
        val idx10 = y0 * velWidth + x1
        val idx01 = y1 * velWidth + x0
        val idx11 = y1 * velWidth + x1

        val u = (1 - fx) * (1 - fy) * velocityU[idx00] +
            fx * (1 - fy) * velocityU[idx10] +
            (1 - fx) * fy * velocityU[idx01] +
            fx * fy * velocityU[idx11]

        val v = (1 - fx) * (1 - fy) * velocityV[idx00] +
            fx * (1 - fy) * velocityV[idx10] +
            (1 - fx) * fy * velocityV[idx01] +
            fx * fy * velocityV[idx11]

        return u to v
    }

    /**
     * Compute divergence of velocity field.
     */
    private fun computeDivergence() {
        for (y in 0 until velHeight) {
            for (x in 0 until velWidth) {
                val idx = y * velWidth + x

                // Sample neighbors with wrapping
                val xPlus = (x + 1).wrapX()
                val xMinus = (x - 1).wrapX()
                val yPlus = (y + 1).wrapY()
                val yMinus = (y - 1).wrapY()

                val e = velocityU[y * velWidth + xPlus]
                val w = velocityU[y * velWidth + xMinus]
                val n = velocityV[yPlus * velWidth + x]
                val s = velocityV[yMinus * velWidth + x]

                divergence[idx] = 0.5f * (e - w + n - s)
            }
        }
    }

    /**
     * Jacobi iteration for pressure solving.
     */
    private fun jacobiIteration() {
        for (y in 0 until velHeight) {
            for (x in 0 until velWidth) {
                val idx = y * velWidth + x

                // Sample neighbors with wrapping
                val xPlus = (x + 1).wrapX()
                val xMinus = (x - 1).wrapX()
                val yPlus = (y + 1).wrapY()
                val yMinus = (y - 1).wrapY()

                val n = pressure[yPlus * velWidth + x]
                val s = pressure[yMinus * velWidth + x]
                val e = pressure[y * velWidth + xPlus]
                val w = pressure[y * velWidth + xMinus]
                val d = divergence[idx]

                pressureTemp[idx] = (n + s + e + w + pressureCalcAlpha * d) * pressureCalcBeta
            }
        }

        // Swap buffers
        pressure = pressureTemp.also { pressureTemp = pressure }
    }

    /**
     * Subtract pressure gradient from velocity to enforce incompressibility.
     */
    private fun subtractPressureGradient() {
        for (y in 0 until velHeight) {
            for (x in 0 until velWidth) {
                val idx = y * velWidth + x

                // Sample neighbors with wrapping
                val xPlus = (x + 1).wrapX()
                val xMinus = (x - 1).wrapX()
                val yPlus = (y + 1).wrapY()
                val yMinus = (y - 1).wrapY()

                val n = pressure[yPlus * velWidth + x]
                val s = pressure[yMinus * velWidth + x]
                val e = pressure[y * velWidth + xPlus]
                val w = pressure[y * velWidth + xMinus]

                velocityU[idx] -= 0.5f * (e - w)
                velocityV[idx] -= 0.5f * (n - s)
            }
        }
    }

    /**
     * Apply force at a position (in screen coordinates).
     */
    fun applyForce(screenX: Float, screenY: Float, forceX: Float, forceY: Float, radius: Float = 30f) {
        val velX = screenX / velocityScale
        val velY = screenY / velocityScale
        val velRadius = radius / velocityScale

        val radiusSq = velRadius * velRadius

        val minX = max(0, (velX - velRadius).toInt())
        val maxX = min(velWidth - 1, (velX + velRadius).toInt())
        val minY = max(0, (velY - velRadius).toInt())
        val maxY = min(velHeight - 1, (velY + velRadius).toInt())

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - velX
                val dy = y - velY
                val distSq = dx * dx + dy * dy

                if (distSq < radiusSq) {
                    val idx = y * velWidth + x
                    val falloff = 1f - distSq / radiusSq

                    // Add force with falloff
                    var newU = velocityU[idx] + forceX * falloff * 2f
                    var newV = velocityV[idx] + forceY * falloff * 2f

                    // Clamp velocity magnitude
                    val mag = sqrt(newU * newU + newV * newV)
                    if (mag > maxVelocity) {
                        newU = newU / mag * maxVelocity
                        newV = newV / mag * maxVelocity
                    }

                    velocityU[idx] = newU
                    velocityV[idx] = newV
                }
            }
        }
    }

    /**
     * Get velocity at screen position using bilinear interpolation.
     */
    fun velocityAt(screenX: Float, screenY: Float): Pair<Float, Float> {
        val velX = screenX / velocityScale
        val velY = screenY / velocityScale
        return sampleVelocity(velX, velY)
    }

    /**
     * Get velocity field for rendering.
     */
    fun velocityU(): FloatArray = velocityU
    fun velocityV(): FloatArray = velocityV
    fun pressure(): FloatArray = pressure

    /**
     * Reset the simulation.
     */
    fun reset() {
        velocityU.fill(0f)
        velocityV.fill(0f)
        velocityUTemp.fill(0f)
        velocityVTemp.fill(0f)
        divergence.fill(0f)
        pressure.fill(0f)
        pressureTemp.fill(0f)
    }
}
