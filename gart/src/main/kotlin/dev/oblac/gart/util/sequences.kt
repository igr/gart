package dev.oblac.gart.util

/**
 * Generates a sequence of integers starting from [from], incrementing by [step], and taking [count] elements.
 *
 * @param from The starting integer of the sequence.
 * @param count The number of elements to generate in the sequence.
 * @param step The increment step between consecutive integers in the sequence. Default is 1.
 * @return A sequence of integers.
 */
fun countSequence(from: Int, count: Int, step: Int = 1) =
    generateSequence(from) { it + step }.take(count)

fun forSequence(from: Int, to: Int, step: Int = 1) =
    generateSequence(from) { it + step }.take((to - from) / step)

fun repeatSequence(times: Int) =
    generateSequence(0) { it + 1 }.take(times)
