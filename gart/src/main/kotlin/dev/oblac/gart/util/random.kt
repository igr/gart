package dev.oblac.gart.util

import kotlin.random.Random

/**
 * Random number generator that can be reset to the initial state.
 * Very useful for frame-by-frame drawing where you want the same random values.
 */
class RRandom {
    private val seed = Random.nextLong()

    var rnd = Random(seed)

    /**
     * Resets the random number generator to its initial state.
     * Usually called at the end of the drawing frame.
     */
    fun reset() {
        rnd = Random(seed)
    }
}
