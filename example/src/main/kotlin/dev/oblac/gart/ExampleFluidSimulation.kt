package dev.oblac.gart

import dev.oblac.gart.fluid.all.*
import dev.oblac.gart.gfx.randomPoints
import java.awt.event.MouseEvent

/**
 * Main fluid simulation combining solver, particles, and renderer.
 *
 * Controls:
 * - Mouse drag: Add force to fluid
 * - 1: Fluid mode (particle trails)
 * - 2: Pressure mode
 * - 3: Velocity mode
 * - R: Reset simulation
 * - P: Save PNG screenshot
 */
fun main() {
    val gart = Gart.of("example-fluid", 1024, 1024, 60)
    val d = gart.d

    // Initialize components
    val solver = FluidSolver(d.w, d.h)
    val particles = FluidParticles(d.w, d.h, randomPoints(d, 500_00))
    val renderer = FluidRenderer(solver, particles)

    // Current render function (can be switched via keyboard)
    var renderFn: (org.jetbrains.skia.Canvas) -> Unit = { canvas ->
        renderer.renderFluid(ParticleRendererTwoColors(canvas))
    }

    // Mouse tracking for force application
    var lastMouseX = -1f
    var lastMouseY = -1f

    val w = gart.window()
    val wv = w.show { canvas, _, frames ->
        // Update simulation
        if (frames.new) {
            solver.step()
            particles.update(solver)
        }

        renderFn(canvas)
    }

    // Mouse handling for force application
    wv.onMouseMotion { e ->
        val isDragging = (e.modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0
        val x = e.x.toFloat() * 2  // Account for retina scaling
        val y = e.y.toFloat() * 2

        if (isDragging) {
            if (lastMouseX >= 0 && lastMouseY >= 0) {
                val forceX = x - lastMouseX
                val forceY = y - lastMouseY

                if (forceX != 0f || forceY != 0f) {
                    solver.applyForce(x, y, forceX, forceY, 50f)
                }
            }
            lastMouseX = x
            lastMouseY = y
        } else {
            lastMouseX = -1f
            lastMouseY = -1f
        }
    }

    // Keyboard controls
    wv.onKey { key ->
        when (key) {
            Key.KEY_1 -> {
                renderFn = { canvas -> renderer.renderFluid(ParticleRendererTwoColors(canvas)) }
                println("Mode: Fluid")
            }

            Key.KEY_2 -> {
                renderFn = { canvas -> renderFluidPressure(canvas, solver) }
                println("Mode: Pressure")
            }

            Key.KEY_3 -> {
                renderFn = { canvas -> renderFluidVelocityField(canvas, solver) }
                println("Mode: Velocity")
            }

            Key.KEY_R -> {
                solver.reset()
                particles.reset()
                renderer.clearTrails()
                println("Reset")
            }

            Key.KEY_P -> {
                val g = gart.gartvas()
                renderFn(g.canvas)
                gart.saveImage(g)
                println("Saved PNG")
            }

            else -> {}
        }
    }

    println("Fluid Simulation")
    println("Controls:")
    println("  Mouse drag: Add force")
    println("  1: Fluid mode")
    println("  2: Pressure mode")
    println("  3: Velocity mode")
    println("  R: Reset")
    println("  P: Save PNG")
}
