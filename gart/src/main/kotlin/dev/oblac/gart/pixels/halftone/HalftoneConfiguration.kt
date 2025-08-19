package dev.oblac.gart.pixels.halftone

/**
 * Configuration for halftone rendering.
 * Uses TV defaults for angles.
 */
data class HalftoneConfiguration(
    val dotSize: Int = 10,
    val dotResolution: Int = 5,
    val yellowAngle: Float = 82.5f,
    val cyanAngle: Float = 112.5f,
    val magentaAngle: Float = 52.5f,
    val keyAngle: Float = 22.5f,
)
