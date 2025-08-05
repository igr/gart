package dev.oblac.gart.noise

import dev.oblac.gart.gfx.times
import org.jetbrains.skia.Point

/**
 * Fractal Brownian Motion.
 */
fun fbm(
    point: Point,
    octaves: Int = 6,
    lacunarity: Float = 4.0f,
    gain: Float = 0.6f,
    frequency: Float = 1.0f,
    amplitude: Float = 0.5f,
    offset: Float = 0.0f,
    noise: (Point) -> Float,
): Float {
    var value = offset
    var amplitude = amplitude
    var p = point

    repeat(octaves) {
        value += amplitude * noise(p * frequency)
        p *= lacunarity
        amplitude *= gain
    }

    return value
}
