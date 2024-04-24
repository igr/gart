package dev.oblac.gart.math

import kotlin.random.Random

fun rnd(max: Int) = (Math.random() * max).toInt()

fun rnd(min: Int, max: Int) = (Math.random() * (max - min) + min).toInt()

fun rnd(min: Float, max: Float): Float = (Math.random() * (max - min) + min).toFloat()

fun nextFloat(range: Number) = Random.nextFloat() * range.toFloat()

/**
 * Returns true with the probability of [success] / [total].
 */
fun rndIn(success: Int, total: Int) = Math.random() < success.toDouble() / total
