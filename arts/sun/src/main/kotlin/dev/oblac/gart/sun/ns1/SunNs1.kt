package dev.oblac.gart.sun.ns1

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.fluid.navstr.NavierStokesSolver

val p = Palettes.cool21.reversed().expand(200)

fun main() {
    val gart = Gart.of("sunNS1", 800, 800, 10)
    val d = gart.d
    val g = gart.gartvas()
    val w = gart.window()

    val fluidWidth = d.w
    val fluidHeight = d.h

    val ns = NavierStokesSolver(
        fluidWidth, fluidHeight,
        dt = 1.0 / 10,
        fadeSpeed = 0.0,
        viscocity = 0.00001
    )

    addForce(ns, 0.4, 0.4, 1.0, 0.0)
    addForce(ns, 0.6, 0.6, -1.0, 1.0)

    val bitmap = gart.gartmap(g)
    w.show { c, _, f ->
        c.clear(Colors.white)

        for (x in 0..<ns.nx) {
            for (y in 0..<ns.ny) {
//                bitmap[x, y] = p.bound(s.u(x, y) * 1e8)
                bitmap[x, y] = p.bound(ns.v(x, y) * 1e8)
            }
        }

        c.drawImage(bitmap.image(), 0f, 0f)
        ns.update()

        f.onFrame(230) {
            gart.saveImage(c)
        }
    }
}


fun addForce(s: NavierStokesSolver, xx: Double, yy: Double, dx: Double, dy: Double) {
    var x = xx
    var y = yy
    val aspectRatio = 1f
    val speed = dx * dx + dy * dy * aspectRatio // balance the x and y
    if (speed > 0) {
        if (x < 0) x = 0.0
        else if (x > 1) x = 1.0
        if (y < 0) y = 0.0
        else if (y > 1) y = 1.0

        val velocityMult = 150.0

        val index = s.indexForNormalizedPosition(x, y)

        // colorMode(HSB, 360, 1, 1);
        // float hue = ((x + y) * 180 + frameCount) % 360;
        // drawColor = color(hue, 1, 1);
        // colorMode(RGB, 1);
        //
        // fluidSolver.rOld[index] += red(drawColor) * colorMult;
        // fluidSolver.gOld[index] += green(drawColor) * colorMult;
        // fluidSolver.bOld[index] += blue(drawColor) * colorMult;

        // particleSystem.addParticles(x * width, y * height, 10);
        s.uOld[index] += dx * velocityMult
        s.vOld[index] += dy * velocityMult
    }
}

