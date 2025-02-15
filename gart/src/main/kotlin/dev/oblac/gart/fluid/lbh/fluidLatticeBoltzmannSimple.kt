package dev.oblac.gart.fluid.lbh

import dev.oblac.gart.math.hypotFast

// Define the lattice Boltzmann weights
private val WEIGHTS = doubleArrayOf(
    4.0 / 9,
    1.0 / 9,
    1.0 / 9,
    1.0 / 9,
    1.0 / 9,
    1.0 / 36,
    1.0 / 36,
    1.0 / 36,
    1.0 / 36
)

private data class Direction(val x: Int, val y: Int)

private val DIRECTIONS = arrayOf(
    Direction(0, 0),
    Direction(1, 0),
    Direction(0, 1),
    Direction(-1, 0),
    Direction(0, -1),
    Direction(1, 1),
    Direction(-1, 1),
    Direction(-1, -1),
    Direction(1, -1)
)

data class Lattice(
    var density: Double = 0.0,
    var velocityX: Double = 0.0,
    var velocityY: Double = 0.0,
    val f: DoubleArray = DoubleArray(9) { 0.0 },
    val fEq: DoubleArray = DoubleArray(9) { 0.0 }
) {
    /**
     * Calculates velocity of the lattice.
     */
    fun velocity() = hypotFast(velocityX, velocityY)
}

class LatticeBoltzmannSimpleFluid(
    val width: Int,
    val height: Int,
    private val viscosity: Double = 0.01,
    private val relaxationParam: Double = 1 / (3 * viscosity + 0.5f)
) {
    private val lattices: Array<Array<Lattice>> = Array(width) { Array(height) { Lattice() } }

    fun init(latticeInitConsumer: (Lattice, Int, Int) -> Unit) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val lattice = lattices[x][y]
                latticeInitConsumer(lattice, x, y)
                for (j in 0 until 9) {
                    lattice.f[j] = WEIGHTS[j] * lattice.density
                    lattice.fEq[j] = WEIGHTS[j] * lattice.density
                }
            }
        }
    }

    // Compute the equilibrium distribution function
    private fun equilibrium(lattice: Lattice) {
        val localDensity = lattice.density
        val localVelocityX = lattice.velocityX
        val localVelocityY = lattice.velocityY

        for (j in 0 until 9) {
            val dotProduct = DIRECTIONS[j].x * localVelocityX + DIRECTIONS[j].y * localVelocityY
            lattice.fEq[j] = WEIGHTS[j] * localDensity * (1 + 3 * dotProduct + 4.5f * dotProduct * dotProduct - 1.5f * (localVelocityX * localVelocityX + localVelocityY * localVelocityY))
        }
    }

    private fun collide() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                equilibrium(lattices[x][y])
                for (j in 0 until 9) {
                    lattices[x][y].f[j] = lattices[x][y].f[j] - relaxationParam * (lattices[x][y].f[j] - lattices[x][y].fEq[j])
                }
            }
        }
    }


    // Perform a streaming step
    private fun stream() {
        val newLattice = Array(width) { Array(height) { Lattice() } }

        for (x in 0 until width) {
            for (y in 0 until height) {
                for (j in 0 until 9) {
                    val newX = (x + DIRECTIONS[j].x + width) % width
                    val newY = (y + DIRECTIONS[j].y + height) % height
                    newLattice[newX][newY].f[j] = lattices[x][y].f[j]
                }
            }
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                lattices[x][y] = newLattice[x][y]
            }
        }
    }

    // Update the density and velocity
    private fun update() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                lattices[x][y].density = 0.0
                lattices[x][y].velocityX = 0.0
                lattices[x][y].velocityY = 0.0

                for (j in 0 until 9) {
                    lattices[x][y].density += lattices[x][y].f[j]
                    lattices[x][y].velocityX += DIRECTIONS[j].x * lattices[x][y].f[j]
                    lattices[x][y].velocityY += DIRECTIONS[j].y * lattices[x][y].f[j]
                }

                lattices[x][y].velocityX /= lattices[x][y].density
                lattices[x][y].velocityY /= lattices[x][y].density
            }
        }
    }

    fun simulate() {
        collide()
        stream()
        update()
    }

    fun lattices(latticeConsumer: (lattice: Lattice, x: Int, y: Int) -> Unit) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                latticeConsumer(lattices[x][y], x, y)
            }
        }
    }

    operator fun get(x: Int, y: Int) = lattices[x][y]
}
