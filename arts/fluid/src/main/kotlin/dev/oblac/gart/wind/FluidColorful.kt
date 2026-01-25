package dev.oblac.gart.wind

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.argb
import dev.oblac.gart.fluid.all.FluidParticles
import dev.oblac.gart.fluid.all.FluidSolver
import dev.oblac.gart.gfx.randomPoints
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Colorful fluid simulation with custom palette rendering.
 */
fun main() {
    val gart = Gart.of("fluid", 1024, 1024, 60)
    val d = gart.d

    val solver = FluidSolver(d.w, d.h)
    val particles = FluidParticles(d.w, d.h, randomPoints(d, 10_000))
    val colorRenderer = ColorfulFluidRenderer(d.w, d.h)

    var angle = 0f
    val centerX = d.cx
    val centerY = d.cy
    val radius = d.wf / 3

    val w = gart.window()
    w.show { canvas, _, frames ->
        if (frames.new) {
            angle += 0.02f
            val forceX = cos(angle) * 10f
            val forceY = sin(angle) * 10f

            for (i in 0 until 4) {
                val a = angle + i * (Math.PI.toFloat() / 2)
                val px = centerX + cos(a) * radius
                val py = centerY + sin(a) * radius
                solver.applyForce(px, py, forceX, forceY, 50f)
            }

            if (frames.frame % 20 == 0L) {
                val rx = Random.nextFloat() * d.wf
                val ry = Random.nextFloat() * d.hf
                val rfx = (Random.nextFloat() - 0.5f) * 30f
                val rfy = (Random.nextFloat() - 0.5f) * 30f
                solver.applyForce(rx, ry, rfx, rfy, 80f)
            }

            solver.step()
            particles.update(solver)
        }

        colorRenderer.render(canvas, solver, particles)
    }
}

/**
 * Custom renderer with colorful palette.
 */
class ColorfulFluidRenderer(
    private val width: Int,
    private val height: Int
) {
    private val trailBuffer = FloatArray(width * height)
    private val colorBuffer = IntArray(width * height)
    private var trailLength = 20

    private val palette = Palettes.cool28.expand(256)
    private val backgroundColor = argb(255, 20, 20, 30)

    fun render(canvas: Canvas, solver: FluidSolver, particles: FluidParticles) {
        // Fade trails
        val fadeIncrement = -1f / trailLength
        for (i in trailBuffer.indices) {
            trailBuffer[i] = maxOf(trailBuffer[i] + fadeIncrement, 0f)
        }

        // Add particles with velocity-based coloring
        particles.forEachParticleWithVelocity(solver) { x, y, opacity, velX, velY ->
            val px = x.toInt().coerceIn(0, width - 1)
            val py = y.toInt().coerceIn(0, height - 1)
            val idx = py * width + px

            // Color based on velocity direction
            val velMag = sqrt(velX * velX + velY * velY)
            val colorIdx = ((velX / (velMag + 0.1f) + 1f) * 0.5f * 255).toInt().coerceIn(0, 255)

            trailBuffer[idx] = minOf(trailBuffer[idx] + opacity, 1f)
            if (opacity > 0.1f) {
                colorBuffer[idx] = colorIdx
            }

            // Expand particle footprint
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val nx = (px + dx).coerceIn(0, width - 1)
                    val ny = (py + dy).coerceIn(0, height - 1)
                    val nidx = ny * width + nx
                    trailBuffer[nidx] = minOf(trailBuffer[nidx] + opacity * 0.2f, 1f)
                    if (opacity > 0.1f) {
                        colorBuffer[nidx] = colorIdx
                    }
                }
            }
        }

        // Render
        canvas.clear(backgroundColor)

        val paint = Paint().apply {
            mode = PaintMode.FILL
        }

        val blockSize = 2
        for (by in 0 until height step blockSize) {
            for (bx in 0 until width step blockSize) {
                val cx = minOf(bx + blockSize / 2, width - 1)
                val cy = minOf(by + blockSize / 2, height - 1)
                val idx = cy * width + cx
                val value = trailBuffer[idx]

                if (value > 0.02f) {
                    val colorIdx = colorBuffer[idx]
                    val baseColor = palette[colorIdx]

                    // Blend with background based on trail intensity
                    val alpha = (value * 255).toInt().coerceIn(0, 255)
                    paint.color = argb(
                        alpha,
                        (baseColor shr 16) and 0xFF,
                        (baseColor shr 8) and 0xFF,
                        baseColor and 0xFF
                    )

                    canvas.drawRect(
                        Rect.makeXYWH(
                            bx.toFloat(), by.toFloat(),
                            blockSize.toFloat(), blockSize.toFloat()
                        ),
                        paint
                    )
                }
            }
        }
    }
}
