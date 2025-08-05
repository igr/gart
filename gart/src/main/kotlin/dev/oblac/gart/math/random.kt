package dev.oblac.gart.math

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

fun rndi(): Int = Random.nextInt()
fun rndi(max: Int): Int = Random.nextInt(max)
fun rndi(min: Int, max: Int): Int = Random.nextInt(min, max)

fun rndf() = Random.nextFloat()
fun rndf(max: Float): Float = Random.nextFloat() * max
fun rndf(max: Number): Float = Random.nextFloat() * max.toFloat()
fun rndf(min: Float, max: Float): Float = (Random.nextFloat() * (max - min) + min)
fun rndf(min: Number, max: Number): Float = (Random.nextFloat() * (max.toFloat() - min.toFloat()) + min.toFloat())

fun rnd(min: Double, max: Double): Double = min + (max - min) * Random.nextDouble()

/**
 * Returns true with the probability of [success] / [total].
 */
fun rndb(success: Int, total: Int): Boolean = Math.random() < (success.toDouble() / total)

fun rndb(): Boolean = Math.random() < 0.5

/**
 * Generates a random number following a Gaussian (normal) distribution.
 * Uses the Box-Muller transform to convert uniform random numbers to Gaussian distribution.
 *
 * @param mean The mean (center) of the distribution (default: 0.0)
 * @param standardDeviation The standard deviation (width) of the distribution (default: 1.0)
 * @return A random Float following the specified Gaussian distribution
 */
fun rndGaussian(mean: Float = 0.0f, standardDeviation: Float = 1.0f): Float {
    // Box-Muller transform
    val u1 = Random.nextDouble()
    val u2 = Random.nextDouble()

    val z0 = sqrt(-2.0 * ln(u1)) * cos(2.0 * kotlin.math.PI * u2)

    return (z0 * standardDeviation + mean).toFloat()
}
