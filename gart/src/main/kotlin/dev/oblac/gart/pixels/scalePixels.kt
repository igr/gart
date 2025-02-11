package dev.oblac.gart.pixels

import dev.oblac.gart.Dimension
import dev.oblac.gart.MemPixels
import dev.oblac.gart.Pixels
import dev.oblac.gart.color.*

fun createScaledPixels(input: Pixels, newD: Dimension): Pixels {
    val newWidth = newD.w
    val newHeight = newD.h
    val output = MemPixels(newD)
    val scaleX = input.d.w.toDouble() / newWidth
    val scaleY = input.d.h.toDouble() / newHeight

    for (y in 0 until newHeight) {
        for (x in 0 until newWidth) {
            // Find the corresponding pixel in the original image
            val srcX = x * scaleX
            val srcY = y * scaleY

            // Get the 4 surrounding pixels
            val x1 = srcX.toInt().coerceIn(0, input.d.w - 1)
            val y1 = srcY.toInt().coerceIn(0, input.d.h - 1)
            val x2 = (x1 + 1).coerceIn(0, input.d.w - 1)
            val y2 = (y1 + 1).coerceIn(0, input.d.h - 1)

            // Get pixel colors
            val c11 = input[x1, y1]
            val c12 = input[x1, y2]
            val c21 = input[x2, y1]
            val c22 = input[x2, y2]

            // Interpolation weights
            val dx = srcX - x1
            val dy = srcY - y1

            // Bilinear interpolation
            val r = (red(c11) * (1 - dx) * (1 - dy) +
                red(c21) * dx * (1 - dy) +
                red(c12) * (1 - dx) * dy +
                red(c22) * dx * dy).toInt()

            val g = (green(c11) * (1 - dx) * (1 - dy) +
                green(c21) * dx * (1 - dy) +
                green(c12) * (1 - dx) * dy +
                green(c22) * dx * dy).toInt()

            val b = (blue(c11) * (1 - dx) * (1 - dy) +
                blue(c21) * dx * (1 - dy) +
                blue(c12) * (1 - dx) * dy +
                blue(c22) * dx * dy).toInt()

            val a = (alpha(c11) * (1 - dx) * (1 - dy) +
                alpha(c21) * dx * (1 - dy) +
                alpha(c12) * (1 - dx) * dy +
                alpha(c22) * dx * dy).toInt()

            // Set new pixel value
            output[x, y] = argb(a, r, b, g)
        }
    }
    return output
}
