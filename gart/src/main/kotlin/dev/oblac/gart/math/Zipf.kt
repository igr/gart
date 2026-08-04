package dev.oblac.gart.math

import kotlin.math.pow
import kotlin.random.Random

/**
 * Sampler over a Zipf (power-law) distribution of [n] ranked items.
 *
 * Item `i` carries a weight of `1 / (i + 1)^s`, so rank 0 is the most likely one and the
 * tail falls away faster as the exponent [s] grows:
 *
 * - `s = 0` — every weight is 1, which is just a uniform pick
 * - `s = 1` — true Zipf, the harmonic series; roughly what natural language does
 * - `s > 1` — steeper than natural, a handful of items take nearly all the probability
 *
 * This is the thing that makes generated text read as *writing* rather than as texture: a
 * few tokens recur constantly and the rest stay rare, and that repetition is the rhythm the
 * eye actually recognises as language. Useful anywhere a pick should have favourites —
 * glyphs, words, palette entries, shape variants.
 *
 * Weights are built once into a normalised cumulative table and drawn by inverse transform,
 * costing exactly one [Random.nextFloat] per [next]. The lookup is a linear scan, so this is
 * meant for small-to-moderate [n] (alphabets, lexicons, palettes) and not for huge tables.
 */
class Zipf(n: Int, s: Float, private val random: Random = Random) {
    private val cum = FloatArray(n)

    init {
        require(n > 0) { "Zipf needs at least one item, got $n" }
        var t = 0f
        for (i in 0 until n) {
            t += 1f / (i + 1f).pow(s)
            cum[i] = t
        }
        for (i in cum.indices) cum[i] /= t // normalise
    }

    /** Number of ranked items this samples over. */
    val size get() = cum.size

    /** Draws a rank in `0 until size`, 0 being the most likely. */
    fun next(): Int {
        val u = random.nextFloat()
        for (i in cum.indices) if (u <= cum[i]) return i
        return cum.size - 1 // float slop, shouldn't really get here
    }
}
