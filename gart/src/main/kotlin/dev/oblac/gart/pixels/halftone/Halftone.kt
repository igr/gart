package dev.oblac.gart.pixels.halftone

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.red
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.color.space.RGBA.Companion.BLACK
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Renders a halftone pattern from source pixels to target pixels.
 * @param isLayer If true, does not clear the target background.
 */
fun renderHalftone(
    source: Pixels,
    target: Pixels,
    angle: Double,
    dotSize: Int,
    dotResolution: Int,
    color: RGBA = BLACK,
    isLayer: Boolean = false
) {
    val angleRad = angle * PI / 180.0
    val width = source.d.w
    val height = source.d.h

    // Clear background if not a layer
    if (!isLayer) {
        target.fill(Colors.white)
    }

    // Calculate rotation boundaries to determine iteration range
    val boundaries = calculateRotationBoundaries(width, height, angleRad)
    val (minX, minY, maxX, maxY) = boundaries

    // Iterate through the rotated grid
    for (y in minY until maxY step dotResolution) {
        for (x in minX until maxX step dotResolution) {
            processHalftonePoint(
                source, target,
                x, y, width, height, angleRad,
                dotSize, color
            )
        }
    }
}

private fun calculateRotationBoundaries(width: Int, height: Int, angle: Double): List<Int> {
    val centerX = width / 2.0
    val centerY = height / 2.0

    // Get the four corners of the screen
    val corners = listOf(
        0.0 to 0.0,                    // top-left
        width.toDouble() to 0.0,       // top-right
        width.toDouble() to height.toDouble(), // bottom-right
        0.0 to height.toDouble()       // bottom-left
    )

    // Rotate all corners and find min/max bounds
    val rotatedCorners = corners.map { corner ->
        rotatePointAroundPosition(corner, centerX to centerY, angle)
    }

    val minX = rotatedCorners.minOf { it.first }.toInt()
    val minY = rotatedCorners.minOf { it.second }.toInt()
    val maxX = rotatedCorners.maxOf { it.first }.toInt()
    val maxY = rotatedCorners.maxOf { it.second }.toInt()

    return listOf(minX, minY, maxX, maxY)
}

private fun processHalftonePoint(
    source: Pixels,
    target: Pixels,
    x: Int, y: Int,
    width: Int, height: Int,
    angle: Double,
    dotSize: Int,
    color: RGBA
) {
    val centerX = width / 2.0
    val centerY = height / 2.0

    // Rotate the point back to original coordinate system
    val (rotatedX, rotatedY) = rotatePointAroundPosition(
        x.toDouble() to y.toDouble(),
        centerX to centerY,
        -angle
    )

    // Check bounds
    if (rotatedX < 0 || rotatedY < 0 || rotatedX >= width || rotatedY >= height) {
        return
    }

    val pixelX = rotatedX.toInt()
    val pixelY = rotatedY.toInt()

    // Sample the grayscale value (we assume it's grayscale so we just take red channel)
    val sourcePixel = source[pixelX, pixelY]
    val value = red(sourcePixel)
    val alpha = alpha(sourcePixel)

    // Only draw if pixel has alpha
    if (alpha > 0) {
        // Map brightness to circle radius (brighter = smaller circle)
        val circleRadius = map(value.toDouble(), 0.0, 255.0, dotSize / 2.0, 0.0)

        if (circleRadius > 0) {
            drawCircle(target, rotatedX, rotatedY, circleRadius, color)
        }
    }
}

private fun drawCircle(
    pixels: Pixels,
    centerX: Double,
    centerY: Double,
    radius: Double,
    color: RGBA
) {
    val cx = centerX.toInt()
    val cy = centerY.toInt()
    val r = radius.toInt()

    // Use Bresenham-like approach for filled circle
    for (dy in -r..r) {
        for (dx in -r..r) {
            val x = cx + dx
            val y = cy + dy

            // Check if point is within canvas bounds
            if (x >= 0 && y >= 0 && x < pixels.d.w && y < pixels.d.h) {
                // Check if point is inside circle
                val distance = sqrt((dx * dx + dy * dy).toDouble())
                if (distance <= radius) {
                    pixels[x, y] = color.value
                }
            }
        }
    }
}
