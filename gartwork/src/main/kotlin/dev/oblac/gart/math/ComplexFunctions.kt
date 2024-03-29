package dev.oblac.gart.math

/**
 * Just a set of complex functions.
 * Function names are pure fantasy.
 */
object ComplexFunctions {

    val simple = { z: Complex -> (z - .5) * (z + .5) * z }

    val threes = { z: Complex -> (z - .5) * (z + .5) * z / ((z - 0.5 * i + 0.5) * (z + 0.5 * i - 0.5) * (z + .1 - .1 * i)) }

    /**
     * Creates a function that has poles and holes at given points.
     */
    fun polesAndHoles(poles: Array<Complex>, holes: Array<Complex>) = { z: Complex ->
        var result = z - holes[0]
        for (hole in holes.slice(1 until holes.size)) {
            result *= (z - hole)
        }
        for (pole in poles) {
            result /= (z - pole)
        }
        result
    }
}

