package dev.oblac.gart.math

import kotlin.random.Random

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
