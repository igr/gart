package dev.oblac.gart.pixels

import dev.oblac.gart.Gartmap
import dev.oblac.gart.MemPixels
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.cos
import dev.oblac.gart.angle.sin
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.space.RGBA

/**
 * Apply motion blur effect to simulate movement in a specific direction.
 */
fun applyMotionBlur(b: Gartmap, distance: Int, angle: Angle) {
    val width = b.d.w
    val height = b.d.h
    val newBitmap = MemPixels(b.d)

    // calculate direction vector from angle
    val dx = cos(angle) * distance
    val dy = sin(angle) * distance

    // number of samples along the motion path
    val samples = distance.coerceAtLeast(1)

    for (x in 0 until width) {
        for (y in 0 until height) {
            var sumR = 0
            var sumG = 0
            var sumB = 0
            var sumA = 0
            var validSamples = 0

            // sample along the motion vector
            for (i in 0 until samples) {
                val t = i.toFloat() / samples
                val sampleX = (x - dx * t).toInt()
                val sampleY = (y - dy * t).toInt()

                // check bounds
                if (sampleX in 0 until width && sampleY in 0 until height) {
                    val pixel = b[sampleX, sampleY]
                    sumR += red(pixel)
                    sumG += green(pixel)
                    sumB += blue(pixel)
                    sumA += alpha(pixel)
                    validSamples++
                }
            }

            if (validSamples > 0) {
                val newR = (sumR / validSamples).coerceIn(0, 255)
                val newG = (sumG / validSamples).coerceIn(0, 255)
                val newB = (sumB / validSamples).coerceIn(0, 255)
                val newA = (sumA / validSamples).coerceIn(0, 255)
                newBitmap[x, y] = RGBA(newR, newG, newB, newA).value
            } else {
                newBitmap[x, y] = b[x, y]
            }
        }
    }

    b.copyPixelsFrom(newBitmap)
}
