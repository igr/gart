package dev.oblac.gart.pixels.halftone

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.CssColors
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
    angle: Float,
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
        target.fill(CssColors.white)
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
    val bound = (radius + 2.0).toInt()

    // Anti-aliased circle with coverage calculation
    for (dy in -bound..bound) {
        for (dx in -bound..bound) {
            val x = cx + dx
            val y = cy + dy

            // Check if point is within canvas bounds
            if (x >= 0 && y >= 0 && x < pixels.d.w && y < pixels.d.h) {
                val distance = sqrt((dx * dx + dy * dy).toDouble())
                
                if (distance <= radius + 1.0) {
                    val coverage = when {
                        distance <= radius - 1.0 -> 1.0
                        distance >= radius + 1.0 -> 0.0
                        else -> {
                            // Simple anti-aliasing: linear falloff in edge region
                            val edgeDistance = kotlin.math.abs(distance - radius)
                            1.0 - edgeDistance
                        }
                    }
                    
                    if (coverage > 0.0) {
                        // Apply coverage-based alpha blending
                        val existingPixel = pixels[x, y]
                        val blendedColor = blendWithCoverage(existingPixel, color.value, coverage)
                        pixels[x, y] = blendedColor
                    }
                }
            }
        }
    }
}

private fun blendWithCoverage(background: Int, foreground: Int, coverage: Double): Int {
    val alpha = coverage.toFloat()
    
    val bgA = (background ushr 24) and 0xFF
    val bgR = (background ushr 16) and 0xFF
    val bgG = (background ushr 8) and 0xFF
    val bgB = background and 0xFF
    
    val fgA = (foreground ushr 24) and 0xFF
    val fgR = (foreground ushr 16) and 0xFF
    val fgG = (foreground ushr 8) and 0xFF
    val fgB = foreground and 0xFF
    
    val finalA = (fgA * alpha + bgA * (1f - alpha)).toInt().coerceIn(0, 255)
    val finalR = (fgR * alpha + bgR * (1f - alpha)).toInt().coerceIn(0, 255)
    val finalG = (fgG * alpha + bgG * (1f - alpha)).toInt().coerceIn(0, 255)
    val finalB = (fgB * alpha + bgB * (1f - alpha)).toInt().coerceIn(0, 255)
    
    return (finalA shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
}
