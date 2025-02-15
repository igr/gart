package dev.oblac.gart.fluid.navstr

import kotlin.math.max
import kotlin.math.pow

/**
 * Solves the compressible navier stokes equation.
 *
 * The equation is solved using a backward time central space scheme.
 * The continuity equation is solved using a forward time central space scheme.
 * The energy equation is solved using a forward time central space scheme.
 *
 * An Alternating direction implicit method to account for the two dimensions.
 *
 * The calculation is done with a staggered grid where the velocities are calculated
 * at the nodes and pressure, density and velocity are calculated in the center of each cell.
 *
 * Some simplification has been made, ie the divergence part of the NS equation is not included
 *
 * To ensure stability, dt should be less than: 0.002*Math.min(dx, dy)
 *
 * Known issues:
 * 	-	No inlet or outlet
 * 	-	Only periodic in all directions or none
 * 	-	Is not optimized for speed.
 *
 * Howto simulate:
 * 	-	Initially the simulation starts with T=300K, P=1atm, u=v=0
 * 	-	Either set the velocity at a single point with setVel(i,j,vel,x-dir)
 * 		or add/remove energy in a cell with the function addEnergy(i,j,energy)
 * 		this could be heat or radiation
 * 	-	Step the simulation one timestep with step(dt)
 * 	-	Read the velocity/pressure/temperature with the functions: getU,getV,getP,getT
 */
