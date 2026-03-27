package dev.oblac.gart.knot

/**
 * Each wave is defined as a list of (x, dy) knots.
 * x is in [0..1] range (normalized across width).
 * dy is the vertical offset from the baseline (positive = up).
 */
data class Knot(val x: Float, val dy: Float)
