package dev.oblac.gart.noise

/**
 * Simplex and friends have no seed of their own - the way to "seed" them is to shift the
 * sample window. Turns [seed] into that shift; add it to the noise coordinates.
 */
fun noiseOffset(seed: Long): Float = (seed and 0xffff) * 0.01f
