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

/**
 * Fractal Brownian Motion over raw float coordinates (for performance).
 *
 * Accumulates exactly like the [Point] overload, but allocates nothing: that one builds a
 * fresh `Point` per octave through `times`, which rules it out of a per-pixel or per-cell
 * loop. Reach for this one whenever the call sits in a hot path.
 *
 * @param noise the noise source, sampled as `(x, y)`; defaults to [SimplexNoise].
 */
fun fbm(
    x: Float,
    y: Float,
    octaves: Int = 6,
    lacunarity: Float = 4.0f,
    gain: Float = 0.6f,
    frequency: Float = 1.0f,
    amplitude: Float = 0.5f,
    offset: Float = 0.0f,
    noise: (Float, Float) -> Float = SimplexNoise::noise,
): Float {
    var value = offset
    var amp = amplitude
    var px = x
    var py = y

    repeat(octaves) {
        value += amp * noise(px * frequency, py * frequency)
        px *= lacunarity
        py *= lacunarity
        amp *= gain
    }

    return value
}
