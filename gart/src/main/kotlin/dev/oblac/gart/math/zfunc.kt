package dev.oblac.gart.math

import dev.oblac.gart.math.Convergence.CONVERGED
import dev.oblac.gart.math.Convergence.DIVERGED

enum class Convergence {
    CONVERGED, DIVERGED
}

data class ZFuncResult(
    val z: Complex,
    val iter: Int,
    val convergence: Convergence,
    val maxIterations: Int
)

// Select a complex function f(z) and repeatedly apply it to a starting point corresponding to each pixel.
// A bound is applied on the number of iterations OR on the value reached.
// Then a color is selected depending on the number of iterations performed before convergence,
// divergence, or on the last computed value.

fun zfunc(
    x: Float,
    y: Float,
    maxIterations: Int,
    escapeRadius: Float,
    f: (Complex) -> Complex
): ZFuncResult {
    var z = Complex(x, y)
    var iter = 0

    val escapeRadiusPow2 = escapeRadius * escapeRadius
    while (iter < maxIterations && z.normSquared() < escapeRadiusPow2) {
        z = f(z)
        iter++
    }
    return ZFuncResult(
        z,
        iter,
        if (iter == maxIterations) CONVERGED else DIVERGED,
        maxIterations
    )
}

