package dev.oblac.gart.wind

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fluid.all.FluidParticles
import dev.oblac.gart.fluid.all.FluidRenderer
import dev.oblac.gart.fluid.all.FluidSolver
import dev.oblac.gart.fluid.all.ParticleRenderer
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.randomPoints
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.noise.PerlinNoise
import dev.oblac.gart.pack.simpleCirclePacker
import dev.oblac.gart.saveImageToFile
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("fluid-pack", 1024 * GOLDEN_RATIO, 1024, 60)
    val d = gart.d

    val solver = FluidSolver(d.w, d.h, velocityScale = 4)
    val particles = FluidParticles(
        d.w, d.h,
        randomPoints(d, 10_000),
        lifetime = 600,
        numRenderSteps = 2
    )
    val renderer = FluidRenderer(solver, particles, trailLength = 0)

    val noise = PerlinNoise()
    var time = 0f

    val w = gart.window()
    w.show { canvas, _, frames ->
        if (frames.new) {
            time += 0.005f

            // Apply wind-like forces using Perlin noise
            val gridStep = 80
            for (gy in 0 until d.h step gridStep) {
                for (gx in 0 until d.w step gridStep) {
                    val nx = gx.toFloat() / d.wf
                    val ny = gy.toFloat() / d.hf

                    // Use noise to determine wind direction
                    val noiseVal = noise.noise(nx * 3 + time, ny * 3 + time, time * 0.5)
                    val angle = noiseVal * Math.PI.toFloat() * 2

                    // Wind strength varies with position
                    val strength = 2f + noise.noise(nx * 2, ny * 2, time * 0.3) * 4f

                    val forceX = cos(angle) * strength
                    val forceY = sin(angle) * strength

                    solver.applyForce(gx.toFloat(), gy.toFloat(), forceX, forceY, 60f)
                }
            }

            solver.step()
            particles.update(solver)
        }

        // 56
        val expandedColors = Palettes.cool56.expand(256)
        val pixelData = mutableListOf<Triple<Int, Int, Float>>()

        renderer.renderFluid(object : ParticleRenderer {
            override fun clear() {
                canvas.clear(expandedColors[0])
                pixelData.clear()
            }

            override fun renderPixel(x: Int, y: Int, value: Float, blockSize: Float) {
                pixelData.add(Triple(x, y, value))
            }
        })

        if (frames.frame == 40L) {
            // Circle packing on frame 40
            if (pixelData.isNotEmpty()) {
                val valueMap = pixelData.associate { (x, y, value) ->
                    Pair(x, y) to value
                }

                val circles = simpleCirclePacker(
                    d.rect,
                    attempts = pixelData.size * 2,
                    minRadius = 3f,
                    maxRadius = 20f,
                    growth = 1,
                    padding = 2
                )

                circles.forEach { circle ->
                    val nearestValue = valueMap.entries
                        .minByOrNull { (pos, _) ->
                            val dx = pos.first - circle.x
                            val dy = pos.second - circle.y
                            dx * dx + dy * dy
                        }?.value ?: 0.5f

                    val index = (nearestValue * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(circle.x, circle.y, circle.radius, fillOf(expandedColors[index]))
                }
            }
            saveImageToFile(canvas, d, "${gart.name}.png")
        } else {
            // Simple circle drawing on other frames
            pixelData.forEach { (x, y, value) ->
                val index = (value * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(x.toFloat(), y.toFloat(), 2f, fillOf(expandedColors[index]))
            }
        }
    }
}
