package dev.oblac.gart.math

import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

fun Random.rndi(): Int = nextInt()
fun Random.rndi(max: Int): Int = nextInt(max)
fun Random.rndi(min: Int, max: Int): Int = nextInt(min, max)

fun Random.rndf(): Float = nextFloat()
fun Random.rndf(max: Float): Float = nextFloat() * max
fun Random.rndf(max: Number): Float = nextFloat() * max.toFloat()
fun Random.rndf(min: Float, max: Float): Float = nextFloat() * (max - min) + min
fun Random.rndf(min: Number, max: Number): Float = nextFloat() * (max.toFloat() - min.toFloat()) + min.toFloat()

/** Returns a uniformly distributed value between [from] and [to]. */
fun Random.between(from: Float, to: Float): Float = from + nextFloat() * (to - from)

fun Random.rnd(min: Double, max: Double): Double = min + (max - min) * nextDouble()

/**
 * Returns true with the probability of [success] / [total].
 */
fun Random.rndb(success: Int, total: Int): Boolean = nextDouble() < (success.toDouble() / total)

fun Random.rndb(): Boolean = nextDouble() < 0.5

fun Random.rndsgn(): Int = if (rndb()) 1 else -1

/**
 * Generates a random number following a Gaussian (normal) distribution.
 * Uses the Box-Muller transform to convert uniform random numbers to Gaussian distribution.
 *
 * @param mean The mean (center) of the distribution (default: 0.0)
 * @param standardDeviation The standard deviation (width) of the distribution (default: 1.0)
 * @return A random Float following the specified Gaussian distribution
 */
fun Random.rndGaussian(mean: Float = 0.0f, standardDeviation: Float = 1.0f): Float {
    // Box-Muller transform
    val u1 = nextDouble()
    val u2 = nextDouble()

    val z0 = sqrt(-2.0 * ln(u1)) * cos(2.0 * kotlin.math.PI * u2)

    return (z0 * standardDeviation + mean).toFloat()
}

/**
 * Returns a uniformly distributed random point inside a disc of radius [r].
 */
fun Random.rndInDisc(r: Float): Point {
    while (true) {
        val x = rndf(-r, r); val y = rndf(-r, r)
        if (x * x + y * y <= r * r) return Point(x, y)
    }
}

/**
 * Returns a uniformly distributed random point inside a ball of radius [r].
 */
fun Random.rndInBall(r: Float): Vec3 {
    while (true) {
        val x = rndf(-r, r); val y = rndf(-r, r); val z = rndf(-r, r)
        if (x * x + y * y + z * z <= r * r) return Vec3(x, y, z)
    }
}

fun rndi(): Int = Random.rndi()
fun rndi(max: Int): Int = Random.rndi(max)
fun rndi(min: Int, max: Int): Int = Random.rndi(min, max)

fun rndf(): Float = Random.rndf()
fun rndf(max: Float): Float = Random.rndf(max)
fun rndf(max: Number): Float = Random.rndf(max)
fun rndf(min: Float, max: Float): Float = Random.rndf(min, max)
fun rndf(min: Number, max: Number): Float = Random.rndf(min, max)

fun rnd(min: Double, max: Double): Double = Random.rnd(min, max)

fun rndb(success: Int, total: Int): Boolean = Random.rndb(success, total)
fun rndb(): Boolean = Random.rndb()
fun rndsgn(): Int = Random.rndsgn()

fun rndGaussian(mean: Float = 0.0f, standardDeviation: Float = 1.0f): Float =
    Random.rndGaussian(mean, standardDeviation)

fun rndInDisc(r: Float): Point = Random.rndInDisc(r)
fun rndInBall(r: Float): Vec3 = Random.rndInBall(r)
