package dev.oblac.gart.math

import kotlin.math.ln

/**
 * Binary entropy function: H(p) = -p*ln(p) - (1-p)*ln(1-p).
 * Returns 0 for p <= 0 or p >= 1.
 */
fun binaryEntropy(p: Double): Double {
    if (p <= 0.0 || p >= 1.0) return 0.0
    return -p * ln(p) - (1.0 - p) * ln(1.0 - p)
}

