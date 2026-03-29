package dev.oblac.gart.pixels.dither

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.luminance
import dev.oblac.gart.color.space.of
import dev.oblac.gart.math.doubleLoop
import org.jetbrains.skia.Color4f
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Dot-based dithering: divides the image into cells and replaces each cell
 * with a filled circle whose radius is proportional to the cell's darkness.
 * Darker cells get larger dots, lighter cells get smaller (or no) dots.
 *
 * @param dotSize cell size in pixels (each cell becomes one dot)
 * @param foreground dot color (ARGB)
 * @param background cell background color (ARGB)
 */
fun ditherDots(
    bitmap: Pixels,
    dotSize: Int = 8,
    gap: Float = 0f,
    foreground: Int = 0xFF000000.toInt(),
    background: Int = 0xFFFFFFFF.toInt()
) {
    require(dotSize >= 2) { "Dot size must be 2 or greater" }
    require(gap >= 0f) { "Gap must be non-negative" }
    val width = bitmap.d.w
    val height = bitmap.d.h
    val maxRadius = (dotSize / 2f) - gap

    doubleLoop(width, height, dotSize, dotSize) { (cx, cy) ->
        val brightness = cellBrightness(bitmap, cx, cy, dotSize)
        // darkness 0..1 (0 = white, 1 = black)
        val darkness = 1f - brightness
        val radius = maxRadius * darkness

        // clear cell to background
        for (dy in 0 until min(dotSize, height - cy)) {
            for (dx in 0 until min(dotSize, width - cx)) {
                bitmap[cx + dx, cy + dy] = background
            }
        }

        // draw filled circle
        if (radius > 0.4f) {
            val centerX = cx + maxRadius
            val centerY = cy + maxRadius
            val bound = (radius + 1f).toInt()
            val icx = centerX.toInt()
            val icy = centerY.toInt()

            for (dy in -bound..bound) {
                for (dx in -bound..bound) {
                    val px = icx + dx
                    val py = icy + dy
                    if (px in 0 until width && py in 0 until height) {
                        val dist = sqrt((dx * dx + dy * dy).toFloat())
                        if (dist <= radius) {
                            bitmap[px, py] = foreground
                        }
                    }
                }
            }
        }
    }
}

private fun cellBrightness(bitmap: Pixels, cx: Int, cy: Int, dotSize: Int): Float {
    var total = 0f
    var count = 0
    for (dy in 0 until min(dotSize, bitmap.d.h - cy)) {
        for (dx in 0 until min(dotSize, bitmap.d.w - cx)) {
            val pixel = bitmap[cx + dx, cy + dy]
            total += Color4f.of(pixel).luminance
            count++
        }
    }
    return if (count > 0) total / count else 1f
}
