/**
 * Deterministic integer hashes producing a stable pseudo-random Float in `[0, 1)`.
 *
 * Handy for per-cell / per-index jitter that must stay identical across runs for a given
 * seed (e.g. seeded generative art). Uses an xxHash/murmur-style avalanche finalizer for
 * good bit diffusion.
 */
package dev.oblac.gart.math


/** Hashes two integer keys with a [seed]. */
fun hash01(a: Int, k: Int, seed: Int): Float =
    _hash01((a * 0x1f1f1f1f) xor (k * -0x61c88647) xor (seed * 0x165667b1))

/** Hashes three integer keys with a [seed]. */
fun hash01(a: Int, b: Int, k: Int, seed: Int): Float =
    _hash01((a * 0x1f1f1f1f) xor (b * -0x61c88647) xor (k * 0x27d4eb2f) xor (seed * 0x165667b1))

private fun _hash01(hash: Int): Float {
    var h = hash
    h = h xor (h ushr 15); h *= -0x7ee3623b
    h = h xor (h ushr 13); h *= -0x3d4d51cb
    h = h xor (h ushr 16)
    return ((h ushr 8) and 0xFFFF) / 65535f
}
