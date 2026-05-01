package dev.oblac.gart.gfx

import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Point
import kotlin.math.max
import kotlin.random.Random

/**
 * Spline step.
 *
 * The list is treated as the ordered nodes of a path. Each node receives a
 * random-walk offset whose amplitude varies along the path from [minNoise] to
 * [maxNoise]. Repeatedly applying this function to its own output gives the
 * drifting-node behavior; feed the result to `toSmoothQuadraticPath()` to
 * draw the spline.
 */
fun List<Point>.withVaryingSplineNoise(
    minNoise: Float = 0f,
    maxNoise: Float = 8f,
    random: Random = Random.Default,
    preserveEnds: Boolean = false,
    noiseAt: (Float) -> Float = { it },
): List<Point> {
    if (isEmpty()) return emptyList()
    if (size == 1) return toList()

    val progressByPoint = progressAlongPath()
    return mapIndexed { index, point ->
        if (preserveEnds && (index == 0 || index == lastIndex)) {
            point
        } else {
            val progress = progressByPoint[index]
            val noiseAmount = lerp(minNoise, maxNoise, noiseAt(progress).coerceIn(0f, 1f))
            point.offset(
                random.rndf(-noiseAmount, noiseAmount),
                random.rndf(-noiseAmount, noiseAmount),
            )
        }
    }
}

private fun List<Point>.progressAlongPath(): FloatArray {
    val progress = FloatArray(size)
    var total = 0f
    for (index in 1..lastIndex) {
        total += this[index - 1].distanceTo(this[index])
        progress[index] = total
    }
    if (total == 0f) {
        return FloatArray(size) { it.toFloat() / max(1, lastIndex).toFloat() }
    }
    for (index in progress.indices) {
        progress[index] /= total
    }
    return progress
}
