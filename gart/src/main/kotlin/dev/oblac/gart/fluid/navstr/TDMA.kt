package dev.oblac.gart.fluid.navstr

/**
 * Implements the **Thomas Algorithm** (a specialized form of Gaussian elimination)
 * for solving tri-diagonal systems of linear equations.
 *
 * n - number of equations
 * a - sub-diagonal (means it is the diagonal below the main diagonal) -- indexed from 1..n-1
 * b - the main diagonal
 * c - sup-diagonal (means it is the diagonal above the main diagonal) -- indexed from 0..n-2
 * v - right part
 * x - the result
 */
internal fun solveTDMA(n: Int, a: DoubleArray, b: DoubleArray, c: DoubleArray, v: DoubleArray): DoubleArray {
    val x = DoubleArray(n)

    for (i in 1..<n) {
        val m = a[i] / b[i - 1]
        b[i] = b[i] - m * c[i - 1]
        v[i] = v[i] - m * v[i - 1]
    }

    x[n - 1] = v[n - 1] / b[n - 1]

    for (i in n - 2 downTo 0) {
        x[i] = (v[i] - c[i] * x[i + 1]) / b[i]
    }
    return x
}


/**
 * Solves the tri diagonal matrix equation using TDMA for problems with Periodic boundaries.         * n - number of equations
 * a - sub-diagonal (means it is the diagonal below the main diagonal) -- indexed from 0..n-1
 * b - the main diagonal
 * c - sup-diagonal (means it is the diagonal above the main diagonal) -- indexed from 0..n-1
 * v - right part
 * x - the answer
 */
internal fun solveTDMAP(n: Int, a: DoubleArray, b: DoubleArray, c: DoubleArray, v: DoubleArray): DoubleArray {
    require(n > 2) { "n is too small for a cyclic problem" }
    val bb = DoubleArray(n)
    val u = DoubleArray(n)

    val beta = a[0]
    val alpha = c[n - 1]
    val gamma = -b[0]

    bb[0] = b[0] - gamma
    bb[n - 1] = b[n - 1] - alpha * beta / gamma

    for (i in 1..<n - 1) {
        bb[i] = b[i]
    }

    val x = solveTDMA(n, a, bb, c, v)
    u[0] = gamma
    u[n - 1] = alpha
    for (i in 1..<n - 1) {
        u[i] = 0.0
    }
    val z = solveTDMA(n, a, bb, c, u)
    val fact = (x[0] + beta * x[n - 1] / gamma) / (1.0 + z[0] + beta * z[n - 1] / gamma)

    for (i in 0..<n) {
        x[i] -= fact * z[i]
    }
    return x

}
