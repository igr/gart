package dev.oblac.gart.pixels

import dev.oblac.gart.MemPixels
import dev.oblac.gart.Pixels
import dev.oblac.gart.color.*
import dev.oblac.gart.gfx.Circle

fun liquify(target: Pixels, circle: Circle, growFactor: Float = 0.7f) {
    val width = target.d.w
    val height = target.d.h
    val centerX = circle.x.toInt()
    val centerY = circle.y.toInt()
    val radius = circle.radius.toInt()
    val radiusSquared = radius * radius
    
    val srcPixels = MemPixels(target.d)
    srcPixels.copyPixelsFrom(target)

    for (y in -radius until radius) {
        for (x in -radius until radius) {
            // Check if the pixel is inside the effect circle
            if (x * x + y * y <= radiusSquared) {
                // Get the destination pixel position
                val destX = x + centerX
                val destY = y + centerY

                if (destX in 0..<width && destY in 0..<height) {
                    // Transform the pixel Cartesian coordinates (x, y) to polar coordinates (r, alpha)
                    val r = kotlin.math.sqrt((x * x + y * y).toDouble())
                    val alpha = kotlin.math.atan2(y.toDouble(), x.toDouble())

                    val interpolationFactor = r / radius

                    // interpolation
                    val newR = interpolationFactor * r + (1.0 - interpolationFactor) * growFactor * kotlin.math.sqrt(r)

                    // back from polar coordinates to Cartesian
                    val newY = newR * kotlin.math.sin(alpha)
                    val newX = newR * kotlin.math.cos(alpha)

                    // Calculate the (x, y) coordinates of the transformation and keep
                    // the fractional part in the delta variables
                    val x0 = kotlin.math.floor(newX).toInt()
                    val xf = x0 + 1
                    val y0 = kotlin.math.floor(newY).toInt()
                    val yf = y0 + 1
                    val deltaX = newX - x0
                    val deltaY = newY - y0

                    // Check bounds for source coordinates
                    val srcX0 = x0 + centerX
                    val srcXf = xf + centerX
                    val srcY0 = y0 + centerY
                    val srcYf = yf + centerY

                    if (srcX0 >= 0 && srcXf < width && srcY0 >= 0 && srcYf < height) {
                        // Get the four surrounding pixels
                        val pixel00 = srcPixels[srcX0, srcY0]
                        val pixel10 = srcPixels[srcXf, srcY0]
                        val pixel01 = srcPixels[srcX0, srcYf]
                        val pixel11 = srcPixels[srcXf, srcYf]

                        // Extract ARGB components and do bilinear interpolation
                        val a = bilinearInterpolate(
                            alpha(pixel00), alpha(pixel10), alpha(pixel01), alpha(pixel11),
                            deltaX, deltaY
                        ).coerceIn(0, 255)

                        val r = bilinearInterpolate(
                            red(pixel00), red(pixel10), red(pixel01), red(pixel11),
                            deltaX, deltaY
                        ).coerceIn(0, 255)

                        val g = bilinearInterpolate(
                            green(pixel00), green(pixel10), green(pixel01), green(pixel11),
                            deltaX, deltaY
                        ).coerceIn(0, 255)

                        val bl = bilinearInterpolate(
                            blue(pixel00), blue(pixel10), blue(pixel01), blue(pixel11),
                            deltaX, deltaY
                        ).coerceIn(0, 255)

                        // target
                        target[destX, destY] = argb(a, r, g, bl)
                    }
                }
            }
        }
    }
}

private fun bilinearInterpolate(
    c00: Int, c10: Int, c01: Int, c11: Int,
    deltaX: Double, deltaY: Double
): Int {
    // Interpolate along x for top row
    val top = c00 * (1.0 - deltaX) + c10 * deltaX
    // Interpolate along x for bottom row
    val bottom = c01 * (1.0 - deltaX) + c11 * deltaX
    // Interpolate along y between the two rows
    return (top * (1.0 - deltaY) + bottom * deltaY).toInt()
}
