package dev.oblac.gart.fluid.lbh


@Suppress("t")
class BoltzmannFluid(
    private val overallVelocity: Double,     // wind speed
    private val viscosity: Double,
    private val rows: Int,
    private val cols: Int
) {
    // discretized thermal velocities for each of the 9 possible directions
    private val velocityHere: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val velocityUp: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val velocityDown: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val velocityRight: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val velocityLeft: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val velocityNorthWest: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val velocityNorthEast: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val velocitySouthWest: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val velocitySouthEast: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }

    private val solid: Array<BooleanArray> = Array(rows) { BooleanArray(cols) } //indicates if a solid boundary is present

    //for UI purposes; not used in calculations
    private val density: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    //for UI purposes; not used in calculations
    private val vSquared: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val vX: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }
    private val vY: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }

    init {
        // initiate fluid with discretized velocity vectors
        // the probabilities derived from Boltzmann distribution; Weber State University paper
        // it does not matter if element is solid as the solver disregards velocity data
        // lots of values are precalculated here to make sure no unnecessary operations are performed

        val here = 4.0 / 9.0 * (1 - 1.5 * overallVelocity * overallVelocity)
        val up = 1.0 / 9.0 * (1 - 1.5 * overallVelocity * overallVelocity)
        val down = 1.0 / 9.0 * (1 - 1.5 * overallVelocity * overallVelocity)
        val left = 1.0 / 9.0 * (1 - 3.0 * overallVelocity + 3.0 * overallVelocity * overallVelocity)
        val right = 1.0 / 9.0 * (1 + 3.0 * overallVelocity + 3.0 * overallVelocity * overallVelocity)
        val northEast = 1.0 / 36.0 * (1 + 3.0 * overallVelocity + 3.0 * overallVelocity * overallVelocity)
        val northWest = 1.0 / 36.0 * (1 - 3.0 * overallVelocity + 3.0 * overallVelocity * overallVelocity)
        val southEast = 1.0 / 36.0 * (1 + 3.0 * overallVelocity + 3.0 * overallVelocity * overallVelocity)
        val southWest = 1.0 / 36.0 * (1 - 3.0 * overallVelocity + 3.0 * overallVelocity * overallVelocity)

        for (r in 0..<rows) {
            for (c in 0..<cols) {
                if (solid[r][c]) {
                    // clear the values
                    velocityHere[r][c] = 0.0
                    velocityRight[r][c] = 0.0
                    velocityLeft[r][c] = 0.0
                    velocityUp[r][c] = 0.0
                    velocityDown[r][c] = 0.0
                    velocityNorthEast[r][c] = 0.0
                    velocityNorthWest[r][c] = 0.0
                    velocitySouthEast[r][c] = 0.0
                    velocitySouthWest[r][c] = 0.0
                    vSquared[r][c] = 0.0
                } else {
                    // init values for each element
                    velocityHere[r][c] = here
                    velocityRight[r][c] = right
                    velocityLeft[r][c] = left
                    velocityUp[r][c] = up
                    velocityDown[r][c] = down
                    velocityNorthEast[r][c] = northEast
                    velocitySouthEast[r][c] = southEast
                    velocityNorthWest[r][c] = northWest
                    velocitySouthWest[r][c] = southWest
                    density[r][c] = 1.0
                    vSquared[r][c] = overallVelocity * overallVelocity
                }
            }
        }
    }

    /**
     * Performs one single iteration of the algorithm.
     */
    fun iterate() {
        collide()
        move()
        collideBoundary()
    }

    private fun collide() {
        var sumVelocities: Double
        val relaxationTime = 1 / (3.0 * viscosity + 0.5) // omega in the equation; mostly an experimental value. Tweak for different results.

        for (x in 0..<rows) {
            for (y in 0..<cols) {
                if (!solid[x][y]) {
                    sumVelocities = (velocityHere[x][y] + velocityUp[x][y] + velocityDown[x][y] + velocityRight[x][y]
                        + velocityLeft[x][y] + velocityNorthWest[x][y] + velocityNorthEast[x][y] + velocitySouthWest[x][y]
                        + velocitySouthEast[x][y])

                    density[x][y] = sumVelocities //sets total density rho for UI
                    val xVelocity: Double //flow velocity in x basis
                    val yVelocity: Double //flow velocity in y basis

                    // calculate the flow velocities
                    if (sumVelocities > 0) {
                        xVelocity = (((velocityRight[x][y] + velocityNorthEast[x][y]
                            + velocitySouthEast[x][y]) - velocityLeft[x][y]
                            - velocityNorthWest[x][y] - velocitySouthWest[x][y])) / sumVelocities
                        yVelocity = (((velocityUp[x][y] + velocityNorthEast[x][y]
                            + velocityNorthWest[x][y]) - velocityDown[x][y]
                            - velocitySouthEast[x][y] - velocitySouthWest[x][y])) / sumVelocities
                    } else {
                        xVelocity = 0.0
                        yVelocity = 0.0
                    }


                    // constants for the following calculations, different for every iteration of the loop
                    val threeXVelocity = 3.0 * xVelocity
                    val threeYVelocity = 3.0 * yVelocity
                    val xVelocitySquared = xVelocity * xVelocity
                    val yVelocitySquared = yVelocity * yVelocity
                    val twoXYVelocities = 2 * xVelocity * yVelocity
                    val sumSquares = xVelocitySquared + yVelocitySquared

                    vSquared[x][y] = sumSquares
                    vX[x][y] = xVelocity
                    vY[x][y] = yVelocity

                    // Easy replacement for the magnitude of the curl, which is more difficult to calculate

                    // sets thermal velocities after collision
                    velocityHere[x][y] += relaxationTime * (((4.0 / 9.0) * sumVelocities
                        * (1 - 1.5 * sumSquares)) - velocityHere[x][y])
                    velocityRight[x][y] += relaxationTime * (((1.0 / 9.0) * sumVelocities
                        * (1 + threeXVelocity + 4.5 * xVelocitySquared - 1.5 * sumSquares)) - velocityRight[x][y])
                    velocityLeft[x][y] += relaxationTime * (((1.0 / 9.0) * sumVelocities
                        * (1 - threeXVelocity + 4.5 * xVelocitySquared - 1.5 * sumSquares)) - velocityLeft[x][y])
                    velocityUp[x][y] += relaxationTime * (((1.0 / 9.0) * sumVelocities
                        * (1 + threeYVelocity + 4.5 * yVelocitySquared - 1.5 * sumSquares)) - velocityUp[x][y])
                    velocityDown[x][y] += relaxationTime * (((1.0 / 9.0) * sumVelocities
                        * (1 - threeYVelocity + 4.5 * yVelocitySquared - 1.5 * sumSquares)) - velocityDown[x][y])

                    velocityNorthEast[x][y] += relaxationTime * (((1.0 / 36.0) * sumVelocities
                        * (1 + threeXVelocity + threeYVelocity + 4.5 * (sumSquares + twoXYVelocities) - 1.5 * sumSquares)) - velocityNorthEast[x][y])
                    velocityNorthWest[x][y] += relaxationTime * (((1.0 / 36.0) * sumVelocities
                        * (1 - threeXVelocity + threeYVelocity + 4.5 * (sumSquares - twoXYVelocities) - 1.5 * sumSquares)) - velocityNorthWest[x][y])
                    velocitySouthEast[x][y] += relaxationTime * (((1.0 / 36.0) * sumVelocities
                        * (1 + threeXVelocity - threeYVelocity + 4.5 * (sumSquares - twoXYVelocities) - 1.5 * sumSquares)) - velocitySouthEast[x][y])
                    velocitySouthWest[x][y] += relaxationTime * (((1.0 / 36.0) * sumVelocities
                        * (1 - threeXVelocity - threeYVelocity + 4.5 * (sumSquares + twoXYVelocities) - 1.5 * sumSquares)) - velocitySouthWest[x][y])
                }
            }
        }
    }

    /**
     * Handles boundary conditions by reversing direction vector and adding it to the corresponding value.
     */
    private fun collideBoundary() {
        for (x in 0..<rows) {
            for (y in 0..<cols) {
                if (solid[x][y]) {
                    if (velocityUp[x][y] > 0) {
                        velocityDown[x][y - 1] += velocityUp[x][y]
                        velocityUp[x][y] = 0.0
                    }
                    if (velocityDown[x][y] > 0) {
                        velocityUp[x][y + 1] += velocityDown[x][y]
                        velocityDown[x][y] = 0.0
                    }
                    if (velocityRight[x][y] > 0) {
                        velocityLeft[x - 1][y] += velocityRight[x][y]
                        velocityRight[x][y] = 0.0
                    }
                    if (velocityLeft[x][y] > 0) {
                        velocityRight[x + 1][y] += velocityLeft[x][y]
                        velocityLeft[x][y] = 0.0
                    }

                    if (velocityNorthWest[x][y] > 0) {
                        velocitySouthEast[x + 1][y - 1] += velocityNorthWest[x][y]
                        velocityNorthWest[x][y] = 0.0
                    }
                    if (velocityNorthEast[x][y] > 0) {
                        velocitySouthWest[x - 1][y - 1] += velocityNorthEast[x][y]
                        velocityNorthEast[x][y] = 0.0
                    }
                    if (velocitySouthWest[x][y] > 0) {
                        velocityNorthEast[x + 1][y + 1] += velocitySouthWest[x][y]
                        velocitySouthWest[x][y] = 0.0
                    }
                    if (velocitySouthEast[x][y] > 0) {
                        velocityNorthWest[x - 1][y + 1] += velocitySouthEast[x][y]
                        velocitySouthEast[x][y] = 0.0
                    }
                }
            }
        }
    }

    /**
     * Propagates flow: f(x+e*deltat, t+deltat)=f(x,t+deltat)
     */
    private fun move() {

        // constants for the following loops

        val v = overallVelocity
        val threeTimesOverallVelocity = 3.0 * v
        val threeTimesOverallVelocitySquared = threeTimesOverallVelocity * v
        val here = 4.0 / 9.0 * (1 - 1.5 * v * v)
        val up = 1.0 / 9.0 * (1 - 1.5 * v * v)
        val down = 1.0 / 9.0 * (1 - 1.5 * v * v)
        val left = 1.0 / 9.0 * (1 - threeTimesOverallVelocity + threeTimesOverallVelocitySquared)
        val right = 1.0 / 9.0 * (1 + threeTimesOverallVelocity + threeTimesOverallVelocitySquared)
        val northEast = 1.0 / 36.0 * (1 + threeTimesOverallVelocity + threeTimesOverallVelocitySquared)
        val northWest = 1.0 / 36.0 * (1 - threeTimesOverallVelocity + threeTimesOverallVelocitySquared)
        val southEast = 1.0 / 36.0 * (1 + threeTimesOverallVelocity + threeTimesOverallVelocitySquared)
        val southWest = 1.0 / 36.0 * (1 - threeTimesOverallVelocity + threeTimesOverallVelocitySquared)

        // handle edges
        for (c in 0..<cols - 1) {
            velocityDown[0][c] = velocityDown[0][c + 1]
        }

        for (c in cols - 1 downTo 1) {
            velocityUp[rows - 1][c] = velocityUp[rows - 1][c - 1]
        }

        // handle the 8 directions; note that velocityHere is not propagated
        for (r in 0..<rows - 1) {
            for (c in cols - 1 downTo 1) {
                velocityUp[r][c] = velocityUp[r][c - 1] // moves up component
                velocityNorthWest[r][c] = velocityNorthWest[r + 1][c - 1] // moves northWest component
            }
        }

        for (r in rows - 1 downTo 1) {
            for (c in cols - 1 downTo 1) {
                velocityRight[r][c] = velocityRight[r - 1][c] // moves right component
                velocityNorthEast[r][c] = velocityNorthEast[r - 1][c - 1] // moves northEast component
            }
        }

        for (r in rows - 1 downTo 1) {
            for (c in 0..<cols - 1) {
                velocityDown[r][c] = velocityDown[r][c + 1] // moves down component
                velocitySouthEast[r][c] = velocitySouthEast[r - 1][c + 1] // moves southEast component
            }
        }

        for (r in 0..<rows - 1) {
            for (c in 0..<cols - 1) {
                velocityLeft[r][c] = velocityLeft[r + 1][c] // moves left component
                velocitySouthWest[r][c] = velocitySouthWest[r + 1][c + 1] // moves southWest component
            }
        }


        // wind tunnel walls boundary condition
        for (x in 0..<rows) {
            velocityHere[x][0] = here
            velocityRight[x][0] = right
            velocityLeft[x][0] = left
            velocityUp[x][0] = up
            velocityDown[x][0] = down
            velocityNorthEast[x][0] = northEast
            velocitySouthEast[x][0] = southEast
            velocityNorthWest[x][0] = northWest
            velocitySouthWest[x][0] = southWest
            velocityHere[x][cols - 1] = here
            velocityRight[x][cols - 1] = right
            velocityLeft[x][cols - 1] = left
            velocityUp[x][cols - 1] = up
            velocityDown[x][cols - 1] = down
            velocityNorthEast[x][cols - 1] = northEast
            velocitySouthEast[x][cols - 1] = southEast
            velocityNorthWest[x][cols - 1] = northWest
            velocitySouthWest[x][cols - 1] = southWest
        }

        // inlet boundary condition (fluid entering)
        for (y in 0..<cols) {
            if (!solid[0][y]) {
                velocityRight[0][y] = right
                velocityNorthEast[0][y] = northEast
                velocitySouthEast[0][y] = southEast
            }
        }

        // outlet boundary condition, rho = 0
        for (y in 0..<cols) {
            if (!solid[0][y]) {
                velocityLeft[rows - 1][y] = left
                velocityNorthWest[rows - 1][y] = northWest
                velocitySouthWest[rows - 1][y] = southWest
            }
        }
    }

    fun setSolid(r: Int, c: Int) {
        solid[r][c] = true
    }

    fun solid(r: Int, c: Int) = solid[r][c]

    /**
     * Returns velocity value.
     * When no overallVelocity is specified,
     * the velocity value is e.g., 1E-5 or 1E-6.
     */
    fun velocity(r: Int, c: Int) = vSquared[r][c]

    fun velocityX(r: Int, c: Int) = vX[r][c]
    fun velocityY(r: Int, c: Int) = vY[r][c]

    /**
     * Returns density value. The stable density is 1.0
     */
    fun density(r: Int, c: Int) = density[r][c]

    /**
     * Iterates over all lattices.
     */
    fun forEach(
        consumer: (
            x: Int, y: Int,
            solid: Boolean,
            density: Double,
            vx: Double,
            vy: Double,
            velocity: Double,
        ) -> Unit
    ) {
        for (x in 0..<rows) {
            for (y in 0..<cols) {
                consumer(
                    x, y, solid(x, y), density(x, y),
                    velocityX(x, y),
                    velocityY(x, y),
                    velocity(x, y)
                )
            }
        }
    }

    fun resetSolids() {
        for (x in 0..<rows) {
            for (y in 0..<cols) {
                solid[x][y] = false
            }
        }
    }
}