@Suppress("t")
class NSSolverImplicit(
    val nx: Int,
    val ny: Int,
    val dx: Double,
    val dy: Double,
    val numIter: Int = 3 // number of iterations with implicit solver
) {
    private val periodic: Boolean = true

    private var relax: Double = 0.0 // Relaxation factor. Should not be used for transient

    private var negativeDensity: Boolean = false

    private val u = Array(nx) { DoubleArray(ny) } // velocity x
    private val v = Array(nx) { DoubleArray(ny) } // velocity y
    private var uPrev: Array<DoubleArray>
    private var vPrev: Array<DoubleArray>
    private val p = Array(nx) { DoubleArray(ny) } // pressure

    private val e = Array(nx) { DoubleArray(ny) } // internal energy
    private var ePrev: Array<DoubleArray>
    private val r = Array(nx) { DoubleArray(ny) } // density
    private var rPrev: Array<DoubleArray>
    private val t = Array(nx) { DoubleArray(ny) } // temperature in Kelvin
    private var tPrev: Array<DoubleArray>

    private var eAdd = ArrayList<DoubleArray>() // A list of

    // containing points which should have its energy increased
    private val wall = Array(nx) { BooleanArray(ny) }

    private val viscosity = 1.81e-5

    private var cv: Double = 0.001297 * (100 * 100 * 100) // Heat Capacity at

    // constant
    // volume J/(m3*K)
    private var R: Double = 287.04 // Specific gas constant Air

    private fun copy2DArray(src: Array<DoubleArray>): Array<DoubleArray> =
        Array(src.size) { src[it].copyOf() }

    init {
        cv *= (dx * dy)

        for (i in 0..<nx) {
            for (j in 0..<ny) {
                wall[i][j] = false
                u[i][j] = 0.0
                v[i][j] = 0.0
                p[i][j] = 101130.0
                e[i][j] = 0.0
                t[i][j] = 300.0
                r[i][j] = p[i][j] / (R * t[i][j])
            }
        }
        uPrev = copy2DArray(u)
        vPrev = copy2DArray(v)
        ePrev = copy2DArray(e)
        tPrev = copy2DArray(t)
        rPrev = copy2DArray(r)
    }

    /**
     * Adds energy to the cell.
     */
    fun addEnergy(i: Int, j: Int, de: Double) {
        val l = doubleArrayOf(i.toDouble(), j.toDouble(), de / r[i][j])
        eAdd.add(l)
    }

    fun addPressure(i: Int, j: Int, de: Double) {
        rPrev[i][j] += de / (R * t[i][j])
    }

    /**
     * Calculates the velocity at the staggered grid position.
     */
    fun uS(i: Int, j: Int): Double {
        return (u[i][j] + u[i][j + 1] + u[i + 1][j] + u[i + 1][j + 1]) / 4
    }

    /**
     * Calculates the velocity at the staggered grid position.
     */
    fun vS(i: Int, j: Int): Double {
        return (v[i][j] + v[i][j + 1] + v[i + 1][j] + v[i + 1][j + 1]) / 4
    }

    private fun mod(x: Int, y: Int): Int = (x % y).let { if (it < 0) it + y else it }

    /**
     * Calculates the new density.
     */
    private fun stepRho(dt: Double, stepI: Boolean) {
        var start = 1
        if (periodic) {
            start = 0
        }

//        val a = DoubleArray(max((nx - 1).toDouble(), (ny - 1).toDouble()).toInt())
//        val b = DoubleArray(max((nx - 1).toDouble(), (ny - 1).toDouble()).toInt())
//        val c = DoubleArray(max((nx - 1).toDouble(), (ny - 1).toDouble()).toInt())
//        val s = DoubleArray(max((nx - 1).toDouble(), (ny - 1).toDouble()).toInt())
//        val x = DoubleArray(max((nx - 1).toDouble(), (ny - 1).toDouble()).toInt())

        for (i in 0..<nx - start) {
            val ineg = mod(i - 1, nx)
            val ipos = mod(i + 1, nx)

            for (j in 0..<ny - start) {
                val jneg = mod(j - 1, ny)
                val jpos = mod(j + 1, ny)
                val rp = rPrev[i][j]
                val uw = (u[i][j] + u[i][jpos]) / 2
                val ue = (u[ipos][j] + u[ipos][jpos]) / 2
                val us = (u[i][jpos] + u[ipos][jpos]) / 2
                val un = (u[i][j] + u[ipos][j]) / 2
                val vw = (v[i][j] + v[i][jpos]) / 2
                val ve = (v[ipos][j] + v[ipos][jpos]) / 2
                val vs = (v[i][jpos] + v[ipos][jpos]) / 2
                val vn = (v[i][j] + v[ipos][j]) / 2
                r[i][j] = rPrev[i][j] + (rp * ((uw - ue) / dx + (vn - vs) / dy)
                    * dt)
                if (r[i][j] < 0) {
                    negativeDensity = true
                }
            }
        }
    }

    /**
     * Zero out the wall velocities.
     */
    fun applyWall() {
        for (i in 0..<nx) {
            for (j in 0..<ny) {
                if (wall[i][j]) {
                    uPrev[i][j] = 0.0
                    vPrev[i][j] = 0.0
                    u[i][j] = 0.0
                    v[i][j] = 0.0
                }
            }
        }
    }

    /**
     * Calculates with TDMA - Implicit solver.
     */
    fun stepVel(dt: Double, calcU: Boolean, stepI: Boolean) {
        val calcV = !calcU
        var rho: Double

        for (ni in 0..<numIter) {
            stepT()
            stepRho(dt, stepI)
            stepP()
            stepE(dt)

            // applyWall();
            var a = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())
            var b = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())
            var c = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())
            var s = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())
            var x = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())

            var k = 1
            var start = 1
            if (periodic) {
                start = 0
            }

            if (calcU) {
                // Calculate U TMDA in I dir

                if (stepI) {
                    var j = start
                    while (j > (start - 1)) {
                        val jneg = mod(j - 1, ny)
                        val jpos = mod(j + 1, ny)
                        for (i in start..<nx - start) {
                            val ineg = mod(i - 1, nx)
                            val ipos = mod(i + 1, nx)

                            rho = (r[i][j] + r[i][jneg] + r[ineg][j] + r[ineg][jneg]) / 4

                            val Cw = (-rho * u[i][j] / (2 * dx) - viscosity / (dx * dx))
                            val C = (rho / (dt) + 2 * viscosity / (dx * dx) + (2 * viscosity
                                / (dy * dy)))
                            val Ce = (rho * u[i][j] / (2 * dx) - viscosity / (dx * dx))
                            val Cn = (-rho * v[i][j] / (2 * dy) - viscosity / (dy * dy))
                            val Cs = (rho * v[i][j] / (2 * dy) - viscosity / (dy * dy))

                            a[i - start] = Ce
                            b[i - start] = C
                            c[i - start] = Cw

                            s[i - start] = ((rho
                                * uPrev[i][j]
                                / dt
                                ) - (((p[i][jneg] + p[i][j]) / 2 - (p[ineg][j] + p[ineg][jneg]) / 2)
                                / (dx)
                                ) - (Cs * u[i][jpos] + Cn * u[i][jneg]))
                        }
                        x = if (periodic) {
                            solveTDMAP(nx, a, b, c, s)
                        } else {
                            solveTDMA(nx - 2, a, b, c, s)
                        }

                        for (i in start..<nx - start) {
                            u[i][j] = (u[i][j] * relax + x[i - start]
                                * (1 - relax))
                        }
                        if (j == ny - 1 - start) {
                            k *= -1
                        }
                        j += k
                    }
                } else {
                    // Calculate U TMDA in J dir
                    var i = start
                    while (i > (start - 1)) {
                        val ineg = mod(i - 1, nx)
                        val ipos = mod(i + 1, nx)
                        for (j in start..<ny - start) {
                            val jneg = mod(j - 1, ny)
                            val jpos = mod(j + 1, ny)

                            rho = (r[i][j] + r[i][jneg] + r[ineg][j] + r[ineg][jneg]) / 4

                            val Cw = (-rho * u[i][j] / (2 * dx) - viscosity / (dx * dx))
                            val C = (rho / dt + 2 * viscosity / (dx * dx) + (2 * viscosity / (dy * dy)))
                            val Ce = (rho * u[i][j] / (2 * dx) - viscosity / (dx * dx))
                            val Cn = (-rho * v[i][j] / (2 * dy) - viscosity / (dy * dy))
                            val Cs = (rho * v[i][j] / (2 * dy) - viscosity / (dy * dy))

                            a[j - start] = Cs
                            b[j - start] = C
                            c[j - start] = Cn

                            s[j - start] = ((rho * uPrev[i][j] / dt)
                                - (((p[i][jneg] + p[i][j]) / 2 - (p[ineg][j] + p[ineg][jneg]) / 2) / (dx))
                                - (Ce * u[ipos][j] + Cw * u[ineg][j]))
                        }

                        x = if (periodic) {
                            solveTDMAP(ny, a, b, c, s)
                        } else {
                            solveTDMA(ny - 2, a, b, c, s)
                        }
                        for (j in start..<ny - start) {
                            u[i][j] = (u[i][j] * relax + x[j - start]
                                * (1 - relax))
                        }

                        if (i == nx - 1 - start) {
                            k *= -1
                        }
                        i += k
                    }
                }
            }
            if (calcV) {
                a = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())
                b = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())
                c = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())
                s = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())
                x = DoubleArray(max(nx.toDouble(), ny.toDouble()).toInt())

                // Calculate V TMDA in J dir
                if (!stepI) {
                    var i = start
                    while (i > (start - 1)) {
                        val ineg = mod(i - 1, nx)
                        val ipos = mod(i + 1, nx)

                        for (j in start..<ny - start) {
                            val jneg = mod(j - 1, ny)
                            val jpos = mod(j + 1, ny)

                            rho = (r[i][j] + r[i][jneg] + r[ineg][j] + r[ineg][jneg]) / 4

                            val Cw = (-rho * u[i][j] / (2 * dx) - viscosity / (dx * dx))
                            val C = (rho / dt + 2 * viscosity / (dx * dx) + (2 * viscosity
                                / (dy * dy)))
                            val Ce = (rho * u[i][j] / (2 * dx) - viscosity / (dx * dx))
                            val Cn = (-rho * v[i][j] / (2 * dy) - viscosity / (dy * dy))
                            val Cs = (rho * v[i][j] / (2 * dy) - viscosity / (dy * dy))

                            a[j - start] = Cs
                            b[j - start] = C
                            c[j - start] = Cn

                            s[j - start] = ((rho * vPrev[i][j] / dt)
                                - (((p[i][j] + p[ineg][j]) / 2 - (p[i][jneg] + p[ineg][jneg]) / 2) / (dy))
                                - (Ce * v[ipos][j] + Cw * v[ineg][j]))
                        }

                        x = if (periodic) {
                            solveTDMAP(ny, a, b, c, s)
                        } else {
                            solveTDMA(ny - 2, a, b, c, s)
                        }
                        for (j in start..<ny - start) {
                            v[i][j] = (v[i][j] * relax + x[j - start]
                                * (1 - relax))
                        }

                        if (i == nx - 1 - start) {
                            k *= -1
                        }
                        i += k
                    }
                } else {
                    // Calculate V TMDA in I dir
                    var j = start
                    while (j > (start - 1)) {
                        // Positive j direction
                        val jneg = mod(j - 1, ny)
                        val jpos = mod(j + 1, ny)

                        for (i in start..<nx - start) {
                            val ineg = mod(i - 1, nx)
                            val ipos = mod(i + 1, nx)

                            rho = (r[i][j] + r[i][jneg] + r[ineg][j] + r[ineg][jneg]) / 4

                            val Cw = (-rho * u[i][j] / (2 * dx) - viscosity / (dx * dx))
                            val C = (rho / dt + 2 * viscosity / (dx * dx) + (2 * viscosity
                                / (dy * dy)))
                            val Ce = (rho * u[i][j] / (2 * dx) - viscosity / (dx * dx))
                            val Cn = (-rho * v[i][j] / (2 * dy) - viscosity / (dy * dy))
                            val Cs = (rho * v[i][j] / (2 * dy) - viscosity / (dy * dy))

                            a[i - start] = Ce
                            b[i - start] = C
                            c[i - start] = Cw

                            s[i - start] = ((rho * vPrev[i][j] / dt)
                                - (((p[i][j] + p[ineg][j]) / 2 - (p[i][jneg] + p[ineg][jneg]) / 2) / (dy))
                                - (Cs * v[i][jpos] + Cn * v[i][jneg]))
                        }

                        x = if (periodic) {
                            solveTDMAP(nx, a, b, c, s)
                        } else {
                            solveTDMA(nx - 2, a, b, c, s)
                        }
                        for (i in start..<nx - start) {
                            v[i][j] = (v[i][j] * relax + x[i - start]
                                * (1 - relax))
                        }

                        if (j == ny - 1 - start) {
                            k *= -1
                        }
                        j += k
                    }
                }
            }
        }
    }

    fun stepE(dt: Double) { // Calculate the new energy

        var start = 1
        if (periodic) {
            start = 0
        }

        for (i in 0..<nx - start) {
            val ineg = mod(i - 1, nx)
            val ipos = mod(i + 1, nx)

            for (j in 0..<ny - start) {
                val jneg = mod(j - 1, ny)
                val jpos = mod(j + 1, ny)
                val rp = rPrev[i][j]
                val uw = (u[i][j] + u[i][jpos]) / 2
                val ue = (u[ipos][j] + u[ipos][jpos]) / 2
                val us = (u[i][jpos] + u[ipos][jpos]) / 2
                val un = (u[i][j] + u[ipos][j]) / 2
                val vw = (v[i][j] + v[i][jpos]) / 2
                val ve = (v[ipos][j] + v[ipos][jpos]) / 2
                val vs = (v[i][jpos] + v[ipos][jpos]) / 2
                val vn = (v[i][j] + v[ipos][j]) / 2
                e[i][j] = (ePrev[i][j]
                    + ((uw * (ePrev[ineg][j] + p[ineg][j]) - ue
                    * (ePrev[ipos][j] + p[ipos][j]))
                    / dx + (vn * (ePrev[i][jneg] + p[i][jneg]) - vs
                    * (ePrev[i][jpos] + p[i][jpos]))
                    / dy) * dt)
            }
        }
        for (i in eAdd.indices) {
            val item = eAdd[i]
            val ii = item[0].toInt()
            val jj = item[1].toInt()
            val de = item[2] / 4
            e[ii][jj] += de

            // Add 1/4th of the energy as this is done four times
            // see step()
        }
    }

    /**
     * Calculates new temperatures.
     */
    private fun stepT() {
        for (i in 0..<nx) {
            for (j in 0..<ny) {
                val e1 = ((e[i][j]) / r[i][j] - 0.5
                    * (u[i][j].pow(2.0) + v[i][j].pow(2.0)))
                val e2 = ((ePrev[i][j]) / rPrev[i][j] - 0.5
                    * (uPrev[i][j].pow(2.0) + vPrev[i][j].pow(2.0)))
                t[i][j] += (e1 - e2) / (cv)
            }
        }
    }

    /**
     * Calculates new Pressure from ideal gass law.
     */
    private fun stepP() { //
        for (i in 0..<nx) {
            for (j in 0..<ny) {
                p[i][j] = p[i][j] * (relax) + (r[i][j] * R * t[i][j] * (1 - relax))
            }
        }
    }

    /**
     * Steps the function one time step, dt
     */
    fun step(dt: Double) {

        stepVel(dt / 4, true, true)

        uPrev = copy2DArray(u)
        vPrev = copy2DArray(v)
        ePrev = copy2DArray(e)
        tPrev = copy2DArray(t)
        rPrev = copy2DArray(r)

        stepVel(dt / 4, true, false)

        uPrev = copy2DArray(u)
        vPrev = copy2DArray(v)
        ePrev = copy2DArray(e)
        tPrev = copy2DArray(t)
        rPrev = copy2DArray(r)
        stepVel(dt / 4, false, true)

        uPrev = copy2DArray(u)
        vPrev = copy2DArray(v)
        ePrev = copy2DArray(e)
        tPrev = copy2DArray(t)
        rPrev = copy2DArray(r)

        stepVel(dt / 4, false, false)

        uPrev = copy2DArray(u)
        vPrev = copy2DArray(v)
        ePrev = copy2DArray(e)
        tPrev = copy2DArray(t)
        rPrev = copy2DArray(r)

        // Reset the added energy
        eAdd = ArrayList()
    }

    fun p(i: Int, j: Int): Double = p[i][j]

    fun e(i: Int, j: Int): Double = e[i][j]

    fun t(i: Int, j: Int): Double = t[i][j]

    fun u(i: Int, j: Int): Double = u[i][j]

    fun v(i: Int, j: Int): Double = v[i][j]

    fun r(i: Int, j: Int): Double = r[i][j]

    fun setWall(i: Int, j: Int) {
        wall[i][j] = true
    }

    fun setVelocity(i: Int, j: Int, vel: Double, udir: Boolean) {
        if (udir) {
            u[i][j] = vel
            uPrev[i][j] = vel
        } else {
            vPrev[i][j] = vel
            v[i][j] = vel
        }
    }
}
