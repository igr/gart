package dev.oblac.gart.fluid.navstr

import kotlin.math.floor
import kotlin.math.min

/**
 * Solving real-time fluid dynamics simulations based on Navier-Stokes equations
 * and code from Jos Stam's paper "Real-Time Fluid Dynamics for Games".
 * http://www.dgp.toronto.edu/people/stam/reality/Research/pdf/GDC03.pdf
 */
@Suppress("t")
class NavierStokesSolver(
    val nx: Int,
    val ny: Int,
    val dt: Double = 1.0,     // time step
    val viscocity: Double = 0.0, // viscosity
    val fadeSpeed: Double = 0.0,  // how quickly the fluid dye dissipates and fades out
    val solverIterations: Int = 10 // number of iterations for solver (higher is slower but more accurate)
) {
    val width = nx + 2
    val height = ny + 2

    private val invWidth = 1.0 / nx
    private val invHeight = 1.0 / ny

    val numCells = (nx + 2) * (ny + 2)  // Total number of cells

    private val invNx = 1.0 / nx
    private val invNy = 1.0 / ny
    private val invNumCells = 1.0 / numCells

    var r = DoubleArray(numCells)
    var u = DoubleArray(numCells)
    var v = DoubleArray(numCells)

    var rOld = DoubleArray(numCells)
    var uOld = DoubleArray(numCells)
    var vOld = DoubleArray(numCells)

    private var avgDensity = 0.0 // Average density of fluid
    private var avgUniformity = 0.0 // average uniformity (distribution of densities and dye)
    private var avgSpeed = 0.0    // average speed of fluid (how uniform the colors is)

    /**
     * Randomize dye (useful for debugging).
     */
    fun randomizeColor() {
        for (i in 0..<width) {
            for (j in 0..<height) {
                val index = fluidIX(i, j)
                rOld[index] = Math.random()
                r[index] = rOld[index]
            }
        }
    }

    /**
     * Returns fluid cell index for (i,j) cell coordinates.
     * @return cell index (to be used in r, u, v arrays)
     */
    fun indexForCellPosition(i: Int, j: Int): Int {
        val ii = when {
            i < 1 -> 1
            i > nx -> nx
            else -> i
        }
        val jj = when {
            j < 1 -> 1
            j > ny -> ny
            else -> j
        }

        return fluidIX(ii, jj)
    }

    /**
     * Returns fluid cell index for normalized (x, y) coordinates.
     * @param x 0...1 normalized x position
     * @param y 0...1 normalized y position
     * @return cell index (to be used in r, u, v arrays)
     */
    fun indexForNormalizedPosition(x: Double, y: Double): Int {
        return indexForCellPosition(
            floor((x * (nx + 2))).toInt(),
            floor((y * (ny + 2))).toInt()
        )
    }

    fun addForceAtPos(x: Double, y: Double, vx: Double, vy: Double) {
        val i = (x * nx + 1)
        val j = (y * ny + 1)
        if (i < 0 || i > x + 1 || j < 0 || j > y + 1) return;
        addForceAtCell(i.toInt(), j.toInt(), vx, vy);
    }

    fun addForceAtCell(i: Int, j: Int, vx: Double, vy: Double) {
        val index = fluidIX(i, j)
        uOld[index] += vx * nx
        vOld[index] += vy * ny
    }
    
    /**
     * Updates the calculation. Must be called once every frame to move the
     * solver one step forward.
     */
    fun update() {
        addSourceUV()

        swapU()
        swapV()

        diffuseUV(0, viscocity)

        project(u, v, uOld, vOld)

        swapU()
        swapV()

        advect(1, u, uOld, uOld, vOld)
        advect(2, v, vOld, uOld, vOld)

        project(u, v, uOld, vOld)


        addSource(r, rOld)
        swapR()

        diffuse(0, r, rOld, 0.0)
        swapR()

        advect(0, r, rOld, u, v)
        fadeR()
    }

    private fun fadeR() {
        // I want the fluid to gradually fade out so the screen doesn't fill.
        // the amount it fades out depends on how full it is, and how uniform
        // (i.e. boring) the fluid is...
        // float holdAmount = 1 - _avgDensity * _avgDensity * _fadeSpeed;
        // this is how fast the density will decay depending on how full the
        // screen currently is
        val holdAmount = 1 - fadeSpeed

        avgDensity = 0.0
        avgSpeed = 0.0

        var totalDeviations = 0.0
        var currentDeviation: Double

        // float uniformityMult = uniformity * 0.05f;
        avgSpeed = 0.0
        for (i in 0..<numCells) {
            // clear old values
            vOld[i] = 0.0
            uOld[i] = vOld[i]
            rOld[i] = 0.0

            // gOld[i] = bOld[i] = 0;

            // calc avg speed
            avgSpeed += u[i] * u[i] + v[i] * v[i]

            // calc avg density
            r[i] = min(1.0, r[i].toDouble()).toDouble()
            // float density = Math.max(r[i], Math.max(g[i], b[i]));
            val density = r[i]
            avgDensity += density // add it up

            // calc deviation (for uniformity)
            currentDeviation = density - avgDensity
            totalDeviations += currentDeviation * currentDeviation

            // fade out old
            r[i] *= holdAmount
        }
        avgDensity *= invNumCells

        // _avgSpeed *= _invNumCells;

        avgUniformity = 1.0 / (1 + totalDeviations * invNumCells)
        // 0: very wide distribution
        // 1: very uniform
    }

    private fun addSourceUV() {
        for (i in 0..<numCells) {
            u[i] += dt * uOld[i]
            v[i] += dt * vOld[i]
        }
    }

    private fun addSource(x: DoubleArray, x0: DoubleArray) {
        for (i in 0..<numCells) {
            x[i] += dt * x0[i]
        }
    }

    private fun advect(b: Int, d: DoubleArray, d0: DoubleArray, du: DoubleArray, dv: DoubleArray) {
        val dt0 = dt * nx

        for (i in 1..nx) {
            for (j in 1..ny) {
                var x = i - dt0 * du[fluidIX(i, j)]
                var y = j - dt0 * dv[fluidIX(i, j)]

                if (x > nx + 0.5) x = nx + 0.5
                if (x < 0.5) x = 0.5

                val i0 = x.toInt()
                val i1 = i0 + 1

                if (y > ny + 0.5) y = ny + 0.5
                if (y < 0.5) y = 0.5

                val j0 = y.toInt()
                val j1 = j0 + 1

                val s1 = x - i0
                val s0 = 1 - s1
                val t1 = y - j0
                val t0 = 1 - t1

                d[fluidIX(i, j)] = (s0 * (t0 * d0[fluidIX(i0, j0)] + t1 * d0[fluidIX(i0, j1)])
                    + s1 * (t0 * d0[fluidIX(i1, j0)] + t1 * d0[fluidIX(i1, j1)]))
            }
        }
        setBoundary(b, d)
    }

    private fun diffuse(b: Int, c: DoubleArray, c0: DoubleArray, diff: Double) {
        val a = dt * diff * nx * ny
        linearSolver(b, c, c0, a, 1.0 + 4 * a)
    }

    private fun diffuseUV(b: Int, diff: Double) {
        val a = dt * diff * nx * ny
        linearSolverUV(b, a, 1.0 + 4 * a)
    }

    private fun project(x: DoubleArray, y: DoubleArray, p: DoubleArray, div: DoubleArray) {
        for (i in 1..nx) {
            for (j in 1..ny) {
                div[fluidIX(i, j)] = (x[fluidIX(i + 1, j)] - x[fluidIX(i - 1, j)]
                    + y[fluidIX(i, j + 1)] - y[fluidIX(i, j - 1)]) * -0.5f / nx
                p[fluidIX(i, j)] = 0.0
            }
        }

        setBoundary(0, div)
        setBoundary(0, p)

        linearSolver(0, p, div, 1.0, 4.0)

        for (i in 1..nx) {
            for (j in 1..ny) {
                x[fluidIX(i, j)] -= 0.5 * nx * (p[fluidIX(i + 1, j)] - p[fluidIX(i - 1, j)])
                y[fluidIX(i, j)] -= 0.5 * nx * (p[fluidIX(i, j + 1)] - p[fluidIX(i, j - 1)])
            }
        }

        setBoundary(1, x)
        setBoundary(2, y)
    }

    private fun linearSolver(b: Int, x: DoubleArray, x0: DoubleArray, a: Double, c: Double) {
        for (k in 0..<solverIterations) {
            for (i in 1..nx) {
                for (j in 1..ny) {
                    x[fluidIX(
                        i,
                        j
                    )] = (a * (x[fluidIX(i - 1, j)] + x[fluidIX(i + 1, j)]
                        + x[fluidIX(i, j - 1)] + x[fluidIX(i, j + 1)])
                        + x0[fluidIX(i, j)]) / c
                }
            }
            setBoundary(b, x)
        }
    }

    private fun linearSolverUV(bound: Int, a: Double, c: Double) {
        for (k in 0..<solverIterations) { // MEMO
            for (i in 1..nx) {
                for (j in 1..ny) {
                    val index5 = fluidIX(i, j)
                    val index1 = index5 - 1 // FLUID_IX(i-1, j);
                    val index2 = index5 + 1 // FLUID_IX(i+1, j);
                    val index3 = index5 - (nx + 2) // FLUID_IX(i, j-1);
                    val index4 = index5 + (nx + 2) // FLUID_IX(i, j+1);

                    u[index5] = ((a * (u[index1] + u[index2] + u[index3] + u[index4]) + uOld[index5]) / c)
                    v[index5] = ((a * (v[index1] + v[index2] + v[index3] + v[index4]) + vOld[index5]) / c)
                    // x[FLUID_IX(i, j)] = (a * ( x[FLUID_IX(i-1, j)] +
                    // x[FLUID_IX(i+1, j)] + x[FLUID_IX(i, j-1)] + x[FLUID_IX(i,
                    // j+1)]) + x0[FLUID_IX(i, j)]) / c;
                }
            }
            setBoundaryRGB(bound)
        }
    }

    private fun setBoundary(b: Int, x: DoubleArray) {
        // return;
        for (i in 1..nx) {
            if (i <= ny) {
                x[fluidIX(0, i)] = if (b == 1) -x[fluidIX(1, i)] else x[fluidIX(1, i)]
                x[fluidIX(nx + 1, i)] = if (b == 1) -x[fluidIX(nx, i)] else x[fluidIX(nx, i)]
            }

            x[fluidIX(i, 0)] = if (b == 2) -x[fluidIX(i, 1)] else x[fluidIX(i, 1)]
            x[fluidIX(i, ny + 1)] = if (b == 2) -x[fluidIX(i, ny)] else x[fluidIX(i, ny)]
        }

        x[fluidIX(0, 0)] = 0.5f * (x[fluidIX(1, 0)] + x[fluidIX(0, 1)])
        x[fluidIX(0, ny + 1)] = 0.5f * (x[fluidIX(1, ny + 1)] + x[fluidIX(0, ny)])
        x[fluidIX(nx + 1, 0)] = 0.5f * (x[fluidIX(nx, 0)] + x[fluidIX(nx + 1, 1)])
        x[fluidIX(nx + 1, ny + 1)] = (0.5f
            * (x[fluidIX(nx, ny + 1)] + x[fluidIX(nx + 1, ny)]))
    }

    private fun setBoundaryRGB(bound: Int) {
        var index1: Int
        var index2: Int
        for (i in 1..nx) {
            if (i <= ny) {
                index1 = fluidIX(0, i)
                index2 = fluidIX(1, i)
                r[index1] = if (bound == 1) -r[index2] else r[index2]

                index1 = fluidIX(nx + 1, i)
                index2 = fluidIX(nx, i)
                r[index1] = if (bound == 1) -r[index2] else r[index2]
            }

            index1 = fluidIX(i, 0)
            index2 = fluidIX(i, 1)
            r[index1] = if (bound == 2) -r[index2] else r[index2]

            index1 = fluidIX(i, ny + 1)
            index2 = fluidIX(i, ny)
            r[index1] = if (bound == 2) -r[index2] else r[index2]
        }

        // x[FLUID_IX( 0, 0)] = 0.5f * (x[FLUID_IX(1, 0 )] + x[FLUID_IX( 0,
        // 1)]);
        // x[FLUID_IX( 0, _NY+1)] = 0.5f * (x[FLUID_IX(1, _NY+1)] + x[FLUID_IX(
        // 0, _NY)]);
        // x[FLUID_IX(_NX+1, 0)] = 0.5f * (x[FLUID_IX(_NX, 0 )] +
        // x[FLUID_IX(_NX+1, 1)]);
        // x[FLUID_IX(_NX+1, _NY+1)] = 0.5f * (x[FLUID_IX(_NX, _NY+1)] +
        // x[FLUID_IX(_NX+1, _NY)]);
    }

    private fun swapU() {
        val tmp = u
        u = uOld
        uOld = tmp
    }

    private fun swapV() {
        val tmp = v
        v = vOld
        vOld = tmp
    }

    private fun swapR() {
        val tmp = r
        r = rOld
        rOld = tmp
    }

    private fun fluidIX(i: Int, j: Int) = ((i) + (nx + 2) * (j))

    // protected void ADD_SOURCE_UV() { addSource(u, uOld); addSource(v, vOld);}
    // protected void DIFFUSE_UV() { diffuse(0, u, uOld, visc); diffuse(0, v, vOld, visc); }

    fun u(i: Int, j: Int) = u[fluidIX(i, j)]
    fun v(i: Int, j: Int) = v[fluidIX(i, j)]

}
