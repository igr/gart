package dev.oblac.gart.wind

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fluid.all.FluidParticles
import dev.oblac.gart.fluid.all.FluidRenderer
import dev.oblac.gart.fluid.all.FluidSolver
import dev.oblac.gart.fluid.all.ParticleRendererPalette
import dev.oblac.gart.gfx.randomPoints
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.noise.PerlinNoise
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("fluid-wind", 1024 * GOLDEN_RATIO, 1024, 60)
    val d = gart.d

    val solver = FluidSolver(d.w, d.h, velocityScale = 4)
    val particles = FluidParticles(
        d.w, d.h,
        randomPoints(d, 50_000),
        lifetime = 600,
        numRenderSteps = 2
    )
    val renderer = FluidRenderer(solver, particles, trailLength = 30)

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

        //renderer.renderFluid(ParticleRendererTwoColors(canvas))
        // 15 22 54 56
        renderer.renderFluid(ParticleRendererPalette(canvas, Palettes.cool56))
    }
}
